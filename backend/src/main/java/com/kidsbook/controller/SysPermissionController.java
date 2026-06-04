package com.kidsbook.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kidsbook.common.BusinessException;
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

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/sys/permissions")
@RequiredArgsConstructor
public class SysPermissionController {
    private final SysPermissionMapper sysPermissionMapper;
    private final SysRolePermissionMapper sysRolePermissionMapper;
    private final SysRoleMapper sysRoleMapper;

    private static final Set<String> BUILT_IN_CODES;
    static {
        Set<String> codes = new HashSet<>();
        for (Permission p : Permission.values()) {
            codes.add(p.name());
        }
        BUILT_IN_CODES = Collections.unmodifiableSet(codes);
    }

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
            item.put("builtIn", BUILT_IN_CODES.contains(perm.getCode()));

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

    @PostMapping
    @RequirePermission(Permission.PERMISSION_MANAGE)
    public Result<?> create(@RequestBody Map<String, String> body) {
        String code = body.getOrDefault("code", "").trim().toUpperCase();
        String name = body.getOrDefault("name", "").trim();
        String module = body.getOrDefault("module", "其他").trim();
        String type = body.getOrDefault("type", "button").trim();
        String description = body.getOrDefault("description", "").trim();

        if (code.isEmpty() || name.isEmpty()) {
            throw new BusinessException(400, "权限编码和名称不能为空");
        }
        if (!code.matches("[A-Z][A-Z0-9_]*")) {
            throw new BusinessException(400, "权限编码格式不正确，应为大写字母开头的英文、数字和下划线组合");
        }

        Long existCount = sysPermissionMapper.selectCount(
            new LambdaQueryWrapper<SysPermission>().eq(SysPermission::getCode, code));
        if (existCount > 0) {
            throw new BusinessException(400, "权限编码已存在");
        }

        SysPermission perm = new SysPermission();
        perm.setCode(code);
        perm.setName(name);
        perm.setModule(module);
        perm.setType(type);
        perm.setDescription(description);
        perm.setCreateTime(LocalDateTime.now());
        sysPermissionMapper.insert(perm);

        return Result.success(perm);
    }

    @PutMapping("/{id}")
    @RequirePermission(Permission.PERMISSION_MANAGE)
    public Result<?> update(@PathVariable Long id, @RequestBody Map<String, String> body) {
        SysPermission perm = sysPermissionMapper.selectById(id);
        if (perm == null) {
            throw new BusinessException(404, "权限不存在");
        }

        boolean isBuiltIn = BUILT_IN_CODES.contains(perm.getCode());
        String newCode = body.getOrDefault("code", "").trim().toUpperCase();
        if (isBuiltIn && !newCode.isEmpty() && !newCode.equals(perm.getCode())) {
            throw new BusinessException(400, "内置权限不可修改编码");
        }

        if (!newCode.isEmpty() && !newCode.equals(perm.getCode())) {
            if (!newCode.matches("[A-Z][A-Z0-9_]*")) {
                throw new BusinessException(400, "权限编码格式不正确");
            }
            Long existCount = sysPermissionMapper.selectCount(
                new LambdaQueryWrapper<SysPermission>().eq(SysPermission::getCode, newCode));
            if (existCount > 0) {
                throw new BusinessException(400, "权限编码已存在");
            }
            perm.setCode(newCode);
        }

        if (body.containsKey("name")) perm.setName(body.get("name").trim());
        if (body.containsKey("module")) perm.setModule(body.get("module").trim());
        if (body.containsKey("type")) perm.setType(body.get("type").trim());
        if (body.containsKey("description")) perm.setDescription(body.get("description").trim());

        sysPermissionMapper.updateById(perm);
        return Result.success(perm);
    }

    @DeleteMapping("/{id}")
    @RequirePermission(Permission.PERMISSION_MANAGE)
    public Result<?> delete(@PathVariable Long id) {
        SysPermission perm = sysPermissionMapper.selectById(id);
        if (perm == null) {
            throw new BusinessException(404, "权限不存在");
        }
        if (BUILT_IN_CODES.contains(perm.getCode())) {
            throw new BusinessException(400, "内置权限不可删除");
        }

        sysRolePermissionMapper.delete(
            new LambdaQueryWrapper<SysRolePermission>().eq(SysRolePermission::getPermissionId, id));
        sysPermissionMapper.deleteById(id);
        return Result.success(null);
    }
}
