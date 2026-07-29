package com.kidsbook.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kidsbook.annotation.RequirePermission;
import com.kidsbook.common.Result;
import com.kidsbook.dto.BookRequest;
import com.kidsbook.entity.Book;
import com.kidsbook.service.BookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookController {
    private final BookService bookService;

    @GetMapping
    @RequirePermission("book:view")
    public Result<Page<Book>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category) {
        return Result.success(bookService.listBooks(page, size, keyword, category));
    }

    @PostMapping
    @RequirePermission("book:add")
    public Result<Void> add(@Valid @RequestBody BookRequest request) {
        bookService.addBook(request);
        return Result.success();
    }

    @PutMapping("/{id}")
    @RequirePermission("book:edit")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody BookRequest request) {
        request.setId(id);
        bookService.updateBook(request);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @RequirePermission("book:delete")
    public Result<Void> delete(@PathVariable Long id) {
        bookService.deleteBook(id);
        return Result.success();
    }

    @GetMapping("/{id}")
    @RequirePermission("book:view")
    public Result<Book> detail(@PathVariable Long id) {
        return Result.success(bookService.getById(id));
    }

    @GetMapping("/statistics")
    @RequirePermission("book:view")
    public Result<Map<String, Object>> statistics() {
        return Result.success(bookService.getStatistics());
    }

    @GetMapping("/categories")
    @RequirePermission("book:view")
    public Result<List<String>> categories() {
        return Result.success(bookService.getAllCategories());
    }
}
