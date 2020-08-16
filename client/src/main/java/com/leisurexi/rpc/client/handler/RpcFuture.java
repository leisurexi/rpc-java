package com.leisurexi.rpc.client.handler;

import com.leisurexi.rpc.client.RpcClient;
import com.leisurexi.rpc.common.codec.RpcRequest;
import com.leisurexi.rpc.common.codec.RpcResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.ReentrantLock;

/**
 * RPCFuture for async RPC call
 *
 * @author: leisurexi
 * @date: 2020-08-15 5:42 下午
 */
@Slf4j
public class RpcFuture implements Future<Object> {

    private Sync sync;
    private RpcRequest request;
    private RpcResponse response;
    private long startTime;
    /**
     * 响应时间阈值，超过会打印警告日志
     */
    private long responseTimeThreshold;
    private List<AsyncRpcCallback> pendingCallbacks = new ArrayList<>();
    private ReentrantLock lock = new ReentrantLock();

    public RpcFuture(RpcRequest request) {
        this(request, 5000);
    }

    public RpcFuture(RpcRequest request, long responseTimeThreshold) {
        this.request = request;
        this.sync = new Sync();
        this.startTime = System.currentTimeMillis();
        this.responseTimeThreshold = responseTimeThreshold;
    }

    @Override
    public boolean isDone() {
        return sync.isDone();
    }

    @Override
    public Object get() {
        sync.acquire(1);
        if (this.response != null) {
            return this.response.getResult();
        }
        return null;
    }

    @Override
    public Object get(long timeout, TimeUnit unit) throws InterruptedException {
        boolean success = sync.tryAcquireNanos(1, unit.toNanos(timeout));
        if (success) {
            if (response != null) {
                return response.getResult();
            } else {
                return null;
            }
        } else {
            throw new RuntimeException("Timeout exception. Request id: " + request.getRequestId()
                    + ". Request class name: " + request.getClassName()
                    + ". Request method: " + request.getMethodName());
        }
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isCancelled() {
        throw new UnsupportedOperationException();
    }

    public void done(RpcResponse response) {
        this.response = response;
        sync.release(1);
        invokeCallbacks();
        long responseTime = System.currentTimeMillis() - startTime;
        if (responseTime > responseTimeThreshold) {
            log.warn("Service response time is too slow. Request id = {}. Response time = {}ms", request.getRequestId(), responseTime);
        }
    }

    /**
     * 调用回调
     *
     * @see #runCallback(AsyncRpcCallback)
     */
    private void invokeCallbacks() {
        lock.lock();
        try {
            for (AsyncRpcCallback pendingCallback : pendingCallbacks) {
                runCallback(pendingCallback);
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * 异步 RPC 调用回调
     *
     * @param callback 回调函数
     */
    private void runCallback(final AsyncRpcCallback callback) {
        final RpcResponse resp = this.response;
        RpcClient.submit(() -> {
            if (!resp.isError()) {
                callback.success(resp.getResult());
            } else {
                callback.fail(new RuntimeException("Response error", new Throwable(resp.getError())));
            }
        });
    }

    static class Sync extends AbstractQueuedSynchronizer {

        /**
         * 完成状态
         */
        private final int done = 1;

        /**
         * 等待状态
         */
        private final int pending = 0;

        @Override
        protected boolean tryAcquire(int arg) {
            return getState() == done;
        }

        @Override
        protected boolean tryRelease(int arg) {
            if (getState() == pending) {
                if (compareAndSetState(pending, done)) {
                    return true;
                } else {
                    return false;
                }
            } else {
                return true;
            }
        }

        public boolean isDone() {
            return getState() == done;
        }

    }

}
