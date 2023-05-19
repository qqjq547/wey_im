package framework.telegram.business.bridge.event

import framework.ideas.common.model.group.GroupMemberModel

//type: 0显示所有成员, 2@成员,  3转让群主
class SearchGroupOperateEvent(val type: Int, val member: GroupMemberModel)
