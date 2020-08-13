package com.leisurexi.rpc.server;

/**
 * Server SPI
 *
 * @author: leisurexi
 * @date: 2020-08-13 5:06 下午
 */
public interface Server {

    /**
     * 启动服务
     */
    void start();

    /**
     * 停止服务
     */
    void stop();

}
