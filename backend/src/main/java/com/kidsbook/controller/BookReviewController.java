package com.kidsbook.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kidsbook.common.PageResult;
import com.kidsbook.common.Permission;
import com.kidsbook.common.RequirePermission;
import com.kidsbook.common.Result;
import com.kidsbook.dto.AdminReplyRequest;
import com.kidsbook.entity.BookReview;
import com.kidsbook.service.BookReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class BookReviewController {
    private final BookReviewService bookReviewService;

    @GetMapping
    @RequirePermission(Permission.REVIEW_READ)
    public Result<PageResult<BookReview>> list(@RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "10") int size,
                          @RequestParam(required = false) String status,
                          @RequestParam(required = false) Long bookId) {
        Page<BookReview> result = bookReviewService.adminListReviews(page, size, status, bookId);
        return Result.success(PageResult.of(result));
    }

    @PutMapping("/{id}/approve")
    @RequirePermission(Permission.REVIEW_UPDATE)
    public Result<?> approve(@PathVariable Long id) {
        bookReviewService.approveReview(id);
        return Result.success(null);
    }

    @PutMapping("/{id}/reject")
    @RequirePermission(Permission.REVIEW_UPDATE)
    public Result<?> reject(@PathVariable Long id) {
        bookReviewService.rejectReview(id);
        return Result.success(null);
    }

    @PutMapping("/{id}/reply")
    @RequirePermission(Permission.REVIEW_UPDATE)
    public Result<?> reply(@PathVariable Long id, @RequestBody @Valid AdminReplyRequest request) {
        bookReviewService.adminReply(id, request.getReply());
        return Result.success(null);
    }

    @DeleteMapping("/{id}")
    @RequirePermission(Permission.REVIEW_DELETE)
    public Result<?> delete(@PathVariable Long id) {
        bookReviewService.adminDeleteReview(id);
        return Result.success(null);
    }
}
