package com.leisurexi.rpc.registry.zk;

/**
 * zookeeper 相关常量
 *
 * @author: leisurexi
 * @date: 2020-08-13 9:34 上午
 */
public interface ZookeeperConstant {

    /**
     * zookeeper session 超时时间
     */
    int ZK_SESSION_TIMEOUT = 5000;

    /**
     * zookeeper 连接超时时间
     */
    int ZK_CONNECTION_TIMEOUT = 5000;

    /**
     * 根路径
     */
    String ZK_REGISTER_PATH = "/leisurexi-rpc";

}
