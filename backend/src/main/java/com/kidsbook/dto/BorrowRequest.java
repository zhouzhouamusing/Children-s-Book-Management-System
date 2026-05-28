package com.kidsbook.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BorrowRequest {
    @NotNull(message = "读者ID不能为空")
    private Long readerId;

    @NotNull(message = "图书ID不能为空")
    private Long bookId;

    private Integer borrowDays = 14;
}
