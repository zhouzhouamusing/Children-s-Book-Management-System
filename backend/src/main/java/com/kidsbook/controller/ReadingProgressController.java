package com.kidsbook.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.kidsbook.common.BusinessException;
import com.kidsbook.common.PageResult;
import com.kidsbook.common.Permission;
import com.kidsbook.common.RequirePermission;
import com.kidsbook.common.Result;
import com.kidsbook.entity.ReadingNote;
import com.kidsbook.entity.ReadingProgress;
import com.kidsbook.service.ReadingProgressService;
import com.kidsbook.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/reader-center/reading-progress")
@RequiredArgsConstructor
public class ReadingProgressController {
    private final ReadingProgressService progressService;
    private final JwtUtil jwtUtil;

    private Long getCurrentReaderId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getCredentials() == null) {
            throw new BusinessException(401, "无法获取读者信息，请重新登录");
        }
        try {
            String token = auth.getCredentials().toString();
            Long readerId = jwtUtil.getReaderIdFromToken(token);
            if (readerId == null) {
                throw new BusinessException(401, "无法获取读者信息，请使用读者账号登录");
            }
            return readerId;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(401, "无法获取读者信息，请重新登录");
        }
    }

    @GetMapping
    @RequirePermission(Permission.READING_PROGRESS_READ)
    public Result<PageResult<ReadingProgress>> getProgressList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {
        Long readerId = getCurrentReaderId();
        IPage<ReadingProgress> result = progressService.getProgressList(readerId, page, size, status);
        return Result.success(PageResult.of(result));
    }

    @PostMapping
    @RequirePermission(Permission.READING_PROGRESS_CREATE)
    public Result<ReadingProgress> createOrUpdate(@RequestBody Map<String, Object> body) {
        Long readerId = getCurrentReaderId();
        if (body.get("bookId") == null) {
            throw new BusinessException(400, "图书ID不能为空");
        }
        Long bookId = Long.valueOf(body.get("bookId").toString());
        Integer totalPages = body.get("totalPages") != null ? Integer.valueOf(body.get("totalPages").toString()) : null;
        Integer currentPage = body.get("currentPage") != null ? Integer.valueOf(body.get("currentPage").toString()) : null;
        Integer readingMinutes = body.get("readingMinutes") != null ? Integer.valueOf(body.get("readingMinutes").toString()) : null;

        ReadingProgress progress = progressService.createOrUpdateProgress(readerId, bookId, totalPages, currentPage, readingMinutes);
        return Result.success(progress);
    }

    @PutMapping("/{id}/status")
    @RequirePermission(Permission.READING_PROGRESS_UPDATE)
    public Result<Void> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Long readerId = getCurrentReaderId();
        String status = body.get("status");
        if (status == null || status.isEmpty()) {
            throw new BusinessException(400, "状态不能为空");
        }
        progressService.updateProgressStatus(readerId, id, status);
        return Result.success(null);
    }

    @DeleteMapping("/{id}")
    @RequirePermission(Permission.READING_PROGRESS_DELETE)
    public Result<Void> deleteProgress(@PathVariable Long id) {
        Long readerId = getCurrentReaderId();
        progressService.deleteProgress(readerId, id);
        return Result.success(null);
    }

    @GetMapping("/statistics")
    @RequirePermission(Permission.READING_PROGRESS_READ)
    public Result<Map<String, Object>> getStatistics() {
        Long readerId = getCurrentReaderId();
        Map<String, Object> stats = progressService.getStatistics(readerId);
        return Result.success(stats);
    }

    @GetMapping("/notes")
    @RequirePermission(Permission.READING_PROGRESS_READ)
    public Result<PageResult<ReadingNote>> getNotes(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long bookId) {
        Long readerId = getCurrentReaderId();
        IPage<ReadingNote> result = progressService.getNotes(readerId, bookId, page, size);
        return Result.success(PageResult.of(result));
    }

    @PostMapping("/notes")
    @RequirePermission(Permission.READING_PROGRESS_CREATE)
    public Result<ReadingNote> addNote(@RequestBody Map<String, Object> body) {
        Long readerId = getCurrentReaderId();
        if (body.get("bookId") == null) {
            throw new BusinessException(400, "图书ID不能为空");
        }
        Long bookId = Long.valueOf(body.get("bookId").toString());
        Long progressId = body.get("progressId") != null ? Long.valueOf(body.get("progressId").toString()) : null;
        String content = (String) body.get("content");
        if (content == null || content.trim().isEmpty()) {
            throw new BusinessException(400, "笔记内容不能为空");
        }
        Integer pageNumber = body.get("pageNumber") != null ? Integer.valueOf(body.get("pageNumber").toString()) : null;

        ReadingNote note = progressService.addNote(readerId, bookId, progressId, content, pageNumber);
        return Result.success(note);
    }

    @PutMapping("/notes/{id}")
    @RequirePermission(Permission.READING_PROGRESS_UPDATE)
    public Result<Void> updateNote(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Long readerId = getCurrentReaderId();
        String content = (String) body.get("content");
        if (content == null || content.trim().isEmpty()) {
            throw new BusinessException(400, "笔记内容不能为空");
        }
        Integer pageNumber = body.get("pageNumber") != null ? Integer.valueOf(body.get("pageNumber").toString()) : null;
        progressService.updateNote(readerId, id, content, pageNumber);
        return Result.success(null);
    }

    @DeleteMapping("/notes/{id}")
    @RequirePermission(Permission.READING_PROGRESS_DELETE)
    public Result<Void> deleteNote(@PathVariable Long id) {
        Long readerId = getCurrentReaderId();
        progressService.deleteNote(readerId, id);
        return Result.success(null);
    }
}
