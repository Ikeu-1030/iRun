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
 * 用户端 JWT 拦截器 —— 校验 access token 并将用户 ID 放入 ThreadLocal。
 *
 * <p>支持从 HTTP 头或请求参数中获取 token，
 * token 名称由 {@link JwtProperties#getUserTokenName()} 配置。
 * @author ikeu
 * @since 2025/05/21
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenUserInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        // 1. 从 HTTP 头获取 token
        String token = request.getHeader(jwtProperties.getUserTokenName());

        if (token == null || token.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"msg\":\"" + MessageConstant.TOKEN_NOT_FOUND + "\"}");
            return false;
        }

        // 2. 验证 access token 签名及有效期，并提取 userId
        try {
            log.debug("用户端 jwt 校验, token length: {}", token.length());
            Claims claims = jwtUtil.parseUserAccessToken(token);
            Long userId = jwtUtil.getUserIdFromClaims(claims);
            log.info("当前用户 id: {}", userId);
            if (userId == null) {
                log.error("Token 中未包含 userId，请检查生成逻辑！");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":401,\"msg\":\"" + MessageConstant.TOKEN_INVALID + "\"}");
                return false;
            }

            // 3. 存入 ThreadLocal，供后续请求处理使用
            BaseContext.setCurrentId(userId);
            return true;
        } catch (Exception e) {
            log.error("用户端 token 校验失败: {}", e.getMessage());
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