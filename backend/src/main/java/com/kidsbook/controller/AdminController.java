package com.kidsbook.controller;

import com.kidsbook.common.BusinessException;
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
        LoginResponse response = adminService.login(request);
        return Result.success(response);
    }

    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterRequest request) {
        adminService.register(request);
        return Result.success();
    }

    @PostMapping("/send-code")
    public Result<Void> sendCode(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null || email.isBlank()) {
            throw new BusinessException(400, "邮箱不能为空");
        }
        emailService.sendVerificationCode(email);
        return Result.success();
    }

    @PostMapping("/reset-password")
    public Result<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        adminService.resetPassword(request);
        return Result.success();
    }
}
