package com.kidsbook.controller;

import com.kidsbook.common.Result;
import com.kidsbook.dto.LoginRequest;
import com.kidsbook.dto.LoginResponse;
import com.kidsbook.dto.ReaderRegisterRequest;
import com.kidsbook.service.ReaderAccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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
}
