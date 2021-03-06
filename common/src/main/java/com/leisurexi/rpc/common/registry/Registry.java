package com.leisurexi.rpc.common.registry;

import com.leisurexi.rpc.common.base.Destroyable;
import com.leisurexi.rpc.common.base.Initializeable;
import com.leisurexi.rpc.common.client.ProviderInfo;
import com.leisurexi.rpc.common.listener.ProviderInfoListener;

import java.util.List;
import java.util.Map;

/**
 * @author: leisurexi
 * @date: 2020-08-13 2:19 下午
 */
public interface Registry extends Initializeable, Destroyable {

    /**
     * 启动
     */
    boolean start();

    /**
     * 注册服务提供者
     *
     * @param addr       服务地址
     * @param serviceMap 服务Map
     */
    void register(String addr, Map<String, Object> serviceMap);

    /**
     * 取消注册服务
     *
     * @param addr       服务地址
     * @param serviceMap 服务Map
     */
    void unRegister(String addr, Map<String, Object> serviceMap);

    /**
     * 根据服务名称查找服务地址
     *
     * @param serviceName 服务名称
     * @return 服务地址
     */
    List<ProviderInfo> discovery(String serviceName);

    /**
     * 添加服务监听器，当服务节点发生变化时，发出相应的事件
     */
    void addProviderInfoListener(ProviderInfoListener providerInfoListener);

}
