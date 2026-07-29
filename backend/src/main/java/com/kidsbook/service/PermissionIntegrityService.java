package com.kidsbook.service;

import com.kidsbook.dto.PermissionIntegrityReport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionIntegrityService {

    private final JdbcTemplate jdbcTemplate;
    private final PermissionCacheService permissionCacheService;

    public PermissionIntegrityReport checkIntegrity() {
        PermissionIntegrityReport report = new PermissionIntegrityReport();

        Integer orphanRp = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_role_permission rp " +
                "WHERE NOT EXISTS (SELECT 1 FROM sys_permission p WHERE p.id = rp.permission_id) " +
                "OR NOT EXISTS (SELECT 1 FROM sys_role r WHERE r.id = rp.role_id)", Integer.class);
        report.setOrphanRolePermissionCount(orphanRp != null ? orphanRp : 0);

        Integer orphanUr = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_user_role ur " +
                "WHERE NOT EXISTS (SELECT 1 FROM sys_role r WHERE r.id = ur.role_id)", Integer.class);
        report.setOrphanUserRoleCount(orphanUr != null ? orphanUr : 0);

        Integer unassigned = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_permission p " +
                "WHERE p.type = 'button' AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp WHERE rp.permission_id = p.id)", Integer.class);
        report.setUnassignedPermissionCount(unassigned != null ? unassigned : 0);

        boolean healthy = report.getOrphanRolePermissionCount() == 0 && report.getOrphanUserRoleCount() == 0;
        report.setHealthy(healthy);
        report.setMessage(healthy ? "权限数据完整，无异常" : "发现数据完整性问题，建议修复");
        return report;
    }

    @Transactional
    public PermissionIntegrityReport repairAndReport() {
        int deletedRp = jdbcTemplate.update(
                "DELETE FROM sys_role_permission WHERE " +
                "NOT EXISTS (SELECT 1 FROM sys_permission WHERE sys_permission.id = sys_role_permission.permission_id) " +
                "OR NOT EXISTS (SELECT 1 FROM sys_role WHERE sys_role.id = sys_role_permission.role_id)");

        int deletedUr = jdbcTemplate.update(
                "DELETE FROM sys_user_role WHERE " +
                "NOT EXISTS (SELECT 1 FROM sys_role WHERE sys_role.id = sys_user_role.role_id)");

        log.info("权限数据修复完成: 删除无效角色-权限关联 {} 条, 删除无效用户-角色关联 {} 条", deletedRp, deletedUr);

        permissionCacheService.invalidateAll();

        return checkIntegrity();
    }
}
