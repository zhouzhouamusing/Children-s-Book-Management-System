package com.kidsbook.dto;

import lombok.Data;

import java.util.List;

@Data
public class LoginResponse {
    private String token;
    private String nickname;
    private String avatar;
    private String role;
    private Long readerId;
    private List<String> roles;
    private List<String> permissions;
}
