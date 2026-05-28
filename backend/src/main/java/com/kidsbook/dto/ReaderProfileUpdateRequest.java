package com.kidsbook.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReaderProfileUpdateRequest {
    @NotBlank(message = "姓名不能为空")
    private String name;

    @NotNull(message = "年龄不能为空")
    @Min(value = 1, message = "年龄最小为1岁")
    @Max(value = 18, message = "年龄最大为18岁")
    private Integer age;

    @NotBlank(message = "性别不能为空")
    private String gender;

    private String parentName;

    @NotBlank(message = "家长手机号不能为空")
    private String parentPhone;
}
