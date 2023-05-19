package framework.telegram.business.ui.me.presenter

import android.content.Context
import android.util.Log
import com.im.domain.pb.CommonProto
import com.im.domain.pb.UserProto
import com.trello.rxlifecycle3.android.ActivityEvent
import framework.telegram.business.bridge.bean.AccountInfo
import framework.telegram.business.http.HttpManager
import framework.telegram.business.http.creator.UserHttpReqCreator
import framework.telegram.business.http.getResult
import framework.telegram.business.http.protocol.UserHttpProtocol
import framework.telegram.support.account.AccountManager
import framework.telegram.support.system.network.http.HttpReq
import framework.telegram.support.tools.BitUtils
import framework.telegram.support.tools.Helper
import io.reactivex.Observable

class PrivacyPresenterImpl : PrivacyContract.Presenter {

    private val mContext: Context
    private val mView: PrivacyContract.View
    private val mViewObservalbe: Observable<ActivityEvent>

    constructor(view: PrivacyContract.View, context: Context, observable: Observable<ActivityEvent>) {
        this.mView = view
        this.mViewObservalbe = observable
        this.mContext = context
        view.setPresenter(this)
    }

    override fun start() {

    }

    /**
     * 保存用户布尔值信息
     */
    override fun savePerfectInfo(index: Int, pos: Int, value: Boolean,callBack:(()->Unit)?) {
        val accountInfo = AccountManager.getLoginAccount(AccountInfo::class.java)
        var privacy = accountInfo.getPrivacy()
        privacy = BitUtils.setBitValue(privacy,index,pos,value)
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
                    callBack?.invoke()
                    mView.savePerfectInfoSuccess(index, pos, value)
                }, {
                    //请求失败
                    mView.showErrMsg(it.message)
                })
    }

    override fun saveClearAccountTime(index: Int, callBack: ((Int) -> Unit)?) {
        val value = when(index){
            1->{
                CommonProto.ClearTimeType.ONE_MONTH
            }
            2->{
                  CommonProto.ClearTimeType.THIRD_MONTH
            }
            3->{
                  CommonProto.ClearTimeType.TWELVE_MONTH
            }else->{
                CommonProto.ClearTimeType.SIX_MONTH
            }
        }
        val userParam = UserProto.UserParam.newBuilder().setClearTime(value).build()
        val opList = listOf(UserProto.UserOperator.CLEAR_TIME)
        HttpManager.getStore(UserHttpProtocol::class.java)
                .updateUserInfo(object : HttpReq<UserProto.UpdateReq>() {
                    override fun getData(): UserProto.UpdateReq {
                        return UserHttpReqCreator.createUserUpdateReq(opList, userParam)
                    }
                })
                .getResult(mViewObservalbe, {
                    //请求成功
                    callBack?.invoke(index)
                }, {
                    //请求失败
                    mView.showErrMsg(it.message)
                })
    }

}