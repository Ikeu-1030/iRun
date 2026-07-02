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
 * 用户表实体类，映射 user 表。
 *
 * <p>包含用户基本信息（用户名、密码、手机号、头像、昵称）、
 * 实名认证信息（真实姓名、学号、学生证照片）、账户余额、
 * 注册类型（手机/微信）、状态、最后登录时间等字段。
 *
 * @author ikeu
 * @since 2026/06/22
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("user")
public class User implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户名 */
    @TableField("username")
    private String username;

    /** 密码 */
    @TableField("password")
    private String password;

    /** 支付密码 */
    @TableField("pay_password")
    private String payPassword;

    /** 用户昵称 */
    @TableField("nickname")
    private String nickname;

    /** 头像URL */
    @TableField("avatar_url")
    private String avatarUrl;

    /** 手机号 */
    @TableField("phone")
    private String phone;

    /** 性别：男/女 */
    @TableField("sex")
    private String sex;

    /** 微信openid */
    @TableField("openid")
    private String openid;

    /** 微信unionid */
    @TableField("unionid")
    private String unionid;

    /** 真实姓名（提现/认证用） */
    @TableField("real_name")
    private String realName;

    /** 学号 */
    @TableField("student_id")
    private String studentId;

    /** 认证照片URL（学生证照片） */
    @TableField("certify_img")
    private String certifyImg;

    /** 认证审核备注（驳回原因） */
    @TableField("certify_remark")
    private String certifyRemark;

    /** 账户余额（元） */
    @TableField("balance")
    private BigDecimal balance;

    /** 学院名 */
    @TableField("campus")
    private String campus;

    /** 个性签名 */
    @TableField("signature")
    private String signature;

    /** 用户所在经度 */
    @TableField("lng")
    private BigDecimal lng;

    /** 用户所在纬度 */
    @TableField("lat")
    private BigDecimal lat;

    /** 注册类型：1-手机号，2-微信 */
    @TableField("register_type")
    private Integer registerType;

    /** 状态：0-禁用，1-正常 */
    @TableField("status")
    private Integer status;

    /** 是否认证：0-未认证 1-已认证 */
    @TableField("is_certify")
    private Integer isCertify;

    /** 最后登录时间 */
    @TableField("last_login_time")
    private LocalDateTime lastLoginTime;

    /** 创建时间 */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间 */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}