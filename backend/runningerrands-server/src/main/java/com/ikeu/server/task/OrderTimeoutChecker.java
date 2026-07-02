package com.ikeu.server.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ikeu.common.constant.MessageConstant;
import com.ikeu.common.constant.RedisConstant;
import com.ikeu.common.constant.StatusConstant;
import com.ikeu.model.entity.Task;
import com.ikeu.model.entity.TaskOrder;
import com.ikeu.server.mapper.TaskMapper;
import com.ikeu.server.mapper.TaskOrderMapper;
import com.ikeu.server.service.NotificationService;
import com.ikeu.server.service.PaymentService;
import com.ikeu.server.service.RunnerProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 订单超时检查定时任务，待取货超时自动取消并退款，配送中超时仅发提醒。
 * @author ikeu
 * @since 2026/05/14
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutChecker {

    private final TaskOrderMapper orderMapper;
    private final TaskMapper taskMapper;
    private final PaymentService paymentService;
    private final RunnerProfileService runnerProfileService;
    private final NotificationService notificationService;
    private final RedissonClient redissonClient;
    private final StringRedisTemplate stringRedisTemplate;
    private final CacheManager cacheManager;

    /**
     * 每30秒检查超时订单，分两阶段处理。
     *
     * <p>阶段1 —— 待取货超时（自动取消并退款）：
     * <ol>
     *   <li>查询 status=待取货 且 expect_finish_time ≤ now 的订单</li>
     *   <li>更新订单状态为已取消，取消原因设为取货超时提示</li>
     *   <li>将关联任务状态回退为待接单</li>
     *   <li>调用 PaymentService 退款给发布者</li>
     *   <li>通知发布者（退款）和跑腿员（取消）</li>
     *   <li>递减跑腿员当前接单数</li>
     * </ol>
     *
     * <p>阶段2 —— 配送中超时（仅发提醒一次）：
     * <ol>
     *   <li>查询 status=配送中 且 expect_finish_time ≤ now 的订单</li>
     *   <li>检查 Redis 提醒标记 ORDER_DELAY_REMINDED，已提醒过则跳过</li>
     *   <li>设置提醒标记（24小时过期）</li>
     *   <li>分别通知发布者和跑腿员配送延迟</li>
     * </ol>
     */
    @Scheduled(fixedRate = 30000, initialDelay = 30000)
    @Transactional
    public void cancelTimeoutOrders() {
        RLock lock = redissonClient.getLock(RedisConstant.ORDER_TIMEOUT_LOCK_KEY);
        boolean processed = false;
        try {
            if (!lock.tryLock(0, 30, TimeUnit.SECONDS)) {
                return;
            }
            LocalDateTime now = LocalDateTime.now();

            List<TaskOrder> timeoutPickupOrders = orderMapper.selectList(
                    new LambdaQueryWrapper<TaskOrder>()
                            .eq(TaskOrder::getStatus, StatusConstant.ORDER_WAIT_PICKUP)
                            .le(TaskOrder::getExpectFinishTime, now)
                            .eq(TaskOrder::getIsDeleted, 0)
            );

            for (TaskOrder order : timeoutPickupOrders) {
                try {
                    // 条件UPDATE防止与confirmPickup并发：仅当订单仍为待取货状态时才更新
                    int affected = orderMapper.updateStatusIf(order.getId(),
                            StatusConstant.ORDER_WAIT_PICKUP,
                            StatusConstant.ORDER_CANCELLED,
                            MessageConstant.ORDER_PICKUP_TIMEOUT);
                    if (affected == 0) {
                        log.info("订单 {} 状态已变更，跳过超时取消", order.getId());
                        continue;
                    }

                    // 持任务锁防止与 cancelOrder/cancelTask/adminCancel 竞态覆盖任务状态
                    RLock taskLock = redissonClient.getLock(RedisConstant.ORDER_LOCK_KEY + order.getTaskId());
                    try {
                        if (!taskLock.tryLock(RedisConstant.LOCK_WAIT_TIME, RedisConstant.LOCK_EXPIRE, TimeUnit.SECONDS)) {
                            log.warn("获取任务锁失败，跳过订单 {} 超时取消", order.getId());
                            continue;
                        }
                        Task task = taskMapper.selectById(order.getTaskId());
                        if (task != null && Objects.equals(task.getStatus(), StatusConstant.TASK_ACCEPTED)) {
                            task.setStatus(StatusConstant.TASK_WAITING);
                            task.setUpdatedAt(now);
                            taskMapper.updateById(task);
                        }

                        if (task != null) {
                            paymentService.refundForTask(task.getPublisherId(), task.getId(), task.getReward());
                            notificationService.sendNotification(task.getPublisherId(),
                                    StatusConstant.NOTICE_SYSTEM, "订单超时取消",
                                    "您的任务 " + task.getTaskNo() + " 因配送员未按时取货自动取消，已退款",
                                    task.getId());
                        }
                    } finally {
                        if (taskLock.isHeldByCurrentThread()) {
                            taskLock.unlock();
                        }
                    }

                    notificationService.sendNotification(order.getRunnerId(),
                            StatusConstant.NOTICE_SYSTEM, "订单超时取消",
                            "您接取的订单因未按时取货已被自动取消",
                            order.getId());

                    runnerProfileService.decrementCurrentOrders(order.getRunnerId());
                    log.info("订单 {} 超时未取货，已自动取消并退款", order.getId());
                    processed = true;
                } catch (Exception e) {
                    log.error("处理超时未取货订单 {} 失败", order.getId(), e);
                }
            }

            List<TaskOrder> timeoutDeliveringOrders = orderMapper.selectList(
                    new LambdaQueryWrapper<TaskOrder>()
                            .eq(TaskOrder::getStatus, StatusConstant.ORDER_DELIVERING)
                            .le(TaskOrder::getExpectFinishTime, now)
                            .eq(TaskOrder::getIsDeleted, 0)
            );

            for (TaskOrder order : timeoutDeliveringOrders) {
                try {
                    String remindedKey = RedisConstant.ORDER_DELAY_REMINDED + order.getId();
                    if (stringRedisTemplate.hasKey(remindedKey)) {
                        continue;
                    }
                    stringRedisTemplate.opsForValue().set(remindedKey, "1", 24, TimeUnit.HOURS);

                    Task task = taskMapper.selectById(order.getTaskId());
                    if (task != null) {
                        notificationService.sendNotification(task.getPublisherId(),
                                StatusConstant.NOTICE_SYSTEM, "配送延迟提醒",
                                "您的任务 " + task.getTaskNo() + " 配送已超时，请关注配送进度或联系跑腿员",
                                task.getId());
                    }
                    notificationService.sendNotification(order.getRunnerId(),
                            StatusConstant.NOTICE_SYSTEM, "配送延迟提醒",
                            "您有配送订单已超时，请尽快完成配送",
                            order.getId());

                    log.info("订单 {} 配送超时（expect_finish_time={}），已发送提醒", order.getId(), order.getExpectFinishTime());
                } catch (Exception e) {
                    log.error("处理配送超时订单 {} 提醒失败", order.getId(), e);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
        if (processed) evictCaches();
    }

    private void evictCaches() {
        try {
            Objects.requireNonNull(cacheManager.getCache(RedisConstant.CACHE_TASK_HALL)).clear();
            Objects.requireNonNull(cacheManager.getCache(RedisConstant.CACHE_TASK_DETAIL)).clear();
            Objects.requireNonNull(cacheManager.getCache(RedisConstant.CACHE_DASHBOARD)).clear();
        } catch (Exception e) {
            log.error("清除缓存失败", e);
        }
    }
}
