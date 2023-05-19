package framework.telegram.message.connect

import android.annotation.SuppressLint
import android.content.Context
import android.os.PowerManager
import android.text.TextUtils
import com.im.domain.pb.LoginProto
import framework.ideas.common.rlog.RLogManager
import framework.telegram.business.bridge.bean.AccountInfo
import framework.telegram.message.bridge.event.SocketStatusChangeEvent
import framework.telegram.message.connect.bean.SocketPackageBean
import framework.telegram.message.http.HttpManager
import framework.telegram.message.http.creator.LoginHttpReqCreator
import framework.telegram.message.http.getResult
import framework.telegram.message.http.protocol.LoginHttpProtocol
import framework.telegram.message.manager.ArouterServiceManager
import framework.telegram.message.manager.MessagesManager
import framework.telegram.message.manager.ReceiveMessageManager
import framework.telegram.message.manager.SendMessageManager
import framework.telegram.support.BaseApp
import framework.telegram.support.account.AccountManager
import framework.telegram.support.system.event.EventBus
import framework.telegram.support.system.network.http.HttpReq
import framework.telegram.support.tools.ActivitiesHelper
import framework.telegram.support.tools.ThreadUtils
import framework.telegram.ui.videoplayer.utils.NetworkUtils
import io.netty.bootstrap.Bootstrap
import io.netty.channel.*
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.http.DefaultHttpHeaders
import io.netty.handler.codec.http.FullHttpResponse
import io.netty.handler.codec.http.HttpClientCodec
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.handler.codec.http.websocketx.*
import io.netty.handler.logging.LogLevel
import io.netty.handler.logging.LoggingHandler
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import io.netty.handler.timeout.IdleState
import io.netty.handler.timeout.IdleStateEvent
import io.netty.handler.timeout.IdleStateHandler
import io.netty.util.CharsetUtil
import java.net.URI
import java.util.concurrent.TimeUnit

class MessageSocketService : Thread("message_" + System.currentTimeMillis()) {

    companion object {
        const val TAG = "Socket"

        var exitSocket = false

        var connectDisconnectCount = 0

        var connectRetryCount = 0

        var connectConnectExceptionCount = 0

        private var lastRetryConnectTime = 0L

        private var lastConnectSuccessTime = 0L

        private var lastActiveInstance: MessageSocketService? = null

        fun disConnect(source: String) {
            lastActiveInstance?.disConnection(source)
        }

        fun newMessageSocketService() {
            lastActiveInstance = MessageSocketService()
            lastActiveInstance?.start()
        }

        fun getChannel(): Channel? {
            return lastActiveInstance?.mChannel
        }

        fun loginAccount() {
            lastActiveInstance?.loginAccount()
        }

        fun checkConnectStatus() {
            lastActiveInstance?.checkConnectStatus()
        }
    }

    private val mGroup by lazy { NioEventLoopGroup() }

    private var mChannel: Channel? = null

    private var isWebSocket = false

    private var isWss = false

    private var mHost = ""

    private var mPort = 0

    private fun connect() {
        try {
            if (AccountManager.hasLoginAccount()) {
                if (ActivitiesHelper.getInstance().toForeground() || System.currentTimeMillis() - lastRetryConnectTime >= 60000) {
                    lastRetryConnectTime = System.currentTimeMillis()

                    val accountInfo = AccountManager.getLoginAccount(AccountInfo::class.java)
                    RLogManager.w(TAG, "当前已登录的账号--->${accountInfo.getNickName()}/${accountInfo.getUserId()}")
                    isWebSocket = accountInfo.getUseWebSocket()
                    if (NetworkUtils.isAvailable(BaseApp.app)) {
                        // 连接socket
                        val future = if (isWebSocket) {
                            val webSocketAddress = accountInfo.getWebSocketAddress()
                            val webSocketURI = URI(webSocketAddress)
                            isWss = "wss".equals(webSocketURI.scheme, true)
                            mHost = webSocketURI.host
                            mPort = webSocketURI.port
                            RLogManager.d(TAG, "使用webSocket进行连接--->isWss:${isWss} host:${mHost} port:${mPort}")
                            webSocketClientHandler.setHandShaker(true, WebSocketClientHandshakerFactory.newHandshaker(webSocketURI, WebSocketVersion.V13, null, true, DefaultHttpHeaders()))
                            bootstrap.connect(mHost, mPort)?.sync()
                        } else {
                            mHost = accountInfo.getSocketIp()
                            mPort = accountInfo.getSocketPort()
                            RLogManager.d(TAG, "使用普通Socket进行连接--->host:${mHost} port:${mPort}")
                            webSocketClientHandler.setHandShaker(false, null)
                            bootstrap.connect(mHost, mPort)?.sync()
                        }

                        if (future?.isSuccess == true) {
                            // 连接成功
                            RLogManager.d(TAG, "socket连接成功,开启消息监听--->")
                            mChannel = future.channel()
                            ReceiveMessageManager.lastReceiveHeartTime = System.currentTimeMillis()

                            // 监听关闭事件
                            mChannel?.closeFuture()?.sync()
                            lastConnectSuccessTime = 0L
                            connectDisconnectCount++ //统计断开次数
                            RLogManager.e(TAG, "socket连接已断开--->${connectDisconnectCount}")
                        } else {
                            checkSessionHost()
                            connectConnectExceptionCount++ //统计无法连接的次数
                            RLogManager.e(TAG, "socket连接失败--->${connectDisconnectCount}")
                        }
                    } else {
                        RLogManager.w(TAG, "当前没有网络,socket连接失败--->")
                    }
                } else {
                    RLogManager.w(TAG, "当前应用不在前台且距离上一次自动重连的时间不足60秒--->")
                }
            } else {
                RLogManager.w(TAG, "当前没有已登录的账号--->")
            }
        } catch (t: Throwable) {
            RLogManager.e(TAG, "socket连接失败--->", t)
            checkSessionHost()
        } finally {
            retryConnect()
        }
    }

