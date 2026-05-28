package com.kidsbook.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kidsbook.common.Result;
import com.kidsbook.dto.ReaderRequest;
import com.kidsbook.entity.BorrowRecord;
import com.kidsbook.entity.Reader;
import com.kidsbook.service.ReaderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/readers")
@RequiredArgsConstructor
public class ReaderController {

    private final ReaderService readerService;

    @GetMapping
    public Result<Page<Reader>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String gender
    ) {
        Page<Reader> result = readerService.listReaders(page, size, keyword, status, gender);
        return Result.success(result);
    }

    @GetMapping("/{id}")
    public Result<Reader> getById(@PathVariable Long id) {
        Reader reader = readerService.getById(id);
        if (reader == null) {
            return Result.error("读者不存在");
        }
        return Result.success(reader);
    }

    @PostMapping
    public Result<Void> add(@Valid @RequestBody ReaderRequest request) {
        readerService.addReader(request);
        return Result.success();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody ReaderRequest request) {
        readerService.updateReader(id, request);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        readerService.deleteReader(id);
        return Result.success();
    }

    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String status = body.get("status");
        if (!"normal".equals(status) && !"suspended".equals(status)) {
            return Result.error("状态值无效，仅支持 normal 或 suspended");
        }
        readerService.updateStatus(id, status);
        return Result.success();
    }

    @GetMapping("/{id}/borrow-records")
    public Result<Page<BorrowRecord>> getBorrowRecords(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status
    ) {
        Page<BorrowRecord> records = readerService.getBorrowRecords(id, page, size, status);
        return Result.success(records);
    }

    @GetMapping("/statistics")
    public Result<Map<String, Object>> statistics() {
        return Result.success(readerService.getStatistics());
    }
}
