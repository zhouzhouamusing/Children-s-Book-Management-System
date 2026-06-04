package com.kidsbook.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kidsbook.common.BusinessException;
import com.kidsbook.dto.BorrowRequest;
import com.kidsbook.entity.Book;
import com.kidsbook.entity.BorrowRecord;
import com.kidsbook.entity.Reader;
import com.kidsbook.entity.ReaderAccount;
import com.kidsbook.mapper.BookMapper;
import com.kidsbook.mapper.BorrowRecordMapper;
import com.kidsbook.mapper.ReaderAccountMapper;
import com.kidsbook.mapper.ReaderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class BorrowService extends ServiceImpl<BorrowRecordMapper, BorrowRecord> {
    private final BorrowRecordMapper borrowRecordMapper;
    private final BookMapper bookMapper;
    private final ReaderMapper readerMapper;
    private final ReaderAccountMapper readerAccountMapper;
    private final ReaderPointsService readerPointsService;
    private final EmailService emailService;
    private final AuditLogService auditLogService;
    @Lazy
    private final BookReservationService bookReservationService;
    @Lazy
    private final ReaderService readerService;

    @Transactional
    public void borrowBook(BorrowRequest request) {
        if (request.getReaderId() == null) {
            throw new BusinessException(400, "读者ID不能为空");
        }
        if (request.getBookId() == null) {
            throw new BusinessException(400, "图书ID不能为空");
        }

        Reader reader = readerMapper.selectById(request.getReaderId());
        if (reader == null) {
            throw new BusinessException(404, "读者不存在");
        }
        if ("suspended".equals(reader.getStatus())) {
            throw new BusinessException(400, "该读者借阅权限已暂停");
        }

        Book book = bookMapper.selectById(request.getBookId());
        if (book == null) {
            throw new BusinessException(404, "图书不存在");
        }
        if (book.getStatus() == 0) {
            throw new BusinessException(400, "该图书已下架");
        }
        if (book.getStock() <= 0) {
            throw new BusinessException(400, "该图书库存不足");
        }

        LambdaQueryWrapper<BorrowRecord> duplicateCheck = new LambdaQueryWrapper<>();
        duplicateCheck.eq(BorrowRecord::getReaderId, request.getReaderId())
                .eq(BorrowRecord::getBookId, request.getBookId())
                .eq(BorrowRecord::getStatus, "borrowing");
        if (borrowRecordMapper.selectCount(duplicateCheck) > 0) {
            throw new BusinessException(400, "该读者已借阅此书且未归还");
        }

        BorrowRecord record = new BorrowRecord();
        record.setReaderId(request.getReaderId());
        record.setBookId(request.getBookId());
        record.setBookTitle(book.getTitle());
        record.setBorrowDate(LocalDate.now());
        record.setDueDate(LocalDate.now().plusDays(request.getBorrowDays() != null ? request.getBorrowDays() : 14));
        record.setStatus("borrowing");
        borrowRecordMapper.insert(record);

        book.setStock(book.getStock() - 1);
        bookMapper.updateById(book);

        reader.setBorrowCount(reader.getBorrowCount() + 1);
        readerMapper.updateById(reader);

        readerPointsService.onBorrow(reader.getId(), record.getId(), book.getTitle());

        bookReservationService.fulfillReservation(request.getReaderId(), request.getBookId());

        sendBorrowNotification(reader, book.getTitle(), record.getDueDate());

        auditLogService.log("BORROW_BOOK", "borrow_record", record.getId(),
            "读者[" + reader.getName() + "]借阅《" + book.getTitle() + "》");

        log.info("借书成功: 读者={}, 图书={}", reader.getName(), book.getTitle());
    }

    @Transactional
    public void returnBook(Long recordId) {
        BorrowRecord record = borrowRecordMapper.selectById(recordId);
        if (record == null) {
            throw new BusinessException(404, "借阅记录不存在");
        }
        if ("returned".equals(record.getStatus())) {
            throw new BusinessException(400, "该图书已归还");
        }

        record.setReturnDate(LocalDate.now());
        record.setStatus("returned");
        borrowRecordMapper.updateById(record);

        Book book = bookMapper.selectById(record.getBookId());
        if (book != null) {
            book.setStock(book.getStock() + 1);
            bookMapper.updateById(book);

            bookReservationService.notifyNextReservation(book.getId(), book.getTitle());
        }

        boolean isOverdue = LocalDate.now().isAfter(record.getDueDate());
        if (isOverdue) {
            Reader reader = readerMapper.selectById(record.getReaderId());
            if (reader != null) {
                reader.setOverdueCount(reader.getOverdueCount() + 1);
                readerMapper.updateById(reader);

                if ("suspended".equals(reader.getStatus()) && !"manual".equals(reader.getSuspendReason())) {
                    LambdaQueryWrapper<BorrowRecord> activeOverdue = new LambdaQueryWrapper<>();
                    activeOverdue.eq(BorrowRecord::getReaderId, reader.getId())
                            .eq(BorrowRecord::getStatus, "overdue");
                    if (borrowRecordMapper.selectCount(activeOverdue) == 0) {
                        reader.setStatus("normal");
                        reader.setSuspendReason(null);
                        readerMapper.updateById(reader);
                        log.info("读者{}已自动恢复借阅权限", reader.getName());
                    }
                }
            }
        }

        if (record.getReaderId() != null) {
            readerPointsService.onReturn(record.getReaderId(), record.getId(), record.getBookTitle(), !isOverdue);
        }

        auditLogService.log("RETURN_BOOK", "borrow_record", record.getId(),
            "归还《" + record.getBookTitle() + "》" + (isOverdue ? "(逾期)" : ""));

        log.info("还书成功: recordId={}, 图书={}", recordId, record.getBookTitle());
    }

    @Transactional
    public void renewBook(Long recordId, Integer extraDays) {
        BorrowRecord record = borrowRecordMapper.selectById(recordId);
        if (record == null) {
            throw new BusinessException(404, "借阅记录不存在");
        }
        if (!"borrowing".equals(record.getStatus())) {
            throw new BusinessException(400, "只能续借状态为'借阅中'的记录");
        }
        if (record.getDueDate().isBefore(LocalDate.now())) {
            throw new BusinessException(400, "已逾期图书不能续借，请先归还");
        }

        int days = (extraDays != null && extraDays > 0) ? extraDays : 14;
        record.setDueDate(record.getDueDate().plusDays(days));
        borrowRecordMapper.updateById(record);
        log.info("续借成功: recordId={}, 新到期日={}", recordId, record.getDueDate());
    }

    @Transactional
    public IPage<BorrowRecord> list(int page, int size, String keyword, String status) {
        markNewOverdue();

        LambdaQueryWrapper<BorrowRecord> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(BorrowRecord::getBookTitle, keyword);
        }
        if (status != null && !status.isEmpty() && !"all".equals(status)) {
            wrapper.eq(BorrowRecord::getStatus, status);
        }
        wrapper.orderByDesc(BorrowRecord::getCreateTime);
        return borrowRecordMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Transactional
    public Map<String, Object> getStatistics() {
        markNewOverdue();

        long total = borrowRecordMapper.selectCount(null);
        LambdaQueryWrapper<BorrowRecord> borrowingWrapper = new LambdaQueryWrapper<>();
        borrowingWrapper.eq(BorrowRecord::getStatus, "borrowing");
        long borrowing = borrowRecordMapper.selectCount(borrowingWrapper);

        LambdaQueryWrapper<BorrowRecord> overdueWrapper = new LambdaQueryWrapper<>();
        overdueWrapper.eq(BorrowRecord::getStatus, "overdue");
        long overdue = borrowRecordMapper.selectCount(overdueWrapper);

        LambdaQueryWrapper<BorrowRecord> returnedWrapper = new LambdaQueryWrapper<>();
        returnedWrapper.eq(BorrowRecord::getStatus, "returned");
        long returned = borrowRecordMapper.selectCount(returnedWrapper);

        LambdaQueryWrapper<BorrowRecord> todayWrapper = new LambdaQueryWrapper<>();
        todayWrapper.eq(BorrowRecord::getBorrowDate, LocalDate.now());
        long todayBorrows = borrowRecordMapper.selectCount(todayWrapper);

        Map<String, Object> stats = new HashMap<>();
        stats.put("total", total);
        stats.put("borrowing", borrowing);
        stats.put("overdue", overdue);
        stats.put("returned", returned);
        stats.put("todayBorrows", todayBorrows);
        return stats;
    }

    @Transactional
    public void markNewOverdue() {
        LambdaQueryWrapper<BorrowRecord> overdueCheck = new LambdaQueryWrapper<>();
        overdueCheck.eq(BorrowRecord::getStatus, "borrowing")
                .lt(BorrowRecord::getDueDate, LocalDate.now());
        List<BorrowRecord> newOverdue = borrowRecordMapper.selectList(overdueCheck);
        for (BorrowRecord r : newOverdue) {
            r.setStatus("overdue");
            borrowRecordMapper.updateById(r);
            readerService.checkAndSuspend(r.getReaderId());
        }
        if (!newOverdue.isEmpty()) {
            log.info("实时标记{}条逾期借阅记录", newOverdue.size());
        }
    }

    @Transactional
    public void markOverdueForReader(Long readerId) {
        LambdaQueryWrapper<BorrowRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BorrowRecord::getReaderId, readerId)
                .eq(BorrowRecord::getStatus, "borrowing")
                .lt(BorrowRecord::getDueDate, LocalDate.now());
        List<BorrowRecord> newOverdue = borrowRecordMapper.selectList(wrapper);
        for (BorrowRecord r : newOverdue) {
            r.setStatus("overdue");
            borrowRecordMapper.updateById(r);
        }
    }

    private void sendBorrowNotification(Reader reader, String bookTitle, LocalDate dueDate) {
        try {
            ReaderAccount account = readerAccountMapper.selectOne(
                new LambdaQueryWrapper<ReaderAccount>().eq(ReaderAccount::getReaderId, reader.getId()));
            if (account != null && account.getEmail() != null && !account.getEmail().isEmpty()) {
                emailService.sendBorrowConfirmation(account.getEmail(), reader.getName(), bookTitle, dueDate);
            }
        } catch (Exception e) {
            log.warn("借阅通知发送失败: {}", e.getMessage());
        }
    }
}
