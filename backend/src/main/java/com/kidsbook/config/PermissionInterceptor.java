package com.kidsbook.config;

import com.kidsbook.common.BusinessException;
import com.kidsbook.common.Permission;
import com.kidsbook.common.RequirePermission;
import com.kidsbook.common.RolePermissions;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Aspect
@Component
public class PermissionInterceptor {

    @Around("@annotation(com.kidsbook.common.RequirePermission)")
    public Object checkPermission(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature sig = (MethodSignature) joinPoint.getSignature();
        RequirePermission annotation = sig.getMethod().getAnnotation(RequirePermission.class);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new BusinessException(401, "未登录，请先登录");
        }

        Set<String> userPermissions = auth.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .filter(a -> !a.startsWith("ROLE_"))
            .collect(Collectors.toSet());

        Permission[] required = annotation.value();
        boolean requireAll = annotation.requireAll();

        boolean granted;
        if (!userPermissions.isEmpty()) {
            if (requireAll) {
                granted = true;
                for (Permission p : required) {
                    if (!userPermissions.contains(p.name())) {
                        granted = false;
                        break;
                    }
                }
            } else {
                granted = false;
                for (Permission p : required) {
                    if (userPermissions.contains(p.name())) {
                        granted = true;
                        break;
                    }
                }
            }
        } else {
            // Fallback to static RolePermissions for old tokens without embedded permissions
            String role = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring(5))
                .findFirst()
                .orElse("UNKNOWN");

            if (requireAll) {
                granted = true;
                for (Permission p : required) {
                    if (!RolePermissions.hasPermission(role, p)) {
                        granted = false;
                        break;
                    }
                }
            } else {
                granted = false;
                for (Permission p : required) {
                    if (RolePermissions.hasPermission(role, p)) {
                        granted = true;
                        break;
                    }
                }
            }
        }

        if (!granted) {
            log.warn("权限拒绝: user={}, required={}, method={}",
                auth.getName(), required, sig.toShortString());
            throw new BusinessException(403, "权限不足，无法执行此操作");
        }

        return joinPoint.proceed();
    }
}
