package com.ikeu.server.service;

import com.ikeu.common.exception.BusinessException;
import com.ikeu.common.exception.NotFoundException;
import com.ikeu.common.exception.ParamErrorException;
import com.ikeu.common.exception.UnauthorizedException;
import com.ikeu.model.dto.*;
import com.ikeu.model.vo.CertifyStatusVO;
import com.ikeu.model.vo.UserInfoVO;
import com.ikeu.model.vo.UserLoginVO;

/**
 * 用户服务接口，提供注册登录、信息管理、实名认证、支付密码和账户操作等功能。
 *
 * @author ikeu
 * @since 2026/05/14
 */
public interface UserService {

    /**
     * 发送短信验证码，存入 Redis 并设置 5 分钟有效期。
     *
     * @param sendCodeDTO 发送验证码 DTO（包含手机号和操作类型）
     * @throws BusinessException 操作类型不合法或短信发送失败时抛出
     */
    void sendCode(SendCodeDTO sendCodeDTO);

    /**
     * 用户注册，校验验证码和唯一性后创建用户并生成双令牌。
     *
     * @param userRegisterDTO 用户注册 DTO（包含用户名、密码、手机号、验证码等）
     * @return 登录 VO，包含用户信息和双令牌
     * @throws BusinessException 验证码错误、用户名或密码为空、用户名或手机号已存在时抛出
     */
    UserLoginVO register(UserRegisterDTO userRegisterDTO);

    /**
     * 用户登录，支持账号密码和手机验证码两种方式，返回双令牌。
     *
     * <p>账号密码登录含登录失败计数防暴力破解；微信注册用户仅允许验证码或微信登录。
     *
     * @param loginDTO 用户登录 DTO（包含登录类型、用户名/手机号、密码/验证码）
     * @return 登录 VO，包含用户信息和双令牌
     * @throws UnauthorizedException 账号不存在、密码错误、验证码错误、登录类型无效或账号被禁用时抛出
     * @throws BusinessException 登录失败次数过多导致锁定时抛出
     * @throws NotFoundException 验证码登录时用户不存在抛出
     */
    UserLoginVO login(UserLoginDTO loginDTO);

    /**
     * 根据用户 ID 查询用户基本信息及配送员认证状态。
     *
     * @param userId 用户 ID
     * @return 用户信息 VO（含注册信息、余额、实名认证状态、配送员认证状态等）
     * @throws UnauthorizedException 用户未登录（userId 为空）时抛出
     * @throws NotFoundException 用户不存在时抛出
     */
    UserInfoVO getUserInfo(Long userId);

    /**
     * 刷新访问令牌（令牌轮换），删除旧 refresh token 并生成新令牌对。
     *
     * @param refreshToken 旧 refresh token
     * @return 登录 VO，包含新生成的双令牌
     * @throws UnauthorizedException refresh token 无效、已过期或账号被禁用时抛出
     * @throws NotFoundException 用户不存在时抛出
     */
    UserLoginVO refreshAccessToken(String refreshToken);

    /**
     * 退出登录，清除 Redis 中该用户所有 refresh token。
     *
     * @param userId 用户 ID
     * @throws UnauthorizedException 用户未登录（userId 为空）时抛出
     */
    void logout(Long userId);

    /**
     * 注销账户，删除关联数据（地址簿、跑腿员档案、评价、流水）后物理删除用户。
     *
     * @param userId 用户 ID
     * @throws UnauthorizedException 用户未登录（userId 为空）时抛出
     * @throws NotFoundException 用户不存在时抛出
     */
    void deleteAccount(Long userId);

    /**
     * 修改手机号，校验验证码和新手机号唯一性。
     *
     * @param userId 用户 ID
     * @param changePhoneDTO 修改手机号 DTO（包含新手机号和验证码）
     * @throws UnauthorizedException 用户未登录或验证码错误时抛出
     * @throws ParamErrorException 新手机号已被其他用户绑定时抛出
     */
    void changePhone(Long userId, ChangePhoneDTO changePhoneDTO);

