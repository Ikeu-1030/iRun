package com.ikeu.server.controller.admin;

import com.ikeu.common.constant.MessageConstant;
import com.ikeu.common.result.PageResult;
import com.ikeu.common.result.Result;
import com.ikeu.model.dto.AdminCreateDTO;
import com.ikeu.model.dto.AdminPasswordResetDTO;
import com.ikeu.model.dto.AdminUpdateDTO;
import com.ikeu.model.vo.AdminListVO;
import com.ikeu.server.annotation.OperationLog;
import com.ikeu.server.annotation.RequireRole;
import com.ikeu.server.service.AdminEmployeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "管理端-员工管理")
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminEmployeeController {

    private final AdminEmployeeService adminEmployeeService;

    @Operation(summary = "管理员员工列表")
    @GetMapping("/employees")
    @RequireRole({1, 2})
    public Result<PageResult<AdminListVO>> listEmployees(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(adminEmployeeService.listEmployees(page, size));
    }

    @Operation(summary = "管理员员工详情")
    @GetMapping("/employees/{id}")
    @RequireRole({1, 2})
    public Result<AdminListVO> getEmployee(@PathVariable Long id) {
        return Result.success(adminEmployeeService.getEmployee(id));
    }

    @OperationLog(module = "员工管理", action = "新增", description = "新建管理员 #dto.username")
    @Operation(summary = "新增管理员")
    @PostMapping("/employees")
    @RequireRole({1})
    public Result<Void> createEmployee(@Valid @RequestBody AdminCreateDTO dto) {
        adminEmployeeService.createEmployee(dto);
        return Result.success(MessageConstant.ADMIN_CREATE_SUCCESS);
    }

    @OperationLog(module = "员工管理", action = "修改", description = "修改管理员 #id 信息")
    @Operation(summary = "编辑管理员")
    @PutMapping("/employees/{id}")
    @RequireRole({1})
    public Result<Void> updateEmployee(@PathVariable Long id, @Valid @RequestBody AdminUpdateDTO dto) {
        adminEmployeeService.updateEmployee(id, dto);
        return Result.success(MessageConstant.ADMIN_UPDATE_SUCCESS);
    }

    @OperationLog(module = "员工管理", action = "封禁/解封", description = "管理员 #id → #enabled")
    @Operation(summary = "启用/停用管理员")
    @PutMapping("/employees/{id}/status")
    @RequireRole({1})
    public Result<Void> toggleStatus(@PathVariable Long id, @RequestParam boolean enabled) {
        adminEmployeeService.toggleStatus(id, enabled);
        return Result.success(MessageConstant.SUCCESS);
    }

    @OperationLog(module = "员工管理", action = "重置密码", description = "重置管理员 #id 密码")
    @Operation(summary = "重置管理员密码")
    @PutMapping("/employees/{id}/password")
    @RequireRole({1})
    public Result<Void> resetPassword(@PathVariable Long id, @Valid @RequestBody AdminPasswordResetDTO dto) {
        adminEmployeeService.resetPassword(id, dto);
        return Result.success(MessageConstant.ADMIN_PASSWORD_RESET);
    }

    @OperationLog(module = "员工管理", action = "删除", description = "删除管理员 #id")
    @Operation(summary = "删除管理员")
    @DeleteMapping("/employees/{id}")
    @RequireRole({1})
    public Result<Void> deleteEmployee(@PathVariable Long id) {
        adminEmployeeService.deleteEmployee(id);
        return Result.success(MessageConstant.ADMIN_DELETE_SUCCESS);
    }
}
