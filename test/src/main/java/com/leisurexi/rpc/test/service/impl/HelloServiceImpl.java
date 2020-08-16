package com.leisurexi.rpc.test.service.impl;

import com.leisurexi.rpc.test.service.HelloService;

/**
 * @author: leisurexi
 * @date: 2020-08-16 11:08 上午
 */
public class HelloServiceImpl implements HelloService {

    @Override
    public String hello(String name) {
        return "hello, " + name;
    }

}
