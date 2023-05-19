package framework.telegram.business.bridge.service

import com.alibaba.android.arouter.facade.template.IProvider
import com.chad.library.adapter.base.BaseViewHolder
import com.chad.library.adapter.base.entity.MultiItemEntity
import framework.telegram.business.bridge.search.BaseSearchAdapter

/**
 * Created by lzh on 19-5-28.
 * INFO: 搜索的adapter 参考 SearchContactsAdapter
 */
interface ISearchAdapterService : IProvider {

    fun addItems()

    fun convertImpl(helper: BaseViewHolder, item: MultiItemEntity)

    fun getAdapter(): BaseSearchAdapter<MultiItemEntity,BaseViewHolder>

    fun setKeyword(keyword:String)

    fun setSearchType(searchType:Int)

    fun setSearchTargetId(targerId:Long)

    fun setExtra(mapList:List<MutableMap<String,String>>)

    fun setExtra(dataSet:Set<Long>)

}