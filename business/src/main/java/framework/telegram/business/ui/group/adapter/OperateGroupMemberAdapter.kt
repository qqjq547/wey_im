package framework.telegram.business.ui.group.adapter

import android.view.View
import android.widget.CheckBox
import android.widget.CompoundButton
import com.chad.library.adapter.base.BaseViewHolder
import com.im.domain.pb.CommonProto
import framework.ideas.common.model.group.GroupMemberModel
import framework.telegram.business.ArouterServiceManager
import framework.telegram.business.R
import framework.telegram.business.bridge.bean.AccountInfo
import framework.telegram.business.ui.group.OperateGroupMemberActivity
import framework.telegram.business.ui.widget.ViewUtils
import framework.telegram.support.account.AccountManager
import framework.telegram.support.tools.framework.telegram.support.UriUtils
import framework.telegram.ui.doubleclick.recycler.AppBaseQuickAdapter
import framework.telegram.ui.image.AppImageView
import java.util.*

class OperateGroupMemberAdapter(var operateType: Int) : AppBaseQuickAdapter<GroupMemberModel, BaseViewHolder>(R.layout.bus_contacts_selectable_item), CompoundButton.OnCheckedChangeListener {

    private val mSelectedUids by lazy { ArrayList<Long>() }

    private val mMineUid by lazy { AccountManager.getLoginAccount(AccountInfo::class.java).getUserId() }

    fun getSelectedUids(): List<Long> {
        return mSelectedUids
    }

    override fun convert(helper: BaseViewHolder, item: GroupMemberModel?) {
        item?.let {
            helper?.setText(R.id.app_text_view_name, item.displayName)
            helper?.getView<AppImageView>(R.id.image_view_icon)?.setImageURI(UriUtils.parseUri(item.icon))
            helper?.getView<CheckBox>(R.id.check_box_selected)?.visibility = View.GONE

            if (operateType == OperateGroupMemberActivity.OPERATE_TYPE_DISPLAY_ALL_MEMBER||operateType == OperateGroupMemberActivity.OPERATE_TYPE_ADD_ADMIN) {
                if (item.type == CommonProto.GroupMemberType.HOST.number) {
                    helper?.setGone(R.id.text_view_flag, true)
                    helper?.setGone(R.id.tv_admin, false)
                } else if(item.type == CommonProto.GroupMemberType.MANAGE.number){
                    helper?.setGone(R.id.text_view_flag, false)
                    helper?.setGone(R.id.tv_admin, true)
                } else {
                    helper?.setGone(R.id.text_view_flag, false)
                    helper?.setGone(R.id.tv_admin, false)
                }

                helper?.setGone(R.id.text_view_online_status_point, false)
                if (item?.isShowLastOnlineTime && item.isOnlineStatus) {
                    helper?.setGone(R.id.text_view_online_status_point, true)
                }

                helper?.setGone(R.id.text_view_online_status, true)
                ViewUtils.showOnlineStatus(ArouterServiceManager.messageService, item, helper?.getView(R.id.text_view_online_status))
            } else {
                helper?.setGone(R.id.text_view_online_status, false)
                helper?.setGone(R.id.text_view_online_status_point, false)
                helper?.setGone(R.id.text_view_flag, false)
            }
        }
    }

    override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
        if (isChecked) {
            mSelectedUids.add(buttonView?.tag as Long)
        } else {
            mSelectedUids.remove(buttonView?.tag as Long)
        }
    }
}