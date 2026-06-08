package com.ikeu.server.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ikeu.common.constant.JwtClaimsConstant;
import com.ikeu.common.constant.MessageConstant;
import com.ikeu.common.constant.RedisConstant;
import com.ikeu.common.constant.StatusConstant;
import com.ikeu.common.exception.BusinessException;
import com.ikeu.common.exception.NotFoundException;
import com.ikeu.common.enums.OrderStateMachine;
import com.ikeu.common.exception.UnauthorizedException;
import com.ikeu.common.properties.JwtProperties;
import com.ikeu.common.result.PageResult;
import com.ikeu.common.utils.JwtUtil;
import com.ikeu.model.dto.AdminLoginDTO;
import com.ikeu.model.entity.*;
import com.ikeu.model.vo.*;
import com.ikeu.server.mapper.*;
import com.ikeu.server.service.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.concurrent.TimeUnit;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 管理员服务实现类，处理管理员登录、用户管理、任务管理、订单管理、跑腿员审核及仪表盘数据等功能
 * @author ikeu
 * @since 2026/05/21
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminServiceImpl extends ServiceImpl<AdminMapper, Admin> implements AdminService {

    private final UserMapper userMapper;
    private final RunnerProfileMapper runnerProfileMapper;
    private final TaskMapper taskMapper;
    private final TaskOrderMapper taskOrderMapper;
    private final TransactionRecordMapper transactionRecordMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;
    private final StringRedisTemplate redisTemplate;

    /**
     * 管理员登录。
     *
     * <p>校验账号密码并检查状态后，生成双令牌（access + refresh），
     * refresh token 存入 Redis 支持后续轮换。
     *
     * @param dto 管理员登录DTO（用户名、密码）
     * @return AdminLoginVO 包含双令牌和过期时间的登录结果
     */
    @Override
    @Transactional
    public AdminLoginVO login(AdminLoginDTO dto) {
        // 登录失败次数检查（防暴力破解）
        String failKey = RedisConstant.ADMIN_LOGIN_FAIL_PREFIX + dto.getUsername();
        String failCount = redisTemplate.opsForValue().get(failKey);
        if (failCount != null && Integer.parseInt(failCount) >= RedisConstant.LOGIN_MAX_FAIL_COUNT) {
            throw new BusinessException("登录失败次数过多，请15分钟后再试");
        }

        Admin admin = lambdaQuery().eq(Admin::getUsername, dto.getUsername()).one();
        if (admin == null || !passwordEncoder.matches(dto.getPassword(), admin.getPassword())) {
            redisTemplate.opsForValue().increment(failKey);
            redisTemplate.expire(failKey, RedisConstant.LOGIN_LOCK_SECONDS, TimeUnit.SECONDS);
            throw new BusinessException(MessageConstant.ADMIN_LOGIN_FAILED);
        }

        // 登录成功后清除失败计数
        redisTemplate.delete(failKey);
        if (Objects.equals(admin.getStatus(), StatusConstant.DISABLE)) {
            throw new BusinessException(MessageConstant.ADMIN_DISABLED);
        }
        admin.setLastLoginTime(LocalDateTime.now());
        updateById(admin);

        // 生成双 Token
        String accessToken = jwtUtil.generateAdminAccessToken(Map.of(JwtClaimsConstant.ADMIN_ID, admin.getId()));
        String refreshToken = jwtUtil.generateAdminRefreshToken(Map.of(JwtClaimsConstant.ADMIN_ID, admin.getId()));

        // 存储 refresh token 到 Redis
        var refreshClaims = jwtUtil.parseAdminRefreshToken(refreshToken);
        String jti = refreshClaims.get(JwtClaimsConstant.JTI, String.class);
        long ttlSeconds = jwtProperties.getAdminRefreshTtl() / 1000;
        redisTemplate.opsForValue().set(
                RedisConstant.ADMIN_REFRESH_TOKEN_PREFIX + admin.getId() + ":" + jti,
                refreshToken,
                ttlSeconds,
                java.util.concurrent.TimeUnit.SECONDS);

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
     * 获取当前登录管理员信息，仅返回基本信息不含令牌。
     */
    @Override
    public AdminLoginVO getAdminInfo() {
        Long adminId = com.ikeu.common.context.BaseContext.getCurrentId();
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
     * 刷新管理员访问令牌（令牌轮换）。
     *
     * <p>流程：
     * <ol>
     *   <li>解析 refresh token 并验证 {@code type=refresh}、{@code adminId}、{@code jti}。</li>
     *   <li>Redis 反查 —— 若不存在说明已被轮换或已过期，拒绝请求。</li>
     *   <li>删除旧 token 并颁发新的 token 对（access + refresh），存储新 refresh token 到 Redis。</li>
     * </ol>
     *
     * @param refreshToken 刷新令牌token
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

        // 删除旧 refresh token（令牌轮换）
        redisTemplate.delete(redisKey);

        Admin admin = getById(adminId);
        if (admin == null || Objects.equals(admin.getStatus(), StatusConstant.DISABLE)) {
            throw new UnauthorizedException(MessageConstant.ACCOUNT_DISABLED_OR_NOT_EXIST);
        }

        String newAccessToken = jwtUtil.generateAdminAccessToken(Map.of(JwtClaimsConstant.ADMIN_ID, adminId));
        String newRefreshToken = jwtUtil.generateAdminRefreshToken(Map.of(JwtClaimsConstant.ADMIN_ID, adminId));

        var newClaims = jwtUtil.parseAdminRefreshToken(newRefreshToken);
        String newJti = newClaims.get(JwtClaimsConstant.JTI, String.class);
        long ttlSeconds = jwtProperties.getAdminRefreshTtl() / 1000;
        redisTemplate.opsForValue().set(
                RedisConstant.ADMIN_REFRESH_TOKEN_PREFIX + adminId + ":" + newJti,
                newRefreshToken,
                ttlSeconds,
                java.util.concurrent.TimeUnit.SECONDS);

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
     * 管理员退出登录，清除所有 refresh token。
     *
     * <p>通过模式匹配 {@code admin:refresh:token:{adminId}:*} 删除该管理员所有 token，
     * 使所有终端的 refresh token 同时失效。
     *
     * @param adminId 管理员id
     */
    @Override
    public void logout(Long adminId) {
        if (adminId == null) return;
        String pattern = RedisConstant.ADMIN_REFRESH_TOKEN_PREFIX + adminId + ":*";
        var keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
        log.info("管理员 {} 退出登录，已清除 refresh token", adminId);
    }

    /**
     * 分页查询用户列表方法
     *  逻辑：支持按用户状态、认证状态、关键词搜索条件筛选，关键词匹配用户名、手机号、昵称
     *
     * @param status 用户状态筛选条件（0-禁用，1-正常），可为null
     * @param isCertify 认证状态筛选条件（0-未认证，1-审核中，2-已认证，3-驳回），可为null
     * @param keyword 搜索关键词，匹配用户名、手机号、昵称，可为null
     * @param page 页码
     * @param size 每页记录数
     * @return PageResult<UserInfoVO> 分页用户列表
     */
    @Override
    public PageResult<UserInfoVO> listUsers(Integer status, Integer isCertify, String keyword, int page, int size) {
        Page<User> p = userMapper.selectUsersWithKeyword(new Page<>(page, size), status, isCertify, keyword);
        List<UserInfoVO> records = p.getRecords().stream()
                .map(u -> BeanUtil.copyProperties(u, UserInfoVO.class))
                .collect(Collectors.toList());
        return new PageResult<>(p.getTotal(), records);
    }

    /**
     * 切换用户状态方法（启用/禁用）
     *  逻辑：根据enabled参数设置用户状态为启用或禁用，清除仪表盘缓存
     *
     * @param userId 用户ID
     * @param enabled 是否启用（true-启用，false-禁用）
     */
    @Override
    @Transactional
    @CacheEvict(value = RedisConstant.CACHE_DASHBOARD, allEntries = true)
    public void toggleUserStatus(Long userId, Boolean enabled) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new NotFoundException(MessageConstant.USER_NOT_EXIST);
        user.setStatus(enabled ? StatusConstant.ENABLE : StatusConstant.DISABLE);
        userMapper.updateById(user);
        log.info("管理员切换用户 {} 状态为 {}", userId, enabled ? "启用" : "禁用");
    }

    /**
     * 审核跑腿员认证信息方法
     *  校验：跑腿员档案存在且处于"审核中"状态
     *  逻辑：更新审核状态和审核备注，清除仪表盘缓存
     *
     * @param runnerProfileId 跑腿员档案ID
     * @param verifyStatus 审核结果
     * @param remark 审核备注
     */
    @Override
    @Transactional
    @CacheEvict(value = RedisConstant.CACHE_DASHBOARD, allEntries = true)
    public void reviewRunnerCertification(Long runnerProfileId, Integer verifyStatus, String remark) {
        RunnerProfile profile = runnerProfileMapper.selectById(runnerProfileId);
        if (profile == null) throw new NotFoundException(MessageConstant.RUNNER_NOT_EXIST);
        if (!profile.getVerifyStatus().equals(StatusConstant.CERTIFY_AUDITING)) {
            throw new BusinessException(MessageConstant.RUNNER_NOT_IN_AUDITING_STATUS);
        }
        profile.setVerifyStatus(verifyStatus);
        profile.setVerifyRemark(remark != null ? remark : "");
        profile.setUpdatedAt(LocalDateTime.now());
        runnerProfileMapper.updateById(profile);

        log.info("管理员审核跑腿员 {} 结果为 {}", runnerProfileId, verifyStatus);
    }

    /**
     *  审核用户实名认证方法（学生身份认证）
     *  校验：用户存在且认证状态为"审核中"
     *  逻辑：更新认证审核结果和审核备注，清除仪表盘缓存
     *
     * @param userId 用户ID
     * @param isCertify 认证审核结果（2-通过，3-驳回）
     * @param remark 审核备注（驳回原因）
     */
    @Override
    @Transactional
    @CacheEvict(value = RedisConstant.CACHE_DASHBOARD, allEntries = true)
    public void reviewUserCertification(Long userId, Integer isCertify, String remark) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new NotFoundException(MessageConstant.USER_NOT_EXIST);
        if (!Objects.equals(user.getIsCertify(), StatusConstant.CERTIFY_AUDITING)) {
            throw new BusinessException(MessageConstant.USER_NOT_IN_AUDITING_STATUS);
        }
        user.setIsCertify(isCertify);
        user.setCertifyRemark(remark != null ? remark : "");
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        log.info("管理员审核用户 {} 实名认证，结果：{}，备注：{}", userId, isCertify, remark);
    }

    @Override
    public UserInfoVO getUserDetail(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new NotFoundException(MessageConstant.USER_NOT_EXIST);
        UserInfoVO vo = BeanUtil.copyProperties(user, UserInfoVO.class);
        RunnerProfile rp = runnerProfileMapper.selectOne(
                new LambdaQueryWrapper<RunnerProfile>().eq(RunnerProfile::getUserId, userId));
        if (rp != null) {
            vo.setVerifyStatus(rp.getVerifyStatus());
        }
        return vo;
    }

    @Override
    public RunnerManageVO getRunnerDetail(Long profileId) {
        RunnerProfile rp = runnerProfileMapper.selectById(profileId);
        if (rp == null) throw new NotFoundException(MessageConstant.RUNNER_NOT_EXIST);
        User u = userMapper.selectById(rp.getUserId());

        // 累计收入：数据库侧 SUM 聚合，避免全字段拉取
        BigDecimal totalIncome = transactionRecordMapper.sumIncomeByUserId(rp.getUserId());

        return RunnerManageVO.builder()
                .profileId(rp.getId())
                .userId(rp.getUserId())
                .nickname(u != null ? u.getNickname() : "")
                .phone(u != null ? u.getPhone() : "")
                .realName(rp.getRealName())
                .verifyStatus(rp.getVerifyStatus())
                .creditScore(rp.getCreditScore())
                .totalOrders(rp.getTotalOrders())
                .successOrders(rp.getSuccessOrders())
                .avgRating(rp.getAvgRating() != null ? rp.getAvgRating().doubleValue() : null)
                .isOnline(rp.getIsOnline())
                .currentOrders(rp.getCurrentOrders())
                .maxConcurrentOrders(rp.getMaxConcurrentOrders())
                .isBanned(rp.getIsBanned())
                .totalEarnings(totalIncome)
                .build();
    }

    @Override
    @Transactional
    @CacheEvict(value = RedisConstant.CACHE_DASHBOARD, allEntries = true)
    public void toggleRunnerBan(Long profileId, boolean banned) {
        RunnerProfile rp = runnerProfileMapper.selectById(profileId);
        if (rp == null) throw new NotFoundException(MessageConstant.RUNNER_NOT_EXIST);
        rp.setIsBanned(banned ? 1 : 0);
        rp.setUpdatedAt(LocalDateTime.now());
        runnerProfileMapper.updateById(rp);
        log.info("管理员 {} 跑腿员 {} (profileId={})", banned ? "禁止" : "恢复", rp.getUserId(), profileId);
    }

    /**
     *  获取所有任务列表方法
     *  逻辑：支持按任务状态筛选，关联查询发布者信息填充昵称和头像
     *
     * @param status 任务状态筛选条件，可为null表示不限制
     * @param page 页码
     * @param size 每页记录数
     * @return PageResult<TaskListVO> 分页任务列表
     */
    @Override
    public PageResult<TaskListVO> listAllTasks(Integer status, int page, int size) {
        LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(status != null, Task::getStatus, status)
                .orderByDesc(Task::getCreatedAt);

        Page<Task> p = taskMapper.selectPage(new Page<>(page, size), wrapper);
        if (p.getRecords().isEmpty()) {
            return new PageResult<>(0L, Collections.emptyList());
        }

        List<Long> publisherIds = p.getRecords().stream()
                .map(Task::getPublisherId).distinct().collect(Collectors.toList());
        List<User> publishers = userMapper.selectBatchIds(publisherIds);
        java.util.Map<Long, User> publisherMap = publishers.stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        List<TaskListVO> records = p.getRecords().stream().map(task -> {
            TaskListVO vo = BeanUtil.copyProperties(task, TaskListVO.class, "imageUrls", "publishTime");
            vo.setTaskId(task.getId());
            vo.setPublishTime(task.getCreatedAt());
            User pub = publisherMap.get(task.getPublisherId());
            vo.setPublisherNickname(pub != null ? pub.getNickname() : "");
            vo.setPublisherUsername(pub != null ? pub.getUsername() : "");
            vo.setPublisherAvatar(pub != null ? pub.getAvatarUrl() : "");
            vo.setImageUrls(task.getImageUrls() != null
                    ? JSONUtil.toList(task.getImageUrls(), String.class) : Collections.emptyList());
            return vo;
        }).collect(Collectors.toList());

        return new PageResult<>(p.getTotal(), records);
    }

    /**
     * 获取仪表盘数据方法
     *  逻辑：统计用户总数、任务总数、订单总数、认证跑腿员数、今日新增用户数和今日完成任务的报酬总额，结果缓存3分钟
     *
     * @return DashboardVO 仪表盘数据VO
     */
    @Override
    @Cacheable(value = RedisConstant.CACHE_DASHBOARD, key = "'dashboard'")
    public DashboardVO getDashboard() {
        Long userCount = userMapper.selectCount(null);
        Long taskCount = taskMapper.selectCount(null);
        Long orderCount = taskOrderMapper.selectCount(null);
        Long runnerCount = runnerProfileMapper.selectCount(
                new LambdaQueryWrapper<RunnerProfile>().eq(RunnerProfile::getVerifyStatus, StatusConstant.CERTIFY_APPROVED));
        Long onlineRunnerCount = runnerProfileMapper.selectCount(
                new LambdaQueryWrapper<RunnerProfile>().eq(RunnerProfile::getIsOnline, 1));

        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime sevenDaysAgo = todayStart.minusDays(6);

        Long todayNewUsers = userMapper.selectCount(
                new LambdaQueryWrapper<User>().ge(User::getCreatedAt, todayStart));
        Long todayNewTasks = taskMapper.selectCount(
                new LambdaQueryWrapper<Task>().ge(Task::getCreatedAt, todayStart));
        Long todayCompletedOrders = taskOrderMapper.selectCount(
                new LambdaQueryWrapper<TaskOrder>().eq(TaskOrder::getStatus, StatusConstant.ORDER_COMPLETED)
                        .ge(TaskOrder::getConfirmTime, todayStart));

        BigDecimal todayRevenue = taskMapper.sumRewardByStatusSince(StatusConstant.TASK_COMPLETED, todayStart);

        // 近7天新增用户趋势
        List<DashboardVO.TrendPoint> userTrend = buildUserTrend(sevenDaysAgo);

        // 近7天收入趋势
        List<DashboardVO.TrendPoint> revenueTrend = buildRevenueTrend(sevenDaysAgo);

        // 任务分类占比
        List<DashboardVO.CategoryPie> taskCategories = buildTaskCategories();

        // 订单状态分布
        List<DashboardVO.CategoryPie> orderStatusDistribution = buildOrderStatusDistribution();

        return DashboardVO.builder()
                .userCount(userCount).taskCount(taskCount)
                .orderCount(orderCount).runnerCount(runnerCount)
                .onlineRunnerCount(onlineRunnerCount)
                .todayRevenue(todayRevenue).todayNewUsers(todayNewUsers)
                .todayNewTasks(todayNewTasks).todayCompletedOrders(todayCompletedOrders)
                .userTrend(userTrend).revenueTrend(revenueTrend)
                .taskCategories(taskCategories)
                .orderStatusDistribution(orderStatusDistribution)
                .build();
    }

    private List<DashboardVO.TrendPoint> buildUserTrend(LocalDateTime since) {
        List<DashboardVO.TrendPoint> result = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM-dd");
        LocalDate cursor = since.toLocalDate();
        LocalDate today = LocalDate.now();
        while (!cursor.isAfter(today)) {
            LocalDateTime dayStart = cursor.atStartOfDay();
            LocalDateTime dayEnd = cursor.plusDays(1).atStartOfDay();
            Long count = userMapper.selectCount(new LambdaQueryWrapper<User>()
                    .ge(User::getCreatedAt, dayStart).lt(User::getCreatedAt, dayEnd));
            result.add(DashboardVO.TrendPoint.builder()
                    .date(cursor.format(fmt)).value(count).build());
            cursor = cursor.plusDays(1);
        }
        return result;
    }

    private List<DashboardVO.TrendPoint> buildRevenueTrend(LocalDateTime since) {
        List<DashboardVO.TrendPoint> result = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM-dd");
        List<Map<String, Object>> rows = taskMapper.sumRewardPerDay(StatusConstant.TASK_COMPLETED, since);
        Map<String, BigDecimal> dateSum = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            String date = row.get("date").toString();
            BigDecimal val = row.get("value") != null ? new BigDecimal(row.get("value").toString()) : BigDecimal.ZERO;
            dateSum.put(date, val);
        }
        LocalDate cursor = since.toLocalDate();
        LocalDate today = LocalDate.now();
        while (!cursor.isAfter(today)) {
            String key = cursor.toString();
            Long value = dateSum.containsKey(key) ? dateSum.get(key).longValue() : 0L;
            result.add(DashboardVO.TrendPoint.builder()
                    .date(cursor.format(fmt)).value(value).build());
            cursor = cursor.plusDays(1);
        }
        return result;
    }

    private List<DashboardVO.CategoryPie> buildTaskCategories() {
        List<Map<String, Object>> rows = taskMapper.countTasksByType();
        return rows.stream().map(row -> {
            String name = row.get("name") != null ? row.get("name").toString() : "";
            Long value = row.get("value") != null ? Long.parseLong(row.get("value").toString()) : 0L;
            return DashboardVO.CategoryPie.builder().name(name).value(value).build();
        }).collect(Collectors.toList());
    }

    private static final String[] ORDER_STATUS_LABELS = {"待取货", "配送中", "待确认", "已完成", "已取消"};

    private List<DashboardVO.CategoryPie> buildOrderStatusDistribution() {
        List<DashboardVO.CategoryPie> result = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            Long count = taskOrderMapper.selectCount(
                    new LambdaQueryWrapper<TaskOrder>().eq(TaskOrder::getStatus, i)
                            .eq(TaskOrder::getIsDeleted, 0));
            result.add(DashboardVO.CategoryPie.builder()
                    .name(ORDER_STATUS_LABELS[i - 1]).value(count).build());
        }
        return result;
    }

    /**
     * 获取任务详情（管理端），不脱敏。
     */
    @Override
    public TaskDetailVO getTaskDetail(Long taskId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) throw new NotFoundException(MessageConstant.TASK_NOT_EXIST);
        User publisher = userMapper.selectById(task.getPublisherId());

        return TaskDetailVO.builder()
                .taskId(task.getId())
                .taskNo(task.getTaskNo())
                .type(task.getType())
                .subType(task.getSubType())
                .taskSpecs(task.getTaskSpecs())
                .publicDesc(task.getPublicDesc())
                .privateNote(task.getPrivateNote())
                .reward(task.getReward())
                .pickupAddress(task.getPickupAddress())
                .pickupLng(task.getPickupLng())
                .pickupLat(task.getPickupLat())
                .deliveryAddress(task.getDeliveryAddress())
                .deliveryLng(task.getDeliveryLng())
                .deliveryLat(task.getDeliveryLat())
                .expireTime(task.getExpireTime())
                .publishTime(task.getCreatedAt())
                .publisherNickname(publisher != null ? publisher.getNickname() : "")
                .publisherUsername(publisher != null ? publisher.getUsername() : "")
                .publisherAvatar(publisher != null ? publisher.getAvatarUrl() : "")
                .imageUrls(task.getImageUrls() != null
                        ? JSONUtil.toList(task.getImageUrls(), String.class) : Collections.emptyList())
                .pickupCode(task.getPickupCode())
                .hasPickupCode(task.getPickupCode() != null && !task.getPickupCode().isBlank())
                .requireSex(task.getRequireSex())
                .contactName(task.getContactName())
                .contactPhone(task.getContactPhone())
                .status(task.getStatus())
                .cancelReason(task.getCancelReason())
                .cancelTime(task.getUpdatedAt())
                .build();
    }

    /**
     * 强制更新任务状态方法
     *  逻辑：直接更新任务状态为指定值，用于管理员手动调整，清除仪表盘缓存
     *
     * @param taskId 任务ID
     * @param status 新状态值
     */
    @Override
    @Transactional
    @CacheEvict(value = RedisConstant.CACHE_DASHBOARD, allEntries = true)
    public void updateTaskStatus(Long taskId, Integer status) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) throw new NotFoundException(MessageConstant.TASK_NOT_EXIST);
        com.ikeu.common.enums.TaskStateMachine.validate(task.getStatus(), status, "任务");
        task.setStatus(status);
        task.setUpdatedAt(LocalDateTime.now());
        taskMapper.updateById(task);
        log.info("管理员强制更新任务 {} 状态为 {} → {}", taskId, task.getStatus(), status);
    }

    /**
     * 获取所有订单列表方法
     *  逻辑：支持按订单状态筛选，关联查询任务信息、跑腿员信息和发布者信息构造OrderManageVO
     *
     * @param status 订单状态筛选条件，可为null表示不限制
     * @param page 页码
     * @param size 每页记录数
     * @return PageResult<OrderManageVO> 分页订单管理列表
     */
    @Override
    public PageResult<OrderManageVO> listAllOrders(Integer status, int page, int size) {
        LambdaQueryWrapper<TaskOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(status != null, TaskOrder::getStatus, status)
                .orderByDesc(TaskOrder::getAcceptTime);

        Page<TaskOrder> p = taskOrderMapper.selectPage(new Page<>(page, size), wrapper);
        if (p.getRecords().isEmpty()) return new PageResult<>(0L, Collections.emptyList());

        List<Long> taskIds = p.getRecords().stream().map(TaskOrder::getTaskId).distinct().collect(Collectors.toList());
        List<Long> runnerIds = p.getRecords().stream().map(TaskOrder::getRunnerId).distinct().toList();
        List<Task> tasks = taskMapper.selectBatchIds(taskIds);

        Map<Long, Task> taskMap = tasks.stream().collect(Collectors.toMap(Task::getId, t -> t));

        // 合并 runner 和 publisher 用户ID，一次批量查询
        Set<Long> allUserIds = new HashSet<>(runnerIds);
        tasks.stream().map(Task::getPublisherId).distinct().forEach(allUserIds::add);
        Map<Long, User> userMap = userMapper.selectBatchIds(new ArrayList<>(allUserIds))
                .stream().collect(Collectors.toMap(User::getId, u -> u));

        List<OrderManageVO> records = p.getRecords().stream().map(order -> {
            Task task = taskMap.get(order.getTaskId());
            User runner = userMap.get(order.getRunnerId());
            User publisher = task != null ? userMap.get(task.getPublisherId()) : null;

            return OrderManageVO.builder()
                    .orderId(order.getId())
                    .taskId(order.getTaskId())
                    .taskNo(task != null ? task.getTaskNo() : "")
                    .publicDesc(task != null ? task.getPublicDesc() : "")
                    .reward(task != null ? task.getReward() : BigDecimal.ZERO)
                    .publisherId(task != null ? task.getPublisherId() : null)
                    .publisherNickname(publisher != null ? publisher.getNickname() : "")
                    .runnerId(order.getRunnerId())
                    .runnerNickname(runner != null ? runner.getNickname() : "")
                    .status(order.getStatus())
                    .acceptTime(order.getAcceptTime())
                    .pickupTime(order.getPickupTime())
                    .deliverTime(order.getDeliverTime())
                    .confirmTime(order.getConfirmTime())
                    .expectFinishTime(order.getExpectFinishTime())
                    .build();
        }).collect(Collectors.toList());

        return new PageResult<>(p.getTotal(), records);
    }

    /**
     * 获取订单详情方法
     *  逻辑：查询订单及关联的任务、发布者、跑腿员信息，构建完整的 OrderDetailVO
     *
     * @param orderId 订单ID
     * @return OrderDetailVO 订单详情
     */
    @Override
    public OrderDetailVO getOrderDetail(Long orderId) {
        TaskOrder order = taskOrderMapper.selectById(orderId);
        if (order == null) throw new NotFoundException(MessageConstant.ORDER_NOT_EXIST);

        Task task = taskMapper.selectById(order.getTaskId());
        if (task == null) throw new NotFoundException(MessageConstant.TASK_NOT_EXIST);

        User publisher = userMapper.selectById(task.getPublisherId());
        User runner = userMapper.selectById(order.getRunnerId());

        return OrderDetailVO.builder()
                .orderId(order.getId())
                .taskId(task.getId())
                .taskNo(task.getTaskNo())
                .type(task.getType())
                .subType(task.getSubType())
                .taskSpecs(task.getTaskSpecs())
                .publicDesc(task.getPublicDesc())
                .privateNote(task.getPrivateNote())
                .reward(task.getReward())
                .orderStatus(order.getStatus())
                .pickupAddress(task.getPickupAddress())
                .deliveryAddress(task.getDeliveryAddress())
                .pickupCode(task.getPickupCode())
                .imageUrls(task.getImageUrls() != null
                        ? JSONUtil.toList(task.getImageUrls(), String.class) : Collections.emptyList())
                .publisherId(task.getPublisherId())
                .runnerId(order.getRunnerId())
                .contactName(task.getContactName())
                .contactPhone(task.getContactPhone())
                .publisherPhone(publisher != null ? publisher.getPhone() : "")
                .runnerPhone(runner != null ? runner.getPhone() : "")
                .publisherAvatar(publisher != null ? publisher.getAvatarUrl() : "")
                .runnerAvatar(runner != null ? runner.getAvatarUrl() : "")
                .publisherNickname(publisher != null ? publisher.getNickname() : "")
                .publisherUsername(publisher != null ? publisher.getUsername() : "")
                .runnerNickname(runner != null ? runner.getNickname() : "")
                .runnerUsername(runner != null ? runner.getUsername() : "")
                .acceptTime(order.getAcceptTime())
                .pickupTime(order.getPickupTime())
                .deliverTime(order.getDeliverTime())
                .confirmTime(order.getConfirmTime())
                .expectFinishTime(order.getExpectFinishTime())
                .pickupProofImgs(order.getPickupProofImgs() != null
                        ? JSONUtil.toList(order.getPickupProofImgs(), String.class) : Collections.emptyList())
                .deliverProofImgs(order.getDeliverProofImgs() != null
                        ? JSONUtil.toList(order.getDeliverProofImgs(), String.class) : Collections.emptyList())
                .cancelReason(order.getCancelReason())
                .cancelTime(order.getStatus().equals(StatusConstant.ORDER_CANCELLED) ? order.getConfirmTime() : null)
                .build();
    }

    /**
     * 强制更新订单状态方法
     *  逻辑：使用 OrderStateMachine 校验状态转换合法性后更新订单状态，
     *  同时同步更新关联任务状态，清除仪表盘缓存
     *
     * @param orderId 订单ID
     * @param status 新状态值
     */
    @Override
    @Transactional
    @CacheEvict(value = RedisConstant.CACHE_DASHBOARD, allEntries = true)
    public void updateOrderStatus(Long orderId, Integer status) {
        TaskOrder order = taskOrderMapper.selectById(orderId);
        if (order == null) throw new NotFoundException(MessageConstant.ORDER_NOT_EXIST);
        Task task = taskMapper.selectById(order.getTaskId());

        OrderStateMachine.validate(order.getStatus(), status, "订单");

        LocalDateTime now = LocalDateTime.now();
        order.setStatus(status);
        if (status.equals(StatusConstant.ORDER_COMPLETED)) order.setConfirmTime(now);
        if (status.equals(StatusConstant.ORDER_CANCELLED)) order.setConfirmTime(now);
        taskOrderMapper.updateById(order);

        // 同步任务状态
        if (task != null) {
            Integer taskStatus = mapOrderStatusToTaskStatus(status);
            if (taskStatus != null) {
                task.setStatus(taskStatus);
                task.setUpdatedAt(now);
                taskMapper.updateById(task);
            }
        }

        log.info("管理员强制更新订单 {} 状态为 {} → {}", orderId, order.getStatus(), status);
    }

    private Integer mapOrderStatusToTaskStatus(Integer orderStatus) {
        if (orderStatus.equals(StatusConstant.ORDER_WAIT_PICKUP)) return StatusConstant.TASK_ACCEPTED;
        if (orderStatus.equals(StatusConstant.ORDER_DELIVERING)) return StatusConstant.TASK_DELIVERING;
        if (orderStatus.equals(StatusConstant.ORDER_WAIT_CONFIRM)) return StatusConstant.TASK_WAIT_CONFIRM;
        if (orderStatus.equals(StatusConstant.ORDER_COMPLETED)) return StatusConstant.TASK_COMPLETED;
        if (orderStatus.equals(StatusConstant.ORDER_CANCELLED)) return StatusConstant.TASK_CANCELLED;
        return null;
    }

    /**
     * 获取所有跑腿员列表方法
     *  逻辑：支持按审核状态和关键词（昵称/手机号）筛选，
     *  先根据关键词匹配用户表获取用户ID，再下推到SQL过滤保证分页正确，
     *  关联查询用户信息填充昵称和手机号
     *
     * @param verifyStatus 审核状态筛选条件，可为null表示不限制
     * @param keyword 昵称或手机号关键词，可为null表示不限制
     * @param page 页码
     * @param size 每页记录数
     * @return PageResult<RunnerManageVO> 分页跑腿员管理列表
     */
    @Override
    public PageResult<RunnerManageVO> listRunners(Integer verifyStatus, String keyword, int page, int size) {
        // 单次 JOIN 查询，不再先查 user 表再 IN 下推
        String kw = (keyword != null && !keyword.isBlank()) ? keyword : null;
        Page<RunnerProfile> p = (Page<RunnerProfile>) runnerProfileMapper.selectRunnersWithKeyword(new Page<>(page, size), verifyStatus, kw);
        if (p.getRecords().isEmpty()) return new PageResult<>(0L, Collections.emptyList());

        List<Long> userIds = p.getRecords().stream().map(RunnerProfile::getUserId).collect(Collectors.toList());
        List<User> users = userMapper.selectBatchIds(userIds);
        Map<Long, User> userMap = users.stream().collect(Collectors.toMap(User::getId, u -> u));

        // 批量查询累计收入：数据库侧 GROUP BY SUM，避免全字段拉取到内存
        Map<Long, BigDecimal> incomeMap = transactionRecordMapper.sumIncomeByUserIds(userIds)
                .stream()
                .collect(Collectors.toMap(
                        row -> (Long) row.get("user_id"),
                        row -> (BigDecimal) row.get("total_income")));

        List<RunnerManageVO> records = p.getRecords().stream().map(rp -> {
            User u = userMap.get(rp.getUserId());
            return RunnerManageVO.builder()
                    .profileId(rp.getId())
                    .userId(rp.getUserId())
                    .nickname(u != null ? u.getNickname() : "")
                    .phone(u != null ? u.getPhone() : "")
                    .realName(rp.getRealName())
                    .verifyStatus(rp.getVerifyStatus())
                    .creditScore(rp.getCreditScore())
                    .totalOrders(rp.getTotalOrders())
                    .successOrders(rp.getSuccessOrders())
                    .avgRating(rp.getAvgRating() != null ? rp.getAvgRating().doubleValue() : null)
                    .isOnline(rp.getIsOnline())
                    .currentOrders(rp.getCurrentOrders())
                    .isBanned(rp.getIsBanned())
                    .maxConcurrentOrders(rp.getMaxConcurrentOrders())
                    .totalEarnings(incomeMap.getOrDefault(rp.getUserId(), BigDecimal.ZERO))
                    .build();
        }).collect(Collectors.toList());

        return new PageResult<>(p.getTotal(), records);
    }
}
