package com.kidsbook.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kidsbook.dto.BookReviewRequest;
import com.kidsbook.entity.Book;
import com.kidsbook.entity.BookReview;
import com.kidsbook.entity.BorrowRecord;
import com.kidsbook.entity.Reader;
import com.kidsbook.mapper.BookMapper;
import com.kidsbook.mapper.BookReviewMapper;
import com.kidsbook.mapper.BorrowRecordMapper;
import com.kidsbook.mapper.ReaderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookReviewService {
    private final BookReviewMapper bookReviewMapper;
    private final BookMapper bookMapper;
    private final BorrowRecordMapper borrowRecordMapper;
    private final ReaderMapper readerMapper;
    private final ReaderPointsService readerPointsService;

    @Transactional
    public BookReview createReview(Long readerId, BookReviewRequest request) {
        LambdaQueryWrapper<BorrowRecord> borrowWrapper = new LambdaQueryWrapper<>();
        borrowWrapper.eq(BorrowRecord::getReaderId, readerId)
                .eq(BorrowRecord::getBookId, request.getBookId());
        Long borrowCount = borrowRecordMapper.selectCount(borrowWrapper);
        if (borrowCount == 0) {
            throw new RuntimeException("只能评价已借阅过的图书");
        }

        LambdaQueryWrapper<BookReview> existWrapper = new LambdaQueryWrapper<>();
        existWrapper.eq(BookReview::getReaderId, readerId)
                .eq(BookReview::getBookId, request.getBookId());
        if (bookReviewMapper.selectCount(existWrapper) > 0) {
            throw new RuntimeException("您已经评价过这本书了");
        }

        Reader reader = readerMapper.selectById(readerId);
        Book book = bookMapper.selectById(request.getBookId());
        if (book == null) {
            throw new RuntimeException("图书不存在");
        }

        BookReview review = new BookReview();
        review.setBookId(request.getBookId());
        review.setReaderId(readerId);
        review.setReaderName(reader != null ? reader.getName() : "未知");
        review.setBookTitle(book.getTitle());
        review.setRating(request.getRating());
        review.setContent(request.getContent());
        review.setStatus("pending");
        review.setCreateTime(LocalDateTime.now());
        review.setUpdateTime(LocalDateTime.now());
        bookReviewMapper.insert(review);

        readerPointsService.awardPoints(readerId, 5, "review", "评价《" + book.getTitle() + "》", null);
        log.info("读者{}评价了图书《{}》, 评分: {}", readerId, book.getTitle(), request.getRating());
        return review;
    }

    public BookReview updateReview(Long readerId, Long reviewId, BookReviewRequest request) {
        BookReview review = bookReviewMapper.selectById(reviewId);
        if (review == null) {
            throw new RuntimeException("评价不存在");
        }
        if (!review.getReaderId().equals(readerId)) {
            throw new RuntimeException("只能修改自己的评价");
        }
        if ("approved".equals(review.getStatus())) {
            throw new RuntimeException("已审核通过的评价不可修改");
        }

        review.setRating(request.getRating());
        review.setContent(request.getContent());
        review.setStatus("pending");
        review.setUpdateTime(LocalDateTime.now());
        bookReviewMapper.updateById(review);
        return review;
    }

    public void deleteOwnReview(Long readerId, Long reviewId) {
        BookReview review = bookReviewMapper.selectById(reviewId);
        if (review == null) {
            throw new RuntimeException("评价不存在");
        }
        if (!review.getReaderId().equals(readerId)) {
            throw new RuntimeException("只能删除自己的评价");
        }
        bookReviewMapper.deleteById(reviewId);
        refreshBookRating(review.getBookId());
    }

    public Page<BookReview> getMyReviews(Long readerId, int page, int size) {
        Page<BookReview> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<BookReview> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BookReview::getReaderId, readerId)
                .orderByDesc(BookReview::getCreateTime);
        return bookReviewMapper.selectPage(pageParam, wrapper);
    }

    public Page<BookReview> getBookReviews(Long bookId, int page, int size) {
        Page<BookReview> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<BookReview> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BookReview::getBookId, bookId)
                .eq(BookReview::getStatus, "approved")
                .orderByDesc(BookReview::getCreateTime);
        return bookReviewMapper.selectPage(pageParam, wrapper);
    }

    public Page<BookReview> adminListReviews(int page, int size, String status, Long bookId) {
        Page<BookReview> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<BookReview> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isEmpty()) {
            wrapper.eq(BookReview::getStatus, status);
        }
        if (bookId != null) {
            wrapper.eq(BookReview::getBookId, bookId);
        }
        wrapper.orderByDesc(BookReview::getCreateTime);
        return bookReviewMapper.selectPage(pageParam, wrapper);
    }

    @Transactional
    public void approveReview(Long reviewId) {
        BookReview review = bookReviewMapper.selectById(reviewId);
        if (review == null) {
            throw new RuntimeException("评价不存在");
        }
        review.setStatus("approved");
        review.setUpdateTime(LocalDateTime.now());
        bookReviewMapper.updateById(review);
        refreshBookRating(review.getBookId());
    }

    @Transactional
    public void rejectReview(Long reviewId) {
        BookReview review = bookReviewMapper.selectById(reviewId);
        if (review == null) {
            throw new RuntimeException("评价不存在");
        }
        review.setStatus("rejected");
        review.setUpdateTime(LocalDateTime.now());
        bookReviewMapper.updateById(review);
        refreshBookRating(review.getBookId());
    }

    public void adminReply(Long reviewId, String reply) {
        BookReview review = bookReviewMapper.selectById(reviewId);
        if (review == null) {
            throw new RuntimeException("评价不存在");
        }
        review.setAdminReply(reply);
        review.setReplyTime(LocalDateTime.now());
        review.setUpdateTime(LocalDateTime.now());
        bookReviewMapper.updateById(review);
    }

    @Transactional
    public void adminDeleteReview(Long reviewId) {
        BookReview review = bookReviewMapper.selectById(reviewId);
        if (review == null) {
            throw new RuntimeException("评价不存在");
        }
        Long bookId = review.getBookId();
        bookReviewMapper.deleteById(reviewId);
        refreshBookRating(bookId);
    }

    public void refreshBookRating(Long bookId) {
        BigDecimal avg = bookReviewMapper.avgRatingByBookId(bookId);
        Integer count = bookReviewMapper.countApprovedByBookId(bookId);
        Book book = bookMapper.selectById(bookId);
        if (book != null) {
            book.setAvgRating(avg != null ? avg.setScale(1, RoundingMode.HALF_UP) : BigDecimal.ZERO);
            book.setReviewCount(count != null ? count : 0);
            bookMapper.updateById(book);
        }
    }

    public boolean hasReaderBorrowedBook(Long readerId, Long bookId) {
        LambdaQueryWrapper<BorrowRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BorrowRecord::getReaderId, readerId)
                .eq(BorrowRecord::getBookId, bookId);
        return borrowRecordMapper.selectCount(wrapper) > 0;
    }

    public boolean hasReaderReviewed(Long readerId, Long bookId) {
        LambdaQueryWrapper<BookReview> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BookReview::getReaderId, readerId)
                .eq(BookReview::getBookId, bookId);
        return bookReviewMapper.selectCount(wrapper) > 0;
    }
}
