package framework.telegram.business.event

/**
 * Created by lzh on 19-5-22.
 * INFO:
 * from 1:来自 编辑框
 * from 2：来自 checkBox
 */
class RemoveGroupSearchMemberEvent(val keyword:String, val from:Int)