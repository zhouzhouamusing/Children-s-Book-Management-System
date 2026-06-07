package com.kidsbook.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kidsbook.entity.*;
import com.kidsbook.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PermissionCacheService {

    private final SysUserRoleMapper userRoleMapper;
    private final SysRolePermissionMapper rolePermissionMapper;
    private final SysPermissionMapper permissionMapper;
    private final SysRoleMapper roleMapper;

    private final ConcurrentHashMap<String, List<String>> permCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, List<String>> roleCache = new ConcurrentHashMap<>();

    public List<String> getPermissions(String userType, Long userId) {
        String key = userType + ":" + userId;
        return permCache.computeIfAbsent(key, k -> loadPermissions(userType, userId));
    }

    public List<String> getRoleCodes(String userType, Long userId) {
        String key = userType + ":" + userId;
        return roleCache.computeIfAbsent(key, k -> loadRoleCodes(userType, userId));
    }

    private List<String> loadRoleCodes(String userType, Long userId) {
        List<SysUserRole> userRoles = userRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>()
                        .eq(SysUserRole::getUserType, userType)
                        .eq(SysUserRole::getUserId, userId));
        if (userRoles.isEmpty()) return Collections.emptyList();

        List<Long> roleIds = userRoles.stream().map(SysUserRole::getRoleId).collect(Collectors.toList());
        List<SysRole> roles = roleMapper.selectList(
                new LambdaQueryWrapper<SysRole>()
                        .in(SysRole::getId, roleIds)
                        .eq(SysRole::getStatus, 1));
        return roles.stream().map(SysRole::getCode).collect(Collectors.toList());
    }

    private List<String> loadPermissions(String userType, Long userId) {
        List<SysUserRole> userRoles = userRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>()
                        .eq(SysUserRole::getUserType, userType)
                        .eq(SysUserRole::getUserId, userId));
        if (userRoles.isEmpty()) return Collections.emptyList();

        List<Long> roleIds = userRoles.stream().map(SysUserRole::getRoleId).collect(Collectors.toList());

        List<SysRolePermission> rolePerms = rolePermissionMapper.selectList(
                new LambdaQueryWrapper<SysRolePermission>()
                        .in(SysRolePermission::getRoleId, roleIds));
        if (rolePerms.isEmpty()) return Collections.emptyList();

        List<Long> permIds = rolePerms.stream().map(SysRolePermission::getPermissionId).distinct().collect(Collectors.toList());

        List<SysPermission> perms = permissionMapper.selectList(
                new LambdaQueryWrapper<SysPermission>()
                        .in(SysPermission::getId, permIds)
                        .eq(SysPermission::getStatus, 1));

        return perms.stream().map(SysPermission::getCode).collect(Collectors.toList());
    }

    public void invalidateUser(String userType, Long userId) {
        String key = userType + ":" + userId;
        permCache.remove(key);
        roleCache.remove(key);
    }

    public void invalidateAll() {
        permCache.clear();
        roleCache.clear();
    }
}
