package com.ikeu.server.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ikeu.common.constant.MessageConstant;
import com.ikeu.common.constant.RedisConstant;
import com.ikeu.common.constant.StatusConstant;
import com.ikeu.common.enums.TaskStateMachine;
import com.ikeu.common.exception.BusinessException;
import com.ikeu.common.exception.NotFoundException;
import com.ikeu.common.result.PageResult;
import com.ikeu.model.entity.Task;
import com.ikeu.model.entity.TaskOrder;
import com.ikeu.model.entity.User;
import com.ikeu.model.vo.TaskDetailVO;
import com.ikeu.model.vo.TaskListVO;
import com.ikeu.server.mapper.TaskMapper;
import com.ikeu.server.mapper.TaskOrderMapper;
import com.ikeu.server.mapper.UserMapper;
import com.ikeu.server.service.AdminTaskService;
import com.ikeu.server.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 管理端任务管理服务实现，处理任务列表、详情和强制状态更新。
 * @author ikeu
 * @since 2026/06/11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminTaskServiceImpl implements AdminTaskService {

    private final TaskMapper taskMapper;
    private final TaskOrderMapper taskOrderMapper;
    private final UserMapper userMapper;
    private final CacheManager cacheManager;
    private final StringRedisTemplate stringRedisTemplate;
    private final RedissonClient redissonClient;
    private final PaymentService paymentService;

    /**
     * 分页查询所有任务，关联发布者信息填充昵称/头像，支持按状态筛选。
     *
     * <p>按创建时间降序排列。如果按状态筛选，自动拼接 {@code eq(status)} 条件。
     * 查出任务列表后，根据 publisherId 批量查询用户信息填充发布者昵称、用户名和头像。
     *
     * @param status 任务状态筛选（可为 null 表示查询全部）
     * @param page 页码，从 1 开始
     * @param size 每页条数
     * @return 分页结果，每项包含任务信息和发布者信息
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
        Map<Long, User> publisherMap = publishers.stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        List<TaskListVO> records = p.getRecords().stream().map(task -> {
            TaskListVO vo = BeanUtil.copyProperties(task, TaskListVO.class, "imageUrls", "publishTime");
            vo.setTaskId(task.getId());
            vo.setPublishTime(task.getCreatedAt());
            User publisher = publisherMap.get(task.getPublisherId());
            vo.setPublisherNickname(publisher != null ? publisher.getNickname() : "");
            vo.setPublisherUsername(publisher != null ? publisher.getUsername() : "");
            vo.setPublisherAvatar(publisher != null ? publisher.getAvatarUrl() : "");
            vo.setImageUrls(task.getImageUrls() != null
                    ? JSONUtil.toList(task.getImageUrls(), String.class) : Collections.emptyList());
            return vo;
        }).collect(Collectors.toList());

        return new PageResult<>(p.getTotal(), records);
    }

    /**
     * 查询任务详情（管理端，不脱敏），含发布者信息和图片列表。
     *
     * <p>管理端视角的详情查询，不脱敏展示发布者手机号等敏感信息。
     * 如果任务已取消（status = CANCELLED），自动将 {@code updatedAt} 设为取消时间；
     * 取证码仅返回是否存在（{@code hasPickupCode} 布尔值），不返回原文。
     *
     * @param taskId 任务 ID
     * @return 任务详情 VO，含发布者信息、图片 URL 列表和取证码标记
     * @throws NotFoundException 任务不存在时抛出
     */
    @Override
    public TaskDetailVO getTaskDetail(Long taskId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) throw new NotFoundException(MessageConstant.TASK_NOT_EXIST);
        User publisher = userMapper.selectById(task.getPublisherId());

        TaskDetailVO vo = BeanUtil.copyProperties(task, TaskDetailVO.class, "imageUrls");
        vo.setTaskId(task.getId());
        vo.setPublishTime(task.getCreatedAt());
        if (StatusConstant.TASK_CANCELLED.equals(task.getStatus())) {
            vo.setCancelTime(task.getUpdatedAt());
        }
        vo.setImageUrls(task.getImageUrls() != null
                ? JSONUtil.toList(task.getImageUrls(), String.class) : Collections.emptyList());
        vo.setHasPickupCode(task.getPickupCode() != null && !task.getPickupCode().isBlank());
        if (publisher != null) {
            vo.setPublisherNickname(publisher.getNickname());
            vo.setPublisherUsername(publisher.getUsername());
            vo.setPublisherAvatar(publisher.getAvatarUrl());
        }
        return vo;
    }

    /**
     * 管理员强制更新任务状态，分布式锁保护 + 状态机校验防并发覆盖。
     *
     * <p>使用 {@code ORDER_LOCK_KEY + taskId} 加锁，与跑腿员接单/取件/送达/完成操作
     * 及定时任务共用同一把锁，保证管理员操作与其他流程互斥。
     * 锁内重新查询任务实体以消除 TOCTOU 竞态，再调用
     * {@link TaskStateMachine#validate} 校验状态转换合法性。
     * 取消时自动同步取消关联订单（含退款），防止孤儿订单。
     * 状态更新后清除仪表盘、任务大厅列表（含空结果缓存）、任务详情缓存。
     *
     * @param taskId 任务 ID
     * @param status 目标任务状态（见 StatusConstant）
     * @throws NotFoundException 任务不存在时抛出
     * @throws BusinessException 状态转换不合法或系统繁忙时抛出
     */
    @Override
    @Transactional
    public void updateTaskStatus(Long taskId, Integer status) {
        // 使用 ORDER_LOCK_KEY，与用户端及定时任务共用同一把锁
        RLock lock = redissonClient.getLock(RedisConstant.ORDER_LOCK_KEY + taskId);
        try {
            if (!lock.tryLock(RedisConstant.LOCK_WAIT_TIME, RedisConstant.LOCK_EXPIRE, TimeUnit.SECONDS)) {
                throw new BusinessException(MessageConstant.SYSTEM_BUSY);
            }
            // 锁内重查，消除 TOCTOU
            Task task = taskMapper.selectById(taskId);
            if (task == null) throw new NotFoundException(MessageConstant.TASK_NOT_EXIST);
            Integer oldStatus = task.getStatus();
            TaskStateMachine.validate(oldStatus, status, "任务");
            task.setStatus(status);
            task.setUpdatedAt(LocalDateTime.now());
            taskMapper.updateById(task);
            log.info("管理员强制更新任务 {} 状态为 {} → {}", taskId, oldStatus, status);

            // 取消时同步取消关联订单，防止孤儿订单
            if (StatusConstant.TASK_CANCELLED.equals(status)) {
                LambdaQueryWrapper<TaskOrder> wrapper = new LambdaQueryWrapper<TaskOrder>()
                        .eq(TaskOrder::getTaskId, taskId)
                        .ne(TaskOrder::getStatus, StatusConstant.ORDER_CANCELLED);
                TaskOrder order = taskOrderMapper.selectOne(wrapper);
                if (order != null) {
                    order.setStatus(StatusConstant.ORDER_CANCELLED);
                    order.setCancelTime(LocalDateTime.now());
                    taskOrderMapper.updateById(order);
                    paymentService.refundForTask(task.getPublisherId(), taskId, task.getReward());
                    log.info("同步取消关联订单 {} 并退款 (任务 {})", order.getId(), taskId);
                }
            }

            // 完成时结算跑腿报酬（与 AdminOrderService 行为对齐）
            if (StatusConstant.TASK_COMPLETED.equals(status)) {
                LambdaQueryWrapper<TaskOrder> wrapper = new LambdaQueryWrapper<TaskOrder>()
                        .eq(TaskOrder::getTaskId, taskId)
                        .ne(TaskOrder::getStatus, StatusConstant.ORDER_CANCELLED);
                TaskOrder order = taskOrderMapper.selectOne(wrapper);
                if (order != null) {
                    paymentService.payToRunner(order.getRunnerId(), taskId, task.getReward());
                    log.info("管理员完成任务 {}，结算跑腿报酬 #{}", taskId, order.getRunnerId());
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(MessageConstant.ERROR);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }

        // 锁释放后清除缓存 — 与 @CacheEvict 语义一致（事务提交后清除）
        clearCache(RedisConstant.CACHE_DASHBOARD);
        clearCache(RedisConstant.CACHE_TASK_HALL);
        clearCache(RedisConstant.CACHE_TASK_DETAIL);
        clearCache(RedisConstant.CACHE_LEADERBOARD);
        var nullKeys = stringRedisTemplate.keys(RedisConstant.TASK_HALL_NULL_PREFIX + "*");
        if (nullKeys != null && !nullKeys.isEmpty()) stringRedisTemplate.delete(nullKeys);
    }

    /** 安全清除缓存，缓存不存在时静默跳过而非 NPE */
    private void clearCache(String name) {
        Cache cache = cacheManager.getCache(name);
        if (cache != null) cache.clear();
    }
}
