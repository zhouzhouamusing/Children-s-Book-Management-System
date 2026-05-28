package com.kidsbook.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kidsbook.entity.Book;
import com.kidsbook.entity.BookReservation;
import com.kidsbook.mapper.BookMapper;
import com.kidsbook.mapper.BookReservationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookReservationService {
    private final BookReservationMapper reservationMapper;
    private final BookMapper bookMapper;

    public void createReservation(Long readerId, Long bookId) {
        Book book = bookMapper.selectById(bookId);
        if (book == null) {
            throw new RuntimeException("图书不存在");
        }
        if (book.getStatus() == 0) {
            throw new RuntimeException("该图书已下架");
        }

        int activeReservations = reservationMapper.countActiveByBookId(bookId);
        if (book.getStock() <= activeReservations) {
            throw new RuntimeException("该图书暂无可用库存");
        }

        LambdaQueryWrapper<BookReservation> duplicateCheck = new LambdaQueryWrapper<>();
        duplicateCheck.eq(BookReservation::getReaderId, readerId)
                .eq(BookReservation::getBookId, bookId)
                .eq(BookReservation::getStatus, "pending");
        if (reservationMapper.selectCount(duplicateCheck) > 0) {
            throw new RuntimeException("您已预约过该图书，请勿重复预约");
        }

        int readerActiveCount = reservationMapper.countActiveByReaderId(readerId);
        if (readerActiveCount >= 5) {
            throw new RuntimeException("您的待取书预约已达上限（5本），请先取书或取消部分预约");
        }

        BookReservation reservation = new BookReservation();
        reservation.setReaderId(readerId);
        reservation.setBookId(bookId);
        reservation.setBookTitle(book.getTitle());
        reservation.setReserveDate(LocalDateTime.now());
        reservation.setExpireDate(LocalDateTime.now().plusDays(3));
        reservation.setStatus("pending");
        reservationMapper.insert(reservation);
        log.info("读者{}预约图书{}成功", readerId, book.getTitle());
    }

    public void cancelReservation(Long readerId, Long reservationId) {
        BookReservation reservation = reservationMapper.selectById(reservationId);
        if (reservation == null) {
            throw new RuntimeException("预约记录不存在");
        }
        if (!reservation.getReaderId().equals(readerId)) {
            throw new RuntimeException("无权操作该预约");
        }
        if (!"pending".equals(reservation.getStatus())) {
            throw new RuntimeException("只能取消待取书状态的预约");
        }
        reservation.setStatus("cancelled");
        reservationMapper.updateById(reservation);
        log.info("读者{}取消预约{}", readerId, reservationId);
    }

    public IPage<BookReservation> getMyReservations(Long readerId, int page, int size, String status) {
        LambdaQueryWrapper<BookReservation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BookReservation::getReaderId, readerId);
        if (status != null && !status.isEmpty() && !"all".equals(status)) {
            wrapper.eq(BookReservation::getStatus, status);
        }
        wrapper.orderByDesc(BookReservation::getCreateTime);

        expireOldReservations(readerId);

        return reservationMapper.selectPage(new Page<>(page, size), wrapper);
    }

    private void expireOldReservations(Long readerId) {
        LambdaQueryWrapper<BookReservation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BookReservation::getReaderId, readerId)
                .eq(BookReservation::getStatus, "pending")
                .lt(BookReservation::getExpireDate, LocalDateTime.now());
        var expired = reservationMapper.selectList(wrapper);
        for (BookReservation r : expired) {
            r.setStatus("expired");
            reservationMapper.updateById(r);
        }
    }
}
