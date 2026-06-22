package com.ikeu.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "设置支付密码请求DTO（首次设置无需身份校验）")
public class SetPayPasswordDTO implements Serializable {

    @NotBlank(message = "支付密码不能为空")
    @Pattern(regexp = "^\\d{6}$", message = "支付密码必须为6位数字")
    @Schema(description = "支付密码（6位数字）")
    private String payPassword;
}
