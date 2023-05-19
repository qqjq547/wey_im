package framework.telegram.business.http.protocol;


import com.im.domain.pb.LoginProto;
import com.im.domain.pb.SysProto;

import framework.telegram.support.system.network.http.HttpReq;
import io.reactivex.Observable;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface SystemHttpProtocol {

    @POST("sys/feedback")
    Observable<SysProto.FeedbackResp> setFeedback(@Body HttpReq<SysProto.FeedbackReq> req);

    @POST("sys/getKeyPair")
    Observable<SysProto.GetKeyPairResp> getKeyPair(@Body HttpReq<SysProto.GetKeyPairReq> req);

    @POST("sys/getKeyPairOfVer")
    Observable<SysProto.GetKeyPairOfVerResp> getKeyPairOfVer(@Body HttpReq<SysProto.GetKeyPairOfVerReq> req);

    @POST("sys/updateUserKeyPair")
    Observable<SysProto.UpdateKeyPairResp> updateUserKeyPair(@Body HttpReq<SysProto.UpdateKeyPairReq> req);

    @POST("sys/openInstall")
    Observable<SysProto.OpenInstallResp> updateOpenInstall(@Body HttpReq<SysProto.OpenInstallReq> req);

    @POST("sys/getInviteLink")
    Observable<SysProto.GetInviteLinkResp> getInviteLink(@Body HttpReq<SysProto.GetInviteLinkReq> req);

    @POST("sys/updateInviteLink")
    Observable<SysProto.UpdateInviteLinkResp> updateInviteLink(@Body HttpReq<SysProto.UpdateInviteLinkReq> req);

    @POST("sys/disableAccount")
    Observable<SysProto.DisableAccountResp> disableAccount(@Body HttpReq<SysProto.DisableAccountReq> req);
}
