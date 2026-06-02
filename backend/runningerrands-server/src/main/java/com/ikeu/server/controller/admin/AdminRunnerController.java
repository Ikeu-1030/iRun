package com.ikeu.server.controller.admin;

import com.ikeu.common.constant.MessageConstant;
import com.ikeu.common.result.PageResult;
import com.ikeu.common.result.Result;
import com.ikeu.model.vo.RunnerManageVO;
import com.ikeu.server.annotation.OperationLog;
import com.ikeu.server.annotation.RequireRole;
import com.ikeu.server.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "管理端-跑腿员管理")
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminRunnerController {

    private final AdminService adminService;

    @RequireRole({1, 2})
    @Operation(summary = "跑腿员管理列表")
    @GetMapping("/runners")
    public Result<PageResult<RunnerManageVO>> listRunners(
            @RequestParam(required = false) Integer verifyStatus,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(adminService.listRunners(verifyStatus, keyword, page, size));
    }

    @RequireRole({1, 2})
    @OperationLog(module = "跑腿员管理", action = "审核认证", description = "审核跑腿员档案 #profileId → #verifyStatus")
    @Operation(summary = "审核跑腿员认证")
    @PutMapping("/reviews/{profileId}")
    public Result<Void> reviewCertification(
            @PathVariable Long profileId,
            @RequestParam Integer verifyStatus,
            @RequestParam(required = false) String remark) {
        adminService.reviewRunnerCertification(profileId, verifyStatus, remark);
        return Result.success(MessageConstant.SUCCESS);
    }

    @RequireRole({1, 2})
    @Operation(summary = "跑腿员详情")
    @GetMapping("/runners/{profileId}")
    public Result<RunnerManageVO> getRunnerDetail(@PathVariable Long profileId) {
        return Result.success(adminService.getRunnerDetail(profileId));
    }

    @RequireRole({1})
    @OperationLog(module = "跑腿员管理", action = "禁止接单", description = "禁止/恢复跑腿员 #profileId 接单")
    @Operation(summary = "禁止/恢复跑腿员接单")
    @PutMapping("/runners/{profileId}/ban")
    public Result<Void> toggleBan(@PathVariable Long profileId, @RequestParam boolean banned) {
        adminService.toggleRunnerBan(profileId, banned);
        return Result.success(MessageConstant.SUCCESS);
    }
}
