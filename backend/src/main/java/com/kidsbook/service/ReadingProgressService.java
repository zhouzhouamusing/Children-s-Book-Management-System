package com.kidsbook.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kidsbook.entity.Book;
import com.kidsbook.entity.Reader;
import com.kidsbook.entity.ReadingNote;
import com.kidsbook.entity.ReadingProgress;
import com.kidsbook.mapper.BookMapper;
import com.kidsbook.mapper.ReaderMapper;
import com.kidsbook.mapper.ReadingNoteMapper;
import com.kidsbook.mapper.ReadingProgressMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReadingProgressService {
    private final ReadingProgressMapper progressMapper;
    private final ReadingNoteMapper noteMapper;
    private final BookMapper bookMapper;
    private final ReaderMapper readerMapper;
    private final ReaderPointsService readerPointsService;

    public IPage<ReadingProgress> getProgressList(Long readerId, int page, int size, String status) {
        LambdaQueryWrapper<ReadingProgress> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReadingProgress::getReaderId, readerId);
        if (status != null && !status.isEmpty() && !"all".equals(status)) {
            wrapper.eq(ReadingProgress::getStatus, status);
        }
        wrapper.orderByDesc(ReadingProgress::getUpdateTime);
        return progressMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Transactional
    public ReadingProgress createOrUpdateProgress(Long readerId, Long bookId, Integer totalPages, Integer currentPage, Integer readingMinutes) {
        LambdaQueryWrapper<ReadingProgress> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReadingProgress::getReaderId, readerId)
                .eq(ReadingProgress::getBookId, bookId);
        ReadingProgress progress = progressMapper.selectOne(wrapper);

        Book book = bookMapper.selectById(bookId);
        String bookTitle = book != null ? book.getTitle() : "";

        boolean wasNotCompleted = true;

        if (progress == null) {
            progress = new ReadingProgress();
            progress.setReaderId(readerId);
            progress.setBookId(bookId);
            progress.setBookTitle(bookTitle);
            progress.setTotalPages(totalPages != null ? totalPages : 0);
            progress.setCurrentPage(currentPage != null ? currentPage : 0);
            progress.setReadingMinutes(readingMinutes != null ? readingMinutes : 0);
            progress.setProgressPercent(calcPercent(progress.getCurrentPage(), progress.getTotalPages()));
            progress.setStatus(progress.getProgressPercent() >= 100 ? "completed" : "reading");
            progressMapper.insert(progress);
        } else {
            wasNotCompleted = !"completed".equals(progress.getStatus());
            if (totalPages != null) progress.setTotalPages(totalPages);
            if (currentPage != null) progress.setCurrentPage(currentPage);
            if (readingMinutes != null) progress.setReadingMinutes(progress.getReadingMinutes() + readingMinutes);
            progress.setBookTitle(bookTitle);
            progress.setProgressPercent(calcPercent(progress.getCurrentPage(), progress.getTotalPages()));
            progress.setStatus(progress.getProgressPercent() >= 100 ? "completed" : "reading");
            progressMapper.updateById(progress);
        }

        if ("completed".equals(progress.getStatus()) && wasNotCompleted) {
            onReadingCompleted(readerId, bookTitle);
        }

        return progress;
    }

    @Transactional
    public void updateProgressStatus(Long readerId, Long progressId, String status) {
        ReadingProgress progress = progressMapper.selectById(progressId);
        if (progress == null || !progress.getReaderId().equals(readerId)) {
            throw new RuntimeException("阅读进度不存在");
        }
        boolean wasNotCompleted = !"completed".equals(progress.getStatus());
        progress.setStatus(status);
        if ("completed".equals(status)) {
            progress.setProgressPercent(100);
            progress.setCurrentPage(progress.getTotalPages());
        }
        progressMapper.updateById(progress);

        if ("completed".equals(status) && wasNotCompleted) {
            onReadingCompleted(readerId, progress.getBookTitle());
        }
    }

    private void onReadingCompleted(Long readerId, String bookTitle) {
        readerPointsService.awardPoints(readerId, 15, "reading_complete",
                "完成阅读《" + bookTitle + "》", null);

        Reader reader = readerMapper.selectById(readerId);
        if (reader != null) {
            int days = reader.getTotalReadingDays() != null ? reader.getTotalReadingDays() : 0;
            reader.setTotalReadingDays(days + 1);
            readerMapper.updateById(reader);
        }

        log.info("阅读完成: readerId={}, book={}", readerId, bookTitle);
    }

    @Transactional
    public void deleteProgress(Long readerId, Long progressId) {
        ReadingProgress progress = progressMapper.selectById(progressId);
        if (progress == null || !progress.getReaderId().equals(readerId)) {
            throw new RuntimeException("阅读进度不存在");
        }
        progressMapper.deleteById(progressId);
        LambdaQueryWrapper<ReadingNote> noteWrapper = new LambdaQueryWrapper<>();
        noteWrapper.eq(ReadingNote::getProgressId, progressId);
        noteMapper.delete(noteWrapper);
    }

    public IPage<ReadingNote> getNotes(Long readerId, Long bookId, int page, int size) {
        LambdaQueryWrapper<ReadingNote> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReadingNote::getReaderId, readerId);
        if (bookId != null) {
            wrapper.eq(ReadingNote::getBookId, bookId);
        }
        wrapper.orderByDesc(ReadingNote::getCreateTime);
        return noteMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Transactional
    public ReadingNote addNote(Long readerId, Long bookId, Long progressId, String content, Integer pageNumber) {
        Book book = bookMapper.selectById(bookId);
        String bookTitle = book != null ? book.getTitle() : "";

        ReadingNote note = new ReadingNote();
        note.setReaderId(readerId);
        note.setBookId(bookId);
        note.setProgressId(progressId);
        note.setBookTitle(bookTitle);
        note.setContent(content);
        note.setPageNumber(pageNumber != null ? pageNumber : 0);
        noteMapper.insert(note);

        readerPointsService.awardPoints(readerId, 3, "reading_note",
                "为《" + bookTitle + "》添加阅读笔记", null);

        return note;
    }

    @Transactional
    public void updateNote(Long readerId, Long noteId, String content, Integer pageNumber) {
        ReadingNote note = noteMapper.selectById(noteId);
        if (note == null || !note.getReaderId().equals(readerId)) {
            throw new RuntimeException("笔记不存在");
        }
        note.setContent(content);
        if (pageNumber != null) note.setPageNumber(pageNumber);
        noteMapper.updateById(note);
    }

    @Transactional
    public void deleteNote(Long readerId, Long noteId) {
        ReadingNote note = noteMapper.selectById(noteId);
        if (note == null || !note.getReaderId().equals(readerId)) {
            throw new RuntimeException("笔记不存在");
        }
        noteMapper.deleteById(noteId);
    }

    public Map<String, Object> getStatistics(Long readerId) {
        int totalMinutes = progressMapper.sumReadingMinutesByReaderId(readerId);
        int completedCount = progressMapper.countCompletedByReaderId(readerId);
        int totalCount = progressMapper.countByReaderId(readerId);
        int completionRate = totalCount > 0 ? (completedCount * 100 / totalCount) : 0;

        LambdaQueryWrapper<ReadingNote> noteWrapper = new LambdaQueryWrapper<>();
        noteWrapper.eq(ReadingNote::getReaderId, readerId);
        long noteCount = noteMapper.selectCount(noteWrapper);

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalReadingMinutes", totalMinutes);
        stats.put("totalReadingHours", totalMinutes / 60);
        stats.put("completedBooks", completedCount);
        stats.put("totalBooks", totalCount);
        stats.put("completionRate", completionRate);
        stats.put("noteCount", noteCount);
        return stats;
    }

    private int calcPercent(int current, int total) {
        if (total <= 0) return 0;
        int percent = (int) ((current * 100.0) / total);
        return Math.min(percent, 100);
    }
}
