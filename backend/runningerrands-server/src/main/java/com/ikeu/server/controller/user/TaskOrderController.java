package com.ikeu.server.controller.user;

import com.ikeu.common.constant.MessageConstant;
import com.ikeu.common.context.BaseContext;
import com.ikeu.common.result.PageResult;
import com.ikeu.common.result.Result;
import com.ikeu.model.dto.CancelOrderDTO;
import com.ikeu.model.dto.ProofImageDTO;
import com.ikeu.model.vo.OrderDetailVO;
import com.ikeu.model.vo.OrderListVO;
import com.ikeu.server.annotation.RequireCertify;
import com.ikeu.server.service.TaskOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 订单流程管理接口，提供接单、取货、送达、确认完成、取消和详情查看等操作。
 * @author ikeu
 * @since 2025/05/23
 */
@Tag(name = "用户端-订单管理相关接口", description = "订单流程：接单、取货、送达、确认、取消、详情")
@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
@Slf4j
@Validated
public class TaskOrderController {

    private final TaskOrderService taskOrderService;

    /**
     * 配送员接单。
     *
     * <p>委托 {@link TaskOrderService#acceptOrder} 校验配送员在线状态、信用分、
     * 任务当前状态为"待接单"、配送员未超过最大接单数等条件，使用 Redis 分布式锁防止并发抢单，
     * 创建 task_order 记录并将任务状态更新为"已接单"。
     *
     * @param taskId 任务ID
     * @return 操作结果
     */
    @RequireCertify
    @Operation(summary = "配送员接单")
    @PostMapping("/accept/{taskId}")
    public Result<Void> accept(@PathVariable Long taskId) {
        Long runnerId = BaseContext.getCurrentId();
        taskOrderService.acceptOrder(runnerId, taskId);
        return Result.success(MessageConstant.ORDER_ACCEPT_SUCCESS);
    }

    /**
     * 配送员确认取货。
     *
     * <p>委托 {@link TaskOrderService#confirmPickup} 校验订单归属和状态为"已接单"，
     * 上传取货凭证图片，更新订单状态为"已取货"并记录取货时间。
     *
     * @param orderId 订单ID
     * @param proof 取货凭证图片DTO
     * @return 操作结果
     */
    @RequireCertify
    @Operation(summary = "确认取货")
    @PostMapping("/{orderId}/pickup")
    public Result<Void> pickup(@PathVariable Long orderId,
                               @Valid @RequestBody ProofImageDTO proof) {
        Long runnerId = BaseContext.getCurrentId();
        taskOrderService.confirmPickup(runnerId, orderId, proof);
        return Result.success(MessageConstant.ORDER_PICKUP_SUCCESS);
    }

    /**
     * 配送员确认送达。
     *
     * <p>委托 {@link TaskOrderService#confirmDeliver} 校验订单归属和状态为"已取货"，
     * 上传送达凭证图片，更新订单状态为"已送达"并记录送达时间。
     *
     * @param orderId 订单ID
     * @param proof 送达凭证图片DTO
     * @return 操作结果
     */
    @RequireCertify
    @Operation(summary = "确认送达")
    @PostMapping("/{orderId}/deliver")
    public Result<Void> deliver(@PathVariable Long orderId,
                                @Valid @RequestBody ProofImageDTO proof) {
        Long runnerId = BaseContext.getCurrentId();
        taskOrderService.confirmDeliver(runnerId, orderId, proof);
        return Result.success(MessageConstant.ORDER_DELIVERED_SUCCESS);
    }

    /**
     * 用户（发布者）确认订单完成。
     *
     * <p>委托 {@link TaskOrderService#confirmComplete} 校验当前用户为任务发布者、
     * 订单状态为"已送达"，更新订单和任务状态为"已完成"，
     * 触发报酬结算和信用分更新。
     *
     * @param orderId 订单ID
     * @return 操作结果
     */
    @RequireCertify
    @Operation(summary = "用户确认完成")
    @PostMapping("/{orderId}/confirm")
    public Result<Void> confirm(@PathVariable Long orderId) {
        Long userId = BaseContext.getCurrentId();
        taskOrderService.confirmComplete(userId, orderId);
        return Result.success(MessageConstant.ORDER_COMPLETED_SUCCESS);
    }

