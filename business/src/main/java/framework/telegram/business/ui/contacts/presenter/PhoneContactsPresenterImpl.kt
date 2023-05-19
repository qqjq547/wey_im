package framework.telegram.business.ui.contacts.presenter

import android.content.Context
import android.database.Cursor
import android.provider.ContactsContract
import android.text.TextUtils
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.im.domain.pb.ContactsProto
import com.trello.rxlifecycle3.android.ActivityEvent
import framework.telegram.business.ArouterServiceManager
import framework.telegram.business.R
import framework.telegram.business.http.HttpManager
import framework.telegram.business.http.creator.ContactsHttpReqCreator
import framework.telegram.business.http.getResult
import framework.telegram.business.http.protocol.FriendHttpProtocol
import framework.telegram.business.ui.contacts.bean.IPhoneContactInfo
import framework.telegram.business.ui.contacts.bean.PhoneContactsBean
import framework.telegram.business.ui.contacts.bean.PhoneContactsBean.Companion.ITEM_HEAD
import framework.telegram.business.ui.contacts.bean.PhoneContactsInfo
import framework.telegram.support.BaseApp
import framework.telegram.support.account.AccountManager
import framework.telegram.support.system.network.http.HttpReq
import framework.telegram.support.system.storage.sp.core.IdeasPreference
import framework.telegram.support.tools.ExpandClass.toast
import framework.telegram.support.tools.ThreadUtils
import io.reactivex.Observable

class PhoneContactsPresenterImpl : PhoneContactsContract.Presenter {

    private val mUploadedContact by lazy { IdeasPreference().create(BaseApp.app, IPhoneContactInfo::class.java,AccountManager.getLoginAccountUUid()) }

    private var mContext: Context?
    private var mView: PhoneContactsContract.View
    private var mObservalbe: Observable<ActivityEvent>
    private val mPageSize = 100//每一次上传的pagesize大小

    constructor(view: PhoneContactsContract.View, context: Context?, observable: Observable<ActivityEvent>) {
        this.mView = view
        this.mObservalbe = observable
        this.mContext = context
        view.setPresenter(this)
    }

    override fun start() {
    }

    override fun updateData() {

        ThreadUtils.runOnIOThread {
            var page = 1
            var cursor: Cursor? = null
            var phoneCursor: Cursor? = null
            try {
                // 查询联系人数据
                val saveContact = dataToGson(mUploadedContact.getPhoneList())
                val uploadContacts = mutableListOf<PhoneContactsInfo>()
                val localContacts = mutableListOf<PhoneContactsInfo>()

                cursor = mContext?.contentResolver?.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null)
                while (cursor != null && cursor.moveToNext() ) {
                    if (null != phoneCursor && !phoneCursor.isClosed) {
                        phoneCursor.close()
                    }
                    // 获取联系人的Id
                    val contactId = cursor.getLong(cursor.getColumnIndex(ContactsContract.Contacts._ID))
                    // 获取联系人的姓名
                    val contactName = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME))
                    phoneCursor = mContext?.contentResolver?.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + "=" + contactId, null, null)
                    while (phoneCursor != null && phoneCursor.moveToNext()) {
                        var phoneNumber = phoneCursor.getString(phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))

                        if (!TextUtils.isEmpty(phoneNumber)) {
                            phoneNumber = phoneNumber.replace(" ", "")
                            phoneNumber = phoneNumber.replace("-", "")
                            phoneNumber = phoneNumber.replace("+55", "")
                        }

