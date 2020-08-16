package com.leisurexi.rpc.client;

import com.leisurexi.rpc.client.connect.ConnectionManager;
import com.leisurexi.rpc.client.proxy.ObjectProxy;
import com.leisurexi.rpc.client.proxy.RpcService;
import com.leisurexi.rpc.common.protocol.RpcProtocol;
import com.leisurexi.rpc.common.registry.Registry;
import com.leisurexi.rpc.common.util.ThreadPoolUtils;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * RPC Client 创建RPC代理
 *
 * @author: leisurexi
 * @date: 2020-08-15 5:38 下午
 */
public class RpcClient {

    private static ThreadPoolExecutor threadPoolExecutor = ThreadPoolUtils.createThreadPool("client", 10, 20);

    private Registry registry;

    public RpcClient(Registry registry) {
        this.registry = registry;
    }

    public <T> T createService(Class<T> interfaceClass, String version) {
        List<RpcProtocol> serverList = registry.discovery(interfaceClass.getName());
        ConnectionManager.getInstance().updateConnectedServer(serverList);
        return (T) Proxy.newProxyInstance(
                interfaceClass.getClassLoader(),
                new Class[]{interfaceClass},
                new ObjectProxy<>(interfaceClass, version));
    }

    public <T> RpcService createAsyncService(Class<T> interfaceClass, String version) {
        return new ObjectProxy<>(interfaceClass, version);
    }

    public static Future<?> submit(Runnable task) {
        return threadPoolExecutor.submit(task);
    }

    public void stop() {
        if (threadPoolExecutor != null && !threadPoolExecutor.isShutdown()) {
            threadPoolExecutor.shutdown();
        }
        ConnectionManager.getInstance().stop();
    }

}
