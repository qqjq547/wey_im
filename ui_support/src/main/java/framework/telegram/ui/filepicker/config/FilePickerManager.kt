package framework.telegram.ui.filepicker.config


import android.app.Activity
import android.webkit.MimeTypeMap
import androidx.fragment.app.Fragment
import java.lang.ref.WeakReference
import java.lang.Exception as Exception1

/**
 *
 * @author rosu
 * @date 2018/11/22
 */
object FilePickerManager {


    /**
     * 启动 Launcher Activity 所需的 request code
     */
    const val REQUEST_CODE = 10401

    internal var context: WeakReference<Activity>? = null
    internal var fragment: WeakReference<Fragment>? = null
    internal var config: FilePickerConfig? = null

    fun from(activity: Activity): FilePickerConfig {
        this.context = WeakReference(activity)
        this.config = FilePickerConfig(this)
        return config!!
    }

    /**
     * 不能使用 fragment.getContext()，因为无法保证外部的代码环境
     */
    fun from(fragment: Fragment): FilePickerConfig {
        this.fragment = WeakReference(fragment)
        this.context = WeakReference(fragment.activity!!)
        return config!!
    }

    private var dataList: List<String> = ArrayList()

    /**
     * 保存数据@param list List<String>到本类中
     */
    fun saveData(list: List<String>) {
        dataList = list
    }

    /**
     * 供调用者获取结果
     *
     */
    fun obtainData(): ArrayList<HashMap<String, Any>> {
        val list: ArrayList<HashMap<String, Any>> = arrayListOf()
        for (i in dataList.indices){
            val hashMap = HashMap<String, Any>(2)
            //0为文件路径
            //1为文件类型
            try {
                hashMap["mimetype"] = MimeTypeMap.getSingleton().getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(dataList[i])) as String
            } catch (e: Exception1) {
                hashMap["mimetype"] = ""
            }
            hashMap["path"] = dataList[i]
            list.add(hashMap)
        }
        return list
    }
}