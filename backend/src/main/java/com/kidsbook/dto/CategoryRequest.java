package com.kidsbook.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import lombok.Data;

@Data
public class CategoryRequest {
    private Long id;

    @NotBlank(message = "分类名称不能为空")
    private String name;

    private String icon;
    private String color;

    @Min(value = 0, message = "最小年龄不能小于0")
    private Integer ageRangeMin;

    @Max(value = 18, message = "最大年龄不能超过18")
    private Integer ageRangeMax;

    private Integer sortOrder;
    private String description;
    private Integer status;
}
