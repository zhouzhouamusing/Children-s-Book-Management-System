package com.kidsbook.dto;

import lombok.Data;

@Data
public class PermissionIntegrityReport {
    private int orphanRolePermissionCount;
    private int orphanUserRoleCount;
    private int unassignedPermissionCount;
    private boolean healthy;
    private String message;
}
