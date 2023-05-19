package framework.telegram.business

import android.content.Context
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleOwner
import com.im.domain.pb.CommonProto
import com.im.domain.pb.SysProto
import com.trello.rxlifecycle3.android.ActivityEvent
import framework.telegram.business.http.HttpManager
import framework.telegram.business.http.creator.SysHttpReqCreator
import framework.telegram.business.http.getResult
import framework.telegram.business.http.protocol.LoginHttpProtocol
import framework.telegram.business.sp.CommonPref
import framework.telegram.support.BaseActivity
import framework.telegram.support.system.network.http.HttpReq
import framework.telegram.support.system.storage.sp.SharePreferencesStorage
import framework.telegram.support.tools.ExpandClass.toast
import framework.telegram.support.tools.download.DownloadContract
import framework.telegram.support.tools.download.DownloadPresenterImpl
import framework.telegram.ui.dialog.AppDialog
import io.reactivex.Observable
import java.util.*

/**
 * Created by yanggl on 2019/7/23 14:19
 */
class UpdatePresenterImpl : DownloadContract.View {
    private val mContext: Context
    private val mBaseActivity: BaseActivity
    private val mViewObservalbe: Observable<ActivityEvent>
    private var mDownImpl: DownloadPresenterImpl? = null
    private var mProgressBar: ProgressBar? = null
    private var mFirstDialog: AppDialog? = null
    private var mSecondDialog: AppDialog? = null

    constructor(view: BaseActivity, context: Context, observable: Observable<ActivityEvent>) {
        this.mBaseActivity = view
        this.mViewObservalbe = observable
        this.mContext = context
    }

    /**
     * @param showNotUpdateDialog 是否为用户主动点击
     */
    fun start(showCanUpDialog: Boolean, showNotUpdateDialog: Boolean) {
        saveCheckTime()
        HttpManager.getStore(LoginHttpProtocol::class.java)
                .checkVersion(object : HttpReq<SysProto.CheckVersionReq>() {
                    override fun getData(): SysProto.CheckVersionReq {
                        return SysHttpReqCreator.createCheckVersionReq()
                    }
                })
                .getResult(mViewObservalbe, {
                    //请求成功
                    showDialog(mBaseActivity, mBaseActivity, it, showCanUpDialog, showNotUpdateDialog)
                }, {
                    //请求失败
                    mContext.toast(it.message.toString())
                })
    }

    private fun showDialog(activity: AppCompatActivity, lifecycleOwner: LifecycleOwner, info: SysProto.CheckVersionResp, showCanUpDialog: Boolean, showNotUpdateDialog: Boolean) {
        when (info.flag) {
            CommonProto.CheckVersionFlag.CAN_UP -> {
                if (showCanUpDialog) {
                    // 根据参数决定是否显示给用户一个提示
                    showFirstDialog(lifecycleOwner, info, true)
                }
            }
            CommonProto.CheckVersionFlag.MUST_UP -> {
                // 必须显示
                showFirstDialog(lifecycleOwner, info, false)
            }
            CommonProto.CheckVersionFlag.NOT_UP -> {
                if (showNotUpdateDialog) {
                    // 根据参数决定是否显示给用户一个提示
                    AppDialog.show(activity, lifecycleOwner) {
                        message(text = context.getString(R.string.no_new_version_available))
                        negativeButton(text = context.getString(R.string.cancel))
                        title(text = context.getString(R.string.version_updating))
                    }
                }
            }
            else -> {

            }
        }
    }

    private fun showFirstDialog(lifecycleOwner: LifecycleOwner, info: SysProto.CheckVersionResp, isCanCancel: Boolean) {
        mFirstDialog = AppDialog.show(mContext, lifecycleOwner) {
            positiveButton(text = context.getString(R.string.update), click = {
                showDownloadDialog(lifecycleOwner, info.url, isCanCancel)
            })
            message(text = info.content)
            title(text = info.title)

            cancelOnTouchOutside(isCanCancel)
            cancelable(isCanCancel)
            if (isCanCancel) {
                negativeButton(text = context.getString(R.string.cancel))
            }

        }
    }

    private fun showDownloadDialog(lifecycleOwner: LifecycleOwner, url: String, isCanCancel: Boolean) {
        mSecondDialog = AppDialog.showProgressView(mContext, lifecycleOwner) {
            mDownImpl = DownloadPresenterImpl(url, this@UpdatePresenterImpl)
            cancelOnTouchOutside(false)
            title(text = context.getString(R.string.downloading_installation_package))

            val progressBar: ProgressBar = view.findViewById(R.id.normal_background_progress)
            downloadAndInstall(progressBar)

            if (isCanCancel) {
                negativeButton(text = context.getString(R.string.cancel)) {
                    mDownImpl?.onDestroy()
                }
            }
        }

    }


    private fun downloadAndInstall(progressBar: ProgressBar) {
        mDownImpl?.download()

        progressBar.max = 100
        mProgressBar = progressBar
    }

    fun cancel() {
        if (mDownImpl != null) {
            mDownImpl?.onDestroy()
        }

        mFirstDialog?.dismiss()
        mSecondDialog?.dismiss()
    }

    override fun downloadSucess(str: String) {
        mContext.toast(str)
    }

    override fun downloadError(str: String) {
        mContext.toast(str)
    }

    override fun setDownloadProgress(progress: Float) {
        mProgressBar?.progress = progress.toInt()
    }

    /**
     * 如果今天已经弹窗过，则返回false
     */
    fun needCheckVersion(): Boolean {
        val c: Calendar = Calendar.getInstance()
        val today: Int = c.get(Calendar.DAY_OF_MONTH)
        val t = SharePreferencesStorage.createStorageInstance(CommonPref::class.java).getCheckVersionsTime(-1)
        if (today == t) {
            return false
        }
        return true
    }

    /**
     * 保存更新弹窗时间
     */
    private fun saveCheckTime() {
        val c: Calendar = Calendar.getInstance()
        SharePreferencesStorage.createStorageInstance(CommonPref::class.java).putCheckVersionsTime(c.get(Calendar.DAY_OF_MONTH))
    }

}