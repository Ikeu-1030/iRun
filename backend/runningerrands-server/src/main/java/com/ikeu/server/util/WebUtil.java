package com.ikeu.server.util;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Web 工具类，提供客户端 IP 提取等常用 HTTP 操作方法。
 * @author ikeu
 * @since 2026/06/19
 */
public final class WebUtil {

    private WebUtil() {}

    /**
     * 从 HTTP 请求中提取客户端真实 IP，优先取 X-Forwarded-For 头（兼容反向代理）。
     * 多级代理时取最左侧第一个 IP。
     *
     * <p><b>安全说明</b>：X-Forwarded-For 可被客户端伪造，因此本方法仅适用于
     * 速率限制等非安全关键场景，不得用于鉴权。生产环境必须在反向代理层
     * （Nginx/ALB）配置剥离客户端自带的 X-Forwarded-For 头。
     */
    public static String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) {
            ip = request.getRemoteAddr();
        }
        if (ip == null) return "unknown";
        int comma = ip.indexOf(',');
        if (comma > 0) {
            ip = ip.substring(0, comma).trim();
        }
        return ip;
    }
}
