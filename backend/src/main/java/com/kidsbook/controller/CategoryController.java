package com.kidsbook.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kidsbook.common.Result;
import com.kidsbook.dto.CategoryRequest;
import com.kidsbook.entity.Category;
import com.kidsbook.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {
    private final CategoryService categoryService;

    @GetMapping
    public Result<Page<Category>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        return Result.success(categoryService.listCategories(page, size, keyword, status));
    }

    @GetMapping("/all")
    public Result<List<Category>> listAll() {
        return Result.success(categoryService.listAll());
    }

    @GetMapping("/{id}")
    public Result<Category> detail(@PathVariable Long id) {
        return Result.success(categoryService.getById(id));
    }

    @PostMapping
    public Result<Void> add(@Valid @RequestBody CategoryRequest request) {
        categoryService.addCategory(request);
        return Result.success();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        request.setId(id);
        categoryService.updateCategory(request);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return Result.success();
    }

    @GetMapping("/{id}/book-count")
    public Result<Integer> bookCount(@PathVariable Long id) {
        return Result.success(categoryService.getBookCount(id));
    }
}
