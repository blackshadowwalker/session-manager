package com.gozap.session.serialize;

import java.io.IOException;

import com.gozap.session.util.ByteUtil;

/**
 * 默认的序列化策略实现，使用JDK提供的默认序列化方案。
 * @author Mike
 * @version 1.00 2011-8-9
 * @since 1.5
 */
public class DefaultSerializeStrategy implements SerializeStrategy {

    public DefaultSerializeStrategy() {
    }

    public byte[] serialize(Object source) throws CanNotBeSerializedException {
        try {
            return ByteUtil.objectToByte(source);
        } catch (IOException ex) {
            throw new CanNotBeSerializedException(ex.getMessage(), ex);
        }
    }

    public Object deserialize(byte[] datas) throws CanNotBeUnSerializedException {
        try {
            return ByteUtil.byteToObject(datas);
        } catch (Exception ex) {
            throw new CanNotBeUnSerializedException(ex.getMessage(), ex);
        }
    }
}
