package com.ikeu.server.controller.admin;

import com.ikeu.common.result.PageResult;
import com.ikeu.common.result.Result;
import com.ikeu.model.vo.OrderDetailVO;
import com.ikeu.model.vo.OrderManageVO;
import com.ikeu.server.annotation.OperationLog;
import com.ikeu.server.annotation.RequireRole;
import com.ikeu.server.service.AdminOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 管理端订单管理接口，提供订单列表查询、详情查看和强制修改订单状态功能。
 * @author ikeu
 * @since 2025/06/02
 */
@Tag(name = "管理端-订单管理接口")
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Validated
public class AdminOrderController {

    private final AdminOrderService adminOrderService;

    @RequireRole({1, 2})
    @Operation(summary = "所有订单列表")
    @GetMapping("/orders")
    public Result<PageResult<OrderManageVO>> listAllOrders(
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        return Result.success(adminOrderService.listAllOrders(status, page, size));
    }

    @OperationLog(module = "订单管理", action = "查看", description = "查看订单详情")
    @RequireRole({1, 2})
    @Operation(summary = "订单详情")
    @GetMapping("/orders/{id}")
    public Result<OrderDetailVO> getOrderDetail(@PathVariable Long id) {
        return Result.success(adminOrderService.getOrderDetail(id));
    }

    @OperationLog(module = "订单管理", action = "修改", description = "订单 #id → #orderStatus")
    @RequireRole({1})
    @Operation(summary = "强制修改订单状态")
    @PutMapping("/orders/{id}/status")
    public Result<Void> updateOrderStatus(@PathVariable Long id, @RequestParam("status") Integer orderStatus) {
        adminOrderService.updateOrderStatus(id, orderStatus);
        return Result.success();
    }
}
