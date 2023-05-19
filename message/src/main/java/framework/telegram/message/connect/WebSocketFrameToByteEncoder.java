package framework.telegram.message.connect;

import framework.telegram.message.connect.bean.SocketPackageBean;
import framework.telegram.support.BaseApp;
import framework.telegram.support.tools.BitUtils;
import framework.telegram.support.tools.ShortUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import yourpet.client.android.sign.NativeLibUtil;

public class WebSocketFrameToByteEncoder extends ChannelOutboundHandlerAdapter {

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof SocketPackageBean) {
            SocketPackageBean bean = (SocketPackageBean) msg;
            byte[] packageHead = new byte[2];
            packageHead[0] = BitUtils.setBitValue(packageHead[0], 7, (byte) 1);//固定头
            packageHead[0] = BitUtils.setBitValue(packageHead[0], 6, (byte) 1);//固定头
            packageHead[0] = BitUtils.setBitValue(packageHead[0], 5, (byte) 0);//版本号
            packageHead[0] = BitUtils.setBitValue(packageHead[0], 4, (byte) 0);//版本号
            packageHead[0] = BitUtils.setBitValue(packageHead[0], 3, (byte) 0);//版本号
            packageHead[0] = BitUtils.setBitValue(packageHead[0], 2, (byte) 0);//版本号
            packageHead[0] = BitUtils.setBitValue(packageHead[0], 1, (byte) 0);//版本号
            packageHead[0] = BitUtils.setBitValue(packageHead[0], 0, (byte) 1);//版本号
            packageHead[1] = BitUtils.setBitValue(packageHead[1], 7, (byte) 1);//加密
            packageHead[1] = BitUtils.setBitValue(packageHead[1], 6, (byte) 0);//压缩

            byte[] messageType = ShortUtils.shortToBytes(bean.getMessageType());
            byte[] content = bean.getData();
            ByteBuf out = ctx.alloc().buffer();
            if (content == null || content.length == 0) {
                out.writeBytes(packageHead);
                out.writeBytes(messageType);
                out.writeInt(0);
            } else {
                content = NativeLibUtil.getInstance().sign1(BaseApp.app, BaseApp.Companion.getIS_TEST_SERVER(), content, 1);
                out.writeBytes(packageHead);
                out.writeBytes(messageType);
                out.writeInt(content.length);
                out.writeBytes(content);
            }

            msg = new BinaryWebSocketFrame(out);
        }

        super.write(ctx, msg, promise);
    }
}
