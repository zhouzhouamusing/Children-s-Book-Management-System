package com.kidsbook.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kidsbook.common.BusinessException;
import com.kidsbook.common.PageResult;
import com.kidsbook.common.Permission;
import com.kidsbook.common.RequirePermission;
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
        if (auth == null || auth.getCredentials() == null) {
            return null;
        }
        try {
            String token = auth.getCredentials().toString();
            return jwtUtil.getReaderIdFromToken(token);
        } catch (Exception e) {
            return null;
        }
    }

    @PostMapping
    @RequirePermission(Permission.READER_REVIEW_CREATE)
    public Result<?> create(@RequestBody @Valid BookReviewRequest request) {
        Long readerId = getCurrentReaderId();
        if (readerId == null) {
            throw new BusinessException(401, "请使用读者账号登录");
        }
        BookReview review = bookReviewService.createReview(readerId, request);
        return Result.success(review);
    }

    @GetMapping("/my")
    @RequirePermission(Permission.READER_REVIEW_READ)
    public Result<PageResult<BookReview>> myReviews(@RequestParam(defaultValue = "1") int page,
                               @RequestParam(defaultValue = "10") int size) {
        Long readerId = getCurrentReaderId();
        if (readerId == null) {
            throw new BusinessException(401, "请使用读者账号登录");
        }
        Page<BookReview> result = bookReviewService.getMyReviews(readerId, page, size);
        return Result.success(PageResult.of(result));
    }

    @PutMapping("/{id}")
    @RequirePermission(Permission.READER_REVIEW_UPDATE)
    public Result<?> update(@PathVariable Long id, @RequestBody @Valid BookReviewRequest request) {
        Long readerId = getCurrentReaderId();
        if (readerId == null) {
            throw new BusinessException(401, "请使用读者账号登录");
        }
        BookReview review = bookReviewService.updateReview(readerId, id, request);
        return Result.success(review);
    }

    @DeleteMapping("/{id}")
    @RequirePermission(Permission.READER_REVIEW_DELETE)
    public Result<?> delete(@PathVariable Long id) {
        Long readerId = getCurrentReaderId();
        if (readerId == null) {
            throw new BusinessException(401, "请使用读者账号登录");
        }
        bookReviewService.deleteOwnReview(readerId, id);
        return Result.success(null);
    }

    @GetMapping("/book/{bookId}")
    @RequirePermission(Permission.READER_REVIEW_READ)
    public Result<PageResult<BookReview>> bookReviews(@PathVariable Long bookId,
                                 @RequestParam(defaultValue = "1") int page,
                                 @RequestParam(defaultValue = "10") int size) {
        Page<BookReview> result = bookReviewService.getBookReviews(bookId, page, size);
        return Result.success(PageResult.of(result));
    }

    @GetMapping("/check/{bookId}")
    @RequirePermission(Permission.READER_REVIEW_READ)
    public Result<?> checkCanReview(@PathVariable Long bookId) {
        Long readerId = getCurrentReaderId();
        if (readerId == null) {
            throw new BusinessException(401, "请使用读者账号登录");
        }
        Map<String, Object> data = new HashMap<>();
        data.put("hasBorrowed", bookReviewService.hasReaderBorrowedBook(readerId, bookId));
        data.put("hasReviewed", bookReviewService.hasReaderReviewed(readerId, bookId));
        return Result.success(data);
    }
}
