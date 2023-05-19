package framework.telegram.message.bridge

import framework.ideas.common.model.common.SearchChatModel
import framework.ideas.common.model.contacts.ContactDataModel
import framework.ideas.common.model.im.MessageModel
import framework.ideas.common.model.im.StreamCallModel
import framework.telegram.support.BuildConfig

object Constant {

    object Common {
        var LOGIN_HTTP_HOST =
            //if (BuildConfig.JENKINS_IS_TEST_SERVER) "http://login.jargutech.com:8080" else "https://login.lamboim.live"
            if (BuildConfig.JENKINS_IS_TEST_SERVER) "http://login.jargutech.com:8080" else "https://login.futeapi.com"
        var BIZ_HTTP_HOST = ""

        var MAP_HTTP_HOST = ""

        var SYSTEM_USER_MAX_UID = 100001L

        var FILE_TRANSFER_UID = 10002L

        var FIRST_LOAD_MESSAGE_HISTORY_COUNT: Long = 200L

        var SHOW_RECEIPT_MAX_GROUP_MEMBER_COUNT: Int = 50

        var SHOW_RECEIPT_MAX_GROUP_MESSAGE_COUNT: Int = 1000

        var AUTO_CLEAR_MESSAGE_MAX_TIME: Int = 7 * 24 * 60 * 60 * 1000
    }

    object ARouter {
        const val ROUNTE_SERVICE_MESSAGE: String = "/msg/service/message"

        const val ROUNTE_MSG_PRIVATE_CHAT_ACTIVITY: String = "/msg/act/chat/pvt"
        const val ROUNTE_MSG_PRIVATE_SEND_CHAT_ACTIVITY: String = "/msg/act/chat/send_pvt"
        const val ROUNTE_MSG_GROUP_CHAT_ACTIVITY: String = "/msg/act/chat/group"
        const val ROUNTE_MSG_DETAIL_ACTIVITY: String = "/msg/act/chat/message_detail"

        const val ROUNTE_MSG_PREVIEW_BRIDGE_ACTIVITY: String = "/msg/act/preview_bridge/activity"
        const val ROUNTE_MSG_PREVIEW_PRIVATE_BRIDGE_ACTIVITY: String =
            "/msg/act/preview_private_bridge/activity"
        const val ROUNTE_MSG_PREVIEW_ACTIVITY: String = "/msg/act/preview/activity"
        const val ROUNTE_MSG_PREVIEW_PRIVATE_ACTIVITY: String = "/msg/act/preview_private/activity"
        const val ROUNTE_MSG_PREVIEW_GIF_ACTIVITY: String = "/msg/act/preview_gif/activity"
        const val ROUNTE_MSG_PREVIEW_IMAGE_FRAGMENT: String = "/msg/act/preview/image_fragment"
        const val ROUNTE_MSG_PREVIEW_FILE: String = "/msg/act/preview/file"
        const val ROUNTE_MSG_PREVIEW_TEXT: String = "/msg/act/preview/text"

        const val ROUNTE_MSG_PREVIEW_VIDEO_FRAGMENT: String = "/msg/act/preview/video_fragment"

        const val ROUNTE_MSG_STREAM_CALL_HISTORY_FRAGMENT: String = "/msg/fragment/stream/calls"
        const val ROUNTE_MSG_STREAM_CALL_HISTORY_FRAGMENT_ALL: String =
            "/msg/fragment/stream/calls/all"
        const val ROUNTE_MSG_STREAM_CALL_HISTORY_FRAGMENT_UNCALL: String =
            "/msg/fragment/stream/calls/uncall"

        const val ROUNTE_MSG_CHATS_FRAGMENT: String = "/msg/fragment/chats"

        const val ROUNTE_MSG_FORWARD_CHATS: String = "/msg/act/forward/chats"
        const val ROUNTE_MSG_FORWARD_SELECT_CONTACTS: String = "/msg/act/forward/select_contacts"
        const val ROUNTE_MSG_FORWARD_SELECT_GROUP = "/msg/act/forward/select_group"

        const val ROUNTE_MSG_CARD_CHATS: String = "/msg/act/card/chats"
        const val ROUNTE_MSG_CARD_SELECT_CONTACTS: String = "/msg/act/card/select_contacts"
        const val ROUNTE_MSG_CARD_SELECT_GROUP = "/msg/act/card/select_group"

