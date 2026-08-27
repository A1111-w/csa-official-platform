package com.csa.official.modules.sys.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ContributionAwardRequest {

    @NotNull(message = "成员不能为空")
    private Long userId;

    @NotBlank(message = "贡献类型不能为空")
    @Size(max = 16, message = "贡献类型不正确")
    private String type;

    @NotNull(message = "贡献分值不能为空")
    @DecimalMin(value = "0.01", message = "贡献分值必须大于 0")
    @DecimalMax(value = "99999999.99", message = "贡献分值超出范围")
    @Digits(integer = 8, fraction = 2, message = "贡献分值最多 8 位整数和 2 位小数")
    private BigDecimal score;

    @NotBlank(message = "贡献说明不能为空")
    @Size(max = 500, message = "贡献说明不能超过 500 个字符")
    private String reason;
}
