package framework.telegram.business.ui.qr

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.os.LocaleList
import android.text.TextUtils
import android.util.Log
import android.view.ViewConfiguration
import androidx.annotation.RequiresApi
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.im.domain.pb.ContactsProto
import com.im.domain.pb.GroupProto
import framework.telegram.business.ArouterServiceManager
import framework.telegram.business.R
import framework.telegram.business.bridge.Constant
import framework.telegram.business.bridge.Constant.ARouter_Key.KEY_ADD_TOKEN
import framework.telegram.business.bridge.Constant.ARouter_Key.KEY_TARGET_UID
import framework.telegram.business.http.HttpException
import framework.telegram.business.http.HttpManager
import framework.telegram.business.http.creator.GroupHttpReqCreator
import framework.telegram.business.http.getResult
import framework.telegram.business.http.protocol.GroupHttpProtocol
import framework.telegram.support.BaseApp
import framework.telegram.support.system.network.http.HttpReq
import framework.telegram.support.tools.DeviceInfo
import framework.telegram.support.tools.ExpandClass.toast
import framework.telegram.support.tools.language.LocalManageUtil
import framework.telegram.ui.dialog.AppDialog
import framework.telegram.ui.qr.activity.CaptureActivity
import framework.telegram.ui.qr.activity.CodeUtils

