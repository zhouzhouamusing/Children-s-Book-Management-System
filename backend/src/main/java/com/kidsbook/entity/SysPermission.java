package com.kidsbook.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_permission")
public class SysPermission {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String code;
    private String name;
    private String module;
    private String description;
    private String type;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
