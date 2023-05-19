package framework.telegram.business.ui.contacts

import android.text.TextUtils
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.im.domain.pb.CommonProto
import com.im.domain.pb.ContactsProto
import framework.telegram.business.bridge.Constant
import framework.telegram.business.bridge.Constant.ARouter_Key.KEY_ADD_FRIEND_FROM
import framework.telegram.business.bridge.Constant.ARouter_Key.KEY_TARGET_PHONE
import framework.telegram.business.bridge.Constant.ARouter_Key.KEY_TARGET_UID
import framework.telegram.business.R
import framework.telegram.business.bridge.Constant.ARouter_Key.KEY_ADD_TOKEN
import framework.telegram.business.ui.base.BaseBusinessActivity
import framework.telegram.business.ui.contacts.SearchContactListActivity.Companion.KEY_FRIEND_LIST
import framework.telegram.business.ui.contacts.presenter.SearchContactContract
import framework.telegram.business.ui.contacts.presenter.SearchContactPresenterImpl
import framework.telegram.business.utils.ValidationUtils
import framework.telegram.support.mvp.BasePresenter
import framework.telegram.support.tools.ExpandClass.toast
import framework.telegram.ui.dialog.AppDialog
import kotlinx.android.synthetic.main.bus_contacts_activity_search_friend.*
import kotlinx.android.synthetic.main.bus_recycler_inside_linear_with_toolbar.custom_toolbar

/**
 * Created by lzh on 19-5-27.
 * INFO:新朋友
 */
@Route(path = Constant.ARouter.ROUNTE_BUS_CONTACTS_SEARCH_FRIEND)
class SearchContactActivity : BaseBusinessActivity<SearchContactContract.Presenter>(),
    SearchContactContract.View {

    var phoneNumber = ""

    override fun getLayoutId() = R.layout.bus_contacts_activity_search_friend

    override fun initView() {
        custom_toolbar.setBackIcon(R.drawable.common_icon_black_back) {
            finish()
        }

        custom_toolbar.showCenterTitle(getString(R.string.bus_contacts_search_friend))
    }

    override fun initListen() {
        text_view_search.setOnClickListener {
            phoneNumber = edit_text_search.text.trim().toString()
            mPresenter?.getDataList(phoneNumber)
        }
    }

    override fun initData() {
        SearchContactPresenterImpl(this, this, lifecycle()).start()
    }

    override fun showLoading() {
        dialog = AppDialog.showLoadingView(this@SearchContactActivity, this@SearchContactActivity)
    }

    override fun showEmpty(str: String?) {
        toast(str ?: "")
        dialog?.dismiss()
    }

    override fun setPresenter(presenter: BasePresenter) {
        mPresenter = presenter as SearchContactPresenterImpl
    }

    override fun isActive(): Boolean {
        return true
    }

    override fun showError(errStr: String?) {
        toast(errStr ?: "")
        dialog?.dismiss()
    }

    override fun refreshUI(result: ContactsProto.FindContactsListResp) {
        dialog?.dismiss()

        if (result.detailListCount == 1) {
            val firstResult = result.getDetailList(0)
            ARouter.getInstance().build(Constant.ARouter.ROUNTE_BUS_CONTACT_DETAIL)
                .withString(KEY_TARGET_PHONE, phoneNumber)
                .withString(KEY_ADD_TOKEN, firstResult.addToken)
                .withSerializable(
                    KEY_ADD_FRIEND_FROM,
                    ContactsProto.ContactsAddType.forNumber(firstResult.searchType)
                        ?: ContactsProto.ContactsAddType.PHONE
                )
                .withLong(KEY_TARGET_UID, firstResult.userInfo.uid).navigation()
        } else {
            ARouter.getInstance().build(Constant.ARouter.ROUNTE_BUS_CONTACTS_SEARCH_FRIEND_LIST)
                .withSerializable(KEY_ADD_FRIEND_FROM, ContactsProto.ContactsAddType.REQ_MSG)
                .withSerializable(KEY_FRIEND_LIST, result).navigation()
        }
    }
}