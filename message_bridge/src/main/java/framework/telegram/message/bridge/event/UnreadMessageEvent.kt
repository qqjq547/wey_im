package framework.telegram.message.bridge.event

class UnreadMessageEvent {

    var targetType: Int = 0
    var targetId: Long = 0L

    constructor()

    constructor(targetType: Int, targetId: Long) {
        this@UnreadMessageEvent.targetType = targetType
        this@UnreadMessageEvent.targetId = targetId
    }
}