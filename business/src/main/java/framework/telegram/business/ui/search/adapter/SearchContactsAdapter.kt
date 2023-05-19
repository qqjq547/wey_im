package framework.telegram.business.ui.search.adapter

import android.content.Context
import com.afollestad.materialdialogs.customview.getCustomView
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.chad.library.adapter.base.BaseViewHolder
import com.chad.library.adapter.base.entity.MultiItemEntity
import com.im.domain.pb.ContactsProto
import framework.ideas.common.model.contacts.ContactDataModel
import framework.telegram.business.ArouterServiceManager
import framework.telegram.business.BuildConfig
import framework.telegram.business.R
import framework.telegram.business.bridge.Constant
import framework.telegram.business.bridge.Constant.Search.SEARCH_BLACK
import framework.telegram.business.bridge.Constant.Search.SEARCH_CONTACTS
import framework.telegram.business.bridge.event.*
import framework.telegram.business.bridge.search.BaseSearchAdapter
import framework.telegram.business.bridge.search.StringUtil
import framework.telegram.message.bridge.Constant.ARouter.ROUNTE_MSG_PRIVATE_CHAT_ACTIVITY
import framework.telegram.message.bridge.Constant.Search.CONTACT_ITEM_TYPE
import framework.telegram.support.system.event.EventBus
import framework.telegram.support.tools.ExpandClass.toast
import framework.telegram.ui.dialog.AppDialog
import framework.telegram.ui.image.AppImageView
import framework.telegram.ui.text.AppTextView

/**
 * Created by lzh on 19-5-21.
 * INFO:通讯录 adapter
 */
@Route(path = Constant.ARouter.ROUNTE_SERVICE_ADAPTER_CONTACT, name = "")
class SearchContactsAdapter : BaseSearchAdapter<MultiItemEntity, BaseViewHolder>(null) {
    override fun setSearchTargetId(tagerId: Long) {
    }

    override fun setExtra(dataSet: Set<Long>) {
    }

    private var mKeyword = ""
    private var mTargetPic: String? = ""
    private var mTargetName: String? = ""


    override fun setKeyword(keyword: String) {
        mKeyword = keyword
    }

    private var mSearchType = 0

    override fun setSearchType(searchType: Int) {
        mSearchType = searchType
    }

    override fun setExtra(mapList: List<MutableMap<String, String>>) {
        mapList.forEach {
            mTargetName = it[SEARCH_USER_NAME]
            mTargetPic = it[SEARCH_USER_ICON]
        }
    }

    override fun getAdapter(): BaseSearchAdapter<MultiItemEntity, BaseViewHolder> = this

    override fun init(context: Context?) {
    }

    override fun addItems() {
        putLayout(CONTACT_ITEM_TYPE, R.layout.bus_search_contacts_item)
    }

    override fun convertImpl(helper: BaseViewHolder, item: MultiItemEntity) {
        when (item.itemType) {
            CONTACT_ITEM_TYPE -> {
                if (item is ContactDataModel) {
                    try {
                        helper.getView<AppTextView>(R.id.app_text_view_name)?.text = StringUtil.setHitTextColor(mKeyword, item.displayName)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    helper.getView<AppImageView>(R.id.image_view_icon)?.setImageURI(item.icon)

                    helper.itemView.setOnClickListener {
                        when (mSearchType) {
                            SEARCH_CONTACTS , Constant.Search.SEARCH_UN_VIEW_ONLINE -> {
                                ARouter.getInstance().build(Constant.ARouter.ROUNTE_BUS_CONTACT_DETAIL)
                                        .withSerializable(Constant.ARouter_Key.KEY_ADD_FRIEND_FROM, ContactsProto.ContactsAddType.PHONE)
                                        .withLong(Constant.ARouter_Key.KEY_TARGET_UID, item.uid).navigation()
                            }
                            Constant.Search.SEARCH_CHAT -> {
                                ARouter.getInstance().build(ROUNTE_MSG_PRIVATE_CHAT_ACTIVITY).withLong("targetUid", item.uid).navigation()
                            }
                            Constant.Search.SEARCH_CARD_CONTACTS -> {
                                AppDialog.showCustomView(helper?.itemView.context, R.layout.common_dialog_share_item, null) {
                                    getCustomView().findViewById<AppImageView>(R.id.image_view_icon2).setImageURI(mTargetPic)
                                    getCustomView().findViewById<AppImageView>(R.id.image_view_icon1).setImageURI(item.icon)
                                    getCustomView().findViewById<AppTextView>(R.id.app_text_view_name).text = String.format(helper?.itemView.context.getString(R.string.recommend_mat), item.displayName, mTargetName)

                                    positiveButton(text = helper?.itemView.context.getString(R.string.confirm), click = {
                                        EventBus.publishEvent(SearchCardEvent(item.uid))
                                    })
                                    negativeButton(text = helper?.itemView.context.getString(R.string.cancel))
                                    title(text = helper?.itemView.context.getString(R.string.send_a_card))
                                }
                            }
                        }

                    }

                    helper.itemView.setOnLongClickListener {
                        when (mSearchType) {
                            SEARCH_BLACK->{
                                AppDialog.showBottomListView(helper?.itemView.context, "",
                                        mutableListOf(helper?.itemView.context.getString(R.string.remove_from_blacklist))) { dialog, index, _ ->
                                    when (index) {
                                        0 -> {
                                            ArouterServiceManager.contactService.setContactBlack(null, item.uid, false, {
                                                EventBus.publishEvent(SearchSetBlackEvent())
                                            }) {
                                                mContext?.toast(it)
                                            }
                                        }
                                    }
                                }
                            }
                            Constant.Search.SEARCH_UN_VIEW_ONLINE -> {
                                AppDialog.showList(helper.itemView.context,
                                        listOf(helper?.itemView.context.getString(R.string.delete))) { _, _, _ ->

                                    ArouterServiceManager.contactService.deleteDisShowOnlineContact(null, item.uid, {
                                        EventBus.publishEvent(SearchFinishEvent())
                                    }, {
                                        if (BuildConfig.DEBUG) {
                                            helper.itemView.context.toast(helper?.itemView.context.getString(R.string.deletion_not_visible_failed_sign) + it.message)
                                        } else {
                                            helper.itemView.context.toast(helper?.itemView.context.getString(R.string.deletion_not_visible_failed))
                                        }
                                    })
                                }
                            }
                        }
                        return@setOnLongClickListener false
                    }
                }
            }
        }
    }
}

