package com.kidsbook.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("reader")
public class Reader {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private Integer age;

    private String gender;

    private String parentName;

    private String parentPhone;

    private String status;

    private Integer borrowCount;

    private Integer overdueCount;

    private Integer points;

    private Integer totalReadingDays;

    private String level;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
