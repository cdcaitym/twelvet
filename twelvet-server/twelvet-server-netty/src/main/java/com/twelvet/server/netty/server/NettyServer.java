package com.twelvet.server.netty.server;

import com.twelvet.server.netty.properties.NettyProperties;
import com.twelvet.server.netty.server.handler.NettyServerHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketServerCompressionHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.stereotype.Component;

/**
 * @author twelvet
 */
@Component
public class NettyServer implements ApplicationRunner, ApplicationListener<ContextClosedEvent>, ApplicationContextAware {

    private final static Logger log = LoggerFactory.getLogger(NettyServer.class);

    @Autowired
    private NettyProperties nettyProperties;

    @Override
    public void run(ApplicationArguments args) throws InterruptedException {
        // 主线程池（接受客户端请求链接）
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        // 从线程池：主要处理主线程给予的任务
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            // 创建服务启动类
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            // 设置主从线程组
            serverBootstrap.group(bossGroup, workerGroup);

            serverBootstrap
                    // 使用NIO通道作为实现
                    .channel(NioServerSocketChannel.class)
                    // 设置保持活动链接状态
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    // 设置处理器
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) {
                            ChannelPipeline pipeline = socketChannel.pipeline();

                            // 自定义处理事件
                            pipeline.addLast(new NettyServerHandler());

                            pipeline.addLast(new HttpServerCodec());
                            pipeline.addLast(new ChunkedWriteHandler());
                            pipeline.addLast(new HttpObjectAggregator(65536));
                            pipeline.addLast(new WebSocketServerCompressionHandler());
                        }
                    });

            Channel channel = serverBootstrap
                    .bind(nettyProperties.getPort())
                    .sync()
                    .channel();
            // 对关闭通道进行监听
            channel.closeFuture().sync();
            log.info("netty 服务启动");

        } finally {
            // 关闭线程池
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        log.info("websocket 服务启动");
    }

    @Override
    public void onApplicationEvent(ContextClosedEvent contextClosedEvent) {
        log.info("Netty 安全关闭");
    }

}
