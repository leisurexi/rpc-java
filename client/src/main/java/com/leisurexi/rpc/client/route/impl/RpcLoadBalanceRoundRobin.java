package com.leisurexi.rpc.client.route.impl;

import com.leisurexi.rpc.client.proxy.RpcClientHandler;
import com.leisurexi.rpc.client.route.RpcLoadBalance;
import com.leisurexi.rpc.common.client.ProviderInfo;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 轮询策略实现
 *
 * @author: leisurexi
 * @date: 2020-08-15 9:39 下午
 */
public class RpcLoadBalanceRoundRobin extends RpcLoadBalance {

    /**
     * 轮询计数器
     */
    private AtomicInteger roundRobin = new AtomicInteger(0);

    @Override
    public ProviderInfo select(String serviceKey, Map<ProviderInfo, RpcClientHandler> connectedServerNodes) throws Exception {
        Map<String, List<ProviderInfo>> serviceMap = getServiceMap(connectedServerNodes);
        List<ProviderInfo> addressList = serviceMap.get(serviceKey);
        if (!CollectionUtils.isEmpty(addressList)) {
            return doSelect(addressList);
        }
        throw new Exception("Can not find connection for service: " + serviceKey);
    }

    /**
     * 轮询获取列表中的服务
     */
    public ProviderInfo doSelect(List<ProviderInfo> addressList) {
        int size = addressList.size();
        int index = (roundRobin.getAndAdd(1) + size) % size;
        return addressList.get(index);
    }

}
