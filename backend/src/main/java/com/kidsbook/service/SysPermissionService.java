package com.kidsbook.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kidsbook.entity.SysPermission;
import com.kidsbook.entity.SysRolePermission;
import com.kidsbook.mapper.SysPermissionMapper;
import com.kidsbook.mapper.SysRolePermissionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysPermissionService {

    private final SysPermissionMapper permissionMapper;
    private final SysRolePermissionMapper rolePermissionMapper;

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

    private List<SysPermission> buildTree(List<SysPermission> all, Long parentId) {
        Map<Long, List<SysPermission>> grouped = all.stream()
                .collect(Collectors.groupingBy(SysPermission::getParentId));

        List<SysPermission> roots = grouped.getOrDefault(parentId, new ArrayList<>());
        for (SysPermission node : roots) {
            List<SysPermission> children = buildTree(all, node.getId());
            node.setChildren(children.isEmpty() ? null : children);
        }
        return roots;
    }
}
