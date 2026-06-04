package com.kidsbook.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kidsbook.common.BusinessException;
import com.kidsbook.dto.BookRequest;
import com.kidsbook.entity.Book;
import com.kidsbook.entity.BookReservation;
import com.kidsbook.entity.BookReview;
import com.kidsbook.entity.BorrowRecord;
import com.kidsbook.entity.Reader;
import com.kidsbook.entity.ReadingNote;
import com.kidsbook.entity.ReadingProgress;
import com.kidsbook.mapper.BookMapper;
import com.kidsbook.mapper.BookReservationMapper;
import com.kidsbook.mapper.BookReviewMapper;
import com.kidsbook.mapper.BorrowRecordMapper;
import com.kidsbook.mapper.ReaderMapper;
import com.kidsbook.mapper.ReadingNoteMapper;
import com.kidsbook.mapper.ReadingProgressMapper;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.bean.BeanUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class BookService extends ServiceImpl<BookMapper, Book> {
    private final BookMapper bookMapper;
    private final BorrowRecordMapper borrowRecordMapper;
    private final ReaderMapper readerMapper;
    private final BookReviewMapper bookReviewMapper;
    private final BookReservationMapper bookReservationMapper;
    private final ReadingProgressMapper readingProgressMapper;
    private final ReadingNoteMapper readingNoteMapper;
    private final FileUploadService fileUploadService;
    private final AuditLogService auditLogService;

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

    public Long addBook(BookRequest request) {
        Book book = BeanUtil.copyProperties(request, Book.class);
        bookMapper.insert(book);
        auditLogService.log("ADD_BOOK", "book", book.getId(), "新增图书《" + book.getTitle() + "》");
        return book.getId();
    }

    @Transactional
    public void updateBook(BookRequest request) {
        Book existing = bookMapper.selectById(request.getId());
        Book book = BeanUtil.copyProperties(request, Book.class);
        bookMapper.updateById(book);

        if (existing != null && request.getTitle() != null
                && !request.getTitle().equals(existing.getTitle())) {
            LambdaQueryWrapper<BookReview> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(BookReview::getBookId, request.getId());
            List<BookReview> reviews = bookReviewMapper.selectList(wrapper);
            for (BookReview review : reviews) {
                review.setBookTitle(request.getTitle());
                bookReviewMapper.updateById(review);
            }
        }
    }

    @Transactional
    public void deleteBook(Long id) {
        Book book = bookMapper.selectById(id);
        if (book == null) {
            throw new BusinessException(404, "图书不存在");
        }

        // 检查是否存在未归还的借阅记录（包括借阅中、逾期、续借状态）
        LambdaQueryWrapper<BorrowRecord> unreturnedCheck = new LambdaQueryWrapper<>();
        unreturnedCheck.eq(BorrowRecord::getBookId, id)
            .isNull(BorrowRecord::getReturnDate);
        long unreturnedCount = borrowRecordMapper.selectCount(unreturnedCheck);
        if (unreturnedCount > 0) {
            throw new BusinessException(400, "该图书有" + unreturnedCount + "条未归还的借阅记录，不能删除");
        }

        // 检查是否存在活跃预约（待处理或待取书）
        if (bookReservationMapper.countActiveByBookId(id) > 0) {
            throw new BusinessException(400, "该图书有待处理的预约记录，不能删除");
        }

        // 查询借阅记录总数用于审计
        LambdaQueryWrapper<BorrowRecord> allBorrowWrapper = new LambdaQueryWrapper<>();
        allBorrowWrapper.eq(BorrowRecord::getBookId, id);
        long totalBorrowRecords = borrowRecordMapper.selectCount(allBorrowWrapper);

        // 清理阅读笔记
        LambdaQueryWrapper<ReadingNote> noteWrapper = new LambdaQueryWrapper<>();
        noteWrapper.eq(ReadingNote::getBookId, id);
        readingNoteMapper.delete(noteWrapper);

        // 清理阅读进度
        LambdaQueryWrapper<ReadingProgress> progressWrapper = new LambdaQueryWrapper<>();
        progressWrapper.eq(ReadingProgress::getBookId, id);
        readingProgressMapper.delete(progressWrapper);

        // 清理预约记录
        LambdaQueryWrapper<BookReservation> reservationWrapper = new LambdaQueryWrapper<>();
        reservationWrapper.eq(BookReservation::getBookId, id);
        bookReservationMapper.delete(reservationWrapper);

        // 清理所有借阅记录（已确认均为已归还状态）
        borrowRecordMapper.delete(allBorrowWrapper);

        // 清理文件资源
        fileUploadService.deleteByBookId(id);

        // 清理评论
        LambdaQueryWrapper<BookReview> reviewWrapper = new LambdaQueryWrapper<>();
        reviewWrapper.eq(BookReview::getBookId, id);
        bookReviewMapper.delete(reviewWrapper);

        // 删除图书
        bookMapper.deleteById(id);
        auditLogService.log("DELETE_BOOK", "book", id,
            "删除图书《" + book.getTitle() + "》及关联数据（借阅记录" + totalBorrowRecords + "条）");
    }

    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();

        // 图书统计
        stats.put("totalBooks", bookMapper.totalCount());
        stats.put("totalStock", bookMapper.totalStock());
        stats.put("categoryStats", bookMapper.countByCategory());

        // 借阅统计
        long totalBorrows = borrowRecordMapper.selectCount(null);
        LambdaQueryWrapper<BorrowRecord> borrowingW = new LambdaQueryWrapper<>();
        borrowingW.eq(BorrowRecord::getStatus, "borrowing");
        long activeBorrows = borrowRecordMapper.selectCount(borrowingW);
        LambdaQueryWrapper<BorrowRecord> overdueW = new LambdaQueryWrapper<>();
        overdueW.eq(BorrowRecord::getStatus, "overdue");
        long overdueBorrows = borrowRecordMapper.selectCount(overdueW);
        stats.put("totalBorrows", totalBorrows);
        stats.put("activeBorrows", activeBorrows);
        stats.put("overdueBorrows", overdueBorrows);

        // 读者统计
        long totalReaders = readerMapper.selectCount(null);
        stats.put("totalReaders", totalReaders);
        stats.put("activeReaders", readerMapper.countActive());
        stats.put("systemStatus", "运行正常");

        return stats;
    }

    public List<String> getAllCategories() {
        List<Map<String, Object>> list = bookMapper.countByCategory();
        return list.stream().map(m -> (String) m.get("category")).toList();
    }
}
