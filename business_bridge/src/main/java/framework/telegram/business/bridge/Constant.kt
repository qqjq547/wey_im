package framework.telegram.business.bridge

import framework.ideas.common.model.common.TitleModel
import framework.ideas.common.model.contacts.ContactDataModel
import framework.ideas.common.model.group.GroupInfoModel
import framework.ideas.common.model.group.GroupMemberModel
import framework.telegram.business.bridge.bean.SelectedUsersModel
import framework.telegram.support.BuildConfig.JENKINS_IS_TEST_SERVER

object Constant {

    object Common {

        var LOGIN_HTTP_HOST =
           // if (JENKINS_IS_TEST_SERVER) "http://login.jargutech.com:8080" else "https://login.lamboim.live"
            if (JENKINS_IS_TEST_SERVER) "http://login.jargutech.com:8080" else "https://login.futeapi.com"

        var BIZ_HTTP_HOST: String = ""

        var BIZ_HTTP_CONTACT: String = ""

        var BIZ_HTTP_GROUP: String = ""

        var MAP_HTTP_HOST = ""

        var DOWNLOAD_HTTP_HOST = ""

    }

    object TmpData {
        var groupSendContactList = mutableListOf<SelectedUsersModel>()
    }

    object ARouter {
        const val ROUNTE_BUS_CLEAR_ACCOUNT: String = "/bus/clear_account"
        const val ROUNTE_BUS_CLEAR_STORAGE: String = "/bus/clear_storage"

        const val ROUNTE_SYSTEM_RLOG: String = "/system/rlog"

        const val ROUNTE_BUS_GROUP_ADMIN_PERMISSION: String = "/contacts/group_admin_permission"
        const val ROUNTE_BUS_GROUP_ADMINISTRATOR: String = "/contacts/group_administrator"
        const val ROUNTE_SERVICE_GROUP: String = "/bus/service/group"
        const val ROUNTE_SERVICE_CONTACT: String = "/bus/service/contact"
        const val ROUNTE_SERVICE_SETTING: String = "/bus/service/setting"
        const val ROUNTE_SERVICE_QR: String = "/bus/service/qr"
        const val ROUNTE_SERVICE_SYSTEM: String = "/bus/service/system"
        const val ROUNTE_SERVICE_SEARCH: String = "/bus/service/search"

        const val ROUNTE_SERVICE_ADAPTER_TITLE: String = "/bus/service/adapter_title"
        const val ROUNTE_SERVICE_ADAPTER_COUNTRY: String = "/bus/service/adapter_country"
        const val ROUNTE_SERVICE_ADAPTER_GROUP: String = "/bus/service/adapter_group"
        const val ROUNTE_SERVICE_ADAPTER_CONTACT: String = "/bus/service/adapter_contact"
        const val ROUNTE_SERVICE_ADAPTER_GROUP_MEMBER: String = "/bus/service/adapter_group_member"
        const val ROUNTE_SERVICE_ADAPTER_FORWARD_GROUP: String =
            "/bus/service/adapter_forward_group"
        const val ROUNTE_SERVICE_ADAPTER_FORWARD_CONTACT: String =
            "/bus/service/adapter_forward_contact"
        const val ROUNTE_SERVICE_ADAPTER_MERGE_CHAT: String = "/bus/service/adapter_merge_chat"

        //消息/通讯录 特殊处理的页面
        const val ROUNTE_SERVICE_ADAPTER_CONTACT_2: String = "/bus/service/adapter_contact_2"


        const val ROUNTE_BUS_LOGIN_SECOND: String = "/bus/login/login_second"
        const val ROUNTE_BUS_LOGIN_FIRST: String = "/bus/login/login_first"
        const val ROUNTE_BUS_LOGIN_REGISTER: String = "/bus/login/register"
        const val ROUNTE_BUS_LOGIN_PERFECT_INFO: String = "/bus/login/perfect_info"
        const val ROUNTE_BUS_LOGIN_SELECT_COUNTRY: String = "/bus/login/select_country"
        const val ROUNTE_BUS_LOGIN_GET_SMS_CODE: String = "/bus/login/get_sms_code"

