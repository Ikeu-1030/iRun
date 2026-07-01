package com.ikeu.server.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ikeu.common.constant.RedisConstant;
import com.ikeu.common.constant.StatusConstant;
import com.ikeu.model.entity.SystemConfig;
import com.ikeu.model.entity.Task;
import com.ikeu.model.entity.TaskOrder;
import com.ikeu.server.mapper.RunnerProfileMapper;
import com.ikeu.server.mapper.SystemConfigMapper;
import com.ikeu.server.mapper.TaskMapper;
import com.ikeu.server.mapper.TaskOrderMapper;
import com.ikeu.server.service.NotificationService;
import com.ikeu.server.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 订单自动完成定时任务，发布者超时未确认时自动结算并支付报酬给跑腿员。
 * 超时时间由系统配置 order.auto_confirm_hours 决定，默认 24 小时。
 * @author ikeu
 * @since 2026/05/14
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderAutoCompleteChecker {

    private final TaskOrderService taskOrderService;
    private final TaskOrderMapper orderMapper;
    private final TaskMapper taskMapper;
    private final RedissonClient redissonClient;
    private final CacheManager cacheManager;
    private final SystemConfigMapper systemConfigMapper;

    private static final String CONFIG_KEY_AUTO_CONFIRM_HOURS = "order.auto_confirm_hours";
    private static final int DEFAULT_AUTO_CONFIRM_HOURS = 24;

    /**
     * 每分钟检查超时未确认的订单，自动完成并支付跑腿员报酬。
     *
     * <p>执行流程：
     * <ol>
     *   <li>从系统配置读取 auto_confirm_hours（默认 24 小时）</li>
     *   <li>获取分布式锁 ORDER_AUTO_COMPLETE_LOCK_KEY</li>
     *   <li>查询 status=待确认 且 deliver_time ≤ 当前时间-auto_confirm_hours 的订单列表</li>
     *   <li>逐个处理：校验关联任务状态为待确认</li>
     *   <li>更新订单状态为已完成，任务状态为已完成</li>
     *   <li>原子递减跑腿员当前接单数</li>
     *   <li>调用 PaymentService 支付报酬（幂等，手动确认已支付则跳过统计更新）</li>
     *   <li>原子更新跑腿员完成单数统计</li>
     *   <li>分别通知发布者和跑腿员订单已自动完成</li>
     * </ol>
     * 单个订单处理异常仅记录日志不影响其他订单。
     */
    @Scheduled(fixedRate = 60000, initialDelay = 30000)
    public void autoCompleteOrders() {
        int autoConfirmHours = loadAutoConfirmHours();
        RLock lock = redissonClient.getLock(RedisConstant.ORDER_AUTO_COMPLETE_LOCK_KEY);
        boolean processed = false;
        try {
            if (!lock.tryLock(0, 30, TimeUnit.SECONDS)) {
                return;
            }
            LocalDateTime deadline = LocalDateTime.now().minusHours(autoConfirmHours);
            List<TaskOrder> staleOrders = orderMapper.selectList(
                    new LambdaQueryWrapper<TaskOrder>()
                            .eq(TaskOrder::getStatus, StatusConstant.ORDER_WAIT_CONFIRM)
                            .le(TaskOrder::getDeliverTime, deadline)
                            .eq(TaskOrder::getIsDeleted, 0)
            );

            for (TaskOrder order : staleOrders) {
                RLock orderLock = redissonClient.getLock(RedisConstant.ORDER_LOCK_KEY + order.getTaskId());
                try {
                    if (!orderLock.tryLock(0, 10, TimeUnit.SECONDS)) {
                        continue;
                    }
                    order = orderMapper.selectById(order.getId());
                    if (order == null || !order.getStatus().equals(StatusConstant.ORDER_WAIT_CONFIRM)) {
                        continue;
                    }
                    Task task = taskMapper.selectById(order.getTaskId());
                    if (task == null || !task.getStatus().equals(StatusConstant.TASK_WAIT_CONFIRM)) {
                        continue;
                    }

                    taskOrderService.autoCompleteOrder(order, task, autoConfirmHours);
                    processed = true;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    log.error("处理自动结算订单 {} 失败", order.getId(), e);
                } finally {
                    if (orderLock.isHeldByCurrentThread()) {
                        orderLock.unlock();
                    }
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

    /** 从系统配置表读取自动确认小时数，读取失败或未配置时使用默认值 24。 */
    private int loadAutoConfirmHours() {
        try {
            SystemConfig config = systemConfigMapper.selectOne(
                    new LambdaQueryWrapper<SystemConfig>().eq(SystemConfig::getConfigKey, CONFIG_KEY_AUTO_CONFIRM_HOURS));
            if (config != null && config.getConfigValue() != null) {
                int hours = Integer.parseInt(config.getConfigValue());
                if (hours > 0) {
                    return hours;
                }
                log.warn("配置 order.auto_confirm_hours 值 {} 无效（必须 > 0），使用默认值 {}h", hours, DEFAULT_AUTO_CONFIRM_HOURS);
            }
        } catch (Exception e) {
            log.warn("读取 order.auto_confirm_hours 配置失败，使用默认值 {}h", DEFAULT_AUTO_CONFIRM_HOURS, e);
        }
        return DEFAULT_AUTO_CONFIRM_HOURS;
    }

    private void evictCaches() {
        try {
            Objects.requireNonNull(cacheManager.getCache(RedisConstant.CACHE_DASHBOARD)).clear();
            Objects.requireNonNull(cacheManager.getCache(RedisConstant.CACHE_LEADERBOARD)).clear();
            Objects.requireNonNull(cacheManager.getCache(RedisConstant.CACHE_TASK_HALL)).clear();
            Objects.requireNonNull(cacheManager.getCache(RedisConstant.CACHE_TASK_DETAIL)).clear();
        } catch (Exception e) {
            log.error("清除缓存失败", e);
        }
    }
}
