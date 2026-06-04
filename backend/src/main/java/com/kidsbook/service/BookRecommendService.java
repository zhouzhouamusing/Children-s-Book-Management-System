package com.kidsbook.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kidsbook.common.BusinessException;
import com.kidsbook.entity.Book;
import com.kidsbook.entity.BorrowRecord;
import com.kidsbook.entity.Reader;
import com.kidsbook.entity.ReadingProgress;
import com.kidsbook.mapper.BookMapper;
import com.kidsbook.mapper.BorrowRecordMapper;
import com.kidsbook.mapper.ReaderMapper;
import com.kidsbook.mapper.ReadingProgressMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookRecommendService {
    private final BookMapper bookMapper;
    private final BorrowRecordMapper borrowRecordMapper;
    private final ReaderMapper readerMapper;
    private final ReadingProgressMapper readingProgressMapper;

    public List<Book> getRecommendByHistory(Long readerId, int limit) {
        LambdaQueryWrapper<BorrowRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BorrowRecord::getReaderId, readerId)
                .orderByDesc(BorrowRecord::getBorrowDate);
        List<BorrowRecord> records = borrowRecordMapper.selectList(wrapper);

        if (records.isEmpty()) {
            return getTopBorrowedBooks(limit);
        }

        Set<Long> borrowedBookIds = records.stream()
                .map(BorrowRecord::getBookId)
                .collect(Collectors.toSet());

        Map<String, Long> categoryCount = records.stream()
                .map(BorrowRecord::getBookId)
                .map(bookMapper::selectById)
                .filter(Objects::nonNull)
                .filter(b -> b.getCategory() != null)
                .collect(Collectors.groupingBy(Book::getCategory, Collectors.counting()));

        List<String> topCategories = categoryCount.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(3)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        if (topCategories.isEmpty()) {
            return getTopBorrowedBooks(limit);
        }

        LambdaQueryWrapper<Book> bookWrapper = new LambdaQueryWrapper<>();
        bookWrapper.in(Book::getCategory, topCategories)
                .eq(Book::getStatus, 1)
                .notIn(Book::getId, borrowedBookIds)
                .orderByDesc(Book::getAvgRating)
                .orderByDesc(Book::getCreateTime)
                .last("LIMIT " + limit);

        List<Book> recommended = bookMapper.selectList(bookWrapper);

        if (recommended.size() < limit) {
            LambdaQueryWrapper<Book> fillWrapper = new LambdaQueryWrapper<>();
            fillWrapper.eq(Book::getStatus, 1)
                    .notIn(Book::getId, borrowedBookIds);
            if (!recommended.isEmpty()) {
                fillWrapper.notIn(Book::getId, recommended.stream().map(Book::getId).collect(Collectors.toList()));
            }
            fillWrapper.orderByDesc(Book::getCreateTime)
                    .last("LIMIT " + (limit - recommended.size()));
            recommended.addAll(bookMapper.selectList(fillWrapper));
        }

        return recommended;
    }

    public List<Book> getRecommendByAge(Long readerId, int limit) {
        Reader reader = readerMapper.selectById(readerId);
        if (reader == null || reader.getAge() == null) {
            return getTopBorrowedBooks(limit);
        }

        int age = reader.getAge();
        String agePattern = determineAgeRange(age);

        LambdaQueryWrapper<Book> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Book::getStatus, 1);
        if (agePattern != null) {
            wrapper.like(Book::getAgeRange, agePattern);
        }
        wrapper.orderByDesc(Book::getCreateTime)
                .last("LIMIT " + limit);

        List<Book> books = bookMapper.selectList(wrapper);

        if (books.size() < limit) {
            LambdaQueryWrapper<Book> fillWrapper = new LambdaQueryWrapper<>();
            fillWrapper.eq(Book::getStatus, 1);
            if (!books.isEmpty()) {
                fillWrapper.notIn(Book::getId, books.stream().map(Book::getId).collect(Collectors.toList()));
            }
            fillWrapper.orderByDesc(Book::getCreateTime)
                    .last("LIMIT " + (limit - books.size()));
            books.addAll(bookMapper.selectList(fillWrapper));
        }

        return books;
    }

    public List<Map<String, Object>> getTopBorrowedBooksWithCount(int limit) {
        LambdaQueryWrapper<BorrowRecord> wrapper = new LambdaQueryWrapper<>();
        List<BorrowRecord> allRecords = borrowRecordMapper.selectList(wrapper);

        Map<Long, Long> bookBorrowCount = allRecords.stream()
                .filter(r -> r.getBookId() != null)
                .collect(Collectors.groupingBy(BorrowRecord::getBookId, Collectors.counting()));

        return bookBorrowCount.entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
                .limit(limit)
                .map(entry -> {
                    Book book = bookMapper.selectById(entry.getKey());
                    Map<String, Object> item = new HashMap<>();
                    if (book != null) {
                        item.put("id", book.getId());
                        item.put("title", book.getTitle());
                        item.put("author", book.getAuthor());
                        item.put("category", book.getCategory());
                        item.put("ageRange", book.getAgeRange());
                        item.put("coverUrl", book.getCoverUrl());
                        item.put("description", book.getDescription());
                        item.put("stock", book.getStock());
                    }
                    item.put("borrowCount", entry.getValue());
                    return item;
                })
                .filter(item -> item.containsKey("title"))
                .collect(Collectors.toList());
    }

    public List<Book> getTopBorrowedBooks(int limit) {
        return getTopBorrowedBooksWithCount(limit).stream()
                .map(item -> {
                    Book book = new Book();
                    book.setId((Long) item.get("id"));
                    book.setTitle((String) item.get("title"));
                    book.setAuthor((String) item.get("author"));
                    book.setCategory((String) item.get("category"));
                    book.setAgeRange((String) item.get("ageRange"));
                    book.setCoverUrl((String) item.get("coverUrl"));
                    book.setDescription((String) item.get("description"));
                    book.setStock((Integer) item.get("stock"));
                    return book;
                })
                .collect(Collectors.toList());
    }

    private String determineAgeRange(int age) {
        if (age <= 3) return "0-3";
        if (age <= 6) return "3-6";
        if (age <= 9) return "6-9";
        if (age <= 12) return "9-12";
        return "12";
    }

    public List<Book> getRecommendByReadingProgress(Long readerId, int limit) {
        LambdaQueryWrapper<ReadingProgress> progressWrapper = new LambdaQueryWrapper<>();
        progressWrapper.eq(ReadingProgress::getReaderId, readerId)
                .in(ReadingProgress::getStatus, List.of("reading", "completed"));
        List<ReadingProgress> progressList = readingProgressMapper.selectList(progressWrapper);

        if (progressList.isEmpty()) {
            return getTopRatedBooks(limit);
        }

        Set<Long> progressBookIds = progressList.stream()
                .map(ReadingProgress::getBookId)
                .collect(Collectors.toSet());

        Map<String, Double> categoryScore = new HashMap<>();
        for (ReadingProgress progress : progressList) {
            Book book = bookMapper.selectById(progress.getBookId());
            if (book != null && book.getCategory() != null) {
                double weight = "completed".equals(progress.getStatus()) ? 2.0 : 1.0;
                categoryScore.merge(book.getCategory(), weight, Double::sum);
            }
        }

        List<String> topCategories = categoryScore.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(3)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        if (topCategories.isEmpty()) {
            return getTopRatedBooks(limit);
        }

        LambdaQueryWrapper<BorrowRecord> borrowWrapper = new LambdaQueryWrapper<>();
        borrowWrapper.eq(BorrowRecord::getReaderId, readerId);
        Set<Long> borrowedBookIds = borrowRecordMapper.selectList(borrowWrapper).stream()
                .map(BorrowRecord::getBookId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<Long> excludeIds = new HashSet<>(progressBookIds);
        excludeIds.addAll(borrowedBookIds);

        LambdaQueryWrapper<Book> bookWrapper = new LambdaQueryWrapper<>();
        bookWrapper.in(Book::getCategory, topCategories)
                .eq(Book::getStatus, 1);
        if (!excludeIds.isEmpty()) {
            bookWrapper.notIn(Book::getId, excludeIds);
        }
        bookWrapper.orderByDesc(Book::getAvgRating)
                .orderByDesc(Book::getCreateTime)
                .last("LIMIT " + limit);

        List<Book> recommended = bookMapper.selectList(bookWrapper);

        if (recommended.size() < limit) {
            Set<Long> allExclude = new HashSet<>(excludeIds);
            allExclude.addAll(recommended.stream().map(Book::getId).collect(Collectors.toSet()));
            LambdaQueryWrapper<Book> fillWrapper = new LambdaQueryWrapper<>();
            fillWrapper.eq(Book::getStatus, 1);
            if (!allExclude.isEmpty()) {
                fillWrapper.notIn(Book::getId, allExclude);
            }
            fillWrapper.orderByDesc(Book::getAvgRating)
                    .orderByDesc(Book::getCreateTime)
                    .last("LIMIT " + (limit - recommended.size()));
            recommended.addAll(bookMapper.selectList(fillWrapper));
        }

        return recommended;
    }

    public List<Book> getTopRatedBooks(int limit) {
        LambdaQueryWrapper<Book> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Book::getStatus, 1)
                .gt(Book::getReviewCount, 0)
                .orderByDesc(Book::getAvgRating)
                .orderByDesc(Book::getReviewCount)
                .last("LIMIT " + limit);
        return bookMapper.selectList(wrapper);
    }
}