    @Synchronized
    private fun checkSessionHost() {
        // 连续超过10次socket连接或登录异常，触发更新各业务host地址
        if (connectRetryCount++ > 10) {
            connectRetryCount = 0

            if (AccountManager.hasLoginAccount()) {
                RLogManager.d("Http", "socket无法连接激活更换urls--->")
                HttpManager.getStore(LoginHttpProtocol::class.java).getUrls(object : HttpReq<LoginProto.GetUrlsReq>() {
                    override fun getData(): LoginProto.GetUrlsReq {
                        return LoginHttpReqCreator.createGetUrlsReq()
                    }
                }).getResult(null, { resp ->
                    if (!TextUtils.isEmpty(resp.urls.biz)) {
                        ArouterServiceManager.systemService.saveUrls(resp.urls)
                        disConnect("已更换host，主动断开连接--->")
                    }
                }, {
                    it.printStackTrace()
                })
            }
        }
    }

    private fun retryConnect() {
        if (!exitSocket) {
            RLogManager.d(TAG, "5秒后重连socket--->")

            resetConnection()

            sleepMillis(5000) //等待5秒
        }
    }

    @ChannelHandler.Sharable
    inner class WebSocketClientHandler : SimpleChannelInboundHandler<Any>() {

        /**
         * 用于 WebSocket 的握手
         */
        private var mHandShaker: WebSocketClientHandshaker? = null

        private var mChannelPromise: ChannelPromise? = null

        private var isWebSocketConnect = false

        override fun handlerAdded(ctx: ChannelHandlerContext?) {
            super.handlerAdded(ctx)
            mChannelPromise = ctx?.newPromise()
        }

        fun setHandShaker(isWebSocket: Boolean, handShaker: WebSocketClientHandshaker? = null) {
            mHandShaker = handShaker
            isWebSocketConnect = isWebSocket
        }

        @Throws(Exception::class)
        override fun userEventTriggered(ctx: ChannelHandlerContext, evt: Any) {
            super.userEventTriggered(ctx, evt)

            // 必须有登录的账号
            if (AccountManager.hasLoginAccount()) {
                // 连上socket超过6秒以后才会响应
                if (lastConnectSuccessTime > 0 && System.currentTimeMillis() - lastConnectSuccessTime > 6 * 1000) {
                    checkConnectStatus()
                    MessagesManager.checkToMessageAllStatus()
                }
            }
        }

        override fun channelActive(ctx: ChannelHandlerContext?) {
            super.channelActive(ctx)

            if (isWebSocketConnect) {
                mHandShaker?.handshake(ctx?.channel())
                RLogManager.d(TAG, "webSocket开始握手--->")
            } else {
                RLogManager.d(TAG, "连接成功登录socket--->")
                lastConnectSuccessTime = System.currentTimeMillis()
                loginAccount()
            }
        }

        override fun channelRead0(ctx: ChannelHandlerContext?, msg: Any?) {
            if (msg is SocketPackageBean) {
                ThreadUtils.runOnIOThread {
                    ReceiveMessageManager.receiveMsg(msg)
                }
            } else if (msg is FullHttpResponse) {
                if (mHandShaker?.isHandshakeComplete == false) {
                    try {
                        RLogManager.d(TAG, "webSocket握手成功--->")
                        mHandShaker?.finishHandshake(ctx?.channel(), msg)
                        mChannelPromise?.setSuccess()

                        if (mHandShaker?.isHandshakeComplete == true) {
                            RLogManager.d(TAG, "连接成功登录socket--->")
                            lastConnectSuccessTime = System.currentTimeMillis()
                            loginAccount()
                        }
                    } catch (e: WebSocketHandshakeException) {
                        e.printStackTrace()
                        mChannelPromise?.setFailure(Exception(e.message))
                    }
                } else {
                    throw IllegalStateException("Unexpected FullHttpResponse (getStatus=${msg.status} content=${msg.content().toString(CharsetUtil.UTF_8)}")
                }
            }
        }

        override fun exceptionCaught(ctx: ChannelHandlerContext?, cause: Throwable?) {
            super.exceptionCaught(ctx, cause)

            RLogManager.e(TAG, "发生异常--->", cause)

            if (isWebSocketConnect) {
                if (mChannelPromise?.isDone == false) {
                    mChannelPromise?.setFailure(cause)
                }
            }

            disConnect("发生异常，主动断开连接--->")
        }
    }

