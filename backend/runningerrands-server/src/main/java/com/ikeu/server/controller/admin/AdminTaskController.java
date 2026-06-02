package com.ikeu.server.controller.admin;

import com.ikeu.common.constant.MessageConstant;
import com.ikeu.common.result.PageResult;
import com.ikeu.common.result.Result;
import com.ikeu.model.vo.TaskDetailVO;
import com.ikeu.model.vo.TaskListVO;
import com.ikeu.server.annotation.OperationLog;
import com.ikeu.server.annotation.RequireRole;
import com.ikeu.server.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "管理端-任务管理")
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminTaskController {

    private final AdminService adminService;

    @RequireRole({1, 2})
    @Operation(summary = "所有任务列表")
    @GetMapping("/tasks")
    public Result<PageResult<TaskListVO>> listAllTasks(
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(adminService.listAllTasks(status, page, size));
    }

    @RequireRole({1, 2})
    @Operation(summary = "任务详情")
    @GetMapping("/tasks/{taskId}")
    public Result<TaskDetailVO> getTaskDetail(@PathVariable Long taskId) {
        return Result.success(adminService.getTaskDetail(taskId));
    }

    @RequireRole({1})
    @OperationLog(module = "任务管理", action = "修改状态", description = "任务 #taskId → #status")
    @Operation(summary = "强制更新任务状态")
    @PutMapping("/tasks/{taskId}/status")
    public Result<Void> updateTaskStatus(@PathVariable Long taskId,
                                         @RequestParam Integer status) {
        adminService.updateTaskStatus(taskId, status);
        return Result.success(MessageConstant.TASK_STATUS_UPDATED);
    }
}
