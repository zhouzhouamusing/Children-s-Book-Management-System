package com.kidsbook.controller;

import com.kidsbook.common.Result;
import com.kidsbook.dto.LoginRequest;
import com.kidsbook.dto.LoginResponse;
import com.kidsbook.dto.ReaderRegisterRequest;
import com.kidsbook.service.ReaderAccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/reader")
@RequiredArgsConstructor
public class ReaderAuthController {
    private final ReaderAccountService readerAccountService;

    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody @Valid LoginRequest request) {
        log.info("收到读者登录请求: username={}", request.getUsername());
        try {
            LoginResponse response = readerAccountService.login(request);
            return Result.success(response);
        } catch (Exception e) {
            log.warn("读者登录失败: username={}, reason={}", request.getUsername(), e.getMessage());
            return Result.error(401, e.getMessage() != null ? e.getMessage() : "登录失败，请检查用户名和密码");
        }
    }

    @PostMapping("/register")
    public Result<Void> register(@RequestBody @Valid ReaderRegisterRequest request) {
        try {
            readerAccountService.register(request);
            return Result.success(null);
        } catch (Exception e) {
            log.warn("读者注册失败: reason={}", e.getMessage());
            return Result.error(400, e.getMessage() != null ? e.getMessage() : "注册失败");
        }
    }
}
