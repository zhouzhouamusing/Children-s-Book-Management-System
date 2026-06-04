package com.kidsbook.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("reader_account")
public class ReaderAccount {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String password;
    private Long readerId;
    private String email;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
