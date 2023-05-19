package framework.telegram.message.http.protocol;

import com.im.domain.pb.LoginProto;
import com.im.domain.pb.SysProto;

import framework.telegram.support.system.network.http.HttpReq;
import io.reactivex.Observable;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface LoginHttpProtocol {

    @POST("sys/getUploadToken")
    Observable<SysProto.GetUploadTokenResp> getUploadToken(@Body HttpReq<SysProto.GetUploadTokenReq> req);

    @POST("sys/getUploadUrl")
    Observable<SysProto.GetUploadUrlResp> getUploadUrl(@Body HttpReq<SysProto.GetUploadUrlReq> req);

    @POST("login/getUrls")
    Observable<LoginProto.GetUrlsResp> getUrls(@Body HttpReq<LoginProto.GetUrlsReq> req);

    @POST("login/getUrls")
    Call<LoginProto.GetUrlsResp> getUrlsCall(@Body HttpReq<LoginProto.GetUrlsReq> req);
}
