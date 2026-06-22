package com.ikeu.server.service.impl;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ikeu.common.constant.CreditConstant;
import com.ikeu.common.constant.MessageConstant;
import com.ikeu.common.constant.RedisConstant;
import com.ikeu.common.constant.StatusConstant;
import com.ikeu.common.exception.NotFoundException;
import com.ikeu.common.exception.ForbiddenException;
import com.ikeu.common.enums.OrderStateMachine;
import com.ikeu.common.enums.TaskStateMachine;
import com.ikeu.common.exception.BusinessException;
import com.ikeu.common.result.PageResult;
import com.ikeu.model.dto.CancelOrderDTO;
import com.ikeu.model.dto.ProofImageDTO;
import com.ikeu.model.entity.*;
import com.ikeu.model.vo.OrderDetailVO;
import com.ikeu.model.vo.OrderListVO;
import com.ikeu.server.annotation.RedisDefend;
import com.ikeu.server.annotation.SendNotification;
import com.ikeu.server.mapper.*;
import com.ikeu.server.service.PaymentService;
import com.ikeu.server.service.TaskOrderService;
import com.ikeu.server.util.RedisDefendUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


/**
 * 订单服务实现，处理订单的接单、取件、送达、完成等全生命周期管理。
 * @author ikeu
 * @since 2026/05/18
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskOrderServiceImpl extends ServiceImpl<TaskOrderMapper, TaskOrder> implements TaskOrderService {

    private final TaskOrderMapper taskOrderMapper;
    private final TaskMapper taskMapper;
    private final UserMapper userMapper;
    private final RunnerProfileMapper runnerProfileMapper;
    private final ReviewMapper reviewMapper;
    private final PaymentService paymentService;
    private final RedissonClient redissonClient;
    private final CacheManager cacheManager;
    private final StringRedisTemplate stringRedisTemplate;
    private final RedisDefendUtil redisDefendUtil;

    //TODO 预计配送时间（分钟），可根据取件点和送达点估算，这里简化固定为30分钟
    private static final int ESTIMATED_DELIVERY_MINUTES = 30;

    /**
     * 将任务订单数据转换成订单列表VO
     *
     * @param order 订单实体
     * @param taskMap 任务Map映射
     * @param publisherMap 发布者Map映射
     * @return OrderListVO 订单列表VO
     */
    private OrderListVO convertToOrderListVO(TaskOrder order, Map<Long, Task> taskMap,
                                             Map<Long, User> publisherMap, java.util.Set<Long> reviewedTaskIds) {
        OrderListVO vo = BeanUtil.copyProperties(order, OrderListVO.class);
        vo.setOrderId(order.getId());
        vo.setOrderStatus(order.getStatus());

        Task task = taskMap.get(order.getTaskId());
        if (task != null) {
            vo.setType(task.getType());
            vo.setSubType(task.getSubType());
            vo.setTaskSpecs(task.getTaskSpecs());
            vo.setPublicDesc(task.getPublicDesc() != null ? task.getPublicDesc() : "");
            vo.setReward(task.getReward());
            vo.setPickupAddress(task.getPickupAddress());
            vo.setDeliveryAddress(task.getDeliveryAddress());
            vo.setContactName(task.getContactName() != null ? task.getContactName() : "");
            vo.setContactPhone(task.getContactPhone() != null ? task.getContactPhone() : "");

            User publisher = publisherMap.get(task.getPublisherId());
            vo.setPublisherNickname(publisher != null ? publisher.getNickname() : "");
        } else {
            vo.setPublicDesc("");
            vo.setReward(BigDecimal.ZERO);
            vo.setPublisherNickname("");
        }

        vo.setHasReviewed(reviewedTaskIds != null && reviewedTaskIds.contains(order.getTaskId()));

        return vo;
    }


    /**
     * 配送员接取订单。
     *
     * <p>前置校验（持锁前）：
     * <ol>
     *   <li>任务存在且状态为待接单</li>
     *   <li>任务未过期</li>
     *   <li>配送员不是任务发布者</li>
     *   <li>任务性别限制与配送员性别匹配（“不限”跳过）</li>
     *   <li>配送员已认证、在线、未达接单上限、信用分 ≥ 60、未被封禁</li>
     * </ol>
     *
     * <p>持锁后二次校验（防并发抢单）：
     * <ol>
     *   <li>任务状态仍为待接单、未过期</li>
     *   <li>该任务不存在未取消的订单</li>
     * </ol>
     *
     * <p>业务执行：创建订单（状态=待取货，预计完成时间=当前+30min），
     * 更新任务状态为已接单，原子增加配送员当前接单数，清除任务大厅和详情缓存。
     *
     * <p>通过 {@code @RedisDefend(Type.LOCK)} 声明分布式锁，
     * {@code @SendNotification} 声明接单成功后推送通知给发布者。
     *
     * @param runnerId 配送员ID
     * @param taskId 任务ID
     * @throws BusinessException 任务不存在/已过期/已接单/性别不匹配/配送员未认证/离线/订单满/信用低/被封禁/系统繁忙
     */
    @Override
    @Transactional
    @RedisDefend(RedisDefend.Type.LOCK)
    @SendNotification(
            targetUserType = 1, // 发布者
            noticeType = StatusConstant.NOTICE_ORDER,
            title = "订单已被接取",
            content = "您的任务 #taskNo 已被接单"
    )
    public void acceptOrder(Long runnerId, Long taskId) {
        // 1. 获取任务，校验任务状态
        Task task = taskMapper.selectById(taskId);
        if (task == null) throw new BusinessException(MessageConstant.TASK_NOT_EXIST);
        if (!Objects.equals(task.getStatus(), StatusConstant.TASK_WAITING)) {
            throw new BusinessException(MessageConstant.ORDER_ACCEPTED_FAIL);
        }
        if (task.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException(MessageConstant.TASK_EXPIRED);
        }
        if (task.getPublisherId().equals(runnerId)) {
            throw new BusinessException(MessageConstant.TASK_CANNOT_ACCEPT_SELF);
        }
        if (task.getRequireSex() != null && !task.getRequireSex().isEmpty()
                && !"不限".equals(task.getRequireSex())) {
            User runnerUser = userMapper.selectById(runnerId);
            if (runnerUser == null || !task.getRequireSex().equals(runnerUser.getSex())) {
                throw new BusinessException(MessageConstant.GENDER_RESTRICTION_NOT_MATCH);
            }
        }

        // 2. 校验配送员状态
        RunnerProfile runner = runnerProfileMapper.selectOne(
                new LambdaQueryWrapper<RunnerProfile>().eq(RunnerProfile::getUserId, runnerId));
        if (runner == null || !Objects.equals(runner.getVerifyStatus(), StatusConstant.CERTIFY_APPROVED)) {
            throw new BusinessException(MessageConstant.RUNNER_NOT_CERTIFIED);
        }
        if (!Objects.equals(runner.getIsOnline(), StatusConstant.RUNNER_ONLINE)) {
            throw new BusinessException(MessageConstant.RUNNER_OFFLINE);
        }
        if (runner.getCurrentOrders() >= runner.getMaxConcurrentOrders()) {
            throw new BusinessException(MessageConstant.RUNNER_MAX_ORDERS);
        }
        if (runner.getCreditScore() != null && runner.getCreditScore() < CreditConstant.CREDIT_FREEZE_THRESHOLD) {
            throw new BusinessException(MessageConstant.RUNNER_LOW_CREDIT);
        }
        if (Objects.equals(runner.getIsBanned(), 1)) {
            throw new BusinessException(MessageConstant.RUNNER_IS_BANNED);
        }

        // 3. 使用 Redis 分布式锁防止抢单冲突
        String lockKey = RedisConstant.ORDER_LOCK_KEY + taskId;
        RLock lock = redissonClient.getLock(lockKey);
        try {
            if (!lock.tryLock(RedisConstant.LOCK_WAIT_TIME, RedisConstant.LOCK_EXPIRE, TimeUnit.SECONDS)) {
                throw new BusinessException(MessageConstant.SYSTEM_BUSY);
            }
            // 二次校验任务状态与过期时间
            Task latestTask = taskMapper.selectById(taskId);
            if (latestTask == null || !Objects.equals(latestTask.getStatus(), StatusConstant.TASK_WAITING)) {
                throw new BusinessException(MessageConstant.ORDER_STATUS_CHANGED);
            }
            if (latestTask.getExpireTime().isBefore(LocalDateTime.now())) {
                throw new BusinessException(MessageConstant.TASK_EXPIRED);
            }
            // 二次校验已存在订单（排除已取消的，允许超时取消后重新接单）
            TaskOrder existingOrder = taskOrderMapper.selectOne(
                    new LambdaQueryWrapper<TaskOrder>()
                            .eq(TaskOrder::getTaskId, taskId)
                            .eq(TaskOrder::getIsDeleted, StatusConstant.ORDER_NOT_DELETED)
                            .ne(TaskOrder::getStatus, StatusConstant.ORDER_CANCELLED));
            if (existingOrder != null) {
                throw new BusinessException(MessageConstant.ORDER_ACCEPTED_FAIL);
            }

            // 4. 创建订单
            TaskOrder order = TaskOrder.builder()
                    .taskId(taskId)
                    .runnerId(runnerId)
                    .status(StatusConstant.ORDER_WAIT_PICKUP)
                    .acceptTime(LocalDateTime.now())
                    .expectFinishTime(LocalDateTime.now().plusMinutes(ESTIMATED_DELIVERY_MINUTES))
                    .isDeleted(0)
                    .build();
            taskOrderMapper.insert(order);

            // 5. 状态校验并更新任务为"已接单"
            TaskStateMachine.validate(latestTask.getStatus(), StatusConstant.TASK_ACCEPTED, "Task");
            latestTask.setStatus(StatusConstant.TASK_ACCEPTED);
            latestTask.setUpdatedAt(LocalDateTime.now());
            taskMapper.updateById(latestTask);

            // 6. 原子更新跑腿员当前接单数 +1
            runnerProfileMapper.incrementCurrentOrders(runnerId);

            log.info("跑腿员 {} 接单成功，订单 ID: {}", runnerId, order.getId());

            // 清除任务相关缓存：任务大厅 & 任务详情
            Objects.requireNonNull(cacheManager.getCache(RedisConstant.CACHE_TASK_HALL)).clear();
            Objects.requireNonNull(cacheManager.getCache(RedisConstant.CACHE_TASK_DETAIL)).clear();
            var nullKeys = stringRedisTemplate.keys(RedisConstant.TASK_HALL_NULL_PREFIX + "*");
            if (nullKeys != null && !nullKeys.isEmpty()) stringRedisTemplate.delete(nullKeys);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(MessageConstant.ERROR);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 配送员接单后5分钟内取消订单，关联任务回退至待接单状态。
     *
     * <p>校验和实现逻辑：
     * <ol>
     *   <li>查询订单，校验存在且未删除</li>
     *   <li>校验当前用户为订单的配送员</li>
     *   <li>校验订单状态为"待取货"（仅未取货的订单可取消）</li>
     *   <li>校验接单时间距当前不超过5分钟，超时抛出 ORDER_CANCEL_TIMEOUT_RUNNER</li>
     *   <li>更新订单状态为已取消，记录取消原因</li>
     *   <li>将关联任务状态回退至"待接单"，允许其他配送员重新接单</li>
     *   <li>原子减少配送员当前接单数</li>
     *   <li>清除任务大厅和任务详情缓存</li>
     * </ol>
     *
     * @param runnerId 配送员ID
     * @param orderId 订单ID
     * @param dto 取消订单DTO（包含取消原因）
     */
    @Override
    @Transactional
    @RedisDefend(RedisDefend.Type.LOCK)
    public void cancelOrder(Long runnerId, Long orderId, CancelOrderDTO dto) {
        // 1. 校验订单存在且未删除
        TaskOrder order = getById(orderId);
        if (order == null || Objects.equals(order.getIsDeleted(), StatusConstant.ORDER_DELETED)) {
            throw new NotFoundException(MessageConstant.ORDER_NOT_EXIST);
        }
        // 2. 校验订单归属
        if (!order.getRunnerId().equals(runnerId)) {
            throw new ForbiddenException(MessageConstant.OPERATION_NOT_ALLOWED);
        }
        // 3. 校验订单状态为"待取货"
        if (!Objects.equals(order.getStatus(), StatusConstant.ORDER_WAIT_PICKUP)) {
            throw new BusinessException(MessageConstant.ORDER_CANCEL_NOT_ALLOWED);
        }
        // 4. 校验接单后5分钟内
        if (order.getAcceptTime() == null
                || Duration.between(order.getAcceptTime(), LocalDateTime.now()).toMinutes() > 5) {
            throw new BusinessException(MessageConstant.ORDER_CANCEL_TIMEOUT_RUNNER);
        }

        // 分布式锁防止与 OrderTimeoutChecker 待取货超时取消并发
        String lockKey = RedisConstant.ORDER_LOCK_KEY + order.getTaskId();
        RLock lock = redissonClient.getLock(lockKey);
        try {
            if (!lock.tryLock(RedisConstant.LOCK_WAIT_TIME, RedisConstant.LOCK_EXPIRE, TimeUnit.SECONDS)) {
                throw new BusinessException(MessageConstant.SYSTEM_BUSY);
            }
            // 锁内重查状态，防止已在超时检查中被取消
            order = getById(orderId);
            if (order == null || !Objects.equals(order.getStatus(), StatusConstant.ORDER_WAIT_PICKUP)) {
                throw new BusinessException(MessageConstant.ORDER_STATUS_CHANGED);
            }

            // 5. 更新订单状态
            order.setStatus(StatusConstant.ORDER_CANCELLED);
            order.setCancelTime(LocalDateTime.now());
            order.setCancelReason(dto.getReason() != null ? dto.getReason() : "配送员主动取消");
            updateById(order);

            // 6. 任务回退至待接单
            Task task = taskMapper.selectById(order.getTaskId());
            if (task != null) {
                task.setStatus(StatusConstant.TASK_WAITING);
                task.setUpdatedAt(LocalDateTime.now());
                taskMapper.updateById(task);
            }

            // 7. 原子减少配送员当前接单数
            runnerProfileMapper.decrementCurrentOrders(runnerId);

            // 8. 清除任务相关缓存
            Objects.requireNonNull(cacheManager.getCache(RedisConstant.CACHE_TASK_HALL)).clear();
            Objects.requireNonNull(cacheManager.getCache(RedisConstant.CACHE_TASK_DETAIL)).clear();
            var nullKeys = stringRedisTemplate.keys(RedisConstant.TASK_HALL_NULL_PREFIX + "*");
            if (nullKeys != null && !nullKeys.isEmpty()) stringRedisTemplate.delete(nullKeys);

            log.info("配送员 {} 在5分钟内取消订单 {}，任务 {} 已回退至待接单", runnerId, orderId, order.getTaskId());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(MessageConstant.ERROR);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 配送员确认取件方法
     *  校验：订单存在，配送员为当前配送员，状态机验证订单状态为"已接单"
     *  逻辑：设置订单取货时间，取货凭证，更新订单状态为"配送中"，取货成功后调用NotificationAspect切面发送通知
     *
     * @param runnerId 配送员ID
     * @param orderId 订单ID
     * @param proof 取货凭证
     */
    @Override
    @Transactional
    @RedisDefend(RedisDefend.Type.LOCK)
    @SendNotification(
            targetUserType = 1,
            noticeType = StatusConstant.NOTICE_ORDER,
            title = "配送员已取货",
            content = "您的任务 #taskNo 已确认取货"
    )
    public void confirmPickup(Long runnerId, Long orderId, ProofImageDTO proof) {
        TaskOrder order = getById(orderId);
        if (order == null) throw new NotFoundException(MessageConstant.ORDER_NOT_EXIST);
        if (!order.getRunnerId().equals(runnerId)) {
            throw new ForbiddenException(MessageConstant.OPERATION_NOT_ALLOWED);
        }

        // 分布式锁防止与 OrderTimeoutChecker 待取货超时取消并发
        String lockKey = RedisConstant.ORDER_LOCK_KEY + order.getTaskId();
        RLock lock = redissonClient.getLock(lockKey);
        try {
            if (!lock.tryLock(RedisConstant.LOCK_WAIT_TIME, RedisConstant.LOCK_EXPIRE, TimeUnit.SECONDS)) {
                throw new BusinessException(MessageConstant.SYSTEM_BUSY);
            }
            // 锁内重查状态
            order = getById(orderId);
            if (order == null || !Objects.equals(order.getStatus(), StatusConstant.ORDER_WAIT_PICKUP)) {
                throw new BusinessException(MessageConstant.ORDER_STATUS_CHANGED);
            }

            OrderStateMachine.validate(order.getStatus(), StatusConstant.ORDER_DELIVERING, "Order");
            order.setPickupProofImgs(proof.getImageUrls() != null ? JSONUtil.toJsonStr(proof.getImageUrls()) : null);
            order.setPickupTime(LocalDateTime.now());
            order.setStatus(StatusConstant.ORDER_DELIVERING);
            updateById(order);

            Task task = taskMapper.selectById(order.getTaskId());
            if (task != null) {
                TaskStateMachine.validate(task.getStatus(), StatusConstant.TASK_DELIVERING, "Task");
                task.setStatus(StatusConstant.TASK_DELIVERING);
                task.setUpdatedAt(LocalDateTime.now());
                taskMapper.updateById(task);
            }
            log.info("订单 {} 已取货，凭证：{}", orderId, proof.getImageUrls());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(MessageConstant.ERROR);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 配送员确认送达方法
     *  校验：订单存在，配送员为当前配送员，状态机验证订单状态为"配送中"
     *  逻辑：设置送达凭证和送达时间，更新订单状态为"待确认"，同时更新任务状态为"待确认"
     *
     * @param runnerId 配送员ID
     * @param orderId 订单ID
     * @param proof 送达凭证图片信息
     */
    @Override
    @Transactional
    @SendNotification(
            targetUserType = 1,
            noticeType = StatusConstant.NOTICE_ORDER,
            title = "订单已送达",
            content = "您的任务 #taskNo 已送达，请确认"
    )
    public void confirmDeliver(Long runnerId, Long orderId, ProofImageDTO proof) {
        TaskOrder order = getById(orderId);
        if (order == null) throw new NotFoundException(MessageConstant.ORDER_NOT_EXIST);
        if (!order.getRunnerId().equals(runnerId)) {
            throw new ForbiddenException(MessageConstant.OPERATION_NOT_ALLOWED);
        }

        // 分布式锁防止并发重复送达
        String lockKey = RedisConstant.ORDER_LOCK_KEY + order.getTaskId();
        RLock lock = redissonClient.getLock(lockKey);
        try {
            if (!lock.tryLock(RedisConstant.LOCK_WAIT_TIME, RedisConstant.LOCK_EXPIRE, TimeUnit.SECONDS)) {
                throw new BusinessException(MessageConstant.SYSTEM_BUSY);
            }
            // 锁内重查状态
            order = getById(orderId);
            if (order == null || !Objects.equals(order.getStatus(), StatusConstant.ORDER_DELIVERING)) {
                throw new BusinessException(MessageConstant.ORDER_STATUS_CHANGED);
            }

            OrderStateMachine.validate(order.getStatus(), StatusConstant.ORDER_WAIT_CONFIRM, "Order");
            order.setDeliverProofImgs(proof.getImageUrls() != null ? JSONUtil.toJsonStr(proof.getImageUrls()) : null);
            order.setDeliverTime(LocalDateTime.now());
            order.setStatus(StatusConstant.ORDER_WAIT_CONFIRM);
            updateById(order);

            Task task = taskMapper.selectById(order.getTaskId());
            if (task != null) {
                TaskStateMachine.validate(task.getStatus(), StatusConstant.TASK_WAIT_CONFIRM, "Task");
                task.setStatus(StatusConstant.TASK_WAIT_CONFIRM);
                task.setUpdatedAt(LocalDateTime.now());
                taskMapper.updateById(task);
            }
            log.info("订单 {} 已送达，凭证：{}", orderId, proof.getImageUrls());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(MessageConstant.ERROR);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 用户确认完成方法
     *  校验：订单存在，状态机验证订单状态为"待确认"，任务存在，当前用户为任务发布者
     *  逻辑：设置确认完成时间，更新订单状态为"已完成"，更新任务状态为"已完成",
     *  向跑腿员支付报酬，原子更新跑腿员完成单数统计，清除排行榜和仪表盘缓存
     *
     * @param publisherId 发布者ID
     * @param orderId 订单ID
     */
    @Override
    @Transactional
    @RedisDefend(RedisDefend.Type.LOCK)
    @SendNotification(
            targetUserType = 2,
            noticeType = StatusConstant.NOTICE_ORDER,
            title = "订单已完成",
            content = "您所接的任务 #taskNo 已被发布者确认完成"
    )
    public void confirmComplete(Long publisherId, Long orderId) {
        TaskOrder order = getById(orderId);
        if (order == null) throw new NotFoundException(MessageConstant.ORDER_NOT_EXIST);

        // 分布式锁防止与 OrderAutoCompleteChecker 并发处理同一订单
        String lockKey = RedisConstant.ORDER_LOCK_KEY + order.getTaskId();
        RLock lock = redissonClient.getLock(lockKey);
        try {
            if (!lock.tryLock(RedisConstant.LOCK_WAIT_TIME, RedisConstant.LOCK_EXPIRE, TimeUnit.SECONDS)) {
                throw new BusinessException(MessageConstant.SYSTEM_BUSY);
            }
            // 锁内重查，防止状态已被自动结算修改
            order = getById(orderId);
            if (order == null) throw new NotFoundException(MessageConstant.ORDER_NOT_EXIST);
            OrderStateMachine.validate(order.getStatus(), StatusConstant.ORDER_COMPLETED, "Order");
        Task task = taskMapper.selectById(order.getTaskId());
        if (task == null) throw new NotFoundException(MessageConstant.TASK_NOT_EXIST);
        if (!task.getPublisherId().equals(publisherId)) {
            throw new BusinessException(MessageConstant.ERROR);
        }

        LocalDateTime now = LocalDateTime.now();
        order.setConfirmTime(now);
        order.setStatus(StatusConstant.ORDER_COMPLETED);
        updateById(order);

        TaskStateMachine.validate(task.getStatus(), StatusConstant.TASK_COMPLETED, "Task");
        task.setStatus(StatusConstant.TASK_COMPLETED);
        task.setUpdatedAt(now);
        taskMapper.updateById(task);

        User publisher = userMapper.selectById(task.getPublisherId());
        User runner = userMapper.selectById(order.getRunnerId());
        if (publisher == null || runner == null) throw new NotFoundException(MessageConstant.USER_NOT_EXIST);

        // 订单完成，原子减少当前接单数（无论手动确认还是自动结算，每次完成都必须递减）
        runnerProfileMapper.decrementCurrentOrders(order.getRunnerId());

        // 向跑腿员支付报酬，幂等返回 false 时说明自动结算已处理，跳过统计更新
        if (paymentService.payToRunner(order.getRunnerId(), order.getTaskId(), task.getReward())) {
            // 原子更新跑腿员完成单数统计，防止与 OrderAutoCompleteChecker 并发重复计数
            runnerProfileMapper.incrementCompletedStats(order.getRunnerId());
        }
        log.info("订单 {} 已完成，报酬 {} 元已支付给跑腿员", orderId, task.getReward());

        // 任务完成影响排行榜和仪表盘，清除相关缓存
        Objects.requireNonNull(cacheManager.getCache(RedisConstant.CACHE_LEADERBOARD)).clear();
        Objects.requireNonNull(cacheManager.getCache(RedisConstant.CACHE_DASHBOARD)).clear();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(MessageConstant.ERROR);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 根据订单ID查询订单详情方法
     *  逻辑：根据订单ID查询订单及关联任务、用户信息，
     *  构建OrderDetailVO返回完整的订单详情，包括发布者/配送员信息、地址、凭证图片等
     *
     * @param orderId 订单ID
     * @param currentUserId 当前登录用户ID（用于判断是否为发布者/配送员）
     * @return OrderDetailVO 订单详情VO
     */
    @Override
    public OrderDetailVO getOrderDetailByOrderId(Long orderId, Long currentUserId) {
        TaskOrder order = getById(orderId);
        if (order == null) throw new NotFoundException(MessageConstant.ORDER_NOT_EXIST);

        Task task = taskMapper.selectById(order.getTaskId());
        if (task == null) throw new NotFoundException(MessageConstant.TASK_NOT_EXIST);

        List<Long> userIds = List.of(task.getPublisherId(), order.getRunnerId());
        List<User> users = userMapper.selectBatchIds(userIds);
        Map<Long, User> userMap = users.stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        OrderDetailVO vo = BeanUtil.copyProperties(order, OrderDetailVO.class,
                "pickupProofImgs", "deliverProofImgs");
        vo.setOrderId(order.getId());
        vo.setTaskNo(task.getTaskNo());
        vo.setType(task.getType());
        vo.setSubType(task.getSubType());
        vo.setTaskSpecs(task.getTaskSpecs());
        vo.setPublicDesc(task.getPublicDesc() != null ? task.getPublicDesc() : "");
        vo.setPrivateNote(task.getPrivateNote() != null ? task.getPrivateNote() : "");
        vo.setReward(task.getReward());
        vo.setOrderStatus(order.getStatus());
        vo.setPickupAddress(task.getPickupAddress());
        vo.setDeliveryAddress(task.getDeliveryAddress());
        vo.setPickupCode(task.getPickupCode());
        vo.setImageUrls(task.getImageUrls() != null
                ? JSONUtil.toList(task.getImageUrls(), String.class)
                : Collections.emptyList());

        User publisher = userMap.get(task.getPublisherId());
        User runner = userMap.get(order.getRunnerId());

        vo.setPublisherId(publisher != null ? publisher.getId() : null);
        vo.setRunnerId(runner != null ? runner.getId() : null);
        vo.setPublisherNickname(publisher != null ? publisher.getNickname() : "");
        vo.setRunnerNickname(runner != null ? runner.getNickname() : "");
        vo.setContactName(task.getContactName() != null ? task.getContactName() : "");
        vo.setContactPhone(task.getContactPhone() != null ? task.getContactPhone() : "");
        vo.setPublisherPhone(publisher != null ? publisher.getPhone() : "");
        vo.setRunnerPhone(runner != null ? runner.getPhone() : "");
        vo.setPublisherAvatar(publisher != null ? publisher.getAvatarUrl() : "");
        vo.setRunnerAvatar(runner != null ? runner.getAvatarUrl() : "");

        vo.setPickupProofImgs(order.getPickupProofImgs() != null
                ? JSONUtil.toList(order.getPickupProofImgs(), String.class)
                : Collections.emptyList());
        vo.setDeliverProofImgs(order.getDeliverProofImgs() != null
                ? JSONUtil.toList(order.getDeliverProofImgs(), String.class)
                : Collections.emptyList());

        vo.setOwnerPublisher(currentUserId.equals(task.getPublisherId()));
        vo.setOwnerRunner(currentUserId.equals(order.getRunnerId()));

        // 对非参与方脱敏联系信息
        if (!vo.isOwnerPublisher() && !vo.isOwnerRunner()) {
            String contactName = vo.getContactName();
            if (contactName != null && !contactName.isEmpty()) {
                vo.setContactName(contactName.length() == 1
                        ? contactName + "*"
                        : contactName.charAt(0) + "*".repeat(contactName.length() - 1));
            }
            String contactPhone = vo.getContactPhone();
            if (contactPhone != null && contactPhone.length() > 4) {
                vo.setContactPhone("*".repeat(contactPhone.length() - 4)
                        + contactPhone.substring(contactPhone.length() - 4));
            }
            vo.setPublisherPhone(maskPhone(vo.getPublisherPhone()));
            vo.setRunnerPhone(maskPhone(vo.getRunnerPhone()));
        }

        vo.setCancelReason(order.getCancelReason());

        return vo;
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() <= 4) return phone;
        return "*".repeat(phone.length() - 4) + phone.substring(phone.length() - 4);
    }

    /**
     * 根据任务ID查询订单详情方法
     *  逻辑：根据任务ID查询该任务最新的有效订单（按ID倒序取最新），
     *  若有订单记录-复用getOrderDetailByOrderId方法获取完整订单详情
     *  若无订单记录-检查是否为已取消的任务，如果是，构建OrderDetailVO返回取消任务详情；如果不是，抛出异常
     *
     * @param taskId 任务ID
     * @param currentUserId 当前登录用户ID
     * @return OrderDetailVO 订单详情VO
     */
    @Override
    public OrderDetailVO getOrderDetailByTaskId(Long taskId, Long currentUserId) {
        // 查询该任务的最新订单（按ID倒序取最新）
        TaskOrder order = taskOrderMapper.selectLatestByTaskId(taskId);
        if (order != null) {
            return getOrderDetailByOrderId(order.getId(), currentUserId);
        }

        // 无订单记录 — 检查是否为已取消的任务
        Task task = taskMapper.selectById(taskId);
        if (task != null && StatusConstant.TASK_CANCELLED.equals(task.getStatus())) {
            OrderDetailVO vo = BeanUtil.copyProperties(task, OrderDetailVO.class, "imageUrls");
            vo.setOrderId(null);
            vo.setTaskId(task.getId());
            vo.setOrderStatus(StatusConstant.ORDER_CANCELLED);
            vo.setPickupCode(null); // 已取消不暴露取件码
            vo.setImageUrls(task.getImageUrls() != null
                    ? JSONUtil.toList(task.getImageUrls(), String.class)
                    : Collections.emptyList());
            vo.setCancelReason(task.getCancelReason());
            vo.setCancelTime(task.getUpdatedAt());

            User publisher = userMapper.selectById(task.getPublisherId());
            vo.setPublisherId(task.getPublisherId());
            vo.setPublisherNickname(publisher != null ? publisher.getNickname() : "");
            vo.setPublisherAvatar(publisher != null ? publisher.getAvatarUrl() : "");
            vo.setPublisherPhone(publisher != null ? publisher.getPhone() : "");
            vo.setOwnerPublisher(currentUserId != null && currentUserId.equals(task.getPublisherId()));
            vo.setOwnerRunner(false);
            return vo;
        }

        throw new NotFoundException(MessageConstant.ORDER_BELONGS_TO_NULL_TASK);
    }

    /**
     * 获取配送员接的订单列表方法
     *  逻辑：根据配送员ID和状态条件分页查询订单，
     *  关联查询任务和发布者信息，转换为OrderListVO列表返回
     *
     * @param userId 配送员用户ID
     * @param status 订单状态筛选（可为null）
     * @param page 页码
     * @param size 每页条数
     * @return PageResult<OrderListVO> 分页订单列表
     */
    @Override
    public PageResult<OrderListVO> listMyAcceptOrders(Long userId, Integer status, int page, int size) {
        LambdaQueryWrapper<TaskOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TaskOrder::getRunnerId, userId)
                .eq(TaskOrder::getIsDeleted, StatusConstant.ORDER_NOT_DELETED)
                .eq(status != null, TaskOrder::getStatus, status)
                .orderByDesc(TaskOrder::getAcceptTime);

        Page<TaskOrder> p = page(new Page<>(page, size), wrapper);

        if (p.getRecords().isEmpty()) {
            return new PageResult<>(0L, Collections.emptyList());
        }

        List<Long> taskIds = p.getRecords().stream()
                .map(TaskOrder::getTaskId)
                .distinct()
                .collect(Collectors.toList());

        List<Task> tasks = taskMapper.selectBatchIds(taskIds);
        Map<Long, Task> taskMap = tasks.stream()
                .collect(Collectors.toMap(Task::getId, t -> t));

        List<Long> publisherIds = tasks.stream()
                .map(Task::getPublisherId)
                .distinct()
                .collect(Collectors.toList());

        List<User> publishers = userMapper.selectBatchIds(publisherIds);
        Map<Long, User> publisherMap = publishers.stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        // 查询当前用户已评价的任务ID
        Set<Long> reviewedTaskIds = new HashSet<>(reviewMapper.selectReviewedTaskIds(taskIds, userId));

        List<OrderListVO> records = p.getRecords().stream()
                .map(order -> convertToOrderListVO(order, taskMap, publisherMap, reviewedTaskIds))
                .collect(Collectors.toList());

        return new PageResult<>(p.getTotal(), records);
    }

    /**
     * 软删除订单，仅允许发布者或配送员删除已完成且完成时间超过7天的订单
     *
     * @param userId 当前用户ID
     * @param orderId 订单ID
     */
    @Override
    @Transactional
    public void deleteOrder(Long userId, Long orderId) {
        TaskOrder order = getById(orderId);
        if (order == null) throw new NotFoundException(MessageConstant.ORDER_NOT_EXIST);

        Task task = taskMapper.selectById(order.getTaskId());
        if (task == null) throw new NotFoundException(MessageConstant.TASK_NOT_EXIST);

        boolean isPublisher = userId.equals(task.getPublisherId());
        boolean isRunner = userId.equals(order.getRunnerId());
        if (!isPublisher && !isRunner) {
            throw new ForbiddenException(MessageConstant.OPERATION_NOT_ALLOWED);
        }

        if (!StatusConstant.ORDER_COMPLETED.equals(order.getStatus())) {
            throw new ForbiddenException(MessageConstant.OPERATION_NOT_ALLOWED);
        }

        if (order.getConfirmTime() == null || order.getConfirmTime().isAfter(LocalDateTime.now().minusDays(7))) {
            throw new ForbiddenException(MessageConstant.OPERATION_NOT_ALLOWED);
        }

        if (StatusConstant.ORDER_DELETED.equals(order.getIsDeleted())) {
            throw new ForbiddenException(MessageConstant.ORDER_DELETED);
        }

        order.setIsDeleted(StatusConstant.ORDER_DELETED);
        updateById(order);
        log.info("用户 {} 删除订单 {}", userId, orderId);
    }

}