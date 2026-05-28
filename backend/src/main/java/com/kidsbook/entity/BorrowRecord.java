package com.kidsbook.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("borrow_record")
public class BorrowRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long readerId;

    private Long bookId;

    private String bookTitle;

    private LocalDate borrowDate;

    private LocalDate dueDate;

    private LocalDate returnDate;

    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
