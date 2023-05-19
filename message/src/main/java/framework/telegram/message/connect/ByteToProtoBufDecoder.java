package framework.telegram.message.connect;

import framework.telegram.message.connect.bean.SocketPackageBean;
import framework.telegram.support.BaseApp;
import framework.telegram.support.tools.BitUtils;
import framework.telegram.support.tools.ShortUtils;
import framework.telegram.support.tools.ZipUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import yourpet.client.android.sign.NativeLibUtil;

public class ByteToProtoBufDecoder extends LengthFieldBasedFrameDecoder {

    private static final int HEADER_SIZE = 8;

    /**
     * @param maxFrameLength      帧的最大长度
     * @param lengthFieldOffset   length字段偏移的地址
     * @param lengthFieldLength   length字段所占的字节长
     * @param lengthAdjustment    修改帧数据长度字段中定义的值，可以为负数 因为有时候我们习惯把头部记入长度,若为负数,则说明要推后多少个字段
     * @param initialBytesToStrip 解析时候跳过多少个长度
     * @param failFast            为true，当frame长度超过maxFrameLength时立即报TooLongFrameException异常，为false，读取完整个帧再报异
     */
    public ByteToProtoBufDecoder(int maxFrameLength, int lengthFieldOffset, int lengthFieldLength, int lengthAdjustment, int initialBytesToStrip, boolean failFast) {
        super(maxFrameLength, lengthFieldOffset, lengthFieldLength, lengthAdjustment, initialBytesToStrip, failFast);
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        //在这里调用父类的方法,实现指得到想要的部分,我在这里全部都要,也可以只要body部分
        in = (ByteBuf) super.decode(ctx, in);
        if (in == null) {
            return null;
        }

        if (in.readableBytes() < HEADER_SIZE) {
            throw new Exception("字节数不足");
        }

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

        return new SocketPackageBean(ShortUtils.bytesToShort(messageType, false), content);
    }
}