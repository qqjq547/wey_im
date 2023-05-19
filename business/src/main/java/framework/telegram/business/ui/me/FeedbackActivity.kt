package framework.telegram.business.ui.me

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.text.Editable
import android.text.InputFilter
import android.text.TextUtils
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatTextView
import com.alibaba.android.arouter.facade.annotation.Route
import com.im.domain.pb.CommonProto
import framework.ideas.common.rlog.RLogManager
import framework.telegram.business.R
import framework.telegram.business.bridge.Constant
import framework.telegram.business.manager.UploadManager
import framework.telegram.business.ui.base.BaseBusinessActivity
import framework.telegram.business.ui.me.presenter.FeedbackContract
import framework.telegram.business.ui.me.presenter.FeedbackPresenterImpl
import framework.telegram.support.account.AccountManager
import framework.telegram.support.mvp.BasePresenter
import framework.telegram.support.tools.ExpandClass.getSimpleColor
import framework.telegram.support.tools.ExpandClass.getSimpleDrawable
import framework.telegram.support.tools.ExpandClass.toast
import framework.telegram.support.tools.ThreadUtils
import framework.telegram.support.tools.file.DirManager
import framework.telegram.ui.dialog.AppDialog
import framework.telegram.ui.image.AppImageView
import framework.telegram.ui.utils.KeyboardktUtils
import framework.telegram.ui.utils.ScreenUtils
import kotlinx.android.synthetic.main.bus_me_activity_feedback.*
import java.io.File

@Route(path = Constant.ARouter.ROUNTE_BUS_ME_FEEDBACK)
class FeedbackActivity : BaseBusinessActivity<FeedbackContract.Presenter>(), FeedbackContract.View {

    private var mType = 0 // 0:建议  1:错误  2:其他

    override fun getLayoutId() = R.layout.bus_me_activity_feedback

    override fun initView() {
        custom_toolbar.setBackIcon(R.drawable.common_icon_black_back) {
            finish()
        }
        custom_toolbar.showCenterTitle(getString(R.string.feedback))

        app_text_view_suggest.setOnClickListener {
            mType = 0
            changeButton(mType)
        }

        app_text_view_error.setOnClickListener {
            mType = 1
            changeButton(mType)
        }

        app_text_view_other.setOnClickListener {
            mType = 2
            changeButton(mType)
        }

        text_view_edit_text.filters =
            arrayOf<InputFilter>(InputFilter.LengthFilter(Constant.Bus.MAX_FEEDBACK_TEXT_COUNT))
        text_view_edit_text.text = (Constant.Bus.MAX_FEEDBACK_TEXT_COUNT).toString()
        changeButton(mType)

        //获取焦点并弹出软键盘
        edit_text_context.isFocusable = true
        edit_text_context.isFocusableInTouchMode = true
        edit_text_context.requestFocus()
        Handler().postDelayed({
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS)
        }, 100)

