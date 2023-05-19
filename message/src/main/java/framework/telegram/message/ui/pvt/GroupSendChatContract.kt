package framework.telegram.message.ui.pvt

import framework.telegram.support.mvp.BasePresenter
import framework.telegram.support.mvp.BaseView

interface GroupSendChatContract {

    interface Presenter : BasePresenter {

        fun sendTextMessage(msg: String)

        fun sendVoiceMessage(recordTime: Int, recordFilePath: String, highDArr: Array<Int>)

        fun sendImageMessage(imageFilePath: String)

        fun sendDynamicImageMessage(emoticonId: Long, imageFilePath: String)

        fun sendDynamicImageUrlMessage(emoticonId: Long, imageFileUrl: String, width: Int, height: Int)

        fun sendVideoMessage(videoFilePath: String)

        fun sendNameCardMessage(uid: Long)

        fun sendLocationMessage(lat: Long, lng: Long, address: String)

        fun sendFileMessage(filePath: String, mimeType: String)

        fun destroy()
    }

    interface View : BaseView<Presenter> {
        fun showError(str: String,data: GroupSendMsg)

        fun showProcess(process: Int, size: Int)
    }
}