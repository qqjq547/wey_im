package framework.telegram.business.ui.me.presenter

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.text.TextUtils
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.trello.rxlifecycle3.android.ActivityEvent
import framework.telegram.business.R
import framework.telegram.business.bridge.Constant
import framework.telegram.business.ui.base.BaseBusinessActivity
import framework.telegram.business.ui.me.bean.ContactsBean
import framework.telegram.business.ui.me.bean.ContactsBean.Companion.ITEM_HEAD
import framework.telegram.business.ui.me.bean.ContactsBean.Companion.ITEM_INFO
import framework.telegram.support.system.pinyin.FastPinyin
import framework.telegram.support.tools.ExpandClass.toast
import framework.telegram.support.tools.ThreadUtils
import io.reactivex.Observable

class ContactsPresenterImpl(
    activity: BaseBusinessActivity<ContactsContract.Presenter>,
    view: ContactsContract.View,
    observable: Observable<ActivityEvent>
) : ContactsContract.Presenter {

    val mActivity: BaseBusinessActivity<ContactsContract.Presenter> = activity
    val mView: ContactsContract.View = view
    val mObservable: Observable<ActivityEvent> = observable
    val mList: ArrayList<ContactsBean> by lazy { ArrayList<ContactsBean>() }

    init {
        mView.setPresenter(this@ContactsPresenterImpl)
    }

    /**
     * 选择系统联系人目录中的某一个，然后进行跳转
     */
    override fun sendSMS() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!checkSysContactsAndSMSPermission())
                mActivity.requestPermissions(
                    arrayOf(Manifest.permission.READ_CONTACTS),
                    InfoPresenterImpl.REQCONTACTSPERMISSION
                )
        }
    }

    /**
     * 监听跳转回来的Intent，然后分发跳转结果
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        mView.showFinish()

        data?.data?.let {
            if (resultCode == AppCompatActivity.RESULT_OK)
                when (requestCode) {
                    InfoPresenterImpl.GETSYSCONTACTS -> {
                        val phone = getSpecialPhoneContacts(it)
                        val intent = Intent(Intent.ACTION_SENDTO)
                        intent.data = Uri.parse("smsto:$phone")
                        Log.i("lzh", Constant.Common.DOWNLOAD_HTTP_HOST)

                        val shareHost =
                            mActivity.getString(R.string.invite_to_join_68) + if (!TextUtils.isEmpty(
                                    Constant.Common.DOWNLOAD_HTTP_HOST
                                )
                            ) {
                                Constant.Common.DOWNLOAD_HTTP_HOST
                            } else {
                                "https://www.bufa.chat"
                            }
                        intent.putExtra("sms_body", shareHost)
                        mActivity.startActivity(intent)
                    }
                }
        }
    }

    /**
     * 权限的校验结果
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!checkSysContactsAndSMSPermission()) {
                mActivity.toast(mActivity.getString(R.string.no_permissions_were_obtained))
            } else if (requestCode == InfoPresenterImpl.REQCONTACTSPERMISSION) {
                getAllContacts()
            }
        }
    }


    override fun start() {
        if (!checkSysContactsAndSMSPermission()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                mActivity.requestPermissions(
                    arrayOf(Manifest.permission.READ_CONTACTS),
                    InfoPresenterImpl.REQCONTACTSPERMISSION
                )
            }
        } else {
            getAllContacts()
        }
    }


    /**
     * 校验是否拥有打开联系人目录和跳转到短信页面的权限
     */
    override fun checkSysContactsAndSMSPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return mActivity.checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        }
        return true
    }


    /**
     * 读取联系人信息
     * @param contactUri 目标联系人
     */
    private fun getSpecialPhoneContacts(contactUri: Uri): String? {
        var name = ""
        var phoneNumber = ""
        try {
            val cursor = mActivity.contentResolver.query(contactUri, null, null, null, null)
            cursor?.let {
                if (cursor.moveToFirst()) {
                    name =
                        cursor.getString(cursor.getColumnIndex(android.provider.ContactsContract.Contacts.DISPLAY_NAME))
                    var hasPhone =
                        cursor.getString(cursor.getColumnIndex(android.provider.ContactsContract.Contacts.HAS_PHONE_NUMBER))
                    val id =
                        cursor.getString(cursor.getColumnIndex(android.provider.ContactsContract.Contacts._ID))
                    hasPhone = if (hasPhone.equals("1", ignoreCase = true)) {
                        "true"
                    } else {
                        "false"
                    }
                    if (java.lang.Boolean.parseBoolean(hasPhone)) {
                        val phones = mActivity.contentResolver.query(
                            android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            android.provider.ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + id,
                            null,
                            null
                        )
                        phones?.let {
                            while (phones.moveToNext()) {
                                phoneNumber =
                                    phones.getString(phones.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER))
                            }
                            phones.close()
                        }
                    }
                    cursor.close()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return phoneNumber.trim()
    }

    override fun getAllContacts() {
        mView.showLoading()
        ThreadUtils.runOnIOThread {
            mList.clear()
            val uri = android.provider.ContactsContract.Contacts.CONTENT_URI
            val cursor = mActivity.contentResolver.query(uri, null, null, null, null)
            var i: Int = 0
            cursor?.let {
                while (cursor.moveToNext()) {
                    val id =
                        cursor.getLong(cursor.getColumnIndex(android.provider.ContactsContract.Contacts._ID))
                    val name =
                        cursor.getString(cursor.getColumnIndex(android.provider.ContactsContract.Contacts.DISPLAY_NAME))

                    val cursorPhone = mActivity.contentResolver.query(
                        android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        null,
                        android.provider.ContactsContract.CommonDataKinds.Phone.CONTACT_ID + "=" + id,
                        null,
                        null
                    )
                    cursorPhone?.let {
                        while (cursorPhone.moveToNext()) {
                            val phone =
                                cursorPhone.getString(cursorPhone.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER))
                                    .trim()
                            val bean =
                                ContactsBean(phone, name, ITEM_INFO, FastPinyin.toPinyinChar(name))
                            mList.add(bean)
                        }
                        cursorPhone.close()
                    }

                    i++
                }
            }
            cursor?.close()
            ThreadUtils.runOnUIThread {
                mView.showFinish()
                mView.refreshUI(soft(mList))
            }
        }
    }

    override fun getItem(position: Int): ContactsBean {
        return mList[position]
    }

    /**
     * 先用冒泡进行字母排序，再检索后添加首字母
     */
    private fun soft(list_in: ArrayList<ContactsBean>): ArrayList<ContactsBean> {
        if (list_in.size == 0)
            return list_in
        // 冒泡排序
        for (i in list_in.indices)
            for (j in 0 until list_in.size - 1 - i)
                if (list_in[j + 1].getTitle().toInt() < list_in[j].getTitle().toInt()) {
                    val temp = list_in[j + 1]
                    list_in[j + 1] = list_in[j]
                    list_in[j] = temp
                }
        val list_out = arrayListOf<ContactsBean>()
        for (i in list_in.indices) {
            var tmp: ContactsBean
            if (i == 0) {
                tmp = ContactsBean("", "", ITEM_HEAD, FastPinyin.toPinyinChar(list_in[i].getName()))
                list_out.add(tmp)
            } else if (list_in[i].getTitle() != list_out.last().getTitle()) {
                tmp = ContactsBean("", "", ITEM_HEAD, FastPinyin.toPinyinChar(list_in[i].getName()))
                list_out.add(tmp)
            }
            list_out.add(list_in[i])
        }
        return list_out
    }
}