        addPickPicture()
    }

    override fun initListen() {
        edit_text_context.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                val str = s.toString()
                text_view_edit_text.text =
                    (Constant.Bus.MAX_FEEDBACK_TEXT_COUNT - str.length).toString()
                setLoginBtn(str.isNotEmpty())
            }
        })

        frame_layout_op.setOnClickListener {
            if (TextUtils.isEmpty(edit_text_context.text)) {
                toast(getString(R.string.please_fill_in_the_feedback))
            } else {
                val str = edit_text_context.text.toString()
                val imageList = mutableListOf<String>()
                for (index in 0 until pick_layout.childCount) {
                    val childView = pick_layout.getChildAt(index)
                    val tag = childView.getTag(R.id.image_url)
                    tag?.let {
                        val url = it.toString()
                        if (!TextUtils.isEmpty(url)) {
                            imageList.add(url)
                        }
                    }
                }

                if (imageList.size > 0) {
                    uploadPicture(imageList[0], 0, imageList, mutableListOf(), {
                        var pictureStr = ""
                        it.forEach { picture ->
                            pictureStr = "$picture,"
                        }
                        if (pictureStr.endsWith(",")) {
                            pictureStr = pictureStr.substring(0, pictureStr.length - 1)
                        }

                        uploadLogFile { url ->
                            if (TextUtils.isEmpty(url)) {
                                mPresenter?.setFeedback(mType, str, pictureStr)
                            } else {
                                mPresenter?.setFeedback(mType, "$str---logFile:${url}", pictureStr)
                            }
                        }
                    }, {
                        showErrMsg(it)
                    })
                } else {
                    uploadLogFile { url ->
                        if (TextUtils.isEmpty(url)) {
                            mPresenter?.setFeedback(mType, str, "")
                        } else {
                            mPresenter?.setFeedback(mType, "$str---logFile:${url}", "")
                        }
                    }
                }
            }
        }

        all_layout.setOnClickListener {
            KeyboardktUtils.hideKeyboard(this.all_layout)
        }
    }

    private fun uploadPicture(
        uri: String,
        index: Int,
        imageList: MutableList<String>,
        resultImageList: MutableList<String>,
        complete: (MutableList<String>) -> Unit,
        error: (String) -> Unit
    ) {
        UploadManager.uploadFile(
            this@FeedbackActivity,
            uri,
            CommonProto.AttachType.PIC,
            CommonProto.AttachWorkSpaceType.COMMON,
            {
                resultImageList.add(it)
                if (index + 1 >= imageList.size) {
                    complete.invoke(resultImageList)
                } else {
                    uploadPicture(
                        imageList[index + 1],
                        index + 1,
                        imageList,
                        resultImageList,
                        complete,
                        error
                    )
                }
            },
            {
                error.invoke(getString(R.string.upload_picture_failed))
            })
    }

    private fun uploadLogFile(
        complete: (String) -> Unit
    ) {
        if (mType == 1) {
            // 错误，上传日志
            val logs = StringBuilder()
            RLogManager.getRLogHistory { logModels ->
                ThreadUtils.runOnIOThread {
                    logModels.forEach { log ->
                        logs.append(
                            "${framework.telegram.support.tools.TimeUtils.getYMDHMSFormatTime(log.time)} " +
                                    "${log.level}/${log.tag}:${log.log}\n"
                        )
                    }

                    val file = File(
                        DirManager.getFileCacheDir(this@FeedbackActivity, AccountManager.getLoginAccountUUid()),
                        "${System.currentTimeMillis()}.log"
                    )

                    de.greenrobot.common.io.FileUtils.writeUtf8(
                        file,
                        logs
                    )

                    ThreadUtils.runOnUIThread {
                        UploadManager.uploadFile(
                            this@FeedbackActivity,
                            Uri.fromFile(file).toString(),
                            CommonProto.AttachType.FILE,
                            CommonProto.AttachWorkSpaceType.COMMON,
                            { url ->
                                complete.invoke(url)
                            }, {
                                complete.invoke("")
                            })
                    }
                }
            }
        } else {
            complete.invoke("")
        }
    }

    private fun changeButton(button: Int) {
        when (button) {
            0 -> {
                setButtonDraw(app_text_view_suggest, true)
                setButtonDraw(app_text_view_error, false)
                setButtonDraw(app_text_view_other, false)
            }
            1 -> {
                setButtonDraw(app_text_view_suggest, false)
                setButtonDraw(app_text_view_error, true)
                setButtonDraw(app_text_view_other, false)
            }
            2 -> {
                setButtonDraw(app_text_view_suggest, false)
                setButtonDraw(app_text_view_error, false)
                setButtonDraw(app_text_view_other, true)
            }
        }
    }

    private fun setButtonDraw(view: AppCompatTextView, select: Boolean) {
        if (select) {
            view.setBackgroundResource(R.drawable.common_corners_trans_178aff_6_0)
            view.setTextColor(getSimpleColor(R.color.white))
        } else {
            view.setBackgroundResource(R.drawable.common_corners_trans_edeff2_6_0)
            view.setTextColor(getSimpleColor(R.color.black))
        }
    }

    override fun initData() {
        FeedbackPresenterImpl(this, this, lifecycle())
    }

    override fun showLoading() {
        dialog?.dismiss()
        dialog = AppDialog.showLoadingView(this@FeedbackActivity, this@FeedbackActivity)
    }

    override fun setFeedbackSuccess(msg: String) {
        dialog?.dismiss()
        toast(getString(R.string.thanks_for_the_feedback))
        finish()
    }

    override fun showErrMsg(str: String?) {
        dialog?.dismiss()
        toast(str.toString())
    }

    override fun setPresenter(presenter: BasePresenter) {
        mPresenter = presenter as FeedbackPresenterImpl
    }

    override fun isActive() = true

    private fun setLoginBtn(ok: Boolean) {
        if (ok) {
            frame_layout_op.isEnabled = true
            frame_layout_op.background =
                getSimpleDrawable(R.drawable.common_corners_trans_178aff_6_0)
        } else {
            frame_layout_op.isEnabled = false
            frame_layout_op.background =
                getSimpleDrawable(R.drawable.common_corners_trans_d4d6d9_6_0)
        }
    }

    private fun addPickPicture() {
        val layout =
            LayoutInflater.from(this).inflate(R.layout.bus_contacts_activity_pick_picture, null)
        pick_layout.addView(layout)
        layout.setOnClickListener {
            val index = getPickLayoutChildInt(layout)
            if (index != -1) {
                taskPicture(index)
            }
        }
        val delete = layout.findViewById<ImageView>(R.id.delete)
        delete.setOnClickListener {
            pick_layout.removeView(layout)
            if (getImageCount() == 2) {
                addPickPicture()
            }
        }
    }

    private fun getPickLayoutChildInt(targetView: View): Int {
        for (index in 0 until pick_layout.childCount) {
            val childView = pick_layout.getChildAt(index)
            if (targetView == childView) {
                return index
            }
        }
        return -1
    }

    private fun getImageCount(): Int {
        var count = 0
        for (index in 0 until pick_layout.childCount) {
            val layout = pick_layout.getChildAt(index)
            val data = layout.getTag(R.id.image_url)
            if (data != null)
                count++
        }
        return count
    }

    override fun setUri(taskIndex: Int, url: Uri?) {
        val viewCount = pick_layout.childCount
        if (taskIndex < viewCount) {
            val layout = pick_layout.getChildAt(taskIndex)
            val param = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            param.rightMargin = ScreenUtils.dp2px(this@FeedbackActivity, 8f)
            layout.layoutParams = param
            layout.findViewById<AppImageView>(R.id.image).setImageURI(url)
            layout.findViewById<ImageView>(R.id.delete).visibility = View.VISIBLE
            layout.setTag(R.id.image_url, url.toString())
            if (viewCount < 3) {
                addPickPicture()
            }
        }
    }

    private fun taskPicture(taskIndex: Int) {
        mPresenter?.clickPickPhoto(taskIndex)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        mPresenter?.onActivityResult(requestCode, resultCode, data)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        mPresenter?.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}