package com.leisurexi.rpc.common.listener;

import com.leisurexi.rpc.common.client.ProviderInfo;

import java.util.List;

/**
 * Listener of provider info
 *
 * @author: leisurexi
 * @date: 2020-08-16 10:45 下午
 */
public interface ProviderInfoListener {

    /**
     * 增加服务节点
     *
     * @param providerInfo 服务节点信息
     */
    void addProvider(ProviderInfo providerInfo);

    /**
     * 删除服务节点
     *
     * @param providerInfo 服务节点信息
     */
    void removeProvider(ProviderInfo providerInfo);

    /**
     * 更新服务节点
     *
     * @param providerInfoList 接口下所有服务信息
     */
    void updateProvider(List<ProviderInfo> providerInfoList);

}
