package com.ikeu.server.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ikeu.common.constant.JwtClaimsConstant;
import com.ikeu.common.constant.MessageConstant;
import com.ikeu.common.constant.RedisConstant;
import com.ikeu.common.constant.StatusConstant;
import com.ikeu.common.exception.NotFoundException;
import com.ikeu.common.exception.ParamErrorException;
import com.ikeu.common.exception.UnauthorizedException;
import com.ikeu.common.properties.JwtProperties;
import com.ikeu.common.utils.JwtUtil;
import com.ikeu.common.exception.BusinessException;
import com.ikeu.common.utils.SmsUtil;
import com.ikeu.common.utils.WeChatAuthUtil;
import com.ikeu.model.dto.*;
import com.ikeu.model.entity.*;
import com.ikeu.model.vo.CertifyStatusVO;
import com.ikeu.model.vo.UserInfoVO;
import com.ikeu.model.vo.UserLoginVO;
import com.ikeu.server.mapper.*;
import com.ikeu.server.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.time.LocalDateTime;

/**
 * 用户服务实现，处理用户注册、登录、信息管理、实名认证、支付密码等用户相关功能
 * @author ikeu
 * @since 2026/05/14
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final UserMapper userMapper;
    private final RunnerProfileMapper runnerProfileMapper;
    private final TransactionRecordMapper transactionRecordMapper;
    private final ReviewMapper reviewMapper;
    private final UserAddressMapper userAddressMapper;
    private final StringRedisTemplate redisTemplate;
    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;
    private final SmsUtil smsUtil;
    private final WeChatAuthUtil weChatAuthUtil;
    private final PasswordEncoder passwordEncoder;

    /** 默认头像路径（Spring Boot 静态资源，context-path=/api，对应 static/imgs/default_avatar.jpg） */
    private static final String DEFAULT_AVATAR_URL = "/api/imgs/default_avatar.jpg";
    /** 合法的短信验证码操作类型 */
    private static final Set<String> VALID_CODE_OPERATIONS = Set.of(
            "login", "register", "change_phone", "reset_password", "reset_pay_password");

    /**
     * 生成双 Token（access + refresh）并构建登录 VO。
     *
     * <p>refresh token 携带 jti 存储在 Redis 中，用于后续的令牌轮换验证。
     */
    private UserLoginVO buildLoginVO(User user) {
        Map<String, Object> claims = Map.of(JwtClaimsConstant.USER_ID, user.getId());
        String accessToken = jwtUtil.generateUserAccessToken(claims);
        String refreshToken = jwtUtil.generateUserRefreshToken(Map.of(JwtClaimsConstant.USER_ID, user.getId()));

        // 存储 refresh token 到 Redis
        var refreshClaims = jwtUtil.parseUserRefreshToken(refreshToken);
        String jti = refreshClaims.get(JwtClaimsConstant.JTI, String.class);
        long ttlSeconds = jwtProperties.getUserRefreshTtl() / 1000;
        redisTemplate.opsForValue().set(
                RedisConstant.USER_REFRESH_TOKEN_PREFIX + user.getId() + ":" + jti,
                refreshToken,
                ttlSeconds,
                java.util.concurrent.TimeUnit.SECONDS);

        return UserLoginVO.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatarUrl(user.getAvatarUrl())
                .token(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtProperties.getUserAccessTtl() / 1000)
                .build();
    }

    /**
     * 发送验证码方法
     *  逻辑：生成6位数字验证码，调用短信工具发送验证码，将验证码存入Redis并设置5分钟有效期
     *
     * @param sendCodeDTO 发送验证码DTO（包含手机号）
     */
    @Override
    public void sendCode(SendCodeDTO sendCodeDTO) {
        String phone = sendCodeDTO.getPhone();
        String operation = sendCodeDTO.getOperation();
        if (!VALID_CODE_OPERATIONS.contains(operation)) {
            throw new BusinessException(MessageConstant.PARAM_ERROR);
        }
        // 生成6位数字验证码
        String code = RandomUtil.randomNumbers(6);
        try {
            smsUtil.sendVerifyCode(phone, code);
            // 将验证码存入 Redis，key 包含操作类型防止跨操作复用，有效期5分钟
            redisTemplate.opsForValue().set(
                    RedisConstant.USER_CERTIFY_CODE + operation + ":" + phone,
                    code,
                    RedisConstant.CODE_EXPIRE,
                    TimeUnit.SECONDS
            );
        } catch (Exception e) {
            log.error("短信发送异常", e);
            throw new BusinessException(MessageConstant.CODE_SEND_FAILED);
        }
        log.info("向手机号 {} 发送了验证码，操作类型：{}", phone, operation);
    }

    /**
     * 用户注册方法
     *  校验：验证码正确，用户名和密码非空，用户名和手机号唯一
     *  逻辑：存储用户信息到数据库，删除已使用的验证码，生成JWT Token返回登录VO
     *
     * @param userRegisterDTO 用户注册DTO
     * @return UserLoginVO 登录VO（包含用户信息和Token）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserLoginVO register(UserRegisterDTO userRegisterDTO) {

        // 校验验证码
        String cachedCode = redisTemplate.opsForValue().get(RedisConstant.USER_CERTIFY_CODE + "register:" + userRegisterDTO.getPhone());
        if (cachedCode == null || !cachedCode.equals(userRegisterDTO.getCode())) {
            throw new BusinessException(MessageConstant.CODE_ERROR);
        }

        // 校验用户名和密码不能为空
        if (userRegisterDTO.getUsername() == null || userRegisterDTO.getUsername().trim().isEmpty()) {
            throw new BusinessException(MessageConstant.USERNAME_CANNOT_BE_NULL);
        }
        if (userRegisterDTO.getPassword() == null || userRegisterDTO.getPassword().trim().isEmpty()) {
            throw new BusinessException(MessageConstant.PASSWORD_CANNOT_BE_NULL);
        }

        // 检查用户名是否已存在
        if (lambdaQuery().eq(User::getUsername, userRegisterDTO.getUsername()).one() != null) {
            throw new BusinessException(MessageConstant.USERNAME_ALREADY_EXISTS);
        }

        // 检查手机号是否已注册
        if (lambdaQuery().eq(User::getPhone, userRegisterDTO.getPhone()).one() != null) {
            throw new BusinessException(MessageConstant.PHONE_ALREADY_EXISTS);
        }

        // 存储用户信息
        User user = User.builder()
                .username(userRegisterDTO.getUsername())
                .password(passwordEncoder.encode(userRegisterDTO.getPassword()))
                .phone(userRegisterDTO.getPhone())
                .sex(userRegisterDTO.getSex())
                .nickname("用户" + userRegisterDTO.getPhone().substring(7))
                .avatarUrl(DEFAULT_AVATAR_URL)
                .balance(BigDecimal.ZERO)
                .registerType(1) // 手机号注册
                .status(StatusConstant.ENABLE)
                .isCertify(StatusConstant.NO)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        save(user);
        log.info("新用户注册成功，手机号：{}，用户ID：{}", user.getPhone(), user.getId());

        // 删除验证码
        redisTemplate.delete(RedisConstant.USER_CERTIFY_CODE + "register:" + userRegisterDTO.getPhone());

        // 生成双Token并返回VO
        return buildLoginVO(user);
    }

    /**
     * 用户登录方法
     *  支持两种登录方式：用户名/手机号密码登录或手机号验证码登录
     *  校验：检查账号状态是否正常，更新最后登录时间，生成JWT Token返回
     *
     * @param userLoginDTO 用户登录DTO
     * @return UserLoginVO 登录VO（包含用户信息和Token）
     */
    @Override
    @Transactional
    public UserLoginVO login(UserLoginDTO userLoginDTO) {
        User user;
        if (userLoginDTO.getLoginType() == 1) {
            // 用户名/手机号密码登录
            if (userLoginDTO.getUsername() == null || userLoginDTO.getPassword() == null) {
                throw new UnauthorizedException(MessageConstant.LOGIN_FAILED);
            }
            String account = userLoginDTO.getUsername();

            // 登录失败次数检查（防暴力破解）
            String failKey = RedisConstant.USER_LOGIN_FAIL_PREFIX + account;
            String failCount = redisTemplate.opsForValue().get(failKey);
            if (failCount != null && Integer.parseInt(failCount) >= RedisConstant.LOGIN_MAX_FAIL_COUNT) {
                throw new BusinessException(MessageConstant.LOGIN_FAIL_LOCKED_USER);
            }

            user = lambdaQuery().eq(User::getUsername, account).one();
            if (user == null) {
                user = lambdaQuery().eq(User::getPhone, account).one();
            }
            if (user == null) {
                // 记录失败次数（账号不存在）
                redisTemplate.opsForValue().increment(failKey);
                redisTemplate.expire(failKey, RedisConstant.LOGIN_LOCK_SECONDS, TimeUnit.SECONDS);
                throw new UnauthorizedException(MessageConstant.INVALID_CREDENTIALS);
            }
            // 微信注册用户（registerType=2）禁止密码登录，仅允许验证码或微信OAuth登录
            if (user.getRegisterType() != null && user.getRegisterType() == 2) {
                throw new UnauthorizedException(MessageConstant.WECHAT_USER_NO_PASSWORD);
            }
            if (user.getPassword() == null) {
                throw new UnauthorizedException(MessageConstant.WECHAT_USER_NO_PASSWORD);
            }
            if (!passwordEncoder.matches(userLoginDTO.getPassword(), user.getPassword())) {
                // 记录失败次数
                redisTemplate.opsForValue().increment(failKey);
                redisTemplate.expire(failKey, RedisConstant.LOGIN_LOCK_SECONDS, TimeUnit.SECONDS);
                throw new UnauthorizedException(MessageConstant.INVALID_CREDENTIALS);
            }

            // 登录成功后清除失败计数
            redisTemplate.delete(failKey);
        } else if (userLoginDTO.getLoginType() == 2) {
            // 手机号验证码登录
            if (userLoginDTO.getPhone() == null || userLoginDTO.getCode() == null) {
                throw new UnauthorizedException(MessageConstant.LOGIN_FAILED);
            }
            String cachedCode = redisTemplate.opsForValue().get(RedisConstant.USER_CERTIFY_CODE + "login:" + userLoginDTO.getPhone());
            if (cachedCode == null || !cachedCode.equals(userLoginDTO.getCode())) {
                throw new UnauthorizedException(MessageConstant.CODE_ERROR);
            }
            user = lambdaQuery().eq(User::getPhone, userLoginDTO.getPhone()).one();
            if (user == null) {
                throw new NotFoundException(MessageConstant.USER_NOT_EXIST);
            }
            redisTemplate.delete(RedisConstant.USER_CERTIFY_CODE + "login:" + userLoginDTO.getPhone());
        } else {
            throw new UnauthorizedException(MessageConstant.INVALID_LOGIN_TYPE);
        }

        // 检查帐号状态
        if (Objects.equals(user.getStatus(), StatusConstant.DISABLE)) {
            throw new UnauthorizedException(MessageConstant.USER_DISABLED);
        }

        // 更新最后登录时间
        user.setLastLoginTime(LocalDateTime.now());
        updateById(user);
        log.info("用户登录成功，用户ID：{}", user.getId());

        // 生成双Token并返回VO
        return buildLoginVO(user);
    }

    /**
     * 微信小程序登录方法（自动注册+登录）
     *  逻辑：调用微信接口换取openid，根据openid查找已有用户，
     *  存在则更新最后登录时间，不存在则自动注册新用户，生成JWT Token返回
     *
     * @param weChatLoginDTO 微信登录DTO
     * @return UserLoginVO 登录VO（包含用户信息和Token）
     */
    @Override
    @Transactional
    public UserLoginVO weChatLogin(WeChatLoginDTO weChatLoginDTO) {
        // 调用微信接口换取 openid
        JSONObject session = weChatAuthUtil.code2Session(weChatLoginDTO.getCode());
        String openid = session.getStr("openid");

        // 根据 openid 查找已有用户
        User user = lambdaQuery().eq(User::getOpenid, openid).one();
        if (user != null) {
            if (Objects.equals(user.getStatus(), StatusConstant.DISABLE)) {
                throw new UnauthorizedException(MessageConstant.USER_DISABLED);
            }
            user.setLastLoginTime(LocalDateTime.now());
            updateById(user);
        } else {
            // 自动注册
            String nickname = "微信用户" + RandomUtil.randomString(6);
            user = User.builder()
                    .username(nickname)
                    .nickname(nickname)
                    .avatarUrl(DEFAULT_AVATAR_URL)
                    .openid(openid)
                    .unionid(session.getStr("unionid"))
                    .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                    .balance(BigDecimal.ZERO)
                    .status(StatusConstant.ENABLE)
                    .isCertify(StatusConstant.NO)
                    .registerType(2) // 微信注册
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            save(user);
            log.info("微信用户自动注册: openid={}, userId={}", openid, user.getId());
        }

        return buildLoginVO(user);
    }

    /**
     * 获取当前登录用户信息方法
     *  逻辑：根据用户ID查询用户信息并拷贝为UserInfoVO，同时查询跑腿员认证状态
     *
     * @param userId 用户ID
     * @return UserInfoVO 用户信息VO
     */
    @Override
    public UserInfoVO getUserInfo(Long userId) {
        if (userId == null) {
            throw new UnauthorizedException(MessageConstant.USER_NOT_LOGIN);
        }
        User user = getById(userId);
        if (user == null) {
            throw new NotFoundException(MessageConstant.USER_NOT_EXIST);
        }

        UserInfoVO userInfo = BeanUtil.copyProperties(user, UserInfoVO.class);

        // 查询跑腿员认证状态及信用分信息
        RunnerProfile profile = runnerProfileMapper.selectOne(
                new LambdaQueryWrapper<RunnerProfile>().eq(RunnerProfile::getUserId, userId));
        if (profile != null) {
            userInfo.setVerifyStatus(profile.getVerifyStatus());
            userInfo.setCreditScore(profile.getCreditScore());
            userInfo.setBanUntil(profile.getBanUntil());
        }

        return userInfo;
    }

    /**
     * 用户退出登录，清除所有 refresh token。
     *
     * <p>通过模式匹配 {@code user:refresh:token:{userId}:*} 删除该用户所有 token，
     * 使所有终端的 refresh token 同时失效。
     *
     * @param userId 用户ID
     */
    @Override
    public void logout(Long userId) {
        if (userId == null) {
            throw new UnauthorizedException(MessageConstant.USER_NOT_LOGIN);
        }
        String pattern = RedisConstant.USER_REFRESH_TOKEN_PREFIX + userId + ":*";
        try (var cursor = redisTemplate.scan(org.springframework.data.redis.core.ScanOptions.scanOptions()
                .match(pattern).count(100).build())) {
            while (cursor.hasNext()) {
                redisTemplate.delete(cursor.next());
            }
        }
        log.info("用户 {} 退出登录，已清除 refresh token", userId);
    }

    /**
     * 刷新用户访问令牌（令牌轮换）。
     *
     * <p>流程：
     * <ol>
     *   <li>解析 refresh token 并验证 {@code type=refresh}、{@code userId}、{@code jti}。</li>
     *   <li>Redis 反查 —— 若不存在说明已被轮换或已过期，拒绝请求。</li>
     *   <li>删除旧 token 并颁发新的 token 对（access + refresh），存储新 refresh token 到 Redis。</li>
     * </ol>
     */
    @Override
    public UserLoginVO refreshAccessToken(String refreshToken) {
        var claims = jwtUtil.parseUserRefreshToken(refreshToken);
        String type = claims.get(JwtClaimsConstant.TOKEN_TYPE, String.class);

        if (!JwtClaimsConstant.TOKEN_TYPE_REFRESH.equals(type)) {
            throw new UnauthorizedException(MessageConstant.REFRESH_TOKEN_INVALID);
        }
        Long userId = jwtUtil.getUserIdFromClaims(claims);
        if (userId == null) {
            throw new UnauthorizedException(MessageConstant.REFRESH_TOKEN_INVALID);
        }

        String jti = claims.get(JwtClaimsConstant.JTI, String.class);

        // 2. Redis 反查
        String redisKey = RedisConstant.USER_REFRESH_TOKEN_PREFIX + userId + ":" + jti;
        String storedToken = redisTemplate.opsForValue().get(redisKey);
        if (storedToken == null) {
            throw new UnauthorizedException(MessageConstant.REFRESH_TOKEN_EXPIRED);
        }

        // 3. 删除旧 refresh token（令牌轮换）
        redisTemplate.delete(redisKey);

        User user = getById(userId);
        if (user == null) {
            throw new NotFoundException(MessageConstant.USER_NOT_EXIST);
        }
        if (Objects.equals(user.getStatus(), StatusConstant.DISABLE)) {
            throw new UnauthorizedException(MessageConstant.USER_DISABLED);
        }

        return buildLoginVO(user);
    }

    /**
     * 注销账户方法
     *  逻辑：删除用户关联的地址簿、跑腿员档案、评价记录、资金流水记录，最后删除用户本身
     *
     * @param userId 用户ID
     */
    @Override
    @Transactional
    public void deleteAccount(Long userId) {
        if (userId == null) {
            throw new UnauthorizedException(MessageConstant.USER_NOT_LOGIN);
        }
        User user = getById(userId);
        if (user == null) {
            throw new NotFoundException(MessageConstant.USER_NOT_EXIST);
        }
        // 删除关联的地址簿
        userAddressMapper.delete(new LambdaQueryWrapper<UserAddress>().eq(UserAddress::getUserId, userId));
        // 删除跑腿员档案
        runnerProfileMapper.delete(new LambdaQueryWrapper<RunnerProfile>().eq(RunnerProfile::getUserId, userId));
        // 删除评价记录
        reviewMapper.delete(new LambdaQueryWrapper<Review>()
                .eq(Review::getTargetUserId, userId)
                .or().eq(Review::getReviewerId, userId));
        // 删除资金流水
        transactionRecordMapper.delete(new LambdaQueryWrapper<TransactionRecord>().eq(TransactionRecord::getUserId, userId));
        // 删除用户本身
        removeById(userId);
        log.info("用户 {} 账户已彻底删除", userId);
    }

    /**
     * 修改手机号方法
     *  校验：验证码正确，新手机号未被其他用户绑定
     *  逻辑：更新用户手机号，删除已使用的验证码
     *
     * @param userId 用户ID
     * @param changePhoneDTO 修改手机号DTO
     */
    @Override
    @Transactional
    public void changePhone(Long userId, ChangePhoneDTO changePhoneDTO) {
        if (userId == null) throw new UnauthorizedException(MessageConstant.USER_NOT_LOGIN);

        String newPhone = changePhoneDTO.getNewPhone();
        String code = changePhoneDTO.getCode();

        // 校验验证码
        String cachedCode = redisTemplate.opsForValue().get(RedisConstant.USER_CERTIFY_CODE + "change_phone:" + newPhone);
        if (cachedCode == null || !cachedCode.equals(code)) {
            throw new UnauthorizedException(MessageConstant.CODE_ERROR);
        }

        // 检查新手机号是否已被其他用户绑定
        User existingUser = lambdaQuery().eq(User::getPhone, newPhone).one();
        if (existingUser != null && !existingUser.getId().equals(userId)) {
            throw new ParamErrorException(MessageConstant.PHONE_ALREADY_BIND_TO_OTHER);
        }

        // 更新手机号
        User user = getById(userId);
        user.setPhone(newPhone);
        updateById(user);
        redisTemplate.delete(RedisConstant.USER_CERTIFY_CODE + "change_phone:" + newPhone);
        log.info("用户 {} 修改手机号为 {}", userId, newPhone);
    }

    /**
     * 修改登录密码方法
     *  校验：原密码正确
     *  逻辑：更新用户密码为新密码（加密存储）
     *
     * @param userId 用户ID
     * @param changePasswordDTO 修改密码DTO
     */
    @Override
    @Transactional
    public void changePassword(Long userId, ChangePasswordDTO changePasswordDTO) {
        if (userId == null) throw new UnauthorizedException(MessageConstant.USER_NOT_LOGIN);

        User user = getById(userId);
        if (!passwordEncoder.matches(changePasswordDTO.getOldPassword(), user.getPassword())) {
            throw new ParamErrorException(MessageConstant.OLD_PASSWORD_INCORRECT);
        }

        user.setPassword(passwordEncoder.encode(changePasswordDTO.getNewPassword()));
        updateById(user);
        log.info("用户 {} 修改登录密码成功", userId);
    }

    /**
     * 用户实名认证（学生身份认证）方法
     *  校验：已认证通过或审核中不可重复提交
     *  逻辑：将认证信息（真实姓名、学号、学生证照片）保存到用户表，更新认证状态为"审核中"
     *
     * @param userId 用户ID
     * @param realNameAuthDTO 实名认证DTO（包含真实姓名、学号、学生证照片URL）
     */
    @Override
    @Transactional
    public void submitRealNameAuth(Long userId, RealNameAuthDTO realNameAuthDTO) {
        if (userId == null) throw new UnauthorizedException(MessageConstant.USER_NOT_LOGIN);

        User user = getById(userId);
        // 已认证通过，无需重复提交
        if (Objects.equals(user.getIsCertify(), StatusConstant.CERTIFY_APPROVED)) {
            throw new BusinessException(MessageConstant.CERTIFY_SUCCESS);
        }
        // 审核中，请勿重复提交
        if (Objects.equals(user.getIsCertify(), StatusConstant.CERTIFY_AUDITING)) {
            throw new BusinessException(MessageConstant.CERTIFY_CHECKING);
        }

        // 将认证信息保存到 user 表
        user.setRealName(realNameAuthDTO.getRealName());
        user.setStudentId(realNameAuthDTO.getStudentId());
        user.setCertifyImg(realNameAuthDTO.getCertImageUrl());
        user.setIsCertify(StatusConstant.CERTIFY_AUDITING);
        user.setCertifyRemark(null);
        user.setUpdatedAt(LocalDateTime.now());
        updateById(user);

        log.info("用户 {} 提交实名认证，姓名：{}，学号：{}", userId, realNameAuthDTO.getRealName(), realNameAuthDTO.getStudentId());
    }

    /**
     * 用户修改基本信息方法
     *  逻辑：支持修改昵称、头像、校区、性别、个性签名等字段，非空字段才更新
     *
     * @param userId 用户ID
     * @param updateProfileDTO 修改个人资料DTO
     */
    @Override
    @Transactional
    public void updateProfile(Long userId, UpdateProfileDTO updateProfileDTO) {
        if (userId == null) {
            throw new UnauthorizedException(MessageConstant.USER_NOT_LOGIN);
        }
        User user = getById(userId);
        if (user == null) {
            throw new NotFoundException(MessageConstant.USER_NOT_EXIST);
        }

        if (updateProfileDTO.getNickname() != null) {
            user.setNickname(updateProfileDTO.getNickname());
        }
        if (updateProfileDTO.getAvatarUrl() != null) {
            user.setAvatarUrl(updateProfileDTO.getAvatarUrl());
        }
        if (updateProfileDTO.getCampus() != null) {
            user.setCampus(updateProfileDTO.getCampus());
        }
        if (updateProfileDTO.getSex() != null) {
            user.setSex(updateProfileDTO.getSex());
        }
        if (updateProfileDTO.getSignature() != null) {
            user.setSignature(updateProfileDTO.getSignature());
        }
        updateById(user);
        log.info("用户 {} 修改了个人基本信息", userId);
    }

    /**
     * 首次设置支付密码方法
     *  校验：用户存在，支付密码未设置
     *  逻辑：直接加密存储支付密码，无需身份校验
     *
     * @param userId 用户ID
     * @param dto 设置支付密码DTO
     */
    @Override
    @Transactional
    public void setPayPassword(Long userId, SetPayPasswordDTO dto) {
        if (userId == null) throw new UnauthorizedException(MessageConstant.USER_NOT_LOGIN);
        User user = getById(userId);
        if (user == null) throw new NotFoundException(MessageConstant.USER_NOT_EXIST);
        if (user.getPayPassword() != null) {
            throw new BusinessException(MessageConstant.PAY_PASSWORD_ALREADY_SET);
        }
        user.setPayPassword(passwordEncoder.encode(dto.getPayPassword()));
        updateById(user);
        log.info("用户 {} 设置了支付密码", userId);
    }

    /**
     * 查询用户是否已设置支付密码方法
     *
     * @param userId 用户ID
     * @return boolean 是否已设置支付密码
     */
    @Override
    public boolean hasPayPassword(Long userId) {
        if (userId == null) throw new UnauthorizedException(MessageConstant.USER_NOT_LOGIN);
        User user = getById(userId);
        if (user == null) throw new NotFoundException(MessageConstant.USER_NOT_EXIST);
        return user.getPayPassword() != null;
    }

    /**
     * 重置登录密码方法（忘记登录密码）
     *  校验：用户存在，短信验证码正确，手机号归属当前用户
     *  逻辑：通过手机号验证码验证身份后，加密存储新登录密码
     *
     * @param userId 用户ID
     * @param dto 重置密码DTO（手机号、验证码、新密码）
     */
    @Override
    @Transactional
    public void resetPassword(Long userId, ResetPasswordDTO dto) {
        if (userId == null) throw new UnauthorizedException(MessageConstant.USER_NOT_LOGIN);
        User user = getById(userId);
        if (user == null) throw new NotFoundException(MessageConstant.USER_NOT_EXIST);
        if (user.getPhone() == null || !user.getPhone().equals(dto.getPhone())) {
            throw new BusinessException(MessageConstant.PHONE_NOT_MATCH);
        }

        // 暴力破解防护：失败计数检查
        String failKey = RedisConstant.USER_RESET_PWD_FAIL_PREFIX + userId;
        String failCount = redisTemplate.opsForValue().get(failKey);
        if (failCount != null && Integer.parseInt(failCount) >= RedisConstant.LOGIN_MAX_FAIL_COUNT) {
            throw new BusinessException(MessageConstant.RESET_PWD_FAIL_LOCKED);
        }

        String cachedCode = redisTemplate.opsForValue()
                .get(RedisConstant.USER_CERTIFY_CODE + "reset_password:" + dto.getPhone());
        if (cachedCode == null || !cachedCode.equals(dto.getCode())) {
            redisTemplate.opsForValue().increment(failKey);
            redisTemplate.expire(failKey, RedisConstant.LOGIN_LOCK_SECONDS, TimeUnit.SECONDS);
            throw new BusinessException(MessageConstant.CODE_ERROR);
        }

        // 验证成功，清除失败计数和验证码
        redisTemplate.delete(failKey);
        redisTemplate.delete(RedisConstant.USER_CERTIFY_CODE + "reset_password:" + dto.getPhone());
        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        updateById(user);
        log.info("用户 {} 通过忘记密码重置了登录密码", userId);
    }

    /**
     * 重置支付密码方法（忘记支付密码）
     *  校验：用户存在，短信验证码正确，手机号归属当前用户
     *  逻辑：通过手机号验证码验证身份后，加密存储新支付密码
     *
     * @param userId 用户ID
     * @param dto 重置密码DTO（手机号、验证码、新密码）
     */
    @Override
    @Transactional
    public void resetPayPassword(Long userId, ResetPasswordDTO dto) {
        if (userId == null) throw new UnauthorizedException(MessageConstant.USER_NOT_LOGIN);
        User user = getById(userId);
        if (user == null) throw new NotFoundException(MessageConstant.USER_NOT_EXIST);
        if (user.getPhone() == null || !user.getPhone().equals(dto.getPhone())) {
            throw new BusinessException(MessageConstant.PHONE_NOT_MATCH);
        }

        // 暴力破解防护：失败计数检查
        String failKey = RedisConstant.USER_RESET_PAY_PWD_FAIL_PREFIX + userId;
        String failCount = redisTemplate.opsForValue().get(failKey);
        if (failCount != null && Integer.parseInt(failCount) >= RedisConstant.LOGIN_MAX_FAIL_COUNT) {
            throw new BusinessException(MessageConstant.RESET_PWD_FAIL_LOCKED);
        }

        String cachedCode = redisTemplate.opsForValue()
                .get(RedisConstant.USER_CERTIFY_CODE + "reset_pay_password:" + dto.getPhone());
        if (cachedCode == null || !cachedCode.equals(dto.getCode())) {
            redisTemplate.opsForValue().increment(failKey);
            redisTemplate.expire(failKey, RedisConstant.LOGIN_LOCK_SECONDS, TimeUnit.SECONDS);
            throw new BusinessException(MessageConstant.CODE_ERROR);
        }

        // 验证成功，清除失败计数和验证码
        redisTemplate.delete(failKey);
        redisTemplate.delete(RedisConstant.USER_CERTIFY_CODE + "reset_pay_password:" + dto.getPhone());
        user.setPayPassword(passwordEncoder.encode(dto.getNewPassword()));
        updateById(user);
        log.info("用户 {} 通过忘记密码重置了支付密码", userId);
    }

    /**
     * 修改支付密码方法
     *  校验：用户存在，支付密码已设置，原支付密码正确
     *  逻辑：加密存储新支付密码
     *
     * @param userId 用户ID
     * @param dto 修改支付密码DTO
     */
    @Override
    @Transactional
    public void changePayPassword(Long userId, ChangePayPasswordDTO dto) {
        if (userId == null) throw new UnauthorizedException(MessageConstant.USER_NOT_LOGIN);
        User user = getById(userId);
        if (user == null) throw new NotFoundException(MessageConstant.USER_NOT_EXIST);
        if (user.getPayPassword() == null) {
            throw new BusinessException(MessageConstant.PAY_PASSWORD_NOT_SET);
        }
        if (!passwordEncoder.matches(dto.getOldPayPassword(), user.getPayPassword())) {
            throw new BusinessException(MessageConstant.OLD_PAY_PASSWORD_INCORRECT);
        }
        user.setPayPassword(passwordEncoder.encode(dto.getNewPayPassword()));
        updateById(user);
        log.info("用户 {} 修改了支付密码", userId);
    }

    /**
     * 查询用户实名认证状态方法
     *  逻辑：返回认证状态信息，包含真实姓名、学号、认证照片、审核状态和审核备注
     *
     * @param userId 用户ID
     * @return CertifyStatusVO 认证状态VO
     */
    @Override
    public CertifyStatusVO getCertifyStatus(Long userId) {
        User user = getById(userId);
        if (user == null) throw new NotFoundException(MessageConstant.USER_NOT_EXIST);

        // 配送员认证状态从 runner_profile 读取
        RunnerProfile runnerProfile = runnerProfileMapper.selectOne(
                new LambdaQueryWrapper<RunnerProfile>().eq(RunnerProfile::getUserId, userId));
        Integer verifyStatus = runnerProfile != null ? runnerProfile.getVerifyStatus() : 0;

        return CertifyStatusVO.builder()
                .isCertify(user.getIsCertify())
                .verifyStatus(verifyStatus)
                .verifyRemark(user.getCertifyRemark())
                .realName(user.getRealName())
                .studentId(user.getStudentId())
                .certifyImg(user.getCertifyImg())
                .build();
    }

}