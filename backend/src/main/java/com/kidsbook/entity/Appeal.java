package com.kidsbook.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("appeal")
public class Appeal {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long readerId;
    private String readerName;
    private String type;
    private String reason;
    private String evidence;
    private String status;
    private String adminFeedback;
    private String reviewedBy;
    private LocalDateTime reviewedTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
