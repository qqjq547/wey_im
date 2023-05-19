package framework.telegram.business.ui.contacts

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.chad.library.adapter.base.BaseViewHolder
import com.im.domain.pb.CommonProto
import com.im.domain.pb.ContactsProto
import framework.telegram.business.R
import framework.telegram.business.bridge.Constant
import framework.telegram.business.bridge.Constant.ARouter_Key.KEY_ADD_FRIEND_FROM
import framework.telegram.business.bridge.Constant.ARouter_Key.KEY_TARGET_PHONE
import framework.telegram.business.bridge.Constant.ARouter_Key.KEY_TARGET_UID
import framework.telegram.business.ui.base.BaseBusinessActivity
import framework.telegram.support.mvp.BasePresenter
import framework.telegram.ui.doubleclick.recycler.AppBaseQuickAdapter
import framework.telegram.ui.image.AppImageView
import framework.telegram.ui.text.AppTextView
import kotlinx.android.synthetic.main.bus_contacts_activity_search_list.*
import kotlinx.android.synthetic.main.bus_recycler_inside_linear_with_toolbar.custom_toolbar

/**
 * Created by lzh on 19-5-27.
 * INFO:新朋友 搜索列表
 */
@Route(path = Constant.ARouter.ROUNTE_BUS_CONTACTS_SEARCH_FRIEND_LIST)
class SearchContactListActivity : BaseBusinessActivity<BasePresenter>() {

    companion object {
        val KEY_FRIEND_LIST = "key_friend_list"
    }

    override fun getLayoutId() = R.layout.bus_contacts_activity_search_list

    private val mAdapter by lazy { SearchContactAdapter() }

    private val mList: ContactsProto.FindContactsListResp? by lazy {
        intent.getSerializableExtra(
            KEY_FRIEND_LIST
        ) as ContactsProto.FindContactsListResp?
    }

    private val defaultAddType by lazy {
        intent.getSerializableExtra(
            KEY_ADD_FRIEND_FROM
        ) as ContactsProto.ContactsAddType?
    }

    override fun initView() {
        custom_toolbar.setBackIcon(R.drawable.common_icon_black_back) {
            finish()
        }
        custom_toolbar.showCenterTitle(getString(R.string.search_result))
        common_recycler?.initSingleTypeRecycleView(LinearLayoutManager(this), mAdapter, false)
        common_recycler.refreshController().setEnablePullToRefresh(false)

        mAdapter.setOnItemChildClickListener { adapter, _, position ->
            val data = adapter.data[position]
            if (data is CommonProto.ContactsDetailBase) {
                ARouter.getInstance().build(Constant.ARouter.ROUNTE_BUS_CONTACT_DETAIL)
                    .withString(KEY_TARGET_PHONE, data.phone)
                    .withString(Constant.ARouter_Key.KEY_ADD_TOKEN, data.addToken)
                    .withSerializable(
                        KEY_ADD_FRIEND_FROM,
                        if (defaultAddType == null) ContactsProto.ContactsAddType.forNumber(data.searchType)
                            ?: ContactsProto.ContactsAddType.PHONE else defaultAddType
                    )
                    .withLong(KEY_TARGET_UID, data.userInfo.uid).navigation()
            }
        }

        mAdapter.setNewData(mList?.detailListList)
    }

    override fun initListen() {
    }

    override fun initData() {
    }

    class SearchContactAdapter :
        AppBaseQuickAdapter<CommonProto.ContactsDetailBase, BaseViewHolder>(R.layout.bus_contacts_item) {

        override fun convert(helper: BaseViewHolder, item: CommonProto.ContactsDetailBase) {
            helper.getView<AppTextView>(R.id.app_text_view_name)?.text = item.userInfo.nickName
            helper.getView<AppImageView>(R.id.image_view_icon)?.setImageURI(item.userInfo.icon)
            helper.getView<TextView>(R.id.text_view_online_status)?.visibility = View.VISIBLE
            helper.getView<TextView>(R.id.text_view_online_status)?.text = item.phone
            helper.addOnClickListener(R.id.all_relative_layout)
        }
    }
}