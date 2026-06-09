package com.kidsbook.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PermissionRequest {
    @NotBlank(message = "权限编码不能为空")
    @Size(max = 100, message = "权限编码不能超过100字符")
    private String code;

    @NotBlank(message = "权限名称不能为空")
    @Size(max = 100, message = "权限名称不能超过100字符")
    private String name;

    @NotBlank(message = "权限类型不能为空")
    private String type;

    private Long parentId;

    @Size(max = 200, message = "路径不能超过200字符")
    private String path;

    @Size(max = 50, message = "图标名不能超过50字符")
    private String icon;

    private Integer sortOrder;

    private Integer status;
}
