package com.kidsbook.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kidsbook.common.Permission;
import com.kidsbook.common.RequirePermission;
import com.kidsbook.common.Result;
import com.kidsbook.entity.SysPermission;
import com.kidsbook.mapper.SysPermissionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/sys/permissions")
@RequiredArgsConstructor
public class SysPermissionController {
    private final SysPermissionMapper sysPermissionMapper;

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
}
