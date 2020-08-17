package com.leisurexi.rpc.registry.zk;

import com.leisurexi.rpc.common.client.ProviderInfo;
import org.apache.curator.framework.recipes.cache.ChildData;

/**
 * Helper for Zookeeper Register
 *
 * @author: leisurexi
 * @date: 2020-08-17 2:47 下午
 */
public class ZookeeperRegistryHelper {

    /**
     * 将 zookeeper 中的数据转换成 {@link ProviderInfo}
     *
     * @param providerPath 接口在 zookeeper 中的路径
     * @param childData    节点数据
     * @return 服务信息
     */
    public static ProviderInfo convertUrlToProvider(String providerPath, ChildData childData) {
        String str = childData.getPath().substring(providerPath.length() + 1);
        return ProviderInfo.fromString(str);
    }

}
