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
 * 任务执行订单表实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("task_order")
public class TaskOrder implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联的任务ID */
    @TableField("task_id")
    private Long taskId;

    /** 关联的跑腿员ID */
    @TableField("runner_id")
    private Long runnerId;

    /** 订单状态：1-待取货，2-配送中，3-待确认，4-已完成，5-已取消 */
    @TableField("status")
    private Integer status;

    /** 接单时间 */
    @TableField("accept_time")
    private LocalDateTime acceptTime;

    /** 取货时间 */
    @TableField("pickup_time")
    private LocalDateTime pickupTime;

    /** 送达时间 */
    @TableField("deliver_time")
    private LocalDateTime deliverTime;

    /** 确认完成时间 */
    @TableField("confirm_time")
    private LocalDateTime confirmTime;

    /** 预计送达时间（接单时计算） */
    @TableField("expect_finish_time")
    private LocalDateTime expectFinishTime;

    /** 取货凭证图片JSON数组 */
    @TableField("pickup_proof_img")
    private String pickupProofImgs;

    /** 送达凭证图片JSON数组 */
    @TableField("deliver_proof_img")
    private String deliverProofImgs;

    /** 取消原因（Status = 5） */
    @TableField("cancel_reason")
    private String cancelReason;

    /** 取消时间 */
    @TableField("cancel_time")
    private LocalDateTime cancelTime;

    /** 是否标记为删除：0-否，1-是 */
    @TableField("is_deleted")
    private Integer isDeleted;

}