package framework.telegram.message.bridge.event

import framework.telegram.message.bridge.Constant

class NotificationEvent(var targetId: Long,var title: String,  var pushType: Constant.Push.PUSH_TYPE,var text: String ="")