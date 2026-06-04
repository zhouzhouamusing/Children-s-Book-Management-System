package com.kidsbook.dto;

import lombok.Data;
import java.util.List;

@Data
public class MenuVO {
    private Long id;
    private Long parentId;
    private String name;
    private String path;
    private String icon;
    private Integer sortOrder;
    private String permissionCode;
    private List<MenuVO> children;
}
