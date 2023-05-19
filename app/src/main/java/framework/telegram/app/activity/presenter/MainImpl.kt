package framework.telegram.app.activity.presenter

import android.content.Context
import com.im.domain.pb.SysProto
import com.im.domain.pb.UserProto
import com.trello.rxlifecycle3.android.ActivityEvent
import framework.telegram.business.bridge.bean.AccountInfo
import framework.telegram.business.http.HttpManager
import framework.telegram.business.http.creator.SysHttpReqCreator
import framework.telegram.business.http.creator.UserHttpReqCreator
import framework.telegram.business.http.getResult
import framework.telegram.business.http.protocol.SystemHttpProtocol
import framework.telegram.business.http.protocol.UserHttpProtocol
import framework.telegram.support.account.AccountManager
import framework.telegram.support.system.network.http.HttpReq
import framework.telegram.support.tools.BitUtils
import framework.telegram.support.tools.Helper
import io.reactivex.Observable

class MainImpl : MainContract.Presenter {

    private val mContext: Context
    private val mViewObservalbe: Observable<ActivityEvent>

    constructor(context: Context, observable: Observable<ActivityEvent>) {
        this.mViewObservalbe = observable
        this.mContext = context
    }

    override fun start() {

    }

    override fun savePerfectInfo(vibration: Boolean, sound: Boolean) {
        val accountInfo = AccountManager.getLoginAccount(AccountInfo::class.java)
        var privacy = accountInfo.getPrivacy()
        val setting = Helper.int2Bytes(privacy)

        setting[3] = BitUtils.setBitValue(setting[3], 4, if (vibration) 1 else 0)
        setting[3] = BitUtils.setBitValue(setting[3], 3, if (!sound) 1 else 0)
        privacy = Helper.bytes2Int(setting)
        val userParam = UserProto.UserParam.newBuilder().setPrivacy(privacy).build()
        val opList = listOf(UserProto.UserOperator.PRIVACY)
        HttpManager.getStore(UserHttpProtocol::class.java)
                .updateUserInfo(object : HttpReq<UserProto.UpdateReq>() {
                    override fun getData(): UserProto.UpdateReq {
                        return UserHttpReqCreator.createUserUpdateReq(opList, userParam)
                    }
                })
                .getResult(mViewObservalbe, {
                    //请求成功
                    accountInfo.putPrivacy(privacy)
                }, {
                    //请求失败
                })
    }


    override fun uploadOpenInstallData(params: String, type: Int, channelCode: String) {
        HttpManager.getStore(SystemHttpProtocol::class.java)
                .updateOpenInstall(object : HttpReq<SysProto.OpenInstallReq>() {
                    override fun getData(): SysProto.OpenInstallReq {
                        return SysHttpReqCreator.createUpdateOpenInstallReq(params, type, channelCode)
                    }
                })
                .getResult(mViewObservalbe, {
                    //请求成功
                }, {
                    //请求失败
                })
    }

    /**
     * 检查上次手动登录时间
     */
    override fun autoLoginForResult() {
    }
}