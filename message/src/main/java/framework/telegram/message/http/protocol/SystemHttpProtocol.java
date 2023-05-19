package framework.telegram.message.http.protocol;


import com.im.domain.pb.LoginProto;
import com.im.domain.pb.SysProto;

import framework.telegram.support.system.network.http.HttpReq;
import io.reactivex.Observable;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface SystemHttpProtocol {

    @POST("sys/placeList")
    Observable<SysProto.PlaceListResp> getPlaceList(@Body HttpReq<SysProto.PlaceListReq> req);
}
