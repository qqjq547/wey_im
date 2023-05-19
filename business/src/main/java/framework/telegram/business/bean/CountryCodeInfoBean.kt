package framework.telegram.business.bean

import android.text.TextUtils
import com.chad.library.adapter.base.entity.MultiItemEntity
import framework.telegram.business.bridge.Constant
import framework.telegram.support.tools.language.LocalManageUtil
import java.io.Serializable
import java.util.*


/**
 * Created by zst on 16/9/26.
 */
class CountryCodeInfoBean : Serializable, MultiItemEntity {
    private var mItemType: Int = Constant.Search.SEARCH_COUNTRY

    private var countryCode: String? = null
    private var countryName: String? = null
    var countryNameUS: String? = null
    private var countryNameTW: String? = null
    private var pinYinHead: String? = null
    private var englishHead: String? = null
    private var strokeCount: Int = 0//笔画数

    class ComparatorUS : Comparator<CountryCodeInfoBean> {
        override fun compare(obj1: CountryCodeInfoBean, obj2: CountryCodeInfoBean): Int {
            return obj1.englishHead!!.compareTo(obj2.englishHead!!)
        }
    }

    class ComparatorTW : Comparator<CountryCodeInfoBean> {
        override fun compare(obj1: CountryCodeInfoBean, obj2: CountryCodeInfoBean): Int {
            return obj1.strokeCount - obj2.strokeCount
        }
    }

    class ComparatorCN : Comparator<CountryCodeInfoBean> {
        override fun compare(obj1: CountryCodeInfoBean, obj2: CountryCodeInfoBean): Int {
            return obj1.pinYinHead!!.compareTo(obj2.pinYinHead!!, ignoreCase = true)
        }
    }

    /**
     * 根据语言获取国家名称(与app设置里的语言挂钩)
     */
    fun getNameByLanguage(): String? {
        return when (LocalManageUtil.getCurLanguaue()) {
            LocalManageUtil.SIMPLIFIED_CHINESE -> {
                countryName
            }
            LocalManageUtil.TRADITIONAL_CHINESE -> {
                countryNameTW
            }
            else -> {
                countryNameUS
            }
        }
    }

    fun getLetterByLanguage(): String? {
        return when (LocalManageUtil.getCurLanguaue()) {
            LocalManageUtil.SIMPLIFIED_CHINESE -> {
                pinYinHead
            }
            LocalManageUtil.TRADITIONAL_CHINESE -> {
                pinYinHead
            }
            else -> {
                englishHead
            }
        }
    }

    fun getCountryCode(): String {
        return if (TextUtils.isEmpty(countryCode)) "86" else countryCode!!.replace("+", "")
    }

    fun setItemType(type: Int) {
        this.mItemType = type
    }

    override fun getItemType() = mItemType


    companion object {
        private const val serialVersionUID = -3611096875797761452L

        fun copyTo(target: CountryCodeInfoBean, oldBean: CountryCodeInfoBean) {
            target.countryNameUS = oldBean.countryNameUS
            target.countryCode = oldBean.countryCode
            target.countryName = oldBean.countryName
            target.countryNameTW = oldBean.countryNameTW
            target.pinYinHead = oldBean.pinYinHead
            target.englishHead = oldBean.englishHead
            target.strokeCount = oldBean.strokeCount
        }
    }
}
