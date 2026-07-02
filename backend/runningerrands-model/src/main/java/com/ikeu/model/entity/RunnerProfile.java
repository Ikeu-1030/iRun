package com.ikeu.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 配送员档案表实体类，映射 runner_profile 表。
 *
 * <p>包含配送员认证状态、信用分、接单统计（总单数/成功数/平均评分）、
 * 在线状态、最大接单数、当前接单数、封禁信息等字段。
 *
 * @author ikeu
 * @since 2026/06/22
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("runner_profile")
public class RunnerProfile implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联的用户ID */
    @TableField("user_id")
    private Long userId;

    /** 真实姓名 */
    @TableField("real_name")
    private String realName;

    /** 认证状态：0-未认证，1-审核中，2-已认证，3-认证驳回 */
    @TableField("verify_status")
    private Integer verifyStatus;

    /** 审核备注（驳回原因） */
    @TableField("verify_remark")
    private String verifyRemark;

    /** 信用分 */
    @TableField("credit_score")
    private Integer creditScore;

    /** 历史总接单数 */
    @TableField("total_orders")
    private Integer totalOrders;

    /** 成功完成单数 */
    @TableField("success_orders")
    private Integer successOrders;

    /** 平均评分（1.0~5.0） */
    @TableField("avg_rating")
    private BigDecimal avgRating;

    /** 是否在线接单：0-离线，1-在线 */
    @TableField("is_online")
    private Integer isOnline;

    /** 最大同时接单数 */
    @TableField("max_concurrent_orders")
    private Integer maxConcurrentOrders;

    /** 当前进行中的订单数（接单后+1，完成/取消后-1） */
    @TableField("current_orders")
    private Integer currentOrders;

    /** 是否禁止接单：0-正常 1-禁止 */
    @TableField("is_banned")
    private Integer isBanned;

    /** 禁止接单截止时间（信用分冻结3天后恢复） */
    @TableField("ban_until")
    private LocalDateTime banUntil;

    /** 创建时间 */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间 */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}