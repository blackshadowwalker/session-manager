package com.gozap.session.servlet.filter.session;

import java.io.IOException;
import java.net.URL;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.gozap.session.cache.CacheEngine;
import com.gozap.session.servlet.filter.BaseFilter;
import com.gozap.session.servlet.listener.cache.CacheEngineLoadListener;
import com.gozap.session.servlet.session.CacheHttpSession;
import com.gozap.session.servlet.util.WebUtil;
import com.gozap.session.servlet.wrapper.CacheSessionHttpServletRequest;

/**
 * 包含原始的请求，将原始的HttpServletRequest使用CacheSessionHttpServeltRequest进行包装。
 * 参数说明。
 * sessionCookieName为cookie中SessionID的cookie名称，默认为etnetsessionid.
 * maxInactiveInterval为缓存的最大不活动时间，单位秒。默认为0，不过期。
 * cookieDomain为存放cookie的域设置。
 * cookieContextPath为存放cookie的路径。如果不设置将使用默认的contextPath.
 *
 * sessionAttributeListeners 为HttpSessionAttributeListener监听器实现类全限定名,多个名称以","分隔.
 * sessionListeners 为HttpSessionListener监听器实现类的全限定名,多个名称以","分隔.
 * 所有的监听器实现类都必须提供无参的构造方法.
 *
 * @author Mike
 * @version 2.00 2010-11-12
 * @since 1.5
 */
public class CacheSessionFilter extends BaseFilter {
	private static final Log LOGGER = LogFactory.getLog(CacheSessionFilter.class);

    public static final String COOKIE_SESSION_ID_NAME = "cookieSessionIdName";
    public static final String SESSION_CACHE_KEY_PREFIX = "sessionCacheKeyPrefix";
    public static final String MAX_INACTIVE_INTERVAL = "maxInactiveInterval";
    public static final String COOKIE_DOMAIN = "cookieDomain";
    public static final String COOKIE_CONTEXT_PATH = "cookieContextPath";
    public static final String TOP_LEVEL_DOMAIN_ENABLE = "tldEnable";
    public static final String SYN_ATTR_REAL_TIME = "synRealTime";

    public static String DEFAULT_SESSION_ID_NAME = "SESSIONID";
    public static String DEFAULT_SESSION_CACHE_KEY_PREFIX = "session";

    private String sessionCookieName = null;
    private String cookieDomain = null;
    private boolean tldEnable = false; // use top level domain for cookie
    private boolean synRealTime = false; //if true syn attr at set/get attr, or syn attr after chain.doFilter
    private String cookieContextPath = null;
    /**
     * session过期时间, 单位为秒
     */
    private int maxInactiveInterval = 8 * 60 * 60;
    /**
     * 会话在缓存中的KEY前辍
     */
    private String sessionCacheKeyPrefix;
    private CacheEngine cache;
    private HttpSessionAttributeListener[] sessionAttributeListeners =
            new HttpSessionAttributeListener[0];
    private HttpSessionListener[] sessionListeners = new HttpSessionListener[0];

    /**
     * 构造一个实例。
     */
    public CacheSessionFilter() {
        super();
    }

    @Override
    public void init(FilterConfig config) throws ServletException {
        super.init(config);
        cache = (CacheEngine) filterConfig.getServletContext().getAttribute(CacheEngineLoadListener.CACHE_USE_HOST_DOMAIN_KEY);
        if (cache == null) {
            throw new ServletException(
                    "Environment variables not found in cache instance.");
        }
        try {
            initParameters();
        } catch (Exception ex) {
        	LOGGER.error(ex.getMessage(), ex);
            throw new ServletException(ex);
        }

    }

