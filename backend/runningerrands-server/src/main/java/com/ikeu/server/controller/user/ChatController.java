package com.ikeu.server.controller.user;

import com.ikeu.common.context.BaseContext;
import com.ikeu.common.result.Result;
import com.ikeu.model.dto.ChatSendDTO;
import com.ikeu.model.vo.ChatVO;
import com.ikeu.model.vo.ContactVO;
import com.ikeu.server.annotation.RequireCertify;
import com.ikeu.server.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 用户与配送员实时聊天接口，提供联系人列表、聊天记录、消息收发等功能。
 * @author ikeu
 * @since 2025/05/26
 */
@Slf4j
@Tag(name = "用户端-聊天模块接口", description = "用户与配送员实时聊天")
@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
@Validated
public class ChatController {

    private final ChatService chatService;

    /**
     * 获取当前用户的联系人列表。
     *
     * <p>委托 {@link ChatService#getContacts} 查询与当前用户有过聊天记录的所有用户，
     * 按最近消息时间倒序排列，返回包含对方用户信息和最后一条消息摘要的 ContactVO 列表。
     *
     * @return 联系人列表
     */
    @RequireCertify
    @Operation(summary = "获取联系人列表")
    @GetMapping("/contacts")
    public Result<List<ContactVO>> getContacts() {
        Long userId = BaseContext.getCurrentId();
        return Result.success(chatService.getContacts(userId));
    }

    /**
     * 获取与指定用户的聊天记录，支持分页。
     *
     * <p>委托 {@link ChatService#getHistory} 查询双方之间的所有消息，
     * 按发送时间正序排列（旧消息在前），支持分页滚动加载历史消息。
     *
     * @param targetUserId 目标用户ID
     * @param page 页码，默认1
     * @param size 每页条数，默认20
     * @return 聊天记录列表
     */
    @RequireCertify
    @Operation(summary = "获取与指定用户的聊天记录")
    @GetMapping("/history/{targetUserId}")
    public Result<List<ChatVO>> getHistory(
            @PathVariable Long targetUserId,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") @Min(1) int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        Long userId = BaseContext.getCurrentId();
        return Result.success(chatService.getHistory(userId, targetUserId, page, size));
    }

    /**
     * 标记某发送者的所有未读消息为已读。
     *
     * <p>委托 {@link ChatService#markRead} 将当前用户与指定发送者之间所有未读消息
     * 的 is_read 字段批量更新为 1。
     *
     * @param senderId 发送者ID
     * @return 操作结果
     */
    @RequireCertify
    @Operation(summary = "标记某发送者的消息为已读")
    @PutMapping("/read/{senderId}")
    public Result<Void> markRead(@PathVariable Long senderId) {
        Long userId = BaseContext.getCurrentId();
        chatService.markRead(userId, senderId);
        return Result.success();
    }

    /**
     * 删除自己发送的消息。
     *
     * <p>委托 {@link ChatService#deleteMessage} 校验消息归属（只能删除自己的消息）
     * 后将消息标记为已删除状态（软删除）。
     *
     * @param messageId 消息ID
     * @return 操作结果
     */
    @RequireCertify
    @Operation(summary = "删除自己发送的消息")
    @DeleteMapping("/message/{messageId}")
    public Result<Void> deleteMessage(@PathVariable Long messageId) {
        Long userId = BaseContext.getCurrentId();
        chatService.deleteMessage(userId, messageId);
        return Result.success();
    }

    /**
     * 撤回自己发送的消息（5分钟内可撤回）。
     *
     * <p>委托 {@link ChatService#recallMessage} 校验消息归属和发送时间是否在5分钟内，
     * 满足条件则将消息内容替换为撤回提示文本。
     *
     * @param messageId 消息ID
     * @return 操作结果
     */
    @RequireCertify
    @Operation(summary = "撤回自己发送的消息（5分钟内）")
    @PutMapping("/message/recall/{messageId}")
    public Result<Void> recallMessage(@PathVariable Long messageId) {
        Long userId = BaseContext.getCurrentId();
        chatService.recallMessage(userId, messageId);
        return Result.success();
    }

    /**
     * 通过 STOMP WebSocket 接收并处理用户发送的聊天消息。
     *
     * <p>优先从 {@link BaseContext} ThreadLocal 获取发送者ID（由 AuthChannelInterceptor 注入），
     * 回退到直接从 STOMP session attributes 中提取 userId。
     * 获取到 senderId 后委托 {@link ChatService#sendMessage} 持久化消息并推送给接收者。
     *
     * @param dto 聊天消息DTO（接收者ID、消息内容、消息类型）
     * @param headerAccessor STOMP 消息头访问器
     */
    @MessageMapping("/chat.send")
    public void handleChatMessage(@Payload @Valid ChatSendDTO dto, SimpMessageHeaderAccessor headerAccessor) {
        Long senderId = BaseContext.getCurrentId();
        if (senderId != null) {
            log.debug("Got senderId from ThreadLocal: {}", senderId);
        } else {
            senderId = extractUserIdFromSession(headerAccessor);
            log.debug("Got senderId from session attributes: {}", senderId);
        }
        chatService.sendMessage(senderId, dto.getReceiverId(), dto.getContent(), dto.getMessageType());
    }

    /**
     * 从 STOMP session attributes 中提取用户ID（回退方案）。
     *
     * <p>遍历 session attributes 中的 "userId" 键，兼容 Long、Integer、Number 类型。
     * 仅在 ThreadLocal 中无 userId 时作为回退使用。
     *
     * @param headerAccessor STOMP 消息头访问器
     * @return 用户ID，提取失败返回 null
     */
    private Long extractUserIdFromSession(SimpMessageHeaderAccessor headerAccessor) {
        if (headerAccessor == null) return null;
        Map<String, Object> sessionAttrs = headerAccessor.getSessionAttributes();
        if (sessionAttrs == null) return null;
        Object uid = sessionAttrs.get("userId");
        if (uid instanceof Long l) return l;
        if (uid instanceof Integer i) return i.longValue();
        if (uid instanceof Number n) return n.longValue();
        return null;
    }
}
