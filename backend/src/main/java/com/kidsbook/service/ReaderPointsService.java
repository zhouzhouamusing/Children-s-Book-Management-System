package com.kidsbook.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kidsbook.entity.Reader;
import com.kidsbook.entity.ReaderMonthlyStats;
import com.kidsbook.entity.ReaderPointsLog;
import com.kidsbook.mapper.ReaderMapper;
import com.kidsbook.mapper.ReaderMonthlyStatsMapper;
import com.kidsbook.mapper.ReaderPointsLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReaderPointsService {
    private final ReaderPointsLogMapper pointsLogMapper;
    private final ReaderMonthlyStatsMapper monthlyStatsMapper;
    private final ReaderMapper readerMapper;

    @Transactional
    public void awardPoints(Long readerId, int points, String type, String description, Long borrowRecordId) {
        ReaderPointsLog logEntry = new ReaderPointsLog();
        logEntry.setReaderId(readerId);
        logEntry.setPoints(points);
        logEntry.setType(type);
        logEntry.setDescription(description);
        logEntry.setBorrowRecordId(borrowRecordId);
        logEntry.setCreateTime(LocalDateTime.now());
        pointsLogMapper.insert(logEntry);

        Reader reader = readerMapper.selectById(readerId);
        if (reader != null) {
            int currentPoints = reader.getPoints() != null ? reader.getPoints() : 0;
            int newPoints = Math.max(0, currentPoints + points);
            reader.setPoints(newPoints);
            reader.setLevel(calculateLevel(newPoints));
            readerMapper.updateById(reader);
        }

        updateMonthlyStats(readerId, points);
        log.info("积分变动: readerId={}, points={}, type={}", readerId, points, type);
    }

    @Transactional
    public void onBorrow(Long readerId, Long borrowRecordId, String bookTitle) {
        awardPoints(readerId, 10, "borrow", "借阅《" + bookTitle + "》", borrowRecordId);
        updateMonthlyBorrowCount(readerId);
    }

    @Transactional
    public void onReturn(Long readerId, Long borrowRecordId, String bookTitle, boolean onTime) {
        if (onTime) {
            awardPoints(readerId, 5, "return_ontime", "按时归还《" + bookTitle + "》", borrowRecordId);
        } else {
            awardPoints(readerId, -10, "overdue_penalty", "逾期归还《" + bookTitle + "》", borrowRecordId);
        }
        updateMonthlyReturnCount(readerId);
    }

    public Map<String, Object> getPointsDetail(Long readerId) {
        Reader reader = readerMapper.selectById(readerId);
        int totalPoints = reader != null && reader.getPoints() != null ? reader.getPoints() : 0;
        String level = reader != null && reader.getLevel() != null ? reader.getLevel() : "新手读者";

        LambdaQueryWrapper<ReaderPointsLog> recentWrapper = new LambdaQueryWrapper<>();
        recentWrapper.eq(ReaderPointsLog::getReaderId, readerId)
                .orderByDesc(ReaderPointsLog::getCreateTime)
                .last("LIMIT 20");
        List<ReaderPointsLog> recentLogs = pointsLogMapper.selectList(recentWrapper);

        List<Map<String, Object>> pointsByType = pointsLogMapper.sumPointsByType(readerId);

        int nextLevelPoints = getNextLevelThreshold(totalPoints);

        Map<String, Object> data = new HashMap<>();
        data.put("totalPoints", totalPoints);
        data.put("level", level);
        data.put("nextLevelPoints", nextLevelPoints);
        data.put("recentLogs", recentLogs);
        data.put("pointsByType", pointsByType);
        return data;
    }

    public Map<String, Object> getStatistics(Long readerId) {
        Reader reader = readerMapper.selectById(readerId);
        int totalPoints = reader != null && reader.getPoints() != null ? reader.getPoints() : 0;
        int totalReadingDays = reader != null && reader.getTotalReadingDays() != null ? reader.getTotalReadingDays() : 0;
        String level = reader != null && reader.getLevel() != null ? reader.getLevel() : "新手读者";
        int borrowCount = reader != null && reader.getBorrowCount() != null ? reader.getBorrowCount() : 0;

        List<ReaderMonthlyStats> monthlyStats = monthlyStatsMapper.getLast12Months(readerId);

        Map<String, Object> data = new HashMap<>();
        data.put("totalPoints", totalPoints);
        data.put("totalReadingDays", totalReadingDays);
        data.put("level", level);
        data.put("totalBorrows", borrowCount);
        data.put("monthlyStats", monthlyStats);
        return data;
    }

    private String calculateLevel(int points) {
        if (points >= 500) return "阅读大师";
        if (points >= 200) return "阅读达人";
        if (points >= 50) return "小书虫";
        return "新手读者";
    }

    private int getNextLevelThreshold(int currentPoints) {
        if (currentPoints >= 500) return 1000;
        if (currentPoints >= 200) return 500;
        if (currentPoints >= 50) return 200;
        return 50;
    }

    private void updateMonthlyStats(Long readerId, int points) {
        String yearMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        ReaderMonthlyStats stats = getOrCreateMonthlyStats(readerId, yearMonth);
        if (points > 0) {
            stats.setPointsEarned(stats.getPointsEarned() + points);
        }
        monthlyStatsMapper.updateById(stats);
    }

    private void updateMonthlyBorrowCount(Long readerId) {
        String yearMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        ReaderMonthlyStats stats = getOrCreateMonthlyStats(readerId, yearMonth);
        stats.setBorrowCount(stats.getBorrowCount() + 1);
        monthlyStatsMapper.updateById(stats);
    }

    private void updateMonthlyReturnCount(Long readerId) {
        String yearMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        ReaderMonthlyStats stats = getOrCreateMonthlyStats(readerId, yearMonth);
        stats.setReturnCount(stats.getReturnCount() + 1);
        monthlyStatsMapper.updateById(stats);
    }

    private ReaderMonthlyStats getOrCreateMonthlyStats(Long readerId, String yearMonth) {
        LambdaQueryWrapper<ReaderMonthlyStats> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReaderMonthlyStats::getReaderId, readerId)
                .eq(ReaderMonthlyStats::getYearMonth, yearMonth);
        ReaderMonthlyStats stats = monthlyStatsMapper.selectOne(wrapper);
        if (stats == null) {
            stats = new ReaderMonthlyStats();
            stats.setReaderId(readerId);
            stats.setYearMonth(yearMonth);
            stats.setBorrowCount(0);
            stats.setReturnCount(0);
            stats.setReadingDays(0);
            stats.setPointsEarned(0);
            stats.setCreateTime(LocalDateTime.now());
            stats.setUpdateTime(LocalDateTime.now());
            monthlyStatsMapper.insert(stats);
        }
        return stats;
    }
}