    fun checkConnectStatus() {
        if (ReceiveMessageManager.socketIsLogin) {
            // 已连接上
            if (!SendMessageManager.sendHeartMessagePackage()) {
                disConnect("心跳发送失败，主动断开连接--->")
            } else if (System.currentTimeMillis() - ReceiveMessageManager.lastReceiveHeartTime > 30000) {
                disConnect("最后一次心跳时间超过60秒，主动断开连接--->")
            }
        } else {
            //重新登录
            RLogManager.d(TAG, "socket未处于活跃状态，登录socket--->")
            loginAccount()
        }
    }

    fun loginAccount() {
        if (AccountManager.hasLoginAccount()) {
            // 发送登录消息
            RLogManager.d(TAG, "发送socket登录请求--->")
            ThreadUtils.runOnIOThread(1000) {
                if (!SendMessageManager.sendLoginMessagePackage()) {
                    disConnect("登录socket失败，主动断开连接--->")
                }

                // 检测是否超过3次登录未响应
                checkSessionHost()
            }
        } else {
            disConnect("当前无登录账号，无法发送socket登录请求，断开连接--->")
        }
    }

    @SuppressLint("WakelockTimeout", "InvalidWakeLockTag")
    override fun run() {
        RLogManager.i(TAG, "消息线程已重启------------>")

        try {
            val pm = BaseApp.app.getSystemService(Context.POWER_SERVICE) as PowerManager
            pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MessageService")?.acquire()
        } catch (t: Throwable) {
            t.printStackTrace()
        }

        // 初始化连接器
        initBootstrap()

        while (true) {
            // 开始连接循环
            connect()
        }
    }

    private val bootstrap by lazy { Bootstrap() }
    private val webSocketClientHandler by lazy { WebSocketClientHandler() }
    private fun initBootstrap() {
        bootstrap.group(mGroup)
                .channel(NioSocketChannel::class.java)
                .option(ChannelOption.SO_KEEPALIVE, true)// 长连接
                .option(ChannelOption.TCP_NODELAY, true)// 消息立即发出去
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 30000)
                .handler(LoggingHandler(LogLevel.INFO))
                .handler(object : ChannelInitializer<SocketChannel>() {
                    override fun initChannel(ch: SocketChannel) {
                        //建立管道
                        val channelPipeline = ch.pipeline()
                        //添加相关编码器，解码器，处理器等
                        channelPipeline.addLast(IdleStateHandler(0, 10, 0, TimeUnit.SECONDS))//间隔30s一次登录检查和一次心跳
                        if (isWebSocket) {
                            val sslCtx = if (isWss) {
                                SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build()
                            } else {
                                null
                            }
                            if (sslCtx != null) {
                                channelPipeline.addLast(sslCtx.newHandler(ch.alloc(), mHost, mPort))
                            }

                            channelPipeline.addLast(HttpClientCodec(), HttpObjectAggregator(1024 * 1024 * 10))
                            channelPipeline.addLast(WebSocketFrameToByteEncoder())
                            channelPipeline.addLast(WebSocketFrameToProtoBufDecoder())
                        } else {
                            channelPipeline.addLast(ProtoBufToByteEncoder())
                            channelPipeline.addLast(ByteToProtoBufDecoder(1024 * 1024, 4, 4, 0, 0, true))
                        }
                        channelPipeline.addLast(webSocketClientHandler)
                    }
                })
    }

    private fun sleepMillis(time: Long) {
        try {
            sleep(time)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    //断开连接
    private fun disConnection(source: String? = null) {
        if (mChannel != null) {
            if (!TextUtils.isEmpty(source)) {
                RLogManager.w(TAG, "disConnection--->${source}")
            }

            resetConnection()
        }
    }

    private fun resetConnection() {
        try {
            mChannel?.disconnect()
            mChannel?.close()
            mChannel = null
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            ReceiveMessageManager.socketIsLogin = false
            ReceiveMessageManager.updateAccountPublicKey = false
            EventBus.publishEvent(SocketStatusChangeEvent())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}