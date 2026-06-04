package com.kidsbook.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kidsbook.common.Permission;
import com.kidsbook.common.RequirePermission;
import com.kidsbook.common.Result;
import com.kidsbook.entity.SysPermission;
import com.kidsbook.entity.SysRole;
import com.kidsbook.entity.SysRolePermission;
import com.kidsbook.mapper.SysPermissionMapper;
import com.kidsbook.mapper.SysRoleMapper;
import com.kidsbook.mapper.SysRolePermissionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/sys/permissions")
@RequiredArgsConstructor
public class SysPermissionController {
    private final SysPermissionMapper sysPermissionMapper;
    private final SysRolePermissionMapper sysRolePermissionMapper;
    private final SysRoleMapper sysRoleMapper;

    @GetMapping
    @RequirePermission(Permission.PERMISSION_MANAGE)
    public Result<?> list(@RequestParam(required = false) String module) {
        LambdaQueryWrapper<SysPermission> wrapper = new LambdaQueryWrapper<>();
        if (module != null && !module.isBlank()) {
            wrapper.eq(SysPermission::getModule, module);
        }
        wrapper.orderByAsc(SysPermission::getModule).orderByAsc(SysPermission::getId);
        List<SysPermission> permissions = sysPermissionMapper.selectList(wrapper);
        return Result.success(permissions);
    }

    @GetMapping("/grouped")
    @RequirePermission(Permission.PERMISSION_MANAGE)
    public Result<?> listGrouped() {
        List<SysPermission> all = sysPermissionMapper.selectList(
            new LambdaQueryWrapper<SysPermission>().orderByAsc(SysPermission::getModule));
        Map<String, List<SysPermission>> grouped = all.stream()
            .collect(Collectors.groupingBy(p -> p.getModule() != null ? p.getModule() : "其他"));
        return Result.success(grouped);
    }

    @GetMapping("/modules")
    @RequirePermission(Permission.PERMISSION_MANAGE)
    public Result<?> listModules() {
        List<SysPermission> all = sysPermissionMapper.selectList(null);
        Set<String> modules = all.stream()
            .map(p -> p.getModule() != null ? p.getModule() : "其他")
            .collect(Collectors.toCollection(LinkedHashSet::new));
        return Result.success(modules);
    }

    @GetMapping("/with-roles")
    @RequirePermission(Permission.PERMISSION_MANAGE)
    public Result<?> listWithRoles() {
        List<SysPermission> allPerms = sysPermissionMapper.selectList(
            new LambdaQueryWrapper<SysPermission>().orderByAsc(SysPermission::getModule).orderByAsc(SysPermission::getId));
        List<SysRolePermission> allRolePerms = sysRolePermissionMapper.selectList(null);
        List<SysRole> allRoles = sysRoleMapper.selectList(
            new LambdaQueryWrapper<SysRole>().eq(SysRole::getStatus, 1));

        Map<Long, SysRole> roleMap = allRoles.stream()
            .collect(Collectors.toMap(SysRole::getId, r -> r));
        Map<Long, List<Long>> permToRoleIds = allRolePerms.stream()
            .collect(Collectors.groupingBy(SysRolePermission::getPermissionId,
                Collectors.mapping(SysRolePermission::getRoleId, Collectors.toList())));

        List<Map<String, Object>> result = new ArrayList<>();
        for (SysPermission perm : allPerms) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", perm.getId());
            item.put("code", perm.getCode());
            item.put("name", perm.getName());
            item.put("module", perm.getModule());
            item.put("type", perm.getType());
            item.put("description", perm.getDescription());

            List<Long> roleIds = permToRoleIds.getOrDefault(perm.getId(), Collections.emptyList());
            List<Map<String, Object>> roles = new ArrayList<>();
            for (Long roleId : roleIds) {
                SysRole role = roleMap.get(roleId);
                if (role != null) {
                    Map<String, Object> roleInfo = new HashMap<>();
                    roleInfo.put("id", role.getId());
                    roleInfo.put("code", role.getCode());
                    roleInfo.put("name", role.getName());
                    roleInfo.put("level", role.getLevel());
                    roles.add(roleInfo);
                }
            }
            item.put("roles", roles);
            result.add(item);
        }
        return Result.success(result);
    }
}
