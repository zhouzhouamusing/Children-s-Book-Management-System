package com.kidsbook.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kidsbook.common.Result;
import com.kidsbook.dto.ReaderProfileUpdateRequest;
import com.kidsbook.dto.ReservationRequest;
import com.kidsbook.entity.Book;
import com.kidsbook.entity.BookReservation;
import com.kidsbook.entity.BorrowRecord;
import com.kidsbook.entity.Reader;
import com.kidsbook.mapper.BookMapper;
import com.kidsbook.mapper.BorrowRecordMapper;
import com.kidsbook.mapper.ReaderMapper;
import com.kidsbook.service.BookReservationService;
import com.kidsbook.util.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reader-center")
@RequiredArgsConstructor
public class ReaderCenterController {
    private final ReaderMapper readerMapper;
    private final BorrowRecordMapper borrowRecordMapper;
    private final BookMapper bookMapper;
    private final BookReservationService reservationService;
    private final JwtUtil jwtUtil;

    private Long getCurrentReaderId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String token = (String) auth.getCredentials();
        Long readerId = jwtUtil.getReaderIdFromToken(token);
        if (readerId == null) {
            throw new RuntimeException("无法获取读者信息");
        }
        return readerId;
    }

    @GetMapping("/profile")
    public Result<Reader> getProfile() {
        Long readerId = getCurrentReaderId();
        Reader reader = readerMapper.selectById(readerId);
        if (reader == null) {
            throw new RuntimeException("读者信息不存在");
        }
        return Result.success(reader);
    }

    @PutMapping("/profile")
    public Result<Void> updateProfile(@RequestBody @Valid ReaderProfileUpdateRequest request) {
        Long readerId = getCurrentReaderId();
        Reader reader = readerMapper.selectById(readerId);
        if (reader == null) {
            throw new RuntimeException("读者信息不存在");
        }
        reader.setName(request.getName());
        reader.setAge(request.getAge());
        reader.setGender(request.getGender());
        reader.setParentName(request.getParentName());
        reader.setParentPhone(request.getParentPhone());
        readerMapper.updateById(reader);
        return Result.success(null);
    }

    @GetMapping("/borrow-records")
    public Result<Map<String, Object>> getBorrowRecords(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {
        Long readerId = getCurrentReaderId();
        LambdaQueryWrapper<BorrowRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BorrowRecord::getReaderId, readerId);
        if (status != null && !status.isEmpty() && !"all".equals(status)) {
            wrapper.eq(BorrowRecord::getStatus, status);
        }
        wrapper.orderByDesc(BorrowRecord::getBorrowDate);
        IPage<BorrowRecord> result = borrowRecordMapper.selectPage(new Page<>(page, size), wrapper);

        long totalBorrows = borrowRecordMapper.countByReaderId(readerId);
        LambdaQueryWrapper<BorrowRecord> borrowingWrapper = new LambdaQueryWrapper<>();
        borrowingWrapper.eq(BorrowRecord::getReaderId, readerId).eq(BorrowRecord::getStatus, "borrowing");
        long borrowingCount = borrowRecordMapper.selectCount(borrowingWrapper);
        LambdaQueryWrapper<BorrowRecord> overdueWrapper = new LambdaQueryWrapper<>();
        overdueWrapper.eq(BorrowRecord::getReaderId, readerId).eq(BorrowRecord::getStatus, "overdue");
        long overdueCount = borrowRecordMapper.selectCount(overdueWrapper);

        Map<String, Object> data = new HashMap<>();
        data.put("records", result.getRecords());
        data.put("total", result.getTotal());
        data.put("totalBorrows", totalBorrows);
        data.put("borrowingCount", borrowingCount);
        data.put("overdueCount", overdueCount);
        data.put("returnedCount", totalBorrows - borrowingCount - overdueCount);
        return Result.success(data);
    }

    @GetMapping("/reservations")
    public Result<Map<String, Object>> getReservations(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {
        Long readerId = getCurrentReaderId();
        IPage<BookReservation> result = reservationService.getMyReservations(readerId, page, size, status);
        Map<String, Object> data = new HashMap<>();
        data.put("records", result.getRecords());
        data.put("total", result.getTotal());
        return Result.success(data);
    }

    @PostMapping("/reservations")
    public Result<Void> createReservation(@RequestBody @Valid ReservationRequest request) {
        Long readerId = getCurrentReaderId();
        reservationService.createReservation(readerId, request.getBookId());
        return Result.success(null);
    }

    @PutMapping("/reservations/{id}/cancel")
    public Result<Void> cancelReservation(@PathVariable Long id) {
        Long readerId = getCurrentReaderId();
        reservationService.cancelReservation(readerId, id);
        return Result.success(null);
    }

    @GetMapping("/books")
    public Result<Map<String, Object>> browseBooks(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category) {
        LambdaQueryWrapper<Book> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Book::getStatus, 1);
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(Book::getTitle, keyword)
                    .or().like(Book::getAuthor, keyword));
        }
        if (category != null && !category.isEmpty()) {
            wrapper.eq(Book::getCategory, category);
        }
        wrapper.orderByDesc(Book::getCreateTime);
        IPage<Book> result = bookMapper.selectPage(new Page<>(page, size), wrapper);
        Map<String, Object> data = new HashMap<>();
        data.put("records", result.getRecords());
        data.put("total", result.getTotal());
        return Result.success(data);
    }

    @GetMapping("/statistics")
    public Result<Map<String, Object>> getStatistics() {
        Long readerId = getCurrentReaderId();
        long totalBorrows = borrowRecordMapper.countByReaderId(readerId);

        LambdaQueryWrapper<BorrowRecord> thisMonthWrapper = new LambdaQueryWrapper<>();
        LocalDate firstDayOfMonth = LocalDate.now().withDayOfMonth(1);
        thisMonthWrapper.eq(BorrowRecord::getReaderId, readerId)
                .ge(BorrowRecord::getBorrowDate, firstDayOfMonth);
        long thisMonthBorrows = borrowRecordMapper.selectCount(thisMonthWrapper);

        LambdaQueryWrapper<BorrowRecord> allWrapper = new LambdaQueryWrapper<>();
        allWrapper.eq(BorrowRecord::getReaderId, readerId);
        List<BorrowRecord> allRecords = borrowRecordMapper.selectList(allWrapper);

        long readingDays = allRecords.stream()
                .map(BorrowRecord::getBorrowDate)
                .distinct()
                .count();

        Map<String, Long> categoryMap = allRecords.stream()
                .filter(r -> r.getBookTitle() != null)
                .collect(Collectors.groupingBy(
                        r -> {
                            Book book = bookMapper.selectById(r.getBookId());
                            return book != null && book.getCategory() != null ? book.getCategory() : "其他";
                        },
                        Collectors.counting()
                ));

        List<Map<String, Object>> categoryDistribution = categoryMap.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(6)
                .map(entry -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("name", entry.getKey());
                    item.put("count", entry.getValue());
                    return item;
                })
                .collect(Collectors.toList());

        long totalPoints = totalBorrows * 10;

        Map<String, Object> data = new HashMap<>();
        data.put("totalBorrows", totalBorrows);
        data.put("thisMonthBorrows", thisMonthBorrows);
        data.put("totalBooks", totalBorrows);
        data.put("readingDays", readingDays);
        data.put("totalPoints", totalPoints);
        data.put("categoryDistribution", categoryDistribution);
        return Result.success(data);
    }

    @GetMapping("/points")
    public Result<Map<String, Object>> getPoints() {
        Long readerId = getCurrentReaderId();
        long totalBorrows = borrowRecordMapper.countByReaderId(readerId);
        long totalPoints = totalBorrows * 10;

        Map<String, Object> data = new HashMap<>();
        data.put("totalPoints", totalPoints);
        data.put("level", totalPoints >= 500 ? "阅读大师" : totalPoints >= 200 ? "阅读达人" : totalPoints >= 50 ? "小书虫" : "新手读者");
        return Result.success(data);
    }
}
