package com.kidsbook.controller;

import com.kidsbook.common.Result;
import com.kidsbook.dto.LoginRequest;
import com.kidsbook.dto.LoginResponse;
import com.kidsbook.dto.ReaderRegisterRequest;
import com.kidsbook.service.ReaderAccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/reader")
@RequiredArgsConstructor
public class ReaderAuthController {
    private final ReaderAccountService readerAccountService;

    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody @Valid LoginRequest request) {
        LoginResponse response = readerAccountService.login(request);
        return Result.success(response);
    }

    @PostMapping("/register")
    public Result<Void> register(@RequestBody @Valid ReaderRegisterRequest request) {
        readerAccountService.register(request);
        return Result.success(null);
    }

    @PostMapping("/send-code")
    public Result<Void> sendCode(@RequestBody Map<String, String> body) {
        String email = body != null ? body.get("email") : null;
        readerAccountService.sendResetCode(email);
        return Result.success(null);
    }

    @PostMapping("/reset-password")
    public Result<Void> resetPassword(@RequestBody Map<String, String> body) {
        if (body == null) {
            return Result.error(400, "请求参数不能为空");
        }
        readerAccountService.resetPassword(
            body.get("username"),
            body.get("email"),
            body.get("code"),
            body.get("newPassword"),
            body.get("confirmPassword")
        );
        return Result.success(null);
    }
}
