package com.kidsbook.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kidsbook.dto.PermissionRequest;
import com.kidsbook.entity.SysPermission;
import com.kidsbook.entity.SysRolePermission;
import com.kidsbook.mapper.SysPermissionMapper;
import com.kidsbook.mapper.SysRolePermissionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysPermissionService {

    private final SysPermissionMapper permissionMapper;
    private final SysRolePermissionMapper rolePermissionMapper;
    private final PermissionCacheService permissionCacheService;

    public List<SysPermission> getPermissionTree() {
        List<SysPermission> all = permissionMapper.selectList(
                new LambdaQueryWrapper<SysPermission>()
                        .eq(SysPermission::getStatus, 1)
                        .orderByAsc(SysPermission::getSortOrder));
        return buildTree(all, 0L);
    }

    public List<SysPermission> getAllPermissions() {
        return permissionMapper.selectList(
                new LambdaQueryWrapper<SysPermission>()
                        .orderByAsc(SysPermission::getSortOrder));
    }

    public List<Long> getPermissionIdsByRoleId(Long roleId) {
        List<SysRolePermission> rps = rolePermissionMapper.selectList(
                new LambdaQueryWrapper<SysRolePermission>()
                        .eq(SysRolePermission::getRoleId, roleId));
        return rps.stream().map(SysRolePermission::getPermissionId).collect(Collectors.toList());
    }

    public void addPermission(PermissionRequest request) {
        Long exists = permissionMapper.selectCount(
                new LambdaQueryWrapper<SysPermission>()
                        .eq(SysPermission::getCode, request.getCode()));
        if (exists > 0) {
            throw new RuntimeException("权限编码已存在: " + request.getCode());
        }

        if (request.getParentId() != null && request.getParentId() > 0) {
            SysPermission parent = permissionMapper.selectById(request.getParentId());
            if (parent == null) {
                throw new RuntimeException("父权限不存在");
            }
        }

        SysPermission perm = new SysPermission();
        perm.setCode(request.getCode());
        perm.setName(request.getName());
        perm.setType(request.getType());
        perm.setParentId(request.getParentId() != null ? request.getParentId() : 0L);
        perm.setPath(request.getPath());
        perm.setIcon(request.getIcon());
        perm.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        perm.setStatus(request.getStatus() != null ? request.getStatus() : 1);
        permissionMapper.insert(perm);
        permissionCacheService.invalidateAll();
    }

    public void updatePermission(Long id, PermissionRequest request) {
        SysPermission existing = permissionMapper.selectById(id);
        if (existing == null) {
            throw new RuntimeException("权限不存在");
        }

        Long codeConflict = permissionMapper.selectCount(
                new LambdaQueryWrapper<SysPermission>()
                        .eq(SysPermission::getCode, request.getCode())
                        .ne(SysPermission::getId, id));
        if (codeConflict > 0) {
            throw new RuntimeException("权限编码已被其他权限使用: " + request.getCode());
        }

        if (request.getParentId() != null && request.getParentId() > 0) {
            if (request.getParentId().equals(id)) {
                throw new RuntimeException("不能将自身设为父权限");
            }
            SysPermission parent = permissionMapper.selectById(request.getParentId());
            if (parent == null) {
                throw new RuntimeException("父权限不存在");
            }
        }

        existing.setCode(request.getCode());
        existing.setName(request.getName());
        existing.setType(request.getType());
        existing.setParentId(request.getParentId() != null ? request.getParentId() : 0L);
        existing.setPath(request.getPath());
        existing.setIcon(request.getIcon());
        existing.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        existing.setStatus(request.getStatus() != null ? request.getStatus() : 1);
        permissionMapper.updateById(existing);

        // 禁用时级联禁用所有子权限
        if (request.getStatus() != null && request.getStatus() == 0) {
            cascadeDisableChildren(id);
        }

        permissionCacheService.invalidateAll();
    }

    @Transactional
    public void deletePermission(Long id) {
        SysPermission existing = permissionMapper.selectById(id);
        if (existing == null) {
            throw new RuntimeException("权限不存在");
        }

        Long childCount = permissionMapper.selectCount(
                new LambdaQueryWrapper<SysPermission>()
                        .eq(SysPermission::getParentId, id));
        if (childCount > 0) {
            throw new RuntimeException("该权限下有子权限，请先删除子权限");
        }

        rolePermissionMapper.delete(
                new LambdaQueryWrapper<SysRolePermission>()
                        .eq(SysRolePermission::getPermissionId, id));
        permissionMapper.deleteById(id);
        permissionCacheService.invalidateAll();
    }

    public List<SysPermission> getMenusByUserPermissions(List<String> permissionCodes, boolean isSuperAdmin) {
        List<SysPermission> menus;
        if (isSuperAdmin) {
            menus = permissionMapper.selectList(
                    new LambdaQueryWrapper<SysPermission>()
                            .eq(SysPermission::getType, "menu")
                            .eq(SysPermission::getStatus, 1)
                            .orderByAsc(SysPermission::getSortOrder));
        } else {
            menus = permissionMapper.selectList(
                    new LambdaQueryWrapper<SysPermission>()
                            .eq(SysPermission::getType, "menu")
                            .eq(SysPermission::getStatus, 1)
                            .in(SysPermission::getCode, permissionCodes)
                            .orderByAsc(SysPermission::getSortOrder));
        }
        return menus;
    }

    private void cascadeDisableChildren(Long parentId) {
        List<SysPermission> children = permissionMapper.selectList(
                new LambdaQueryWrapper<SysPermission>()
                        .eq(SysPermission::getParentId, parentId)
                        .eq(SysPermission::getStatus, 1));
        for (SysPermission child : children) {
            child.setStatus(0);
            permissionMapper.updateById(child);
            cascadeDisableChildren(child.getId());
        }
    }

    private List<SysPermission> buildTree(List<SysPermission> all, Long parentId) {
        Map<Long, List<SysPermission>> grouped = all.stream()
                .collect(Collectors.groupingBy(SysPermission::getParentId));

        List<SysPermission> roots = grouped.getOrDefault(parentId, new ArrayList<>());
        for (SysPermission node : roots) {
            List<SysPermission> children = grouped.getOrDefault(node.getId(), new ArrayList<>());
            if (!children.isEmpty()) {
                for (SysPermission child : children) {
                    child.setChildren(null);
                }
                node.setChildren(children);
            } else {
                node.setChildren(null);
            }
        }
        return roots;
    }
}
