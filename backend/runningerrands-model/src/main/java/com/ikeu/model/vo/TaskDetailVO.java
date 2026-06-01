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
@Schema(description = "任务详情VO")
public class TaskDetailVO implements Serializable {

    @Schema(name = "任务ID")
    private Long taskId;

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

    @Schema(name = "私密备注")
    private String privateNote;

    @Schema(name = "报酬")
    private BigDecimal reward;

    @Schema(name = "取件地址")
    private String pickupAddress;

    @Schema(name = "送达地址")
    private String deliveryAddress;

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

    @Schema(name = "任务图片URL列表")
    private List<String> imageUrls;

    @Schema(name = "取件码")
    private String pickupCode;

    @Schema(name = "是否有取件码")
    private Boolean hasPickupCode;

    @Schema(name = "取件地址经度")
    private BigDecimal pickupLng;

    @Schema(name = "取件地址纬度")
    private BigDecimal pickupLat;

    @Schema(name = "送达地址经度")
    private BigDecimal deliveryLng;

    @Schema(name = "送达地址纬度")
    private BigDecimal deliveryLat;

    @Schema(name = "要求接单人性别")
    private String requireSex;

    @Schema(name = "收货联系人")
    private String contactName;

    @Schema(name = "收货联系电话")
    private String contactPhone;

    @Schema(name = "任务状态")
    private Integer status;

    @Schema(name = "是否本人发布")
    private Boolean isOwner;

    @Schema(description = "取消原因")
    private String cancelReason;

    @Schema(description = "取消时间")
    private LocalDateTime cancelTime;
}