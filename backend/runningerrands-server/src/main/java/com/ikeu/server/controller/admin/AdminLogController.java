package com.ikeu.server.controller.admin;

import com.ikeu.common.result.PageResult;
import com.ikeu.common.result.Result;
import com.ikeu.model.entity.OperationLog;
import com.ikeu.server.annotation.RequireRole;
import com.ikeu.server.service.OperationLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 管理端操作日志接口，提供操作日志的分页查询和条件筛选。
 * @author ikeu
 * @since 2025/06/01
 */
@Tag(name = "管理端-操作日志")
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminLogController {

    private final OperationLogService operationLogService;

    @RequireRole({1, 2})
    @Operation(summary = "操作日志列表")
    @GetMapping("/logs")
    public Result<PageResult<OperationLog>> listLogs(
            @RequestParam(required = false) String module,
            @RequestParam(required = false) Long adminId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(operationLogService.listLogs(module, adminId, start, end, page, size));
    }
}
