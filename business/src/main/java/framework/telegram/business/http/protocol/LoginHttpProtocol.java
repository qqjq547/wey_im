package framework.telegram.business.http.protocol;

import com.im.domain.pb.LoginProto;
import com.im.domain.pb.SysProto;
import com.im.domain.pb.UploadFileProto;

import framework.telegram.support.system.network.http.HttpReq;
import io.reactivex.Observable;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface LoginHttpProtocol {

    @POST("login/login")
    Observable<LoginProto.LoginResp> login(@Body HttpReq<LoginProto.LoginReq> req);

    @POST("login/reg")
    Observable<LoginProto.RegResp> register(@Body HttpReq<LoginProto.RegReq> req);

    @POST("login/login")
    Call<LoginProto.LoginResp> autoLogin(@Body HttpReq<LoginProto.LoginReq> req);

    @POST("sys/getSmsCode")
    Observable<SysProto.GetSmsCodeResp> getSmsCode(@Body HttpReq<SysProto.GetSmsCodeReq> user);

    @POST("sys/checkSmsCode")
    Observable<SysProto.CheckSmsCodeResp> checkSmsCode(@Body HttpReq<SysProto.CheckSmsCodeReq> req);

    @POST("sys/getUploadToken")
    Observable<SysProto.GetUploadTokenResp> getUploadToken(@Body HttpReq<SysProto.GetUploadTokenReq> req);

    @POST("sys/checkVersion")
    Observable<SysProto.CheckVersionResp> checkVersion(@Body HttpReq<SysProto.CheckVersionReq> req);

    @POST("sys/getUploadUrl")
    Observable<UploadFileProto.GetUploadUrlResp> getUploadUrl(@Body HttpReq<UploadFileProto.GetUploadUrlReq> req);

    @POST("login/logout")
    Observable<LoginProto.LogoutResp> logout(@Body HttpReq<LoginProto.LogoutReq> req);

    @POST("login/forgetPassword")
    Observable<LoginProto.ForgetPasswordResp> findPassword(@Body HttpReq<LoginProto.ForgetPasswordReq> req);

    @POST("login/getUrls")
    Call<LoginProto.GetUrlsResp> getUrls(@Body HttpReq<LoginProto.GetUrlsReq> req);

    @POST("sys/getAwsUpload")
    Observable<UploadFileProto.GetAwsUploadResp> getAwsUpload(@Body HttpReq<UploadFileProto.GetAwsUploadReq> req);
}
