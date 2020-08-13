package com.leisurexi.rpc.common.codec;

import com.leisurexi.rpc.common.serializer.Serializer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

/**
 * RPC 解码器
 *
 * @author: leisurexi
 * @date: 2020-08-13 10:19 上午
 */
public class RpcDecoder extends ByteToMessageDecoder {

    private Class<?> genericClass;

    private Serializer serializer;

    public RpcDecoder(Class<?> genericClass, Serializer serializer) {
        this.genericClass = genericClass;
        this.serializer = serializer;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (in.readableBytes() < 4) {
            return;
        }
        in.markReaderIndex();
        // 获取数据的长度
        int dataLength = in.readInt();
        // 如果可读字节数据小于数据长度，代表是半包
        if (in.readableBytes() < dataLength) {
            in.resetReaderIndex();
            return;
        }
        byte[] data = new byte[dataLength];
        in.readBytes(data);
        Object obj = serializer.deserialize(data, genericClass);
        out.add(obj);
    }

}
