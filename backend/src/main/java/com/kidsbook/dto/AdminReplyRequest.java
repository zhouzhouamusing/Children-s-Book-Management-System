package com.kidsbook.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AdminReplyRequest {
    @NotBlank(message = "回复内容不能为空")
    private String reply;
}
