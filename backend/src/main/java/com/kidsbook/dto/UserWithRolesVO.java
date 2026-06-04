package com.kidsbook.dto;

import com.kidsbook.entity.SysRole;
import lombok.Data;
import java.util.List;

@Data
public class UserWithRolesVO {
    private Long userId;
    private String username;
    private String displayName;
    private String userType;
    private List<SysRole> roles;
}
