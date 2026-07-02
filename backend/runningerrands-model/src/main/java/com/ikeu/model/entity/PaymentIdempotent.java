package com.ikeu.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 支付幂等记录表实体类，映射 payment_idempotent 表。
 *
 * <p>通过唯一幂等键防止支付/退款/结算等操作重复执行，
 * 每条记录在创建后保留 7 天自动清理。
 *
 * @author ikeu
 * @since 2026/06/22
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("payment_idempotent")
public class PaymentIdempotent implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 幂等拦截key */
    @TableField("idempotent_key")
    private String idempotentKey;

    /** 创建时间 */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
