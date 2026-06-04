package com.kidsbook.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kidsbook.common.PageResult;
import com.kidsbook.common.Permission;
import com.kidsbook.common.RequirePermission;
import com.kidsbook.common.Result;
import com.kidsbook.entity.AuditLog;
import com.kidsbook.entity.Book;
import com.kidsbook.entity.BorrowRecord;
import com.kidsbook.entity.Reader;
import com.kidsbook.mapper.BookMapper;
import com.kidsbook.mapper.BorrowRecordMapper;
import com.kidsbook.mapper.ReaderMapper;
import com.kidsbook.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {
    private final BorrowRecordMapper borrowRecordMapper;
    private final ReaderMapper readerMapper;
    private final BookMapper bookMapper;
    private final AuditLogService auditLogService;

    @GetMapping("/borrow-trends")
    @RequirePermission(Permission.DASHBOARD_READ)
    public Result<List<Map<String, Object>>> getBorrowTrends(
            @RequestParam(defaultValue = "12") int months) {
        List<Map<String, Object>> trends = new ArrayList<>();
        YearMonth current = YearMonth.now();

        for (int i = months - 1; i >= 0; i--) {
            YearMonth month = current.minusMonths(i);
            LocalDate start = month.atDay(1);
            LocalDate end = month.atEndOfMonth();

            LambdaQueryWrapper<BorrowRecord> borrowWrapper = new LambdaQueryWrapper<>();
            borrowWrapper.ge(BorrowRecord::getBorrowDate, start)
                    .le(BorrowRecord::getBorrowDate, end);
            long borrowCount = borrowRecordMapper.selectCount(borrowWrapper);

            LambdaQueryWrapper<BorrowRecord> returnWrapper = new LambdaQueryWrapper<>();
            returnWrapper.ge(BorrowRecord::getReturnDate, start)
                    .le(BorrowRecord::getReturnDate, end);
            long returnCount = borrowRecordMapper.selectCount(returnWrapper);

            Map<String, Object> item = new HashMap<>();
            item.put("month", month.format(DateTimeFormatter.ofPattern("yyyy-MM")));
            item.put("borrowCount", borrowCount);
            item.put("returnCount", returnCount);
            trends.add(item);
        }
        return Result.success(trends);
    }

    @GetMapping("/overdue-analytics")
    @RequirePermission(Permission.DASHBOARD_READ)
    public Result<List<Map<String, Object>>> getOverdueAnalytics(
            @RequestParam(defaultValue = "12") int months) {
        List<Map<String, Object>> analytics = new ArrayList<>();
        YearMonth current = YearMonth.now();

        for (int i = months - 1; i >= 0; i--) {
            YearMonth month = current.minusMonths(i);
            LocalDate start = month.atDay(1);
            LocalDate end = month.atEndOfMonth();

            LambdaQueryWrapper<BorrowRecord> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(BorrowRecord::getStatus, "overdue")
                    .ge(BorrowRecord::getDueDate, start)
                    .le(BorrowRecord::getDueDate, end);
            long overdueCount = borrowRecordMapper.selectCount(wrapper);

            Map<String, Object> item = new HashMap<>();
            item.put("month", month.format(DateTimeFormatter.ofPattern("yyyy-MM")));
            item.put("overdueCount", overdueCount);
            analytics.add(item);
        }
        return Result.success(analytics);
    }

    @GetMapping("/top-readers")
    @RequirePermission(Permission.DASHBOARD_READ)
    public Result<List<Map<String, Object>>> getTopReaders(
            @RequestParam(defaultValue = "10") int limit) {
        LambdaQueryWrapper<Reader> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Reader::getStatus, "normal")
                .orderByDesc(Reader::getBorrowCount)
                .last("LIMIT " + Math.min(limit, 50));
        List<Reader> readers = readerMapper.selectList(wrapper);

        List<Map<String, Object>> result = readers.stream().map(reader -> {
            Map<String, Object> item = new HashMap<>();
            item.put("name", reader.getName());
            item.put("borrowCount", reader.getBorrowCount());
            item.put("points", reader.getPoints());
            item.put("level", reader.getLevel());
            return item;
        }).collect(Collectors.toList());

        return Result.success(result);
    }

    @GetMapping("/category-distribution")
    @RequirePermission(Permission.DASHBOARD_READ)
    public Result<List<Map<String, Object>>> getCategoryDistribution() {
        List<Map<String, Object>> distribution = bookMapper.countByCategory();
        if (distribution == null) {
            distribution = new ArrayList<>();
        }
        return Result.success(distribution);
    }

    @GetMapping("/audit-logs")
    @RequirePermission(Permission.AUDIT_LOG_READ)
    public Result<PageResult<AuditLog>> getAuditLogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String operator) {
        IPage<AuditLog> result = auditLogService.listLogs(page, size, action, operator);
        return Result.success(PageResult.of(result));
    }

    @GetMapping("/reader-growth")
    @RequirePermission(Permission.DASHBOARD_READ)
    public Result<List<Map<String, Object>>> getReaderGrowth(
            @RequestParam(defaultValue = "12") int months) {
        List<Map<String, Object>> growth = new ArrayList<>();
        YearMonth current = YearMonth.now();

        for (int i = months - 1; i >= 0; i--) {
            YearMonth month = current.minusMonths(i);
            LocalDate start = month.atDay(1);
            LocalDate end = month.atEndOfMonth();

            LambdaQueryWrapper<Reader> wrapper = new LambdaQueryWrapper<>();
            wrapper.ge(Reader::getCreateTime, start.atStartOfDay())
                    .le(Reader::getCreateTime, end.atTime(23, 59, 59));
            long count = readerMapper.selectCount(wrapper);

            Map<String, Object> item = new HashMap<>();
            item.put("month", month.format(DateTimeFormatter.ofPattern("yyyy-MM")));
            item.put("newReaders", count);
            growth.add(item);
        }
        return Result.success(growth);
    }

    @GetMapping("/book-utilization")
    @RequirePermission(Permission.DASHBOARD_READ)
    public Result<List<Map<String, Object>>> getBookUtilization(
            @RequestParam(defaultValue = "10") int limit) {
        LambdaQueryWrapper<Book> bookWrapper = new LambdaQueryWrapper<>();
        bookWrapper.eq(Book::getStatus, 1)
                .gt(Book::getStock, 0)
                .orderByDesc(Book::getReviewCount)
                .last("LIMIT " + Math.min(limit, 50));
        List<Book> books = bookMapper.selectList(bookWrapper);

        List<Map<String, Object>> result = books.stream().map(book -> {
            LambdaQueryWrapper<BorrowRecord> brWrapper = new LambdaQueryWrapper<>();
            brWrapper.eq(BorrowRecord::getBookId, book.getId());
            long totalBorrows = borrowRecordMapper.selectCount(brWrapper);

            LambdaQueryWrapper<BorrowRecord> activeWrapper = new LambdaQueryWrapper<>();
            activeWrapper.eq(BorrowRecord::getBookId, book.getId())
                    .in(BorrowRecord::getStatus, "borrowing", "overdue");
            long activeBorrows = borrowRecordMapper.selectCount(activeWrapper);

            Map<String, Object> item = new HashMap<>();
            item.put("title", book.getTitle());
            item.put("stock", book.getStock());
            item.put("totalBorrows", totalBorrows);
            item.put("activeBorrows", activeBorrows);
            item.put("avgRating", book.getAvgRating());
            return item;
        }).collect(Collectors.toList());

        return Result.success(result);
    }

    @GetMapping("/reading-stats")
    @RequirePermission(Permission.DASHBOARD_READ)
    public Result<Map<String, Object>> getReadingStats() {
        Map<String, Object> stats = new HashMap<>();
        long totalReaders = readerMapper.selectCount(null);
        long totalBorrows = borrowRecordMapper.selectCount(null);

        LambdaQueryWrapper<Reader> activeWrapper = new LambdaQueryWrapper<>();
        activeWrapper.gt(Reader::getTotalReadingDays, 0);
        long activeReaders = readerMapper.selectCount(activeWrapper);

        stats.put("totalReaders", totalReaders);
        stats.put("activeReaders", activeReaders);
        stats.put("totalBorrows", totalBorrows);
        stats.put("avgBorrowsPerReader", totalReaders > 0 ? totalBorrows / totalReaders : 0);
        return Result.success(stats);
    }
}