    /**
     * 修改登录密码，校验原密码后加密存储新密码。
     *
     * @param userId 用户 ID
     * @param changePasswordDTO 修改密码 DTO（包含原密码和新密码）
     * @throws UnauthorizedException 用户未登录时抛出
     * @throws ParamErrorException 原密码不正确时抛出
     */
    void changePassword(Long userId, ChangePasswordDTO changePasswordDTO);

    /**
     * 提交实名认证（学生身份），保存认证信息并将状态设为审核中。
     *
     * <p>已认证通过或审核中不可重复提交，驳回后可重新提交。
     *
     * @param userId 用户 ID
     * @param realNameAuthDTO 实名认证 DTO（包含真实姓名、学号、学生证照片 URL）
     * @throws UnauthorizedException 用户未登录时抛出
     * @throws BusinessException 已认证通过或审核中时抛出
     */
    void submitRealNameAuth(Long userId, RealNameAuthDTO realNameAuthDTO);

    /**
     * 修改个人基本信息（昵称、头像、校区、性别、签名等），非空字段才更新。
     *
     * @param userId 用户 ID
     * @param updateProfileDTO 修改个人资料 DTO
     * @throws UnauthorizedException 用户未登录时抛出
     * @throws NotFoundException 用户不存在时抛出
     */
    void updateProfile(Long userId, UpdateProfileDTO updateProfileDTO);

    /**
     * 微信小程序登录，首次自动注册，返回双令牌。
     *
     * @param dto 微信登录 DTO（包含微信 code）
     * @return 登录 VO，包含用户信息和双令牌
     * @throws UnauthorizedException 用户被禁用时抛出
     */
    UserLoginVO weChatLogin(WeChatLoginDTO dto);

    /**
     * 首次设置支付密码，无需身份校验。
     *
     * @param userId 用户 ID
     * @param dto 设置支付密码 DTO（包含新支付密码）
     * @throws UnauthorizedException 用户未登录时抛出
     * @throws NotFoundException 用户不存在时抛出
     * @throws BusinessException 支付密码已设置时抛出
     */
    void setPayPassword(Long userId, SetPayPasswordDTO dto);

    /**
     * 修改支付密码，需校验原支付密码。
     *
     * @param userId 用户 ID
     * @param dto 修改支付密码 DTO（包含原密码和新密码）
     * @throws UnauthorizedException 用户未登录时抛出
     * @throws NotFoundException 用户不存在时抛出
     * @throws BusinessException 支付密码未设置或原密码不正确时抛出
     */
    void changePayPassword(Long userId, ChangePayPasswordDTO dto);

    /**
     * 查询用户是否已设置支付密码。
     *
     * @param userId 用户 ID
     * @return true 已设置支付密码，false 未设置
     * @throws UnauthorizedException 用户未登录时抛出
     * @throws NotFoundException 用户不存在时抛出
     */
    boolean hasPayPassword(Long userId);

    /**
     * 重置登录密码（忘记密码），通过短信验证码验证身份。
     *
     * <p>含失败计数锁定机制，5 次失败后锁定 300 秒。
     *
     * @param userId 用户 ID
     * @param dto 重置密码 DTO（包含手机号、验证码、新密码）
     * @throws UnauthorizedException 用户未登录时抛出
     * @throws NotFoundException 用户不存在时抛出
     * @throws BusinessException 手机号不匹配、验证码错误或失败次数过多锁定时抛出
     */
    void resetPassword(Long userId, ResetPasswordDTO dto);

    /**
     * 重置支付密码（忘记支付密码），通过短信验证码验证身份。
     *
     * <p>含失败计数锁定机制，5 次失败后锁定 300 秒。
     *
     * @param userId 用户 ID
     * @param dto 重置密码 DTO（包含手机号、验证码、新密码）
     * @throws UnauthorizedException 用户未登录时抛出
     * @throws NotFoundException 用户不存在时抛出
     * @throws BusinessException 手机号不匹配、验证码错误或失败次数过多锁定时抛出
     */
    void resetPayPassword(Long userId, ResetPasswordDTO dto);

    /**
     * 查询用户实名认证状态和配送员认证状态。
     *
     * @param userId 用户 ID
     * @return 认证状态 VO（包含实名认证状态、配送员认证状态、真实姓名、学号等）
     * @throws NotFoundException 用户不存在时抛出
     */
    CertifyStatusVO getCertifyStatus(Long userId);
}