                        val contactBean = PhoneContactsInfo(contactName, phoneNumber, false)
                        localContacts.add(contactBean)
                    }
                }

                uploadContacts.addAll(getDiffContacts(saveContact, localContacts, true))
                uploadContacts.addAll(getDiffContacts(localContacts, saveContact, false))

                mUploadedContact.putPhoneList(gsonToData(localContacts))

                if (uploadContacts.size>0){
                    val bfFirst = saveContact.size <= 0
                    val uploadList =  upload(uploadContacts)
                    uploadContactByOne(bfFirst,0,uploadList)
                }else{
                    getContactsList(1, 20)
                }
            } catch (e: SecurityException) {
                e.printStackTrace()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                if (null != cursor && !cursor.isClosed) {
                    cursor.close()
                }

                if (null != phoneCursor && !phoneCursor.isClosed) {
                    phoneCursor.close()
                }
            }
        }
    }

    private fun uploadContactByOne(bfFirst :Boolean, index:Int,uploadList :MutableList<MutableList<PhoneContactsInfo> >){
        var bfEnd = false
        if (index == uploadList.size -1){
            bfEnd = true
        }
        uploadContactsList(bfFirst, bfEnd, uploadList[index],{
            uploadContactByOne(bfFirst,index+1,uploadList)
        }) {

        }
    }

    private fun upload(list: MutableList<PhoneContactsInfo>):MutableList<MutableList<PhoneContactsInfo> > {
        val uploadList = mutableListOf<MutableList<PhoneContactsInfo> >()
        var size = 0
        while (list.size > size) {
            var result: MutableList<PhoneContactsInfo>? = null
            if (list.size > size + mPageSize) {
                result = list.subList(size, size + mPageSize)
            } else {
                result = list.subList(size, list.size)
            }
            uploadList.add(result)

            size += mPageSize
        }
        return uploadList
    }

    private fun getDiffContacts(list1: MutableList<PhoneContactsInfo>, list2: MutableList<PhoneContactsInfo>, flat: Boolean): MutableList<PhoneContactsInfo> {
        val result = mutableListOf<PhoneContactsInfo>()
        list1.forEach { info1 ->
            var isFind = false
            list2.forEach { info2 ->
                if (info1.phone.equals(info2.phone) && info1.nickName.equals(info2.nickName)) {
                    isFind = true
                }
            }
            if (!isFind) {
                info1.flag = flat
                result.add(info1)
            }
        }
        return result
    }


    private fun uploadContactsList(bfFirst: Boolean, bfEnd: Boolean, list: MutableList<PhoneContactsInfo>, success: (() -> Unit),error: (() -> Unit)) {
        val updateList = mutableListOf<ContactsProto.UploadMobileParam>()
        list.forEach {
            val data = ContactsProto.UploadMobileParam.newBuilder().setMobileName(it.nickName).setFlag(it.flag).setPhone(it.phone).build()
            updateList.add(data)
        }
        HttpManager.getStore(FriendHttpProtocol::class.java)
                .uploadContacts(object : HttpReq<ContactsProto.UploadContactsReq>() {
                    override fun getData(): ContactsProto.UploadContactsReq {
                        return ContactsHttpReqCreator.uploadContacts(bfFirst, bfEnd, updateList)
                    }
                })
                .getResult(mObservalbe, {
                    if (bfEnd) {
                        getContactsList(1, 20)
                    }else{
                        success.invoke()
                    }
                }, {
                    //请求失败
                    mContext?.toast(mContext?.getString(R.string.common_fail).toString())
                    error.invoke()
                    getContactsList(1, 20)
                })
    }

    override fun getContactsList(pageNum: Int, pageSize: Int) {
        HttpManager.getStore(FriendHttpProtocol::class.java)
                .getMobileContacts(object : HttpReq<ContactsProto.MobileContactsReq>() {
                    override fun getData(): ContactsProto.MobileContactsReq {
                        return ContactsHttpReqCreator.getMobileContacts(pageNum, pageSize)
                    }
                })
                .getResult(mObservalbe, {
                    val agreeDataList = mutableListOf<PhoneContactsBean>()
                    val unAuditDataList = mutableListOf<PhoneContactsBean>()
                    it.contactsListList.forEach { resultInfo ->
                        if (resultInfo.bfAdd) {
                            agreeDataList.add(PhoneContactsBean(PhoneContactsBean.ITEM_AGREE, resultInfo))
                        } else {
                            unAuditDataList.add(PhoneContactsBean(PhoneContactsBean.ITEM_UNAUDIT, resultInfo))
                        }
                    }
                    val dataList = mutableListOf<PhoneContactsBean>()
                    if (unAuditDataList.size > 0) {
                        dataList.add(PhoneContactsBean(ITEM_HEAD, mContext?.getString(R.string.no_friends_added)?:""))
                        dataList.addAll(unAuditDataList)
                    }
                    if (agreeDataList.size > 0) {
                        dataList.add(PhoneContactsBean(ITEM_HEAD, mContext?.getString(R.string.friends_added)?:""))
                        dataList.addAll(agreeDataList)
                    }
                    mView.refreshListUI(pageNum + 1, dataList)
                }, {
                    //请求失败
                    mView.showError(it.message)
                })
    }

    override fun makeFriend(applyUid: Long, op: ContactsProto.ContactsOperator) {
        when (op) {
            ContactsProto.ContactsOperator.ADD_REQ -> {
                ArouterServiceManager.contactService.agreeContactReq(mObservalbe, applyUid, {

                }, {
                    mView.showError(mContext?.getString(R.string.common_fail) + "  " + it.message)
                })
            }
            else -> {
                ArouterServiceManager.contactService.deleteContactReq(mObservalbe, applyUid, {

                }, {
                    mView.showError(mContext?.getString(R.string.common_fail) + "  " + it.message)
                })
            }
        }
    }


    private fun gsonToData(value: MutableList<PhoneContactsInfo>): String {
        return Gson().toJson(value)
    }

    private fun dataToGson(value: String): MutableList<PhoneContactsInfo> {
        if (TextUtils.isEmpty(value))
            return mutableListOf()
        val listType = object : TypeToken<MutableList<PhoneContactsInfo>>() {}.type
        return Gson().fromJson(value, listType)

    }
}

