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
 * 任务图片表实体类，映射 task_image 表。
 *
 * <p>存储任务发布时上传的图片URL及排序，与 task 表多对一关联。
 *
 * @author ikeu
 * @since 2026/06/22
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("task_image")
public class TaskImage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联task表的id */
    @TableField("task_id")
    private Long taskId;

    /** 图片URL */
    @TableField("url")
    private String url;

    /** 排序 */
    @TableField("sort_order")
    private Integer sortOrder;

    /** 创建时间 */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
