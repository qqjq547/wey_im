package framework.telegram.business.event

/**
 * Created by lzh on 19-5-22.
 * INFO:
 * from 1:来自 添加选择界面
 * from 2：来自 搜索
 * from 3:来自 activity
 * op   1:add
 * op   2:remove
 */
class RemoveSelectMemberEvent(val uid :Long, val pic :String, val op:Int, val from:Int) {
}