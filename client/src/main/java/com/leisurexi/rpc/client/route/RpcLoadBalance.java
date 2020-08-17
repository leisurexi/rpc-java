package com.leisurexi.rpc.client.route;

import com.leisurexi.rpc.client.proxy.RpcClientHandler;
import com.leisurexi.rpc.common.client.ProviderInfo;
import com.leisurexi.rpc.common.util.ServiceKeyUtils;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RPC 负载均衡
 *
 * @author: leisurexi
 * @date: 2020-08-15 9:31 下午
 */
public abstract class RpcLoadBalance {

    /**
     * 获取服务 Map
     *
     * @param connectedServerNodes 已经跟服务端连接的节点
     * @return
     */
    protected Map<String, List<ProviderInfo>> getServiceMap(Map<ProviderInfo, RpcClientHandler> connectedServerNodes) {
        Map<String, List<ProviderInfo>> serviceMap = new HashMap<>();
        if (connectedServerNodes != null && connectedServerNodes.size() > 0) {
            for (ProviderInfo providerInfo : connectedServerNodes.keySet()) {
                String serviceKey = ServiceKeyUtils.buildServiceKey(providerInfo.getServiceName(), providerInfo.getVersion());
                List<ProviderInfo> protocolList = serviceMap.get(serviceKey);
                if (CollectionUtils.isEmpty(protocolList)) {
                    protocolList = new ArrayList<>();
                }
                protocolList.add(providerInfo);
                serviceMap.putIfAbsent(serviceKey, protocolList);
            }
        }
        return serviceMap;
    }

    /**
     * 根据负载均衡策略获取服务端连接信息
     *
     * @param serviceKey           服务关键字
     * @param connectedServerNodes 已经跟服务端连接的节点
     * @return 服务端连接信息
     * @throws Exception
     */
    public abstract ProviderInfo select(String serviceKey, Map<ProviderInfo, RpcClientHandler> connectedServerNodes) throws Exception;

}
