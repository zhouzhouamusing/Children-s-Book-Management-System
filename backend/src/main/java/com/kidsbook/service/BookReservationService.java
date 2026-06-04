package com.kidsbook.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kidsbook.common.BusinessException;
import com.kidsbook.entity.Book;
import com.kidsbook.entity.BookReservation;
import com.kidsbook.entity.Reader;
import com.kidsbook.entity.ReaderAccount;
import com.kidsbook.mapper.BookMapper;
import com.kidsbook.mapper.BookReservationMapper;
import com.kidsbook.mapper.ReaderAccountMapper;
import com.kidsbook.mapper.ReaderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookReservationService extends ServiceImpl<BookReservationMapper, BookReservation> {
    private final BookReservationMapper reservationMapper;
    private final BookMapper bookMapper;
    private final ReaderMapper readerMapper;
    private final ReaderAccountMapper readerAccountMapper;
    private final EmailService emailService;

    @Transactional
    public void createReservation(Long readerId, Long bookId) {
        Reader reader = readerMapper.selectById(readerId);
        if (reader == null) {
            throw new BusinessException(404, "读者不存在");
        }
        if ("suspended".equals(reader.getStatus())) {
            throw new BusinessException(400, "您的借阅权限已暂停，无法预约图书");
        }

        Book book = bookMapper.selectById(bookId);
        if (book == null) {
            throw new BusinessException(404, "图书不存在");
        }
        if (book.getStatus() == 0) {
            throw new BusinessException(400, "该图书已下架");
        }

        int activeReservations = reservationMapper.countActiveByBookId(bookId);
        if (book.getStock() <= activeReservations) {
            throw new BusinessException(400, "该图书暂无可用库存");
        }

        LambdaQueryWrapper<BookReservation> duplicateCheck = new LambdaQueryWrapper<>();
        duplicateCheck.eq(BookReservation::getReaderId, readerId)
                .eq(BookReservation::getBookId, bookId)
                .eq(BookReservation::getStatus, "pending");
        if (reservationMapper.selectCount(duplicateCheck) > 0) {
            throw new BusinessException(400, "您已预约过该图书，请勿重复预约");
        }

        int readerActiveCount = reservationMapper.countActiveByReaderId(readerId);
        if (readerActiveCount >= 5) {
            throw new BusinessException(400, "您的待取书预约已达上限（5本），请先取书或取消部分预约");
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

    @Transactional
    public void cancelReservation(Long readerId, Long reservationId) {
        BookReservation reservation = reservationMapper.selectById(reservationId);
        if (reservation == null) {
            throw new BusinessException(404, "预约记录不存在");
        }
        if (!reservation.getReaderId().equals(readerId)) {
            throw new BusinessException(403, "无权操作该预约");
        }
        if (!"pending".equals(reservation.getStatus()) && !"ready_for_pickup".equals(reservation.getStatus())) {
            throw new BusinessException(400, "只能取消待取书或待领取状态的预约");
        }
        reservation.setStatus("cancelled");
        reservationMapper.updateById(reservation);
        log.info("读者{}取消预约{}", readerId, reservationId);
    }

    @Transactional
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

    @Transactional
    public void fulfillReservation(Long readerId, Long bookId) {
        LambdaQueryWrapper<BookReservation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BookReservation::getReaderId, readerId)
                .eq(BookReservation::getBookId, bookId)
                .eq(BookReservation::getStatus, "pending");
        BookReservation reservation = reservationMapper.selectOne(wrapper);
        if (reservation != null) {
            reservation.setStatus("fulfilled");
            reservationMapper.updateById(reservation);
            log.info("预约已履行: readerId={}, bookId={}", readerId, bookId);
        }
    }

    public void notifyNextReservation(Long bookId, String bookTitle) {
        try {
            LambdaQueryWrapper<BookReservation> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(BookReservation::getBookId, bookId)
                    .eq(BookReservation::getStatus, "pending")
                    .orderByAsc(BookReservation::getReserveDate)
                    .last("LIMIT 1");
            BookReservation next = reservationMapper.selectOne(wrapper);
            if (next == null) return;

            Reader reader = readerMapper.selectById(next.getReaderId());
            if (reader == null) return;

            ReaderAccount account = readerAccountMapper.selectOne(
                new LambdaQueryWrapper<ReaderAccount>().eq(ReaderAccount::getReaderId, reader.getId()));
            if (account != null && account.getEmail() != null && !account.getEmail().isEmpty()) {
                emailService.sendReservationReady(account.getEmail(), reader.getName(), bookTitle);
            }
        } catch (Exception e) {
            log.warn("预约通知发送失败: {}", e.getMessage());
        }
    }

    public IPage<BookReservation> listAllReservations(int page, int size, String status) {
        LambdaQueryWrapper<BookReservation> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isEmpty() && !"all".equals(status)) {
            wrapper.eq(BookReservation::getStatus, status);
        }
        wrapper.orderByDesc(BookReservation::getCreateTime);
        return reservationMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Transactional
    public void markReadyForPickup(Long reservationId) {
        BookReservation reservation = reservationMapper.selectById(reservationId);
        if (reservation == null) {
            throw new BusinessException(404, "预约记录不存在");
        }
        if (!"pending".equals(reservation.getStatus())) {
            throw new BusinessException(400, "只能将待处理的预约标记为待领取");
        }
        reservation.setStatus("ready_for_pickup");
        reservationMapper.updateById(reservation);
        log.info("预约{}已标记为待领取", reservationId);

        try {
            Reader reader = readerMapper.selectById(reservation.getReaderId());
            if (reader != null) {
                ReaderAccount account = readerAccountMapper.selectOne(
                    new LambdaQueryWrapper<ReaderAccount>().eq(ReaderAccount::getReaderId, reader.getId()));
                if (account != null && account.getEmail() != null && !account.getEmail().isEmpty()) {
                    emailService.sendReservationReady(account.getEmail(), reader.getName(), reservation.getBookTitle());
                }
            }
        } catch (Exception e) {
            log.warn("待领取通知发送失败: {}", e.getMessage());
        }
    }

    @Transactional
    public void adminFulfillReservation(Long reservationId) {
        BookReservation reservation = reservationMapper.selectById(reservationId);
        if (reservation == null) {
            throw new BusinessException(404, "预约记录不存在");
        }
        if (!"pending".equals(reservation.getStatus()) && !"ready_for_pickup".equals(reservation.getStatus())) {
            throw new BusinessException(400, "只能完成待处理或待领取状态的预约");
        }
        reservation.setStatus("completed");
        reservationMapper.updateById(reservation);
        log.info("管理员完成预约: reservationId={}", reservationId);
    }
}
