package com.kidsbook.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("reader_points_log")
public class ReaderPointsLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long readerId;

    private Integer points;

    private String type;

    private String description;

    private Long borrowRecordId;

    private LocalDateTime createTime;
}
