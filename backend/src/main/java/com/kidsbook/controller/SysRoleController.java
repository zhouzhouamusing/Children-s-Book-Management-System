package com.kidsbook.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kidsbook.common.BusinessException;
import com.kidsbook.common.RequirePermission;
import com.kidsbook.common.Permission;
import com.kidsbook.common.Result;
import com.kidsbook.dto.RolePermissionAssignDTO;
import com.kidsbook.dto.SysRoleDTO;
import com.kidsbook.entity.SysRole;
import com.kidsbook.entity.SysRolePermission;
import com.kidsbook.entity.SysPermission;
import com.kidsbook.entity.SysUserRole;
import com.kidsbook.mapper.SysRoleMapper;
import com.kidsbook.mapper.SysRolePermissionMapper;
import com.kidsbook.mapper.SysPermissionMapper;
import com.kidsbook.mapper.SysUserRoleMapper;
import com.kidsbook.service.RbacService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/sys/roles")
@RequiredArgsConstructor
public class SysRoleController {
    private final SysRoleMapper sysRoleMapper;
    private final SysRolePermissionMapper sysRolePermissionMapper;
    private final SysPermissionMapper sysPermissionMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final RbacService rbacService;

    @GetMapping
    @RequirePermission(Permission.ROLE_MANAGE)
    public Result<?> list(@RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "20") int size,
                          @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(SysRole::getName, keyword).or().like(SysRole::getCode, keyword);
        }
        wrapper.orderByDesc(SysRole::getLevel);
        Page<SysRole> result = sysRoleMapper.selectPage(new Page<>(page, size), wrapper);

        // Attach user count for each role
        List<Map<String, Object>> records = new ArrayList<>();
        for (SysRole role : result.getRecords()) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", role.getId());
            map.put("code", role.getCode());
            map.put("name", role.getName());
            map.put("level", role.getLevel());
            map.put("description", role.getDescription());
            map.put("status", role.getStatus());
            map.put("createTime", role.getCreateTime());
            map.put("updateTime", role.getUpdateTime());
            Long userCount = sysUserRoleMapper.selectCount(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getRoleId, role.getId()));
            map.put("userCount", userCount);
            records.add(map);
        }

        Map<String, Object> pageResult = new HashMap<>();
        pageResult.put("records", records);
        pageResult.put("total", result.getTotal());
        pageResult.put("current", result.getCurrent());
        pageResult.put("size", result.getSize());
        return Result.success(pageResult);
    }

    @GetMapping("/all")
    @RequirePermission(Permission.ROLE_MANAGE)
    public Result<?> listAll() {
        List<SysRole> roles = sysRoleMapper.selectList(
            new LambdaQueryWrapper<SysRole>().orderByDesc(SysRole::getLevel));
        return Result.success(roles);
    }

    @PostMapping
    @RequirePermission(Permission.ROLE_MANAGE)
    public Result<?> create(@RequestBody SysRoleDTO dto) {
        SysRole existing = sysRoleMapper.selectOne(
            new LambdaQueryWrapper<SysRole>().eq(SysRole::getCode, dto.getCode()));
        if (existing != null) {
            throw new BusinessException(400, "角色编码已存在");
        }
        SysRole role = new SysRole();
        role.setCode(dto.getCode());
        role.setName(dto.getName());
        role.setLevel(dto.getLevel() != null ? dto.getLevel() : 0);
        role.setDescription(dto.getDescription());
        role.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        sysRoleMapper.insert(role);

        if (dto.getPermissionIds() != null && !dto.getPermissionIds().isEmpty()) {
            for (Long permId : dto.getPermissionIds()) {
                SysRolePermission rp = new SysRolePermission();
                rp.setRoleId(role.getId());
                rp.setPermissionId(permId);
                sysRolePermissionMapper.insert(rp);
            }
        }
        return Result.success(role);
    }

    @PutMapping("/{id}")
    @RequirePermission(Permission.ROLE_MANAGE)
    public Result<?> update(@PathVariable Long id, @RequestBody SysRoleDTO dto) {
        SysRole role = sysRoleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException(404, "角色不存在");
        }
        if (dto.getName() != null) role.setName(dto.getName());
        if (dto.getLevel() != null) role.setLevel(dto.getLevel());
        if (dto.getDescription() != null) role.setDescription(dto.getDescription());
        if (dto.getStatus() != null) role.setStatus(dto.getStatus());
        sysRoleMapper.updateById(role);

        if (dto.getPermissionIds() != null) {
            sysRolePermissionMapper.delete(
                new LambdaQueryWrapper<SysRolePermission>().eq(SysRolePermission::getRoleId, id));
            for (Long permId : dto.getPermissionIds()) {
                SysRolePermission rp = new SysRolePermission();
                rp.setRoleId(id);
                rp.setPermissionId(permId);
                sysRolePermissionMapper.insert(rp);
            }
        }
        return Result.success(role);
    }

    @DeleteMapping("/{id}")
    @RequirePermission(Permission.ROLE_MANAGE)
    public Result<?> delete(@PathVariable Long id) {
        SysRole role = sysRoleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException(404, "角色不存在");
        }
        if ("ADMIN".equals(role.getCode()) || "READER".equals(role.getCode()) || "SUPER_ADMIN".equals(role.getCode())) {
            throw new BusinessException(400, "内置角色不允许删除");
        }
        sysRolePermissionMapper.delete(
            new LambdaQueryWrapper<SysRolePermission>().eq(SysRolePermission::getRoleId, id));
        sysRoleMapper.deleteById(id);
        return Result.success("删除成功");
    }

    @GetMapping("/{id}/permissions")
    @RequirePermission(Permission.ROLE_MANAGE)
    public Result<?> getRolePermissions(@PathVariable Long id) {
        List<SysRolePermission> rps = sysRolePermissionMapper.selectList(
            new LambdaQueryWrapper<SysRolePermission>().eq(SysRolePermission::getRoleId, id));
        List<Long> permIds = rps.stream().map(SysRolePermission::getPermissionId).collect(Collectors.toList());
        return Result.success(permIds);
    }

    @GetMapping("/{id}/effective-permissions")
    @RequirePermission(Permission.ROLE_MANAGE)
    public Result<?> getEffectivePermissions(@PathVariable Long id) {
        SysRole role = sysRoleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException(404, "角色不存在");
        }
        // Get all roles at or below this role's level
        List<SysRole> inheritedRoles = sysRoleMapper.selectList(
            new LambdaQueryWrapper<SysRole>()
                .le(SysRole::getLevel, role.getLevel())
                .eq(SysRole::getStatus, 1));
        List<Long> roleIds = inheritedRoles.stream().map(SysRole::getId).collect(Collectors.toList());
        List<SysRolePermission> rps = sysRolePermissionMapper.selectList(
            new LambdaQueryWrapper<SysRolePermission>().in(SysRolePermission::getRoleId, roleIds));
        Set<Long> permIds = rps.stream().map(SysRolePermission::getPermissionId).collect(Collectors.toSet());

        List<SysPermission> permissions = permIds.isEmpty() ? Collections.emptyList() :
            sysPermissionMapper.selectList(
                new LambdaQueryWrapper<SysPermission>().in(SysPermission::getId, permIds));

        Map<String, Object> result = new HashMap<>();
        result.put("ownPermissionIds", sysRolePermissionMapper.selectList(
            new LambdaQueryWrapper<SysRolePermission>().eq(SysRolePermission::getRoleId, id))
            .stream().map(SysRolePermission::getPermissionId).collect(Collectors.toList()));
        result.put("effectivePermissions", permissions);
        result.put("inheritedRoles", inheritedRoles);
        return Result.success(result);
    }

    @PutMapping("/{id}/permissions")
    @RequirePermission(Permission.ROLE_MANAGE)
    public Result<?> assignPermissions(@PathVariable Long id, @RequestBody RolePermissionAssignDTO dto) {
        SysRole role = sysRoleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException(404, "角色不存在");
        }
        // Remove old assignments
        sysRolePermissionMapper.delete(
            new LambdaQueryWrapper<SysRolePermission>().eq(SysRolePermission::getRoleId, id));
        // Insert new ones
        if (dto.getPermissionIds() != null) {
            for (Long permId : dto.getPermissionIds()) {
                SysRolePermission rp = new SysRolePermission();
                rp.setRoleId(id);
                rp.setPermissionId(permId);
                sysRolePermissionMapper.insert(rp);
            }
        }
        return Result.success("权限分配成功");
    }
}
