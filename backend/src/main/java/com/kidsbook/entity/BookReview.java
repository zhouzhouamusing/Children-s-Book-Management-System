package com.kidsbook.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("book_review")
public class BookReview {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long bookId;
    private Long readerId;
    private String readerName;
    private String bookTitle;
    private Integer rating;
    private String content;
    private String status;
    private String adminReply;
    private LocalDateTime replyTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
