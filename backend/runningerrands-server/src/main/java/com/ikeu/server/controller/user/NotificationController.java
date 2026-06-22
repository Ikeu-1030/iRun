package com.ikeu.server.controller.user;

import com.ikeu.common.context.BaseContext;
import com.ikeu.common.result.PageResult;
import com.ikeu.common.result.Result;
import com.ikeu.model.vo.NotificationVO;
import com.ikeu.server.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 站内信通知接口，提供通知列表查询、单条/全部标记已读、删除通知等功能。
 * @author ikeu
 * @since 2025/05/23
 */
@Tag(name = "用户端-消息通知接口", description = "站内信查看与管理")
@RestController
@RequestMapping("/notification")
@RequiredArgsConstructor
@Validated
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * 获取当前用户的通知列表，支持按已读状态筛选和分页。
     *
     * <p>委托 {@link NotificationService#listNotifications} 构建条件构造器按 userId 和
     * isRead 条件查询 notification 表，按创建时间倒序排列分页返回 NotificationVO。
     *
     * @param isRead 已读状态（可选，0未读 1已读）
     * @param page 页码，默认1
     * @param size 每页条数，默认10
     * @return 通知列表分页结果
     */
    @Operation(summary = "获取我的通知列表")
    @GetMapping
    public Result<PageResult<NotificationVO>> list(
            @RequestParam(required = false) Integer isRead,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        Long userId = BaseContext.getCurrentId();
        return Result.success(notificationService.listNotifications(userId, isRead, page, size));
    }

    /**
     * 标记单条通知为已读。
     *
     * <p>委托 {@link NotificationService#markAsRead} 校验通知归属当前用户后，
     * 将该通知的 is_read 字段更新为 1。
     *
     * @param id 通知ID
     * @return 操作结果
     */
    @Operation(summary = "标记单条已读")
    @PutMapping("/{id}/read")
    public Result<Void> markRead(@PathVariable Long id) {
        Long userId = BaseContext.getCurrentId();
        notificationService.markAsRead(userId, id);
        return Result.success();
    }

    /**
     * 将当前用户的所有通知标记为已读。
     *
     * <p>委托 {@link NotificationService#markAllAsRead} 批量更新该用户所有通知的
     * is_read 字段为 1。
     *
     * @return 操作结果
     */
    @Operation(summary = "全部标记已读")
    @PutMapping("/read-all")
    public Result<Void> markAllRead() {
        Long userId = BaseContext.getCurrentId();
        notificationService.markAllAsRead(userId);
        return Result.success();
    }

    /**
     * 批量标记多条通知为已读。
     */
    @Operation(summary = "批量标记已读")
    @PutMapping("/batch-read")
    public Result<Void> markBatchRead(@RequestBody Map<String, List<Long>> body) {
        Long userId = BaseContext.getCurrentId();
        notificationService.markBatchRead(userId, body.get("ids"));
        return Result.success();
    }

    /**
     * 删除指定通知。
     *
     * <p>委托 {@link NotificationService#deleteNotification} 校验通知归属当前用户后
     * 执行物理删除。
     *
     * @param id 通知ID
     * @return 操作结果
     */
    @Operation(summary = "删除通知")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        Long userId = BaseContext.getCurrentId();
        notificationService.deleteNotification(userId, id);
        return Result.success();
    }
}
