package com.gozap.session.servlet.util;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.gozap.session.util.IdGenerate;

/**
 * WEB相关的工具方法。
 * 
 * @version 1.00 2009.07.27
 * @since 1.5
 * @author Mike
 */
public class WebUtil {

	private static final Log LOGGER = LogFactory.getLog(WebUtil.class);

	private WebUtil() {
	}

	/**
	 * 取实际用户的访问地址。
	 * 
	 * @param request
	 *            当前请求。
	 * @return 客户端IP地址。
	 */
	public static String getIpAddr(HttpServletRequest request) {
		String ip = request.getHeader("X-Real-IP");
		if (null == ip || ip.isEmpty()) {
			ip = request.getRemoteAddr();
		}
		
		return ip;
	}

	/**
	 * 查找指定请求中的指定名称的Cookie。
	 * 
	 * @param request
	 *            请求。
	 * @param name
	 *            cookie名称。
	 * @return 如果有相应名称的Cookie，则返回相应Cookie实例。没有返回null。
	 */
	public static Cookie findLastCookie(HttpServletRequest request, String name) {
		Cookie c = null;
		if (request == null) {
			return null;
		}
		Cookie[] cookies = request.getCookies();
		if (cookies == null || cookies.length <1) {
			return null;
		}
		for (Cookie cookie : cookies) {
			if (cookie.getName().equals(name)) {
				c = cookie;
			}
		}
		return c;
	}

	public static Cookie findFirstCookie(HttpServletRequest request, String name) {
		Cookie c = null;
		if (request == null) {
			return null;
		}
		Cookie[] cookies = request.getCookies();
		if (cookies == null || cookies.length <1) {
			return null;
		}
		for (Cookie cookie : cookies) {
			if (cookie.getName().equals(name)) {
				c = cookie;
				break;
			}
		}
		return c;
	}

	/**
	 * 查找指定请求中的指定名称Cookie的值，如果不存在将返回null。
	 * 
	 * @param request
	 *            请求。
	 * @param name
	 *            Cookie名称。
	 * @return cookie的值。
	 */
	public static String findCookieValue(HttpServletRequest request, String name) {
		Cookie cookie = findLastCookie(request, name);
		return cookie != null ? cookie.getValue() : null;
	}

	/**
	 * 增加一个Cookie,使用默认域名。
	 * 
	 * @param request
	 *            请求。
	 * @param response
	 *            响应。
	 * @param name
	 *            Cookie名称 。
	 * @param value
	 *            Cookie的值。
	 * #@param tldEnable
     *          top level domain enable
	 * @param maxAge
	 *            生命周期。
	 */
    public static void addCookie(HttpServletRequest request, HttpServletResponse response, String name, String value, int maxAge) {
        addCookie(request, response, name, value, null, false, maxAge);
    }
	public static void addCookie(HttpServletRequest request, HttpServletResponse response, String name, String value, boolean tldEnable,
			int maxAge) {
		addCookie(request, response, name, value, null, tldEnable, maxAge);
	}

	/**
	 * 增加一个Cookie,使用指定域名。
	 * 
	 * @param request
	 *            请求。
	 * @param response
	 *            响应。
	 * @param name
	 *            Cookie名称 。
	 * @param value
	 *            Cookie的值。
	 * @param maxAge
	 *            生命周期。
	 */
	public static void addCookie(HttpServletRequest request, HttpServletResponse response, String name, String value,
			String domain, boolean tldEnable, int maxAge) {
		String contextPath = request.getContextPath();
		if (contextPath == null || contextPath.isEmpty()) {
			contextPath = "/";
		}
		addCookie(request, response, name, value, domain, tldEnable, contextPath, maxAge);
	}

