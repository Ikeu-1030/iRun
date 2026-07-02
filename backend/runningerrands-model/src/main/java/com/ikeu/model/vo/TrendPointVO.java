package com.ikeu.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "趋势数据点VO")
public class TrendPointVO implements Serializable {

    @Schema(description = "日期")
    private String date;

    @Schema(description = "数量")
    private Long value;
}
