package com.gozap.session.servlet.filter;

import javax.servlet.Filter;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * 过滤器的超类。
 * 
 * @author Mike
 * @version 1.00 2010-6-25
 * @since 1.5
 */
public abstract class BaseFilter implements Filter {
	protected FilterConfig filterConfig;
	private static final Log LOGGER = LogFactory.getLog(BaseFilter.class);

	/**
	 * 初始化的实现。
	 * 
	 * @param config
	 * @throws javax.servlet.ServletException
	 */
	@Override
	public void init(FilterConfig config) throws ServletException {
		this.filterConfig = config;

		LOGGER.info("{"+getClass().getName()+"} filter initialization.");
	}

	/**
	 * 销毁方法。
	 */
	@Override
	public void destroy() {
		LOGGER.info("{"+getClass().getName()+"} filter destroy.");
	}
}
