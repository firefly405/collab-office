package com.collaboffice.ws.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;

@Slf4j
@Component
@RequiredArgsConstructor
public class NettyWebSocketServer implements InitializingBean {

    @Value("${ws.port:9000}")
    private int port;

    @Value("${ws.host:${server.address:0.0.0.0}}")
    private String host;

    private final WebSocketAuthHandler authHandler;
    private final com.collaboffice.ws.router.MessageRouter messageRouter;
    private final EventLoopGroup bossGroup = new NioEventLoopGroup(1);
    private final EventLoopGroup workerGroup = new NioEventLoopGroup();

    @Override
    public void afterPropertiesSet() throws Exception {
        new Thread(() -> {
            try {
                ServerBootstrap bootstrap = new ServerBootstrap();
                bootstrap.group(bossGroup, workerGroup)
                        .channel(NioServerSocketChannel.class)
                        .handler(new LoggingHandler(LogLevel.INFO))
                        .childHandler(new ChannelInitializer<Channel>() {
                            @Override
                            protected void initChannel(Channel ch) {
                                ChannelPipeline pipeline = ch.pipeline();
                                pipeline.addLast(new HttpServerCodec());
                                pipeline.addLast(new ChunkedWriteHandler());
                                pipeline.addLast(new HttpObjectAggregator(64 * 1024));
                                pipeline.addLast(authHandler);
                                pipeline.addLast(new WebSocketServerProtocolHandler("/ws", null, true, 64 * 1024));
                                pipeline.addLast(new MessageDispatchHandler(messageRouter));
                            }
                        })
                        .childOption(ChannelOption.SO_KEEPALIVE, true);

                ChannelFuture future = bootstrap.bind(new InetSocketAddress(host, port)).sync();
                log.info("Netty WebSocket服务端启动成功，端口: {}", port);
                future.channel().closeFuture().sync();
            } catch (Exception e) {
                log.error("Netty WebSocket服务启动失败", e);
            } finally {
                bossGroup.shutdownGracefully();
                workerGroup.shutdownGracefully();
            }
        }, "netty-ws-server").start();
    }

    public void shutdown() {
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }
}
