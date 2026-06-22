package com.ikeu.server.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ikeu.common.constant.JwtClaimsConstant;
import com.ikeu.common.constant.MessageConstant;
import com.ikeu.common.constant.RedisConstant;
import com.ikeu.common.constant.StatusConstant;
import com.ikeu.common.context.BaseContext;
import com.ikeu.common.exception.BusinessException;
import com.ikeu.common.exception.UnauthorizedException;
import com.ikeu.common.properties.JwtProperties;
import com.ikeu.common.utils.JwtUtil;
import com.ikeu.model.dto.AdminLoginDTO;
import com.ikeu.model.entity.Admin;
import com.ikeu.model.vo.AdminLoginVO;
import com.ikeu.server.mapper.AdminMapper;
import com.ikeu.server.service.AdminAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 管理端认证服务实现，处理管理员登录、令牌刷新和退出。
 * @author ikeu
 * @since 2026/06/11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAuthServiceImpl extends ServiceImpl<AdminMapper, Admin> implements AdminAuthService {

    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;
    private final StringRedisTemplate redisTemplate;

    /**
     * 管理员登录，校验失败计数（防暴力破解）→ 校验密码 → 校验状态，
     * 通过后生成双令牌（access+refresh），refresh token 写入 Redis 支持轮换。
     *
     * <p>失败计数使用 Redis INCR + EXPIRE 实现，5 次失败后锁定 300 秒；
     * 校验成功清除失败计数。生成 JWT 双令牌后，refresh token 的 jti 存入 Redis
     * 以实现令牌轮换和即时失效。
     *
     * @param dto 登录参数，包含用户名和密码
     * @return 登录成功后的认证 VO，包含管理员信息、双令牌和过期时间
     * @throws BusinessException 账号锁定、密码错误或账号已禁用时抛出
     */
    @Override
    @Transactional
    public AdminLoginVO login(AdminLoginDTO dto) {
        String failKey = RedisConstant.ADMIN_LOGIN_FAIL_PREFIX + dto.getUsername();
        String failCount = redisTemplate.opsForValue().get(failKey);
        if (failCount != null && Integer.parseInt(failCount) >= RedisConstant.LOGIN_MAX_FAIL_COUNT) {
            throw new BusinessException(MessageConstant.LOGIN_FAIL_LOCKED);
        }

        Admin admin = lambdaQuery().eq(Admin::getUsername, dto.getUsername()).one();
        if (admin == null || !passwordEncoder.matches(dto.getPassword(), admin.getPassword())) {
            redisTemplate.opsForValue().increment(failKey);
            redisTemplate.expire(failKey, RedisConstant.LOGIN_LOCK_SECONDS, TimeUnit.SECONDS);
            throw new BusinessException(MessageConstant.ADMIN_LOGIN_FAILED);
        }

        redisTemplate.delete(failKey);
        if (Objects.equals(admin.getStatus(), StatusConstant.DISABLE)) {
            throw new BusinessException(MessageConstant.ADMIN_DISABLED);
        }
        admin.setLastLoginTime(LocalDateTime.now());
        updateById(admin);

        var claims = new HashMap<String, Object>();
        claims.put(JwtClaimsConstant.ADMIN_ID, admin.getId());
        claims.put("role", admin.getRole());
        String accessToken = jwtUtil.generateAdminAccessToken(claims);
        String refreshToken = jwtUtil.generateAdminRefreshToken(claims);

        var refreshClaims = jwtUtil.parseAdminRefreshToken(refreshToken);
        String jti = refreshClaims.get(JwtClaimsConstant.JTI, String.class);
        long ttlSeconds = jwtProperties.getAdminRefreshTtl() / 1000;
        redisTemplate.opsForValue().set(
                RedisConstant.ADMIN_REFRESH_TOKEN_PREFIX + admin.getId() + ":" + jti,
                refreshToken,
                ttlSeconds,
                TimeUnit.SECONDS);

        return AdminLoginVO.builder()
                .adminId(admin.getId())
                .username(admin.getUsername())
                .name(admin.getName())
                .role(admin.getRole())
                .token(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtProperties.getAdminAccessTtl() / 1000)
                .build();
    }

    /**
     * 获取当前登录管理员信息（不含令牌），管理员不存在或已禁用则抛异常。
     *
     * <p>从 {@link BaseContext} 获取当前请求的管理员 ID，
     * 查询数据库后返回基本信息（不含密码和令牌），
     * 用于管理端前端展示当前登录用户信息。
     *
     * @return 管理员信息 VO，包含 ID、用户名、姓名、角色
     * @throws UnauthorizedException 管理员不存在或已被禁用时抛出
     */
    @Override
    public AdminLoginVO getAdminInfo() {
        Long adminId = BaseContext.getCurrentId();
        Admin admin = getById(adminId);
        if (admin == null || Objects.equals(admin.getStatus(), StatusConstant.DISABLE)) {
            throw new UnauthorizedException(MessageConstant.ACCOUNT_DISABLED_OR_NOT_EXIST);
        }
        return AdminLoginVO.builder()
                .adminId(admin.getId())
                .username(admin.getUsername())
                .name(admin.getName())
                .role(admin.getRole())
                .build();
    }

    /**
     * 令牌轮换：解析 refresh token → 校验 type/jti → Redis 反查 →
     * 删除旧 token → 颁发新 token 对，旧 refresh token 立即失效。
     *
     * <p>支持 refresh token 一次性使用：校验通过后立即删除 Redis 中的旧记录，
     * 再生成新的 access + refresh 令牌对。可有效防止 refresh token 泄露后的重放攻击。
     *
     * @param refreshToken 旧的刷新令牌字符串
     * @return 新生成的认证 VO，含新的双令牌和过期时间
     * @throws UnauthorizedException refresh token 无效、已过期或账号被禁用时抛出
     */
    @Override
    public AdminLoginVO refreshAccessToken(String refreshToken) {
        var claims = jwtUtil.parseAdminRefreshToken(refreshToken);
        String type = claims.get(JwtClaimsConstant.TOKEN_TYPE, String.class);
        if (!JwtClaimsConstant.TOKEN_TYPE_REFRESH.equals(type)) {
            throw new UnauthorizedException(MessageConstant.REFRESH_TOKEN_INVALID);
        }
        Long adminId = jwtUtil.getAdminIdFromClaims(claims);
        if (adminId == null) {
            throw new UnauthorizedException(MessageConstant.REFRESH_TOKEN_INVALID);
        }
        String jti = claims.get(JwtClaimsConstant.JTI, String.class);

        String redisKey = RedisConstant.ADMIN_REFRESH_TOKEN_PREFIX + adminId + ":" + jti;
        String storedToken = redisTemplate.opsForValue().get(redisKey);
        if (storedToken == null) {
            throw new UnauthorizedException(MessageConstant.REFRESH_TOKEN_EXPIRED);
        }

        redisTemplate.delete(redisKey);

        Admin admin = getById(adminId);
        if (admin == null || Objects.equals(admin.getStatus(), StatusConstant.DISABLE)) {
            throw new UnauthorizedException(MessageConstant.ACCOUNT_DISABLED_OR_NOT_EXIST);
        }

        var newTokenClaims = new java.util.HashMap<String, Object>();
        newTokenClaims.put(JwtClaimsConstant.ADMIN_ID, adminId);
        newTokenClaims.put("role", admin.getRole());
        String newAccessToken = jwtUtil.generateAdminAccessToken(newTokenClaims);
        String newRefreshToken = jwtUtil.generateAdminRefreshToken(newTokenClaims);

        var newClaims = jwtUtil.parseAdminRefreshToken(newRefreshToken);
        String newJti = newClaims.get(JwtClaimsConstant.JTI, String.class);
        long ttlSeconds = jwtProperties.getAdminRefreshTtl() / 1000;
        redisTemplate.opsForValue().set(
                RedisConstant.ADMIN_REFRESH_TOKEN_PREFIX + adminId + ":" + newJti,
                newRefreshToken,
                ttlSeconds,
                TimeUnit.SECONDS);

        return AdminLoginVO.builder()
                .adminId(admin.getId())
                .username(admin.getUsername())
                .name(admin.getName())
                .role(admin.getRole())
                .token(newAccessToken)
                .refreshToken(newRefreshToken)
                .expiresIn(jwtProperties.getAdminAccessTtl() / 1000)
                .build();
    }

    /**
     * 管理员退出，通过 {@code admin:refresh:token:{id}:*} 通配清除所有 token，
     * 使所有终端同时失效。
     *
     * <p>使用 Redis KEYS 通配匹配该管理员的所有 refresh token 记录并全部删除，
     * 确保同一账号在其他终端的会话也立即失效。
     *
     * @param adminId 要退出的管理员 ID；为 null 时直接返回不执行任何操作
     */
    @Override
    public void logout(Long adminId) {
        if (adminId == null) return;
        String pattern = RedisConstant.ADMIN_REFRESH_TOKEN_PREFIX + adminId + ":*";
        try (var cursor = redisTemplate.scan(org.springframework.data.redis.core.ScanOptions.scanOptions()
                .match(pattern).count(100).build())) {
            while (cursor.hasNext()) {
                redisTemplate.delete(cursor.next());
            }
        }
        log.info("管理员 {} 退出登录，已清除 refresh token", adminId);
    }
}
