package framework.telegram.business.utils

/**
 * Created by lzh on 19-6-6.
 * INFO:
 */

import android.text.TextUtils
import framework.telegram.business.bean.CountryCodeInfoBean
import framework.telegram.business.bridge.Constant
import framework.telegram.support.BaseApp
import framework.telegram.support.system.gson.GsonInstanceCreater
import framework.telegram.support.tools.IoUtils
import framework.telegram.support.tools.language.LocalManageUtil
import java.util.*

object CountryUtil {

    fun getCountryListSync(): List<CountryCodeInfoBean>? {
        return try {
            GsonInstanceCreater.defaultGson.fromJson<Array<CountryCodeInfoBean>>(
                String(IoUtils.readAllBytesAndClose(BaseApp.app.assets.open("countryCode.json"))),
                Array<CountryCodeInfoBean>::class.java
            ).toMutableList()
        } catch (e: Exception) {
            e.printStackTrace()
            ArrayList()
        }
    }

    fun populateAsync(
        countryListBeans: List<CountryCodeInfoBean>?,
        keyword: String?,
        withoutSection: Boolean = false,
        error: (() -> Unit),
        success: ((List<CountryCodeInfoBean>?) -> Unit)
    ) {
        run {
            if (countryListBeans != null) {
                if (!TextUtils.isEmpty(keyword)) {
                    val kw = keyword?.lowercase(Locale.getDefault()) ?: ""
                    val filterCountryBeans = ArrayList<CountryCodeInfoBean>()
                    for (item in countryListBeans) {
                        if (item.getNameByLanguage()?.lowercase(Locale.getDefault())
                                ?.contains(kw) == true || item.getCountryCode()
                                .contains(kw)
                        ) {
                            filterCountryBeans.add(item)
                        }
                    }
                    populate(ArrayList(filterCountryBeans), withoutSection, error, success)
                } else {
                    populate(ArrayList(countryListBeans), withoutSection, error, success)
                }
            }
        }
    }


    private fun populate(
        countryListBeans: List<CountryCodeInfoBean>,
        withoutSection: Boolean = false,
        error: (() -> Unit),
        success: ((List<CountryCodeInfoBean>?) -> Unit)
    ) {
        if (countryListBeans.isNotEmpty()) {
            sortByLanguage(countryListBeans)
            if (withoutSection) {
                success.invoke(countryListBeans)
                return
            }
            val dataList = mutableListOf<CountryCodeInfoBean>()
            var prevSection = ""
            for (i in countryListBeans.indices) {
                val currentItem = countryListBeans[i]
                val currentSection = currentItem.getLetterByLanguage() ?: ""
                if (prevSection != currentSection) {
                    val copyItem = CountryCodeInfoBean()
                    CountryCodeInfoBean.copyTo(copyItem, currentItem)
                    copyItem.itemType = Constant.Search.SEARCH_HEAD
                    dataList.add(copyItem)
                    prevSection = currentSection
                }
                currentItem.itemType = Constant.Search.SEARCH_COUNTRY
                dataList.add(currentItem)
            }
            success.invoke(dataList)
        } else {
            error.invoke()
        }
    }

    //根據語言排序
    private fun sortByLanguage(countryListBeans: List<CountryCodeInfoBean>) {
        when (LocalManageUtil.getCurLanguaue()) {
            LocalManageUtil.SIMPLIFIED_CHINESE -> {
                Collections.sort(countryListBeans, CountryCodeInfoBean.ComparatorCN())
            }
            LocalManageUtil.TRADITIONAL_CHINESE -> {
                Collections.sort(countryListBeans, CountryCodeInfoBean.ComparatorTW())
            }
            else -> {
                Collections.sort(countryListBeans, CountryCodeInfoBean.ComparatorUS())
            }
        }
    }
}

