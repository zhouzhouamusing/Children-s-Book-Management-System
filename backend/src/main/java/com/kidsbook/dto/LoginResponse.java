package com.kidsbook.dto;

import lombok.Data;

@Data
public class LoginResponse {
    private String token;
    private String nickname;
    private String avatar;
    private String role;
    private Long readerId;
}
