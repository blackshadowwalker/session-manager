/**
 * @project: session-manager-independent
 * @create: 2013-6-16 下午9:12:15
 */
package com.gozap.session.cache.support;

import java.nio.charset.Charset;
import java.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import redis.clients.jedis.*;
import redis.clients.jedis.exceptions.JedisException;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.gozap.session.cache.AbstractCacheEngine;

import javax.servlet.ServletContext;

/**
 * 基于redis的cache实现
 *
 * @author chenboxiang
 * @date 2013-6-16 下午9:12:15
 * @since JDK1.6
 */
public class RedisCacheEngine extends AbstractCacheEngine {
    private static final Log log = LogFactory.getLog(RedisCacheEngine.class);
    private static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");
    public static final String JEDIS_POOL = "jedisPool";

    protected JedisPool jedisPool;

    public void setJedisPool(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
    }

    @Override
    protected void doStart() {
        try {
            Jedis jedis = jedisPool.getResource();
            jedis.close();
        } catch (JedisException e) {
            log.error("Jedis can not connect to the redis server!");
            throw e;
        }
    }

    @Override
    protected void doInit(Properties properties, ServletContext servletContext) {
        log.info("redisCacheEngine init start");
        if(servletContext!=null)
            this.jedisPool = (JedisPool) servletContext.getAttribute(JEDIS_POOL);
    }

    @Override
    public boolean containsKey(String key) {
        checkInit();
        checkKey(key);
        Jedis jedis = null;
        jedis = jedisPool.getResource();
        Boolean result = jedis.exists(key);
        jedis.close();
        return result;
    }

    @Override
    public void put(String key, Object value) {
        checkInit();
        checkKey(key);
        Jedis jedis = jedisPool.getResource();
        String valueJson;
        try {
            valueJson = JSON.toJSONString(value, SerializerFeature.WriteClassName);
        } catch (Exception e) {
            jedis.close();
            throw new RuntimeException(e);
        }
        jedis.set(key, valueJson);
        jedis.close();
    }

    @Override
    public void put(String key, Object value, int seconds) {
        checkInit();
        checkKey(key);
        Jedis jedis = null;
        jedis = jedisPool.getResource();
        String valueJson;
        try {
            valueJson = JSON.toJSONString(value, SerializerFeature.WriteClassName);
        } catch (Exception e) {
            jedis.close();
            throw new RuntimeException(e);
        }
        jedis.setex(key, seconds, valueJson);
        jedis.close();
    }

    @Override
    public void put(String key, Object value, String[] group) {
        throw new UnsupportedOperationException("not implement yet");
    }

    @Override
    public void del(String key) {
        checkInit();
        checkKey(key);
        Jedis jedis = null;
        jedis = jedisPool.getResource();
        Long ret = 0L;
        do {
            ret += jedis.del(key);
        }while(jedis.exists(key));
        jedis.close();
    }

    @Override
    public Object get(String key) {
        checkInit();
        checkKey(key);
        Jedis jedis = null;
        jedis = jedisPool.getResource();
        String valueJson = jedis.get(key);
        jedis.close();
        try {
            Object value = JSON.parse(valueJson);
            return value;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Map<String, Object> get(String[] keys) {
        throw new UnsupportedOperationException("not implement yet");
    }

    @Override
    public long increase(String key, long magnitude) {
        throw new UnsupportedOperationException("not implement yet");
    }

    @Override
    public long decrease(String key, long magnitude) {
        throw new UnsupportedOperationException("not implement yet");
    }

    @Override
    public void remove(String key) {
        checkInit();
        checkKey(key);
        Jedis jedis = jedisPool.getResource();
        do {
            jedis.del(key);
        }while(jedis.exists(key));
        jedis.close();
    }

    @Override
    public void flushGroup(String group) {
        throw new UnsupportedOperationException("not implement yet");
    }

    @Override
    protected void doStop() {
        log.info("redisCacheEngine stop start");
        jedisPool.destroy();
        log.info("redisCacheEngine stop end");
    }

    private void checkInit() {
        if (!this.isInitialized()) {
            IllegalStateException ex = new IllegalStateException("This client has not properly initialized.");
            throw ex;
        }
    }

    private void checkKey(String key) {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("Cache key is null or not a length of 0.");
        }
    }

    public static void main(String[] args) {
        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        jedisPoolConfig.setMaxIdle(20);
        jedisPoolConfig.setMinIdle(1);

        JedisPool jedisPool = new JedisPool(jedisPoolConfig, "172.16.2.10",  6379);
        RedisCacheEngine cacheEngine = new RedisCacheEngine();
        cacheEngine.setJedisPool(jedisPool);
        cacheEngine.init(null, null);
        cacheEngine.start();
        cacheEngine.put("boolean", true);
        cacheEngine.put("string", "string");
        cacheEngine.put("long", 1000L);
        cacheEngine.put("int", 1);
        System.out.println(cacheEngine.get("boolean"));
        System.out.println(cacheEngine.get("string"));
        System.out.println(cacheEngine.get("long"));
        System.out.println(cacheEngine.get("int"));
        List<Long> cacheList = new ArrayList<Long>();
        for (int i = 0; i < 100; i++) {
            cacheList.add(Long.valueOf(i));
        }
        cacheEngine.put("cacheList", cacheList);
        System.out.println("---------------------------------------");

        cacheList = (List<Long>) cacheEngine.get("cacheList");
        System.out.println(cacheList);
        cacheEngine.remove("boolean");
        cacheEngine.remove("string");
        cacheEngine.remove("long");
        cacheEngine.remove("int");
        cacheEngine.remove("cacheList");
        cacheEngine.stop();
    }
}
