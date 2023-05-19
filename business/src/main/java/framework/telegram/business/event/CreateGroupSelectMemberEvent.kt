package framework.telegram.business.event

/**
 * Created by lzh on 19-5-22.
 * INFO:
 * from 1:来自 添加选择界面
 * from 2：来自 来自
 * from 3:来自 activity
 * op   1:add
 * op   2:remove
 * name
 */
class CreateGroupSelectMemberEvent(val uid :Long, val pic :String,val name: String,val op:Int,val from:Int) {
}

class CreateGroupAllSelectMemberEvent(val uidList :List<Long>, val picList :List<String>,val nameList: List<String>,val op:Int,val from:Int) {
}