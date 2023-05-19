package framework.telegram.business.ui.search.adapter

import com.chad.library.adapter.base.BaseViewHolder
import com.chad.library.adapter.base.entity.MultiItemEntity
import framework.telegram.business.bridge.service.ISearchAdapterService
import framework.telegram.ui.doubleclick.recycler.AppBaseMultiItemQuickAdapter

/**
 * Created by lzh on 19-6-14.
 * INFO:
 */
class SearchAdapter
(private val mAdapterList: MutableList<ISearchAdapterService>) : AppBaseMultiItemQuickAdapter<MultiItemEntity, BaseViewHolder>(null) {

    init {
        mAdapterList.forEach { service ->
            service.getAdapter().getLayouts().forEach {
                addItemType(it.key, it.value)
            }
        }
    }

    override fun convert(helper: BaseViewHolder, item: MultiItemEntity?) {
        mAdapterList.forEach { service ->
            if (service.getAdapter().getLayouts().keys.contains(item?.itemType)) {
                service.convertImpl(helper!!, item!!)
                return
            }
        }
    }

    fun setKeyword(keyword:String){
        mAdapterList.forEach { service ->
            service.setKeyword(keyword)
        }
    }

    fun setExtra(mapList:List<MutableMap<String,String>>){
        mAdapterList.forEach { service ->
            service.setExtra(mapList)
        }
    }

    fun setSearchTargerId(targetId :Long){
        mAdapterList.forEach { service ->
            service.setSearchTargetId(targetId)
        }
    }

    fun setSearchType(searchType:Int){
        mAdapterList.forEach { service ->
            service.setSearchType(searchType)
        }
    }

    fun setDataSet(dataSet:Set<Long>){
        mAdapterList.forEach { service ->
            service.setExtra(dataSet)
        }
    }
}