package com.kidsbook.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("admin_application")
public class AdminApplication {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long readerId;
    private String readerName;
    private String username;
    private String reason;
    private String status;
    private String rejectReason;
    private String approvedBy;
    private LocalDateTime approvedTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
