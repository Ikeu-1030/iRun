package com.ikeu.server.controller.admin;

import com.ikeu.common.constant.MessageConstant;
import com.ikeu.common.constant.RedisConstant;
import com.ikeu.common.context.BaseContext;
import com.ikeu.common.exception.BusinessException;
import com.ikeu.common.result.Result;
import com.ikeu.model.dto.AdminLoginDTO;
import com.ikeu.model.vo.AdminLoginVO;
import com.ikeu.server.service.AdminAuthService;
import com.ikeu.server.util.WebUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;

/**
 * 管理端认证接口，提供管理员登录、刷新令牌和退出登录功能。
 * @author ikeu
 * @since 2025/06/01
 */
@Tag(name = "管理端-认证接口")
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AdminAuthService adminAuthService;
    private final StringRedisTemplate stringRedisTemplate;

    private static final int LOGIN_RATE_MAX = 10;   // 每 IP 每分钟最多 10 次登录尝试
    private static final int REFRESH_RATE_MAX = 10; // 每 IP 每分钟最多 10 次刷新

    /**
     * 管理员账号密码登录，含 IP 级速率限制（每 IP 每分钟最多 10 次）。
     *
     * @param dto     登录 DTO（用户名 + 密码）
     * @param request HTTP 请求（用于提取客户端 IP）
     * @return 登录成功后的双令牌 + 管理员信息
     */
    @Operation(summary = "管理员登录")
    @PostMapping("/login")
    public Result<AdminLoginVO> login(@Valid @RequestBody AdminLoginDTO dto, HttpServletRequest request) {
        if (isRateLimited(request, RedisConstant.ADMIN_LOGIN_RATE_KEY, LOGIN_RATE_MAX))
            throw new BusinessException(MessageConstant.LOGIN_FAIL_LOCKED_USER);
        return Result.success(adminAuthService.login(dto));
    }

    /**
     * 刷新管理端访问令牌（令牌轮换），含 IP 级速率限制。
     *
     * @param refreshToken 旧 refresh token（从 X-Refresh-Token 请求头获取）
     * @param request      HTTP 请求（用于提取客户端 IP）
     * @return 新的双令牌 + 管理员信息
     */
    @Operation(summary = "刷新管理员访问令牌")
    @PostMapping("/refresh")
    public Result<AdminLoginVO> refresh(@RequestHeader("X-Refresh-Token") String refreshToken, HttpServletRequest request) {
        if (isRateLimited(request, RedisConstant.ADMIN_REFRESH_RATE_KEY, REFRESH_RATE_MAX))
            throw new BusinessException(MessageConstant.SYSTEM_BUSY);
        return Result.success(adminAuthService.refreshAccessToken(refreshToken));
    }

    private boolean isRateLimited(HttpServletRequest request, String keyPrefix, int max) {
        String ip = WebUtil.getClientIp(request);
        String key = keyPrefix + ip;
        Long n = stringRedisTemplate.opsForValue().increment(key);
        stringRedisTemplate.expire(key, 60, TimeUnit.SECONDS);
        return n != null && n > max;
    }

    /**
     * 从 ThreadLocal 获取当前管理员 ID，返回其基本信息（不含密码）。
     *
     * @return 当前管理员基本信息
     */
    @Operation(summary = "获取当前管理员信息")
    @GetMapping("/info")
    public Result<AdminLoginVO> info() {
        return Result.success(adminAuthService.getAdminInfo());
    }

    /**
     * 管理员退出登录，通过模式匹配删除 Redis 中该管理员所有 refresh token。
     *
     * @return 操作结果
     */
    @Operation(summary = "管理员退出登录")
    @PostMapping("/logout")
    public Result<Void> logout() {
        adminAuthService.logout(BaseContext.getCurrentId());
        return Result.success(MessageConstant.LOGOUT_SUCCESS);
    }
}
