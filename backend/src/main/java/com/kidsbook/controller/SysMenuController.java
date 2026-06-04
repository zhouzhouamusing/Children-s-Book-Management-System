package com.kidsbook.controller;

import com.kidsbook.common.Result;
import com.kidsbook.dto.MenuVO;
import com.kidsbook.entity.SysMenu;
import com.kidsbook.service.RbacService;
import com.kidsbook.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/sys/menus")
@RequiredArgsConstructor
public class SysMenuController {
    private final RbacService rbacService;
    private final JwtUtil jwtUtil;

    @GetMapping("/my")
    public Result<?> getMyMenus() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String token = (String) auth.getCredentials();
        List<String> permissions = jwtUtil.getPermissionsFromToken(token);
        Set<String> permSet = new HashSet<>(permissions);

        List<SysMenu> menus = rbacService.getUserMenus(permSet);
        List<MenuVO> menuVOs = menus.stream().map(this::toVO).collect(Collectors.toList());
        return Result.success(menuVOs);
    }

    private MenuVO toVO(SysMenu menu) {
        MenuVO vo = new MenuVO();
        vo.setId(menu.getId());
        vo.setParentId(menu.getParentId());
        vo.setName(menu.getName());
        vo.setPath(menu.getPath());
        vo.setIcon(menu.getIcon());
        vo.setSortOrder(menu.getSortOrder());
        vo.setPermissionCode(menu.getPermissionCode());
        return vo;
    }
}
