package com.leisurexi.rpc.common.registry;

import com.leisurexi.rpc.common.base.Destroyable;
import com.leisurexi.rpc.common.base.Initializeable;

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
     * @param serviceName 服务名称
     * @param addr        服务地址
     */
    void register(String serviceName, String addr);

    /**
     * 根据服务名称查找服务地址
     *
     * @param serviceName 服务名称
     * @return 服务地址
     */
    String discovery(String serviceName);

}
