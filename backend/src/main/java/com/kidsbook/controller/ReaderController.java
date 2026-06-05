package com.kidsbook.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kidsbook.common.PageResult;
import com.kidsbook.common.Permission;
import com.kidsbook.common.RequirePermission;
import com.kidsbook.common.Result;
import com.kidsbook.dto.ReaderRequest;
import com.kidsbook.entity.AuditLog;
import com.kidsbook.entity.BorrowRecord;
import com.kidsbook.entity.ReadingProgress;
import com.kidsbook.entity.Reader;
import com.kidsbook.service.AuditLogService;
import com.kidsbook.service.ReaderService;
import com.kidsbook.service.ReadingProgressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/readers")
@RequiredArgsConstructor
public class ReaderController {

    private final ReaderService readerService;
    private final ReadingProgressService readingProgressService;
    private final AuditLogService auditLogService;

    @GetMapping
    @RequirePermission(Permission.READER_READ)
    public Result<PageResult<Reader>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String gender
    ) {
        Page<Reader> result = readerService.listReaders(page, size, keyword, status, gender);
        return Result.success(PageResult.of(result));
    }

    @GetMapping("/{id}")
    @RequirePermission(Permission.READER_READ)
    public Result<Reader> getById(@PathVariable Long id) {
        Reader reader = readerService.getById(id);
        if (reader == null) {
            return Result.error("读者不存在");
        }
        return Result.success(reader);
    }

    @PostMapping
    @RequirePermission(Permission.READER_CREATE)
    public Result<Void> add(@Valid @RequestBody ReaderRequest request) {
        readerService.addReader(request);
        return Result.success();
    }

    @PutMapping("/{id}")
    @RequirePermission(Permission.READER_UPDATE)
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody ReaderRequest request) {
        readerService.updateReader(id, request);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @RequirePermission(Permission.READER_DELETE)
    public Result<Void> delete(@PathVariable Long id) {
        readerService.deleteReader(id);
        return Result.success();
    }

    @PutMapping("/{id}/status")
    @RequirePermission(Permission.READER_UPDATE)
    public Result<Void> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String status = body.get("status");
        if (!"normal".equals(status) && !"suspended".equals(status)) {
            return Result.error("状态值无效，仅支持 normal 或 suspended");
        }
        readerService.updateStatus(id, status);
        return Result.success();
    }

    @GetMapping("/{id}/borrow-records")
    @RequirePermission(Permission.READER_READ)
    public Result<PageResult<BorrowRecord>> getBorrowRecords(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status
    ) {
        Page<BorrowRecord> records = readerService.getBorrowRecords(id, page, size, status);
        return Result.success(PageResult.of(records));
    }

    @GetMapping("/statistics")
    @RequirePermission(Permission.READER_READ)
    public Result<Map<String, Object>> statistics() {
        return Result.success(readerService.getStatistics());
    }

    @GetMapping("/{id}/reading-progress")
    @RequirePermission(Permission.READER_READ)
    public Result<PageResult<ReadingProgress>> getReadingProgress(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {
        Reader reader = readerService.getById(id);
        if (reader == null) {
            return Result.error("读者不存在");
        }
        IPage<ReadingProgress> result = readingProgressService.getProgressList(id, page, size, status);
        return Result.success(PageResult.of(result));
    }

    @GetMapping("/{id}/reading-statistics")
    @RequirePermission(Permission.READER_READ)
    public Result<Map<String, Object>> getReadingStatistics(@PathVariable Long id) {
        Reader reader = readerService.getById(id);
        if (reader == null) {
            return Result.error("读者不存在");
        }
        Map<String, Object> stats = readingProgressService.getStatistics(id);
        stats.put("readerName", reader.getName());
        stats.put("totalReadingDays", reader.getTotalReadingDays() != null ? reader.getTotalReadingDays() : 0);
        return Result.success(stats);
    }

    @GetMapping("/suspension-appeals")
    @RequirePermission(Permission.READER_READ)
    public Result<PageResult<AuditLog>> getSuspensionAppeals(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        IPage<AuditLog> result = auditLogService.listLogs(page, size, "SUSPENSION_APPEAL", null);
        return Result.success(PageResult.of(result));
    }

    @PutMapping("/{id}/resolve-appeal")
    @RequirePermission(Permission.READER_UPDATE)
    public Result<Void> resolveAppeal(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String action = body != null ? body.get("action") : null;
        if (!"approve".equals(action) && !"deny".equals(action)) {
            return Result.error("操作无效，仅支持 approve 或 deny");
        }
        if ("approve".equals(action)) {
            readerService.updateStatus(id, "normal");
        }
        auditLogService.log("RESOLVE_APPEAL", "reader", id,
            "approve".equals(action) ? "批准暂停申诉，恢复借阅权限" : "拒绝暂停申诉");
        return Result.success();
    }

    @DeleteMapping("/batch")
    @RequirePermission(Permission.READER_BATCH_DELETE)
    public Result<Void> batchDelete(@RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Result.success();
        }
        for (Long id : ids) {
            readerService.deleteReader(id);
        }
        return Result.success();
    }

    @GetMapping("/export")
    @RequirePermission(Permission.READER_EXPORT)
    public Result<PageResult<Reader>> exportReaders(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String gender) {
        Page<Reader> result = readerService.listReaders(1, 10000, keyword, status, gender);
        return Result.success(PageResult.of(result));
    }
}
