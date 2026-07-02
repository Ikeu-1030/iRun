package com.ikeu.server.service;

import com.ikeu.common.exception.BusinessException;
import com.ikeu.common.exception.NotFoundException;
import com.ikeu.model.vo.ChatVO;
import com.ikeu.model.vo.ContactVO;

import java.util.List;

/**
 * 聊天服务接口，提供消息收发、聊天记录查询、联系人管理和消息操作等功能。
 *
 * @author ikeu
 * @since 2026/05/14
 */
public interface ChatService {

    /**
     * 发送消息并持久化，通过 WebSocket 推送给接收者和发送者（双向推送）。
     *
     * <p>发送者将获取到服务端生成的 messageId，无需刷新即可操作删除/撤回。
     *
     * @param senderId 发送者 ID
     * @param receiverId 接收者 ID
     * @param content 消息内容
     * @param messageType 消息类型（1-文本 等）
     * @throws BusinessException 发送者 ID 为空时抛出
     */
    void sendMessage(Long senderId, Long receiverId, String content, Integer messageType);

    /**
     * 分页查询与指定用户的双向聊天记录，按时间倒序排列。
     *
     * <p>已撤回消息的内容替换为撤回提示文本。
     *
     * @param userId 当前用户 ID
     * @param targetUserId 目标用户 ID
     * @param page 页码，从 1 开始
     * @param size 每页条数
     * @return 聊天记录列表
     */
    List<ChatVO> getHistory(Long userId, Long targetUserId, int page, int size);

    /**
     * 获取联系人列表，包含最近消息摘要和未读计数，按最近消息时间倒序排列。
     *
     * @param userId 当前用户 ID
     * @return 联系人列表
     */
    List<ContactVO> getContacts(Long userId);

    /**
     * 标记某发送者的所有未读消息为已读。
     *
     * @param userId 当前用户 ID（接收者）
     * @param senderId 发送者 ID
     */
    void markRead(Long userId, Long senderId);

    /**
     * 软删除消息（仅标记删除），仅发送者可操作，通过 STOMP 通知接收者。
     *
     * @param userId 当前用户 ID
     * @param messageId 消息 ID
     * @throws NotFoundException 消息不存在时抛出
     * @throws BusinessException 当前用户非消息发送者时抛出
     */
    void deleteMessage(Long userId, Long messageId);

    /**
     * 撤回自己发送的消息（5 分钟内可撤回），内容替换为撤回占位文本并推送接收者。
     *
     * @param userId 当前用户 ID
     * @param messageId 消息 ID
     * @throws NotFoundException 消息不存在时抛出
     * @throws BusinessException 当前用户非发送者、消息已删除或已超过撤回时限时抛出
     */
    void recallMessage(Long userId, Long messageId);
}
