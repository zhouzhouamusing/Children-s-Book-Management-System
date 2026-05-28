package com.kidsbook.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kidsbook.entity.BookReservation;
import com.kidsbook.entity.BorrowRecord;
import com.kidsbook.mapper.BookReservationMapper;
import com.kidsbook.mapper.BorrowRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduledTasks {
    private final BookReservationMapper reservationMapper;
    private final BorrowRecordMapper borrowRecordMapper;

    /**
     * 每小时检查并处理过期预约
     */
    @Scheduled(fixedRate = 3600000, initialDelay = 60000)
    public void expireReservations() {
        LambdaQueryWrapper<BookReservation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BookReservation::getStatus, "pending")
                .lt(BookReservation::getExpireDate, LocalDateTime.now());
        List<BookReservation> expired = reservationMapper.selectList(wrapper);

        if (!expired.isEmpty()) {
            for (BookReservation r : expired) {
                r.setStatus("expired");
                reservationMapper.updateById(r);
            }
            log.info("定时任务: 已处理{}条过期预约", expired.size());
        }
    }

    /**
     * 每天凌晨1点检查逾期借阅记录
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void markOverdueBorrows() {
        LambdaQueryWrapper<BorrowRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BorrowRecord::getStatus, "borrowing")
                .lt(BorrowRecord::getDueDate, LocalDate.now());
        List<BorrowRecord> overdue = borrowRecordMapper.selectList(wrapper);

        if (!overdue.isEmpty()) {
            for (BorrowRecord record : overdue) {
                record.setStatus("overdue");
                borrowRecordMapper.updateById(record);
            }
            log.info("定时任务: 已标记{}条逾期借阅记录", overdue.size());
        }
    }
}
