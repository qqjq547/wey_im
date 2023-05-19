package framework.telegram.message.ui.pvt

/**
 * Created by lzh on 20-1-7.
 * INFO:
 */
abstract class GroupSendMsg()

data class GroupSendMsgImage(val imageFilePath: String) : GroupSendMsg()

data class GroupSendMsgVoice(val recordTime: Int, val recordFilePath: String, val highDArr: Array<Int>) : GroupSendMsg()

data class GroupSendMsgDynamicImage(val emoticonId: Long, val imageFilePath: String) : GroupSendMsg()

data class GroupSendMsgDynamicUrlImage(val emoticonId: Long, val imageFilePath: String, val width: Int, val height: Int) : GroupSendMsg()

data class GroupSendMsgVideo(val videoFilePath: String) : GroupSendMsg()

data class GroupSendMsgFile(val filePath: String, val mimeType: String) : GroupSendMsg()
