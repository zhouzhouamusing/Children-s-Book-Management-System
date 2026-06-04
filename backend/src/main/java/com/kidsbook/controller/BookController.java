package com.kidsbook.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kidsbook.common.PageResult;
import com.kidsbook.common.Permission;
import com.kidsbook.common.RequirePermission;
import com.kidsbook.common.Result;
import com.kidsbook.dto.BookRequest;
import com.kidsbook.entity.Book;
import com.kidsbook.service.BookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookController {
    private final BookService bookService;

    @GetMapping
    @RequirePermission(Permission.BOOK_READ)
    public Result<PageResult<Book>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category) {
        return Result.success(PageResult.of(bookService.listBooks(page, size, keyword, category)));
    }

    @PostMapping
    @RequirePermission(Permission.BOOK_CREATE)
    public Result<Map<String, Object>> add(@Valid @RequestBody BookRequest request) {
        Long bookId = bookService.addBook(request);
        Map<String, Object> data = new HashMap<>();
        data.put("id", bookId);
        return Result.success(data);
    }

    @PutMapping("/{id}")
    @RequirePermission(Permission.BOOK_UPDATE)
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody BookRequest request) {
        request.setId(id);
        bookService.updateBook(request);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @RequirePermission(Permission.BOOK_DELETE)
    public Result<Void> delete(@PathVariable Long id) {
        bookService.deleteBook(id);
        return Result.success();
    }

    @GetMapping("/{id}")
    @RequirePermission(Permission.BOOK_READ)
    public Result<Book> detail(@PathVariable Long id) {
        return Result.success(bookService.getById(id));
    }

    @GetMapping("/statistics")
    @RequirePermission(Permission.BOOK_READ)
    public Result<Map<String, Object>> statistics() {
        return Result.success(bookService.getStatistics());
    }

    @GetMapping("/categories")
    @RequirePermission(Permission.BOOK_READ)
    public Result<List<String>> categories() {
        return Result.success(bookService.getAllCategories());
    }
}
