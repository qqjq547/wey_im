package framework.telegram.business.ui.contacts

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import com.alibaba.android.arouter.facade.annotation.Route
import com.im.domain.pb.CommonProto
import framework.telegram.business.R
import framework.telegram.business.bridge.Constant
import framework.telegram.business.ui.base.BaseBusinessActivity
import framework.telegram.business.ui.contacts.presenter.ComplainEditContract
import framework.telegram.business.ui.contacts.presenter.ComplainEditPresenterImpl
import framework.telegram.business.manager.UploadManager
import framework.telegram.support.mvp.BasePresenter
import framework.telegram.support.tools.ExpandClass.getSimpleDrawable
import framework.telegram.support.tools.ExpandClass.toast
import framework.telegram.support.tools.ThreadUtils
import framework.telegram.ui.dialog.AppDialog
import framework.telegram.ui.image.AppImageView
import framework.telegram.ui.utils.KeyboardktUtils
import framework.telegram.ui.utils.ScreenUtils
import kotlinx.android.synthetic.main.bus_contacts_activity_complaint_edit.*

/**
 * Created by lzh on 19-7-5.
 * INFO:
 */
@Route(path = Constant.ARouter.ROUNTE_BUS_CONTACTS_COMPLAINT_EDIT)
class ComplaintEditActivity : BaseBusinessActivity<ComplainEditContract.Presenter>(), ComplainEditContract.View {

    private val mTargetUId by lazy { intent.getLongExtra("targetUId", 0) }
    private val mType by lazy { intent.getIntExtra("type", 0) }//0 人 ;1 群
    private val mReportType by lazy { intent.getIntExtra("reportType", 0) }

    private var mContextOk = false

    override fun getLayoutId() = R.layout.bus_contacts_activity_complaint_edit

    private var mPictureSize = 0

