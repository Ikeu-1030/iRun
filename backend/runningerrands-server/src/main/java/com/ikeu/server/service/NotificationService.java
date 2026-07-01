package com.ikeu.server.service;

import com.ikeu.common.exception.NotFoundException;
import com.ikeu.common.result.PageResult;
import com.ikeu.model.vo.NotificationVO;

import java.util.List;

/**
 * 通知服务接口，提供通知发送、列表查询、已读标记、删除和过期清理等功能。
 *
 * @author ikeu
 * @since 2026/05/14
 */
public interface NotificationService {

    /**
     * 发送通知并持久化，同时通过 WebSocket 实时推送给用户。
     *
     * @param userId 接收者用户 ID
     * @param type 通知类型
     * @param title 通知标题
     * @param content 通知内容
     * @param targetId 关联业务 ID（如任务 ID / 订单 ID）
     */
    void sendNotification(Long userId, Integer type, String title, String content, Long targetId);

    /**
     * 分页查询当前用户的通知列表，支持按已读状态筛选，按创建时间倒序排列。
     *
     * @param userId 用户 ID
     * @param isRead 已读状态筛选（null 表示不限制）
     * @param page 页码，从 1 开始
     * @param size 每页条数
     * @return 分页通知列表
     */
    PageResult<NotificationVO> listNotifications(Long userId, Integer isRead, int page, int size);

    /**
     * 标记单条通知为已读，校验通知归属当前用户。
     *
     * @param userId 用户 ID
     * @param notificationId 通知 ID
     */
    void markAsRead(Long userId, Long notificationId);

    /**
     * 将当前用户的所有未读通知标记为已读。
     *
     * @param userId 用户 ID
     */
    void markAllAsRead(Long userId);

    /**
     * 批量标记指定 ID 列表的通知为已读。
     *
     * @param userId 用户 ID
     * @param ids 待标记的通知 ID 列表
     */
    void markBatchRead(Long userId, List<Long> ids);

    /**
     * 删除指定通知，校验通知存在且属于当前用户后物理删除。
     *
     * @param userId 用户 ID
     * @param notificationId 通知 ID
     * @throws NotFoundException 通知不存在或不属于当前用户时抛出
     */
    void deleteNotification(Long userId, Long notificationId);

    /**
     * 分批清理过期通知，返回当次删除行数。
     *
     * @param batchSize 每批处理数量
     * @return 本次清理的通知条数
     */
    int cleanupExpiredNotifications(int batchSize);

    /**
     * 获取当前用户的未读通知数量。
     *
     * @param userId 用户 ID
     * @return 未读通知数量
     */
    long getUnreadCount(Long userId);
}
