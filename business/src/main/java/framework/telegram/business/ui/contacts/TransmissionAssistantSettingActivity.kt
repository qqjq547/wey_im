package framework.telegram.business.ui.contacts

import com.alibaba.android.arouter.facade.annotation.Route
import framework.ideas.common.model.im.ChatModel
import framework.telegram.business.ArouterServiceManager
import framework.telegram.business.R
import framework.telegram.business.bridge.Constant
import framework.telegram.business.ui.base.BaseBusinessActivity
import framework.telegram.business.ui.me.presenter.InfoContract
import framework.telegram.ui.dialog.AppDialog
import kotlinx.android.synthetic.main.bus_contacts_transmission_assistant_setting_activity.*
import kotlinx.android.synthetic.main.bus_group_activity_group_contact_setting.custom_toolbar

/**
 * Created by yanggl on 2019/10/25 14:07
 */
@Route(path = Constant.ARouter.ROUNTE_BUS_TRANSMISSION_ASSISTANT_SETTING)
class TransmissionAssistantSettingActivity : BaseBusinessActivity<InfoContract.Presenter>() {
    override fun getLayoutId(): Int {
        return R.layout.bus_contacts_transmission_assistant_setting_activity
    }

    override fun initView() {
        custom_toolbar.setBackIcon(R.drawable.common_icon_black_back) {
            finish()
        }
        custom_toolbar.showCenterTitle(getString(R.string.chat_settings))

        ArouterServiceManager.messageService.getChatTopStatus(ChatModel.CHAT_TYPE_PVT, framework.telegram.message.bridge.Constant.Common.FILE_TRANSFER_UID, {
            switch_button_top_chat.setData(getString(R.string.top_chat), it) { newValue ->
                //置顶
                ArouterServiceManager.messageService.setChatTopStatus(ChatModel.CHAT_TYPE_PVT, framework.telegram.message.bridge.Constant.Common.FILE_TRANSFER_UID, newValue)
            }
        })

        tv_clear.setOnClickListener {
            //清空聊天记录
            AppDialog.show(this@TransmissionAssistantSettingActivity, this@TransmissionAssistantSettingActivity) {
                positiveButton(text = getString(R.string.confirm), click = {
                    //清空聊天记录
                    ArouterServiceManager.messageService.clearMessageHistory(ChatModel.CHAT_TYPE_PVT, framework.telegram.message.bridge.Constant.Common.FILE_TRANSFER_UID)
                })
                negativeButton(text = getString(R.string.cancel))
                title(text = getString(R.string.whether_to_clear_chat_record))
                message(text = getString(R.string.are_you_sure_empty_chat_logs))
            }
        }
    }

    override fun initListen() {
    }

    override fun initData() {
    }

}