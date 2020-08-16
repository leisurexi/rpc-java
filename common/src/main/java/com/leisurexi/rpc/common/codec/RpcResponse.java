package com.leisurexi.rpc.common.codec;

import lombok.Data;

import java.io.Serializable;

/**
 * RPC 响应
 *
 * @author: leisurexi
 * @date: 2020-08-13 9:31 上午
 */
@Data
public class RpcResponse implements Serializable {

    private static final long serialVersionUID = 8915935790241399277L;

    /**
     * 请求id
     */
    private String requestId;
    /**
     * 错误信息
     */
    private String error;
    /**
     * 结果
     */
    private Object result;

    public boolean isError() {
        return this.error != null;
    }

}
