package com.kidsbook.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AppealRequest {
    @NotBlank(message = "申诉类型不能为空")
    private String type;

    @NotBlank(message = "申诉标题不能为空")
    @Size(max = 200, message = "标题不能超过200字")
    private String title;

    @NotBlank(message = "申诉内容不能为空")
    @Size(max = 2000, message = "内容不能超过2000字")
    private String content;

    private Long relatedId;

    @Size(max = 1000, message = "证据链接过长")
    private String evidenceUrls;
}
