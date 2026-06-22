package com.ikeu.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 交易记录表实体类，映射 transaction_record 表。
 *
 * <p>包含交易用户、关联任务、金额、交易类型（支出/收入/充值/提现/退款）、
 * 交易前后余额等字段。
 *
 * @author ikeu
 * @since 2026/06/22
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("transaction_record")
public class TransactionRecord implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联的用户ID */
    @TableField("user_id")
    private Long userId;

    /** 关联的任务ID */
    @TableField("task_id")
    private Long taskId;

    /** 变动金额（正为收入，负为支出） */
    @TableField("amount")
    private BigDecimal amount;

    /** 类型：1-任务悬赏支出，2-跑腿收入，3-充值，4-提现，5-退款 */
    @TableField("type")
    private Integer type;

    /** 变动前余额 */
    @TableField("balance_before")
    private BigDecimal balanceBefore;

    /** 变动后余额 */
    @TableField("balance_after")
    private BigDecimal balanceAfter;

    /** 创建时间 */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}