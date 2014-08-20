package com.scipublish.sm.servlet.session;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;
import javax.servlet.http.HttpSessionContext;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.scipublish.sm.cache.CacheEngine;
import com.scipublish.sm.servlet.listener.cache.CacheEngineLoadListener;
import com.scipublish.sm.util.CheckUtil;

/**
 * 一个HttpSession的实现，实际的属性会储存在指定的缓存实现中。
 *
 * 以下方法没有实现。调用将抛出UnsupportedOperationException异常。
 *   public HttpSessionContext getSessionContext();
 *
 * 现在底层将Session的信息和储存在Session中的信息分别进行储存。只有当Session中的键值
 * 对属性被改变时才会进行缓存的同步。
 *
 * @author Mike
 * @version 2.1 2011-04-07
 * @since 1.5
 */
public class CacheHttpSession implements HttpSession {
	
	public static final Logger LOGGER = LoggerFactory.getLogger(CacheHttpSession.class);

    private String id;
    private CacheEngine cache;
    // 会话过期时间，单位秒，默认8小时
    private int maxInactiveInterval;
    // session在缓存中的key的前缀
    private String sessionCacheKeyPrefix;
    private ServletContext context;
    // session缓存的头信息及session本身的信息在缓存中的key
    private final String sessionCacheKeyHeader;
    // session缓存的属性信息在缓存的key
    private final String sessionCacheKeyAttribute;
    // session缓存的头信息及session本身的信息
    private CacheSessionHeader sessionHeader;
    // session缓存的属性信息
    private CacheSessionAttribute sessionAttribute;
    // session是否合法标示
    private boolean invalid = false;
    //是否需同步缓存（只有改变Session中的键值对才会进行同步）
    private boolean update = false;
    private HttpSessionAttributeListener[] sessionAttributeListeners;
    private HttpSessionListener[] sessionListeners;

    /**
     * 初始化时必须指定一个id字符串和缓存引擎实现，以及缓存的key前缀
     * @param servletContext
     * @param id
     * @param sessionCacheKeyPrefix
     */
    public CacheHttpSession(ServletContext servletContext, String id, String sessionCacheKeyPrefix) {
        this.sessionCacheKeyPrefix = sessionCacheKeyPrefix;
        this.id = id;
        this.cache = (CacheEngine) servletContext.getAttribute(CacheEngineLoadListener.CACHE_USE_HOST_DOMAIN_KEY);
        this.context = servletContext;
        sessionCacheKeyHeader = this.sessionCacheKeyPrefix + this.id + ".hd";
        sessionCacheKeyAttribute = this.sessionCacheKeyPrefix + this.id + ".attr";
    }

    /**
     * 此Session的ID值。
     * @return id值。
     */
    public String getId() {
        return id;
    }

    /**
     * 获取此Session的创建时间。
     * @return 创建时间。
     */
    public long getCreationTime() {
        return sessionHeader.getCreateTime();
    }

    /**
     * 获取最后访问时间。
     * @return 最后访问时间。
     */
    public long getLastAccessedTime() {
        return sessionHeader.getLastAccessTime();
    }

    /**
     * 是否用户键值对被改变过。
     * @return true被改变过，false没有改变。
     */
    public boolean isUpdate() {
        return update;
    }

    /**
     * 获取当前的属性键值对。
     * @return 属性键值对。
     */
    public CacheSessionAttribute getSessionAttribute() {
        return sessionAttribute;
    }

    /**
     * 获取当前的Session本身属性。
     * @return 本身属性。
     */
    public CacheSessionHeader getSessionHeader() {
        return sessionHeader;
    }

    /**
     * 表示此Session被访问。
     */
    public void access() {
        sessionHeader.setLastAccessTime(Calendar.getInstance().getTimeInMillis());
    }

    /**
     * 获取相关的环境变量。
     * @return 环境变量。
     */
    public ServletContext getServletContext() {
        return this.context;
    }

    /**
     * 设定Session的最长不活动时限(秒），如果此时限没有活动的Session将被删除。
     * @param maxInactiveInterval 最长活动时限。
     */
    public void setMaxInactiveInterval(int maxInactiveInterval) {
        this.maxInactiveInterval = maxInactiveInterval;
    }

    /**
     * 获取最长不活动时限(秒）。
     * @return 最长活动时限。
     */
    public int getMaxInactiveInterval() {
        return maxInactiveInterval;
    }

    public String getSessionCacheKeyPrefix() {
        return sessionCacheKeyPrefix;
    }

