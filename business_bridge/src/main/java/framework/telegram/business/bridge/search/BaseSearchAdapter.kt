package framework.telegram.business.bridge.search

import com.chad.library.adapter.base.BaseViewHolder
import com.chad.library.adapter.base.entity.MultiItemEntity
import framework.telegram.business.bridge.service.ISearchAdapterService
import framework.telegram.ui.doubleclick.recycler.AppBaseMultiItemQuickAdapter

/**
 * Created by lzh on 19-6-17.
 * INFO:
 */
abstract class BaseSearchAdapter<T : MultiItemEntity, K : BaseViewHolder>(data: List<T>?) : AppBaseMultiItemQuickAdapter<T, K>(data) , ISearchAdapterService {
    companion object{
        const  val SEARCH_USER_NAME = "name"
        const  val SEARCH_USER_ID = "id"
        const  val SEARCH_USER_ICON = "icon"
    }

    private val mLayouts by lazy { HashMap<Int, Int>() }

    init {
        addItems()
        getLayouts().forEach {
            addItemType(it.key, it.value)
        }
    }

    override fun convert(helper: K, item: T) {
        convertImpl(helper, item)
    }

    fun getLayouts() :HashMap<Int, Int> {
        return  mLayouts
    }

    fun putLayout(key:Int,value:Int){
        mLayouts[key] = value
    }

}


