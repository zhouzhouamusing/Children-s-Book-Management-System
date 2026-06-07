package com.kidsbook.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kidsbook.annotation.RequirePermission;
import com.kidsbook.common.Result;
import com.kidsbook.service.PermissionCacheService;
import com.kidsbook.service.SysUserRoleService;
import com.kidsbook.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sys/user-roles")
@RequiredArgsConstructor
public class SysUserRoleController {

    private final SysUserRoleService userRoleService;
    private final PermissionCacheService permissionCacheService;
    private final JwtUtil jwtUtil;

    @GetMapping("/admins")
    @RequirePermission("user-role:view")
    public Result<Map<String, Object>> listAdmins(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        Page<Map<String, Object>> result = userRoleService.listAdminsWithRoles(page, size, keyword);
        Map<String, Object> data = new HashMap<>();
        data.put("records", result.getRecords());
        data.put("total", result.getTotal());
        return Result.success(data);
    }

    @PutMapping("/assign")
    @RequirePermission("user-role:assign")
    public Result<Void> assignRoles(@RequestBody Map<String, Object> body) {
        String userType = (String) body.get("userType");
        Number userIdNum = (Number) body.get("userId");
        Long userId = userIdNum.longValue();
        @SuppressWarnings("unchecked")
        List<Number> roleIdNums = (List<Number>) body.get("roleIds");
        List<Long> roleIds = roleIdNums.stream().map(Number::longValue).toList();
        userRoleService.assignRoles(userType, userId, roleIds);
        return Result.success();
    }

    @GetMapping("/my-permissions")
    public Result<Map<String, Object>> myPermissions() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getCredentials() == null) {
            return Result.error("未登录");
        }
        String token = auth.getCredentials().toString();
        String userType = jwtUtil.getUserTypeFromToken(token);
        Long userId = jwtUtil.getUserIdFromToken(token);

        if (userType == null || userId == null) {
            return Result.error("token格式不正确，请重新登录");
        }

        List<String> roles = permissionCacheService.getRoleCodes(userType, userId);
        List<String> permissions = permissionCacheService.getPermissions(userType, userId);

        Map<String, Object> data = new HashMap<>();
        data.put("roles", roles);
        data.put("permissions", permissions);
        return Result.success(data);
    }
}