    public void setSessionCacheKeyPrefix(String sessionCacheKeyPrefix) {
        this.sessionCacheKeyPrefix = sessionCacheKeyPrefix;
    }

    /**
     * 获取属性值。
     * @param attributeName 属性名称。
     * @return 属性值。
     */
    public Object getAttribute(String attributeName) {
        checkSessionInvalild();
        return findCacheSessionAttribute().getAttribute(attributeName);
    }

    /**
     * 获取属性名称集合。
     * @return 属性名称集合。
     */
    public Enumeration getAttributeNames() {
        checkSessionInvalild();
        Set<String> attributeNameSet = findCacheSessionAttribute().getAttributes().keySet();
        Enumeration enumeration = new CacheEnumeration(attributeNameSet);
        return enumeration;
    }

    /**
     * 更新属性。
     * @param attributeName 属性名称.
     * @param attributeValue 属性值。
     */
    public void setAttribute(String attributeName, Object attributeValue) {
        checkSessionInvalild();

        Object oldValue = findCacheSessionAttribute().getAttribute(attributeName);

        findCacheSessionAttribute().putAttribute(attributeName, attributeValue);
        update = true;

        doHttpSessionBindingListener(attributeName, attributeValue,
                AccessType.SET_ATTRIBUTE);

        if (oldValue == null) {
            doHttpSessionAttributeListener(attributeName, attributeValue,
                    AccessType.SET_ATTRIBUTE);
        } else {
            doHttpSessionAttributeListener(attributeName, oldValue,
                    AccessType.REPLACE_ATTRIBUTE);
        }
    }

    /**
     * 移除已有的属性。
     * @param attributeName 属性名称。
     */
    public void removeAttribute(String attributeName) {
        checkSessionInvalild();
        Object value = findCacheSessionAttribute().removeAttribute(attributeName);
        update = true;

        doHttpSessionBindingListener(attributeName, value,
                AccessType.REMOVE_ATTRIBUTE);

        doHttpSessionAttributeListener(attributeName, value,
                AccessType.REMOVE_ATTRIBUTE);
    }

    /**
     * Session过期。
     */
    public void invalidate() {
        LOGGER.debug("Session {}, to fail.", id);

        doHttpSessionListener(AccessType.REMOVE_ATTRIBUTE);

        invalid = true;
    }

    /**
     * 判断是否已经超过了最大活动时间。
     * @return true超过，false没有超过。
     */
    public boolean isInvalid() {
        if (invalid) {
            return invalid;
        } else {
            if (getMaxInactiveInterval() <= 0) {
                invalid = false;

                LOGGER.debug("Session [{}] non-perishable.", id);
            } else {
                long invalidMillis = getMaxInactiveInterval() * 1000;
                long lastAccessTime = getLastAccessedTime();
                long now = System.currentTimeMillis();
                invalid = (now - lastAccessTime) > invalidMillis;

                LOGGER.debug(
                        "Session {}, last access time {}, {} the current time. valid status: [{}].",
                        new Object[]{id,
                        lastAccessTime,
                        now,
                        !invalid});
            }

            return invalid;
        }
    }

    /**
     * 判断此Session是否为新的。
     * @return true 为新的，false为非新的。
     */
    public boolean isNew() {
        checkSessionInvalild();
        return sessionHeader.isNewbuild();
    }

    @Override
    public int hashCode() {
        return sessionCacheKeyHeader.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final CacheHttpSession other = (CacheHttpSession) obj;
        if ((id == null) ? (other.getId() != null) : !id.equals(other.getId())) {
            return false;
        }
        return true;
    }

    /**
     * 获取当前HttpSessionAttributeListener监听器列表.
     * @return HttpSessionAttributeListener实例列表.
     */
    public HttpSessionAttributeListener[] getSessionAttributeListeners() {
        return Arrays.copyOf(sessionAttributeListeners,
                sessionAttributeListeners.length);
    }

    /**
     * 设置HttpSessionAttributeListener监听器列表.
     * @param sessionAttributeListeners HttpSessionAttributeListener监听器列表.
     */
    public void setSessionAttributeListeners(
            HttpSessionAttributeListener[] sessionAttributeListeners) {
        if (sessionAttributeListeners != null) {
            this.sessionAttributeListeners =
                    Arrays.copyOf(sessionAttributeListeners,
                    sessionAttributeListeners.length);
        }
    }

