package com.kidsbook.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kidsbook.annotation.RequirePermission;
import com.kidsbook.common.Result;
import com.kidsbook.dto.RoleRequest;
import com.kidsbook.entity.SysRole;
import com.kidsbook.service.SysPermissionService;
import com.kidsbook.service.SysRoleService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sys/roles")
@RequiredArgsConstructor
@Validated
public class SysRoleController {

    private final SysRoleService roleService;
    private final SysPermissionService permissionService;

    @GetMapping
    @RequirePermission("role:view")
    public Result<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
            @RequestParam(required = false) String keyword) {
        Page<SysRole> result = roleService.listRoles(page, size, keyword);
        Map<String, Object> data = new HashMap<>();
        data.put("records", result.getRecords());
        data.put("total", result.getTotal());
        return Result.success(data);
    }

    @GetMapping("/all")
    @RequirePermission("role:view")
    public Result<List<SysRole>> listAll() {
        return Result.success(roleService.getAllRoles());
    }

    @GetMapping("/{id}")
    @RequirePermission("role:view")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        SysRole role = roleService.getRoleById(id);
        if (role == null) {
            return Result.error("角色不存在");
        }
        List<Long> permissionIds = roleService.getRolePermissionIds(id);
        Map<String, Object> data = new HashMap<>();
        data.put("role", role);
        data.put("permissionIds", permissionIds);
        return Result.success(data);
    }

    @PostMapping
    @RequirePermission("role:add")
    public Result<Void> add(@Valid @RequestBody RoleRequest request) {
        SysRole role = new SysRole();
        role.setCode(request.getCode());
        role.setName(request.getName());
        role.setDescription(request.getDescription());
        role.setStatus(request.getStatus() != null ? request.getStatus() : 1);
        roleService.addRole(role);
        return Result.success();
    }

    @PutMapping("/{id}")
    @RequirePermission("role:edit")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody RoleRequest request) {
        SysRole role = new SysRole();
        role.setId(id);
        role.setCode(request.getCode());
        role.setName(request.getName());
        role.setDescription(request.getDescription());
        role.setStatus(request.getStatus() != null ? request.getStatus() : 1);
        roleService.updateRole(role);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @RequirePermission("role:delete")
    public Result<Void> delete(@PathVariable Long id) {
        roleService.deleteRole(id);
        return Result.success();
    }

    @PutMapping("/{id}/permissions")
    @RequirePermission("permission:assign")
    public Result<Void> assignPermissions(@PathVariable Long id, @RequestBody Map<String, List<Long>> body) {
        List<Long> permissionIds = body.get("permissionIds");
        if (permissionIds == null) {
            throw new RuntimeException("permissionIds不能为空");
        }
        roleService.assignPermissions(id, permissionIds);
        return Result.success();
    }
}
