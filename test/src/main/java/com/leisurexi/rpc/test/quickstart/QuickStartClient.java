package com.leisurexi.rpc.test.quickstart;

import com.leisurexi.rpc.client.RpcClient;
import com.leisurexi.rpc.common.registry.Registry;
import com.leisurexi.rpc.registry.zk.ZookeeperRegistry;
import com.leisurexi.rpc.test.service.HelloService;
import lombok.extern.slf4j.Slf4j;

/**
 * @author: leisurexi
 * @date: 2020-08-16 11:53 上午
 */
@Slf4j
public class QuickStartClient {

    public static void main(String[] args) {
        Registry registry = new ZookeeperRegistry("123.57.155.136:2181");
        registry.init();
        registry.start();
        RpcClient client = new RpcClient(registry);
        HelloService service = client.createService(HelloService.class, "0.0.1");
        String result = service.hello("world");
        log.info(result);
    }

}
