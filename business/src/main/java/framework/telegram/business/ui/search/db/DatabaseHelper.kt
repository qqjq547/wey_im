package framework.telegram.business.ui.search.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteDatabase.CursorFactory
import android.database.sqlite.SQLiteOpenHelper
import framework.telegram.business.ui.search.db.Constant

class DatabaseHelper(context: Context, name: String, factory: CursorFactory?, version: Int) :
    SQLiteOpenHelper(context, name, factory, version) {

    override fun onCreate(db: SQLiteDatabase) {
        //创建数据库sql语句 并 执行
//        val sql = "create table user(name varchar(20))"
//        val sql  = "CREATE VIRTUAL TABLE pages USING fts4(name)"tokenize= porter
        val sql  = "CREATE VIRTUAL TABLE IF NOT EXISTS  ${Constant.DATA_TABLE} USING fts4(" +
                "${Constant.ROW_CHAT_ID} ," +
                "${Constant.ROW_CHAT_TYPE} ," +
                "${Constant.ROW_CHAT_CONTENT} ," +
                "${Constant.ROW_MSG_ID} ," +
                "${Constant.ROW_MSG_LOCAL_ID} ," +
                "${Constant.ROW_MSG_TIME} ," +
                "${Constant.ROW_MSG_TYPE} ,"
                "${Constant.ROW_INDEX_ID} ,"
                "tokenize "+"=icu zh_CN)"
        db.execSQL(sql)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {

    }

}