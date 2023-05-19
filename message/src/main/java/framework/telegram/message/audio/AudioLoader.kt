package framework.telegram.message.audio

import android.app.Activity
import android.net.Uri
import framework.ideas.common.model.im.MessageModel
import framework.telegram.message.controller.DownloadAttachmentController
import framework.telegram.support.BaseApp

import java.io.File

import framework.telegram.support.tools.ThreadUtils
import framework.telegram.ui.videoplayer.utils.NetworkUtils
import java.lang.ref.WeakReference

class AudioLoader(var audioPlayer: AudioPlayer) : DownloadAttachmentController.DownloadAttachmentListener {

    //正在loading的消息(消息的id)
    var loadingTag: Any? = null
        private set

    var isSpeakerphoneOn = false
        private set

    private var downloadUrl: String? = null

    private var activity: WeakReference<Activity>? = null

    private var tag: String = ""

    private var seekTo = 0

    /**
     * 播放语音消息
     *
     * @param audioPlayer
     * @param activity
     * @param messageModel
     * @param isSpeakerphoneOn
     * @param seekTo
     */
    fun playVoice(activity: WeakReference<Activity>?, chatType: Int, messageModel: MessageModel, tag: String, isSpeakerphoneOn: Boolean, seekTo: Int) {
        ThreadUtils.runOnIOThread {
            try {
                if (activity?.get() == null) {
                    return@runOnIOThread
                }

                loadingTag = 0

                this@AudioLoader.activity = activity
                this@AudioLoader.tag = tag
                this@AudioLoader.isSpeakerphoneOn = isSpeakerphoneOn
                this@AudioLoader.seekTo = seekTo
                this@AudioLoader.downloadUrl = null

                val voiceMessage = messageModel.voiceMessageContent
                val cacheFileUri = voiceMessage.recordFileBackupUri
                val downloadUrl = voiceMessage.recordFileUri
                val msgModel = messageModel.copyMessage()
                val cacheFile = DownloadAttachmentController.hasCacheFile(cacheFileUri, downloadUrl, msgModel)
                if (cacheFile != null) {
                    try {
                        audioPlayer.startPlaying(activity, Uri.fromFile(cacheFile), tag, isSpeakerphoneOn, seekTo)
                    } catch (e: Exception) {
                        //视频播放失败
                        cacheFile.delete()
                    }
                } else {
                    if (!DownloadAttachmentController.isDownloading(chatType, msgModel)) {
                        if (NetworkUtils.isAvailable(BaseApp.app)) {
                            this@AudioLoader.downloadUrl = downloadUrl
                            DownloadAttachmentController.downloadAttachment(chatType, msgModel)
                        } else {
                            audioPlayer.onAudioPlayerListener?.onStopPlay()
                        }
                    }
                }
            } catch (e: Exception) {
                audioPlayer.onAudioPlayerListener?.onStopPlay()
            }
        }
    }

    fun attachListener() {
        DownloadAttachmentController.attachDownloadListener(this@AudioLoader)
    }

    fun detachListener() {
        DownloadAttachmentController.detachDownloadListener(this@AudioLoader)
    }

    override fun downloadStart(downloadUrl: String, chatType: Int, targetId: Long, msgLocalId: Long) {
        if (downloadUrl == this@AudioLoader.downloadUrl) {
            audioPlayer.onAudioPlayerListener?.onDownloadStart()
        }
    }

    override fun downloadProgress(downloadUrl: String, chatType: Int, targetId: Long, msgLocalId: Long, percent: Double, currentOffset: Long, totalLength: Long) {
        if (downloadUrl == this@AudioLoader.downloadUrl) {
            audioPlayer.onAudioPlayerListener?.onDownloadProgress(currentOffset, totalLength, (currentOffset / (totalLength * 1.0f)).toInt())
        }
    }

    override fun downloadCancel(downloadUrl: String, chatType: Int, targetId: Long, msgLocalId: Long) {
        if (downloadUrl == this@AudioLoader.downloadUrl) {
            loadingTag = 0

            audioPlayer.onAudioPlayerListener?.onStopPlay()
        }
    }

    override fun downloadComplete(downloadUrl: String, chatType: Int, targetId: Long, msgLocalId: Long, file: File) {
        if (downloadUrl == this@AudioLoader.downloadUrl) {
            loadingTag = 0

            audioPlayer.onAudioPlayerListener?.onDownloadComplete()

            activity?.let {
                audioPlayer.startPlaying(it, Uri.fromFile(file), tag, isSpeakerphoneOn, seekTo)
            }
        }
    }

    override fun downloadFail(downloadUrl: String, chatType: Int, targetId: Long, msgLocalId: Long) {
        if (downloadUrl == this@AudioLoader.downloadUrl) {
            loadingTag = 0

            audioPlayer.onAudioPlayerListener?.onStopPlay()
        }
    }
}
