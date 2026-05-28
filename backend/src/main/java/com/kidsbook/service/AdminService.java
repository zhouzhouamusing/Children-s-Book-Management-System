package com.kidsbook.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {
    private final AdminMapper adminMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;

    public LoginResponse login(LoginRequest request) {
        Admin admin = adminMapper.selectOne(
            new LambdaQueryWrapper<Admin>().eq(Admin::getUsername, request.getUsername())
        );

        if (admin == null) {
            throw new RuntimeException("用户名或密码错误");
        }

        if (!passwordEncoder.matches(request.getPassword(), admin.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }

        String token = jwtUtil.generateToken(admin.getUsername(), "ADMIN", null);

        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setNickname(admin.getNickname());
        response.setAvatar(admin.getAvatar());
        response.setRole("ADMIN");
        return response;
    }

    public void register(RegisterRequest request) {
        throw new RuntimeException("管理员账号不支持自主注册，请联系超级管理员添加");
    }

    public void resetPassword(ResetPasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("两次输入的密码不一致");
        }

        if (!emailService.verifyCode(request.getEmail(), request.getCode())) {
            throw new RuntimeException("验证码错误或已过期");
        }

        Admin admin = adminMapper.selectOne(
            new LambdaQueryWrapper<Admin>()
                .eq(Admin::getUsername, request.getUsername())
                .eq(Admin::getEmail, request.getEmail())
        );

        if (admin == null) {
            throw new RuntimeException("用户名与邮箱不匹配");
        }

        admin.setPassword(passwordEncoder.encode(request.getNewPassword()));
        adminMapper.updateById(admin);
        log.info("密码重置成功: username={}", request.getUsername());
    }
}
