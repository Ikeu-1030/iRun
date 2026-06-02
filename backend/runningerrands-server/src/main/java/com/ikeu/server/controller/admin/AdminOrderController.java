package com.ikeu.server.controller.admin;

import com.ikeu.common.result.PageResult;
import com.ikeu.common.result.Result;
import com.ikeu.model.vo.OrderDetailVO;
import com.ikeu.model.vo.OrderManageVO;
import com.ikeu.server.annotation.OperationLog;
import com.ikeu.server.annotation.RequireRole;
import com.ikeu.server.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "管理端-订单管理")
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminOrderController {

    private final AdminService adminService;

    @RequireRole({1, 2})
    @Operation(summary = "所有订单列表")
    @GetMapping("/orders")
    public Result<PageResult<OrderManageVO>> listAllOrders(
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(adminService.listAllOrders(status, page, size));
    }

    @OperationLog(module = "订单管理", action = "查看", description = "查看订单详情")
    @RequireRole({1, 2})
    @Operation(summary = "订单详情")
    @GetMapping("/orders/{id}")
    public Result<OrderDetailVO> getOrderDetail(@PathVariable Long id) {
        return Result.successData(adminService.getOrderDetail(id));
    }

    @OperationLog(module = "订单管理", action = "修改", description = "订单 #id → #orderStatus")
    @RequireRole({1, 2})
    @Operation(summary = "强制修改订单状态")
    @PutMapping("/orders/{id}/status")
    public Result<Void> updateOrderStatus(@PathVariable Long id, @RequestParam("status") Integer orderStatus) {
        adminService.updateOrderStatus(id, orderStatus);
        return Result.success();
    }
}
