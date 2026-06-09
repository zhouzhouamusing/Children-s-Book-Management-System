package com.kidsbook.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.kidsbook.annotation.RequirePermission;
import com.kidsbook.common.Result;
import com.kidsbook.entity.ReadingNote;
import com.kidsbook.entity.ReadingProgress;
import com.kidsbook.service.ReadingProgressService;
import com.kidsbook.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/reader-center/reading-progress")
@RequiredArgsConstructor
public class ReadingProgressController {
    private final ReadingProgressService progressService;
    private final JwtUtil jwtUtil;

    private Long getCurrentReaderId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String token = (String) auth.getCredentials();
        return jwtUtil.getReaderIdFromToken(token);
    }

    @GetMapping
    @RequirePermission("reader-center:progress")
    public Result<Map<String, Object>> getProgressList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {
        Long readerId = getCurrentReaderId();
        if (readerId == null) {
            throw new RuntimeException("无法获取读者信息");
        }
        IPage<ReadingProgress> result = progressService.getProgressList(readerId, page, size, status);
        Map<String, Object> data = new HashMap<>();
        data.put("records", result.getRecords());
        data.put("total", result.getTotal());
        return Result.success(data);
    }

    @PostMapping
    @RequirePermission("reader-center:progress")
    public Result<ReadingProgress> createOrUpdate(@RequestBody Map<String, Object> body) {
        Long readerId = getCurrentReaderId();
        if (readerId == null) {
            throw new RuntimeException("无法获取读者信息");
        }
        Long bookId = Long.valueOf(body.get("bookId").toString());
        Integer totalPages = body.get("totalPages") != null ? Integer.valueOf(body.get("totalPages").toString()) : null;
        Integer currentPage = body.get("currentPage") != null ? Integer.valueOf(body.get("currentPage").toString()) : null;
        Integer readingMinutes = body.get("readingMinutes") != null ? Integer.valueOf(body.get("readingMinutes").toString()) : null;

        ReadingProgress progress = progressService.createOrUpdateProgress(readerId, bookId, totalPages, currentPage, readingMinutes);
        return Result.success(progress);
    }

    @PutMapping("/{id}/status")
    @RequirePermission("reader-center:progress")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Long readerId = getCurrentReaderId();
        if (readerId == null) {
            throw new RuntimeException("无法获取读者信息");
        }
        progressService.updateProgressStatus(readerId, id, body.get("status"));
        return Result.success(null);
    }

    @DeleteMapping("/{id}")
    @RequirePermission("reader-center:progress")
    public Result<Void> deleteProgress(@PathVariable Long id) {
        Long readerId = getCurrentReaderId();
        if (readerId == null) {
            throw new RuntimeException("无法获取读者信息");
        }
        progressService.deleteProgress(readerId, id);
        return Result.success(null);
    }

    @GetMapping("/statistics")
    @RequirePermission("reader-center:progress")
    public Result<Map<String, Object>> getStatistics() {
        Long readerId = getCurrentReaderId();
        if (readerId == null) {
            throw new RuntimeException("无法获取读者信息");
        }
        Map<String, Object> stats = progressService.getStatistics(readerId);
        return Result.success(stats);
    }

    @GetMapping("/notes")
    @RequirePermission("reader-center:progress")
    public Result<Map<String, Object>> getNotes(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long bookId) {
        Long readerId = getCurrentReaderId();
        if (readerId == null) {
            throw new RuntimeException("无法获取读者信息");
        }
        IPage<ReadingNote> result = progressService.getNotes(readerId, bookId, page, size);
        Map<String, Object> data = new HashMap<>();
        data.put("records", result.getRecords());
        data.put("total", result.getTotal());
        return Result.success(data);
    }

    @PostMapping("/notes")
    @RequirePermission("reader-center:progress")
    public Result<ReadingNote> addNote(@RequestBody Map<String, Object> body) {
        Long readerId = getCurrentReaderId();
        if (readerId == null) {
            throw new RuntimeException("无法获取读者信息");
        }
        Long bookId = Long.valueOf(body.get("bookId").toString());
        Long progressId = body.get("progressId") != null ? Long.valueOf(body.get("progressId").toString()) : null;
        String content = (String) body.get("content");
        Integer pageNumber = body.get("pageNumber") != null ? Integer.valueOf(body.get("pageNumber").toString()) : null;

        ReadingNote note = progressService.addNote(readerId, bookId, progressId, content, pageNumber);
        return Result.success(note);
    }

    @PutMapping("/notes/{id}")
    @RequirePermission("reader-center:progress")
    public Result<Void> updateNote(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Long readerId = getCurrentReaderId();
        if (readerId == null) {
            throw new RuntimeException("无法获取读者信息");
        }
        String content = (String) body.get("content");
        Integer pageNumber = body.get("pageNumber") != null ? Integer.valueOf(body.get("pageNumber").toString()) : null;
        progressService.updateNote(readerId, id, content, pageNumber);
        return Result.success(null);
    }

    @DeleteMapping("/notes/{id}")
    @RequirePermission("reader-center:progress")
    public Result<Void> deleteNote(@PathVariable Long id) {
        Long readerId = getCurrentReaderId();
        if (readerId == null) {
            throw new RuntimeException("无法获取读者信息");
        }
        progressService.deleteNote(readerId, id);
        return Result.success(null);
    }
}
