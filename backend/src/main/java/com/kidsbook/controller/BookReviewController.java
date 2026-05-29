package com.kidsbook.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kidsbook.common.Result;
import com.kidsbook.dto.AdminReplyRequest;
import com.kidsbook.entity.BookReview;
import com.kidsbook.service.BookReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class BookReviewController {
    private final BookReviewService bookReviewService;

    @GetMapping
    public Result<?> list(@RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "10") int size,
                          @RequestParam(required = false) String status,
                          @RequestParam(required = false) Long bookId) {
        Page<BookReview> result = bookReviewService.adminListReviews(page, size, status, bookId);
        Map<String, Object> data = new HashMap<>();
        data.put("records", result.getRecords());
        data.put("total", result.getTotal());
        return Result.success(data);
    }

    @PutMapping("/{id}/approve")
    public Result<?> approve(@PathVariable Long id) {
        bookReviewService.approveReview(id);
        return Result.success(null);
    }

    @PutMapping("/{id}/reject")
    public Result<?> reject(@PathVariable Long id) {
        bookReviewService.rejectReview(id);
        return Result.success(null);
    }

    @PutMapping("/{id}/reply")
    public Result<?> reply(@PathVariable Long id, @RequestBody @Valid AdminReplyRequest request) {
        bookReviewService.adminReply(id, request.getReply());
        return Result.success(null);
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        bookReviewService.adminDeleteReview(id);
        return Result.success(null);
    }
}
