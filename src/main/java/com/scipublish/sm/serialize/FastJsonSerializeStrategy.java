package com.scipublish.sm.serialize;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;

/**
 * @author chenboxiang
 * @date 2013-12-9 下午12:01:16
 */
public class FastJsonSerializeStrategy implements SerializeStrategy {

	@Override
	public byte[] serialize(Object source) throws CanNotBeSerializedException {
		return JSON.toJSONBytes(source, SerializerFeature.WriteClassName);
	}

	@Override
	public Object deserialize(byte[] datas) throws CanNotBeUnSerializedException {
		return JSON.parse(datas);
	}

}