	/**
	 * 增加一个Cookie.ContextPath如果为空或者长度为0，将使用"/".
	 * 
	 * @param request
	 *            当前请求。
	 * @param response
	 *            当前响应。
	 * @param name
	 *            cookie名称
	 * @param value
	 *            cookie值
	 * @param domain
	 *            cookie域名
	 * @param contextPath
	 *            cookie路径。
	 * @param maxAge
	 *            有效时间。
	 */
	public static void addCookie(HttpServletRequest request, HttpServletResponse response, String name, String value,
			String domain, boolean tldEnable,  String contextPath, int maxAge) {
		if (request != null && response != null) {
			Cookie cookie = new Cookie(name, value);
			cookie.setMaxAge(maxAge);
			// 默认都设为false：不管http,还是https都传送此cookie
			cookie.setSecure(false);

			if (contextPath == null || contextPath.isEmpty()) {
				cookie.setPath("/");
			} else {
				cookie.setPath(contextPath);
			}

            String host = getHost(request.getRequestURL().toString());
            String d = domain;
			if (domain != null && !domain.isEmpty() && tldEnable) {
                d = getTopLevelDomain(domain);
			}else if(tldEnable){
                d = getTopLevelDomain(request.getRequestURL().toString());
            }
            if(d!=null && host!=null && !d.equalsIgnoreCase(host)) {
                cookie.setDomain(d);
            }
			response.addCookie(cookie);
			LOGGER.debug(String.format("Cookie update the sessionID.[name={%s},value={%s},maxAge={%s},secure={%s},path={%s},domain={%s}]",cookie.getName(), cookie.getValue(), String.valueOf(cookie.getMaxAge()),
							String.valueOf(cookie.getSecure()), cookie.getPath(), cookie.getDomain()));
		}
	}

