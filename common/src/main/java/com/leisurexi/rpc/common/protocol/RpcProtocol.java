package com.leisurexi.rpc.common.protocol;

import com.leisurexi.rpc.common.util.JsonUtils;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Objects;

/**
 * @author: leisurexi
 * @date: 2020-08-13 9:44 上午
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RpcProtocol {

    /** 唯一id */
    private String uuid;
    /** 服务地址 */
    private String host;
    /** 服务ip */
    private int port;
    /** 接口名称 */
    private String serviceName;
    /** 版本号 */
    private String version;

    public String toJsonString() {
        String jsonStr = JsonUtils.objectToJson(this);
        return jsonStr;
    }

    public static RpcProtocol fromJson(String jsonStr) {
        return JsonUtils.jsonToObject(jsonStr, RpcProtocol.class);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RpcProtocol)) {
            return false;
        }
        RpcProtocol that = (RpcProtocol) o;
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
}
