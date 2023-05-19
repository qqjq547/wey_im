package framework.telegram.business.bridge.service

import com.alibaba.android.arouter.facade.template.IProvider
import framework.ideas.common.model.common.SearchChatModel

interface ISearchService : IProvider {

    fun insertMessage(searchModels:List<SearchChatModel>,progress:((SearchChatModel)->Unit)?=null,finishCallback:(()->Unit)?=null)

    /**
     * 是否可发出声音
     */
    fun deleteMessage(msgIds:List<Long>)

}