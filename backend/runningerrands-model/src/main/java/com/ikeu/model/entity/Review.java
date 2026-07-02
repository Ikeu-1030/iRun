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
 * 评价表实体类，映射 review 表。
 *
 * <p>包含评价者/被评价者、关联任务、评分（1-5）、评价内容、
 * 标签（JSON数组）、父评价ID（支持追评嵌套）等字段。
 *
 * @author ikeu
 * @since 2026/06/22
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("review")
public class Review implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联的任务ID */
    @TableField("task_id")
    private Long taskId;

    /** 评价人ID */
    @TableField("reviewer_id")
    private Long reviewerId;

    /** 被评价人ID */
    @TableField("target_user_id")
    private Long targetUserId;

    /** 评分 1-5 */
    @TableField("rating")
    private Integer rating;

    /** 评价内容 */
    @TableField("content")
    private String content;

    /** 评价标签，如["态度好","速度快"]（JSON格式） */
    @TableField("tags")
    private String tags;

    /** 父评价ID，NULL表示根评价 */
    @TableField("parent_id")
    private Long parentId;

    /** 创建时间 */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}