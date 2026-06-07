package com.kidsbook.controller;

import com.kidsbook.annotation.RequirePermission;
import com.kidsbook.common.Result;
import com.kidsbook.entity.SysPermission;
import com.kidsbook.service.SysPermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sys/permissions")
@RequiredArgsConstructor
public class SysPermissionController {

    private final SysPermissionService permissionService;

    @GetMapping("/tree")
    @RequirePermission("permission:view")
    public Result<List<SysPermission>> tree() {
        return Result.success(permissionService.getPermissionTree());
    }

    @GetMapping
    @RequirePermission("permission:view")
    public Result<List<SysPermission>> list() {
        return Result.success(permissionService.getAllPermissions());
    }
}
