package com.ikeu.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "任务列表项VO")
public class TaskListVO implements Serializable {

    @Schema(name = "任务ID")
    private Long taskId;

    @Schema(name = "发布者ID")
    private Long publisherId;

    @Schema(name = "任务编号")
    private String taskNo;

    @Schema(name = "任务大类")
    private String type;

    @Schema(name = "任务小类")
    private String subType;

    @Schema(name = "任务规格JSON")
    private String taskSpecs;

    @Schema(name = "公开任务描述")
    private String publicDesc;

    @Schema(name = "任务状态：1-待接单 2-已接单 3-配送中 4-待确认 5-已完成 6-已取消")
    private Integer status;

    @Schema(name = "报酬（合计）")
    private BigDecimal reward;

    @Schema(name = "小费")
    private BigDecimal tip;

    @Schema(name = "配送费")
    private BigDecimal deliveryFee;

    @Schema(name = "预估商品费")
    private BigDecimal productCost;

    @Schema(name = "取件地址")
    private String pickupAddress;

    @Schema(name = "取件地址经度")
    private BigDecimal pickupLng;

    @Schema(name = "取件地址纬度")
    private BigDecimal pickupLat;

    @Schema(name = "送达地址")
    private String deliveryAddress;

    @Schema(name = "送达地址经度")
    private BigDecimal deliveryLng;

    @Schema(name = "送达地址纬度")
    private BigDecimal deliveryLat;

    @Schema(name = "任务过期时间")
    private LocalDateTime expireTime;

    @Schema(name = "发布时间")
    private LocalDateTime publishTime;

    @Schema(name = "发布者昵称")
    private String publisherNickname;

    @Schema(name = "发布者用户名")
    private String publisherUsername;

    @Schema(name = "发布者头像URL")
    private String publisherAvatar;

    @Schema(name = "要求接单人性别")
    private String requireSex;

    @Schema(name = "收货联系人")
    private String contactName;

    @Schema(name = "收货联系电话")
    private String contactPhone;

    @Schema(name = "任务图片URL列表")
    private List<String> imageUrls;

    @Schema(name = "当前用户是否已对该任务发布评价")
    private Boolean hasReviewed;
}