package com.ikeu.server.controller.admin;

import com.ikeu.common.constant.MessageConstant;
import com.ikeu.common.context.BaseContext;
import com.ikeu.common.result.Result;
import com.ikeu.model.dto.AdminLoginDTO;
import com.ikeu.model.vo.AdminLoginVO;
import com.ikeu.server.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 管理端认证接口，提供管理员登录、刷新令牌和退出登录功能。
 * @author ikeu
 * @since 2025/06/01
 */
@Tag(name = "管理端-认证")
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AdminService adminService;

    @Operation(summary = "管理员登录")
    @PostMapping("/login")
    public Result<AdminLoginVO> login(@Valid @RequestBody AdminLoginDTO dto) {
        return Result.success(adminService.login(dto));
    }

    @Operation(summary = "刷新管理员访问令牌")
    @PostMapping("/refresh")
    public Result<AdminLoginVO> refresh(@RequestHeader("X-Refresh-Token") String refreshToken) {
        return Result.success(adminService.refreshAccessToken(refreshToken));
    }

    @Operation(summary = "获取当前管理员信息")
    @GetMapping("/info")
    public Result<AdminLoginVO> info() {
        return Result.success(adminService.getAdminInfo());
    }

    @Operation(summary = "管理员退出登录")
    @PostMapping("/logout")
    public Result<Void> logout() {
        adminService.logout(BaseContext.getCurrentId());
        return Result.success(MessageConstant.LOGOUT_SUCCESS);
    }
}
