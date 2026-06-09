package com.kidsbook.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kidsbook.common.Result;
import com.kidsbook.dto.AppealRequest;
import com.kidsbook.entity.ReaderAppeal;
import com.kidsbook.service.ReaderAppealService;
import com.kidsbook.util.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reader-center/appeals")
@RequiredArgsConstructor
public class ReaderAppealController {

    private final ReaderAppealService appealService;
    private final JwtUtil jwtUtil;

    @PostMapping
    public Result<Void> submit(@Valid @RequestBody AppealRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String token = auth.getCredentials().toString();
        Long readerId = jwtUtil.getReaderIdFromToken(token);
        String username = jwtUtil.getUsernameFromToken(token);
        if (readerId == null) {
            throw new RuntimeException("无法获取读者信息");
        }
        appealService.submitAppeal(readerId, username, request);
        return Result.success(null);
    }

    @GetMapping("/my")
    public Result<Page<ReaderAppeal>> myAppeals(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String token = auth.getCredentials().toString();
        Long readerId = jwtUtil.getReaderIdFromToken(token);
        if (readerId == null) {
            throw new RuntimeException("无法获取读者信息");
        }
        return Result.success(appealService.getMyAppeals(readerId, page, size));
    }

    @GetMapping("/{id}")
    public Result<ReaderAppeal> detail(@PathVariable Long id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String token = auth.getCredentials().toString();
        Long readerId = jwtUtil.getReaderIdFromToken(token);
        ReaderAppeal appeal = appealService.getAppealDetail(id);
        if (!appeal.getReaderId().equals(readerId)) {
            throw new RuntimeException("无权查看此申诉");
        }
        return Result.success(appeal);
    }
}
