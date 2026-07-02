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
 * 信用分变更日志表实体类，映射 credit_log 表。
 *
 * <p>记录跑腿员每次信用分变更的详情：变更分数、变更前后值、
 * 变更原因类型（超时/投诉/手动/奖励/恢复）、关联订单ID。
 *
 * @author ikeu
 * @since 2026/06/22
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("credit_log")
public class CreditLog implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("runner_id")
    private Long runnerId;

    @TableField("delta")
    private Integer delta;

    @TableField("score_before")
    private Integer scoreBefore;

    @TableField("score_after")
    private Integer scoreAfter;

    @TableField("reason_type")
    private String reasonType;

    @TableField("reason_detail")
    private String reasonDetail;

    @TableField("related_order_id")
    private Long relatedOrderId;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
