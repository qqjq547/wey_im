package framework.telegram.message.manager

import android.annotation.SuppressLint
import android.text.TextUtils
import android.util.Log
import framework.ideas.common.model.common.SearchChatModel
import framework.ideas.common.model.common.SearchDBModel
import framework.ideas.common.model.im.ChatModel
import framework.ideas.common.model.im.MessageModel
import framework.telegram.message.db.RealmCreator
import io.reactivex.Flowable
import io.reactivex.schedulers.Schedulers
import io.realm.Realm

/**
 * Created by lzh on 20-4-9.
 * INFO:
 */
object SearchSyncManager {

    val msgTypeMap = mutableMapOf<Long,Int>()//旧版本的消息里面，会没有chatType ,所以，在chat拿

    /**
     * 1.在外层（bus层）会一个字段标志是不是同步过数据，同步完成才会为true
     * 2.为false 就会进来
     * 3.如果searchDBModel 为空，就代表，没初始过，就把ChatsHistory数据同步过来，之后同步数据
     * 4.如果searchDBModel 不为空，直接接着同步数据
     * 5.每同步200条，会更新一次searchDbModel的时间，避免用户杀掉应用，又得重头开始同步,下次同步，就会从大于这个时间的继续插入数据
     * 6.只有全部同步完成，即 searchDBModel 没有数据了，标志为才会返回true
     */
    fun syncAllChatMessage(myUid: Long,finish:()->Unit){
        val searchDBList = mutableListOf<SearchDBModel>()
                val searchReaml= RealmCreator.getSearchDbRealm(myUid)
        searchReaml .executeTransactionAsync({realm: Realm ->
            val list = realm.where(SearchDBModel::class.java)?.findAll()
            list?.forEach {
                searchDBList.add(it.copyModel())
            }
        },{
            searchReaml.close()
            if (searchDBList.size !=0){
                Log.i("lzh","读取消息")
                syncSearchDb(myUid,searchDBList,finish)
            }else{//是第一次进来的，从historyChat 获取chatID
                Log.i("lzh","第一次进来，建立searchDB")
                initSearchDb(myUid){
                    syncSearchDb(myUid,it,finish)
                }
            }
        }){
            Log.i("lzh","err msg1 ${it.message}")
            //todo
        }
    }

    @SuppressLint("CheckResult")
    private fun syncSearchDb(myUid: Long, allChatModels:MutableList<SearchDBModel>,finish:()->Unit){
        val allMsgList = mutableListOf<MessageModel>()

        allChatModels.forEach {
            msgTypeMap[it.chatId] = it.chatType
        }
        Flowable.fromIterable(allChatModels)
                .observeOn(Schedulers.io())
                .map {
                    val msgList = mutableListOf<MessageModel>()
                    val msgRealm = if (it.chatType == ChatModel.CHAT_TYPE_PVT){
                        RealmCreator.getPvtChatMessagesRealm(myUid,it.chatId)
                    }else{
                        RealmCreator.getGroupChatMessagesRealm(myUid,it.chatId)
                    }
                    val result = msgRealm.where(MessageModel::class.java)
                            ?.notEqualTo("type", MessageModel.MESSAGE_TYPE_RECALL)
                            ?.and()
                            ?.notEqualTo("type", MessageModel.MESSAGE_TYPE_RECALL_SUCCESS)
                            ?.equalTo("expireTime", 0L)
                            ?.greaterThan("time", it.msgTime)
                            ?.sort("time")?.findAll()
                    result?.forEach {msg->
                        msgList.add(msg.copyMessage())
                    }
                    msgRealm.close()
                    msgList
                }.subscribeOn(Schedulers.io()).subscribe ({
                    allMsgList.addAll(it)
                },{
                    //todo 同步错误
                    Log.i("lzh","err msg2 ${it.message}")
                }){
                    insertMessageToDb(myUid,allMsgList,finish)
                }
    }

