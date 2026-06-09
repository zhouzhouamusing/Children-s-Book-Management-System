package com.kidsbook.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.kidsbook.entity.*;
import com.kidsbook.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PermissionCacheService {

    private final SysUserRoleMapper userRoleMapper;
    private final SysRolePermissionMapper rolePermissionMapper;
    private final SysPermissionMapper permissionMapper;
    private final SysRoleMapper roleMapper;

    // 权限缓存：key="userType:userId", value=权限编码列表
    // 策略：写入后30分钟过期，最大500条；变更时主动失效
    private final Cache<String, List<String>> permCache = Caffeine.newBuilder()
            .expireAfterWrite(30, TimeUnit.MINUTES)
            .maximumSize(500)
            .build();

    // 角色缓存：key="userType:userId", value=角色编码列表
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

    /**
     * 登录时刷新缓存，确保返回最新权限数据并预热缓存
     */
    public void refreshOnLogin(String userType, Long userId) {
        invalidateUser(userType, userId);
        getPermissions(userType, userId);
        getRoleCodes(userType, userId);
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

        // 过滤掉禁用的角色
        List<SysRole> activeRoles = roleMapper.selectList(
                new LambdaQueryWrapper<SysRole>()
                        .in(SysRole::getId, roleIds)
                        .eq(SysRole::getStatus, 1));
        if (activeRoles.isEmpty()) return Collections.emptyList();
        List<Long> activeRoleIds = activeRoles.stream().map(SysRole::getId).collect(Collectors.toList());

        List<SysRolePermission> rolePerms = rolePermissionMapper.selectList(
                new LambdaQueryWrapper<SysRolePermission>()
                        .in(SysRolePermission::getRoleId, activeRoleIds));
        if (rolePerms.isEmpty()) return Collections.emptyList();

        List<Long> permIds = rolePerms.stream().map(SysRolePermission::getPermissionId).distinct().collect(Collectors.toList());

        // 直接分配的活跃权限
        List<SysPermission> directPerms = permissionMapper.selectList(
                new LambdaQueryWrapper<SysPermission>()
                        .in(SysPermission::getId, permIds)
                        .eq(SysPermission::getStatus, 1));

        // 权限层级继承：拥有父权限自动获得所有活跃子权限
        List<SysPermission> allActivePerms = permissionMapper.selectList(
                new LambdaQueryWrapper<SysPermission>()
                        .eq(SysPermission::getStatus, 1));

        Set<Long> expandedIds = directPerms.stream()
                .map(SysPermission::getId).collect(Collectors.toCollection(HashSet::new));

        boolean changed = true;
        while (changed) {
            changed = false;
            for (SysPermission p : allActivePerms) {
                if (!expandedIds.contains(p.getId()) && expandedIds.contains(p.getParentId())) {
                    expandedIds.add(p.getId());
                    changed = true;
                }
            }
        }

        return allActivePerms.stream()
                .filter(p -> expandedIds.contains(p.getId()))
                .map(SysPermission::getCode)
                .collect(Collectors.toList());
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
