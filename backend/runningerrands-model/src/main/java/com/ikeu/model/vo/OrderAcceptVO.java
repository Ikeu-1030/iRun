package com.ikeu.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
@Schema(description = "接单响应VO")
public class OrderAcceptVO implements Serializable {

    @Schema(description = "订单ID")
    private Long orderId;

}
