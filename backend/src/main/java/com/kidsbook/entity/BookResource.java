package com.kidsbook.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("book_resource")
public class BookResource {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long bookId;
    private String fileName;
    private String originalName;
    private String filePath;
    private String fileType;
    private Long fileSize;
    private String mimeType;
    private LocalDateTime createTime;
}
