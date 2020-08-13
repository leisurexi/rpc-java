package com.leisurexi.rpc.server;

import com.leisurexi.rpc.common.codec.*;
import com.leisurexi.rpc.common.registry.Registry;
import com.leisurexi.rpc.common.serializer.Serializer;
import com.leisurexi.rpc.common.util.ThreadPoolUtils;
import com.leisurexi.rpc.registry.zk.ZookeeperRegistry;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Netty 服务端实现
 *
 * @author: leisurexi
 * @date: 2020-08-13 5:09 下午
 */
@Slf4j
public class NettyServer implements Server {

    private ExecutorService executorService;

    /**
     * 服务名称
     */
    private final String serviceName;

    /**
     * 服务地址
     */
    private final String serverAddress;

    /**
     * 注册中心
     */
    private final Registry registry;

    public NettyServer(String serviceName, String serverAddress, String registryAddress) {
        this.serviceName = serviceName;
        this.serverAddress = serverAddress;
        this.registry = new ZookeeperRegistry(registryAddress);
    }

    @Override
    public synchronized void start() {
        if (executorService == null) {
            executorService = Executors.newSingleThreadExecutor();
            ThreadPoolExecutor threadPool = ThreadPoolUtils.createThreadPool(NettyServer.class.getSimpleName(), 10, 20);
            executorService.execute(() -> {
                EventLoopGroup bossGroup = new NioEventLoopGroup(1);
                EventLoopGroup workerGroup = new NioEventLoopGroup();
                try {
                    ServerBootstrap bootstrap = new ServerBootstrap();
                    bootstrap.group(bossGroup, workerGroup)
                            .channel(NioServerSocketChannel.class)
                            .option(ChannelOption.SO_BACKLOG, 1024)
                            .childOption(ChannelOption.SO_KEEPALIVE, true)
                            .childHandler(new ChannelInitializer<SocketChannel>() {
                                @Override
                                protected void initChannel(SocketChannel socketChannel) throws Exception {
                                    Serializer serializer = Serializer.DEFAULT;
                                    socketChannel.pipeline().addLast(new IdleStateHandler(0, 0, HeartBeat.HEART_BEAT_TIMEOUT, TimeUnit.SECONDS))
                                            .addLast(new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 0))
                                            .addLast(new RpcDecoder(RpcRequest.class, serializer))
                                            .addLast(new RpcEncoder(RpcResponse.class, serializer))
                                            .addLast();

                                }
                            });
                    String[] inetAddress = serverAddress.split(":");
                    String host = inetAddress[0];
                    int port = Integer.parseInt(inetAddress[1]);
                    ChannelFuture future = bootstrap.bind(host, port).sync();
                    // 向注册中心注册
                    registry.register(serviceName, serverAddress);
                    log.info("Server started on port {}", port);
                    future.channel().closeFuture().sync();
                } catch (Exception e) {
                    if (e instanceof InterruptedException) {
                        log.info("Rpc server remoting server stop.");
                    } else {
                        log.info("Rpc server remoting server error.");
                    }
                } finally {
                    bossGroup.shutdownGracefully();
                    workerGroup.shutdownGracefully();
                }
            });
        } else {
            log.warn("Server already started.");
        }
    }

    @Override
    public void stop() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }

}
