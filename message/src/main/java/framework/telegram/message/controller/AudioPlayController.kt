package framework.telegram.message.controller

import android.annotation.SuppressLint
import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import com.caesar.musicspectrumbarlibrary.MusicSpectrumBar
import com.chad.library.adapter.base.BaseViewHolder
import com.qmuiteam.qmui.widget.QMUIProgressBar
import framework.ideas.common.audio.AudioSampleUtils
import framework.ideas.common.model.im.MessageModel
import framework.telegram.business.bridge.bean.AccountInfo
import framework.telegram.message.R
import framework.telegram.message.audio.AudioLoader
import framework.telegram.message.audio.AudioPlayer
import framework.telegram.message.manager.MessagesManager
import framework.telegram.message.ui.adapter.MessageAdapter
import framework.telegram.message.ui.widget.MessageInputView
import framework.telegram.support.BaseApp
import framework.telegram.support.account.AccountManager
import framework.telegram.support.tools.ThreadUtils
import framework.telegram.support.tools.TimeUtils
import framework.telegram.ui.utils.ScreenUtils
import java.lang.ref.WeakReference

@SuppressLint("UseSparseArrays")
class AudioPlayController(private val activityRef: WeakReference<Activity>, private val adapter: MessageAdapter,
                          private val layoutManager: LinearLayoutManager, private val messageInputView: MessageInputView?,
                          private var isSpeakerphoneOn: Boolean = false, private var chatType: Int) : AudioPlayer.OnAudioPlayerListener {

    private val mVoiceMessageLastPlayList by lazy { HashMap<Long, Int>() }

    /**
     * 语音文件加载器
     */
    private val mAudioLoader by lazy {
        AudioLoader(mAudioPlayer)
    }

    /**
     * 语音播放器
     */
    private val mAudioPlayer by lazy {
        val audioPlayer = AudioPlayer(this@AudioPlayController)
        audioPlayer
    }

    private var mLastPlayVoiceMessage: MessageModel? = null

    /**
     * 停止当前播放语音
     */
    fun stopPlayVoice(clearVoiceLastTimeCache: Boolean = true) {
        if (clearVoiceLastTimeCache && mLastPlayVoiceMessage != null) {
            mVoiceMessageLastPlayList.remove(mLastPlayVoiceMessage!!.id)
        }

        mAudioPlayer.stopPlaying(activityRef)
    }

    /**
     * 如果是当前消息则停止播放
     */
    fun stopPlayVoice(messageModel: MessageModel) {
        if (mLastPlayVoiceMessage != null && mLastPlayVoiceMessage?.id == messageModel.id) {
            stopPlayVoice()
        }
    }

    fun attachListener() {
        mAudioLoader.attachListener()
    }

    /**
     * 移除语音下载监听器
     */
    fun detachListener() {
        mAudioLoader.detachListener()
    }

    /**
     * 切换语音播放方式
     */
    fun setSpeakerphoneOn(isSpeakerphoneOn: Boolean) {
        if (!mAudioPlayer.isPlaying) {
            return
        }

        if (mAudioLoader.isSpeakerphoneOn == isSpeakerphoneOn) {
            return
        }

        mLastPlayVoiceMessage?.let {
            val currentPosition = mAudioPlayer.stopPlaying(activityRef, false)
            mAudioLoader.playVoice(activityRef, chatType, it, it.id.toString(), isSpeakerphoneOn, currentPosition)
        }
    }

    fun playVoice(messageModel: MessageModel?) {
        playVoice(messageModel, isSpeakerphoneOn, 0)
    }

    fun playVoice(messageModel: MessageModel?, isSpeakerphoneOn: Boolean) {
        playVoice(messageModel, isSpeakerphoneOn, 0)
    }

    /**
     * 播放语音
     *
     * @param messageModel
     */
    private fun playVoice(messageModel: MessageModel?, isSpeakerphoneOn: Boolean, seekTo: Int) {
        if (messageModel == null) {
            return
        }

        if (mLastPlayVoiceMessage != null) {
            stopPlayVoice()
        }

        mLastPlayVoiceMessage = messageModel.copyMessage()
        mAudioLoader.playVoice(activityRef, chatType, mLastPlayVoiceMessage!!, mLastPlayVoiceMessage!!.id.toString(), isSpeakerphoneOn, seekTo)

        mLastPlayVoiceMessage?.let {
            if (it.isSend == 0) {
                // 修改为已播放
                MessageController.sendMsgPlayedReceipt(chatType, it.senderId, it.id)
            }
        }
    }


    override fun onDownloadStart() {
        ThreadUtils.runOnUIThread {
            notifyItemChanged(mLastPlayVoiceMessage)
        }
    }

    override fun onDownloadProgress(totalBytes: Long, downloadedBytes: Long, progress: Int) {
        ThreadUtils.runOnUIThread {
            notifyItemChanged(mLastPlayVoiceMessage, progress)
        }
    }

    override fun onDownloadComplete() {

    }

    override fun onPrepare() {

    }

    override fun onStartPlay() {
        ThreadUtils.runOnUIThread {
            notifyItemChanged(mLastPlayVoiceMessage)
        }
    }

    override fun onTimerChange(time: Long) {
        ThreadUtils.runOnUIThread {
            notifyItemChanged(mLastPlayVoiceMessage)
        }
    }

    override fun onStopPlay() {
        val lastPlayVoiceMessage = mLastPlayVoiceMessage
        ThreadUtils.runOnUIThread {
            notifyItemChanged(lastPlayVoiceMessage)
        }
    }

    override fun onPlayComplete() {
        ThreadUtils.runOnUIThread {
            val messageModel = mLastPlayVoiceMessage
            mLastPlayVoiceMessage = null

            messageModel?.let {
                mVoiceMessageLastPlayList.remove(messageModel.id)

                notifyItemChanged(it)

                if (it.isSend == 0 && it.isReadedAttachment == 0) {
                    //寻找下一条未播放的语音自动播放
                    val myUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
                    val targetId = it.senderId
                    MessagesManager.executeChatTransactionAsync(chatType, myUid, targetId, { realm ->
                        val nextVoiceMessage = realm.where(MessageModel::class.java)
                                ?.equalTo("isSend", 0.toInt())
                                ?.and()?.equalTo("type", MessageModel.MESSAGE_TYPE_VOICE)
                                ?.and()?.equalTo("isReadedAttachment", 0.toInt())
                                ?.and()?.greaterThan("time", it.time)
                                ?.and()?.sort("time")?.findFirst()
                        nextVoiceMessage?.let {
                            playVoice(nextVoiceMessage)
                        }
                    })
                }
            }
        }
    }

    fun bindVoiceItem(helper: BaseViewHolder, messageModel: MessageModel) {
        updateVoiceItem(helper, messageModel)
    }

    private fun notifyItemChanged(msg: MessageModel?, progress: Int = 0) {
        msg?.let {
            val first = layoutManager.findFirstVisibleItemPosition()
            val last = layoutManager.findLastVisibleItemPosition()
            for (adapterPosition in last downTo first) {
                val dataPosition = adapterPosition - adapter.headerLayoutCount
                if (dataPosition >= 0) {
                    val data = adapter.data[dataPosition]
                    if (data.id == msg.id) {
                        if (data.type != MessageModel.MESSAGE_TYPE_VOICE) {
                            stopPlayVoice()
                        } else {
                            val holder = adapter.getBindedRecyclerView().findViewHolderForAdapterPosition(adapterPosition)
                            if (holder is BaseViewHolder) {
                                try {
                                    updateVoiceItem(holder, msg, progress)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun updateVoiceItem(helper: BaseViewHolder, messageModel: MessageModel, progress: Int = 0) {
        val voice = messageModel.voiceMessageContent
        val imgPlay = helper.getView<ImageView>(R.id.imgPlay)
        val imgPause = helper.getView<ImageView>(R.id.imgPause)
        val imgFail = helper.getView<ImageView>(R.id.imgFail)
        val progressBar = helper.getView<QMUIProgressBar>(R.id.progressBar)
        val txtCurrentTime = helper.getView<TextView>(R.id.txtCurrentTime)
        val seekBar = helper.getView<MusicSpectrumBar>(R.id.seekBar)
        val txtAllTime = helper.getView<TextView>(R.id.txtAllTime)

        var arr = messageModel.voiceMessageContent.localHighDArr
        if (arr == null || arr.isEmpty()) {
            arr = AudioSampleUtils.getDefaultSamples(messageModel.voiceMessageContent.recordTime)
            messageModel.voiceMessageContent.localHighDArr = arr
        }

        val samplesViewWidth = ScreenUtils.dp2px(BaseApp.app, AudioSampleUtils.getSamplesViewWidth(arr.size).toFloat())
        val params = seekBar.layoutParams
        params.width = samplesViewWidth
        seekBar.layoutParams = params

        if (voice != null) {
            when (messageModel.id.toString()) {
                mAudioLoader.loadingTag -> {
                    // 下载中
                    imgPlay.visibility = View.GONE
                    imgPause.visibility = View.GONE
                    imgFail.visibility = View.GONE
                    if (progress > 0) {
                        progressBar.visibility = View.VISIBLE
                        progressBar.maxValue = 100
                        progressBar.progress = progress
                    } else {
                        progressBar.visibility = View.INVISIBLE
                    }
                    txtCurrentTime.text = "00:00"

                    seekBar.setDatas(messageModel.id, arr)
                    seekBar.current = -1
                }
                mAudioPlayer.playingTag -> {
                    // 正在播放中
                    imgPause.setOnClickListener(unVoiceOnClickListener)
                    imgPause.tag = helper.adapterPosition
                    imgPlay.visibility = View.GONE
                    imgPause.visibility = View.VISIBLE
                    imgFail.visibility = View.GONE
                    progressBar.visibility = View.GONE

                    val totalPosition = mAudioPlayer.mediaPlayer?.duration ?: 100L
                    val currentPosition = mAudioPlayer.mediaPlayer?.currentPosition ?: 0L
                    txtCurrentTime.text = TimeUtils.timeFormatToMediaDuration(currentPosition)

                    seekBar.setDatas(messageModel.id, arr)
                    seekBar.current = try {
                        (100 * currentPosition / totalPosition).toInt()
                    } catch (e: Exception) {
                        -1
                    }

                    seekBar.setOnSeekBarChangeListener(OnSeekBarChangeListener(seekBar))
                    seekBar.tag = helper.adapterPosition
                }
                else -> {
                    // 未处于播放中
                    imgPlay.setOnClickListener(voiceOnClickListener)
                    imgPlay.tag = helper.adapterPosition
                    imgPlay.visibility = View.VISIBLE
                    imgPause.visibility = View.GONE
                    imgFail.visibility = View.GONE
                    progressBar.visibility = View.GONE

                    val lastProgress = mVoiceMessageLastPlayList[messageModel.id] ?: 0
                    if (lastProgress > 0) {
                        val seekTo = lastProgress / 100.0f
                        txtCurrentTime.text = TimeUtils.timeFormatToMediaDuration((seekTo * messageModel.voiceMessageContent.recordTime * 1000).toLong())

                        seekBar.setDatas(messageModel.id, arr)
                        seekBar.current = lastProgress

                        seekBar.setOnSeekBarChangeListener(OnSeekBarChangeListener(seekBar))
                        seekBar.tag = helper.adapterPosition
                    } else {
                        txtCurrentTime.text = "00:00"

                        seekBar.setDatas(messageModel.id, arr)
                        seekBar.current = -1

                        seekBar.setOnSeekBarChangeListener(OnSeekBarChangeListener(seekBar))
                        seekBar.tag = helper.adapterPosition
                    }
                }
            }

            txtAllTime.text = TimeUtils.timeFormatToMediaDuration(voice.recordTime * 1000L)
        } else {
            imgPlay.visibility = View.GONE
            imgPause.visibility = View.GONE
            imgFail.visibility = View.VISIBLE
            progressBar.visibility = View.GONE
            seekBar.visibility = View.VISIBLE
            txtAllTime.text = "00:00"
            txtCurrentTime.text = "00:00"
        }
    }


    /**
     * 播放语音按钮监听事件
     */
    private var voiceOnClickListener = View.OnClickListener { view ->
        val adapterPosition = view.tag as Int
        val dataPosition = adapterPosition - adapter.headerLayoutCount
        val msg = adapter.data.getOrNull(dataPosition)
        if (msg != null) {
            val helper = adapter.getBindedRecyclerView().findViewHolderForAdapterPosition(adapterPosition)
            if (helper != null && helper is BaseViewHolder) {
                val seekBar = helper.getView<MusicSpectrumBar>(R.id.seekBar)
                if (seekBar != null) {
                    val seekTo = seekBar.current / 100.0f
                    messageInputView?.stopPlay()
                    messageInputView?.saveToDraft()
                    playVoice(msg, isSpeakerphoneOn, (seekTo * (msg.voiceMessageContent.recordTime * 1000)).toInt())
                }
            }
        }
    }

    /**MentionEditText
     * 停止播放语音按钮监听事件Au
     */
    private var unVoiceOnClickListener = View.OnClickListener { view ->
        val adapterPosition = view.tag as Int
        val dataPosition = adapterPosition - adapter.headerLayoutCount
        val msg = adapter.data.getOrNull(dataPosition)
        if (msg != null) {
            val helper = adapter.getBindedRecyclerView().findViewHolderForAdapterPosition(adapterPosition) as BaseViewHolder
            val seekBar = helper.getView<MusicSpectrumBar>(R.id.seekBar)

            if (seekBar != null) {
                mVoiceMessageLastPlayList[msg.id] = seekBar.current
                stopPlayVoice(false)
            }
        }
    }

    private inner class OnSeekBarChangeListener(val seekBar: MusicSpectrumBar) : MusicSpectrumBar.OnSeekChangeListener {

        override fun onStopTrackingTouch() {
            val adapterPosition = seekBar.tag as Int
            val dataPosition = adapterPosition - adapter.headerLayoutCount
            val msg = adapter.data[dataPosition]

            val seekTo = seekBar.current / 100.0f
            val time = (msg?.voiceMessageContent?.recordTime ?: 0) * 1000L

            val parent = seekBar.parent as ViewGroup
            parent.findViewById<TextView>(R.id.txtCurrentTime).text = TimeUtils.timeFormatToMediaDuration((seekTo * time).toLong())

            if (msg.id == mLastPlayVoiceMessage?.id && mAudioPlayer.isPlaying) {
                mAudioPlayer.seekTo((seekTo * time).toInt())
            }
        }
    }
}