    override fun initView() {
        custom_toolbar.showCenterTitle(getString(R.string.string_complaint_description))
        custom_toolbar.setBackIcon(R.drawable.common_icon_black_back) {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }

        val reportText = if (mType == 0){
            when(mReportType){
                1->getString(R.string.bad_information)
                2->getString(R.string.attack_action)
                3->getString(R.string.illegal_behavior)
                else->""
            }
        }else{
            when(mReportType){
                1->getString(R.string.group_report_1)
                2->getString(R.string.group_report_2)
                3->getString(R.string.group_report_3)
                4->getString(R.string.group_report_4)
                5->getString(R.string.illegal_behavior)
                else->""
            }
        }
        text_view_report_type.text = reportText

        text_view_count.text = (Constant.Bus.MAX_TEXT_COUNT_2).toString()

        edit_text_context.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                val str = s.toString().trim()
                text_view_count.text = (Constant.Bus.MAX_TEXT_COUNT_2 - str.length).toString()
                mContextOk = str.length >=5
                setSubmitBottom()
            }
        })

        linear_layout_1.visibility = View.VISIBLE
        linear_layout_2.visibility = View.GONE
        setSubmitBottom()

        addPickPicture()
    }

    override fun initListen() {
        frame_layout_op.setOnClickListener {
            submitComplain()
        }

        all_layout.setOnClickListener {
            KeyboardktUtils.hideKeyboard(all_layout)
        }

        frame_layout_finish.setOnClickListener {
            val intent = Intent()
            setResult(Activity.RESULT_OK, intent)
            finish()
        }
    }

    private fun addPickPicture(){
        val layout = LayoutInflater.from(this).inflate(R.layout.bus_contacts_activity_pick_picture, null)
        pick_layout.addView(layout)
        layout.setOnClickListener {
            val index = getPickLayoutChildInt(layout)
            if (index != -1){
                taskPicture(index)
            }
        }
        val delete = layout.findViewById<ImageView>(R.id.delete)
        delete.setOnClickListener {
            pick_layout.removeView(layout)
            mPictureSize--
            if(mPictureSize  == 2){
                addPickPicture()
            }
        }
    }

    private fun getPickLayoutChildInt(targetView:View) :Int{
        for (index in 0 until pick_layout.childCount){
            val childView = pick_layout.getChildAt(index)
            if (targetView == childView){
                return index
            }
        }
        return -1
    }

    override fun initData() {
        ComplainEditPresenterImpl(this, this, lifecycle()).start()
    }

    private fun submitComplain() {
        showLoading()
        val str = edit_text_context.text.toString()
        val imageList = mutableListOf<String>()
        for (index in 0 until pick_layout.childCount){
            val childView = pick_layout.getChildAt(index)
            val tag =  childView.getTag(R.id.image_url)
            tag?.let {
                val url =it.toString()
                if (!TextUtils.isEmpty(url)){
                    imageList.add(url)
                }
            }
        }
        if (imageList.size>0){
            uploadPicture(imageList[0],0,imageList, mutableListOf(),{
                var pictureStr=""
                it.forEach {picture->
                    pictureStr= "$picture,"
                }
                if(pictureStr.endsWith(",")) {
                    pictureStr = pictureStr.substring(0,pictureStr.length - 1)
                }
                mPresenter?.submitComplain(str, mTargetUId, mType,mReportType,pictureStr)
            },{
                showErrMsg(it)
            })
        }else{
            mPresenter?.submitComplain(str, mTargetUId, mType,mReportType,"")
        }
    }

    private fun uploadPicture(uri:String,index:Int ,imageList:MutableList<String>,resultImageList:MutableList<String>,complete:(MutableList<String>)->Unit ,error:(String)->Unit){
        UploadManager.uploadFile(this@ComplaintEditActivity,uri, CommonProto.AttachType.PIC, CommonProto.AttachWorkSpaceType.COMMON, {
            resultImageList.add(it)
            if (index+1 >= imageList.size){
                complete.invoke(resultImageList)
            }else{
                uploadPicture(imageList[index+1],index+1,imageList,resultImageList,complete,error)
            }
        }, {
            error.invoke(getString(R.string.upload_picture_failed))
        })
    }

    private fun taskPicture(taskIndex: Int) {
        mPresenter?.clickPickPhoto(taskIndex)
    }

    private fun setSubmitBottom() {
        if (mContextOk) {
            frame_layout_op.isEnabled = true
            frame_layout_op.background = getSimpleDrawable(R.drawable.common_corners_trans_178aff_6_0)
        } else {
            frame_layout_op.isEnabled = false
            frame_layout_op.background = getSimpleDrawable(R.drawable.common_corners_trans_d4d6d9_6_0)
        }
    }

    override fun onBackPressed() {
        setResult(Activity.RESULT_CANCELED)
        super.onBackPressed()
    }

    override fun showLoading() {
        dialog?.dismiss()
        dialog = AppDialog.showLoadingView(this@ComplaintEditActivity,this@ComplaintEditActivity)
    }

    override fun submitComplainSuccess(str: String?) {
        linear_layout_1.visibility = View.GONE
        linear_layout_2.visibility = View.VISIBLE
        dialog?.dismiss()
    }

    override fun showErrMsg(str: String?) {
        ThreadUtils.runOnUIThread {
            toast(str.toString())
            dialog?.dismiss()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        mPresenter?.onActivityResult(requestCode,resultCode,data)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        mPresenter?.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
    
    override fun setUri(mTaskIndex: Int, url: Uri?) {
        val viewCount = pick_layout.childCount
        if (mTaskIndex < viewCount){
            val layout = pick_layout.getChildAt(mTaskIndex)
            val param = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            param.rightMargin = ScreenUtils.dp2px(this@ComplaintEditActivity, 8f)
            layout.layoutParams = param
            layout.findViewById<AppImageView>(R.id.image).setImageURI(url)
            layout.findViewById<ImageView>(R.id.delete).visibility= View.VISIBLE
            layout.setTag(R.id.image_url,url.toString())
            if (viewCount<3){
                addPickPicture()
            }
            mPictureSize++
        }
    }

    override fun setPresenter(presenter: BasePresenter) {
        mPresenter = presenter as ComplainEditPresenterImpl
    }

    override fun isActive() = true
}