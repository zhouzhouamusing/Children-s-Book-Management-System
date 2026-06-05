package com.kidsbook.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.kidsbook.common.PageResult;
import com.kidsbook.common.Permission;
import com.kidsbook.common.RequirePermission;
import com.kidsbook.common.Result;
import com.kidsbook.dto.BorrowRequest;
import com.kidsbook.entity.BookReservation;
import com.kidsbook.entity.BorrowRecord;
import com.kidsbook.service.BookReservationService;
import com.kidsbook.service.BorrowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/borrows")
@RequiredArgsConstructor
public class BorrowController {
    private final BorrowService borrowService;
    private final BookReservationService reservationService;

    @GetMapping
    @RequirePermission(Permission.BORROW_READ)
    public Result<PageResult<BorrowRecord>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        IPage<BorrowRecord> result = borrowService.list(page, size, keyword, status);
        return Result.success(PageResult.of(result));
    }

    @PostMapping
    @RequirePermission(Permission.BORROW_CREATE)
    public Result<Void> borrow(@RequestBody @Valid BorrowRequest request) {
        borrowService.borrowBook(request);
        return Result.success(null);
    }

    @PutMapping("/{id}/return")
    @RequirePermission(Permission.BORROW_UPDATE)
    public Result<Void> returnBook(@PathVariable Long id) {
        borrowService.returnBook(id);
        return Result.success(null);
    }

    @PutMapping("/{id}/renew")
    @RequirePermission(Permission.BORROW_UPDATE)
    public Result<Void> renew(@PathVariable Long id, @RequestParam(defaultValue = "14") Integer days) {
        borrowService.renewBook(id, days);
        return Result.success(null);
    }

    @GetMapping("/statistics")
    @RequirePermission(Permission.BORROW_READ)
    public Result<Map<String, Object>> statistics() {
        return Result.success(borrowService.getStatistics());
    }

    @GetMapping("/reservations")
    @RequirePermission(Permission.RESERVATION_READ)
    public Result<PageResult<BookReservation>> listReservations(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {
        IPage<BookReservation> result = reservationService.listAllReservations(page, size, status);
        return Result.success(PageResult.of(result));
    }

    @PutMapping("/reservations/{id}/fulfill")
    @RequirePermission(Permission.RESERVATION_UPDATE)
    public Result<Void> fulfillReservation(@PathVariable Long id) {
        reservationService.adminFulfillReservation(id);
        return Result.success(null);
    }

    @PutMapping("/reservations/{id}/ready")
    @RequirePermission(Permission.RESERVATION_UPDATE)
    public Result<Void> markReservationReady(@PathVariable Long id) {
        reservationService.markReadyForPickup(id);
        return Result.success(null);
    }

    @GetMapping("/export")
    @RequirePermission(Permission.BORROW_EXPORT)
    public Result<PageResult<BorrowRecord>> exportBorrows(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        IPage<BorrowRecord> result = borrowService.list(1, 10000, keyword, status);
        return Result.success(PageResult.of(result));
    }
}
