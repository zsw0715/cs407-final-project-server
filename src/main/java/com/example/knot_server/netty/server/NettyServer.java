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
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class NettyServer implements CommandLineRunner {

    private final NettyServerInitializer initializer;

    @Value("${app.netty.port}")
    private int port;

    public NettyServer(NettyServerInitializer initializer) {
        this.initializer = initializer;
    }

    @Override
    public void run(String... args) throws Exception {
        // 打印Netty服务器启动横幅
        printNettyBanner();
        
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);     // 1个线程处理连接
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
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
        System.out.println("=" + "=".repeat(60) + "=");
        System.out.println();
        System.out.println("    ███╗   ██╗███████╗████████╗████████╗██╗   ██╗");
        System.out.println("    ████╗  ██║██╔════╝╚══██╔══╝╚══██╔══╝╚██╗ ██╔╝");
        System.out.println("    ██╔██╗ ██║█████╗     ██║      ██║    ╚████╔╝ ");
        System.out.println("    ██║╚██╗██║██╔══╝     ██║      ██║     ╚██╔╝  ");
        System.out.println("    ██║ ╚████║███████╗   ██║      ██║      ██║   ");
        System.out.println("    ╚═╝  ╚═══╝╚══════╝   ╚═╝      ╚═╝      ╚═╝   ");
        System.out.println();
    }
    
}
