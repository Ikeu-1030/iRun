package com.ikeu.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "系统配置项VO")
public class SystemConfigVO implements Serializable {

    /** ID */
    @Schema(description = "ID")
    private Long id;

    /** 配置键 */
    @Schema(description = "配置键")
    private String configKey;

    /** 配置值 */
    @Schema(description = "配置值")
    private String configValue;

    /** 配置分组 */
    @Schema(description = "配置分组")
    private String configGroup;

    /** 值类型 */
    @Schema(description = "值类型")
    private String valueType;

    /** 描述 */
    @Schema(description = "描述")
    private String description;

    /** 最后更新时间 */
    @Schema(description = "最后更新时间")
    private LocalDateTime updatedAt;
}
