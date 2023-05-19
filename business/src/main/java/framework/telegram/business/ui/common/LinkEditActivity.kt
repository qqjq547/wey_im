package framework.telegram.business.ui.common

import android.content.Intent
import android.text.TextUtils
import android.view.View
import com.alibaba.android.arouter.facade.annotation.Route
import com.im.domain.pb.CommonProto
import com.im.domain.pb.SysProto
import framework.telegram.business.R
import framework.telegram.business.bridge.Constant
import framework.telegram.business.http.HttpManager
import framework.telegram.business.http.creator.SysHttpReqCreator
import framework.telegram.business.http.getResult
import framework.telegram.business.http.protocol.SystemHttpProtocol
import framework.telegram.business.ui.base.BaseBusinessActivity
import framework.telegram.support.BaseApp
import framework.telegram.support.mvp.BasePresenter
import framework.telegram.support.system.network.http.HttpReq
import framework.telegram.support.tools.ExpandClass.toast
import framework.telegram.ui.dialog.AppDialog
import framework.telegram.ui.menu.FloatMenu
import framework.telegram.ui.tools.Helper
import kotlinx.android.synthetic.main.bus_link_activity.*

/**
 * Created by lzh on 19-12-6.
 * INFO:
 */
@Route(path = Constant.ARouter.ROUNTE_BUS_COMMON_LINK)
class LinkEditActivity : BaseBusinessActivity<BasePresenter>(){

    //我的  mType = 0  ； 群的 mType = 1
    private val mType by lazy {intent.getIntExtra("type",0)}

    private val mTargetId by lazy {intent.getLongExtra("targetId",0L)}

    private val mTitle by lazy {
        if (mType ==0 ){
            getString(R.string.string_my_link)
        }else{
            getString(R.string.string_group_link)
        }
    }

    private var mLink = ""

    override fun getLayoutId()= R.layout.bus_link_activity

    override fun initView() {
        custom_toolbar.setBackIcon(R.drawable.common_icon_black_back) {
            finish()
        }

        getLinkDetail()
        custom_toolbar.showCenterTitle(mTitle)
    }

    override fun initListen() {

        text_view_my_link.setOnLongClickListener {
            showCopyDialog(text_view_my_link, mLink)
            return@setOnLongClickListener false
        }

        text_view_copy.setOnClickListener {
            Helper.setPrimaryClip(this@LinkEditActivity,mLink)
            toast(getString(R.string.copy_success))
        }

        text_view_share_link.setOnClickListener {
            val intent = Intent(Intent.ACTION_SEND)
            intent.type = "text/plain"
            intent.putExtra(Intent.EXTRA_TEXT, mLink)
            startActivity(Intent.createChooser(intent, mTitle))
        }

        custom_toolbar.showRightImageView(R.drawable.common_icon_more, {
            AppDialog.showBottomListView(this, this, mutableListOf(getString(R.string.string_update_link))) { _, index, _ ->
                when (index) {
                    0 -> {
                        AppDialog.show(this@LinkEditActivity, this@LinkEditActivity) {
                            message(text = getString(R.string.string_update_link_sure))
                            positiveButton(text = getString(R.string.confirm), click = {
                                updataLink()
                            })
                        }
                    }
                }
            }
        })
    }

    override fun initData() {

    }

    private fun  getLinkDetail(){
        HttpManager.getStore(SystemHttpProtocol::class.java)
                .getInviteLink(object : HttpReq<SysProto.GetInviteLinkReq>() {
                    override fun getData(): SysProto.GetInviteLinkReq {
                        val realType = if (mType == 0 ){
                             CommonProto.InviteLinkType.LINK_USER
                        }else{
                             CommonProto.InviteLinkType.LINK_GROUP
                        }
                        return SysHttpReqCreator.getInviteLink(mTargetId, realType)
                    }
                })
                .getResult(lifecycle(), {
                    mLink = it.inviteLink
                    if (!TextUtils.isEmpty(mLink)){
                        text_view_my_link.text =mLink
                    }else{
//                        toast(it.message.toString())
                        finish()
                    }
                },{
                    toast(it.message.toString())
                    finish()
                })
    }

    private fun updataLink(){
        HttpManager.getStore(SystemHttpProtocol::class.java)
                .updateInviteLink(object : HttpReq<SysProto.UpdateInviteLinkReq>() {
                    override fun getData(): SysProto.UpdateInviteLinkReq {
                        val realType = if (mType == 0 ){
                            CommonProto.InviteLinkType.LINK_USER
                        }else{
                            CommonProto.InviteLinkType.LINK_GROUP
                        }
                        return SysHttpReqCreator.updateInviteLink(mTargetId, realType)
                    }
                })
                .getResult(lifecycle(), {
                    mLink = it.inviteLink
                    if (!TextUtils.isEmpty(mLink)){
                        text_view_my_link.text =mLink
                    }
                },{
                    toast(it.message.toString())
                    finish()
                })
    }


    private fun showCopyDialog(v: View, str:String?){

        val floatMenu = FloatMenu(this)

        val items = mutableListOf<String>()
        items.add(getString(R.string.copy))

        floatMenu.items(*items.toTypedArray())

        floatMenu.showDropDown(v, v.width - 5, 0)
        floatMenu.setOnItemClickListener { _, text ->
            when (text) {
                getString(R.string.copy) ->{
                    Helper.setPrimaryClip(BaseApp.app,str)
                    toast(getString(R.string.copy_success))
                }
            }
        }
    }

}