    /**
     * 获取HttpSessionListener监听器列表.
     * @return HttpSessionListener监听器列表.
     */
    public HttpSessionListener[] getSessionListeners() {
        return Arrays.copyOf(sessionListeners, sessionListeners.length);
    }

    /**
     * 设置 HttpSessionListener监听器列表.
     * @param sessionListeners HttpSessionListener监听器列表.
     */
    public void setSessionListeners(HttpSessionListener[] sessionListeners) {
        if (sessionListeners != null) {
            this.sessionListeners = Arrays.copyOf(sessionListeners,
                    sessionListeners.length);
        }
    }

    @Override
    public String toString() {
        String message = "Session id is {0},key is {1}.";
        return CheckUtil.replaceArgs(message, getId(), sessionCacheKeyHeader);
    }

    /**
     * 没有实现。
     * @return
     * @deprecated 
     */
    public HttpSessionContext getSessionContext() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * 实际调用removeAttribute方法。
     * @param name 需要删除的属性名称。
     * @deprecated 已经过时，请使用removeAttribute.
     */
    public void removeValue(String name) {
        removeAttribute(name);
    }

    /**
     * 实际调用setAttribute方法。
     * @param name 属性名称。
     * @param value 属性方法。
     * @deprecated 已经过时，请使用setAttribute.
     */
    public void putValue(String name, Object value) {
        setAttribute(name, value);
    }

    /**
     * 返回getAttributeNames方法的返回结果集。
     * @return 属性名称数组。
     * @deprecated 已经过时，请使用getAttributeNames.
     */
    public String[] getValueNames() {
        Enumeration attributeNames = getAttributeNames();
        List<String> attributeNameList = new ArrayList<String>();
        while (attributeNames.hasMoreElements()) {
            attributeNameList.add((String) attributeNames.nextElement());
        }

        return attributeNameList.toArray(new String[0]);
    }

    /**
     * 返回getAttribute的调用结果。
     * @param name 属性名称。
     * @return 属性值。
     * @deprecated 已经过时，请使用getAttribute.
     */
    public Object getValue(String name) {
        return getAttribute(name);
    }

    /**
     * 更新当前请求至缓存。如果已经失效，将直接删除。
     * @return false此缓存已经失效,true缓存继续有效。
     */
    public boolean synchronizationCache() {
        if (invalid) {
            removeRemoteSessionForCache();

            LOGGER.debug("Session [{}] has failed and empty the cache.", id);

            return false;
        } else {
            //头信息每次都需要同步
            updateCacheSessionHeader(sessionHeader);

            //属性键值对只有当改变时才更新。
            if (update) {
                updateCacheSessionAttribute(sessionAttribute);

                LOGGER.debug(
                        "Session[{}] information to the cache synchronization.",
                        id);
            }

            update = false;

            return true;
        }
    }

    /**
     * 初始化方法。
     * 初始化日志记录器。
     * 初始化缓存中的属性容器。
     * 如果缓存中没有相就内容即新建并设定创建时间和最后访问时间为当前时间和为新的会话。
     */
    public void init() {
        if (!cache.containsKey(sessionCacheKeyHeader)) {

            LOGGER.debug(
                    "Cache {} does not exist in the specified session container, so a creation.",
                    sessionCacheKeyHeader);

            initCacheSessionHeader(true);
            //新键需要同步缓存
            update = true;
        } else {
            
            LOGGER.debug(
                    "{} exists in the cache specified in the session container to update the attribute (isNew = false).",
                    sessionCacheKeyHeader);
            sessionHeader = findCacheSessionHeader();
            sessionHeader.setNewbuild(false);
            //不需要同步缓存,除非有属性更新。
            update = false;
        }

        doHttpSessionListener(AccessType.SET_ATTRIBUTE);
    }

    /**
     * 初始化一个新的Session基本信息。
     */
    private void initCacheSessionHeader(boolean newBuild) {
        long currentMills = System.currentTimeMillis();
        sessionHeader = new CacheSessionHeader(currentMills);
        sessionHeader.setLastAccessTime(currentMills);
        sessionHeader.setNewbuild(newBuild);
        sessionAttribute = new CacheSessionAttribute();

        LOGGER.info("Init a session, session id is '{}'. session header is [{}].", this.id, sessionHeader);
    }

    /**
     * 操作类型
     */
    private enum AccessType {

        SET_ATTRIBUTE,//set
        REMOVE_ATTRIBUTE,//remove
        REPLACE_ATTRIBUTE//replace
    }

