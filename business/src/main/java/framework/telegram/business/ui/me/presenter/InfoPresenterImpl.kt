package framework.telegram.business.ui.me.presenter

import androidx.appcompat.app.AppCompatActivity
import com.im.domain.pb.LoginProto
import com.im.domain.pb.UserProto
import com.trello.rxlifecycle3.android.ActivityEvent
import framework.telegram.business.http.HttpManager
import framework.telegram.business.http.creator.LoginHttpReqCreator
import framework.telegram.business.http.creator.UserHttpReqCreator
import framework.telegram.business.http.getResult
import framework.telegram.business.http.protocol.LoginHttpProtocol
import framework.telegram.business.http.protocol.UserHttpProtocol
import framework.telegram.support.system.network.http.HttpReq
import io.reactivex.Observable

class InfoPresenterImpl : InfoContract.Presenter {


    companion object{
        /*
            跳转到系统联系人目录
         */
        const val GETSYSCONTACTS         = 0x00

        /*
            请求READ_CONTACTS权限
         */
        const val REQCONTACTSPERMISSION  = 0x01

    }

    private val mActivity : AppCompatActivity
    private val mView: InfoContract.View
    private val mViewObservable: Observable<ActivityEvent>

    constructor(view: InfoContract.View, activity: AppCompatActivity, observable: Observable<ActivityEvent>) {
        this.mView = view
        this.mViewObservable = observable
        this.mActivity = activity
        view.setPresenter(this)
    }

    override fun start() {

    }

    /**
     * 保存完善账户信息
     * @param info
     * @param opList
     * @param userParam 需要调用特殊方式来生成
     *
     */
    override fun savePerfectInfo(info: String, opList: List<UserProto.UserOperator>, userParam: UserProto.UserParam) {
        HttpManager.getStore(UserHttpProtocol::class.java)
                .updateUserInfo(object : HttpReq<UserProto.UpdateReq>() {
                    override fun getData(): UserProto.UpdateReq {
                        return UserHttpReqCreator.createUserUpdateReq(opList, userParam)
                    }
                })
                .getResult(mViewObservable, {
                    //请求成功
                    mView.savePerfectInfoSuccess(info)
                }, {
                    //请求失败
                    mView.showErrMsg(it.message)
                })
    }

    override fun logout() {
        HttpManager.getStore(LoginHttpProtocol::class.java)
                .logout(object : HttpReq<LoginProto.LogoutReq>() {
                    override fun getData(): LoginProto.LogoutReq {
                        return LoginHttpReqCreator.createLogoutReq()
                    }
                })
                .getResult(mViewObservable, {
                }, {})
    }
}