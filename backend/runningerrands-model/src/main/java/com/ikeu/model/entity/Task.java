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
 * 任务表实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("task")
public class Task implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 任务编号（业务唯一，如T202601010001） */
    @TableField("task_no")
    private String taskNo;

    /** 发布者ID，关联user表的id */
    @TableField("publisher_id")
    private Long publisherId;

    /** 任务大类 */
    @TableField("type")
    private String type;

    /** 任务小类 */
    @TableField("sub_type")
    private String subType;

    /** 任务规格JSON */
    @TableField("task_specs")
    private String taskSpecs;

    /** 公开任务描述 */
    @TableField("public_desc")
    private String publicDesc;

    /** 私密备注（仅接单员可见） */
    @TableField("private_note")
    private String privateNote;

    /** 取件码 */
    @TableField("pickup_code")
    private String pickupCode;

    /** 报酬金额（合计 = 小费 + 配送费 + 预估商品费） */
    @TableField("reward")
    private BigDecimal reward;

    /** 小费 */
    @TableField("tip")
    private BigDecimal tip;

    /** 配送费 */
    @TableField("delivery_fee")
    private BigDecimal deliveryFee;

    /** 预估商品费 */
    @TableField("product_cost")
    private BigDecimal productCost;

    /** 取件地址 */
    @TableField("pickup_address")
    private String pickupAddress;

    /** 取件地址经度 */
    @TableField("pickup_lng")
    private BigDecimal pickupLng;

    /** 取件地址纬度 */
    @TableField("pickup_lat")
    private BigDecimal pickupLat;

    /** 送达地址 */
    @TableField("delivery_address")
    private String deliveryAddress;

    /** 送达地址经度 */
    @TableField("delivery_lng")
    private BigDecimal deliveryLng;

    /** 送达地址纬度 */
    @TableField("delivery_lat")
    private BigDecimal deliveryLat;

    /** 任务图片URL数组（JSON格式） */
    @TableField("image_urls")
    private String imageUrls;

    /** 任务过期时间 */
    @TableField("expire_time")
    private LocalDateTime expireTime;

    /** 状态：1-待接单，2-已接单，3-配送中，4-待确认，5-已完成，6-已取消 */
    @TableField("status")
    private Integer status;

    /** 要求接单人性别：男/女/NULL不限 */
    @TableField("require_sex")
    private String requireSex;

    /** 收货联系人 */
    @TableField("contact_name")
    private String contactName;

    /** 收货联系电话 */
    @TableField("contact_phone")
    private String contactPhone;

    /** 取消原因 */
    @TableField("cancel_reason")
    private String cancelReason;

    /** 创建时间 */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间 */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

}