        const val ROUNTE_BUS_JOIN_CONTACTS_REQ_LIST: String = "/contacts/new_friend"
        const val ROUNTE_BUS_CONTACTS_GROUP_CHAT: String = "/contacts/group_chat"
        const val ROUNTE_BUS_CONTACTS_ADD_FRIEND: String = "/contacts/add_friend"
        const val ROUNTE_BUS_CONTACTS_SEARCH_FRIEND: String = "/contacts/search_friend"
        const val ROUNTE_BUS_CONTACTS_SEARCH_FRIEND_LIST: String = "/contacts/search_friend_list"
        const val ROUNTE_BUS_CONTACTS_UN_AUDIT_FRIEND_DETAIL: String =
            "/contacts/un_audit_friend_detail"
        const val ROUNTE_BUS_CONTACT_DETAIL: String = "/contacts/friend_detail"
        const val ROUNTE_BUS_CONTACT_SETTING: String = "/contacts/friend_setting"
        const val ROUNTE_BUS_CONTACTS_PHONE_CONTACTS: String = "/contacts/phone_contacts"
        const val ROUNTE_BUS_CONTACTS_INFO_EDIT: String = "/contacts/me_info_edit"
        const val ROUNTE_BUS_CONTACTS_VERIFY_INFO_EDIT: String = "/contacts/verify_info"
        const val ROUNTE_BUS_CONTACTS_COMPLAINT: String = "/contacts/complaint"
        const val ROUNTE_BUS_CONTACTS_COMPLAINT_EDIT: String = "/contacts/complaint_edit"


        const val ROUNTE_BUS_ME_INFO: String = "/me/me_info"
        const val ROUNTE_BUS_ME_INFO_EDIT: String = "/me/me_info_edit"
        const val ROUNTE_BUS_ME_INFO_NOTICE: String = "/me/me_info_notice"
        const val ROUNTE_BUS_ME_INFO_PRIVACY: String = "/me/me_info_privacy"
        const val ROUNTE_BUS_ME_INFO_PRIVACY_DIS_SHOW_ONLINE: String =
            "/me/me_info_privacy_dis_show_online"
        const val ROUNTE_BUS_ME_INFO_PRIVACY_WAY: String = "/me/me_info_privacy_way"
        const val ROUNTE_BUS_ME_INFO_COMMON: String = "/me/me_info_common"
        const val ROUNTE_BUS_ME_INFO_CHAT: String = "/me/me_info_chat"
        const val ROUNTE_BUS_ME_INFO_SAVE: String = "/me/me_info_save"
        const val ROUNTE_BUS_ME_CLEAR_ACCOUNT: String = "/me/me_clear_account"
        const val ROUNTE_BUS_ME_BLACK_LIST: String = "/me/black_list"
        const val ROUNTE_BUS_ME_CHANGE_PHONE_FIRST: String = "/me/change_phone_first"
        const val ROUNTE_BUS_ME_CHANGE_PHONE_SECOND = "/me/change_phone_second"
        const val ROUNTE_BUS_ME_PASSWORD_SET_FIRST: String = "/me/password_set_first"
        const val ROUNTE_BUS_ME_PASSWORD_SET_SECOND = "/me/password_set_second"
        const val ROUNTE_BUS_ME_PASSWORD_CHANGE: String = "/me/password_change"
        const val ROUNTE_BUS_ME_ABOUT: String = "/me/me_about"
        const val ROUNTE_BUS_ME_FEEDBACK: String = "/me/me_feedback"
        const val ROUNTE_BUS_ME_UNVIEW_ONLINE_LIST: String = "/me/unview_online_list"
        const val ROUNTE_BUS_ME_PHONE_CONTACTS: String = "/me/me_phone_contacts"
        const val ROUNTE_BUS_ME_CONFIG_LANGUAGE: String = "/me/me_config_language"
        const val ROUNTE_BUS_ME_FONT_SIZE: String = "/me/me_font_size"
        const val ROUNTE_BUS_ME_FIND_PASSWORD_FIRST = "/me/find_password_first"
        const val ROUNTE_BUS_ME_FIND_PASSWORD_SECOND = "/me/find_password_second"
        const val ROUNTE_BUS_ME_APPLOCK_SETTING: String = "/me/me_info_applock_setting"
        const val ROUNTE_BUS_ME_APPLOCK_FORGET: String = "/me/applock_forget"

