package com.kidsbook.dto;

import lombok.Data;
import java.util.List;

@Data
public class LoginResponse {
    private String token;
    private String nickname;
    private String avatar;
    private String role;
    private List<String> roles;
    private List<String> permissions;
    private Long readerId;
    private Boolean suspended = false;
}
