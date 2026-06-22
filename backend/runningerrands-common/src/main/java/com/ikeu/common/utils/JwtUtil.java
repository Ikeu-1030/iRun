package com.ikeu.common.utils;

import com.ikeu.common.constant.JwtClaimsConstant;
import com.ikeu.common.properties.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * JWT 工具类，支持 access token + refresh token 双令牌机制。
 *
 * <h3>令牌设计</h3>
 * <ul>
 *   <li><b>Access Token</b>：短有效期（默认 2h），用于日常 HTTP 请求鉴权，由拦截器校验。</li>
 *   <li><b>Refresh Token</b>：长有效期（默认 7d），包含 {@code jti} 唯一标识，存储在 Redis 中，
 *       用于在 access token 过期后换取新的令牌对。</li>
 * </ul>
 *
 * <h3>令牌轮换（Refresh Token Rotation）</h3>
 * 每次 refresh 操作会删除旧的 refresh token 并颁发新的一对 token，
 * 防止 refresh token 被窃取后长期滥用。如果使用已被轮换掉的旧 token 刷新，会因 Redis 中无记录而失败。
 *
 * <h3>管理端与用户端隔离</h3>
 * 管理端和用户端使用完全独立的签名密钥和 TTL 配置，防止越权。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtUtil {

    private final JwtProperties jwtProperties;

    private static final int MIN_KEY_BYTES = 32; // HMAC-SHA256 requires >= 256 bits

    /** 已知弱密钥前缀（Docker .env.example 中的占位密钥），生产环境禁止使用 */
    private static final String[] WEAK_KEY_PREFIXES = {
        "docker-admin-secret-key", "docker-user-secret-key",
        "change-me", "changeme", "your-secret", "your_admin_secret", "your_user_secret"
    };

    /** 启动时校验 JWT 密钥已配置且满足最小长度，并拒绝已知弱密钥，防止空密钥导致运行时 WeakKeyException。 */
    @PostConstruct
    public void validateKeys() {
        String adminKey = jwtProperties.getAdminSecretKey();
        if (adminKey == null || adminKey.isBlank()) {
            throw new IllegalStateException(
                    "JWT admin secret key is empty. Set RUNNING_ERRANDS_JWT_ADMIN_SECRET environment variable.");
        }
        if (adminKey.getBytes(StandardCharsets.UTF_8).length < MIN_KEY_BYTES) {
            throw new IllegalStateException(
                    "JWT admin secret key is too short (min " + MIN_KEY_BYTES + " bytes for HS256).");
        }
        if (isWeakKey(adminKey)) {
            throw new IllegalStateException(
                    "JWT admin secret key is a known weak/default value. Generate a strong random key for production.");
        }
        String userKey = jwtProperties.getUserSecretKey();
        if (userKey == null || userKey.isBlank()) {
            throw new IllegalStateException(
                    "JWT user secret key is empty. Set RUNNING_ERRANDS_JWT_USER_SECRET environment variable.");
        }
        if (userKey.getBytes(StandardCharsets.UTF_8).length < MIN_KEY_BYTES) {
            throw new IllegalStateException(
                    "JWT user secret key is too short (min " + MIN_KEY_BYTES + " bytes for HS256).");
        }
        if (isWeakKey(userKey)) {
            throw new IllegalStateException(
                    "JWT user secret key is a known weak/default value. Generate a strong random key for production.");
        }
        log.info("JWT secret keys validated successfully");
    }

    private boolean isWeakKey(String key) {
        String lower = key.toLowerCase();
        for (String prefix : WEAK_KEY_PREFIXES) {
            if (lower.startsWith(prefix)) return true;
        }
        return false;
    }

    // ========== 密钥获取 ==========

    /** 获取管理端 HMAC-SHA 签名密钥 */
    private SecretKey getAdminSecretKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getAdminSecretKey().getBytes(StandardCharsets.UTF_8));
    }

    /** 获取用户端 HMAC-SHA 签名密钥 */
    private SecretKey getUserSecretKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getUserSecretKey().getBytes(StandardCharsets.UTF_8));
    }

    // ========== 管理端 Access Token ==========

    /**
     * 生成管理端 access token，自动注入 {@code type=access} 声明。
     *
     * @param claims 自定义声明（需包含 {@code adminId}）
     * @return 签名的 JWT access token 字符串
     */
    public String generateAdminAccessToken(Map<String, Object> claims) {
        Map<String, Object> mutable = new HashMap<>(claims);
        mutable.put(JwtClaimsConstant.TOKEN_TYPE, JwtClaimsConstant.TOKEN_TYPE_ACCESS);
        return Jwts.builder()
                .claims(mutable)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtProperties.getAdminAccessTtl()))
                .signWith(getAdminSecretKey())
                .compact();
    }

    /**
     * 解析并验证管理端 access token。
     *
     * @param token JWT token 字符串
     * @return JWT Claims
     * @throws io.jsonwebtoken.JwtException 签名无效或已过期
     */
    public Claims parseAdminAccessToken(String token) {
        return Jwts.parser()
                .verifyWith(getAdminSecretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 快速判断管理端 token 是否有效（不抛出异常）。
     *
     * @param token JWT token 字符串
     * @return true 有效，false 无效或已过期
     */
    public boolean validateAdminToken(String token) {
        try {
            parseAdminAccessToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ========== 用户端 Access Token ==========

    /**
     * 生成用户端 access token，自动注入 {@code type=access} 声明。
     *
     * @param claims 自定义声明（需包含 {@code userId}）
     * @return 签名的 JWT access token 字符串
     */
    public String generateUserAccessToken(Map<String, Object> claims) {
        Map<String, Object> mutable = new HashMap<>(claims);
        mutable.put(JwtClaimsConstant.TOKEN_TYPE, JwtClaimsConstant.TOKEN_TYPE_ACCESS);
        return Jwts.builder()
                .claims(mutable)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtProperties.getUserAccessTtl()))
                .signWith(getUserSecretKey())
                .compact();
    }

    /**
     * 解析并验证用户端 access token。
     *
     * @param token JWT token 字符串
     * @return JWT Claims
     * @throws io.jsonwebtoken.JwtException 签名无效或已过期
     */
    public Claims parseUserAccessToken(String token) {
        return Jwts.parser()
                .verifyWith(getUserSecretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 快速判断用户端 token 是否有效（不抛出异常）。
     *
     * @param token JWT token 字符串
     * @return true 有效，false 无效或已过期
     */
    public boolean validateUserToken(String token) {
        try {
            parseUserAccessToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ========== 管理端 Refresh Token ==========

    /**
     * 生成管理端 refresh token，自动注入 {@code type=refresh} 和 {@code jti} 声明。
     * <p>
     * jti 用于在 Redis 中唯一标识该 refresh token，支持令牌轮换时精确删除。
     *
     * @param claims 自定义声明（需包含 {@code adminId}）
     * @return 签名的 JWT refresh token 字符串
     */
    public String generateAdminRefreshToken(Map<String, Object> claims) {
        Map<String, Object> mutable = new HashMap<>(claims);
        mutable.put(JwtClaimsConstant.TOKEN_TYPE, JwtClaimsConstant.TOKEN_TYPE_REFRESH);
        mutable.put(JwtClaimsConstant.JTI, UUID.randomUUID().toString());
        return Jwts.builder()
                .claims(mutable)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtProperties.getAdminRefreshTtl()))
                .signWith(getAdminSecretKey())
                .compact();
    }

    /**
     * 解析并验证管理端 refresh token。
     *
     * @param token JWT refresh token 字符串
     * @return JWT Claims
     * @throws io.jsonwebtoken.JwtException 签名无效或已过期
     */
    public Claims parseAdminRefreshToken(String token) {
        return Jwts.parser()
                .verifyWith(getAdminSecretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // ========== 用户端 Refresh Token ==========

    /**
     * 生成用户端 refresh token，自动注入 {@code type=refresh} 和 {@code jti} 声明。
     * <p>
     * jti 用于在 Redis 中唯一标识该 refresh token，支持令牌轮换时精确删除。
     *
     * @param claims 自定义声明（需包含 {@code userId}）
     * @return 签名的 JWT refresh token 字符串
     */
    public String generateUserRefreshToken(Map<String, Object> claims) {
        Map<String, Object> mutable = new HashMap<>(claims);
        mutable.put(JwtClaimsConstant.TOKEN_TYPE, JwtClaimsConstant.TOKEN_TYPE_REFRESH);
        mutable.put(JwtClaimsConstant.JTI, UUID.randomUUID().toString());
        return Jwts.builder()
                .claims(mutable)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtProperties.getUserRefreshTtl()))
                .signWith(getUserSecretKey())
                .compact();
    }

    /**
     * 解析并验证用户端 refresh token。
     *
     * @param token JWT refresh token 字符串
     * @return JWT Claims
     * @throws io.jsonwebtoken.JwtException 签名无效或已过期
     */
    public Claims parseUserRefreshToken(String token) {
        return Jwts.parser()
                .verifyWith(getUserSecretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // ========== 通用提取方法 ==========

    /**
     * 从 Claims 中提取 userId。
     *
     * @param claims JWT Claims
     * @return userId，可能为 null
     */
    public Long getUserIdFromClaims(Claims claims) {
        return claims.get("userId", Long.class);
    }

    /**
     * 从 Claims 中提取 adminId。
     *
     * @param claims JWT Claims
     * @return adminId，可能为 null
     */
    public Long getAdminIdFromClaims(Claims claims) {
        return claims.get("adminId", Long.class);
    }
}