package framework.telegram.message.connect

import framework.telegram.message.connect.bean.SocketPackageBean
import io.netty.channel.ChannelFuture

interface Netty {

    /**
     * 是否连接
     *
     * @return
     */
    fun isConnected(): Boolean

    /**
     * 获取[ChannelFuture]
     *
     * @return
     */
    fun getChannelFuture(): ChannelFuture?

    /**
     * 建立连接
     *
     * @param host
     * @param port
     */
    fun connect(host: String, port: Int)

    /**
     * 重连
     */
    fun reconnect(delayMillis: Long)

    /**
     * 发送消息
     *
     * @param msg
     */
    fun sendMessage(msg: SocketPackageBean)

    /**
     * 连接监听
     *
     * @param listener
     */
    fun setOnConnectListener(listener: OnConnectListener)

    /**
     * 通道监听
     *
     * @param handler
     */
    fun setOnChannelHandler(handler: OnChannelHandler)

    /**
     * 关闭
     */
    fun close()

    /**
     * 连接监听
     */
    interface OnConnectListener {
        fun onSuccess()

        fun onFailed()

        fun onError(e: Exception)
    }

    interface OnChannelHandler {
        fun onMessageReceived(msg: SocketPackageBean)

        fun onExceptionCaught(e: Throwable)
    }
}
