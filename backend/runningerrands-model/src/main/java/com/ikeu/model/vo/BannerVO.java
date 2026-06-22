package com.ikeu.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "首页轮播图VO")
public class BannerVO {

    @Schema(description = "图片url列表")
    private List<String> images;

    @Schema(description = "轮播间隔秒数")
    private int interval;
}
