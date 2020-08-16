package com.leisurexi.rpc.server.core;

import com.leisurexi.rpc.common.codec.*;
import com.leisurexi.rpc.common.registry.Registry;
import com.leisurexi.rpc.common.serializer.Serializer;
import com.leisurexi.rpc.common.util.ServiceKeyUtils;
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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
     * 服务地址
     */
    private final String serverAddress;

    /**
     * 注册中心
     */
    private final Registry registry;

    /**
     * 服务 Map
     */
    private Map<String, Object> serviceMap;

    public NettyServer(String serverAddress, String registryAddress) {
        this.serviceMap = new HashMap<>();
        this.serverAddress = serverAddress;
        this.registry = new ZookeeperRegistry(registryAddress);
        this.registry.init();
        this.registry.start();
    }

    /**
     * 添加服务
     *
     * @param interfaceName 接口名称
     * @param version       版本号
     * @param serviceBean   bean 实例
     */
    public void addService(String interfaceName, String version, Object serviceBean) {
        log.info("Add service, interface: [{}]，version: [{}]，bean: [{}]", interfaceName, version, serviceBean);
        String serviceKey = ServiceKeyUtils.buildServiceKey(interfaceName, version);
        serviceMap.put(serviceKey, serviceBean);
    }

    @Override
    public synchronized void start() {
        if (executorService == null) {
            executorService = Executors.newSingleThreadExecutor();
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
                                            .addLast(new RpcServerHandler(serviceMap));
                                }
                            });
                    String[] inetAddress = serverAddress.split(":");
                    String host = inetAddress[0];
                    int port = Integer.parseInt(inetAddress[1]);
                    ChannelFuture future = bootstrap.bind(host, port).sync();
                    // 向注册中心注册
                    registry.register(serverAddress, serviceMap);
                    log.info("Server started on port {}", port);
                    future.channel().closeFuture().sync();
                } catch (Exception e) {
                    if (e instanceof InterruptedException) {
                        log.error("Rpc server remoting server stop.");
                    } else {
                        log.error("Rpc server remoting server error.", e);
                    }
                } finally {
                    bossGroup.shutdownGracefully();
                    workerGroup.shutdownGracefully();
                    // 取消注册
                    registry.unRegister(serverAddress, serviceMap);
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
