package com.leisurexi.rpc.server.core;

import com.leisurexi.rpc.common.codec.HeartBeat;
import com.leisurexi.rpc.common.codec.RpcRequest;
import com.leisurexi.rpc.common.codec.RpcResponse;
import com.leisurexi.rpc.common.util.ServiceKeyUtils;
import com.leisurexi.rpc.common.util.ThreadPoolUtils;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cglib.reflect.FastClass;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author: leisurexi
 * @date: 2020-08-13 5:25 下午
 */
@Slf4j
public class RpcServerHandler extends SimpleChannelInboundHandler<RpcRequest> {

    /**
     * 业务线程池
     */
    private final ThreadPoolExecutor serverHandlerPool;

    private final Map<String, Object> handlerMap;

    public RpcServerHandler(Map<String, Object> handlerMap) {
        this.serverHandlerPool = ThreadPoolUtils.createThreadPool(NettyServer.class.getSimpleName(), 10, 20);
        this.handlerMap = handlerMap;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcRequest request) throws Exception {
        if (HeartBeat.HEART_BEAT_ID.equalsIgnoreCase(request.getRequestId())) {
            log.info("Server receive heart-beat-msg.");
            return;
        }
        // 因为 Netty4 handler 的处理在IO线程中，如果 handler 中有耗时操作，会让IO线程等待，影响性能
        // 所以这里放入线程池处理
        serverHandlerPool.execute(() -> {
            log.info("Receive request [{}]", request.getRequestId());
            RpcResponse response = new RpcResponse();
            try {
                response.setRequestId(request.getRequestId());
                Object result = handle(request);
                response.setResult(result);
            } catch (Throwable throwable) {
                response.setError(throwable.toString());
                log.error("RPC Server handle request error", throwable);
            }
            ctx.writeAndFlush(response).addListener(future -> {
                if (future.isSuccess()) {
                    log.info("Send response for request {}", request.getRequestId());
                } else {
                    log.error("Send response error", future.cause());
                }
            });
        });

    }

    private Object handle(RpcRequest request) throws Throwable {
        String className = request.getClassName();
        String version = request.getVersion();
        String serviceKey = ServiceKeyUtils.buildServiceKey(className, version);
        Object serviceBean = handlerMap.get(serviceKey);
        if (serviceBean == null) {
            log.error("Can not find service implement with interface name: {} and version: {}", className, version);
            return null;
        }
        Class<?> serviceClass = serviceBean.getClass();
        String methodName = request.getMethodName();
        Class<?>[] parameterTypes = request.getParameterTypes();
        Object[] parameters = request.getParameters();
        log.info("service class: [{}], method name: [{}], parameter types: {}, parameters: {}", serviceClass.getName(), methodName, Arrays.toString(parameterTypes), Arrays.toString(parameters));
        // Cglib，反射调用方法
        FastClass fastClass = FastClass.create(serviceClass);
        int methodIndex = fastClass.getIndex(methodName, parameterTypes);
        return fastClass.invoke(methodIndex, serviceBean, parameters);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.warn("Server caught exception: " + cause.getMessage());
        ctx.close();
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            ctx.channel().close();
            log.warn("Channel idle in last {} seconds, close it.", HeartBeat.HEART_BEAT_TIMEOUT);
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }
}
