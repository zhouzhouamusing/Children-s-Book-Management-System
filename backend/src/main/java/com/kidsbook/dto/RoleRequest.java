package com.kidsbook.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RoleRequest {
    @NotBlank(message = "角色编码不能为空")
    @Size(max = 50, message = "角色编码不能超过50字符")
    private String code;

    @NotBlank(message = "角色名称不能为空")
    @Size(max = 50, message = "角色名称不能超过50字符")
    private String name;

    @Size(max = 200, message = "描述不能超过200字符")
    private String description;

    private Integer status;
}
