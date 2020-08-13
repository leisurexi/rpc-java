package com.leisurexi.rpc.common.serializer.impl;

import com.caucho.hessian.io.HessianInput;
import com.caucho.hessian.io.HessianOutput;
import com.leisurexi.rpc.common.serializer.Serializer;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * hessian 序列化实现
 *
 * @author: leisurexi
 * @date: 2020-07-17 11:22 上午
 */
@Slf4j
public class HessianSerializer implements Serializer {

    @Override
    public byte[] serialize(Object object) {
        ByteArrayOutputStream outputStream = null;
        HessianOutput hessianOutput = null;
        try {
            outputStream = new ByteArrayOutputStream();
            hessianOutput = new HessianOutput(outputStream);
            hessianOutput.writeObject(object);
            return outputStream.toByteArray();
        } catch (IOException e) {
            log.error("hessian 序列化异常", e);
        } finally {
            try {
                if (outputStream != null) {
                    outputStream.close();
                }
                if (hessianOutput != null) {
                    hessianOutput.close();
                }
            } catch (IOException e) {
                // ignore
            }
        }
        return null;
    }

    @Override
    public <T> T deserialize(byte[] data, Class<T> clazz) {
        ByteArrayInputStream inputStream = null;
        HessianInput hessianInput = null;
        try {
            inputStream = new ByteArrayInputStream(data);
            hessianInput = new HessianInput(inputStream);
            return (T) hessianInput.readObject(clazz);
        } catch (IOException e) {
            log.error("hessian 反序列异常", e);
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
                if (hessianInput != null) {
                    hessianInput.close();
                }
            } catch (IOException e) {
                // ignore
            }
        }
        return null;
    }

}
