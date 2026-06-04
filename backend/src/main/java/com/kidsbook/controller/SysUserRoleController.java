package com.kidsbook.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kidsbook.common.BusinessException;
import com.kidsbook.common.Permission;
import com.kidsbook.common.RequirePermission;
import com.kidsbook.common.Result;
import com.kidsbook.dto.UserRoleAssignDTO;
import com.kidsbook.entity.SysRole;
import com.kidsbook.entity.SysUserRole;
import com.kidsbook.mapper.SysRoleMapper;
import com.kidsbook.mapper.SysUserRoleMapper;
import com.kidsbook.service.RbacService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/sys/user-roles")
@RequiredArgsConstructor
public class SysUserRoleController {
    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysRoleMapper sysRoleMapper;
    private final RbacService rbacService;

    @GetMapping("/users/{userType}/{userId}/roles")
    @RequirePermission(Permission.USER_ROLE_ASSIGN)
    public Result<?> getUserRoles(@PathVariable String userType, @PathVariable Long userId) {
        List<SysUserRole> userRoles = sysUserRoleMapper.selectList(
            new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getUserType, userType)
                .eq(SysUserRole::getUserId, userId));
        List<Long> roleIds = userRoles.stream().map(SysUserRole::getRoleId).collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("roleIds", roleIds);
        if (!roleIds.isEmpty()) {
            List<SysRole> roles = sysRoleMapper.selectList(
                new LambdaQueryWrapper<SysRole>().in(SysRole::getId, roleIds));
            result.put("roles", roles);
        } else {
            result.put("roles", Collections.emptyList());
        }
        return Result.success(result);
    }

    @PutMapping("/users/{userType}/{userId}/roles")
    @RequirePermission(Permission.USER_ROLE_ASSIGN)
    public Result<?> assignRoles(@PathVariable String userType,
                                 @PathVariable Long userId,
                                 @RequestBody UserRoleAssignDTO dto) {
        if (!"ADMIN".equals(userType) && !"READER".equals(userType)) {
            throw new BusinessException(400, "无效的用户类型");
        }
        // Remove old assignments
        sysUserRoleMapper.delete(
            new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getUserType, userType)
                .eq(SysUserRole::getUserId, userId));
        // Insert new
        if (dto.getRoleIds() != null) {
            for (Long roleId : dto.getRoleIds()) {
                SysUserRole ur = new SysUserRole();
                ur.setUserType(userType);
                ur.setUserId(userId);
                ur.setRoleId(roleId);
                sysUserRoleMapper.insert(ur);
            }
        }
        return Result.success("角色分配成功");
    }

    @GetMapping("/roles/{roleId}/users")
    @RequirePermission(Permission.USER_ROLE_ASSIGN)
    public Result<?> getUsersByRole(@PathVariable Long roleId) {
        List<SysUserRole> userRoles = sysUserRoleMapper.selectList(
            new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getRoleId, roleId));
        return Result.success(userRoles);
    }
}
