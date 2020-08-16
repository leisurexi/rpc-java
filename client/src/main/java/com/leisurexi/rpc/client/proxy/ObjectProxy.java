package com.leisurexi.rpc.client.proxy;

import com.leisurexi.rpc.client.connect.ConnectionManager;
import com.leisurexi.rpc.client.handler.RpcFuture;
import com.leisurexi.rpc.common.codec.RpcRequest;
import com.leisurexi.rpc.common.util.ServiceKeyUtils;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.UUID;

/**
 * @author: leisurexi
 * @date: 2020-08-15 5:41 下午
 */
@Slf4j
public class ObjectProxy<T> implements InvocationHandler, RpcService {

    private Class<T> clazz;
    private String version;

    public ObjectProxy(Class<T> clazz, String version) {
        this.clazz = clazz;
        this.version = version;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (Object.class == method.getDeclaringClass()) {
            String methodName = method.getName();
            if ("equals".equals(methodName)) {
                return proxy = args[0];
            } else if ("hashcode".equals(methodName)) {
                return System.identityHashCode(proxy);
            } else if ("toString".equals(methodName)) {
                return proxy.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(proxy))
                        + ", with InvocationHandler" + this;
            } else {
                throw new IllegalStateException(String.valueOf(method));
            }
        }

        RpcRequest request = new RpcRequest();
        request.setRequestId(UUID.randomUUID().toString());
        request.setClassName(method.getDeclaringClass().getName());
        request.setMethodName(method.getName());
        request.setParameterTypes(method.getParameterTypes());
        request.setParameters(args);
        request.setVersion(version);
        if (log.isDebugEnabled()) {
            log.debug("class name: [{}], method name: [{}], parameter types: {}, parameters: {}, version: [{}]",
                    request.getClassName(), request.getMethodName(), Arrays.toString(request.getParameterTypes()),
                    Arrays.toString(request.getParameters()), version);
        }
        String serviceKey = ServiceKeyUtils.buildServiceKey(method.getDeclaringClass().getName(), version);
        RpcClientHandler handler = ConnectionManager.getInstance().chooseHandler(serviceKey);
        RpcFuture rpcFuture = handler.sendRequest(request);
        return rpcFuture.get();
    }

    @Override
    public RpcFuture call(String funcName, Object... args) throws Exception {
        String serviceKey = ServiceKeyUtils.buildServiceKey(clazz.getName(), version);
        RpcClientHandler handler = ConnectionManager.getInstance().chooseHandler(serviceKey);
        RpcRequest request = createRequest(clazz.getName(), funcName, args);
        RpcFuture rpcFuture = handler.sendRequest(request);
        return rpcFuture;
    }

    private RpcRequest createRequest(String className, String methodName, Object[] args) {
        RpcRequest request = new RpcRequest();
        request.setRequestId(UUID.randomUUID().toString());
        request.setClassName(className);
        request.setMethodName(methodName);
        request.setParameters(args);
        request.setVersion(version);
        Class[] parameterTypes = new Class[args.length];
        for (int i = 0; i < parameterTypes.length; i++) {
            parameterTypes[i] = args[i].getClass();
        }
        if (log.isDebugEnabled()) {
            log.debug("class name: [{}], method name: [{}], parameter types: {}, parameters: {}, version: [{}]",
                    request.getClassName(), request.getMethodName(), Arrays.toString(request.getParameterTypes()),
                    Arrays.toString(request.getParameters()), version);
        }
        return request;
    }

}
