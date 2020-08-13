package com.leisurexi.rpc.server;

import com.leisurexi.rpc.common.codec.HeartBeat;
import com.leisurexi.rpc.common.codec.RpcRequest;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author: leisurexi
 * @date: 2020-08-13 5:25 下午
 */
@Slf4j
public class RpcServerHandler extends SimpleChannelInboundHandler<RpcRequest> {

    private final ThreadPoolExecutor serverHandlerPool;


    public RpcServerHandler(ThreadPoolExecutor serverHandlerPool) {
        this.serverHandlerPool = serverHandlerPool;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, RpcRequest rpcRequest) throws Exception {
        if (HeartBeat.HEART_BEAT_ID.equals(rpcRequest.getRequestId())) {
            log.info("Server receive heart-beat-msg.");
            return;
        }
        // 因为 Netty4 handler 的处理在IO线程中，如果 handler 中有耗时操作，会让IO线程等待，影响性能
        // 所以这里放入线程池处理
        serverHandlerPool.execute(() -> {
            log.info("Receive request []" + rpcRequest.getRequestId());
            

        });

    }
}
