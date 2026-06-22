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
 * 管理员表实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("admin")
public class Admin implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 姓名 */
    @TableField("name")
    private String name;

    /** 用户名 */
    @TableField("username")
    private String username;

    /** 密码 */
    @TableField("password")
    private String password;

    /** 手机号 */
    @TableField("phone")
    private String phone;

    /** 性别 */
    @TableField("sex")
    private String sex;

    /** 身份证号 */
    @TableField("id_number")
    private String idNumber;

    /** 角色：1:超管 2:运营 3:财务 */
    @TableField("role")
    private Integer role;

    /** 状态：0-禁用，1-正常 */
    @TableField("status")
    private Integer status;

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