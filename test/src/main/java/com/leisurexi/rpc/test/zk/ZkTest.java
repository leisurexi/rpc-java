package com.leisurexi.rpc.test.zk;

import com.leisurexi.rpc.registry.zk.ZookeeperConstant;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryNTimes;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * @author: leisurexi
 * @date: 2020-08-16 1:20 下午
 */
public class ZkTest {

    private static final CuratorFramework zkClient;

    static {
        CuratorFrameworkFactory.Builder curatorFactory = CuratorFrameworkFactory.builder()
                .connectString("123.57.155.136:2181")
                .sessionTimeoutMs(ZookeeperConstant.ZK_SESSION_TIMEOUT)
                .connectionTimeoutMs(ZookeeperConstant.ZK_CONNECTION_TIMEOUT)
                .canBeReadOnly(false)
                .retryPolicy(new RetryNTimes(10, 5000))
                .defaultData(null);
        zkClient = curatorFactory.build();
        zkClient.start();
    }

    @Test
    public void test() throws Exception {
        List<String> list = zkClient.getChildren().forPath("/registry/rpc/com.leisurexi.rpc.test.service.HelloService");
        System.out.println(list);
    }

}
