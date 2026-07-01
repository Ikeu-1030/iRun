package com.ikeu.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ikeu.common.constant.CreditConstant;
import com.ikeu.model.entity.CreditLog;
import com.ikeu.model.entity.RunnerProfile;
import com.ikeu.model.entity.TaskOrder;
import com.ikeu.server.mapper.CreditLogMapper;
import com.ikeu.server.mapper.RunnerProfileMapper;
import com.ikeu.server.mapper.TaskOrderMapper;
import com.ikeu.server.service.CreditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 信用分服务实现，处理订单完成后的信用分自动变更、通用加减分及冻结/解冻逻辑。
 * @author ikeu
 * @since 2026/05/14
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CreditServiceImpl implements CreditService {

    private final TaskOrderMapper taskOrderMapper;
    private final RunnerProfileMapper runnerProfileMapper;
    private final CreditLogMapper creditLogMapper;

    /**
     * 根据送达时效自动清算跑腿员履约信用分，在 confirmDeliver 事务提交后触发。
     *
     * <p>以 {@code deliverTime}（跑腿员点击送达的实际时刻）vs {@code expectFinishTime}（预计送达）判定，
     * 不受发布者确认时间延迟影响。使用 REQUIRES_NEW 事务隔离避免行锁冲突。
     *
     * <p>评分规则：提前 +5，按时 +1，超时 0-30min -2，30-60min -5，60min+ -10。
     *
     * @param orderId 订单ID
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processCreditOnComplete(Long orderId) {
        TaskOrder order = taskOrderMapper.selectById(orderId);
        if (order == null || order.getExpectFinishTime() == null) return;

        // 使用送达时间清算履约信用分，不受发布者确认延迟影响
        LocalDateTime deliverTime = order.getDeliverTime() != null ? order.getDeliverTime() : LocalDateTime.now();
        Long runnerId = order.getRunnerId();

        if (deliverTime.isBefore(order.getExpectFinishTime())) {
            recordAndApply(runnerId, CreditConstant.REWARD_EARLY,
                    CreditConstant.ReasonType.REWARD, CreditConstant.ReasonDetail.EARLY, orderId);
            return;
        }

        long minutesLate = Duration.between(order.getExpectFinishTime(), deliverTime).toMinutes();
        if (minutesLate <= 0) {
            recordAndApply(runnerId, CreditConstant.REWARD_ON_TIME,
                    CreditConstant.ReasonType.REWARD, CreditConstant.ReasonDetail.ON_TIME, orderId);
        } else if (minutesLate <= 30) {
            recordAndApply(runnerId, CreditConstant.PENALTY_TIMEOUT_30, CreditConstant.ReasonType.TIMEOUT,
                    CreditConstant.ReasonDetail.TIMEOUT_PREFIX + minutesLate + "分钟", orderId);
        } else if (minutesLate <= 60) {
            recordAndApply(runnerId, CreditConstant.PENALTY_TIMEOUT_60, CreditConstant.ReasonType.TIMEOUT,
                    CreditConstant.ReasonDetail.TIMEOUT_PREFIX + minutesLate + "分钟", orderId);
        } else {
            recordAndApply(runnerId, CreditConstant.PENALTY_TIMEOUT_OVER60, CreditConstant.ReasonType.TIMEOUT,
                    CreditConstant.ReasonDetail.TIMEOUT_PREFIX + minutesLate + "分钟", orderId);
        }
    }

    /**
     * 因投诉、差评或管理员手动操作扣除跑腿员信用分，
     * 内部调用 recordAndApply 写入 CreditLog 并触发 MySQL 原子更新（含冻结/解冻）。
     *
     * @param runnerId 跑腿员用户 ID
     * @param penalty  扣分数值（正数，方法内部取负）
     * @param reason   扣除原因描述文本
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deductCredit(Long runnerId, int penalty, String reason) {
        if (runnerId == null || penalty <= 0) return;
        recordAndApply(runnerId, -penalty, CreditConstant.ReasonType.MANUAL, reason, null);
    }

    /**
     * 因好评或管理员手动操作增加跑腿员信用分，
     * 内部调用 recordAndApply 写入 CreditLog 并触发 MySQL 原子更新。
     *
     * @param runnerId 跑腿员用户 ID
     * @param bonus    加分值（正数）
     * @param reason   加分原因描述文本
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void addCredit(Long runnerId, int bonus, String reason) {
        if (runnerId == null || bonus <= 0) return;
        recordAndApply(runnerId, bonus, CreditConstant.ReasonType.REWARD, reason, null);
    }

    /**
     * 核心方法：查询当前信用分 → 写入变动日志 → 原子更新 + 冻结/解冻。
     *
     * <p>冻结/解冻逻辑已移入 SQL（updateCreditScoreAndFreeze），消除 TOCTOU 竞态。
     */
    private void recordAndApply(Long runnerId, int delta, String reasonType, String reasonDetail, Long orderId) {
        RunnerProfile profile = runnerProfileMapper.selectOne(
                new LambdaQueryWrapper<RunnerProfile>().eq(RunnerProfile::getUserId, runnerId));
        if (profile == null) {
            log.warn("跑腿员 {} 档案不存在，跳过信用分变更", runnerId);
            return;
        }

        int scoreBefore = profile.getCreditScore() != null ? profile.getCreditScore() : CreditConstant.CREDIT_INITIAL;
        int scoreAfter = Math.min(CreditConstant.CREDIT_MAX, Math.max(0, scoreBefore + delta));

        CreditLog creditLog = CreditLog.builder()
                .runnerId(runnerId)
                .delta(delta)
                .scoreBefore(scoreBefore)
                .scoreAfter(scoreAfter)
                .reasonType(reasonType)
                .reasonDetail(reasonDetail)
                .relatedOrderId(orderId)
                .build();
        creditLogMapper.insert(creditLog);

        runnerProfileMapper.updateCreditScoreAndFreeze(runnerId, delta,
                CreditConstant.CREDIT_FREEZE_THRESHOLD, CreditConstant.CREDIT_FREEZE_DAYS,
                CreditConstant.CREDIT_MAX);

        // 重查最终状态用于日志（SQL 内已原子完成冻结/解冻，此处仅为可观测性）
        RunnerProfile after = runnerProfileMapper.selectOne(
                new LambdaQueryWrapper<RunnerProfile>().eq(RunnerProfile::getUserId, runnerId));
        if (after != null) {
            if (after.getIsBanned() == 1 && after.getBanUntil() != null && after.getBanUntil().isAfter(LocalDateTime.now())) {
                log.warn("跑腿员 {} 信用分冻结（当前分 {}），解封时间 {}", runnerId, after.getCreditScore(), after.getBanUntil());
            } else if (after.getCreditScore() != null && after.getCreditScore() >= CreditConstant.CREDIT_FREEZE_THRESHOLD
                    && scoreBefore < CreditConstant.CREDIT_FREEZE_THRESHOLD) {
                log.info("跑腿员 {} 信用分解除冻结（当前分 {}）", runnerId, after.getCreditScore());
            }
        }

        log.info("跑腿员 {} 信用分变更: {} → {} ({}: {})", runnerId, scoreBefore, scoreAfter, reasonType, reasonDetail);
    }
}
