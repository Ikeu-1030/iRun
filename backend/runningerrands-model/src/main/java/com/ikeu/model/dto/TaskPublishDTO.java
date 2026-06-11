package com.ikeu.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
@Schema(description = "任务发布请求DTO")
public class TaskPublishDTO implements Serializable {

    @NotNull(message = "任务大类不能为空")
    @Schema(description = "任务大类", example = "代取快递")
    private String type;

    @Schema(description = "任务小类", example = "校内代购")
    private String subType;

    @Schema(description = "任务规格JSON")
    private String taskSpecs;

    @Schema(description = "公开任务描述")
    @Size(max = 256)
    private String publicDesc;

    @Schema(description = "私密备注（仅接单员可见）")
    @Size(max = 256)
    private String privateNote;

    @Schema(description = "取件码（可选）", example = "12-3-4567")
    private String pickupCode;

    @NotNull
    @DecimalMin(value = "0.0", message = "小费不能为负数")
    @DecimalMax(value = "50.0", message = "小费最高不能超过50元")
    @Schema(description = "小费（跑腿员可获得的报酬，可为0）")
    private BigDecimal tip;

    @DecimalMin(value = "0.0", message = "配送费不能为负数")
    @Schema(description = "配送费（基础配送费）")
    private BigDecimal deliveryFee;

    @DecimalMin(value = "0.0", message = "商品费不能为负数")
    @Schema(description = "预估商品费（代购类任务的商品费用）")
    private BigDecimal productCost;

    @Schema(description = "取件地址（可不填，默认'无需取件'）")
    private String pickupAddress;

    @Schema(description = "取件地址经度")
    private BigDecimal pickupLng;

    @Schema(description = "取件地址纬度")
    private BigDecimal pickupLat;

    @Schema(description = "要求接单人性别：男/女，null表示不限")
    private String requireSex;

    @Schema(description = "送达地址ID（可不填）")
    private Long deliveryAddressId;

    @Schema(description = "联系人姓名（不选地址时手动输入）")
    private String contactName;

    @Schema(description = "联系人电话（不选地址时手动输入）")
    private String contactPhone;

    @Size(max = 3)
    @Schema(description = "图片URL列表（可选）")
    private List<String> imageUrls;

    @Min(10) @Max(1440)
    @Schema(description = "任务有效时长（分钟），默认60")
    private Integer expireMinutes = 60;

    @Schema(description = "送达地址经度")
    private BigDecimal deliveryLng;

    @Schema(description = "送达地址纬度")
    private BigDecimal deliveryLat;

    @NotBlank(message = "支付密码不能为空")
    @Schema(description = "支付密码", example = "123456")
    private String payPassword;
}