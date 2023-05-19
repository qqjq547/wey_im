package framework.telegram.message.controller

import framework.ideas.common.model.im.MessageModel
import framework.telegram.message.manager.MessagesManager
import framework.telegram.support.tools.ThreadUtils
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

object UploadAttachmentController {

    private val mListeners by lazy { CopyOnWriteArrayList<UploadAttachmentListener>() }

    private val mCancelUploadMsgs by lazy { ConcurrentHashMap<String, AtomicBoolean>() }

    fun attachUploadListener(listener: UploadAttachmentListener) {
        if (mListeners.contains(listener)) {
            return
        }

        mListeners.add(listener)
    }

    fun detachUploadListener(listener: UploadAttachmentListener) {
        mListeners.remove(listener)
    }

    fun cancelUpload(chatType: Int, myUid: Long, targetId: Long, msgLocalId: Long) {
        val cancelSignal = mCancelUploadMsgs.remove("${chatType}_${myUid}_${targetId}_$msgLocalId")
        cancelSignal?.set(true)

        // 标识上传失败
        setError(chatType, myUid, targetId, msgLocalId)

        // 重置UI
        notifyItemChangedWithCancel(chatType, targetId, msgLocalId)
    }

    fun cancelAllUpload() {
        mCancelUploadMsgs.forEach {
            it.value.set(true)
        }

        mCancelUploadMsgs.clear()

        // 查询所有消息状态，并进行状态重置
        MessagesManager.resetMessagesByReboot(
            parseUnKnowMessage = false,
            clearRedundantMessageReceipts = false
        )
    }

    fun saveCancelSignel(
        chatType: Int,
        myUid: Long,
        targetId: Long,
        msgLocalId: Long,
        cancelSignal: AtomicBoolean
    ) {
        mCancelUploadMsgs["${chatType}_${myUid}_${targetId}_$msgLocalId"] = cancelSignal
    }

    fun notifyItemChangedWithStart(chatType: Int, targetId: Long, msgLocalId: Long) {
        ThreadUtils.runOnUIThread {
            mListeners.forEach { uploadAttachmentListener ->
                uploadAttachmentListener.uploadStart(chatType, targetId, msgLocalId)
            }
        }
    }

    fun notifyItemChangedWithProgress(
        chatType: Int,
        targetId: Long,
        msgLocalId: Long,
        percent: Double,
        currentOffset: Long,
        totalLength: Long
    ) {
        ThreadUtils.runOnUIThread {
            mListeners.forEach { uploadAttachmentListener ->
                uploadAttachmentListener.uploadProgress(
                    chatType,
                    targetId,
                    msgLocalId,
                    percent,
                    currentOffset,
                    totalLength
                )
            }
        }
    }

    fun notifyItemChangedWithCancel(chatType: Int, targetId: Long, msgLocalId: Long) {
        ThreadUtils.runOnUIThread {
            mListeners.forEach { uploadAttachmentListener ->
                uploadAttachmentListener.uploadCancel(chatType, targetId, msgLocalId)
            }
        }
    }

    fun notifyItemChangedWithComplete(chatType: Int, targetId: Long, msgLocalId: Long) {
        ThreadUtils.runOnUIThread {
            mListeners.forEach { uploadAttachmentListener ->
                uploadAttachmentListener.uploadComplete(chatType, targetId, msgLocalId)
            }
        }
    }

    fun notifyItemChangedWithFail(chatType: Int, targetId: Long, msgLocalId: Long) {
        ThreadUtils.runOnUIThread {
            mListeners.forEach { uploadAttachmentListener ->
                uploadAttachmentListener.uploadFail(chatType, targetId, msgLocalId)
            }
        }
    }

    private fun setError(chatType: Int, myUid: Long, targetId: Long, msgLocalId: Long) {
        MessagesManager.executeChatTransactionAsync(chatType, myUid, targetId, { realm ->
            realm.where(MessageModel::class.java).equalTo("id", msgLocalId).findFirst()?.let {
                if (it.status == MessageModel.STATUS_ATTACHMENT_UPLOADING) {
                    it.status = MessageModel.STATUS_SEND_FAIL
                }
                realm.copyToRealmOrUpdate(it)
            }
        })
    }

    interface UploadAttachmentListener {
        fun uploadStart(chatType: Int, targetId: Long, msgLocalId: Long)

        fun uploadProgress(
            chatType: Int,
            targetId: Long,
            msgLocalId: Long,
            percent: Double,
            currentOffset: Long,
            totalLength: Long
        )

        fun uploadCancel(chatType: Int, targetId: Long, msgLocalId: Long)

        fun uploadComplete(chatType: Int, targetId: Long, msgLocalId: Long)

        fun uploadFail(chatType: Int, targetId: Long, msgLocalId: Long)
    }
}