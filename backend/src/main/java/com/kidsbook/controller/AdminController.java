package com.kidsbook.controller;

import com.kidsbook.common.Result;
import com.kidsbook.dto.LoginRequest;
import com.kidsbook.dto.LoginResponse;
import com.kidsbook.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {
    private final AdminService adminService;

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("收到登录请求: username={}", request.getUsername());
        try {
            LoginResponse response = adminService.login(request);
            log.info("登录成功: username={}", request.getUsername());
            return Result.success(response);
        } catch (Exception e) {
            log.warn("登录失败: username={}, reason={}", request.getUsername(), e.getMessage());
            String msg = e.getMessage() != null ? e.getMessage() : "登录失败，请检查用户名和密码";
            return Result.error(401, msg);
        }
    }
}
