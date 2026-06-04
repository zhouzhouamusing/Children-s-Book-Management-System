package com.kidsbook.controller;

import com.kidsbook.common.Result;
import com.kidsbook.entity.Book;
import com.kidsbook.service.BookRecommendService;
import com.kidsbook.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reader-center/recommend")
@RequiredArgsConstructor
public class BookRecommendController {
    private final BookRecommendService recommendService;
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

    @GetMapping("/by-history")
    public Result<List<Book>> getRecommendByHistory(@RequestParam(defaultValue = "10") int limit) {
        Long readerId = getCurrentReaderId();
        List<Book> books;
        if (readerId != null) {
            books = recommendService.getRecommendByHistory(readerId, limit);
        } else {
            books = recommendService.getTopBorrowedBooks(limit);
        }
        return Result.success(books);
    }

    @GetMapping("/by-age")
    public Result<List<Book>> getRecommendByAge(@RequestParam(defaultValue = "10") int limit) {
        Long readerId = getCurrentReaderId();
        List<Book> books;
        if (readerId != null) {
            books = recommendService.getRecommendByAge(readerId, limit);
        } else {
            books = recommendService.getTopBorrowedBooks(limit);
        }
        return Result.success(books);
    }

    @GetMapping("/by-progress")
    public Result<List<Book>> getRecommendByProgress(@RequestParam(defaultValue = "10") int limit) {
        Long readerId = getCurrentReaderId();
        List<Book> books;
        if (readerId != null) {
            books = recommendService.getRecommendByReadingProgress(readerId, limit);
        } else {
            books = recommendService.getTopRatedBooks(limit);
        }
        return Result.success(books);
    }

    @GetMapping("/top10")
    public Result<List<Map<String, Object>>> getTopBooks() {
        List<Map<String, Object>> top10 = recommendService.getTopBorrowedBooksWithCount(10);
        return Result.success(top10);
    }

    @GetMapping("/all")
    public Result<Map<String, Object>> getAllRecommendations() {
        Long readerId = getCurrentReaderId();
        Map<String, Object> data = new HashMap<>();

        if (readerId != null) {
            data.put("byHistory", recommendService.getRecommendByHistory(readerId, 6));
            data.put("byAge", recommendService.getRecommendByAge(readerId, 6));
            data.put("byProgress", recommendService.getRecommendByReadingProgress(readerId, 6));
        } else {
            data.put("byHistory", recommendService.getTopBorrowedBooks(6));
            data.put("byAge", recommendService.getTopBorrowedBooks(6));
            data.put("byProgress", recommendService.getTopRatedBooks(6));
        }
        data.put("top10", recommendService.getTopBorrowedBooksWithCount(10));
        data.put("topRated", recommendService.getTopRatedBooks(6));

        return Result.success(data);
    }

    @GetMapping("/top-rated")
    public Result<List<Book>> getTopRated(@RequestParam(defaultValue = "10") int limit) {
        return Result.success(recommendService.getTopRatedBooks(limit));
    }
}
