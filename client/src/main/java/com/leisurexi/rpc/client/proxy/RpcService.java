package com.leisurexi.rpc.client.proxy;

import com.leisurexi.rpc.client.handler.RpcFuture;

/**
 * RPC 异步调用接口
 *
 * @author: leisurexi
 * @date: 2020-08-15 5:41 下午
 */
public interface RpcService {

    /**
     * 异步调用
     *
     * @param funcName 方法名称
     * @param args     方法参数
     * @return 异步结果
     * @throws Exception
     */
    RpcFuture call(String funcName, Object... args) throws Exception;

}
