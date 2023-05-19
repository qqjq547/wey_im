package framework.telegram.app.push

import framework.telegram.business.commandhandler.ImCommandHandlers
import framework.telegram.support.system.gson.GsonInstanceCreater

/**
 * Created by lzh on 19-7-3.
 * INFO:
 */
object PushProtocol {
    private const val LINK_KEY = "appLink"
    // appLink=im://page/oneToOneMessage?uid=120164  私聊页面
    // appLink=im://page/groupMessage?groupId=120164  群聊页面
    // appLink=im://page/streamMessage?uid=120164  音视频页面
//    im://page/inviteFriend
//    im://page/inviteJoinGroup
//    im://page/applyJoinGroup

    fun pushProtocolManager(extraMap: String) {
        val map = GsonInstanceCreater.defaultGson.fromJson<HashMap<String, String>>(extraMap, HashMap<String, String>()::class.java)
        if (map.containsKey(LINK_KEY)) {
            val link = map[LINK_KEY]
            ImCommandHandlers().executeCommand(link)
        }
    }

    fun pushProtocolManager(extraMap:   Map<String, String>?) {
        if (extraMap?.containsKey(LINK_KEY) == true) {
            val link = extraMap[LINK_KEY]
            ImCommandHandlers().executeCommand(link)
        }
    }

}