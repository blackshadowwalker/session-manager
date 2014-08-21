/**
 * @project: session-manager-independent
 * @create: 2013-6-16 下午9:12:15
 */
package com.scipublish.sm.cache.support;

import java.nio.charset.Charset;
import java.util.*;

import org.apache.commons.beanutils.BeanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.*;
import redis.clients.jedis.exceptions.JedisException;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.scipublish.sm.cache.AbstractCacheEngine;
import com.scipublish.sm.cache.CacheEngine;
import redis.clients.util.Pool;

/**
 * 基于redis的cache实现
 * @author chenboxiang
 * @date 2013-6-16 下午9:12:15
 * @since JDK1.6
 * @version 
 */
public class RedisCacheEngine extends AbstractCacheEngine {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(RedisCacheEngine.class);
	private static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");
	
	protected Pool<Jedis> jedisPool;
	protected String DEFAULT_HOST = "localhost";
	protected int DEFAULT_PORT = 6379;
    protected int DEFAULT_DATABASE = 0;
	protected String DEFAULT_PASSWORD = null;
	protected int DEFAULT_TIMEOUT = Protocol.DEFAULT_TIMEOUT;
	protected int DEFAULT_MAX_TOTAL = 200;
	protected int DEFAULT_MAX_IDLE = 100;
	protected int DEFAULT_MIN_IDLE = 5;
	protected int DEFAULT_MAX_WAIT = 10000;
	protected boolean DEFAULT_TEST_ON_BORROW = false;
	protected boolean DEFAULT_TEST_ON_RETURN = false;
	
	@Override
	protected void doInit(Properties properties) {
		LOGGER.info("redisCacheEngine init start");
		// set default
		JedisBaseConfig baseConfig = new JedisBaseConfig();
		baseConfig.setHost(DEFAULT_HOST);
		baseConfig.setPort(DEFAULT_PORT);
        baseConfig.setDatabase(DEFAULT_DATABASE);
		baseConfig.setPassword(DEFAULT_PASSWORD);
		baseConfig.setTimeout(DEFAULT_TIMEOUT);

		JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxTotal(DEFAULT_MAX_TOTAL);
		config.setMaxIdle(DEFAULT_MAX_IDLE);
		config.setMinIdle(DEFAULT_MIN_IDLE);
        config.setMaxWaitMillis(DEFAULT_MAX_WAIT);
		config.setTestOnBorrow(DEFAULT_TEST_ON_BORROW);
		config.setTestOnReturn(DEFAULT_TEST_ON_RETURN);
		
		// set custom config
		if (null != properties && properties.size() > 0) {
			Map<String, String> propMap = new HashMap<String, String>((Map) properties);
			// set base config
			for (String name : propMap.keySet()) {
				try {
                    String value = propMap.get(name);
                    if (!name.equals("sentinels")) {
                        BeanUtils.setProperty(baseConfig, name, value);
                        BeanUtils.setProperty(config, name, value);
                    } else {
                        if (value.endsWith(";")) {
                            value = value.substring(0, value.length() - 1);
                        }
                        baseConfig.setSentinels(new HashSet<String>(Arrays.asList(value.split(";"))));
                    }
				} catch (Exception e) {
					LOGGER.error(e.getMessage(), e);
				}
			}
		}

        if (null != baseConfig.getSentinels() && baseConfig.getSentinels().size() > 0) {
            jedisPool = new JedisSentinelPool(baseConfig.getMasterName(), baseConfig.getSentinels(), config, baseConfig.getTimeout(), baseConfig.getPassword(), baseConfig.getDatabase());

        } else {
            jedisPool = new JedisPool(config, baseConfig.getHost(), baseConfig.getPort(), baseConfig.getTimeout(), baseConfig.getPassword(), baseConfig.getDatabase());
        }
		try {
			Jedis jedis = jedisPool.getResource();
			jedisPool.returnResource(jedis);
			
		} catch (JedisException e) {
			LOGGER.error("Jedis can not connect to the redis server!");
			throw e;
		}
		LOGGER.info("jedis pool init with config: {}", properties);
		
		LOGGER.info("redisCacheEngine init end");
	}

	@Override
	public boolean containsKey(String key) {
		checkInit();
		checkKey(key);
		Jedis jedis = null;
		try {
			jedis = jedisPool.getResource();
			Boolean result = jedis.exists(key);
			jedisPool.returnResource(jedis);
			
			return result;
			
		} catch (JedisException e) {
			if (null != jedis) {
				jedisPool.returnBrokenResource(jedis);
			}
			throw e;
		}
	}

	@Override
	public void put(String key, Object value) {
		checkInit();
		checkKey(key);
		Jedis jedis = null;
		try {
			jedis = jedisPool.getResource();
			String valueJson;
			try {
				valueJson = JSON.toJSONString(value, SerializerFeature.WriteClassName);
			} catch (Exception e) {
				jedisPool.returnResource(jedis);
				throw new RuntimeException(e);
			}
			jedis.set(key, valueJson);
			jedisPool.returnResource(jedis);
			
		} catch (JedisException e) {
			if (null != jedis) {
				jedisPool.returnBrokenResource(jedis);
			}
			throw e;
		}
	}

	@Override
	public void put(String key, Object value, int seconds) {
		checkInit();
		checkKey(key);
		Jedis jedis = null;
		try {
			jedis = jedisPool.getResource();
			String valueJson;
			try {
				valueJson = JSON.toJSONString(value, SerializerFeature.WriteClassName);
			} catch (Exception e) {
				jedisPool.returnResource(jedis);
				throw new RuntimeException(e);
			}
			jedis.setex(key, seconds, valueJson);
			jedisPool.returnResource(jedis);
			
		} catch (JedisException e) {
			if (null != jedis) {
				jedisPool.returnBrokenResource(jedis);
			}
			throw e;
		}
	}

	@Override
	public void put(String key, Object value, String[] group) {
		throw new UnsupportedOperationException("not implement yet");
	}

	@Override
	public Object get(String key) {
		checkInit();
		checkKey(key);
		Jedis jedis = null;
		try {
			jedis = jedisPool.getResource();
			String valueJson = jedis.get(key);
			jedisPool.returnResource(jedis);
			try {
				Object value = JSON.parse(valueJson);
				
				return value;
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			
		} catch (Exception e) {
			if (null != jedis) {
				jedisPool.returnBrokenResource(jedis);
			}
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
		Jedis jedis = null;
		try {
			jedis = jedisPool.getResource();
			jedis.del(key);
			jedisPool.returnResource(jedis);
			
		} catch (Exception e) {
			if (null != jedis) {
				jedisPool.returnBrokenResource(jedis);
			}
			throw new RuntimeException(e);
		}
	}

	@Override
	public void flushGroup(String group) {
		throw new UnsupportedOperationException("not implement yet");
	}

	@Override
	protected void doStop() {
		LOGGER.info("redisCacheEngine stop start");
		jedisPool.destroy();
		LOGGER.info("redisCacheEngine stop end");
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
    	CacheEngine cacheEngine = new RedisCacheEngine();
    	Properties properties = new Properties();
    	properties.setProperty("host", "localhost");
    	properties.setProperty("port", "6379");
        properties.setProperty("database", "1");
    	properties.setProperty("testOnBorrow", "true");
    	cacheEngine.init(properties);
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
