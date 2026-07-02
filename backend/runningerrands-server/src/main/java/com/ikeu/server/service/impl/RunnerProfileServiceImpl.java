package com.ikeu.server.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ikeu.common.constant.MessageConstant;
import com.ikeu.common.constant.RedisConstant;
import com.ikeu.common.constant.StatusConstant;
import com.ikeu.common.exception.BusinessException;
import com.ikeu.common.exception.NotFoundException;
import com.ikeu.common.exception.ForbiddenException;
import com.ikeu.model.dto.OnTimeStatsDTO;
import com.ikeu.model.dto.SetMaxOrdersDTO;
import com.ikeu.model.entity.RunnerProfile;
import com.ikeu.model.entity.User;
import com.ikeu.model.vo.RunnerInfoVO;
import com.ikeu.model.vo.RunnerRankingVO;
import com.ikeu.model.vo.RunnerPerformanceVO;
import com.ikeu.server.mapper.RunnerProfileMapper;
import com.ikeu.server.mapper.TaskOrderMapper;
import com.ikeu.server.mapper.TransactionRecordMapper;
import com.ikeu.server.mapper.UserMapper;
import com.ikeu.server.service.RunnerProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 配送员档案服务实现，处理配送员申请、上下线、排行榜和表现数据等功能。
 * @author ikeu
 * @since 2026/05/14
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RunnerProfileServiceImpl extends ServiceImpl<RunnerProfileMapper, RunnerProfile> implements RunnerProfileService {

    private final RunnerProfileMapper runnerProfileMapper;
    private final UserMapper userMapper;
    private final TransactionRecordMapper transactionRecordMapper;
    private final TaskOrderMapper taskOrderMapper;

    /**
     * 根据用户ID获取配送员信息方法
     *  校验：配送员档案不存在时抛出异常
     *
     * @param userId 用户ID
     * @return RunnerProfile 跑腿员档案实体
     */
    private RunnerProfile getByUserId(Long userId) {
        RunnerProfile profile = lambdaQuery().eq(RunnerProfile::getUserId, userId).one();
        if (profile == null) {
            throw new ForbiddenException(MessageConstant.RUNNER_NOT_CERTIFIED);
        }
        return profile;
    }

    /**
     * 申请成为配送员方法（需已通过学生实名认证）
     *  校验：用户已通过实名认证，无重复提交或重复审核中的申请
     *  逻辑：检查是否已有跑腿员档案，审核中或已认证则阻止重复提交，
     *  驳回后可重新申请，新申请创建跑腿员档案并设置初始信用分100分
     *
     * @param userId 用户ID
     */
    @Override
    @Transactional
    public void applyForRunner(Long userId) {
        // 检查用户是否已通过学生实名认证
        User user = userMapper.selectById(userId);
        if (user == null) throw new NotFoundException(MessageConstant.USER_NOT_EXIST);
        if (!Objects.equals(user.getIsCertify(), StatusConstant.CERTIFY_APPROVED)) {
            throw new ForbiddenException(MessageConstant.USER_NOT_CERTIFIED);
        }

        // 检查是否已有跑腿员档案
        RunnerProfile existing = runnerProfileMapper.selectOne(
                new LambdaQueryWrapper<RunnerProfile>().eq(RunnerProfile::getUserId, userId));
        if (existing != null) {
            if (Objects.equals(existing.getVerifyStatus(), StatusConstant.CERTIFY_AUDITING)) {
                throw new BusinessException(MessageConstant.RUNNER_APPLY_AUDITING);
            }
            if (Objects.equals(existing.getVerifyStatus(), StatusConstant.CERTIFY_APPROVED)) {
                throw new BusinessException(MessageConstant.RUNNER_ALREADY_CERTIFIED);
            }
            // 驳回后重新申请
            existing.setVerifyStatus(StatusConstant.CERTIFY_AUDITING);
            existing.setUpdatedAt(LocalDateTime.now());
            runnerProfileMapper.updateById(existing);
        } else {
            RunnerProfile profile = RunnerProfile.builder()
                    .userId(userId)
                    .realName(user.getRealName())
                    .verifyStatus(StatusConstant.CERTIFY_AUDITING)
                    .creditScore(100)
                    .totalOrders(0)
                    .successOrders(0)
                    .avgRating(BigDecimal.valueOf(5.0))
                    .isOnline(StatusConstant.RUNNER_OFFLINE)
                    .maxConcurrentOrders(3)
                    .currentOrders(0)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            runnerProfileMapper.insert(profile);
        }
        log.info("用户 {} 申请成为配送员", userId);
    }

    /**
     * 获取配送员信息方法
     *  逻辑：查询配送员档案信息，转换为RunnerInfoVO返回
     *
     * @param userId 用户ID
     * @return RunnerInfoVO 配送员信息VO
     */
    @Override
    public RunnerInfoVO getProfile(Long userId) {
        RunnerProfile profile = getByUserId(userId);

        RunnerInfoVO runnerInfoVO = BeanUtil.copyProperties(profile, RunnerInfoVO.class);
        if (runnerInfoVO != null && profile.getAvgRating() != null) {
            runnerInfoVO.setAvgRating(profile.getAvgRating().doubleValue());
        }
        return runnerInfoVO;
    }

    /**
     * 配送员上线方法
     *  校验：配送员档案存在且已通过认证，未处于上线状态
     *  逻辑：设置配送员状态为"上线"
     *
     * @param userId 用户ID
     */
    @Override
    @Transactional
    public void goOnline(Long userId) {
        RunnerProfile profile = getByUserId(userId);
        if (!Objects.equals(profile.getVerifyStatus(), StatusConstant.CERTIFY_APPROVED)) {
            throw new ForbiddenException(MessageConstant.RUNNER_NOT_CERTIFIED);
        }
        if (Objects.equals(profile.getIsOnline(), StatusConstant.RUNNER_ONLINE)) {
            throw new BusinessException(MessageConstant.RUNNER_ONLINE);
        }
        profile.setIsOnline(StatusConstant.RUNNER_ONLINE);
        updateById(profile);
        log.info("配送员 {} 上线接单", userId);
    }

    /**
     * 配送员离线方法
     *  校验：配送员未处于离线状态
     *  逻辑：设置配送员状态为"离线"
     *
     * @param userId 用户ID
     */
    @Override
    @Transactional
    public void goOffline(Long userId) {
        RunnerProfile profile = getByUserId(userId);
        if (Objects.equals(profile.getIsOnline(), StatusConstant.RUNNER_OFFLINE)) {
            throw new BusinessException(MessageConstant.RUNNER_GO_OFFLINE);
        }
        profile.setIsOnline(StatusConstant.RUNNER_OFFLINE);
        updateById(profile);
        log.info("配送员 {} 下线", userId);
    }

    /**
     * 修改最大接单数方法
     *  逻辑：更新配送员最大并发接单数量
     *
     * @param userId 用户ID
     * @param setMaxOrdersDTO 设置最大接单数DTO
     */
    @Override
    @Transactional
    public void setMaxOrders(Long userId, SetMaxOrdersDTO setMaxOrdersDTO) {
        RunnerProfile profile = getByUserId(userId);
        profile.setMaxConcurrentOrders(setMaxOrdersDTO.getMaxOrders());
        updateById(profile);
        log.info("配送员 {} 修改最大接单数为 {}", userId, setMaxOrdersDTO.getMaxOrders());
    }

    /**
     * 减少配送员当前接单数方法
     *  逻辑：将配送员当前接单数减1，最小为0
     *
     * @param userId 用户ID
     */
    @Override
    @Transactional
    public void decrementCurrentOrders(Long userId) {
        runnerProfileMapper.decrementCurrentOrders(userId);
    }

    /**
     * 获取配送员排行榜方法（缓存5分钟）
     *  逻辑：查询所有已认证配送员，支持按评分或完成率排序，
     *  计算完成率，关联用户信息填充昵称和头像
     *
     * @param sortBy 排序方式（rating-按评分，completion-按完成率，默认按完成单数）
     * @param limit 返回条数限制
     * @return List<RunnerRankingVO> 配送员排行榜列表
     */
    @Override
    @Cacheable(value = RedisConstant.CACHE_LEADERBOARD, key = "#sortBy + ':' + #limit")
    public List<RunnerRankingVO> getLeaderboard(String sortBy, int limit) {
        List<RunnerProfile> profiles = runnerProfileMapper.selectList(
                new LambdaQueryWrapper<RunnerProfile>()
                        .eq(RunnerProfile::getVerifyStatus, StatusConstant.CERTIFY_APPROVED));

        if (profiles.isEmpty()) return Collections.emptyList();

        List<Long> userIds = profiles.stream().map(RunnerProfile::getUserId).collect(Collectors.toList());
        List<User> users = userMapper.selectBatchIds(userIds);
        Map<Long, User> userMap = users.stream().collect(Collectors.toMap(User::getId, u -> u));

        // 批量查询累计收入：数据库侧 GROUP BY SUM，避免全字段拉取到内存
        Map<Long, BigDecimal> incomeMap = transactionRecordMapper.sumIncomeByUserIds(userIds)
                .stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row.get("user_id")).longValue(),
                        row -> (BigDecimal) row.get("total_income")));

        List<RunnerRankingVO> list = new ArrayList<>();
        int rank = 1;
        for (RunnerProfile p : profiles) {
            User u = userMap.get(p.getUserId());
            double completionRate = p.getTotalOrders() > 0
                    ? (double) p.getSuccessOrders() / p.getTotalOrders() * 100 : 0;

            list.add(RunnerRankingVO.builder()
                    .rank((long) rank++)
                    .userId(p.getUserId())
                    .nickname(u != null ? u.getNickname() : "")
                    .avatarUrl(u != null ? u.getAvatarUrl() : "")
                    .totalOrders(p.getTotalOrders())
                    .successOrders(p.getSuccessOrders())
                    .completionRate(Math.round(completionRate * 10.0) / 10.0)
                    .avgRating(p.getAvgRating() != null ? p.getAvgRating().doubleValue() : 5.0)
                    .totalEarnings(incomeMap.getOrDefault(p.getUserId(), BigDecimal.ZERO))
                    .build());
        }

        if ("rating".equals(sortBy)) {
            list.sort(Comparator.comparingDouble(RunnerRankingVO::getAvgRating).reversed());
        } else if ("completion".equals(sortBy)) {
            list.sort(Comparator.comparingDouble(RunnerRankingVO::getCompletionRate).reversed());
        }
        // default: by successOrders (already sorted by profile order)

        return list.size() > limit ? list.subList(0, limit) : list;
    }

    /**
     * 获取配送员表现数据方法
     *  逻辑：计算配送员的完成率，返回总订单数、成功订单数、平均评分、信用分等表现数据
     *
     * @param runnerId 跑腿员用户ID
     * @return RunnerPerformanceVO 配送员表现数据VO
     */
    @Override
    public RunnerPerformanceVO getRunnerPerformance(Long runnerId) {
        RunnerProfile profile = runnerProfileMapper.selectOne(
                new LambdaQueryWrapper<RunnerProfile>().eq(RunnerProfile::getUserId, runnerId));
        if (profile == null) return null;

        double completionRate = profile.getTotalOrders() > 0
                ? (double) profile.getSuccessOrders() / profile.getTotalOrders() * 100 : 0;

        User user = userMapper.selectById(runnerId);

        // 累计跑腿收入：数据库侧 SUM 聚合
        BigDecimal totalEarnings = transactionRecordMapper.sumIncomeByUserId(runnerId);

        // 准时率：数据库侧 COUNT + CASE WHEN 聚合
        OnTimeStatsDTO onTimeStats = taskOrderMapper.countCompletedOnTime(runnerId);
        long total = onTimeStats.getTotal() != null ? onTimeStats.getTotal() : 0;
        long onTime = onTimeStats.getOnTime() != null ? onTimeStats.getOnTime() : 0;
        double onTimeRate = total > 0 ? (double) onTime / total * 100 : 0;

        return RunnerPerformanceVO.builder()
                .userId(runnerId)
                .totalOrders(profile.getTotalOrders())
                .successOrders(profile.getSuccessOrders())
                .completionRate(Math.round(completionRate * 10.0) / 10.0)
                .onTimeRate(Math.round(onTimeRate * 10.0) / 10.0)
                .currentOrders(profile.getCurrentOrders())
                .avgRating(profile.getAvgRating() != null ? profile.getAvgRating().doubleValue() : 5.0)
                .creditScore(profile.getCreditScore())
                .totalEarnings(totalEarnings)
                .nickname(user != null ? user.getNickname() : "")
                .avatarUrl(user != null ? user.getAvatarUrl() : "")
                .sex(user != null ? user.getSex() : "")
                .build();
    }
}
