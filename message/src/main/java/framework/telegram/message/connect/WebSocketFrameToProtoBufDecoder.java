package framework.telegram.message.connect;

import framework.telegram.message.connect.bean.SocketPackageBean;
import framework.telegram.support.BaseApp;
import framework.telegram.support.tools.BitUtils;
import framework.telegram.support.tools.ShortUtils;
import framework.telegram.support.tools.ZipUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import yourpet.client.android.sign.NativeLibUtil;

public class WebSocketFrameToProtoBufDecoder extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof BinaryWebSocketFrame) {
            BinaryWebSocketFrame frame = (BinaryWebSocketFrame) msg;
            try {
                ByteBuf in = frame.content();
                //在这里调用父类的方法,实现指得到想要的部分,我在这里全部都要,也可以只要body部分
                byte[] packageHead = new byte[2];
                in.readBytes(packageHead);
                byte[] messageType = new byte[2];
                in.readBytes(messageType);
                int packageLength = in.readInt();
                if (in.readableBytes() != packageLength) {
                    throw new Exception("标记的长度不符合实际长度");
                }

                //读取body
                byte[] bytes = new byte[packageLength];
                in.readBytes(bytes);

                boolean isEncrypt = BitUtils.checkBitValue(packageHead[1], 7);
                boolean isGzip = BitUtils.checkBitValue(packageHead[1], 6);

                byte[] content = bytes;
                if (isGzip) {
                    content = ZipUtils.unGZip(content);
                }
                if (isEncrypt) {
                    content = NativeLibUtil.getInstance().sign1(BaseApp.app, BaseApp.Companion.getIS_TEST_SERVER(), content, 2);
                }

                msg = new SocketPackageBean(ShortUtils.bytesToShort(messageType, false), content);
            } finally {
                frame.release();
            }
        }

        ctx.fireChannelRead(msg);
    }
}