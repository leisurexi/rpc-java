package com.leisurexi.rpc.client.route;

import com.leisurexi.rpc.client.proxy.RpcClientHandler;
import com.leisurexi.rpc.common.protocol.RpcProtocol;
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
    protected Map<String, List<RpcProtocol>> getServiceMap(Map<RpcProtocol, RpcClientHandler> connectedServerNodes) {
        Map<String, List<RpcProtocol>> serviceMap = new HashMap<>();
        if (connectedServerNodes != null && connectedServerNodes.size() > 0) {
            for (RpcProtocol rpcProtocol : connectedServerNodes.keySet()) {
                String serviceKey = ServiceKeyUtils.buildServiceKey(rpcProtocol.getServiceName(), rpcProtocol.getVersion());
                List<RpcProtocol> protocolList = serviceMap.get(serviceKey);
                if (CollectionUtils.isEmpty(protocolList)) {
                    protocolList = new ArrayList<>();
                }
                protocolList.add(rpcProtocol);
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
    public abstract RpcProtocol select(String serviceKey, Map<RpcProtocol, RpcClientHandler> connectedServerNodes) throws Exception;

}
