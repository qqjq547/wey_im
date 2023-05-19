package framework.telegram.support.system.network.http;

import com.google.protobuf.MessageLite;

public abstract class HttpReq<T extends MessageLite> {

    public abstract T getData();
}
