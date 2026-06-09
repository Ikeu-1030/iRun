package com.ikeu.server.aspect;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ikeu.common.constant.TaskTypeConstant;
import com.ikeu.model.entity.Task;
import com.ikeu.model.entity.TaskOrder;
import com.ikeu.server.mapper.TaskMapper;
import com.ikeu.server.mapper.TaskOrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 办事代排切面，接单后根据任务 taskSpecs 中服务截止时间弹性调整订单预计完成时间。
 * @author ikeu
 * @since 2026/05/18
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class QueueWaitAspect {

    private final TaskMapper taskMapper;
    private final TaskOrderMapper taskOrderMapper;

    /**
     * 接单后置通知，对"办事代排"子类型任务调整订单预计完成时间。
     *
     * <p>校验和实现逻辑：
     * <ol>
     *   <li>拦截 {@code TaskOrderService.acceptOrder} 方法执行成功的返回</li>
     *   <li>查询 task，校验子类型是否为"办事代排"（SUB_QUEUE_WAIT），非该类型直接跳过</li>
     *   <li>解析 taskSpecs JSON 中的 serviceEndTime 字段 —— 为空或缺失则记录 warn 日志并跳过</li>
     *   <li>校验服务截止时间晚于当前时间 —— 已过期则跳过</li>
     *   <li>按 taskId + runnerId 查询最新 TaskOrder 记录</li>
     *   <li>将 order.expectFinishTime 更新为服务截止时间并保存</li>
     * </ol>
     * 整个过程包裹在 try-catch 中，异常仅记录 error 日志不影响接单主流程。
     *
     * @param runnerId 配送员ID（由切点参数绑定）
     * @param taskId 任务ID（由切点参数绑定）
     */
    @AfterReturning(value = "execution(* com.ikeu.server.service.TaskOrderService.acceptOrder(..)) && args(runnerId, taskId)", argNames = "runnerId,taskId")
    public void afterAcceptOrder(Long runnerId, Long taskId) {
        try {
            Task task = taskMapper.selectById(taskId);
            if (task == null || !TaskTypeConstant.SUB_QUEUE_WAIT.equals(task.getSubType())) return;

            String taskSpecs = task.getTaskSpecs();
            if (taskSpecs == null || taskSpecs.isBlank()) {
                log.warn("办事代排任务 {} 未指定 taskSpecs，跳过 expectFinishTime 调整", taskId);
                return;
            }

            JSONObject specs = JSONUtil.parseObj(taskSpecs);
            String serviceEndTimeStr = specs.getStr("服务截止时间");
            if (serviceEndTimeStr == null || serviceEndTimeStr.isBlank()) {
                log.warn("办事代排任务 {} 的 taskSpecs 未包含 serviceEndTime，跳过调整", taskId);
                return;
            }

            LocalDateTime serviceEndTime = LocalDateTime.parse(serviceEndTimeStr);
            if (!serviceEndTime.isAfter(LocalDateTime.now())) {
                log.warn("办事代排任务 {} 的服务截止时间 {} 已过期，跳过调整", taskId, serviceEndTime);
                return;
            }

            TaskOrder order = taskOrderMapper.selectOne(
                    new LambdaQueryWrapper<TaskOrder>()
                            .eq(TaskOrder::getTaskId, taskId)
                            .eq(TaskOrder::getRunnerId, runnerId)
                            .orderByDesc(TaskOrder::getId)
                            .last("LIMIT 1"));
            if (order == null) return;

            order.setExpectFinishTime(serviceEndTime);
            taskOrderMapper.updateById(order);
            log.info("办事代排任务 {} 订单 {} 预计完成时间已调整为 {}", taskId, order.getId(), serviceEndTime);
        } catch (Exception e) {
            log.error("办事代排 expectFinishTime 调整失败: taskId={}, runnerId={}", taskId, runnerId, e);
        }
    }
}