    //获取顶级域名
    public static String getTopLevelDomain(String url){
        try {
            URL ul = new URL(url);
            String host = ul.getHost().trim();
            String ipReg = "(\\d+)\\.(\\d+)\\.(\\d+)\\.(\\d+)";
            Matcher ipM = Pattern.compile(ipReg).matcher(host);
            if (ipM.matches()) {
                return host;
            }
            String reg = ".*\\.(\\w+\\.\\w+)$";
            Pattern p = Pattern.compile(reg);
            Matcher m = p.matcher(host);
            if (m.matches()) {
                String baseDomain = m.group(1);
                return baseDomain;
            }
            return host;
        }catch (MalformedURLException e1){
            if (url.indexOf("http") < 0) {
                url = "http://" + url;
                return getTopLevelDomain(url);
            }else{
                return getHost(url);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return getHost(url);
    }

    public static String getHost(String url){
        try{
            URL ul = new URL(url);
            return ul.getHost();
        }catch (MalformedURLException e1){
            if (url.indexOf("http") < 0) {
                url = "http://" + url;
                return getHost(url);
            }else{
                return null;
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

	/**
	 * 失效一个Cookie.
	 * 
	 * @param request
	 *            当前请求。
	 * @param response
	 *            当前响应。
	 * @param name
	 *            Cookie名称。
	 * @param domain
	 *            Cookie域名。
	 * @param contextPath
	 *            有效路径。
	 */
	public static void setCookieNull(HttpServletRequest request, HttpServletResponse response, String name,
                                     String domain, boolean tldEnable, String contextPath) {
		if (request != null && response != null) {
			addCookie(request, response, name, null, domain, tldEnable, contextPath, 0);
		}
	}

	/**
	 * 将指定的Cookie失效掉。
	 * 
	 * @param request
	 *            请求
	 * @param response
	 *            响应。
	 * @param name
	 *            cookie名称。
	 * @param domain
	 *            cookie的域名。
	 */
	public static void setCookieNull(HttpServletRequest request, HttpServletResponse response, String name,
                                     String domain, boolean tldEnable) {
		String contextPath = request.getContextPath();
		if (contextPath == null || contextPath.isEmpty()) {
			contextPath = "/";
		}
		setCookieNull(request, response, name, domain, tldEnable, contextPath);
	}

	/**
	 * 将指定的Cookie失效掉。
	 * 
	 * @param request
	 *            请求
	 * @param response
	 *            响应。
	 * @param name
	 *            cookie名称。
	 */
	public static void setCookieNull(HttpServletRequest request, HttpServletResponse response, String name, boolean tldEnable) {
		setCookieNull(request, response, name, null, tldEnable);
	}

	/**
	 * 获取请求的完整地址。
	 * 
	 * @param request
	 *            请求。
	 * @return 完整地址。
	 */
	public static String completeTheRequestAddress(HttpServletRequest request) {
		StringBuilder buff = new StringBuilder(request.getRequestURL().toString());
		String queryString = request.getQueryString();
		if (queryString != null) {
			buff.append("?").append(queryString);
		}

		return buff.toString();
	}

	/**
	 * 将换行符替换成html页面使用的换行元素。
	 * 
	 * @param source
	 *            原始字符串。
	 * @return 替换后的字符串。
	 */
	public static String enterToHtmlWrap(String source) {
		if (source == null || source.trim().isEmpty()) {
			return source;
		} else {
			return source.replaceAll("\r\n", "<br />");
		}
	}

	/**
	 * 一个客户端转向的方便工具方法.可以选择使用301或者302方式进行跳转.
	 * 
	 * @param response
	 *            当前响应.
	 * @param url
	 *            需要转向的地址.
	 * @param movePermanently
	 *            true表示进行301永久跳转,false表示302临时跳转.
	 * @throws IOException
	 *             I/O异常.
	 */
	public static void redirect(HttpServletResponse response, String url, boolean movePermanently) throws IOException {
		if (!movePermanently) {
			response.sendRedirect(url);
		} else {
			response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
			response.setHeader("Location", url);
		}
	}

	/**
	 * 代理的名称,也代理了判断的顺序..
	 */
	private static final String[] AGENT_INDEX = { "MSIE", "Firefox", "Chrome", "Opera", "Safari" };
	/**
	 * 存放用户代理解析的正则容器.
	 */
	private static final Map<String, Pattern> AGENT_PATTERNS = new HashMap<String, Pattern>(AGENT_INDEX.length);

	static {
		AGENT_PATTERNS.put(AGENT_INDEX[0], Pattern.compile("MSIE ([\\d.]+)"));
		AGENT_PATTERNS.put(AGENT_INDEX[1], Pattern.compile("Firefox/(\\d.+)"));
		AGENT_PATTERNS.put(AGENT_INDEX[2], Pattern.compile("Chrome/([\\d.]+)"));
		AGENT_PATTERNS.put(AGENT_INDEX[3], Pattern.compile("Opera[/\\s]([\\d.]+)"));
		AGENT_PATTERNS.put(AGENT_INDEX[4], Pattern.compile("Version/([\\d.]+)"));
	}

	/**
	 * 获取用户代理信息.
	 * 
	 * @param userAgent
	 *            用户代理信息字符串.
	 * @return 用户代理信息.
	 */
	public static UserAgent checkUserAgent(String userAgent) {
		if (userAgent == null || userAgent.isEmpty()) {
			return null;
		}

		Pattern pattern = null;
		Matcher matcher = null;
		for (int point = 0; point < AGENT_INDEX.length; point++) {
			pattern = AGENT_PATTERNS.get(AGENT_INDEX[point]);
			matcher = pattern.matcher(userAgent);
			if (matcher.find()) {
				return new UserAgent(AGENT_INDEX[point], matcher.group(1));
			} else {
				continue;
			}
		}
		return null;
	}

	/**
	 * 获取指定请求中的用户代理.
	 * 
	 * @param request
	 *            请求.
	 * @return 用户代理信息.
	 */
	public static UserAgent checkUserAgent(HttpServletRequest request) {
		if (request == null) {
			return null;
		}
		String userAgentHead = request.getHeader("User-Agent");
		return checkUserAgent(userAgentHead);
	}

	/**
	 * 表示一个用户代理的信息.
	 */
	public static class UserAgent {

		private String name = "";
		private String version = "";

		/**
		 * 构造一个用户代理信息.
		 * 
		 * @param name
		 *            代理名称.
		 * @param version
		 *            代理版本号.
		 */
		public UserAgent(String name, String version) {
			this.name = name;
			this.version = version;
		}

		/**
		 * 获取代理名称.
		 * 
		 * @return 代理名称.
		 */
		public String getName() {
			return name;
		}

		/**
		 * 获取版本号.
		 * 
		 * @return 版本号.
		 */
		public String getVersion() {
			return version;
		}
	}

	/**
	 * 票据名称
	 */
	public static final String TICKET_NAME = "ticket";

	/**
	 * 创建票据.
	 * 
	 * @param request
	 *            当前请求
	 */
	public static String createTicket(HttpServletRequest request) {
		HttpSession session = request.getSession();
		String ticket = IdGenerate.getUUIDString();
		session.setAttribute(TICKET_NAME, ticket);

		return ticket;
	}

	/**
	 * 验证票据. 请求中必须带有票据数据.
	 * 
	 * @param request
	 *            当前请求.
	 * @return true验证通过,false验证不通过.
	 */
	public static boolean testTicket(HttpServletRequest request) {

		HttpSession session = request.getSession();
		String serverTicket = (String) session.getAttribute(TICKET_NAME);
		String clientTicket = request.getParameter(TICKET_NAME);

		try {
			if (serverTicket == null) {
				return true;
			} else {
				if (serverTicket.equals(clientTicket)) {
					return true;
				} else {
					return false;
				}
			}
		} finally {
			session.removeAttribute(TICKET_NAME);
		}
	}
}
