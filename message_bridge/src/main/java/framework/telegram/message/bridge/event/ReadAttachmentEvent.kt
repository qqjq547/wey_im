package framework.telegram.message.bridge.event

class ReadAttachmentEvent(var userId: Long, var msgId: Long, var expireTime: Long)