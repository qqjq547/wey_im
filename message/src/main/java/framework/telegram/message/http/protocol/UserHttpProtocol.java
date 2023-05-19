package framework.telegram.message.http.protocol;


import com.im.domain.pb.UserProto;

import framework.telegram.support.system.network.http.HttpReq;
import io.reactivex.Observable;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface UserHttpProtocol {

    @POST("user/getEmoticon")
    Observable<UserProto.GetEmoticonResp> getEmoticon(@Body HttpReq<UserProto.GetEmoticonReq> req);

    @POST("user/addEmoticon")
    Observable<UserProto.AddEmoticonResp> addEmoticon(@Body HttpReq<UserProto.AddEmoticonReq> req);

    @POST("user/delEmoticon")
    Observable<UserProto.DelEmoticonResp> delEmoticons(@Body HttpReq<UserProto.DelEmoticonReq> req);

}
