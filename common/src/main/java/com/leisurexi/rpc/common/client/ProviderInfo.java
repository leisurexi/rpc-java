package com.leisurexi.rpc.common.client;

import lombok.*;

import java.util.Objects;

/**
 * 服务提供者实体信息
 *
 * @author: leisurexi
 * @date: 2020-08-13 9:44 上午
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProviderInfo {

    /**
     * 唯一id
     */
    private String uuid;
    /**
     * 服务地址
     */
    private String host;
    /**
     * 服务ip
     */
    private int port;
    /**
     * 接口名称
     */
    private String serviceName;
    /**
     * 版本号
     */
    private String version;
    /**
     * 注册时间
     */
    private long startTime;

    public static ProviderInfo fromString(String str) {
        int index = str.indexOf('?');
        if (index == -1) {
            return null;
        }
        String[] inetAddress = str.substring(0, index).split(":");
        ProviderInfo providerInfo = ProviderInfo.builder()
                .host(inetAddress[0])
                .port(Integer.parseInt(inetAddress[1]))
                .build();
        String[] params = str.substring(index + 1).split("&");
        for (String param : params) {
            String[] kvpair = param.split("=");
            if ("uuid".equals(kvpair[0])) {
                providerInfo.setUuid(kvpair[1]);
            } else if ("serviceName".equals(kvpair[0])) {
                providerInfo.setServiceName(kvpair[1]);
            } else if ("version".equals(kvpair[0])) {
                providerInfo.setVersion(kvpair[1]);
            } else if ("startTime".equals(kvpair[0])) {
                providerInfo.setStartTime(Long.valueOf(kvpair[1]));
            }
        }
        return providerInfo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ProviderInfo)) {
            return false;
        }
        ProviderInfo that = (ProviderInfo) o;
        return port == that.port &&
                Objects.equals(uuid, that.uuid) &&
                Objects.equals(host, that.host) &&
                Objects.equals(serviceName, that.serviceName) &&
                Objects.equals(version, that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid, host, port, serviceName, version);
    }

    @Override
    public String toString() {
        return host + ":" + port + "?uuid=" + uuid + "&serviceName=" + serviceName + "&version=" + version + "&startTime=" + startTime;
    }
}
