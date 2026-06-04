package com.kidsbook.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import com.kidsbook.common.Permission;
import com.kidsbook.common.RolePermissions;

@Slf4j
@Component
public class JwtUtil {
    @Value("${jwt.secret:#{null}}")
    private String secret;

    @Value("${jwt.expiration:604800000}")
    private long expiration;

    @PostConstruct
    public void validateSecret() {
        if (secret == null || secret.trim().isEmpty()) {
            throw new IllegalStateException("JWT_SECRET 环境变量未配置，请设置至少32位的密钥");
        }
        if (secret.length() < 32) {
            throw new IllegalStateException("JWT_SECRET 长度不足，至少需要32个字符");
        }
        log.info("JWT密钥已加载，密钥长度: {}字符", secret.length());
    }

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(String username) {
        return generateToken(username, "ADMIN", null);
    }

    public String generateToken(String username, String role, Long readerId) {
        return generateToken(username, List.of(role),
            new ArrayList<>(RolePermissions.getPermissions(role).stream()
                .map(Permission::name).collect(Collectors.toList())),
            readerId);
    }

    public String generateToken(String username, List<String> roles, List<String> permissions, Long readerId) {
        String primaryRole = roles.isEmpty() ? "ADMIN" : roles.get(0);
        var builder = Jwts.builder()
                .subject(username)
                .claim("role", primaryRole)
                .claim("roles", roles)
                .claim("permissions", permissions)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getKey());
        if (readerId != null) {
            builder.claim("readerId", readerId);
        }
        return builder.compact();
    }

    public String getUsernameFromToken(String token) {
        return getClaims(token).getSubject();
    }

    public String getRoleFromToken(String token) {
        String role = (String) getClaims(token).get("role");
        return role != null ? role : "ADMIN";
    }

    @SuppressWarnings("unchecked")
    public List<String> getRolesFromToken(String token) {
        Object roles = getClaims(token).get("roles");
        if (roles instanceof List) {
            return (List<String>) roles;
        }
        String role = getRoleFromToken(token);
        return List.of(role);
    }

    public Long getReaderIdFromToken(String token) {
        Object readerId = getClaims(token).get("readerId");
        if (readerId instanceof Number) {
            return ((Number) readerId).longValue();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public List<String> getPermissionsFromToken(String token) {
        Object perms = getClaims(token).get("permissions");
        if (perms instanceof List) {
            return (List<String>) perms;
        }
        return List.of();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(getKey()).build().parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Claims getClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            throw new com.kidsbook.common.BusinessException(401, "登录已过期，请重新登录");
        } catch (JwtException e) {
            throw new com.kidsbook.common.BusinessException(401, "身份验证失败，请重新登录");
        }
    }
}