    private fun initSearchDb(myUid: Long,call :((MutableList<SearchDBModel>)->Unit)){
        val realm = RealmCreator.getChatsHistoryRealm(myUid)
        val allChatModels = mutableListOf<ChatModel>()
        realm.executeTransactionAsync({ r ->
            val list = r.where(ChatModel::class.java)?.notEqualTo("chaterType", ChatModel.CHAT_TYPE_GROUP_NOTIFY)?.findAll()
            list?.forEach {
                allChatModels.add(it.copyChat())
            }
        }, {
            realm.close()
            val searchList = mutableListOf<SearchDBModel>()
            allChatModels.forEach {
                searchList.add(SearchDBModel.createModel(it.chaterId,it.chaterType,0))
            }
            call.invoke(searchList)
            val searchReaml = RealmCreator.getSearchDbRealm(myUid)
            searchReaml.executeTransactionAsync { r ->
                r.copyToRealmOrUpdate(searchList)
            }
        }){
            //todo
            Log.i("lzh","err msg3 ${it.message}")
        }
    }

    private fun insertMessageToDb(myUid: Long,allMsgList:MutableList<MessageModel>,finish:()->Unit){
        val searchList = mutableListOf<SearchChatModel>()
        allMsgList.forEach {
            if (it.snapchatTime == 0 ){
                val content = when(it.type){
                    MessageModel.MESSAGE_TYPE_NOTICE ->{
                        it.noticeMessageBean.content
                    }
                    MessageModel.MESSAGE_TYPE_TEXT ->{
                        it.content
                    }
                    MessageModel.MESSAGE_TYPE_FILE ->{
                        it.fileMessageContentBean.name
                    }
                    MessageModel.MESSAGE_TYPE_LOCATION ->{
                        it.locationMessageContentBean.address
                    }else->{ ""
                    }
                }
                if (!TextUtils.isEmpty(content)){
                    val id = if (it.isSend ==1){
                        it.targetId
                    }else{
                        it.senderId
                    }
                    val chatType = msgTypeMap[id] ?:0
                    searchList.add(SearchChatModel(id,chatType,content,it.msgId,it.id,it.time,it.type,if (chatType == ChatModel.CHAT_TYPE_PVT) id else -id,it.ownerUid))
                }
            }
        }
        ArouterServiceManager.searchService.insertMessage(searchList,{
//            Log.i("lzh","insertMessage p  ${it.chatId}  ${it.msgTime}")
            val chatType = msgTypeMap[it.chatId] ?:0
            setSearchDbTime(myUid,it.chatId,chatType,it.msgTime)
        }){
            finish.invoke()
        }
    }

    private fun setSearchDbTime(myUid: Long,chatId:Long,chatType:Int,msgTime:Long){
        RealmCreator.getSearchDbRealm(myUid).executeTransactionAsync { realm: Realm ->
            val result = realm.where(SearchDBModel::class.java)
                    .equalTo("chatId",chatId)?.equalTo("chatType",chatType)?.findFirst()
            result?.msgTime = msgTime
            result?.let {
                realm.copyToRealmOrUpdate(it)
            }
        }
    }

    @SuppressLint("CheckResult")
    private fun test(myUid: Long){
        val msgList = mutableListOf<MessageModel>()
        RealmCreator.executeGroupChatTransactionAsync(myUid, 1015, { realm ->
            val result = realm.where(MessageModel::class.java)
                    ?.notEqualTo("type", MessageModel.MESSAGE_TYPE_RECALL)
                    ?.and()
                    ?.notEqualTo("type", MessageModel.MESSAGE_TYPE_RECALL_SUCCESS)
                    ?.equalTo("expireTime", 0L)
//                            ?.greaterThan("time", it.msgTime)
                    ?.sort("time")?.findAll()
            result?.forEach {msg->
                msgList.add(msg.copyMessage())
            }
        }, {
            // 创建会话或更新会话
            msgList.forEach {
                if ("我也在线".equals(it.content)){
                    Log.i("lzh","ssssssss ${it.content}")
                }
            }

        })
    }
}