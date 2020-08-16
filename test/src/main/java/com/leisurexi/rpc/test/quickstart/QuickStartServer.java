package com.leisurexi.rpc.test.quickstart;

import com.leisurexi.rpc.server.RpcServer;
import com.leisurexi.rpc.test.service.HelloService;
import com.leisurexi.rpc.test.service.impl.HelloServiceImpl;

/**
 * @author: leisurexi
 * @date: 2020-08-16 11:02 上午
 */
public class QuickStartServer {

    public static void main(String[] args) {
        String serverAddress = "127.0.0.1:18000";
        String registerAddress = "123.57.155.136:2181";
        RpcServer server = new RpcServer(serverAddress, registerAddress);
        HelloService helloService = new HelloServiceImpl();
        server.addService(HelloService.class.getName(), "0.0.1", helloService);
        server.start();
    }

}
