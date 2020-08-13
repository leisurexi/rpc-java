package com.leisurexi.rpc.common.serializer;

import com.leisurexi.rpc.common.serializer.impl.HessianSerializer;

/**
 * 序列化基本方法接口
 *
 * @author: leisurexi
 * @date: 2020-08-13 10:20 上午
 */
public interface Serializer {

    /**
     * 默认的序列化策略
     */
    Serializer DEFAULT = new HessianSerializer();

    /**
     * java 对象转换成二进制数组
     */
    byte[] serialize(Object obj);

    /**
     * 二进制字节数组转换成 java 对象
     */
    <T> T deserialize(byte[] bytes, Class<T> clazz);

}