    /**
     * 通过订单ID获取订单详情。
     *
     * <p>委托 {@link TaskOrderService#getOrderDetailByOrderId} 查询订单完整信息，
     * 关联任务信息、发布者和配送员的用户信息。校验当前用户是否为发布者或配送员，
     * 非关联用户拒绝访问。
     *
     * @param orderId 订单ID
     * @return 订单详细信息，无权限时返回错误
     */
    @RequireCertify
    @Operation(summary = "通过订单ID获取订单详情")
    @GetMapping("/{orderId}")
    public Result<OrderDetailVO> getDetail(@PathVariable Long orderId) {
        Long userId = BaseContext.getCurrentId();
        OrderDetailVO detail = taskOrderService.getOrderDetailByOrderId(orderId, userId);
        if (detail == null) return Result.error(MessageConstant.ORDER_NOT_EXIST);
        if (!detail.isOwnerPublisher() && !detail.isOwnerRunner()) {
            return Result.error(MessageConstant.ORDER_CANNOT_VIEW);
        }
        return Result.success(detail);
    }

    /**
     * 通过任务ID获取对应的订单详情。
     *
     * <p>委托 {@link TaskOrderService#getOrderDetailByTaskId} 根据 taskId 查找关联订单，
     * 后续校验逻辑同 {@link #getDetail(Long)}。
     *
     * @param taskId 任务ID
     * @return 订单详细信息，无权限时返回错误
     */
    @RequireCertify
    @Operation(summary = "通过任务ID获取订单详情")
    @GetMapping("/task/{taskId}")
    public Result<OrderDetailVO> getDetailByTaskId(@PathVariable Long taskId) {
        Long userId = BaseContext.getCurrentId();
        OrderDetailVO detail = taskOrderService.getOrderDetailByTaskId(taskId, userId);
        if (detail == null) return Result.error(MessageConstant.ORDER_NOT_EXIST);
        if (!detail.isOwnerPublisher() && !detail.isOwnerRunner()) {
            return Result.error(MessageConstant.ORDER_CANNOT_VIEW);
        }
        return Result.success(detail);
    }

    /**
     * 配送员获取当前已接的订单列表，支持按状态筛选。
     *
     * <p>委托 {@link TaskOrderService#listMyAcceptOrders} 按 runnerId 和可选状态条件
     * 查询 task_order 表，关联任务信息后分页返回 OrderListVO。
     *
     * @param status 订单状态（可选）
     * @param page 页码，默认1
     * @param size 每页条数，默认10
     * @return 订单列表分页结果
     */
    @RequireCertify
    @Operation(summary = "配送员获取当前接的订单列表")
    @GetMapping("/mine")
    public Result<PageResult<OrderListVO>> myAcceptOrders(
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        Long userId = BaseContext.getCurrentId();
        return Result.success(taskOrderService.listMyAcceptOrders(userId, status, page, size));
    }

    /**
     * 配送员接单后5分钟内取消订单。
     *
     * <p>委托 {@link TaskOrderService#cancelOrder} 校验订单归属、状态为待取货、
     * 接单时间不超过5分钟，取消订单并将关联任务回退至待接单状态。
     *
     * @param orderId 订单ID
     * @param dto 取消订单DTO（包含取消原因）
     * @return 操作结果
     */
    @RequireCertify
    @Operation(summary = "配送员取消订单（接单后5分钟内）")
    @PostMapping("/{orderId}/cancel")
    public Result<Void> cancelOrder(@PathVariable Long orderId,
                                    @Valid @RequestBody CancelOrderDTO dto) {
        Long runnerId = BaseContext.getCurrentId();
        taskOrderService.cancelOrder(runnerId, orderId, dto);
        return Result.success(MessageConstant.ORDER_CANCEL_SUCCESS);
    }

    /**
     * 软删除订单（发布者或配送员删除已完成超过7天的订单）。
     *
     * <p>委托 {@link TaskOrderService#deleteOrder} 校验当前用户为发布者或配送员、
     * 订单状态为"已完成"且完成时间超过7天，将订单标记为已删除（软删除）。
     *
     * @param orderId 订单ID
     * @return 操作结果
     */
    @RequireCertify
    @Operation(summary = "删除订单")
    @DeleteMapping("/{orderId}")
    public Result<Void> deleteOrder(@PathVariable Long orderId) {
        Long userId = BaseContext.getCurrentId();
        taskOrderService.deleteOrder(userId, orderId);
        return Result.success();
    }
}
