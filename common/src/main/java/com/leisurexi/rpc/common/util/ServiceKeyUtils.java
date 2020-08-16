package com.leisurexi.rpc.common.util;

import org.springframework.util.StringUtils;

/**
 * 业务键工具类
 *
 * @author: leisurexi
 * @date: 2020-08-14 9:39 上午
 */
public class ServiceKeyUtils {

    public static final String SERVICE_CONCAT_TOKEN = "#";

    /**
     * 创建业务 key
     *
     * @param interfaceName 接口名称
     * @param version       版本号
     */
    public static String buildServiceKey(String interfaceName, String version) {
        String serviceKey = interfaceName;
        if (StringUtils.hasText(version)) {
            serviceKey += SERVICE_CONCAT_TOKEN.concat(version);
        }
        return serviceKey;
    }

}
