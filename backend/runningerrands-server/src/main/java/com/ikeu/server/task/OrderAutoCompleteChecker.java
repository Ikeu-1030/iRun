package com.ikeu.server.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ikeu.common.constant.RedisConstant;
import com.ikeu.common.constant.StatusConstant;
import com.ikeu.model.entity.Task;
import com.ikeu.model.entity.TaskOrder;
import com.ikeu.server.mapper.RunnerProfileMapper;
import com.ikeu.server.mapper.TaskMapper;
import com.ikeu.server.mapper.TaskOrderMapper;
import com.ikeu.server.service.NotificationService;
import com.ikeu.server.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 订单自动完成定时任务，发布者24小时未确认时自动结算并支付报酬给跑腿员。
 * @author ikeu
 * @since 2026/05/14
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderAutoCompleteChecker {

    private final TaskOrderMapper orderMapper;
    private final TaskMapper taskMapper;
    private final RunnerProfileMapper runnerProfileMapper;
    private final PaymentService paymentService;
    private final NotificationService notificationService;
    private final RedissonClient redissonClient;

    /**
     * 每分钟检查超时未确认的订单，自动完成并支付跑腿员报酬。
     *
     * <p>执行流程：
     * <ol>
     *   <li>获取分布式锁 ORDER_AUTO_COMPLETE_LOCK_KEY</li>
     *   <li>查询 status=待确认 且 deliver_time ≤ 当前时间-24小时 的订单列表</li>
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
    @Transactional
    public void autoCompleteOrders() {
        RLock lock = redissonClient.getLock(RedisConstant.ORDER_AUTO_COMPLETE_LOCK_KEY);
        try {
            if (!lock.tryLock(0, 30, TimeUnit.SECONDS)) {
                return;
            }
            LocalDateTime deadline = LocalDateTime.now().minusHours(24);
            List<TaskOrder> staleOrders = orderMapper.selectList(
                    new LambdaQueryWrapper<TaskOrder>()
                            .eq(TaskOrder::getStatus, StatusConstant.ORDER_WAIT_CONFIRM)
                            .le(TaskOrder::getDeliverTime, deadline)
                            .eq(TaskOrder::getIsDeleted, 0)
            );

            for (TaskOrder order : staleOrders) {
                // 逐订单加锁，防止与手动 confirmComplete 并发
                RLock orderLock = redissonClient.getLock(RedisConstant.ORDER_LOCK_KEY + order.getTaskId());
                try {
                    if (!orderLock.tryLock(0, 10, TimeUnit.SECONDS)) {
                        continue;
                    }
                    // 锁内重查订单状态
                    order = orderMapper.selectById(order.getId());
                    if (order == null || !order.getStatus().equals(StatusConstant.ORDER_WAIT_CONFIRM)) {
                        continue;
                    }
                    Task task = taskMapper.selectById(order.getTaskId());
                    if (task == null || !task.getStatus().equals(StatusConstant.TASK_WAIT_CONFIRM)) {
                        continue;
                    }

                    LocalDateTime now = LocalDateTime.now();
                    order.setConfirmTime(now);
                    order.setStatus(StatusConstant.ORDER_COMPLETED);
                    orderMapper.updateById(order);

                    task.setStatus(StatusConstant.TASK_COMPLETED);
                    task.setUpdatedAt(now);
                    taskMapper.updateById(task);

                    runnerProfileMapper.decrementCurrentOrders(order.getRunnerId());

                    if (paymentService.payToRunner(order.getRunnerId(), order.getTaskId(), task.getReward())) {
                        runnerProfileMapper.incrementCompletedStats(order.getRunnerId());
                    }

                    notificationService.sendNotification(task.getPublisherId(),
                            StatusConstant.NOTICE_ORDER, "订单已自动确认",
                            "您的任务 " + task.getTaskNo() + " 已超过24小时自动确认完成", order.getId());
                    notificationService.sendNotification(order.getRunnerId(),
                            StatusConstant.NOTICE_ORDER, "订单已自动完成",
                            "您配送的任务 " + task.getTaskNo() + " 已自动确认完成", order.getId());

                    log.info("订单 {} 24h自动结算完成，报酬 {} 支付给跑腿员 {}", order.getId(), task.getReward(), order.getRunnerId());
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
    }
}
