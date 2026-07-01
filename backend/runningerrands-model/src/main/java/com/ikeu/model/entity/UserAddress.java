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
 * 用户地址表实体类，映射 user_address 表。
 *
 * <p>包含联系人姓名/电话/性别、地址详情、经纬度、
 * 是否默认地址等字段，与 user 表多对一关联。
 *
 * @author ikeu
 * @since 2026/06/22
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("user_address")
public class UserAddress implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联的用户ID */
    @TableField("user_id")
    private Long userId;

    /** 联系人 */
    @TableField("contact_name")
    private String contactName;

    /** 联系电话 */
    @TableField("contact_phone")
    private String contactPhone;

    /** 性别 */
    @TableField("sex")
    private String sex;

    /** 详细地址，如：XX宿舍楼XXX室 */
    @TableField("detail")
    private String detail;

    /** 地址经度 */
    @TableField("lng")
    private BigDecimal lng;

    /** 地址纬度 */
    @TableField("lat")
    private BigDecimal lat;

    /** 是否默认地址 */
    @TableField("is_default")
    private Integer isDefault;

    /** 创建时间 */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间 */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

}