    /**
     * 属性值如何实现了HttpSessionBindingListener的处理方法.
     * @param name 操作的属性名称.
     * @param value 操作的属性值.
     * @param type
     */
    private void doHttpSessionBindingListener(String name, Object value, AccessType type) {
        if (HttpSessionBindingListener.class.isInstance(value)) {
            HttpSessionBindingListener listener =
                    (HttpSessionBindingListener) value;
            HttpSessionBindingEvent event =
                    new HttpSessionBindingEvent(this, name, value);

            try {
                switch (type) {
                    case SET_ATTRIBUTE:
                        listener.valueBound(event);
                        break;
                    case REMOVE_ATTRIBUTE:
                        listener.valueUnbound(event);
                        break;
                    default:
                    	break;
                }
            } catch (Exception ex) {
                LOGGER.error(ex.getMessage(), ex);
            }
        }
    }

    /**
     * 操作所有的HttpSessionAttributeListener监听器.
     * @param name 操作的属性名称.
     * @param value 属性值.
     * @param type
     */
    private void doHttpSessionAttributeListener(String name, Object value, AccessType type) {
        HttpSessionAttributeListener listener;
        HttpSessionBindingEvent event = new HttpSessionBindingEvent(this, name, value);

        LOGGER.debug("cache Http sessionAttributeListeners: {}", Arrays.
                toString(sessionAttributeListeners));

        for (int count = 0; count < sessionAttributeListeners.length; count++) {
            listener = sessionAttributeListeners[count];
            try {
                switch (type) {
                    case SET_ATTRIBUTE:
                        listener.attributeAdded(event);
                        break;
                    case REMOVE_ATTRIBUTE:
                        listener.attributeRemoved(event);
                        break;
                    case REPLACE_ATTRIBUTE:
                        listener.attributeReplaced(event);
                        break;
                }
            } catch (Exception ex) {
                LOGGER.error(ex.getMessage(), ex);
                continue;
            }
        }
    }

    /**
     * 操作Session创建和销毁的监听器列表.
     * @param type 操作类型.
     */
    private void doHttpSessionListener(AccessType type) {
        HttpSessionListener listener;
        HttpSessionEvent event = new HttpSessionEvent(this);

        LOGGER.debug("cache Http Session listeners: {}", Arrays.toString(sessionListeners));

        for (int count = 0; count < sessionListeners.length; count++) {
            listener = sessionListeners[count];
            try {
                switch (type) {
                    case SET_ATTRIBUTE:
                        listener.sessionCreated(event);
                        break;
                    case REMOVE_ATTRIBUTE:
                        listener.sessionDestroyed(event);
                        break;
                    default:
                    	break;
                }
            } catch (Exception ex) {
                LOGGER.error(ex.getMessage(), ex);
                continue;
            }
        }
    }

    /**
     * 判断当前Session是否已经失效.
     * @throws IllegalStateException Session已经失效的异常.
     */
    private void checkSessionInvalild() throws IllegalStateException {
        if (invalid) {
            throw new IllegalStateException("Session is invalid.");
        }
    }

    /**
     * 查找缓存中的会话属性容器。如果缓存是失效的将会抛出IllegalStateException异常。
     * @return 会话属性容器。
     */
    private CacheSessionHeader findCacheSessionHeader() throws
            IllegalStateException {
        CacheSessionHeader header = null;
        try {
            header = (CacheSessionHeader) cache.get(sessionCacheKeyHeader);
            if (null == header) {
            	//应该找到的远程容器没有找到，所以重新构造一个。原有属性将丢失。
                LOGGER.warn("SessionCacheKey[{}] is not found.",
                        sessionCacheKeyHeader);
                initCacheSessionHeader(false);
                header = sessionHeader;
            }
        } catch (Exception e) {
        	LOGGER.error("Cache engine is error!", e);
        }

        return header;
    }

    /**
     * 查找一个缓存中的属性储存bean.如果不存在将返回一个新的空BEAN.
     * @return 用户Session属性键键值对储存bean.
     */
    private CacheSessionAttribute findCacheSessionAttribute() {
        CacheSessionAttribute attribute = sessionAttribute;
        if (attribute != null) {
            return attribute;
        }
        try {
        	@SuppressWarnings("unchecked")
			Map<String, Object> attrs = (Map<String, Object>)cache.get(sessionCacheKeyAttribute);
            if (null == attrs) {
            	//应该找到的远程容器没有找到，所以重新构造一个。原有属性将丢失。
                LOGGER.warn("SessionCacheKey[{}] is not found.", sessionCacheKeyAttribute);
                attribute = new CacheSessionAttribute();
            } else {
            	attribute = new CacheSessionAttribute();
            	attribute.setAttributes(attrs);
            }
            
        } catch (Exception e) {
        	LOGGER.error("Cache engine is error!", e);
        }

        sessionAttribute = attribute;
        return attribute;
    }

