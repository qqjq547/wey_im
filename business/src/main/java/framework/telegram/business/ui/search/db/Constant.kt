package framework.telegram.business.ui.search.db

/**
 * Created by lzh on 20-3-24.
 * INFO:
 */
object Constant{
    const val DATA_NAME ="search_7"
    const val DATA_TABLE ="search_table"
    const val DATA_VERSION =1

    const val ROW_CHAT_ID = "row_chat_id"
    const val ROW_CHAT_TYPE = "row_chat_type"
    const val ROW_CHAT_CONTENT = "row_chat_content"
    const val ROW_MSG_ID = "row_msg_id"
    const val ROW_MSG_LOCAL_ID = "row_msg_local_id"
    const val ROW_MSG_TIME = "row_msg_time"
    const val ROW_MSG_TYPE = "row_msg_type"
    const val ROW_INDEX_ID = "row_index_id"//indexId,群是负数，联系人是正式，用来区分 chatType 和 chatId
    const val ROW_SENDER_ID = "row_sender_id"//发送人id 用来给群用的，获取群成员头像，昵称

    const val ROW_TEST_NAME = "row_test_name"

}