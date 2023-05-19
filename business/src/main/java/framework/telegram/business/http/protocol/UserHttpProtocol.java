package framework.telegram.business.http.protocol;


import com.im.domain.pb.LoginProto;
import com.im.domain.pb.UserProto;

import framework.telegram.support.system.network.http.HttpReq;
import io.reactivex.Observable;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface UserHttpProtocol {

    @POST("login/webLoginByQrCode")
    Observable<LoginProto.WebLoginByQrCodeResp> webLoginByQrCode(@Body HttpReq<LoginProto.WebLoginByQrCodeReq> req);

    @POST("login/webLogout")
    Observable<LoginProto.WebLogoutResp> webLogout(@Body HttpReq<LoginProto.WebLogoutReq> req);

    @POST("user/updateToken")
    Observable<UserProto.UpdateTokenResp> updateToken(@Body HttpReq<UserProto.UpdateTokenReq> user);

    @POST("user/update")
    Observable<UserProto.UpdateResp> updateUserInfo(@Body HttpReq<UserProto.UpdateReq> user);

    @POST("user/detail")
    Observable<UserProto.DetailResp> getUserDetail(@Body HttpReq<UserProto.DetailReq> user);

    @POST("user/validatePassword")//验证密码请求
    Observable<UserProto.UpdateResp> checkPassword(@Body HttpReq<UserProto.ValidatePasswordReq> req);

    @POST("user/updatePhone")//更换手机号
    Observable<UserProto.UpdatePhoneResp> updatePhoto(@Body HttpReq<UserProto.UpdatePhoneReq> req);

    @POST("user/password")//设置密码请求
    Observable<UserProto.UpdateResp> setPassword(@Body HttpReq<UserProto.PasswordReq> req);

    @POST("user/updatePassword")//修改密码(检验旧密码)
    Observable<UserProto.UpdatePasswordResp> updatePassword(@Body HttpReq<UserProto.UpdatePasswordReq> req);

    @POST("user/updatePasswordFromSmsCode")//修改密码(校验验证码
    Observable<UserProto.UpdatePasswordFromSmsCodeResp> updatePasswordFromSmsCode(@Body HttpReq<UserProto.UpdatePasswordFromSmsCodeReq> req);

    @POST("user/invisibleList")
    Observable<UserProto.InvisibleListResp> getInvisibleList(@Body HttpReq<UserProto.InvisibleListReq> user);

    @POST("user/updateInvisibleList")
    Observable<UserProto.UpdateInvisibleListResp> updateInvisibleList(@Body HttpReq<UserProto.UpdateInvisibleListReq> user);


}
