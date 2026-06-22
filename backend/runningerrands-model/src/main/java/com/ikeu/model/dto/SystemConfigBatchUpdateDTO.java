package com.ikeu.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * SystemConfigBatchUpdateDTO
 * @author ikeu
 * @since 2026/06/18
 */
@Data
@Schema(description = "批量更新系统配置请求")
public class SystemConfigBatchUpdateDTO implements Serializable {

    @Valid
    @Size(min = 1, message = "更新项列表不能为空")
    @Schema(description = "更新项列表")
    private List<@Valid Item> items;

    /**
     * 单项配置更新
     */
    @Data
    @Schema(description = "单项配置更新")
    public static class Item implements Serializable {

        /** 配置键 */
        @NotBlank(message = "配置键不能为空")
        @Schema(description = "配置键")
        private String configKey;

        /** 配置值 */
        @NotBlank(message = "配置值不能为空")
        @Schema(description = "配置值")
        private String configValue;
    }
}
