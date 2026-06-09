package com.kidsbook.aspect;

import com.kidsbook.annotation.RequirePermission;
import com.kidsbook.service.PermissionCacheService;
import com.kidsbook.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;

@Aspect
@Component
@RequiredArgsConstructor
public class PermissionAspect {

    private final PermissionCacheService permissionCacheService;
    private final JwtUtil jwtUtil;

    @Around("@annotation(requirePermission)")
    public Object checkPermission(ProceedingJoinPoint joinPoint, RequirePermission requirePermission) throws Throwable {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getCredentials() == null) {
            throw new AccessDeniedException("未登录");
        }

        String token = auth.getCredentials().toString();
        String userType = jwtUtil.getUserTypeFromToken(token);
        Long userId = jwtUtil.getUserIdFromToken(token);

        // 旧token没有userType/userId，跳过细粒度检查
        if (userType == null || userId == null) {
            return joinPoint.proceed();
        }

        // 检查是否是超级管理员
        List<String> roleCodes = permissionCacheService.getRoleCodes(userType, userId);
        if (roleCodes.contains("super:admin")) {
            return joinPoint.proceed();
        }

        // 检查具体权限
        List<String> permissions = permissionCacheService.getPermissions(userType, userId);
        String required = requirePermission.value();
        if (!permissions.contains(required)) {
            throw new AccessDeniedException("没有权限: " + required);
        }

        return joinPoint.proceed();
    }
}
