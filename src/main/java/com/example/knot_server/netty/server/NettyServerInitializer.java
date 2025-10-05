package com.example.knot_server.netty.server;

import org.springframework.stereotype.Component;

import com.example.knot_server.netty.server.handler.AuthHandler;
import com.example.knot_server.netty.server.handler.CleanupHandler;
import com.example.knot_server.netty.server.handler.HeartBeatHandler;
import com.example.knot_server.netty.server.handler.LogoutHandler;
import com.example.knot_server.netty.server.handler.MessageHandler;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketServerCompressionHandler;
import io.netty.handler.timeout.IdleStateHandler;

@Component
public class NettyServerInitializer extends ChannelInitializer<SocketChannel> {

    private final AuthHandler authHandler;
    private final CleanupHandler cleanupHandler;
    private final LogoutHandler logoutHandler;
    private final HeartBeatHandler heartBeatHandler;
    private final MessageHandler messageHandler;

    public NettyServerInitializer(
                AuthHandler authHandler, 
                CleanupHandler cleanupHandler, 
                LogoutHandler logoutHandler,
                HeartBeatHandler heartBeatHandler,
                MessageHandler messageHandler
            ) {
        this.authHandler = authHandler;
        this.cleanupHandler = cleanupHandler;
        this.logoutHandler = logoutHandler;
        this.heartBeatHandler = heartBeatHandler;
        this.messageHandler = messageHandler;
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline p = ch.pipeline();

        // HTTP 编解码器，把字节流和 HTTP 请求/响应对象互相转换。
        // 当用户打开App时，客户端首先发送一个HTTP请求，这个Handler会将其解码为可读的HttpRequest对象，供后续Handler处理。
        p.addLast(new HttpServerCodec());

        // 将HTTP的多段数据​（如分块传输的Body）合并为一个完整的FullHttpRequest。
        // 确保WebSocket握手请求是完整的（避免分片传输导致握手失败）。
        // 防御恶意超大HTTP请求（超过64KB直接拒绝）
        p.addLast(new HttpObjectAggregator(64 * 1024));

        // 支持WebSocket数据的压缩传输​（减少带宽消耗）。
        // 压缩地图坐标、好友列表等文本数据（如JSON），提升移动端网络下的性能。
        // 自动协商是否压缩（取决于客户端是否支持）。
        p.addLast(new WebSocketServerCompressionHandler());

        // 完成WebSocket握手，并处理协议升级后的帧控制。
        // 验证握手请求，若成功则升级为WebSocket连接。自动处理Ping/Pong帧（保活机制）。
        p.addLast(new WebSocketServerProtocolHandler("/ws", null, true, 65536));

        p.addLast(new IdleStateHandler(65, 0, 0));

        p.addLast("authHandler", authHandler);
        p.addLast("heartBeatHandler", heartBeatHandler);
        p.addLast("messageHandler", messageHandler);
        p.addLast("logoutHandler", logoutHandler);
        p.addLast("cleanupHandler", cleanupHandler);

    }

}
