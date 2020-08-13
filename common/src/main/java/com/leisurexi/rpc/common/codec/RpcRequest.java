package com.leisurexi.rpc.common.codec;

import lombok.Data;

import java.io.Serializable;

/**
 * RPC 请求
 *
 * @author: leisurexi
 * @date: 2020-08-13 9:26 上午
 */
@Data
public class RpcRequest implements Serializable {

    private static final long serialVersionUID = -3787113902918627530L;

    /**
     * 请求id
     */
    private String requestId;
    /**
     * 类名
     */
    private String className;
    /**
     * 方法名称
     */
    private String methodName;
    /**
     * 参数类型数组
     */
    private Class<?>[] parameterTypes;
    /**
     * 参数数组
     */
    private Object[] parameters;
    /**
     * 版本号
     */
    private String version;

}
