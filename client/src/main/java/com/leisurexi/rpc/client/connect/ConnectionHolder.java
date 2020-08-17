package com.leisurexi.rpc.client.connect;

import com.leisurexi.rpc.client.proxy.RpcClientHandler;
import com.leisurexi.rpc.client.route.RpcLoadBalance;
import com.leisurexi.rpc.client.route.impl.RpcLoadBalanceRoundRobin;
import com.leisurexi.rpc.common.base.Destroyable;
import com.leisurexi.rpc.common.client.ProviderInfo;
import com.leisurexi.rpc.common.codec.*;
import com.leisurexi.rpc.common.listener.ProviderInfoListener;
import com.leisurexi.rpc.common.serializer.Serializer;
import com.leisurexi.rpc.common.util.ThreadPoolUtils;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author: leisurexi
 * @date: 2020-08-17 5:21 下午
 */
@Slf4j
public class ConnectionHolder implements ProviderInfoListener, Destroyable {

    private EventLoopGroup eventLoopGroup = new NioEventLoopGroup(4);
    private static ThreadPoolExecutor threadPoolExecutor = ThreadPoolUtils.createThreadPool("connection", 4, 8);

    private Map<ProviderInfo, RpcClientHandler> connectedServerNodes = new ConcurrentHashMap<>();
    private CopyOnWriteArraySet<ProviderInfo> providerInfoSet = new CopyOnWriteArraySet<>();
    private ReentrantLock lock = new ReentrantLock();
    private Condition connected = lock.newCondition();
    private long waitTimeout = 5000;
    private RpcLoadBalance loadBalance = new RpcLoadBalanceRoundRobin();

    private ConnectionHolder() {
    }

    private static class SingletonHolder {
        private static final ConnectionHolder INSTANCE = new ConnectionHolder();
    }

    public static ConnectionHolder getInstance() {
        return ConnectionHolder.SingletonHolder.INSTANCE;
    }

    public void updateConnectedServer(List<ProviderInfo> serverList) {
        if (!CollectionUtils.isEmpty(serverList)) {
            Set<ProviderInfo> serviceSet = new HashSet<>(serverList);
            for (int i = 0; i < serverList.size(); i++) {
                ProviderInfo providerInfo = serverList.get(i);
                serviceSet.add(providerInfo);
            }
            // 添加新的服务信息
            for (ProviderInfo providerInfo : serviceSet) {
                if (!providerInfoSet.contains(providerInfo)) {
                    providerInfoSet.add(providerInfo);
                    // 与服务端建立连接
                    connectServerNode(providerInfo);
                }
            }

            // 关闭并且删除无效的服务节点
            for (ProviderInfo providerInfo : providerInfoSet) {
                if (!serviceSet.contains(providerInfo)) {
                    log.warn("Remove invalid service: {}", providerInfo.toString());
                    RpcClientHandler handler = connectedServerNodes.get(providerInfo);
                    if (handler != null) {
                        handler.close();
                    }
                    connectedServerNodes.remove(providerInfo);
                    providerInfoSet.remove(providerInfo);
                }
            }
        } else {
            // 没有可访问的服务
            log.error("No available service!");
            // 关闭并且删除无效的服务节点
            for (ProviderInfo providerInfo : providerInfoSet) {
                RpcClientHandler handler = connectedServerNodes.get(providerInfo);
                if (handler != null) {
                    handler.close();
                }
                connectedServerNodes.remove(providerInfo);
                providerInfoSet.remove(providerInfo);
            }
        }
    }

    /**
     * 连接服务端节点
     *
     * @param providerInfo
     */
    private void connectServerNode(ProviderInfo providerInfo) {
        log.info("New service: [{}], version: [{}], host: [{}], port: [{}]", providerInfo.getServiceName(),
                providerInfo.getVersion(), providerInfo.getHost(), providerInfo.getPort());
        final InetSocketAddress remotePeer = new InetSocketAddress(providerInfo.getHost(), providerInfo.getPort());
        threadPoolExecutor.execute(() -> {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(eventLoopGroup)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            Serializer serializer = Serializer.DEFAULT;
                            socketChannel.pipeline()
                                    .addLast(new IdleStateHandler(0, 0, HeartBeat.HEART_BEAT_INTERVAL, TimeUnit.SECONDS))
                                    .addLast(new RpcEncoder(RpcRequest.class, serializer))
                                    .addLast(new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 0))
                                    .addLast(new RpcDecoder(RpcResponse.class, serializer))
                                    .addLast(new RpcClientHandler());
                        }
                    });
            ChannelFuture channelFuture = bootstrap.connect(remotePeer);
            channelFuture.addListener(future -> {
                if (future.isSuccess()) {
                    log.info("Successfully connect to remote server, remote peer = {}", remotePeer);
                    RpcClientHandler handler = channelFuture.channel().pipeline().get(RpcClientHandler.class);
                    connectedServerNodes.put(providerInfo, handler);
                    handler.setProviderInfo(providerInfo);
                    signalAvailableHandler();
                } else {
                    log.error("Can not connect to remote server, remote peer = " + remotePeer);
                }
            });
        });
    }

    private void signalAvailableHandler() {
        lock.lock();
        try {
            connected.signalAll();
        } finally {
            lock.unlock();
        }
    }

    private boolean waitingForHandler() throws InterruptedException {
        lock.lock();
        try {
            log.warn("Waiting for available service");
            return connected.await(this.waitTimeout, TimeUnit.MILLISECONDS);
        } finally {
            lock.unlock();
        }
    }

    public RpcClientHandler chooseHandler(String serviceKey) throws Exception {
        int size = connectedServerNodes.values().size();
        while (size <= 0) {
            try {
                waitingForHandler();
                size = connectedServerNodes.values().size();
            } catch (InterruptedException e) {
                log.error("Waiting for available service is interrupted!", e);
            }
        }
        ProviderInfo providerInfo = loadBalance.select(serviceKey, connectedServerNodes);
        RpcClientHandler handler = connectedServerNodes.get(providerInfo);
        if (handler != null) {
            return handler;
        }
        throw new Exception("Can not get available connection");
    }

    public void removeHandler(ProviderInfo providerInfo) {
        if (providerInfo != null) {
            providerInfoSet.remove(providerInfo);
            connectedServerNodes.remove(providerInfo);
        }
    }

    @Override
    public void destroy() {
        for (ProviderInfo providerInfo : providerInfoSet) {
            RpcClientHandler handler = connectedServerNodes.get(providerInfo);
            if (handler != null) {
                handler.close();
            }
            connectedServerNodes.remove(providerInfo);
            providerInfoSet.remove(providerInfo);
        }
        signalAvailableHandler();
        threadPoolExecutor.shutdown();
        eventLoopGroup.shutdownGracefully();
    }

    @Override
    public void addProvider(ProviderInfo providerInfo) {
        if (providerInfo != null) {
            providerInfoSet.add(providerInfo);
            connectServerNode(providerInfo);
        }
    }

    @Override
    public void removeProvider(ProviderInfo providerInfo) {
        if (providerInfo != null) {
            connectedServerNodes.remove(providerInfo);
            providerInfoSet.remove(providerInfo);
        }
    }

    @Override
    public void updateProvider(List<ProviderInfo> providerInfoList) {
        if (!CollectionUtils.isEmpty(providerInfoList)) {
            updateConnectedServer(providerInfoList);
        }
    }

}
