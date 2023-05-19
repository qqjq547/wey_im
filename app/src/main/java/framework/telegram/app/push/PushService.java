package framework.telegram.app.push;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.alibaba.sdk.android.push.CloudPushService;
import com.alibaba.sdk.android.push.CommonCallback;
import com.alibaba.sdk.android.push.noonesdk.PushServiceFactory;
import com.im.domain.pb.UserProto;

import framework.telegram.business.bridge.bean.AccountInfo;
import framework.telegram.business.http.HttpManager;
import framework.telegram.business.http.creator.UserHttpReqCreator;
import framework.telegram.business.http.protocol.UserHttpProtocol;
import framework.telegram.support.account.AccountManager;
import framework.telegram.support.system.network.http.HttpReq;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class PushService {

    private static boolean IS_INIT_PUSH = false;

    /**
     * 初始化云推送通道
     *
     * @param applicationContext
     */
    public static void registerPushService(Context applicationContext) {
        PushServiceFactory.init(applicationContext);

        final CloudPushService pushService = PushServiceFactory.getCloudPushService();
//        pushService.setDebug(BuildConfig.DEBUG);
        pushService.register(applicationContext, new CommonCallback() {
            @Override
            public void onSuccess(String response) {
                Log.d("Push", "init cloudchannel success");
                IS_INIT_PUSH = true;
                if (AccountManager.INSTANCE.hasLoginAccount()) {
                    AccountInfo accountInfo = AccountManager.INSTANCE.getLoginAccount(AccountInfo.class);
                    long userId = accountInfo.getUserId();
                    String hwToken = accountInfo.getHwToken();
                    //需要绑定到账号
                    if (!TextUtils.isEmpty(Long.toString(userId))) {
                        pushService.bindAccount(Long.toString(userId), new CommonCallback() {
                            @Override
                            public void onSuccess(String s) {
                                updateToken(pushService.getDeviceId(), hwToken);
                            }

                            @Override
                            public void onFailed(String s, String s1) {

                            }
                        });
                    }
                }
            }

            @Override
            public void onFailed(String errorCode, String errorMessage) {
                Log.d("Push", "init cloudchannel failed -- errorcode:" + errorCode + " -- errorMessage:" + errorMessage);
            }
        });
    }

    public static void bindAccountWithPushService(String account, String hwToken) {
        if (IS_INIT_PUSH) {
            CloudPushService pushService = PushServiceFactory.getCloudPushService();
            pushService.bindAccount(account, new CommonCallback() {
                @Override
                public void onSuccess(String s) {
                    updateToken(pushService.getDeviceId(), hwToken);
                }

                @Override
                public void onFailed(String s, String s1) {

                }
            });
        }
    }

    private static void updateToken(String token, String huaweiToken) {
        Log.i("lzh", "token " + token);
        HttpManager.INSTANCE.getStore(UserHttpProtocol.class).updateToken(new HttpReq<UserProto.UpdateTokenReq>() {
            @Override
            public UserProto.UpdateTokenReq getData() {
                return UserHttpReqCreator.Companion.createUpdateTokenReq(token, huaweiToken);
            }
        }).subscribeOn(Schedulers.newThread()).observeOn(AndroidSchedulers.mainThread()).subscribe();
    }

    public static void unbindAccountWithPushService() {
        if (IS_INIT_PUSH) {
            CloudPushService pushService = PushServiceFactory.getCloudPushService();
            pushService.unbindAccount(null);
        }
    }
}
