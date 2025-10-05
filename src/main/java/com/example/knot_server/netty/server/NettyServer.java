package com.example.knot_server.netty.server;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Netty WebSocket 服务器启动类, 
 * 使用 CommandLineRunner 在 Spring Boot 启动时自动运行
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NettyServer implements CommandLineRunner {
    private final NettyServerInitializer initializer;

    // 端口号 目前为 10827
    @Value("${app.netty.port}")
    private int port;

    @Override
    public void run(String... args) throws Exception {
        printNettyBanner();

        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childOption(ChannelOption.TCP_NODELAY, true)       // 禁用 Nagle 算法（它会等到上一个包的 ACK 收到后再发下一个包），减少延迟
                    .childOption(ChannelOption.SO_KEEPALIVE, true)      // 启用 TCP Keepalive（检测连接是否有效，系统级别并非自定义心跳包）
                    .childHandler(initializer);
            ChannelFuture f = b.bind(port).sync();

            System.out.println("Netty WebSocket Server started successfully!");
            System.out.println("Listening on port: " + port);
            System.out.println("Ready to accept connections...");
            System.out.println("=" + "=".repeat(60) + "=");
            System.out.println();

            f.channel().closeFuture().sync();
        } catch (Exception e) {
            log.error("[Netty] server error", e);
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
            log.info("[Netty] server shutdown gracefully");
        }
    }

    private void printNettyBanner() {
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════╗");
        System.out.println("║             Netty WebSocket Ready            ║");
        System.out.println("╚══════════════════════════════════════════════╝");
    }

}
