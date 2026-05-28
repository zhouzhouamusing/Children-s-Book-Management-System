package com.kidsbook.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.kidsbook.common.Result;
import com.kidsbook.dto.BorrowRequest;
import com.kidsbook.entity.BorrowRecord;
import com.kidsbook.service.BorrowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/borrows")
@RequiredArgsConstructor
public class BorrowController {
    private final BorrowService borrowService;

    @GetMapping
    public Result<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        IPage<BorrowRecord> result = borrowService.list(page, size, keyword, status);
        Map<String, Object> data = new HashMap<>();
        data.put("records", result.getRecords());
        data.put("total", result.getTotal());
        return Result.success(data);
    }

    @PostMapping
    public Result<Void> borrow(@RequestBody @Valid BorrowRequest request) {
        borrowService.borrowBook(request);
        return Result.success(null);
    }

    @PutMapping("/{id}/return")
    public Result<Void> returnBook(@PathVariable Long id) {
        borrowService.returnBook(id);
        return Result.success(null);
    }

    @PutMapping("/{id}/renew")
    public Result<Void> renew(@PathVariable Long id, @RequestParam(defaultValue = "14") Integer days) {
        borrowService.renewBook(id, days);
        return Result.success(null);
    }

    @GetMapping("/statistics")
    public Result<Map<String, Object>> statistics() {
        return Result.success(borrowService.getStatistics());
    }
}
