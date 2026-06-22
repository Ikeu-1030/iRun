package com.ikeu.server.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ikeu.common.constant.MessageConstant;
import com.ikeu.common.constant.RedisConstant;
import com.ikeu.common.constant.StatusConstant;
import com.ikeu.common.context.BaseContext;
import com.ikeu.common.enums.OrderStateMachine;
import com.ikeu.common.enums.TaskStateMachine;
import com.ikeu.common.exception.BusinessException;
import com.ikeu.common.exception.NotFoundException;
import com.ikeu.common.result.PageResult;
import com.ikeu.common.utils.PiiMaskUtil;
import com.ikeu.model.entity.Task;
import com.ikeu.model.entity.TaskOrder;
import com.ikeu.model.entity.User;
import com.ikeu.model.vo.OrderDetailVO;
import com.ikeu.model.vo.OrderManageVO;
import com.ikeu.server.mapper.RunnerProfileMapper;
import com.ikeu.server.mapper.TaskMapper;
import com.ikeu.server.mapper.TaskOrderMapper;
import com.ikeu.server.mapper.UserMapper;
import com.ikeu.server.service.AdminOrderService;
import com.ikeu.server.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 管理端订单管理服务实现，处理订单列表、详情和强制状态更新。
 * @author ikeu
 * @since 2026/06/11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminOrderServiceImpl implements AdminOrderService {

    private final TaskOrderMapper taskOrderMapper;
    private final TaskMapper taskMapper;
    private final UserMapper userMapper;
    private final PaymentService paymentService;
    private final RunnerProfileMapper runnerProfileMapper;
    private final RedissonClient redissonClient;
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 分页查询所有订单，关联任务表和用户表批量填充发布者/跑腿员信息，支持按状态筛选。
     *
     * @param status 订单状态（可选，null 表示全部）
     * @param page   页码
     * @param size   每页条数
     * @return 分页订单列表
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
     * 查询订单详情（管理端），内含完整的任务信息、发布者和跑腿员信息、凭证图片等。
     * 管理端可查看全部字段，不受手机号脱敏限制。
     *
     * @param orderId 订单 ID
     * @return 完整订单详情 VO
     */
    @Override
    public OrderDetailVO getOrderDetail(Long orderId) {
        TaskOrder order = taskOrderMapper.selectById(orderId);
        if (order == null) throw new NotFoundException(MessageConstant.ORDER_NOT_EXIST);

        Task task = taskMapper.selectById(order.getTaskId());
        if (task == null) throw new NotFoundException(MessageConstant.TASK_NOT_EXIST);

        User publisher = userMapper.selectById(task.getPublisherId());
        User runner = userMapper.selectById(order.getRunnerId());

        OrderDetailVO vo = BeanUtil.copyProperties(order, OrderDetailVO.class,
                "pickupProofImgs", "deliverProofImgs", "imageUrls");
        BeanUtil.copyProperties(task, vo, "imageUrls");

        vo.setOrderId(order.getId());
        vo.setOrderStatus(order.getStatus());
        vo.setTaskId(task.getId());

        vo.setImageUrls(task.getImageUrls() != null
                ? JSONUtil.toList(task.getImageUrls(), String.class) : Collections.emptyList());
        vo.setPickupProofImgs(order.getPickupProofImgs() != null
                ? JSONUtil.toList(order.getPickupProofImgs(), String.class) : Collections.emptyList());
        vo.setDeliverProofImgs(order.getDeliverProofImgs() != null
                ? JSONUtil.toList(order.getDeliverProofImgs(), String.class) : Collections.emptyList());
        vo.setCancelTime(order.getStatus().equals(StatusConstant.ORDER_CANCELLED) ? order.getCancelTime() : null);

        if (publisher != null) {
            vo.setPublisherPhone(publisher.getPhone());
            vo.setPublisherAvatar(publisher.getAvatarUrl());
            vo.setPublisherNickname(publisher.getNickname());
            vo.setPublisherUsername(publisher.getUsername());
        }
        if (runner != null) {
            vo.setRunnerPhone(runner.getPhone());
            vo.setRunnerAvatar(runner.getAvatarUrl());
            vo.setRunnerNickname(runner.getNickname());
            vo.setRunnerUsername(runner.getUsername());
        }

        if (!Integer.valueOf(1).equals(BaseContext.getCurrentRole())) {
            vo.setPublisherPhone(PiiMaskUtil.maskPhone(vo.getPublisherPhone()));
            vo.setRunnerPhone(PiiMaskUtil.maskPhone(vo.getRunnerPhone()));
        }

        return vo;
    }

    /**
     * 强制更新订单状态（经 OrderStateMachine 校验），分布式锁保护防并发覆盖。
     * 同步更新关联任务状态（经 TaskStateMachine 校验），
     * 终态（已完成/已取消）自动触发资金结算或退款（含幂等保护），
     * 完成后清除仪表盘、任务大厅缓存（含空结果标记）。
     *
     * @param orderId 订单 ID
     * @param status  目标状态码（见 StatusConstant）
     */
    @Override
    @Transactional
    @CacheEvict(value = {RedisConstant.CACHE_DASHBOARD, RedisConstant.CACHE_TASK_HALL, RedisConstant.CACHE_LEADERBOARD, RedisConstant.CACHE_TASK_DETAIL}, allEntries = true)
    public void updateOrderStatus(Long orderId, Integer status) {
        // 先查订单获取 taskId，再以 taskId 加锁，与用户端及定时任务共用同一把锁
        TaskOrder order = taskOrderMapper.selectById(orderId);
        if (order == null) throw new NotFoundException(MessageConstant.ORDER_NOT_EXIST);
        Long taskId = order.getTaskId();

        RLock lock = redissonClient.getLock(RedisConstant.ORDER_LOCK_KEY + taskId);
        try {
            if (!lock.tryLock(RedisConstant.LOCK_WAIT_TIME, RedisConstant.LOCK_EXPIRE, TimeUnit.SECONDS)) {
                throw new BusinessException(MessageConstant.SYSTEM_BUSY);
            }
            // 锁内重查，消除 TOCTOU
            order = taskOrderMapper.selectById(orderId);
            if (order == null) throw new NotFoundException(MessageConstant.ORDER_NOT_EXIST);
            Task task = taskMapper.selectById(taskId);

            OrderStateMachine.validate(order.getStatus(), status, "订单");

            LocalDateTime now = LocalDateTime.now();
            order.setStatus(status);
            if (status.equals(StatusConstant.ORDER_COMPLETED)) order.setConfirmTime(now);
            if (status.equals(StatusConstant.ORDER_CANCELLED)) order.setCancelTime(now);
            taskOrderMapper.updateById(order);

            if (task != null) {
                Integer taskStatus = mapOrderStatusToTaskStatus(status);
                if (taskStatus != null) {
                    // 经 TaskStateMachine 校验后再更新，防止映射表变更导致非法转换
                    TaskStateMachine.validate(task.getStatus(), taskStatus, "任务");
                    task.setStatus(taskStatus);
                    task.setUpdatedAt(now);
                    taskMapper.updateById(task);
                }

                // 终态触发资金结算，paymentService 内已有幂等保护
                if (status.equals(StatusConstant.ORDER_COMPLETED)) {
                    if (paymentService.payToRunner(order.getRunnerId(), taskId, task.getReward())) {
                        runnerProfileMapper.incrementCompletedStats(order.getRunnerId());
                    }
                } else if (status.equals(StatusConstant.ORDER_CANCELLED)) {
                    paymentService.refundForTask(task.getPublisherId(), task.getId(), task.getReward());
                }
            }

            log.info("管理员强制更新订单 {} 状态为 {} → {}", orderId, order.getStatus(), status);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(MessageConstant.ERROR);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }

        // 清除空结果缓存，防止因状态变更产生的新数据被旧空结果屏蔽
        var nullKeys = stringRedisTemplate.keys(RedisConstant.TASK_HALL_NULL_PREFIX + "*");
        if (nullKeys != null && !nullKeys.isEmpty()) stringRedisTemplate.delete(nullKeys);
    }

    /** 订单状态 → 任务状态映射表，保证 admin 强制更新时任务表同步。 */
    private Integer mapOrderStatusToTaskStatus(Integer orderStatus) {
        if (orderStatus.equals(StatusConstant.ORDER_WAIT_PICKUP)) return StatusConstant.TASK_ACCEPTED;
        if (orderStatus.equals(StatusConstant.ORDER_DELIVERING)) return StatusConstant.TASK_DELIVERING;
        if (orderStatus.equals(StatusConstant.ORDER_WAIT_CONFIRM)) return StatusConstant.TASK_WAIT_CONFIRM;
        if (orderStatus.equals(StatusConstant.ORDER_COMPLETED)) return StatusConstant.TASK_COMPLETED;
        if (orderStatus.equals(StatusConstant.ORDER_CANCELLED)) return StatusConstant.TASK_CANCELLED;
        log.warn("无法识别的订单状态 {}，跳过任务表同步", orderStatus);
        return null;
    }
}
