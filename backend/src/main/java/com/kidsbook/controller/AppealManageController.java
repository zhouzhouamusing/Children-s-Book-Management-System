package com.kidsbook.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kidsbook.annotation.RequirePermission;
import com.kidsbook.common.Result;
import com.kidsbook.dto.AppealHandleRequest;
import com.kidsbook.entity.ReaderAppeal;
import com.kidsbook.service.ReaderAppealService;
import com.kidsbook.util.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/appeals")
@RequiredArgsConstructor
public class AppealManageController {

    private final ReaderAppealService appealService;
    private final JwtUtil jwtUtil;

    @GetMapping
    @RequirePermission("appeal:view")
    public Result<Page<ReaderAppeal>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type) {
        return Result.success(appealService.listAppeals(page, size, status, type));
    }

    @GetMapping("/{id}")
    @RequirePermission("appeal:view")
    public Result<ReaderAppeal> detail(@PathVariable Long id) {
        return Result.success(appealService.getAppealDetail(id));
    }

    @PutMapping("/{id}/handle")
    @RequirePermission("appeal:handle")
    public Result<Void> handle(@PathVariable Long id, @Valid @RequestBody AppealHandleRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String token = auth.getCredentials().toString();
        Long adminId = jwtUtil.getUserIdFromToken(token);
        appealService.handleAppeal(id, adminId, request);
        return Result.success(null);
    }
}
