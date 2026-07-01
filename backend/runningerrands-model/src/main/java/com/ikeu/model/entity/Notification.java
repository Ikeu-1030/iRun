package com.ikeu.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 通知表实体类，映射 notification 表。
 *
 * <p>包含通知接收用户、通知类型（系统/订单状态/活动）、
 * 标题、内容、已读状态、关联目标ID等字段。
 *
 * @author ikeu
 * @since 2026/06/22
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("notification")
public class Notification implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    @TableField("user_id")
    private Long userId;

    /** 类型：1-系统通知，2-订单状态，3-活动提醒 */
    @TableField("type")
    private Integer type;

    /** 标题 */
    @TableField("title")
    private String title;

    /** 内容 */
    @TableField("content")
    private String content;

    /** 是否已读：0-未读，1-已读 */
    @TableField("is_read")
    private Integer isRead;

    /** 关联的业务ID，如task_id */
    @TableField("target_id")
    private Long targetId;

    /** 创建时间 */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}