package com.leisurexi.rpc.common.lifecycle;

/**
 * 生命周期接口
 *
 * @author: leisurexi
 * @date: 2020-08-13 11:14 上午
 */
public interface LifeCycle {

    void startup();

    void shutdown();

    boolean isStarted();

}
