package com.ikeu.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志表实体类，映射 operation_log 表。
 *
 * <p>记录管理员的关键操作：操作模块、动作、描述、请求方法/URL/参数、
 * 操作IP及时间，敏感字段自动脱敏。
 *
 * @author ikeu
 * @since 2026/06/22
 */
@Data
@TableName("operation_log")
public class OperationLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long adminId;
    private String adminName;
    private String module;
    private String action;
    private String description;
    private String requestMethod;
    private String requestUrl;
    private String requestParams;
    private String ip;
    private LocalDateTime createdAt;
}
