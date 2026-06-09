package com.kidsbook.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kidsbook.annotation.RequirePermission;
import com.kidsbook.common.Result;
import com.kidsbook.dto.BookReviewRequest;
import com.kidsbook.entity.BookReview;
import com.kidsbook.service.BookReviewService;
import com.kidsbook.util.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/reader-center/reviews")
@RequiredArgsConstructor
public class ReaderReviewController {
    private final BookReviewService bookReviewService;
    private final JwtUtil jwtUtil;

    private Long getCurrentReaderId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String token = (String) auth.getCredentials();
        return jwtUtil.getReaderIdFromToken(token);
    }

    @PostMapping
    @RequirePermission("reader-center:review")
    public Result<?> create(@RequestBody @Valid BookReviewRequest request) {
        Long readerId = getCurrentReaderId();
        if (readerId == null) {
            return Result.error("请使用读者账号登录");
        }
        BookReview review = bookReviewService.createReview(readerId, request);
        return Result.success(review);
    }

    @GetMapping("/my")
    @RequirePermission("reader-center:review")
    public Result<?> myReviews(@RequestParam(defaultValue = "1") int page,
                               @RequestParam(defaultValue = "10") int size) {
        Long readerId = getCurrentReaderId();
        if (readerId == null) {
            return Result.error("请使用读者账号登录");
        }
        Page<BookReview> result = bookReviewService.getMyReviews(readerId, page, size);
        Map<String, Object> data = new HashMap<>();
        data.put("records", result.getRecords());
        data.put("total", result.getTotal());
        return Result.success(data);
    }

    @PutMapping("/{id}")
    @RequirePermission("reader-center:review")
    public Result<?> update(@PathVariable Long id, @RequestBody @Valid BookReviewRequest request) {
        Long readerId = getCurrentReaderId();
        if (readerId == null) {
            return Result.error("请使用读者账号登录");
        }
        BookReview review = bookReviewService.updateReview(readerId, id, request);
        return Result.success(review);
    }

    @DeleteMapping("/{id}")
    @RequirePermission("reader-center:review")
    public Result<?> delete(@PathVariable Long id) {
        Long readerId = getCurrentReaderId();
        if (readerId == null) {
            return Result.error("请使用读者账号登录");
        }
        bookReviewService.deleteOwnReview(readerId, id);
        return Result.success(null);
    }

    @GetMapping("/book/{bookId}")
    @RequirePermission("reader-center:review")
    public Result<?> bookReviews(@PathVariable Long bookId,
                                 @RequestParam(defaultValue = "1") int page,
                                 @RequestParam(defaultValue = "10") int size) {
        Page<BookReview> result = bookReviewService.getBookReviews(bookId, page, size);
        Map<String, Object> data = new HashMap<>();
        data.put("records", result.getRecords());
        data.put("total", result.getTotal());
        return Result.success(data);
    }

    @GetMapping("/check/{bookId}")
    @RequirePermission("reader-center:review")
    public Result<?> checkCanReview(@PathVariable Long bookId) {
        Long readerId = getCurrentReaderId();
        if (readerId == null) {
            return Result.error("请使用读者账号登录");
        }
        Map<String, Object> data = new HashMap<>();
        data.put("hasBorrowed", bookReviewService.hasReaderBorrowedBook(readerId, bookId));
        data.put("hasReviewed", bookReviewService.hasReaderReviewed(readerId, bookId));
        return Result.success(data);
    }
}
