package com.ikeu.server.controller.admin;

import com.ikeu.common.constant.MessageConstant;
import com.ikeu.common.result.PageResult;
import com.ikeu.common.result.Result;
import com.ikeu.model.vo.RunnerManageVO;
import com.ikeu.server.annotation.OperationLog;
import com.ikeu.server.annotation.RequireRole;
import com.ikeu.server.service.AdminRunnerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 管理端跑腿员管理接口，提供跑腿员列表查询、详情查看、认证审核和禁止接单功能。
 * @author ikeu
 * @since 2025/06/01
 */
@Tag(name = "管理端-跑腿员管理接口")
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Validated
public class AdminRunnerController {

    private final AdminRunnerService adminRunnerService;

    @RequireRole({1, 2})
    @Operation(summary = "跑腿员管理列表")
    @GetMapping("/runners")
    public Result<PageResult<RunnerManageVO>> listRunners(
            @RequestParam(required = false) Integer verifyStatus,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        return Result.success(adminRunnerService.listRunners(verifyStatus, keyword, page, size));
    }

    @RequireRole({1, 2})
    @OperationLog(module = "跑腿员管理", action = "审核认证", description = "审核跑腿员档案 #profileId → #verifyStatus")
    @Operation(summary = "审核跑腿员认证")
    @PutMapping("/runners/{profileId}/review")
    public Result<Void> reviewRunnerCertification(
            @PathVariable Long profileId,
            @RequestParam Integer verifyStatus,
            @RequestParam(required = false) String remark) {
        adminRunnerService.reviewRunnerCertification(profileId, verifyStatus, remark);
        return Result.success(MessageConstant.SUCCESS);
    }

    @RequireRole({1, 2})
    @Operation(summary = "跑腿员详情")
    @GetMapping("/runners/{profileId}")
    public Result<RunnerManageVO> getRunnerDetail(@PathVariable Long profileId) {
        return Result.success(adminRunnerService.getRunnerDetail(profileId));
    }

    @RequireRole({1})
    @OperationLog(module = "跑腿员管理", action = "禁止接单", description = "禁止/恢复跑腿员 #profileId 接单")
    @Operation(summary = "禁止/恢复跑腿员接单")
    @PutMapping("/runners/{profileId}/ban")
    public Result<Void> toggleBan(@PathVariable Long profileId, @RequestParam boolean banned) {
        adminRunnerService.toggleRunnerBan(profileId, banned);
        return Result.success(MessageConstant.SUCCESS);
    }
}
