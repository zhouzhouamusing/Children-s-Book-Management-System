package com.kidsbook.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("reader_appeal")
public class ReaderAppeal {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long readerId;
    private String readerName;
    private String type;
    private Long relatedId;
    private String title;
    private String content;
    private String evidenceUrls;
    private String status;
    private Long adminId;
    private String adminReply;
    private LocalDateTime resolveTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
