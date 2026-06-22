package com.ikeu.server.controller.admin;

import com.ikeu.common.constant.MessageConstant;
import com.ikeu.common.result.PageResult;
import com.ikeu.common.result.Result;
import com.ikeu.model.vo.TaskDetailVO;
import com.ikeu.model.vo.TaskListVO;
import com.ikeu.server.annotation.OperationLog;
import com.ikeu.server.annotation.RequireRole;
import com.ikeu.server.service.AdminTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 管理端任务管理接口，提供任务列表查询、详情查看和强制更新状态功能。
 * @author ikeu
 * @since 2025/06/02
 */
@Tag(name = "管理端-任务管理接口")
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Validated
public class AdminTaskController {

    private final AdminTaskService adminTaskService;

    @RequireRole({1, 2})
    @Operation(summary = "所有任务列表")
    @GetMapping("/tasks")
    public Result<PageResult<TaskListVO>> listAllTasks(
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        return Result.success(adminTaskService.listAllTasks(status, page, size));
    }

    @RequireRole({1, 2})
    @Operation(summary = "任务详情")
    @GetMapping("/tasks/{taskId}")
    public Result<TaskDetailVO> getTaskDetail(@PathVariable Long taskId) {
        return Result.success(adminTaskService.getTaskDetail(taskId));
    }

    @RequireRole({1})
    @OperationLog(module = "任务管理", action = "修改状态", description = "任务 #taskId → #status")
    @Operation(summary = "强制更新任务状态")
    @PutMapping("/tasks/{taskId}/status")
    public Result<Void> updateTaskStatus(@PathVariable Long taskId,
                                         @RequestParam Integer status) {
        adminTaskService.updateTaskStatus(taskId, status);
        return Result.success(MessageConstant.TASK_STATUS_UPDATED);
    }
}
