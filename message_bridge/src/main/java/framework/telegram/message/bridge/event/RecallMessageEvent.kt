package framework.telegram.message.bridge.event

class RecallMessageEvent(val chatType: Int, val targetId: Long, val msgId: Long) {
}