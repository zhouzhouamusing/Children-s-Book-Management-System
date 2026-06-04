package com.kidsbook.dto;

import lombok.Data;
import java.util.List;

@Data
public class SysRoleDTO {
    private String code;
    private String name;
    private Integer level;
    private String description;
    private Integer status;
}