        const val ROUNTE_BUS_GROUP_MEMBER_ADD_OR_CREATE: String = "/group/member/addorcreate"
        const val ROUNTE_BUS_GROUP_MEMBER_ADD_OR_CREATE_FRAGMENT: String = "/group/member/fragment"
        const val ROUNTE_BUS_GROUP_MEMBER_ADD_OR_CREATE_SEARCH_FRAGMENT: String =
            "/group/member/search_fragment"
        const val ROUNTE_BUS_GROUP_MEMBER_OPERATE: String = "/group/member/operate"
        const val ROUNTE_BUS_GROUP_MEMBER_REMOVE: String = "/group/member/remove"
        const val ROUNTE_BUS_GROUP_MEMBER_REMOVE_FRAGMENT: String = "/group/member/remove_fragment"
        const val ROUNTE_BUS_GROUP_MEMBER_REMOVE_SEARCH_FRAGMENT: String =
            "/group/member/search_remove_fragment"
        const val ROUNTE_BUS_GROUP_SETTING: String = "/contacts/group_setting"
        const val ROUNTE_BUS_GROUP_MANAGE: String = "/contacts/group_manage"
        const val ROUNTE_BUS_GROUP_JOIN_REQ_LIST: String = "/contacts/group_join_req_list"
        const val ROUNTE_BUS_GROUP_UN_AUDIT_MEMBER_DETAIL: String =
            "/group/un_audit_group_member_detail"
        const val ROUNTE_BUS_GROUP_UN_AUDIT_INVITE_MEMBER_DETAIL: String =
            "/group/un_audit_invite_group_member_detail"
        const val ROUNTE_BUS_GROUP_JOIN: String = "/group/group_join"
        const val ROUNTE_BUS_GROUP_CREATE: String = "/group/group_create"
        const val ROUNTE_BUS_GROUP_EDIT_NAME: String = "/contacts/group_name_edit"
        const val ROUNTE_BUS_GROUP_INFO: String = "/contacts/group_info"
        const val ROUNTE_BUS_GROUP_NOTICE: String = "/contacts/group_notice"
        const val ROUNTE_BUS_GROUP_CREATE_NOTICE: String = "/contacts/group_create_notice"

        const val ROUNTE_BUS_QR_SCAN: String = "/qr/scan_qr"
        const val ROUNTE_BUS_QR_TEXT_SCAN: String = "/qr/scan_text_qr"
        const val ROUNTE_BUS_QR_SWEEP: String = "/qr/sweep_qr"
        const val ROUNTE_BUS_QR_GROUP_SWEEP: String = "/qr/group_sweep_qr"
        const val ROUNTE_BUS_QR_ANALYZE: String = "/qr/qr_analyze_activity"

        const val ROUNTE_BUS_SEARCH_CONTACT: String = "/search/search_contacts"
        const val ROUNTE_BUS_SEARCH_EXPAND: String = "/search/search_expand"
        const val ROUNTE_BUS_SEARCH_CHAT_EXPAND: String = "/search/search_chat_expand"
        const val ROUNTE_BUS_SEARCH_MULT_CHAT_EXPAND: String = "/search/search_mult_chat_expand"

        const val ROUNTE_BUS_SAME_GROUP_CHAT: String = "/bus/act/same/group_chat"

        const val ROUNTE_MSG_WEB_CONFIRM_LOGIN = "/bus/login/web_confirm_login"
        const val ROUNTE_MSG_WEB_ALREADY_LOGIN = "/bus/login/web_already_login"

        const val ROUNTE_BUS_TRANSMISSION_ASSISTANT_SETTING =
            "/bus/contacts/transmission_assistant_setting"

        const val ROUNTE_BUS_COMMON_LINK: String = "/bus/common/link"
    }

    object ARouter_Key {
        const val KEY_GROUP_NICKNAME: String = "key_group_nickname"
        const val KEY_TARGET_UID: String = "key_target_uid"
        const val KEY_TARGET_GID: String = "key_target_gid"
        const val KEY_ADD_TOKEN: String = "key_add_token"
        const val KEY_FIND_SIGN: String = "key_find_sign"
        const val KEY_TARGET_PHONE: String = "key_target_phone"
        const val KEY_ADD_FRIEND_FROM: String = "key_add_friend_from"
        const val KEY_QRCODE: String = "key_qrcode"
        const val KEY_IDCODE: String = "key_idcode"
        const val KEY_REQ_MSG: String = "key_req_msg"
        const val KEY_GROUP_ICON: String = "key_group_icon"
        const val KEY_GROUP_NAME: String = "key_group_name"
        const val KEY_USER_ICON: String = "key_user_icon"
        const val KEY_USER_NAME: String = "key_user_name"
        const val KEY_FROM_SOURCE: String = "key_from_source"
        const val KEY_RECORD_ID: String = "key_record_id"
        const val KEY_POSITION: String = "key_position"
        const val KEY_OPERATE: String = "key_operate"
        const val KEY_FROM: String = "key_from"
        const val KEY_GROUP_ADMIN: String = "key_group_admin"

        const val VALUE_FROM_GROUP = 1
    }

