package com.kidsbook.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kidsbook.common.PageResult;
import com.kidsbook.common.Permission;
import com.kidsbook.common.RequirePermission;
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
    @RequirePermission(Permission.CATEGORY_READ)
    public Result<PageResult<Category>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        return Result.success(PageResult.of(categoryService.listCategories(page, size, keyword, status)));
    }

    @GetMapping("/all")
    @RequirePermission(Permission.CATEGORY_READ)
    public Result<List<Category>> listAll() {
        return Result.success(categoryService.listAll());
    }

    @GetMapping("/{id}")
    @RequirePermission(Permission.CATEGORY_READ)
    public Result<Category> detail(@PathVariable Long id) {
        return Result.success(categoryService.getById(id));
    }

    @PostMapping
    @RequirePermission(Permission.CATEGORY_CREATE)
    public Result<Void> add(@Valid @RequestBody CategoryRequest request) {
        categoryService.addCategory(request);
        return Result.success();
    }

    @PutMapping("/{id}")
    @RequirePermission(Permission.CATEGORY_UPDATE)
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        request.setId(id);
        categoryService.updateCategory(request);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @RequirePermission(Permission.CATEGORY_DELETE)
    public Result<Void> delete(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return Result.success();
    }

    @GetMapping("/{id}/book-count")
    @RequirePermission(Permission.CATEGORY_READ)
    public Result<Integer> bookCount(@PathVariable Long id) {
        return Result.success(categoryService.getBookCount(id));
    }
}
