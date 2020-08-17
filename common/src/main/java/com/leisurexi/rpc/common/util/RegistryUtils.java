package com.leisurexi.rpc.common.util;

/**
 * 注册中心工具类
 *
 * @author: leisurexi
 * @date: 2020-08-13 3:09 下午
 */
public class RegistryUtils {

    public static final String CONTEXT_SEP = "/";

    /**
     * 构建服务提供者地址
     */
    public static String buildProviderPath(String rootPath, String serviceName) {
        return rootPath + "/" + serviceName + "/providers";
    }

    /**
     * 构建服务消费者地址
     */
    public static String buildConsumerPath(String rootPath, String serviceName) {
        return rootPath + "/" + serviceName + "/consumers";
    }

}
