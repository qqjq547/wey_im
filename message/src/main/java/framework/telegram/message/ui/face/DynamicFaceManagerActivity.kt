package framework.telegram.message.ui.face

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.trello.rxlifecycle3.android.lifecycle.kotlin.bindToLifecycle
import java.io.File
import framework.telegram.message.R
import framework.telegram.message.base.BaseMessageActivity
import framework.telegram.message.bridge.Constant
import framework.telegram.message.event.DynamicFaceUpdateEvent
import framework.telegram.support.mvp.BasePresenter
import framework.telegram.support.system.event.EventBus
import framework.telegram.support.tools.ExpandClass.toast
import framework.telegram.ui.dialog.AppDialog
import framework.telegram.ui.face.dynamic.DynamicFaceBean
import framework.telegram.ui.imagepicker.ImagePicker
import framework.telegram.ui.imagepicker.MimeType
import framework.telegram.ui.imagepicker.engine.impl.GlideEngine
import framework.telegram.ui.utils.BitmapUtils
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.msg_dynamic_manager_activity.*
import kotlinx.android.synthetic.main.msg_dynamic_manager_activity.custom_toolbar
import kotlinx.android.synthetic.main.msg_dynamic_manager_activity.layout_check_msg
import java.util.*

/**
 * Created by hyf on 20-1-4.
 */
@Route(path = Constant.ARouter.ROUNTE_MSG_DYNAMIC_FACE_MANAGER)
class DynamicFaceManagerActivity : BaseMessageActivity<DynamicFaceManagerContract.Presenter>(), DynamicFaceManagerContract.View {

    companion object {
        internal const val TOOL_IMAGEPICKER_APPEND_DYNAMIC_FACE_REQUESTCODE = 0x4000
    }

    private var mTilteTextView: TextView? = null
    private var mEditButton: TextView? = null

    private val mAdapter by lazy {
        DynamicFaceManagerAdapter(object : DynamicFaceManagerAdapter.SizeCallback {
            override fun getWidth(): Int {
                return common_recycler.measuredWidth / 4
            }

            override fun getHeight(): Int {
                return common_recycler.measuredWidth / 4
            }

        }) { face ->
            if (face.id == -1L) {
                // 添加表情
                ImagePicker.from(this@DynamicFaceManagerActivity)
                        .choose(EnumSet.of(MimeType.JPEG, MimeType.PNG, MimeType.GIF))
                        .countable(false)
                        .maxSelectable(1)
                        .thumbnailScale(0.85f)
                        .originalEnable(false)
                        .showSingleMediaType(true)
                        .imageEngine(GlideEngine())
                        .forResult(TOOL_IMAGEPICKER_APPEND_DYNAMIC_FACE_REQUESTCODE)
            } else {
                ARouter.getInstance().build(Constant.ARouter.ROUNTE_MSG_PREVIEW_GIF_ACTIVITY)
                        .withString("imageFileUri", face.path)
                        .navigation()
            }
        }
    }

    override fun getLayoutId() = R.layout.msg_dynamic_manager_activity

    override fun initView() {
        custom_toolbar.setBackIcon(R.drawable.common_icon_black_back) {
            finish()
        }

        custom_toolbar.showRightTextView(getString(R.string.edit), onClickCallback = {
            setCheckableMessage()
        }) {
            mEditButton = it
            mEditButton?.visibility = View.GONE
        }

        custom_toolbar.showCenterTitle(getString(R.string.string_dynamic_face_manager_title)) {
            mTilteTextView = it
        }
    }

    @SuppressLint("CheckResult")
    override fun initListen() {
        common_recycler.initSingleTypeRecycleView(GridLayoutManager(this, 4), mAdapter, false)

        common_recycler.refreshController().setOnRefreshListener {
            mPresenter?.start()
        }

        common_recycler.emptyController().setEmpty()

        common_recycler.refreshController().setEnablePullToRefresh(false)

        EventBus.getFlowable(DynamicFaceUpdateEvent::class.java)
                .bindToLifecycle(this@DynamicFaceManagerActivity)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    mPresenter?.loadData(false)
                }
    }

    @SuppressLint("CheckResult")
    override fun initData() {
        DynamicFaceManagerPresenterImpl(this, this, lifecycle()).start()
    }

    override fun showLoading() {
        dialog?.dismiss()
        dialog = AppDialog.showLoadingView(this@DynamicFaceManagerActivity, this@DynamicFaceManagerActivity)
    }

    override fun dismissLoading() {
        dialog?.dismiss()
    }

    override fun showEmpty() {
        dialog?.dismiss()
    }

    override fun setPresenter(presenter: BasePresenter) {
        mPresenter = presenter as DynamicFaceManagerContract.Presenter
    }

    override fun isActive(): Boolean {
        return true
    }

    override fun refreshListUI(list: List<DynamicFaceBean>) {
        dialog?.dismiss()

        common_recycler.itemController().setNewData(list)
        common_recycler.refreshController().refreshComplete()

        if (list.isNotEmpty()) {
            mEditButton?.visibility = View.VISIBLE
            mTilteTextView?.text = "${getString(R.string.string_dynamic_face_manager_title)}(${mAdapter.itemCount - 1})"
        } else {
            mTilteTextView?.text = "${getString(R.string.string_dynamic_face_manager_title)}(0)"
        }
    }

    override fun showError(errStr: String?) {
        dialog?.dismiss()
        toast(errStr.toString())
    }

    override fun showAddEmoticonComplete() {
        toast(getString(R.string.successfully_added))
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == TOOL_IMAGEPICKER_APPEND_DYNAMIC_FACE_REQUESTCODE && resultCode == Activity.RESULT_OK) {
            val paths = ImagePicker.obtainInfoResult(data)
            if (paths != null && paths.isNotEmpty()) {
                paths.first()?.let { mediaInfo ->
                    ARouter.getInstance().build(Constant.ARouter.ROUNTE_MSG_PREVIEW_GIF_ACTIVITY)
                            .withString("imageFileBackupUri", Uri.fromFile(File(mediaInfo.path)).toString())
                            .navigation()
                }
            }
        }
    }

    private fun dismissCheckMessages() {
        mAdapter.setUnCheckable()
        layout_check_msg.visibility = View.GONE
    }

    private fun setCheckableMessage() {
        text_view_cancel_check_msg.setOnClickListener {
            dismissCheckMessages()
        }

        image_view_delete_check_msg.setOnClickListener {
            AppDialog.show(this@DynamicFaceManagerActivity) {
                title(text = getString(R.string.hint))
                message(text = getString(R.string.string_delete_dynamic_face_tip))
                positiveButton(text = getString(R.string.confirm)) {
                    mPresenter?.delEmoticons(mAdapter.getCheckableMessages())

                    dismissCheckMessages()
                    mPresenter?.loadData()
                }
            }
        }

        layout_check_msg.visibility = View.VISIBLE
        mAdapter.setCheckable { msgCount ->
            text_view_check_msg_title.text = "$msgCount"

            if (msgCount == 0) {
                image_view_delete_check_msg.isEnabled = false
                image_view_delete_check_msg.isClickable = false
                image_view_delete_check_msg.setImageResource(R.drawable.msg_icon_delete_disable)
            } else {
                image_view_delete_check_msg.isEnabled = true
                image_view_delete_check_msg.isClickable = true
                image_view_delete_check_msg.setImageResource(R.drawable.msg_icon_delete)
            }
        }
    }

    override fun onBackPressed() {
        if (layout_check_msg.visibility == View.VISIBLE) {
            dismissCheckMessages()
            return
        }

        super.onBackPressed()
    }
}