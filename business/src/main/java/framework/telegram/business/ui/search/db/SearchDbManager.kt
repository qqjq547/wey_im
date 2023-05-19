package framework.telegram.business.ui.search.db

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.os.Environment
import android.util.Log
import de.greenrobot.common.io.FileUtils
import framework.ideas.common.model.im.ChatModel
import framework.telegram.business.ArouterServiceManager
import framework.telegram.business.bridge.bean.AccountInfo
import framework.telegram.business.sp.CommonPref
import framework.telegram.business.ui.search.db.Constant.DATA_NAME
import framework.telegram.business.ui.search.db.Constant.DATA_TABLE
import framework.telegram.business.ui.search.db.Constant.DATA_VERSION
import framework.telegram.business.ui.search.db.Constant.ROW_CHAT_CONTENT
import framework.telegram.business.ui.search.db.Constant.ROW_CHAT_ID
import framework.telegram.business.ui.search.db.Constant.ROW_CHAT_TYPE
import framework.telegram.business.ui.search.db.Constant.ROW_INDEX_ID
import framework.telegram.business.ui.search.db.Constant.ROW_MSG_ID
import framework.telegram.business.ui.search.db.Constant.ROW_MSG_LOCAL_ID
import framework.telegram.business.ui.search.db.Constant.ROW_MSG_TIME
import framework.telegram.business.ui.search.db.Constant.ROW_MSG_TYPE
import framework.telegram.business.ui.search.db.Constant.ROW_SENDER_ID
import framework.telegram.support.account.AccountManager
import framework.telegram.support.system.storage.sp.SharePreferencesStorage
import org.sqlite.database.sqlite.SQLiteDatabase


/**
 * Created by lzh on 20-3-24.
 * INFO:
 */
object SearchDbManager {
    //todo  1.这里需要做数据队列  2.数据库升级的方法GroupBase
    //TODO 群成员的头像  ，聊天记录 3条的分页，所有数据的分页

    init {
        System.loadLibrary("sqliteX")
    }

    private var mSqlDb: SQLiteDatabase? = null

    private var mCurUid = 0L

    private var isSync = false

    fun initDb(context: Context){
        if (AccountManager.hasLoginAccount()) {
            val myUid = AccountManager.getLoginAccount(AccountInfo::class.java).getUserId()
            if (mSqlDb == null || myUid != mCurUid) {
                val dbFile = context.getDatabasePath("${DATA_NAME}_$myUid.db")
                dbFile.parentFile?.mkdirs()
//                if (dbFile?.exists() == true){
//                    Log.i("lzh", "path "+dbFile?.absolutePath)
//                    val file = File(Environment.getExternalStorageDirectory().path,"/zz_lzh/ss1.db")
//                    FileUtils.copyFile(dbFile?.path?:"",file.path)
//                }
                mCurUid = myUid
                try {
                    mSqlDb = SQLiteDatabase.openOrCreateDatabase(dbFile, null)
                    val sql  = "CREATE VIRTUAL TABLE IF NOT EXISTS  $DATA_TABLE USING fts5(" +
                            "$ROW_CHAT_ID ," +
                            "$ROW_CHAT_TYPE ," +
                            "$ROW_CHAT_CONTENT ," +
                            "$ROW_MSG_ID ," +
                            "$ROW_MSG_LOCAL_ID ," +
                            "$ROW_MSG_TIME ," +
                            "$ROW_MSG_TYPE ," +
                            "$ROW_INDEX_ID ," +
                            "$ROW_SENDER_ID ," +
                            "tokenize ='wcicu zh_CN');"
                    mSqlDb?.execSQL(sql)
                    mSqlDb?.version = DATA_VERSION
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
            }
            val sp = SharePreferencesStorage.createStorageInstance(CommonPref::class.java, AccountManager.getLoginAccountUUid())
            if (!sp.getSyncChatMessage() && !isSync){//
                isSync = true
                ArouterServiceManager.messageService.syncAllChatMessage(myUid){
                    Log.i("lzh","finish 333333")
                    sp.putSyncChatMessage(true)
                    isSync = false
                }
            }
        }
    }

    fun inertData(chatId:Long,chatType:Int,chatContent:String,msgId:Long,msgLocalId:Long,msgTime:Long,msgType:Int,senderId:Long){
        mSqlDb?.let {
            val values = ContentValues()
            values.put(ROW_CHAT_ID, chatId.toString())
            values.put(ROW_CHAT_TYPE, chatType.toString())
            values.put(ROW_CHAT_CONTENT, chatContent)
            values.put(ROW_MSG_ID, msgId.toString())//ROW_MSG_ID 一定要用 String 存，不然where 不到数据
            values.put(ROW_MSG_LOCAL_ID, msgLocalId)
            values.put(ROW_MSG_TIME, msgTime)
            values.put(ROW_MSG_TYPE,msgType)
            values.put(ROW_SENDER_ID,senderId)
            val indexId = if (chatType == ChatModel.CHAT_TYPE_PVT){
                chatId
            }else{
                -chatId
            }
            values.put(ROW_INDEX_ID,indexId.toString())//ROW_INDEX_ID 一定要用 String 存，不然where 不到数据
            synchronized (it) {
                try {
                    val whereClause = "$ROW_MSG_ID=? AND $ROW_INDEX_ID=?"
                    val whereArgs = arrayOf(msgId.toString(),indexId.toString())
                    val result = it.update(DATA_TABLE, values, whereClause,whereArgs)
//                    Log.i("lzh","result $result  msgId $msgId")
                    if (result == 0){
                        it.insert(DATA_TABLE, null, values)
                    }

                }catch (e:Exception){
                    //todo
                }
            }
        }
    }

    fun delete( whereClause: String?, whereArgs: Array<String>?) {
        mSqlDb?.let {
            synchronized(it) {
                try {
                    it.delete(DATA_TABLE, whereClause, whereArgs)
                }catch (e:Exception){
                    Log.i("lzh","msg  ${e.message}")
                }
            }
        }
    }

    fun query( columns: Array<String>?, selection: String?, selectionArgs: Array<String>?, groupBy: String?, having: String?, orderBy: String?,limit:String =""): Cursor ?{
        //搜索的关键字不能为“”
        if (selectionArgs?.isEmpty() == true)
            return null
        if (mSqlDb != null){
            synchronized(mSqlDb!!) {
                return try {
                    mSqlDb!!.query(DATA_TABLE, columns, selection, selectionArgs, groupBy, having, orderBy,limit)
                }catch (e:Exception){
                    Log.i("lzh","msg  ${e.message}")
                    null
                }
            }
        }else{
            return null
        }
    }

    fun update( values: ContentValues, whereClause: String, whereArgs: Array<String>) {
        mSqlDb?.let {
            synchronized(it) {
                it.update(DATA_TABLE, values, whereClause, whereArgs)
            }
        }
    }
}