package com.leisurexi.rpc.client.handler;

/**
 * 异步 RPC 调用回调
 *
 * @author: leisurexi
 * @date: 2020-08-15 5:48 下午
 */
public interface AsyncRpcCallback {

    /**
     * 成功回调
     *
     * @param result 结果
     */
    void success(Object result);

    /**
     * 失败回调
     *
     * @param e 异常信息
     */
    void fail(Exception e);

}
