package framework.telegram.business.services

import android.content.Context
import android.util.Log
import com.alibaba.android.arouter.facade.annotation.Route
import framework.ideas.common.model.common.SearchChatModel
import framework.telegram.business.bridge.Constant
import framework.telegram.business.bridge.service.ISearchService
import framework.telegram.business.ui.search.db.Constant.ROW_MSG_ID
import framework.telegram.business.ui.search.db.SearchDbManager
import framework.telegram.support.tools.ThreadUtils

@Route(path = Constant.ARouter.ROUNTE_SERVICE_SEARCH, name = "设置服务")
class SearchServiceImpl : ISearchService {

    override fun insertMessage(searchModels: List<SearchChatModel>,progressCall:((SearchChatModel)->Unit)?, finishCallback: (() -> Unit)?) {
        var lastChatId = 0L
        var lastModel :SearchChatModel ?= null
        ThreadUtils.runOnIOThread {
            searchModels.forEachIndexed{progress,model ->
                SearchDbManager.inertData(model.chatId,model.chatType,model.chatContent,model.msgId,model.msgLocalId,model.msgTime,model.msgType,model.senderId)

                if (progressCall!=null){
                    if (progress != 0 && progress % 200 == 0){
                        callProgress(model,progressCall)
                        Log.i("lzh","size  ${searchModels.size}  chatId ${model.chatId}  ${model.msgTime}")
                    }else if(lastChatId!=0L && lastChatId!=model.chatId){
                        callProgress(lastModel,progressCall)
                        Log.i("lzh","size  ${searchModels.size}  chatId ${model.chatId}  ${model.msgTime}")
                    }else if (progress == searchModels.size -1){
                        callProgress(model,progressCall)
                        Log.i("lzh","size  ${searchModels.size}  chatId ${model.chatId}  ${model.msgTime}")
                    }
                    lastChatId = model.chatId
                    lastModel = model
                }
            }
            finishCallback?.let {callback->
                ThreadUtils.runOnUIThread {
                    callback.invoke()
                }
            }
        }
    }

    private fun callProgress(model:SearchChatModel?,progressCall:((SearchChatModel)->Unit)?){
        progressCall?.let { callback->
            ThreadUtils.runOnUIThread {
                if (model!=null)
                    callback.invoke(model)
            }

        }
    }

    override fun deleteMessage(msgIds: List<Long>) {
        ThreadUtils.runOnIOThread {
            msgIds.forEach {
                SearchDbManager.delete("$ROW_MSG_ID = ?", arrayOf("$it"))
            }
        }
    }

    override fun init(context: Context?) {
    }
}