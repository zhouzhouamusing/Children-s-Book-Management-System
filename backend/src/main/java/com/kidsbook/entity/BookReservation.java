package com.kidsbook.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("book_reservation")
public class BookReservation {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long readerId;
    private Long bookId;
    private String bookTitle;
    private LocalDateTime reserveDate;
    private LocalDateTime expireDate;
    private String status;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
