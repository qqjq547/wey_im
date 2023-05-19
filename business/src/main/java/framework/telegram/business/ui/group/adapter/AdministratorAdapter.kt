package framework.telegram.business.ui.group.adapter

import android.widget.TextView
import com.chad.library.adapter.base.BaseViewHolder
import framework.telegram.business.ArouterServiceManager
import framework.telegram.business.R
import framework.telegram.business.ui.group.bean.AdministratorModel
import framework.telegram.business.ui.widget.ViewUtils
import framework.telegram.ui.doubleclick.recycler.AppBaseMultiItemQuickAdapter
import framework.telegram.ui.image.AppImageView

/**
 * Created by yanggl on 2019/8/29 11:42
 */
class AdministratorAdapter() : AppBaseMultiItemQuickAdapter<AdministratorModel, BaseViewHolder>(null){

    init {
        addItemType(AdministratorModel.Type.TEXT, R.layout.bus_administrator_text)
        addItemType(AdministratorModel.Type.ITEM, R.layout.bus_administrator_item)
    }

    override fun convert(helper: BaseViewHolder, item: AdministratorModel) {
        if(item.itemType==AdministratorModel.Type.TEXT){
            helper.getView<TextView>(R.id.tv).setText(item?.titleName)
        }else{
            helper.getView<AppImageView>(R.id.iv_avatar).setImageURI(item?.icon)
            helper.getView<TextView>(R.id.tv_name).setText(item?.getDisplayName())

            //在线状态
            helper?.setGone(R.id.v_online_status_point, false)
            if (item?.isShowLastOnlineTime && item.onlineStatus) {
                helper?.setGone(R.id.v_online_status_point, true)
            }
            helper?.setGone(R.id.tv_sign_more,   item.isSetAdmin)
            helper?.setGone(R.id.tv_status, true)
            ViewUtils.showOnlineStatus(ArouterServiceManager.messageService, helper?.getView(R.id.tv_status), item.uid,item.isShowLastOnlineTime,item.onlineStatus,item.lastOnlineTime)
        }
    }

}