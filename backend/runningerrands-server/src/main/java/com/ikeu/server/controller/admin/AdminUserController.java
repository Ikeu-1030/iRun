package com.ikeu.server.controller.admin;

import com.ikeu.common.constant.MessageConstant;
import com.ikeu.common.result.PageResult;
import com.ikeu.common.result.Result;
import com.ikeu.model.vo.UserInfoVO;
import com.ikeu.server.annotation.OperationLog;
import com.ikeu.server.annotation.RequireRole;
import com.ikeu.server.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "管理端-用户管理")
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminService adminService;

    @RequireRole({1, 2})
    @Operation(summary = "用户详情")
    @GetMapping("/users/{userId}")
    public Result<UserInfoVO> getUserDetail(@PathVariable Long userId) {
        return Result.success(adminService.getUserDetail(userId));
    }

    @RequireRole({1, 2})
    @Operation(summary = "用户列表")
    @GetMapping("/users")
    public Result<PageResult<UserInfoVO>> listUsers(
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer isCertify,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(adminService.listUsers(status, isCertify, keyword, page, size));
    }

    @RequireRole({1})
    @OperationLog(module = "用户管理", action = "封禁/解封", description = "用户 #userId → #enabled")
    @Operation(summary = "封禁/解封用户")
    @PutMapping("/users/{userId}/status")
    public Result<Void> toggleStatus(@PathVariable Long userId,
                                     @RequestParam boolean enabled) {
        adminService.toggleUserStatus(userId, enabled);
        return Result.success(MessageConstant.SUCCESS);
    }

    @RequireRole({1, 2})
    @OperationLog(module = "用户管理", action = "审核认证", description = "审核用户 #userId 实名认证 → #isCertify")
    @Operation(summary = "审核用户实名认证（学生身份认证）")
    @PutMapping("/users/{userId}/certify")
    public Result<Void> reviewUserCertification(
            @PathVariable Long userId,
            @RequestParam Integer isCertify,
            @RequestParam(required = false) String remark) {
        adminService.reviewUserCertification(userId, isCertify, remark);
        return Result.success(MessageConstant.CERTIFY_REVIEWED);
    }
}
