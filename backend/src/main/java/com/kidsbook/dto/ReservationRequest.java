package com.kidsbook.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReservationRequest {
    @NotNull(message = "图书ID不能为空")
    private Long bookId;
}
