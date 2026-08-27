package com.csa.official.modules.sys.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CarouselSaveRequest {

    @Positive(message = "轮播图 ID 必须大于 0")
    private Long id;

    @NotBlank(message = "轮播图标题不能为空")
    @Size(max = 128, message = "轮播图标题不能超过 128 个字符")
    private String title;

    @NotBlank(message = "轮播图图片地址不能为空")
    @Size(max = 500, message = "轮播图图片地址不能超过 500 个字符")
    private String imgUrl;

    @Size(max = 500, message = "轮播图跳转地址不能超过 500 个字符")
    private String targetUrl;

    @Min(value = -100000, message = "轮播图排序值不能小于 -100000")
    @Max(value = 100000, message = "轮播图排序值不能大于 100000")
    private Integer sortOrder;

    @Min(value = 0, message = "轮播图状态只能是 0 或 1")
    @Max(value = 1, message = "轮播图状态只能是 0 或 1")
    private Integer status;
}
