package com.scipublish.sm.cache.support;

import java.util.Set;

/**
 * jedis基础配置
 * @author chenboxiang
 * @date 2013-6-17 下午4:38:22
 * @since JDK1.6
 * @version
 */
public class JedisBaseConfig {
	private String host;
	private int port;
    private int database;
	private String password;
	private int timeout;
    private String masterName;
    private Set<String> sentinels;
	
	/**
	 * get {@link #host}
	 * @return the host
	 */
	public String getHost() {
		return host;
	}
	/**
	 * set {@link #host}
	 * @param host the host to set
	 */
	public void setHost(String host) {
		this.host = host;
	}
	/**
	 * get {@link #port}
	 * @return the port
	 */
	public int getPort() {
		return port;
	}
	/**
	 * set {@link #port}
	 * @param port the port to set
	 */
	public void setPort(int port) {
		this.port = port;
	}

    public int getDatabase() {
        return database;
    }

    public void setDatabase(int database) {
        this.database = database;
    }

    /**
	 * get {@link #password}
	 * @return the password
	 */
	public String getPassword() {
		return password;
	}
	/**
	 * set {@link #password}
	 * @param password the password to set
	 */
	public void setPassword(String password) {
		this.password = password;
	}
	/**
	 * get {@link #timeout}
	 * @return the timeout
	 */
	public int getTimeout() {
		return timeout;
	}
	/**
	 * set {@link #timeout}
	 * @param timeout the timeout to set
	 */
	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

    public String getMasterName() {
        return masterName;
    }

    public void setMasterName(String masterName) {
        this.masterName = masterName;
    }

    public Set<String> getSentinels() {
        return sentinels;
    }

    public void setSentinels(Set<String> sentinels) {
        this.sentinels = sentinels;
    }
}
