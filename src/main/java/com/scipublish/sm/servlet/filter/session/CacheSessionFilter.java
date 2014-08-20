package com.scipublish.sm.servlet.filter.session;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.scipublish.sm.cache.CacheEngine;
import com.scipublish.sm.servlet.filter.BaseFilter;
import com.scipublish.sm.servlet.listener.cache.CacheEngineLoadListener;
import com.scipublish.sm.servlet.session.CacheHttpSession;
import com.scipublish.sm.servlet.util.WebUtil;
import com.scipublish.sm.servlet.wrapper.CacheSessionHttpServletRequest;

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
	
	private static final Logger LOGGER = LoggerFactory.getLogger(CacheSessionFilter.class);

    private String sessionCookieName = null;
    private String cookieDomain = null;
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
        cacheRequest.setMaxInactiveInterval(maxInactiveInterval);
        cacheRequest.setCookieDomain(cookieDomain);
        cacheRequest.setCookieContextPath(cookieContextPath);
        cacheRequest.setSessionCacheKeyPrefix(sessionCacheKeyPrefix);
        cacheRequest.setSessionAttributeListeners(sessionAttributeListeners);
        cacheRequest.setSessionListeners(sessionListeners);

        chain.doFilter(cacheRequest, httpResponse);

        //如果创建了Session，那么进行缓存同步。
        CacheHttpSession cacheSession = cacheRequest.currentSession();
        if (cacheSession != null) {
        	// session过期则移除cookie
            if (!cacheSession.synchronizationCache()) {
                WebUtil.failureCookie(
                        httpRequest,
                        httpResponse,
                        sessionCookieName,
                        cookieDomain,
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
        String sessionCookieNameParameter = "sessionCookieName";
        String maxInactiveIntervalParameter = "maxInactiveInterval";
        String cookieDomainParameter = "cookieDomain";
        String cookieContextPathParameter = "cookieContextPath";

        String temp = filterConfig.getInitParameter(sessionCookieNameParameter);
        sessionCookieName = (temp == null) ? "SESSIONID" : temp;

        temp = filterConfig.getInitParameter(maxInactiveIntervalParameter);
        if (null != temp) {
        	maxInactiveInterval = Integer.valueOf(temp);
        }

        temp = filterConfig.getInitParameter(cookieDomainParameter);
        cookieDomain = temp;

        temp = filterConfig.getInitParameter(cookieContextPathParameter);
        cookieContextPath = (temp == null) ? "/" : temp;

        temp = filterConfig.getInitParameter("sessionCacheKeyPrefix");
        sessionCacheKeyPrefix = (temp == null) ? "s." : temp;

        LOGGER.info(
                "CacheSessionFilter (sessionCookieName={}, maxInactiveInterval={}, cookieDomain={}, sessionCacheKeyPrefix={})",
                new Object[]{sessionCookieName,
                    maxInactiveInterval,
                    cookieDomain,
                    sessionCacheKeyPrefix});

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
        String listenerStr =
                filterConfig.getInitParameter(sessionAttributeListenersParamName);
        if (listenerStr != null && !listenerStr.isEmpty()) {

            listenerClasses = listenerStr.split(separator);

            sessionAttributeListeners =
                    new HttpSessionAttributeListener[listenerClasses.length];

            for (int count = 0; count < sessionAttributeListeners.length; count++) {

                sessionAttributeListeners[count] =
                        (HttpSessionAttributeListener) Class.forName(
                        listenerClasses[count]).newInstance();

                LOGGER.info(
                        "Instantiated HttpSessionAttributeListener listener instance. [Classname = {}]",
                        listenerClasses[count]);
            }
        }

        //初始化HttpSessionListener监听器列表
        listenerStr =
                filterConfig.getInitParameter(sessionListenersParamName);

        if (listenerStr != null && !listenerStr.isEmpty()) {

            listenerClasses = listenerStr.split(separator);

            sessionListeners =
                    new HttpSessionListener[listenerClasses.length];

            for (int count = 0; count < sessionListeners.length; count++) {
                sessionListeners[count] =
                        (HttpSessionListener) Class.forName(
                        listenerClasses[count]).newInstance();

                LOGGER.info(
                        "Instantiated HttpSessionListener listener instance. [Classname = {}]",
                        listenerClasses[count]);
            }
        }
    }
}
