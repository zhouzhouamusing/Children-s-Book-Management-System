package com.kidsbook.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kidsbook.entity.*;
import com.kidsbook.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RbacService {
    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysRolePermissionMapper sysRolePermissionMapper;
    private final SysPermissionMapper sysPermissionMapper;
    private final SysMenuMapper sysMenuMapper;

    public List<String> getRolesForUser(String userType, Long userId) {
        List<String> roles = sysUserRoleMapper.selectRoleCodesByUser(userType, userId);
        return roles != null ? roles : Collections.emptyList();
    }

    public Set<String> getEffectivePermissions(String userType, Long userId) {
        List<Integer> userLevels = sysUserRoleMapper.selectRoleLevelsByUser(userType, userId);
        if (userLevels == null || userLevels.isEmpty()) {
            return Collections.emptySet();
        }

        int maxLevel = userLevels.stream().mapToInt(Integer::intValue).max().orElse(0);

        // Get all roles at or below the user's max level (inheritance)
        List<SysRole> inheritedRoles = sysRoleMapper.selectList(
            new LambdaQueryWrapper<SysRole>()
                .le(SysRole::getLevel, maxLevel)
                .eq(SysRole::getStatus, 1)
        );

        if (inheritedRoles.isEmpty()) {
            return Collections.emptySet();
        }

        List<Long> roleIds = inheritedRoles.stream()
            .map(SysRole::getId)
            .collect(Collectors.toList());

        List<SysRolePermission> rolePerms = sysRolePermissionMapper.selectList(
            new LambdaQueryWrapper<SysRolePermission>()
                .in(SysRolePermission::getRoleId, roleIds)
        );

        if (rolePerms.isEmpty()) {
            return Collections.emptySet();
        }

        List<Long> permIds = rolePerms.stream()
            .map(SysRolePermission::getPermissionId)
            .distinct()
            .collect(Collectors.toList());

        List<SysPermission> permissions = sysPermissionMapper.selectList(
            new LambdaQueryWrapper<SysPermission>()
                .in(SysPermission::getId, permIds)
        );

        return permissions.stream()
            .map(SysPermission::getCode)
            .collect(Collectors.toSet());
    }

    public List<SysRole> getAllRolesForUser(String userType, Long userId) {
        List<SysUserRole> userRoles = sysUserRoleMapper.selectList(
            new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getUserType, userType)
                .eq(SysUserRole::getUserId, userId)
        );
        if (userRoles.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> roleIds = userRoles.stream()
            .map(SysUserRole::getRoleId)
            .collect(Collectors.toList());
        return sysRoleMapper.selectList(
            new LambdaQueryWrapper<SysRole>()
                .in(SysRole::getId, roleIds)
                .eq(SysRole::getStatus, 1)
        );
    }

    public List<SysMenu> getUserMenus(Set<String> permissionCodes) {
        if (permissionCodes == null || permissionCodes.isEmpty()) {
            return Collections.emptyList();
        }
        List<SysMenu> allMenus = sysMenuMapper.selectList(
            new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getStatus, 1)
                .eq(SysMenu::getType, 1)
                .orderByAsc(SysMenu::getSortOrder)
        );
        return allMenus.stream()
            .filter(menu -> menu.getPermissionCode() == null ||
                           permissionCodes.contains(menu.getPermissionCode()))
            .collect(Collectors.toList());
    }

    public boolean hasPermission(String userType, Long userId, String permissionCode) {
        Set<String> perms = getEffectivePermissions(userType, userId);
        return perms.contains(permissionCode);
    }
}
