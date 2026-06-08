package com.ikeu.server.interceptor;

import com.ikeu.common.constant.MessageConstant;
import com.ikeu.common.context.BaseContext;
import com.ikeu.common.properties.JwtProperties;
import com.ikeu.common.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 管理端 JWT 拦截器 —— 校验 access token 并将管理员 ID 放入 ThreadLocal。
 * <p>仅拦截 {@code /admin/**} 路径（login、refresh、captcha 除外），
 * token 从 {@link JwtProperties#getAdminTokenName()} 配置的 HTTP 头中提取。
 * @author ikeu
 * @since 2025/05/21
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenAdminInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        // 1. 从 HTTP 头提取 access token
        String token = request.getHeader(jwtProperties.getAdminTokenName());
        if (token == null || token.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"msg\":\"" + MessageConstant.TOKEN_NOT_FOUND + "\"}");
            return false;
        }

        // 2. 验证 access token 签名及有效期
        try {
            log.debug("管理端 jwt 校验, token length: {}", token.length());
            Claims claims = jwtUtil.parseAdminAccessToken(token);
            Long adminId = jwtUtil.getAdminIdFromClaims(claims);
            log.info("当前管理员 id: {}", adminId);

            // 3. 存入 ThreadLocal，供后续请求处理使用
            BaseContext.setCurrentId(adminId);
            return true;
        } catch (Exception e) {
            log.error("管理端 token 校验失败: {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"msg\":\"" + MessageConstant.TOKEN_EXPIRED + "\"}");
            return false;
        }
    }

    /**
     * 请求完成后清理 ThreadLocal，防止内存泄漏。
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        BaseContext.removeCurrentId();
    }
}