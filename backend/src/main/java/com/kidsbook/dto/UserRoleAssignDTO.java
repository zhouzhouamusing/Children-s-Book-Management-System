package com.kidsbook.dto;

import lombok.Data;
import java.util.List;

@Data
public class UserRoleAssignDTO {
    private List<Long> roleIds;
}
