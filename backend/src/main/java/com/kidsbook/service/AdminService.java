package com.kidsbook.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kidsbook.common.BusinessException;
import com.kidsbook.entity.Admin;
import com.kidsbook.mapper.AdminMapper;
import com.kidsbook.dto.LoginRequest;
import com.kidsbook.dto.LoginResponse;
import com.kidsbook.dto.RegisterRequest;
import com.kidsbook.dto.ResetPasswordRequest;
import com.kidsbook.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService extends ServiceImpl<AdminMapper, Admin> {
    private final AdminMapper adminMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;
    private final AuditLogService auditLogService;
    private final RbacService rbacService;

    private static final int MAX_RESET_ATTEMPTS = 5;
    private static final long RESET_WINDOW_MS = 15 * 60 * 1000;
    private final ConcurrentHashMap<String, long[]> resetAttempts = new ConcurrentHashMap<>();

    public LoginResponse login(LoginRequest request) {
        Admin admin = adminMapper.selectOne(
            new LambdaQueryWrapper<Admin>().eq(Admin::getUsername, request.getUsername())
        );

        if (admin == null) {
            throw new BusinessException(401, "用户名或密码错误");
        }

        if (!passwordEncoder.matches(request.getPassword(), admin.getPassword())) {
            throw new BusinessException(401, "用户名或密码错误");
        }

        List<String> roles = rbacService.getRolesForUser("ADMIN", admin.getId());
        if (roles.isEmpty()) {
            roles = List.of("ADMIN");
        }
        Set<String> permSet = rbacService.getEffectivePermissions("ADMIN", admin.getId());
        List<String> permissions = new ArrayList<>(permSet);

        String token = jwtUtil.generateToken(admin.getUsername(), roles, permissions, null);

        auditLogService.logAsync(admin.getUsername(), "ADMIN", "ADMIN_LOGIN", "admin", admin.getId(), "管理员登录", null);

        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setNickname(admin.getNickname());
        response.setAvatar(admin.getAvatar());
        response.setRole(roles.contains("SUPER_ADMIN") ? "ADMIN" : roles.get(0));
        response.setRoles(roles);
        response.setPermissions(permissions);
        return response;
    }

    public void register(RegisterRequest request) {
        throw new BusinessException(400, "管理员账号不支持自主注册，请联系超级管理员添加");
    }

    public void resetPassword(ResetPasswordRequest request) {
        if (request.getUsername() == null || request.getUsername().isBlank()) {
            throw new BusinessException(400, "请输入用户名");
        }
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new BusinessException(400, "请输入邮箱");
        }

        checkResetRateLimit(request.getUsername());

        String newPassword = request.getNewPassword();
        if (newPassword == null || newPassword.length() < 8) {
            throw new BusinessException(400, "密码长度不能少于8位");
        }
        if (!newPassword.matches(".*[A-Za-z].*") || !newPassword.matches(".*\\d.*")) {
            throw new BusinessException(400, "密码必须包含字母和数字");
        }

        if (!newPassword.equals(request.getConfirmPassword())) {
            throw new BusinessException(400, "两次输入的密码不一致");
        }

        Admin admin = adminMapper.selectOne(
            new LambdaQueryWrapper<Admin>()
                .eq(Admin::getUsername, request.getUsername())
                .eq(Admin::getEmail, request.getEmail())
        );

        if (admin == null) {
            throw new BusinessException(400, "用户名与邮箱不匹配");
        }

        if (!emailService.verifyCode(request.getEmail(), request.getCode())) {
            throw new BusinessException(400, "验证码错误或已过期");
        }

        admin.setPassword(passwordEncoder.encode(newPassword));
        adminMapper.updateById(admin);
        resetAttempts.remove(request.getUsername());
        log.info("密码重置成功: username={}", request.getUsername());
    }

    private void checkResetRateLimit(String username) {
        long now = System.currentTimeMillis();
        resetAttempts.compute(username, (key, attempts) -> {
            if (attempts == null) {
                return new long[]{1, now};
            }
            if (now - attempts[1] > RESET_WINDOW_MS) {
                return new long[]{1, now};
            }
            attempts[0]++;
            return attempts;
        });
        long[] current = resetAttempts.get(username);
        if (current != null && current[0] > MAX_RESET_ATTEMPTS) {
            throw new BusinessException(429, "密码重置尝试过于频繁，请15分钟后再试");
        }
    }
}
