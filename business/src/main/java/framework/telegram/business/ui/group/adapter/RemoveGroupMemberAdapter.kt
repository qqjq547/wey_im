package framework.telegram.business.ui.group.adapter

import android.view.View
import android.widget.CheckBox
import android.widget.CompoundButton
import com.chad.library.adapter.base.BaseViewHolder
import com.im.domain.pb.CommonProto
import framework.ideas.common.model.group.GroupMemberModel
import framework.telegram.business.R
import framework.telegram.business.bridge.bean.AccountInfo
import framework.telegram.business.event.RemoveSelectMemberEvent
import framework.telegram.support.account.AccountManager
import framework.telegram.support.system.event.EventBus
import framework.telegram.support.tools.framework.telegram.support.UriUtils
import framework.telegram.ui.doubleclick.recycler.AppBaseQuickAdapter
import framework.telegram.ui.image.AppImageView
import java.util.*

class RemoveGroupMemberAdapter : AppBaseQuickAdapter<GroupMemberModel, BaseViewHolder>(R.layout.bus_contacts_selectable_item), CompoundButton.OnCheckedChangeListener {

    private val mSelectedUids by lazy { ArrayList<Long>() }

    private val mMineUid by lazy { AccountManager.getLoginAccount(AccountInfo::class.java).getUserId() }
    private var mGroupType = 2 // 普通成员

    fun getSelectedUids(): List<Long> {
        return mSelectedUids
    }

    fun addSelectedUid(uid: Long) {
        if (!mSelectedUids.contains(uid))
            mSelectedUids.add(uid)
    }

    fun removeSelectedUid(uid: Long) {
        if (mSelectedUids.contains(uid))
            mSelectedUids.remove(uid)
    }


    fun setGroupType(type :Int){
        mGroupType = type
    }

    override fun convert(helper: BaseViewHolder, item: GroupMemberModel?) {
        item?.let {
            helper?.setText(R.id.app_text_view_name, item.displayName)
            helper?.getView<AppImageView>(R.id.image_view_icon)?.setImageURI(UriUtils.parseUri(item.icon))
            helper?.getView<View>(R.id.view_mask)?.setOnClickListener {
                //这是个遮罩，为了拦截点击事件
            }
            //显示多选框
            helper?.getView<CheckBox>(R.id.check_box_selected)?.visibility = View.VISIBLE
            val checkBottom =  helper?.getView<CheckBox>(R.id.check_box_selected)
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

            if (item.uid == mMineUid//自己不可选
                    || item.type == CommonProto.GroupMemberType.HOST.number//群主不可选
                    || ((mGroupType == CommonProto.GroupMemberType.MANAGE.number)  //自己是群管理 或者是普通成员，但对方是管理员
                            && item.type == CommonProto.GroupMemberType.MANAGE.number)) {
                //不可选且默认未选
                checkBottom?.setOnCheckedChangeListener(null)
                checkBottom?.isChecked = false
                checkBottom?.isEnabled = false
                helper?.getView<View>(R.id.view_mask)?.visibility = View.VISIBLE
            } else {
                //可选
                checkBottom?.isEnabled = true
                checkBottom?.setOnCheckedChangeListener(null)
                checkBottom?.isChecked = mSelectedUids.contains(item.uid)
                checkBottom?.setOnCheckedChangeListener(this@RemoveGroupMemberAdapter)
                checkBottom?.tag = item
                helper?.getView<View>(R.id.view_mask)?.visibility = View.GONE
                helper?.itemView?.setOnClickListener{
                    checkBottom?.setOnCheckedChangeListener(null)
                    val check = !(checkBottom?.isChecked ?:false)
                    checkBottom?.isChecked = check
                    setCheckChanged(item,check)
                    checkBottom?.setOnCheckedChangeListener(this@RemoveGroupMemberAdapter)
                }
            }
        }
    }

    private fun setCheckChanged(itemData:GroupMemberModel,isChecked:Boolean){
        if (isChecked) {
            mSelectedUids.add(itemData.uid)
            EventBus.publishEvent(RemoveSelectMemberEvent(itemData.uid, itemData.icon, 1,1))
        } else {
            mSelectedUids.remove(itemData.uid)
            EventBus.publishEvent(RemoveSelectMemberEvent(itemData.uid, itemData.icon, 2,1))
        }
    }

    override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
        if (buttonView?.tag is GroupMemberModel) {
            val itemData = buttonView.tag as GroupMemberModel
            setCheckChanged(itemData,isChecked)
        }
    }
}