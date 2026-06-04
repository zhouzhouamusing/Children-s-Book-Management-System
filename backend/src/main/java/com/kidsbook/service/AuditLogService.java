package com.kidsbook.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kidsbook.entity.AuditLog;
import com.kidsbook.mapper.AuditLogMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {
    private final AuditLogMapper auditLogMapper;

    public void log(String action, String targetType, Long targetId, String detail) {
        try {
            String username = null;
            String role = null;
            String ip = null;

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() != null) {
                username = auth.getName();
                role = auth.getAuthorities().stream()
                        .findFirst()
                        .map(a -> a.getAuthority().replace("ROLE_", ""))
                        .orElse("UNKNOWN");
            }

            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                ip = getClientIp(attrs.getRequest());
            }

            logAsync(username, role, action, targetType, targetId, detail, ip);
        } catch (Exception e) {
            log.error("审计日志记录失败: {}", e.getMessage());
        }
    }

    public void logSync(String username, String role, String action, String targetType, Long targetId, String detail, String ip) {
        doLog(username, role, action, targetType, targetId, detail, ip);
    }

    @Async
    public void logAsync(String username, String role, String action, String targetType, Long targetId, String detail, String ip) {
        doLog(username, role, action, targetType, targetId, detail, ip);
    }

    private void doLog(String username, String role, String action, String targetType, Long targetId, String detail, String ip) {
        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setOperatorUsername(username);
            auditLog.setOperatorRole(role);
            auditLog.setAction(action);
            auditLog.setTargetType(targetType);
            auditLog.setTargetId(targetId);
            auditLog.setDetail(detail);
            auditLog.setIpAddress(ip);
            auditLog.setCreateTime(LocalDateTime.now());
            auditLogMapper.insert(auditLog);
        } catch (Exception e) {
            log.error("审计日志写入失败: {}", e.getMessage());
        }
    }

    public IPage<AuditLog> listLogs(int page, int size, String action, String operatorUsername) {
        LambdaQueryWrapper<AuditLog> wrapper = new LambdaQueryWrapper<>();
        if (action != null && !action.isEmpty()) {
            wrapper.eq(AuditLog::getAction, action);
        }
        if (operatorUsername != null && !operatorUsername.isEmpty()) {
            wrapper.like(AuditLog::getOperatorUsername, operatorUsername);
        }
        wrapper.orderByDesc(AuditLog::getCreateTime);
        return auditLogMapper.selectPage(new Page<>(page, size), wrapper);
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
