package framework.telegram.message.event

// userUid 截图方
// targetUid 被截图方
class ScreenShotDetectionEvent(var userId: Long,var targetId: Long)

class ScreenShotStateEvent(var open: Boolean,var targetId: Long)