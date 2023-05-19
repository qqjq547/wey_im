package framework.telegram.business.ui.search.adapter

import android.content.Context
import android.text.TextUtils
import android.util.Log
import android.widget.TextView
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.chad.library.adapter.base.BaseViewHolder
import com.chad.library.adapter.base.entity.MultiItemEntity
import com.im.domain.pb.ContactsProto
import framework.ideas.common.model.contacts.ContactDataModel
import framework.telegram.business.R
import framework.telegram.business.bridge.Constant
import framework.telegram.business.bridge.Constant.Search.SEARCH_CONTACTS
import framework.telegram.business.bridge.Constant.Search.SEARCH_CONTACTS_EXPAND
import framework.telegram.business.bridge.search.BaseSearchAdapter
import framework.telegram.business.bridge.search.StringUtil
import framework.telegram.message.bridge.Constant.ARouter.ROUNTE_MSG_PRIVATE_CHAT_ACTIVITY
import framework.telegram.message.bridge.Constant.Common.FILE_TRANSFER_UID
import framework.telegram.message.bridge.Constant.Search.CONTACT_ITEM_TYPE
import framework.telegram.support.system.pinyin.FastPinyin
import framework.telegram.support.tools.framework.telegram.support.UriUtils
import framework.telegram.ui.image.AppImageView
import framework.telegram.ui.text.AppTextView

/**
 * Created by lzh on 19-5-21.
 * INFO:通讯录 adapter
 */
@Route(path = Constant.ARouter.ROUNTE_SERVICE_ADAPTER_CONTACT_2, name = "")
class SearchContacts2Adapter : BaseSearchAdapter<MultiItemEntity, BaseViewHolder>(null) {
    override fun setExtra(dataSet: Set<Long>) {
    }

    override fun setSearchTargetId(tagerId: Long) {
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
        putLayout(CONTACT_ITEM_TYPE, R.layout.bus_search_contacts_item_2)
    }

    override fun convertImpl(helper: BaseViewHolder, item: MultiItemEntity) {
        when (item.itemType) {
            CONTACT_ITEM_TYPE -> {
                if (item is ContactDataModel) {
                    var lightWord = mKeyword
                    val view = helper.getView<AppTextView>(R.id.app_text_view_name)
                    var name = item.displayName
                    var secondName = ""
                    var secondHeadName = ""
                    if (!TextUtils.isEmpty(item.displayName)&&item.displayName.contains(mKeyword)) {
                        name = item.displayName
                        secondName = ""
                    }else if (!TextUtils.isEmpty(item.searchNoteName)&&item.searchNoteName.contains(mKeyword.toUpperCase())) {
                        lightWord = FastPinyin.findWordFromPinyin(mKeyword,item.noteName)
                        name = item.noteName
                        secondName = ""
                    }else if (!TextUtils.isEmpty(item.searchNickName)&&item.searchNickName.contains(mKeyword.toUpperCase())) {
                        lightWord = FastPinyin.findWordFromPinyin(mKeyword,item.nickName)
                        name = item.nickName
                        secondName = ""
                    }else if (!TextUtils.isEmpty(item.shortNoteName)&&item.shortNoteName.contains(mKeyword.toUpperCase())) {
                        lightWord = FastPinyin.findWordFromPinyinToFirst(mKeyword,item.noteName)
                        name = item.noteName
                        secondName = ""
                    }else if (!TextUtils.isEmpty(item.shortNickName)&&item.shortNickName.contains(mKeyword.toUpperCase())) {
                        lightWord = FastPinyin.findWordFromPinyinToFirst(mKeyword,item.nickName)
                        name = item.nickName
                        secondName = ""
                    } else if (!TextUtils.isEmpty(item.nickName)&&item.nickName.contains(mKeyword)) {
                        name = item.displayName
                        secondHeadName = view.context.getString(R.string.string_search_name) + " "
                        secondName = item.nickName
                    } else if (!TextUtils.isEmpty(item.searchPhone)&&item.searchPhone.contains(mKeyword)) {
                        name = item.displayName
                        secondHeadName = view.context.getString(R.string.string_search_phone) + " "
                        secondName = item.phone
                    } else if (!TextUtils.isEmpty(item.identify)&&item.identify.contains(mKeyword)) {
                        name = item.displayName
                        secondHeadName = view.context.getString(R.string.string_search_68) + " "
                        secondName = item.identify
                    } else {
                        secondName = ""
                    }

                    try {
                        helper.getView<AppTextView>(R.id.app_text_view_name)?.text = StringUtil.setHitTextColor(lightWord, name)
                        helper.getView<TextView>(R.id.app_head_name)?.text = secondHeadName
                        helper.getView<AppTextView>(R.id.app_text_view_name_2)?.text = StringUtil.setHitTextColor(lightWord, secondName)
                        helper.setGone(R.id.layout_other, !TextUtils.isEmpty(secondName))
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    if (item.uid == FILE_TRANSFER_UID){
                        helper.getView<AppImageView>(R.id.image_view_icon)?.setImageURI(UriUtils.newWithResourceId(R.drawable.transmission_assistant))
                    }else{
                        helper.getView<AppImageView>(R.id.image_view_icon)?.setImageURI(item.icon)
                    }

                    helper.itemView.setOnClickListener {
                        when (mSearchType) {
                            SEARCH_CONTACTS -> {
                                ARouter.getInstance().build(ROUNTE_MSG_PRIVATE_CHAT_ACTIVITY).withLong("targetUid", item.uid).navigation()
                            }
                            SEARCH_CONTACTS_EXPAND -> {
                                ARouter.getInstance().build(ROUNTE_MSG_PRIVATE_CHAT_ACTIVITY).withLong("targetUid", item.uid).navigation()
                            }
                            Constant.Search.SEARCH_CHAT -> {
                                ARouter.getInstance().build(ROUNTE_MSG_PRIVATE_CHAT_ACTIVITY).withLong("targetUid", item.uid).navigation()
                            }
                        }
                    }
                }
            }
        }
    }
}

