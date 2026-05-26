package com.kidsbook.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kidsbook.dto.BookRequest;
import com.kidsbook.entity.Book;
import com.kidsbook.mapper.BookMapper;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.bean.BeanUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class BookService extends ServiceImpl<BookMapper, Book> {
    private final BookMapper bookMapper;

    public Page<Book> listBooks(int page, int size, String keyword, String category) {
        Page<Book> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Book> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.and(w -> w.like(Book::getTitle, keyword)
                    .or().like(Book::getAuthor, keyword)
                    .or().like(Book::getIsbn, keyword));
        }
        if (StrUtil.isNotBlank(category)) {
            wrapper.eq(Book::getCategory, category);
        }
        wrapper.orderByDesc(Book::getCreateTime);
        return bookMapper.selectPage(pageParam, wrapper);
    }

    public void addBook(BookRequest request) {
        Book book = BeanUtil.copyProperties(request, Book.class);
        bookMapper.insert(book);
    }

    public void updateBook(BookRequest request) {
        Book book = BeanUtil.copyProperties(request, Book.class);
        bookMapper.updateById(book);
    }

    public void deleteBook(Long id) {
        bookMapper.deleteById(id);
    }

    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalBooks", bookMapper.totalCount());
        stats.put("totalStock", bookMapper.totalStock());
        stats.put("categoryStats", bookMapper.countByCategory());
        return stats;
    }

    public List<String> getAllCategories() {
        List<Map<String, Object>> list = bookMapper.countByCategory();
        return list.stream().map(m -> (String) m.get("category")).toList();
    }
}