@Route(path = Constant.ARouter.ROUNTE_BUS_QR_SCAN)
class ScanQrActivity : CaptureActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }

    override fun implAnalyzeUrl(result: String) {
        analyzeUrl(result)

        ViewConfiguration.getLongPressTimeout()
    }

    override fun impFailAnalyzeUrl() {
        toast(BaseApp.app.getString(R.string.failed_to_parse_qr_code))
        finish()
    }

    private var dialog: AppDialog? = null
    private fun showLoading(isShow: Boolean) {
        dialog?.dismiss()
        if (isShow) {
            dialog = AppDialog.showLoadingView(this@ScanQrActivity, this@ScanQrActivity)
        }
    }

    private fun analyzeUrl(url: String) {
        when {
            url.contains(Constant.Protocol.PROTOCOL_QRCODE) -> {
                gotoContactDetail(url, {
                    showLoading(it)
                }, {
                    finish()
                }) {
                    toast(it)
                    finish()
                }
            }
            url.contains(Constant.Protocol.PROTOCOL_GROUP_CHAT_QRCODE) -> {
                gotoGroup(url, {
                    showLoading(it)
                }, {
                    finish()
                }) {
                    toast(it)
                    finish()
                }
            }
            url.contains(Constant.Protocol.PROTOCOL_WEB_LOGIN_QRCODE) -> {
                gotoWebLogin(url, this@ScanQrActivity, {
                    finish()
                }) {
                    toast(it)
                }
            }
            url.contains(Constant.Protocol.PROTOCOL_HTTP) -> {
                gotoQrText(url, "") {
                    finish()
                }
            }
            url.contains(Constant.Protocol.PROTOCOL_HTTPS) -> {
                gotoQrText(url, "") {
                    finish()
                }
            }
            else -> {
                gotoQrText("", url) {
                    finish()
                }
            }
        }
    }

    companion object {
        fun analyzeUrl(
            url: String,
            context: Context?,
            loading: ((Boolean) -> Unit),
            complete: (() -> Unit),
            error: (String) -> Unit
        ) {
            when {
                url.contains(Constant.Protocol.PROTOCOL_QRCODE) -> {
                    gotoContactDetail(url, loading, complete, error)
                }
                url.contains(Constant.Protocol.PROTOCOL_GROUP_CHAT_QRCODE) -> {
                    gotoGroup(url, loading, complete, error)
                }
                url.contains(Constant.Protocol.PROTOCOL_WEB_LOGIN_QRCODE) -> {
                    gotoWebLogin(url, context, complete, error)
                }
                url.contains(Constant.Protocol.PROTOCOL_HTTP) -> {
                    gotoQrText(url, "", complete)
                }
                url.contains(Constant.Protocol.PROTOCOL_HTTPS) -> {
                    gotoQrText(url, "", complete)
                }
                else -> {
                    gotoQrText("", url, complete)
                }
            }
        }

        private fun gotoContactDetail(
            url: String,
            loading: ((Boolean) -> Unit),
            complete: (() -> Unit),
            error: (String) -> Unit
        ) {
            try {
                val hashMap = parse(url)
                var uid = 0L
                var qrCode = ""
                hashMap.forEach {
                    when (it.key) {
                        "uid" -> {
                            uid = it.value.toLong()
                        }
                        "qrCode" -> {
                            qrCode = it.value
                        }

                    }
                }
                if (!TextUtils.isEmpty(qrCode)) {
                    loading.invoke(true)
                    ArouterServiceManager.contactService.getContactInfoFromQr(
                        null,
                        uid,
                        qrCode,
                        { contactInfo, addToken ->
                            loading.invoke(false)
                            ARouter.getInstance().build(Constant.ARouter.ROUNTE_BUS_CONTACT_DETAIL)
                                .withLong(KEY_TARGET_UID, contactInfo.uid)
                                .withString(KEY_ADD_TOKEN, addToken)
                                .withSerializable(
                                    Constant.ARouter_Key.KEY_ADD_FRIEND_FROM,
                                    ContactsProto.ContactsAddType.CODE
                                )
                                .navigation()
                            complete.invoke()
                        },
                        {
                            loading.invoke(false)
                            error.invoke(BaseApp.app.getString(R.string.failed_to_parse_qr_code))
                        })
                } else if (uid > 0L) {
                    ARouter.getInstance().build(Constant.ARouter.ROUNTE_BUS_CONTACT_DETAIL)
                        .withSerializable(
                            Constant.ARouter_Key.KEY_ADD_FRIEND_FROM,
                            ContactsProto.ContactsAddType.CODE
                        )
                        .withLong(KEY_TARGET_UID, uid).navigation()
                    complete.invoke()
                } else {
                    error.invoke(BaseApp.app.getString(R.string.failed_to_parse_qr_code))
                }
            } catch (e: Exception) {
                error.invoke(BaseApp.app.getString(R.string.failed_to_parse_qr_code))
            }
        }

        private fun gotoGroup(
            url: String,
            loading: ((Boolean) -> Unit),
            complete: (() -> Unit),
            error: (String) -> Unit
        ) {
            try {
                val hashMap = parse(url)
                var groupId = 0L
                var qrCode = ""
                var idCode = ""
                hashMap.forEach {
                    when (it.key) {
                        "groupId" -> {
                            groupId = it.value.toLong()
                        }
                        "qrCode" -> {
                            qrCode = it.value
                        }
                        "IdCode" -> {
                            idCode = it.value
                        }
                    }
                }
                if (!TextUtils.isEmpty(qrCode)) {
                    loading.invoke(true)
                    HttpManager.getStore(GroupHttpProtocol::class.java)
                        .groupDetailFromQrCode(object :
                            HttpReq<GroupProto.GroupDetailFromQrCodeReq>() {
                            override fun getData(): GroupProto.GroupDetailFromQrCodeReq {
                                return GroupHttpReqCreator.createGroupDetailFromQrCodeReq(
                                    groupId,
                                    qrCode,
                                    idCode
                                )
                            }
                        })
                        .getResult(null, { groupDetail ->

                            if (groupDetail.bfMember) {
                                if (groupDetail.groupBase.bfBanned) {
                                    loading.invoke(false)
                                    ARouter.getInstance()
                                        .build(Constant.ARouter.ROUNTE_BUS_GROUP_JOIN)
                                        .withLong(
                                            Constant.ARouter_Key.KEY_TARGET_GID,
                                            groupDetail.groupBase.groupId
                                        )
                                        .withString(Constant.ARouter_Key.KEY_QRCODE, qrCode)
                                        .withString(Constant.ARouter_Key.KEY_IDCODE, idCode)
                                        .withString(KEY_ADD_TOKEN, groupDetail.addToken)
                                        .navigation()
                                    complete.invoke()
                                } else {
                                    loading.invoke(false)
                                    ARouter.getInstance()
                                        .build(framework.telegram.message.bridge.Constant.ARouter.ROUNTE_MSG_GROUP_CHAT_ACTIVITY)
                                        .withLong("targetGid", groupDetail.groupBase.groupId)
                                        .withString(KEY_ADD_TOKEN, groupDetail.addToken)
                                        .navigation()
                                    complete.invoke()
                                }
                            } else {
                                loading.invoke(false)
                                ARouter.getInstance().build(Constant.ARouter.ROUNTE_BUS_GROUP_JOIN)
                                    .withLong(
                                        Constant.ARouter_Key.KEY_TARGET_GID,
                                        groupDetail.groupBase.groupId
                                    )
                                    .withString(Constant.ARouter_Key.KEY_QRCODE, qrCode)
                                    .withString(Constant.ARouter_Key.KEY_IDCODE, idCode)
                                    .withString(KEY_ADD_TOKEN, groupDetail.addToken)
                                    .navigation()
                                complete.invoke()
                            }
                        }, {
                            loading.invoke(false)
                            error.invoke(it.message.toString())
                        })

                } else {
                    error.invoke(BaseApp.app.getString(R.string.failed_to_parse_qr_code))
                }
            } catch (e: Exception) {
                error.invoke(BaseApp.app.getString(R.string.failed_to_parse_qr_code))
            }
        }

        private fun gotoWebLogin(
            url: String,
            context: Context?,
            complete: (() -> Unit),
            error: (String) -> Unit
        ) {
            try {
                val hashMap = parse(url)
                var token = ""
                hashMap.forEach {
                    when (it.key) {
                        "token" -> {
                            token = it.value
                        }
                    }
                }

                if (!TextUtils.isEmpty(token)) {
                    ARouter.getInstance().build(Constant.ARouter.ROUNTE_MSG_WEB_CONFIRM_LOGIN)
                        .withTransition(R.anim.anim_up_in, 0)
                        .withString(Constant.ARouter_Key.KEY_QRCODE, token).navigation(context)
                    complete.invoke()
                } else {
                    error.invoke(BaseApp.app.getString(R.string.failed_to_parse_qr_code))
                }
            } catch (e: Exception) {
                error.invoke(BaseApp.app.getString(R.string.failed_to_parse_qr_code))
            }
        }

        private fun gotoQrText(url: String, text: String, complete: (() -> Unit)) {
            ARouter.getInstance().build(Constant.ARouter.ROUNTE_BUS_QR_TEXT_SCAN)
                .withString("text", text)
                .withString("html", url).navigation()
            complete.invoke()
        }

        private fun parse(link: String): HashMap<String, String> {
            val dataMap = HashMap<String, String>()
            if (link.contains("?")) {
                val array = link.split("?")
                if (array.size == 2) {
                    val dataList = array[1]
                    if (dataList.contains("&")) {
                        val keyAndValue = dataList.split("&")
                        keyAndValue.forEach {
                            parse2(it, dataMap)
                        }
                    } else {
                        parse2(dataList, dataMap)
                    }
                }
            } else {
                if (link.contains("&")) {
                    val keyAndValue = link.split("&")
                    keyAndValue.forEach {
                        parse2(it, dataMap)
                    }
                } else {
                    parse2(link, dataMap)
                }
            }
            return dataMap
        }

        private fun parse2(link: String, map: HashMap<String, String>) {
            if (link.contains("=")) {
                val keyAndValue = link.split("=")
                if (keyAndValue.size == 2) {
                    map[keyAndValue[0]] = keyAndValue[1]
                }
            }
        }
    }
}