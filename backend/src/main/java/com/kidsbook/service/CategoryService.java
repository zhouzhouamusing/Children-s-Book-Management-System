package com.kidsbook.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kidsbook.dto.CategoryRequest;
import com.kidsbook.entity.Category;
import com.kidsbook.mapper.CategoryMapper;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.bean.BeanUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService extends ServiceImpl<CategoryMapper, Category> {
    private final CategoryMapper categoryMapper;

    public Page<Category> listCategories(int page, int size, String keyword, Integer status) {
        Page<Category> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.like(Category::getName, keyword)
                    .or().like(Category::getDescription, keyword);
        }
        if (status != null) {
            wrapper.eq(Category::getStatus, status);
        }
        wrapper.orderByDesc(Category::getSortOrder).orderByAsc(Category::getCreateTime);
        Page<Category> result = categoryMapper.selectPage(pageParam, wrapper);
        result.getRecords().forEach(c ->
            c.setBookCount(categoryMapper.countBooksByCategory(c.getName()))
        );
        return result;
    }

    public List<Category> listAll() {
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Category::getStatus, 1)
                .orderByDesc(Category::getSortOrder);
        List<Category> list = categoryMapper.selectList(wrapper);
        list.forEach(c -> c.setBookCount(categoryMapper.countBooksByCategory(c.getName())));
        return list;
    }

    public void addCategory(CategoryRequest request) {
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Category::getName, request.getName());
        if (categoryMapper.selectCount(wrapper) > 0) {
            throw new RuntimeException("分类名称已存在");
        }
        Category category = BeanUtil.copyProperties(request, Category.class);
        if (category.getSortOrder() == null) {
            category.setSortOrder(0);
        }
        if (category.getStatus() == null) {
            category.setStatus(1);
        }
        categoryMapper.insert(category);
    }

    public void updateCategory(CategoryRequest request) {
        Category existing = categoryMapper.selectById(request.getId());
        if (existing == null) {
            throw new RuntimeException("分类不存在");
        }
        if (!existing.getName().equals(request.getName())) {
            LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Category::getName, request.getName());
            if (categoryMapper.selectCount(wrapper) > 0) {
                throw new RuntimeException("分类名称已存在");
            }
        }
        Category category = BeanUtil.copyProperties(request, Category.class);
        categoryMapper.updateById(category);
    }

    public boolean deleteCategory(Long id) {
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            throw new RuntimeException("分类不存在");
        }
        Integer bookCount = categoryMapper.countBooksByCategory(category.getName());
        if (bookCount > 0) {
            throw new RuntimeException("该分类下有 " + bookCount + " 本图书，无法删除。请先移除关联图书。");
        }
        categoryMapper.deleteById(id);
        return true;
    }

    public Integer getBookCount(Long id) {
        Category category = categoryMapper.selectById(id);
        if (category == null) return 0;
        return categoryMapper.countBooksByCategory(category.getName());
    }
}
