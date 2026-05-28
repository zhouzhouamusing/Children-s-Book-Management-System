package com.kidsbook.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("reader_monthly_stats")
public class ReaderMonthlyStats {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long readerId;

    private String yearMonth;

    private Integer borrowCount;

    private Integer returnCount;

    private Integer readingDays;

    private Integer pointsEarned;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
