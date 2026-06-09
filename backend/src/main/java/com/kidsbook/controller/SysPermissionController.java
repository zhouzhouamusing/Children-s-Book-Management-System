package com.kidsbook.controller;

import com.kidsbook.annotation.RequirePermission;
import com.kidsbook.common.Result;
import com.kidsbook.dto.PermissionIntegrityReport;
import com.kidsbook.dto.PermissionRequest;
import com.kidsbook.entity.SysPermission;
import com.kidsbook.service.PermissionCacheService;
import com.kidsbook.service.PermissionIntegrityService;
import com.kidsbook.service.SysPermissionService;
import com.kidsbook.util.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sys/permissions")
@RequiredArgsConstructor
public class SysPermissionController {

    private final SysPermissionService permissionService;
    private final PermissionCacheService permissionCacheService;
    private final PermissionIntegrityService integrityService;
    private final JwtUtil jwtUtil;

    @GetMapping("/tree")
    @RequirePermission("permission:view")
    public Result<List<SysPermission>> tree() {
        return Result.success(permissionService.getPermissionTree());
    }

    @GetMapping
    @RequirePermission("permission:view")
    public Result<List<SysPermission>> list() {
        return Result.success(permissionService.getAllPermissions());
    }

    @PostMapping
    @RequirePermission("permission:add")
    public Result<Void> add(@Valid @RequestBody PermissionRequest request) {
        permissionService.addPermission(request);
        return Result.success(null);
    }

    @PutMapping("/{id}")
    @RequirePermission("permission:edit")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody PermissionRequest request) {
        permissionService.updatePermission(id, request);
        return Result.success(null);
    }

    @DeleteMapping("/{id}")
    @RequirePermission("permission:delete")
    public Result<Void> delete(@PathVariable Long id) {
        permissionService.deletePermission(id);
        return Result.success(null);
    }

    @GetMapping("/menus")
    public Result<List<SysPermission>> menus() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getCredentials() == null) {
            return Result.success(List.of());
        }
        String token = auth.getCredentials().toString();
        String userType = jwtUtil.getUserTypeFromToken(token);
        Long userId = jwtUtil.getUserIdFromToken(token);

        List<String> roleCodes = permissionCacheService.getRoleCodes(userType, userId);
        boolean isSuperAdmin = roleCodes.contains("SUPER_ADMIN");

        List<String> permissions = permissionCacheService.getPermissions(userType, userId);
        return Result.success(permissionService.getMenusByUserPermissions(permissions, isSuperAdmin));
    }

    @GetMapping("/integrity-check")
    @RequirePermission("permission:view")
    public Result<PermissionIntegrityReport> integrityCheck() {
        return Result.success(integrityService.checkIntegrity());
    }

    @PostMapping("/integrity-repair")
    @RequirePermission("permission:delete")
    public Result<PermissionIntegrityReport> integrityRepair() {
        return Result.success(integrityService.repairAndReport());
    }
}