    object Result {
        val RESULT_SUCCESS = 200//成功
        val NO_PERMISSION = 100//无权限
        val SERVER_EXCEPTION = 500//服务器内部异常
        val ACCOUNT_NO_REGISTER = 4101//账号未注册
        val ACCOUNT_PASSWORD_ERROR = 4102//登陆密码错误
        val ACCOUNT_UNENABLE = 4103//账号不可用
        val ACCOUNT_DISABLE = 4103//账号不可用
        val MOBILE_NO_REGISTER = 4105//手机号未注册
        val SMS_VERIFY_ERROR = 4106//短信验证失败
        val ACCOUNT_RELOGIN = 4117//账号需要重新登录
        val ACCOUNT_VERIFYDODE_ERROR = 4119//登陆验证码错误
        val ACCOUNT_PASSWORD_ERROR_MORE = 4120//密码输入次数过多
        val ACCOUNT_TOKEN_ERROR = 4127//token过期
        val QR_CODE_ERROR = 1008//qr过期
        val ACCOUNT_BAN_SMS_CODE_LOGIN = 4128//该账号禁止短信验证码登录

        val ACCOUNT_LOGIN_DISABLE = 4130//由于您长时间未登录，请重新登录
        val ACCOUNT_IS_CANCEL = 4131//您的账号已注销

        val ACCOUNT_IS_BANNED = 4133//您的账号已被禁止登录
        val ILLEGAL_GROUP = 1021//该群聊因违反相关规定，已被限制使用
        val USER_IS_BANNED = 4132//对方账号已被限制登录，添加失败
    }

    object Search {
        const val SEARCH_TYPE = "search_type"
        const val SEARCH_TARGET_ID = "search_target_id"
        const val TARGET_PIC = "target_pic"
        const val TARGET_NAME = "target_name"
        const val FORWARD_IDS = "forwardids"
        const val INDEX_ID = "index_id"
        const val KEYWORD = "keyword"
        const val SEARCH_BACK = "search_back"
        const val SEARCH_MATCH_COUNT = "search_match_count"

        const val SEARCH_HEAD = 0
        const val SEARCH_COUNTRY = 1
        const val SEARCH_CONTACTS = 2
        const val SEARCH_CHAT = 3
        const val SEARCH_MY_GROUP = 4
        const val SEARCH_CALL = 5//通话
        const val SEARCH_NEW_CALL = 6//新建通话
        const val SEARCH_BLACK = 7//黑名单

        const val SEARCH_GROUP_ALL_MEMBER = 10//所有成员
        const val SEARCH_GROUP_AT_MEMBER = 11//at 群成员
        const val SEARCH_GROUP_TAN_OWNER = 12//改变群主
        const val SEARCH_GROUP_ADD_ADMIN = 13//增加管理员

        const val SEARCH_UN_VIEW_ONLINE = 20//永不可见

        const val SEARCH_ITEM_CONTACTS = ContactDataModel.CONTACT_ITEM_TYPE
        const val SEARCH_ITEM_GROUP = GroupInfoModel.GROUP_INFO_TYPE
        const val SEARCH_ITEM_GROUP_MEMBER = GroupMemberModel.GROUP_MEMBER_TYPE
        const val SEARCH_ITEM_TITLE = TitleModel.TITLE_HEAD

        const val SEARCH_CARD_CONTACTS = 16//


        const val SEARCH_FORWARD_CONTACTS_GROUP = 14//
        const val SEARCH_FORWARD_GROUP = 15//
        const val SEARCH_FORWARD_CONTACTS = 25//

        const val SEARCH_SHARE_CARD_CONTACTS = 17//
        const val SEARCH_SHARE_CARD_CONTACTS_GROUP = 18//

        const val SEARCH_SHARE_CONTACTS = 21//分享联系人
        const val SEARCH_SHARE_CONTACTS_GROUP = 22//分享联系人和群
        const val SEARCH_SHARE_GROUP = 23//分享群

        const val SEARCH_CONTACTS_EXPAND = 24
        const val SEARCH_GROUP_EXPAND = 25
        const val SEARCH_CHAT_EXPAND = 26 //todo 缺少

        const val SEARCH_CHAT_CONTENT_PRIVATE = 24//聊天内容(私密)
        const val SEARCH_CHAT_CONTENT = 8//私聊聊天内容
        const val SEARCH_GROUP_CHAT_CONTENT = 9//群聊天内容
    }

    object Bus {
        const val MAX_TEXT_NAME = 32
        const val MAX_TEXT_COUNT = 50
        const val MAX_FEEDBACK_TEXT_COUNT = 500
        const val MAX_TEXT_COUNT_2 = 200
    }

    object Protocol {
        const val PROTOCOL_QRCODE = "imQrCodeType=0"
        const val PROTOCOL_GROUP_CHAT_QRCODE = "imQrCodeType=1"
        const val PROTOCOL_WEB_LOGIN_QRCODE = "imQrCodeType=2"
        const val PROTOCOL_HTTP = "http"
        const val PROTOCOL_HTTPS = "https"
    }

    object Permission {
        const val BASIC_PERMISSION_REQUEST_CODE = 110
    }
}