package com.kidsbook.controller;

import com.kidsbook.common.Result;
import com.kidsbook.dto.LoginRequest;
import com.kidsbook.dto.LoginResponse;
import com.kidsbook.dto.RegisterRequest;
import com.kidsbook.dto.ResetPasswordRequest;
import com.kidsbook.service.AdminService;
import com.kidsbook.service.EmailService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {
    private final AdminService adminService;
    private final EmailService emailService;

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("收到登录请求: username={}", request.getUsername());
        try {
            LoginResponse response = adminService.login(request);
            return Result.success(response);
        } catch (Exception e) {
            log.warn("登录失败: username={}, reason={}", request.getUsername(), e.getMessage());
            return Result.error(401, e.getMessage() != null ? e.getMessage() : "登录失败，请检查用户名和密码");
        }
    }

    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterRequest request) {
        log.info("收到注册请求: username={}", request.getUsername());
        try {
            adminService.register(request);
            return Result.success();
        } catch (Exception e) {
            log.warn("注册失败: username={}, reason={}", request.getUsername(), e.getMessage());
            return Result.error(400, e.getMessage() != null ? e.getMessage() : "注册失败");
        }
    }

    @PostMapping("/send-code")
    public Result<Void> sendCode(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null || email.isBlank()) {
            return Result.error(400, "邮箱不能为空");
        }
        try {
            emailService.sendVerificationCode(email);
            return Result.success();
        } catch (Exception e) {
            log.warn("验证码发送失败: email={}, reason={}", email, e.getMessage());
            return Result.error(400, e.getMessage());
        }
    }

    @PostMapping("/reset-password")
    public Result<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        log.info("收到密码重置请求: username={}", request.getUsername());
        try {
            adminService.resetPassword(request);
            return Result.success();
        } catch (Exception e) {
            log.warn("密码重置失败: username={}, reason={}", request.getUsername(), e.getMessage());
            return Result.error(400, e.getMessage() != null ? e.getMessage() : "密码重置失败");
        }
    }
}