    /**
     * 更新缓存中的Session属性。
     * @param header Session属性。
     */
    private void updateCacheSessionHeader(CacheSessionHeader header) {
        cache.put(sessionCacheKeyHeader, header, this.maxInactiveInterval);
    }

    /**
     * 更新缓存中的Session的属性键值对。
     * @param attribute Session中的键值对。
     */
    private void updateCacheSessionAttribute(CacheSessionAttribute attribute) {
        cache.put(sessionCacheKeyAttribute, attribute.getAttributes(), this.maxInactiveInterval);
    }

    /**
     * 移除缓存中的属性容器。
     */
    private void removeRemoteSessionForCache() {
        cache.remove(sessionCacheKeyHeader);
        cache.remove(sessionCacheKeyAttribute);
    }

    /**
     * 迭代器的实现。
     */
    private static class CacheEnumeration implements Enumeration {

        private Iterator iter;

        public CacheEnumeration(Set<String> attributeNames) {
            this.iter = attributeNames.iterator();
        }

        public boolean hasMoreElements() {
            return iter.hasNext();
        }

        public Object nextElement() {
            return iter.next();
        }
    }

    /**
     * 实际进行缓存的储存Session的基本信息.
     */
    public static class CacheSessionHeader {

        private boolean newbuild;
        private long lastAccessTime;
        private long createTime;

        //用以序列化的构造方法
        public CacheSessionHeader() {
        }

        public CacheSessionHeader(long createTime) {
            this.createTime = createTime;
        }

        public long getCreateTime() {
            return createTime;
        }

        public void setCreateTime(long createTime) {
            this.createTime = createTime;
        }

        public long getLastAccessTime() {
            return lastAccessTime;
        }

        public void setLastAccessTime(long lastAccessTime) {
            this.lastAccessTime = lastAccessTime;
        }

        public boolean isNewbuild() {
            return newbuild;
        }

        public void setNewbuild(boolean newbuild) {
            this.newbuild = newbuild;
        }

        @Override
        public String toString() {
            StringBuilder buff = new StringBuilder();
            buff.append("CacheSessionHeader{").append("newbuild=").append(
                    newbuild);
            buff.append(",lastAccessTime=").append(lastAccessTime);
            buff.append(",createTime=").append(createTime);
            buff.append("}");
            return buff.toString();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final CacheSessionHeader other = (CacheSessionHeader) obj;
            if (this.newbuild != other.newbuild) {
                return false;
            }
            if (this.lastAccessTime != other.lastAccessTime) {
                return false;
            }
            if (this.createTime != other.createTime) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 29 * hash + (newbuild ? 1 : 0);
            hash = 29 * hash + (int) (lastAccessTime ^ (lastAccessTime >>> 32));
            hash = 29 * hash + (int) (createTime ^ (createTime >>> 32));
            return hash;
        }
    }

    /**
     * Session的相关属性键值对储存Bean.
     */
    public static class CacheSessionAttribute {

        private Map<String, Object> attributes;

        public CacheSessionAttribute() {
            attributes = new HashMap<String, Object>();
        }
        
        public Map<String, Object> getAttributes() {
			return attributes;
		}

		public void setAttributes(Map<String, Object> attributes) {
			this.attributes = attributes;
		}

		public void putAttribute(String name, Object value) {
            attributes.put(name, value);

            LOGGER.debug(
                    "Attribute [name = {}, value = {}], into the Session.",
                    name,
                    value);
        }

        public Object removeAttribute(String name) {
            Object value = attributes.remove(name);

            LOGGER.debug(
                    "From the Session {} removed property.",
                    name);

            return value;
        }

        public Object getAttribute(String name) {
            Object value = attributes.get(name);

            LOGGER.debug(
                    "Session to obtain property from [name ={},value={}].",
                    name,
                    value);

            return value;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final CacheSessionAttribute other = (CacheSessionAttribute) obj;
            if (this.attributes != other.attributes && (this.attributes == null
                    || !this.attributes.equals(other.attributes))) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 61 * hash + (this.attributes != null ? this.attributes.hashCode() : 0);
            return hash;
        }
    }
}
