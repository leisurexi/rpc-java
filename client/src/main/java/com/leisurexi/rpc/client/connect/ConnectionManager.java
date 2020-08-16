package com.leisurexi.rpc.client.connect;

import com.leisurexi.rpc.client.proxy.RpcClientHandler;
import com.leisurexi.rpc.client.route.RpcLoadBalance;
import com.leisurexi.rpc.client.route.impl.RpcLoadBalanceRoundRobin;
import com.leisurexi.rpc.common.codec.*;
import com.leisurexi.rpc.common.protocol.RpcProtocol;
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
import io.netty.util.concurrent.EventExecutor;
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
 * RPC Connection Manager
 *
 * @author: leisurexi
 * @date: 2020-08-15 11:33 下午
 */
@Slf4j
public class ConnectionManager {

    private EventLoopGroup eventLoopGroup = new NioEventLoopGroup(4);
    private static ThreadPoolExecutor threadPoolExecutor = ThreadPoolUtils.createThreadPool("connection", 4, 8);

    private Map<RpcProtocol, RpcClientHandler> connectedServerNodes = new ConcurrentHashMap<>();
    private CopyOnWriteArraySet<RpcProtocol> rpcProtocolSet = new CopyOnWriteArraySet<>();
    private ReentrantLock lock = new ReentrantLock();
    private Condition connected = lock.newCondition();
    private long waitTimeout = 5000;
    private RpcLoadBalance loadBalance = new RpcLoadBalanceRoundRobin();
    private volatile boolean isRunning = true;

    private ConnectionManager() {
    }

    private static class SingletonHolder {
        private static final ConnectionManager INSTANCE = new ConnectionManager();
    }

    public static ConnectionManager getInstance() {
        return SingletonHolder.INSTANCE;
    }

    public void updateConnectedServer(List<RpcProtocol> serverList) {
        if (!CollectionUtils.isEmpty(serverList)) {
            Set<RpcProtocol> serviceSet = new HashSet<>(serverList);
            for (int i = 0; i < serverList.size(); i++) {
                RpcProtocol rpcProtocol = serverList.get(i);
                serviceSet.add(rpcProtocol);
            }
            // 添加新的服务信息
            for (RpcProtocol rpcProtocol : serviceSet) {
                if (!rpcProtocolSet.contains(rpcProtocol)) {
                    rpcProtocolSet.add(rpcProtocol);
                    // 与服务端建立连接
                    connectServerNode(rpcProtocol);
                }
            }

            // 关闭并且删除无效的服务节点
            for (RpcProtocol rpcProtocol : rpcProtocolSet) {
                if (!serviceSet.contains(rpcProtocol)) {
                    log.warn("Remove invalid service: {}", rpcProtocol.toString());
                    RpcClientHandler handler = connectedServerNodes.get(rpcProtocol);
                    if (handler != null) {
                        handler.close();
                    }
                    connectedServerNodes.remove(rpcProtocol);
                    rpcProtocolSet.remove(rpcProtocol);
                }
            }
        } else {
            // 没有可访问的服务
            log.error("No available service!");
            // 关闭并且删除无效的服务节点
            for (RpcProtocol rpcProtocol : rpcProtocolSet) {
                RpcClientHandler handler = connectedServerNodes.get(rpcProtocol);
                if (handler != null) {
                    handler.close();
                }
                connectedServerNodes.remove(rpcProtocol);
                rpcProtocolSet.remove(rpcProtocol);
            }
        }
    }

    /**
     * 连接服务端节点
     *
     * @param rpcProtocol
     */
    private void connectServerNode(RpcProtocol rpcProtocol) {
        log.info("New service: [{}], version: [{}], host: [{}], port: [{}]", rpcProtocol.getServiceName(),
                rpcProtocol.getVersion(), rpcProtocol.getHost(), rpcProtocol.getPort());
        final InetSocketAddress remotePeer = new InetSocketAddress(rpcProtocol.getHost(), rpcProtocol.getPort());
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
                    connectedServerNodes.put(rpcProtocol, handler);
                    handler.setRpcProtocol(rpcProtocol);
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
        while (isRunning && size <= 0) {
            try {
                waitingForHandler();
                size = connectedServerNodes.values().size();
            } catch (InterruptedException e) {
                log.error("Waiting for available service is interrupted!", e);
            }
        }
        RpcProtocol rpcProtocol = loadBalance.select(serviceKey, connectedServerNodes);
        RpcClientHandler handler = connectedServerNodes.get(rpcProtocol);
        if (handler != null) {
            return handler;
        }
        throw new Exception("Can not get available connection");
    }

    public void removeHandler(RpcProtocol rpcProtocol) {
        if (rpcProtocol != null) {
            rpcProtocolSet.remove(rpcProtocol);
            connectedServerNodes.remove(rpcProtocol);
        }
    }

    public void stop() {
        isRunning = false;
        for (RpcProtocol rpcProtocol : rpcProtocolSet) {
            RpcClientHandler handler = connectedServerNodes.get(rpcProtocol);
            if (handler != null) {
                handler.close();
            }
            connectedServerNodes.remove(rpcProtocol);
            rpcProtocolSet.remove(rpcProtocol);
        }
        signalAvailableHandler();
        threadPoolExecutor.shutdown();
        eventLoopGroup.shutdownGracefully();
    }

}
