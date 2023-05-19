package framework.telegram.business.commandhandler;

import android.app.Activity;
import android.content.Intent;

import com.alibaba.android.arouter.launcher.ARouter;

import java.lang.ref.WeakReference;

import framework.telegram.message.bridge.Constant;
import framework.telegram.support.commandrouter.CommandHandler;
import framework.telegram.support.commandrouter.CommandRouter;
import framework.telegram.support.commandrouter.CommandRouterBuilder;
import framework.telegram.support.commandrouter.annotation.CommandAlias;
import framework.telegram.support.commandrouter.annotation.HandlerAlias;
import framework.telegram.support.commandrouter.annotation.ParamAlias;
import framework.telegram.support.commandrouter.driver.UriDriver;


/**
 * Created by hu on 15/6/12.
 */
public class ImCommandHandlers implements CommandHandlers {

    private CommandRouter mCommandRouter;

    public ImCommandHandlers() {
        mCommandRouter = new CommandRouterBuilder()
                .setDriver(new UriDriver())
                .addCommandHandler(new PageCommandHandler())
                .addGeneralValueConverters()
                .build();
    }

    @Override
    public boolean executeCommand(String redirectUrl) {
        return   mCommandRouter.executeCommand(null, redirectUrl);
    }

    @Override
    public boolean onBackPressed() {
        return false;
    }

    @HandlerAlias("page")//im://page/oneToOneMessage?uid=102231
    public class PageCommandHandler extends CommandHandler {

        @CommandAlias("oneToOneMessage")
        public void oneToOneMessage(Object context, @ParamAlias("uid") long uid) {
            ARouter.getInstance().build(Constant.ARouter.ROUNTE_MSG_PRIVATE_CHAT_ACTIVITY).withLong("targetUid", uid).navigation();
        }

        @CommandAlias("groupMessage")
        public void groupMessage(Object context, @ParamAlias("groupId") long groupId) {
            ARouter.getInstance().build(Constant.ARouter.ROUNTE_MSG_GROUP_CHAT_ACTIVITY).withLong("targetGid", groupId).navigation();
        }

        @CommandAlias("streamMessage")
        public void streamMessage(Object context, @ParamAlias("uid") long targetUid, @ParamAlias("streamType") int streamType) {
            //客户端自己产生的notification 会有targetUid ,就跳转的相应页面
            //服务端返回的，不会有targetUid ，只打开主页面
            if (targetUid != 0){
                ARouter.getInstance().build(Constant.ARouter.ROUNTE_MSG_STREAM_CALL_GO)
                        .withLong("targetUid", targetUid)
                        .withInt("streamType", streamType)
                        .withInt("openType", 1).navigation();
            }
        }

        @CommandAlias("inviteFriend")
        public void inviteFriend(Object context) {
        }

        @CommandAlias("inviteJoinGroup")
        public void inviteJoinGroup(Object context) {
        }

        @CommandAlias("applyJoinGroup")
        public void applyJoinGroup(Object context) {
        }

    }
}
