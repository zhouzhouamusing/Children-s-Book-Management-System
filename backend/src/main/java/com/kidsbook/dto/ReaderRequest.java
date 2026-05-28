package com.kidsbook.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class ReaderRequest {

    private Long id;

    @NotBlank(message = "儿童姓名不能为空")
    private String name;

    @NotNull(message = "年龄不能为空")
    @Min(value = 1, message = "年龄最小为1岁")
    @Max(value = 18, message = "年龄最大为18岁")
    private Integer age;

    @NotBlank(message = "性别不能为空")
    private String gender;

    private String parentName;

    @NotBlank(message = "家长联系方式不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "请输入正确的手机号码")
    private String parentPhone;

    private String remark;
}
