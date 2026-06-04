package com.kidsbook.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kidsbook.common.BusinessException;
import com.kidsbook.common.PageResult;
import com.kidsbook.common.Permission;
import com.kidsbook.common.RequirePermission;
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
import com.kidsbook.service.AppealService;
import com.kidsbook.service.AuditLogService;
import com.kidsbook.service.BookReservationService;
import com.kidsbook.service.BookReviewService;
import com.kidsbook.service.BorrowService;
import com.kidsbook.service.ReaderPointsService;
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
    private final ReaderPointsService readerPointsService;
    private final BookReviewService bookReviewService;
    private final AuditLogService auditLogService;
    private final BorrowService borrowService;
    private final AppealService appealService;
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

    private boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    @GetMapping("/profile")
    @RequirePermission(Permission.READER_PROFILE_READ)
    public Result<Reader> getProfile() {
        Long readerId = getCurrentReaderId();
        if (readerId == null) {
            if (isAdmin()) {
                Reader adminReader = new Reader();
                adminReader.setName("管理员");
                adminReader.setStatus("normal");
                return Result.success(adminReader);
            }
            throw new BusinessException(401, "无法获取读者信息");
        }
        Reader reader = readerMapper.selectById(readerId);
        if (reader == null) {
            throw new BusinessException(404, "读者信息不存在");
        }
        return Result.success(reader);
    }

    @PutMapping("/profile")
    @RequirePermission(Permission.READER_PROFILE_UPDATE)
    public Result<Void> updateProfile(@RequestBody @Valid ReaderProfileUpdateRequest request) {
        Long readerId = getCurrentReaderId();
        if (readerId == null) {
            throw new BusinessException(403, "管理员身份无法修改读者信息");
        }
        Reader reader = readerMapper.selectById(readerId);
        if (reader == null) {
            throw new BusinessException(404, "读者信息不存在");
        }
        String oldName = reader.getName();
        reader.setName(request.getName());
        reader.setAge(request.getAge());
        reader.setGender(request.getGender());
        reader.setParentName(request.getParentName());
        reader.setParentPhone(request.getParentPhone());
        readerMapper.updateById(reader);

        if (request.getName() != null && !request.getName().equals(oldName)) {
            bookReviewService.updateReaderName(readerId, request.getName());
        }
        return Result.success(null);
    }

    @GetMapping("/borrow-records")
    @RequirePermission(Permission.READER_BORROW_READ)
    public Result<PageResult<BorrowRecord>> getBorrowRecords(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {
        Long readerId = getCurrentReaderId();
        if (readerId == null) {
            return Result.success(PageResult.<BorrowRecord>empty(page, size)
                .withExtra("totalBorrows", 0)
                .withExtra("borrowingCount", 0)
                .withExtra("overdueCount", 0)
                .withExtra("returnedCount", 0));
        }

        borrowService.markOverdueForReader(readerId);

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

        return Result.success(PageResult.of(result)
            .withExtra("totalBorrows", totalBorrows)
            .withExtra("borrowingCount", borrowingCount)
            .withExtra("overdueCount", overdueCount)
            .withExtra("returnedCount", totalBorrows - borrowingCount - overdueCount));
    }

    @GetMapping("/reservations")
    @RequirePermission(Permission.READER_RESERVATION_READ)
    public Result<PageResult<BookReservation>> getReservations(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {
        Long readerId = getCurrentReaderId();
        if (readerId == null) {
            return Result.success(PageResult.empty(page, size));
        }
        IPage<BookReservation> result = reservationService.getMyReservations(readerId, page, size, status);
        return Result.success(PageResult.of(result));
    }

    @PostMapping("/reservations")
    @RequirePermission(Permission.READER_RESERVATION_CREATE)
    public Result<Void> createReservation(@RequestBody @Valid ReservationRequest request) {
        Long readerId = getCurrentReaderId();
        if (readerId == null) {
            throw new BusinessException(403, "管理员身份无法进行预约操作");
        }
        reservationService.createReservation(readerId, request.getBookId());
        return Result.success(null);
    }

    @PutMapping("/reservations/{id}/cancel")
    @RequirePermission(Permission.READER_RESERVATION_CANCEL)
    public Result<Void> cancelReservation(@PathVariable Long id) {
        Long readerId = getCurrentReaderId();
        if (readerId == null) {
            throw new BusinessException(403, "管理员身份无法进行预约操作");
        }
        reservationService.cancelReservation(readerId, id);
        return Result.success(null);
    }

    @GetMapping("/books")
    @RequirePermission(Permission.READER_BOOK_BROWSE)
    public Result<PageResult<Book>> browseBooks(
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
        return Result.success(PageResult.of(result));
    }

    @GetMapping("/statistics")
    public Result<Map<String, Object>> getStatistics() {
        Long readerId = getCurrentReaderId();
        Map<String, Object> data = new HashMap<>();
        if (readerId == null) {
            data.put("totalBorrows", 0);
            data.put("thisMonthBorrows", 0);
            data.put("totalBooks", 0);
            data.put("readingDays", 0);
            data.put("totalPoints", 0);
            data.put("level", "");
            data.put("categoryDistribution", List.of());
            return Result.success(data);
        }
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

        List<Long> bookIds = allRecords.stream()
                .map(BorrowRecord::getBookId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, Book> bookMap = new HashMap<>();
        if (!bookIds.isEmpty()) {
            bookMapper.selectBatchIds(bookIds).forEach(b -> bookMap.put(b.getId(), b));
        }

        Map<String, Long> categoryMap = allRecords.stream()
                .filter(r -> r.getBookId() != null)
                .collect(Collectors.groupingBy(
                        r -> {
                            Book book = bookMap.get(r.getBookId());
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

        Map<String, Object> pointsStats;
        try {
            pointsStats = readerPointsService.getStatistics(readerId);
        } catch (Exception e) {
            pointsStats = new HashMap<>();
            pointsStats.put("totalPoints", 0);
            pointsStats.put("level", "");
            pointsStats.put("monthlyStats", List.of());
        }

        data.put("totalBorrows", totalBorrows);
        data.put("thisMonthBorrows", thisMonthBorrows);
        data.put("totalBooks", totalBorrows);
        data.put("readingDays", readingDays);
        data.put("totalPoints", pointsStats.get("totalPoints"));
        data.put("level", pointsStats.get("level"));
        data.put("monthlyStats", pointsStats.get("monthlyStats"));
        data.put("categoryDistribution", categoryDistribution);
        return Result.success(data);
    }

    @GetMapping("/points")
    public Result<Map<String, Object>> getPoints() {
        Long readerId = getCurrentReaderId();
        if (readerId == null) {
            Map<String, Object> data = new HashMap<>();
            data.put("totalPoints", 0);
            data.put("logs", List.of());
            return Result.success(data);
        }
        Map<String, Object> data = readerPointsService.getPointsDetail(readerId);
        return Result.success(data);
    }

    @GetMapping("/categories")
    @RequirePermission(Permission.READER_CATEGORY_BROWSE)
    public Result<List<String>> getCategories() {
        LambdaQueryWrapper<Book> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Book::getStatus, 1)
                .isNotNull(Book::getCategory)
                .select(Book::getCategory)
                .groupBy(Book::getCategory);
        List<Book> books = bookMapper.selectList(wrapper);
        List<String> categories = books.stream()
                .map(Book::getCategory)
                .filter(c -> c != null && !c.isEmpty())
                .distinct()
                .sorted()
                .toList();
        return Result.success(categories);
    }

    @PostMapping("/appeal-suspension")
    @RequirePermission(Permission.READER_APPEAL_CREATE)
    public Result<Void> appealSuspension(@RequestBody Map<String, String> body) {
        Long readerId = getCurrentReaderId();
        if (readerId == null) {
            throw new BusinessException(401, "无法获取读者信息，请重新登录");
        }
        String reason = body != null ? body.getOrDefault("reason", "") : "";
        appealService.submitAppeal(readerId, "suspension", reason, null);
        return Result.success(null);
    }

    @PostMapping("/appeals")
    @RequirePermission(Permission.READER_APPEAL_CREATE)
    public Result<Object> submitAppeal(@RequestBody Map<String, String> body) {
        Long readerId = getCurrentReaderId();
        if (readerId == null) {
            throw new BusinessException(401, "无法获取读者信息，请重新登录");
        }
        String type = body != null ? body.getOrDefault("type", "suspension") : "suspension";
        String reason = body != null ? body.getOrDefault("reason", "") : "";
        String evidence = body != null ? body.getOrDefault("evidence", null) : null;
        if (reason.isEmpty()) {
            throw new BusinessException(400, "申诉原因不能为空");
        }
        var appeal = appealService.submitAppeal(readerId, type, reason, evidence);
        return Result.success(appeal);
    }

    @GetMapping("/appeals")
    @RequirePermission(Permission.READER_APPEAL_VIEW)
    public Result<Object> getMyAppeals(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long readerId = getCurrentReaderId();
        if (readerId == null) {
            throw new BusinessException(401, "无法获取读者信息，请重新登录");
        }
        var result = appealService.getMyAppeals(readerId, page, size);
        return Result.success(PageResult.of(result));
    }

    @GetMapping("/appeals/{id}")
    @RequirePermission(Permission.READER_APPEAL_VIEW)
    public Result<Object> getAppealDetail(@PathVariable Long id) {
        Long readerId = getCurrentReaderId();
        if (readerId == null) {
            throw new BusinessException(401, "无法获取读者信息，请重新登录");
        }
        var appeal = appealService.getAppealById(id);
        if (!appeal.getReaderId().equals(readerId)) {
            throw new BusinessException(403, "无权查看他人的申诉记录");
        }
        return Result.success(appeal);
    }
}
