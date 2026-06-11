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
@Schema(description = "订单详情VO")
public class OrderDetailVO implements Serializable {

    @Schema(description = "订单ID")
    private Long orderId;

    @Schema(description = "任务ID")
    private Long taskId;

    @Schema(description = "任务编号")
    private String taskNo;

    @Schema(description = "任务大类：代取快递、代拿餐食、校内代办、代购物品")
    private String type;

    @Schema(description = "任务小类")
    private String subType;

    @Schema(description = "任务规格JSON")
    private String taskSpecs;

    @Schema(description = "公开任务描述")
    private String publicDesc;

    @Schema(description = "私密备注（仅发布者/接单员可见）")
    private String privateNote;

    @Schema(description = "报酬（合计）")
    private BigDecimal reward;

    @Schema(description = "小费")
    private BigDecimal tip;

    @Schema(description = "配送费")
    private BigDecimal deliveryFee;

    @Schema(description = "预估商品费")
    private BigDecimal productCost;

    @Schema(description = "订单状态")
    private Integer orderStatus;

    @Schema(description = "取件地址")
    private String pickupAddress;

    @Schema(description = "送达地址")
    private String deliveryAddress;

    @Schema(description = "取件码")
    private String pickupCode;

    @Schema(description = "任务图片URL列表")
    private List<String> imageUrls;

    @Schema(description = "发布人ID")
    private Long publisherId;

    @Schema(description = "接单人ID")
    private Long runnerId;

    @Schema(description = "收货联系人")
    private String contactName;

    @Schema(description = "收货联系电话")
    private String contactPhone;

    @Schema(description = "发布人手机号")
    private String publisherPhone;

    @Schema(description = "接单人手机号")
    private String runnerPhone;

    @Schema(description = "发布人头像URL")
    private String publisherAvatar;

    @Schema(description = "接单人昵称")
    private String runnerNickname;

    @Schema(description = "接单人用户名")
    private String runnerUsername;

    @Schema(description = "接单人头像URL")
    private String runnerAvatar;

    @Schema(description = "发布人昵称")
    private String publisherNickname;

    @Schema(description = "发布人用户名")
    private String publisherUsername;

    @Schema(description = "接单时间")
    private LocalDateTime acceptTime;

    @Schema(description = "取货时间")
    private LocalDateTime pickupTime;

    @Schema(description = "送达时间")
    private LocalDateTime deliverTime;

    @Schema(description = "确认时间")
    private LocalDateTime confirmTime;

    @Schema(description = "预计完成时间")
    private LocalDateTime expectFinishTime;

    @Schema(description = "取货凭证图片列表")
    private List<String> pickupProofImgs;

    @Schema(description = "送达凭证图片列表")
    private List<String> deliverProofImgs;

    @Schema(description = "是否为发布人")
    private boolean isOwnerPublisher;

    @Schema(description = "是否为接单人")
    private boolean isOwnerRunner;

    @Schema(description = "取消原因")
    private String cancelReason;

    @Schema(description = "取消时间")
    private LocalDateTime cancelTime;
}