    /**
     * 替换原始的Request，修改为
     * com.etnetchina.servlet.wrapper.CacheSessionHttpServletReqeust。
     * 并根据是否新生成了Session来更新客户端的cookie.
     * 
     * @param request 请求。
     * @param response 响应。
     * @param chain 下一个过滤器。
     * @throws java.io.IOException
     * @throws javax.servlet.ServletException
     */
    public void doFilter(
            ServletRequest request,
            ServletResponse response,
            FilterChain chain)
            throws IOException, ServletException {
    	LOGGER.debug("CacheSessionFilter to work.");

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        CacheSessionHttpServletRequest cacheRequest =
                new CacheSessionHttpServletRequest(
                httpRequest,
                httpResponse,
                filterConfig.getServletContext());
        cacheRequest.setSessionCookieName(sessionCookieName);
        cacheRequest.setMaxInactiveInterval(maxInactiveInterval);// ((HttpServletRequest) request).getSession().getMaxInactiveInterval();
        cacheRequest.setCookieDomain(cookieDomain);
        cacheRequest.setCookieContextPath(cookieContextPath);
        cacheRequest.setSessionCacheKeyPrefix(sessionCacheKeyPrefix);
        cacheRequest.setSessionAttributeListeners(sessionAttributeListeners);
        cacheRequest.setSessionListeners(sessionListeners);
        cacheRequest.setTldEnable(tldEnable);
        cacheRequest.setSynRealTime(synRealTime);

        chain.doFilter(cacheRequest, httpResponse);

        //如果创建了Session，那么进行缓存同步。
        CacheHttpSession cacheSession = cacheRequest.currentSession();
        if (cacheSession != null) {
        	// session过期则移除cookie
            if (!cacheSession.synchronizationCache()) {
                WebUtil.setCookieNull(
                        httpRequest,
                        httpResponse,
                        sessionCookieName,
                        cookieDomain,
                        tldEnable,
                        cookieContextPath);
            }
        }
    }

    /**
     * 初始化。
     */
    private void initParameters()
            throws ClassNotFoundException, InstantiationException,
            IllegalAccessException {
        String temp = filterConfig.getInitParameter(COOKIE_SESSION_ID_NAME);
        sessionCookieName = (temp == null) ? DEFAULT_SESSION_ID_NAME : temp;

        temp = filterConfig.getInitParameter(MAX_INACTIVE_INTERVAL);
        if (null != temp) {
        	maxInactiveInterval = Integer.valueOf(temp);
        }

        temp = filterConfig.getInitParameter(COOKIE_DOMAIN);
        cookieDomain = temp;

        temp = filterConfig.getInitParameter(COOKIE_CONTEXT_PATH);
        cookieContextPath = (temp == null) ? "/" : temp;

        temp = filterConfig.getInitParameter(SESSION_CACHE_KEY_PREFIX);
        sessionCacheKeyPrefix = (temp == null) ? DEFAULT_SESSION_CACHE_KEY_PREFIX : temp;

        temp = filterConfig.getInitParameter(TOP_LEVEL_DOMAIN_ENABLE);
        tldEnable = (temp!=null && temp.trim().equalsIgnoreCase("true"))? true : false;

        temp = filterConfig.getInitParameter(SYN_ATTR_REAL_TIME);
        synRealTime = (temp!=null && temp.trim().equalsIgnoreCase("true"))? true : false;

        LOGGER.info("CacheSessionFilter (sessionCookieName={"+sessionCookieName+"}, maxInactiveInterval={"+maxInactiveInterval+"}, " +
                        "cookieDomain={"+cookieDomain+"}, sessionCacheKeyPrefix={"+sessionCacheKeyPrefix+"})");

        initListener();
    }

    /**
     * 初始化监听器
     * @throws ClassNotFoundException
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    private void initListener()
            throws ClassNotFoundException, InstantiationException,
            IllegalAccessException {
        String separator = ",";
        String sessionAttributeListenersParamName = "sessionAttributeListeners";
        String sessionListenersParamName = "sessionListeners";

        String[] listenerClasses;

        //初始化HttpSessionAttributeListener监听器列表
        String listenerStr = filterConfig.getInitParameter(sessionAttributeListenersParamName);
        if (listenerStr != null && !listenerStr.isEmpty()) {
            listenerClasses = listenerStr.split(separator);
            sessionAttributeListeners = new HttpSessionAttributeListener[listenerClasses.length];
            for (int count = 0; count < sessionAttributeListeners.length; count++) {
                sessionAttributeListeners[count] =
                        (HttpSessionAttributeListener) Class.forName(
                        listenerClasses[count]).newInstance();
                LOGGER.info("Instantiated HttpSessionAttributeListener listener instance. [Classname = {"+listenerClasses[count]+"}]");
            }
        }

        //初始化HttpSessionListener监听器列表
        listenerStr = filterConfig.getInitParameter(sessionListenersParamName);
        if (listenerStr != null && !listenerStr.isEmpty()) {
            listenerClasses = listenerStr.split(separator);
            sessionListeners = new HttpSessionListener[listenerClasses.length];
            for (int count = 0; count < sessionListeners.length; count++) {
                sessionListeners[count] =
                        (HttpSessionListener) Class.forName(
                        listenerClasses[count]).newInstance();
                LOGGER.info("Instantiated HttpSessionListener listener instance. [Classname = {"+listenerClasses[count]+"}]");
            }
        }
    }
}
