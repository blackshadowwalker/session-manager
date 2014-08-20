package com.scipublish.sm.servlet.filter;

import javax.servlet.Filter;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 过滤器的超类。
 * 
 * @author Mike
 * @version 1.00 2010-6-25
 * @since 1.5
 */
public abstract class BaseFilter implements Filter {

	protected FilterConfig filterConfig;

	private static final Logger LOGGER = LoggerFactory.getLogger(BaseFilter.class);

	/**
	 * 初始化的实现。
	 * 
	 * @param config
	 * @throws javax.servlet.ServletException
	 */
	@Override
	public void init(FilterConfig config) throws ServletException {
		this.filterConfig = config;

		LOGGER.info("{} filter initialization.", getClass().getName());
	}

	/**
	 * 销毁方法。
	 */
	@Override
	public void destroy() {
		LOGGER.info("{} filter destroy.", getClass().getName());
	}
}
