package com.scipublish.sm.servlet.listener.cache;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.scipublish.sm.cache.CacheEngine;

/**
 * WEB缓存的启动监听器。具有两个初始化参数，cache-class指定缓存引擎的实现类，
 * cache-config指定配置文件的类路径。
 *
 * @version 1.00 2009.07.27
 * @since 1.5
 * @author Mike
 */
public class CacheEngineLoadListener implements javax.servlet.ServletContextListener {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(CacheEngineLoadListener.class);

    /**
     * 构造的缓存引擎在ServletContext中的key.
     */
    public static final String CACHE_USE_HOST_DOMAIN_KEY = "com.scipublish.sm.cache.tag.key";
    private static final String CACHE_CLASS = "cacheClass";
    private static final String CACHE_CONFIG = "cacheConfigLocation";
    private static final String DEFAULT_CACHE_ENGINE_CLASS = "com.scipublish.sm.cache.support.RedisCacheEngine";

    /**
     * 初始化缓存，并以CacheTag.CACHE_USE_HOST_DOMAIN_KEY的名称写入ServletContext.
     * @param event ServletContext启动事件。
     */
    public void contextInitialized(ServletContextEvent event) {
        String engineClass = event.getServletContext().getInitParameter(CACHE_CLASS);
        String engineConfig = event.getServletContext().getInitParameter(CACHE_CONFIG);
        if (engineClass == null || engineClass.isEmpty()) {
        	engineClass = DEFAULT_CACHE_ENGINE_CLASS;
        }
        LOGGER.info("context initialized, cacheClass:{}, cacheConfigLocation:{}", engineClass, engineConfig);
        Class<?> objectClass = null;
        CacheEngine engine = null;
        InputStream configStream = null;
        try {
            objectClass = Class.forName(engineClass);
            engine = (CacheEngine) objectClass.newInstance();

            if (engineConfig != null && !engineConfig.isEmpty()) {
                Properties properties = new Properties();
                configStream = event.getServletContext().getResourceAsStream(engineConfig);
                // 尝试从classpath中获取
                if (null == configStream) {
                	configStream = this.getClass().getResourceAsStream(engineConfig);
                }
                if (null != configStream) {
                	properties.load(configStream);
                }
                engine.init(properties);
            }
        } catch (Exception ex) {
        	LOGGER.error(ex.getMessage(), ex);
        } finally {
            if (configStream != null) {
                try {
                    configStream.close();
                } catch (IOException ex) {
                	LOGGER.error(ex.getMessage(), ex);
                    configStream = null;
                }
            }
        }

        if (engine != null) {
            event.getServletContext().setAttribute(CACHE_USE_HOST_DOMAIN_KEY, engine);
        }
    }

    /**
     * ServletContext关闭时也关闭已经存在的缓存引擎。
     * @param event ServletContext关闭事件。
     */
    public void contextDestroyed(ServletContextEvent event) {
        ServletContext context = event.getServletContext();

        CacheEngine engine = (CacheEngine) context.getAttribute(CACHE_USE_HOST_DOMAIN_KEY);

        context.removeAttribute(CACHE_USE_HOST_DOMAIN_KEY);

        if (engine != null) {
            engine.stop();
        }
    }
}
