package com.kidsbook.dto;

import lombok.Data;
import java.util.List;

@Data
public class RolePermissionAssignDTO {
    private List<Long> permissionIds;
}
