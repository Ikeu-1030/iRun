package com.ikeu.server.service;

import com.ikeu.model.dto.AdminLoginDTO;
import com.ikeu.model.vo.AdminLoginVO;

/**
 * 管理端认证服务接口，提供登录、令牌刷新、信息查询和退出功能。
 * @author ikeu
 * @since 2026/06/11
 */
public interface AdminAuthService {

    /**
     * 管理员登录，校验用户名密码后生成双令牌（access + refresh），
     * 附带登录失败计数防暴力破解，refresh token 写入 Redis 支持轮换。
     *
     * @param dto 登录请求，包含用户名和密码
     * @return 登录成功后的管理员信息及双令牌
     * @throws BusinessException 登录失败次数过多锁定、密码错误或账号已禁用时抛出
     */
    AdminLoginVO login(AdminLoginDTO dto);

    /**
     * 刷新管理员访问令牌（令牌轮换），旧 refresh token 立即失效，
     * Redis 反查校验后颁发新令牌对。
     *
     * @param refreshToken 旧 refresh token
     * @return 新的管理员信息及双令牌
     * @throws UnauthorizedException refresh token 无效、已过期或管理员被禁用时抛出
     */
    AdminLoginVO refreshAccessToken(String refreshToken);

    /**
     * 获取当前登录管理员信息（不含令牌），内部从请求上下文提取 adminId。
     *
     * @return 当前管理员基本信息
     * @throws UnauthorizedException 管理员不存在或已被禁用时抛出
     */
    AdminLoginVO getAdminInfo();

    /**
     * 管理员退出登录，通配清除 Redis 中该管理员的所有 refresh token，
     * 使所有终端同时失效。
     *
     * @param adminId 管理员 ID；为 null 时直接返回不执行任何操作
     */
    void logout(Long adminId);
}