        const val ROUNTE_MSG_STREAM_CALL_CONTACTS: String = "/msg/act/stream/contacts"

        const val ROUNTE_MSG_SELECT_CONTACT_CARD: String = "/msg/act/card/select"

        const val ROUNTE_MSG_STREAM_CALL_GO: String = "/msg/act/stream/go"
        const val ROUNTE_MSG_STREAM_CALL_DETAIL: String = "/msg/act/stream/detail"

        const val ROUNTE_SERVICE_ADAPTER_CALL: String = "/msg/service/adapter_call"
        const val ROUNTE_SERVICE_ADAPTER_NEW_CALL: String = "/msg/service/adapter_new_call"
        const val ROUNTE_SERVICE_ADAPTER_CHATS: String = "/msg/service/adapter_chats"
        const val ROUNTE_SERVICE_ADAPTER_NEW_CHATS: String = "/msg/service/adapter_new_chats"
        const val ROUNTE_SERVICE_ADAPTER_GROUP_CHATS: String = "/msg/service/adapter_group_chats"
        const val ROUNTE_MSG_AVATAR_PREVIEW: String = "/msg/act/preview/avatar"

        const val ROUNTE_LOCATION_CHOICE_ACTIVITY = "/msg/act/location/choice_activity"

        const val ROUNTE_LOCATION_SEARCH_ACTIVITY = "/msg/act/location/search_location_activity"

        const val ROUNTE_LOCATION_SHOW_ACTIVITY = "/msg/act/location/show_activity"

        const val ROUNTE_MSG_SHARE_CHATS: String = "/msg/act/share/chats"
        const val ROUNTE_MSG_SHARE_SELECT_CONTACTS: String = "/msg/act/share/select_contacts"
        const val ROUNTE_MSG_SHARE_SELECT_GROUP: String = "/msg/act/share/select_group"

        const val ROUNTE_MSG_MEDIA_MANAGER = "/msg/act/media_manager"
        const val ROUNTE_MSG_MEDIA_MANAGER_MEDIA = "/msg/act/media_manager/media"
        const val ROUNTE_MSG_MEDIA_MANAGER_FILE = "/msg/act/media_manager/file"

        const val ROUNTE_MSG_DYNAMIC_FACE_MANAGER: String = "/msg/act/face/manager"
    }

    object Push {
        enum class PUSH_TYPE {
            MSG_PVT,//私聊消息
            MSG_GROUP,//群聊消息
            MSG_GROUP_AT,//群聊@消息
            MSG_GROUP_AT_NOTICE,//群聊公告消息
            AUDIO_STREAM, //音频
            VIDEO_STREAM, //视频
            FRIEND,//好友通知
            INVITE_GROUP,//邀请加入群聊通知
            APPLY_GROUP,//申请加入群聊群通知
            CLEAR_ALL_NOTIFICATION,//清除所有本地通知
            CLEAR_VIDEO_NOTIFICATION,//清除一条视频通知
            CLEAR_AUDIO_NOTIFICATION//清除一条语音通知
        }
    }

    object Search {
        const val SEARCH_ITEM_STREAM_CALL = StreamCallModel.ITEM_STREAM_CALL
        const val CONTACT_ITEM_TYPE = ContactDataModel.CONTACT_ITEM_TYPE

        const val SEARCH_ITEM_CHATS_CONTENT1 = MessageModel.LOCAL_TYPE_MYSELF_TEXT
        const val SEARCH_ITEM_CHATS_CONTENT2 = MessageModel.LOCAL_TYPE_OTHER_TEXT

        const val SEARCH_ITEM_CHATS_LOCATION1 = MessageModel.LOCAL_TYPE_MYSELF_LOCATION
        const val SEARCH_ITEM_CHATS_LOCATION2 = MessageModel.LOCAL_TYPE_OTHER_LOCATION

        const val SEARCH_ITEM_CHATS_FILE1 = MessageModel.LOCAL_TYPE_MYSELF_FILE
        const val SEARCH_ITEM_CHATS_FILE2 = MessageModel.LOCAL_TYPE_OTHER_FILE

        const val SEARCH_ITEM_CHATS_FILE3 = SearchChatModel.SEARCH_CHAT
    }

    object TargetId {
        const val GROUP_SEND_ID = -10005L
    }

}