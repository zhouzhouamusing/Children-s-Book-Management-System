package com.kidsbook.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.kidsbook.entity.*;
import com.kidsbook.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PermissionCacheService {

    private final SysUserRoleMapper userRoleMapper;
    private final SysRolePermissionMapper rolePermissionMapper;
    private final SysPermissionMapper permissionMapper;
    private final SysRoleMapper roleMapper;

    private final Cache<String, List<String>> permCache = Caffeine.newBuilder()
            .expireAfterWrite(30, TimeUnit.MINUTES)
            .maximumSize(500)
            .build();

    private final Cache<String, List<String>> roleCache = Caffeine.newBuilder()
            .expireAfterWrite(30, TimeUnit.MINUTES)
            .maximumSize(500)
            .build();

    public List<String> getPermissions(String userType, Long userId) {
        if (userType == null || userId == null) return Collections.emptyList();
        String key = userType + ":" + userId;
        List<String> cached = permCache.get(key, k -> loadPermissions(userType, userId));
        return cached != null ? cached : Collections.emptyList();
    }

    public List<String> getRoleCodes(String userType, Long userId) {
        if (userType == null || userId == null) return Collections.emptyList();
        String key = userType + ":" + userId;
        List<String> cached = roleCache.get(key, k -> loadRoleCodes(userType, userId));
        return cached != null ? cached : Collections.emptyList();
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
        permCache.invalidate(key);
        roleCache.invalidate(key);
    }

    public void invalidateAll() {
        permCache.invalidateAll();
        roleCache.invalidateAll();
    }
}
