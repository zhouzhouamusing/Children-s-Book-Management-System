package com.kidsbook.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kidsbook.common.Permission;
import com.kidsbook.common.RolePermissions;
import com.kidsbook.entity.BookReservation;
import com.kidsbook.entity.BorrowRecord;
import com.kidsbook.entity.Reader;
import com.kidsbook.entity.ReaderAccount;
import com.kidsbook.mapper.BookReservationMapper;
import com.kidsbook.mapper.BorrowRecordMapper;
import com.kidsbook.mapper.ReaderMapper;
import com.kidsbook.mapper.ReaderAccountMapper;
import com.kidsbook.service.EmailService;
import com.kidsbook.service.ReaderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduledTasks {
    private final BookReservationMapper reservationMapper;
    private final BorrowRecordMapper borrowRecordMapper;
    private final ReaderMapper readerMapper;
    private final ReaderAccountMapper readerAccountMapper;
    private final EmailService emailService;
    private final ReaderService readerService;

    @Transactional
    @Scheduled(fixedRate = 3600000, initialDelay = 60000)
    public void expireReservations() {
        LambdaQueryWrapper<BookReservation> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(BookReservation::getStatus, "pending", "ready_for_pickup")
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

    @Transactional
    @Scheduled(fixedRate = 120000, initialDelay = 60000)
    public void markOverdueBorrows() {
        LambdaQueryWrapper<BorrowRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BorrowRecord::getStatus, "borrowing")
                .lt(BorrowRecord::getDueDate, LocalDate.now());
        List<BorrowRecord> overdue = borrowRecordMapper.selectList(wrapper);

        if (!overdue.isEmpty()) {
            for (BorrowRecord record : overdue) {
                record.setStatus("overdue");
                borrowRecordMapper.updateById(record);

                readerService.checkAndSuspend(record.getReaderId());
                sendOverdueNotification(record);
            }
            log.info("定时任务: 已标记{}条逾期借阅记录", overdue.size());
        }
    }

    private void sendOverdueNotification(BorrowRecord record) {
        try {
            Reader reader = readerMapper.selectById(record.getReaderId());
            if (reader == null) return;

            ReaderAccount account = readerAccountMapper.selectOne(
                new LambdaQueryWrapper<ReaderAccount>().eq(ReaderAccount::getReaderId, reader.getId()));
            if (account != null && account.getEmail() != null && !account.getEmail().isEmpty()) {
                emailService.sendOverdueReminder(account.getEmail(), reader.getName(),
                    record.getBookTitle(), record.getDueDate());
            }
        } catch (Exception e) {
            log.warn("逾期提醒发送失败: recordId={}, {}", record.getId(), e.getMessage());
        }
    }

    @Scheduled(fixedRate = 86400000, initialDelay = 300000)
    public void sendDueReminders() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        LambdaQueryWrapper<BorrowRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BorrowRecord::getStatus, "borrowing")
                .eq(BorrowRecord::getDueDate, tomorrow);
        List<BorrowRecord> dueSoon = borrowRecordMapper.selectList(wrapper);

        for (BorrowRecord record : dueSoon) {
            try {
                Reader reader = readerMapper.selectById(record.getReaderId());
                if (reader == null) continue;

                ReaderAccount account = readerAccountMapper.selectOne(
                    new LambdaQueryWrapper<ReaderAccount>().eq(ReaderAccount::getReaderId, reader.getId()));
                if (account != null && account.getEmail() != null && !account.getEmail().isEmpty()) {
                    emailService.sendDueReminderNotification(account.getEmail(), reader.getName(),
                        record.getBookTitle(), record.getDueDate());
                }
            } catch (Exception e) {
                log.warn("到期提醒发送失败: recordId={}, {}", record.getId(), e.getMessage());
            }
        }
        if (!dueSoon.isEmpty()) {
            log.info("定时任务: 已发送{}条到期提醒", dueSoon.size());
        }
    }

    @Transactional
    @Scheduled(fixedRate = 600000, initialDelay = 120000)
    public void restoreReaderPermissions() {
        readerService.checkAndRestorePermissions();
    }

    @Scheduled(cron = "0 0 2 * * ?")
    public void auditPermissions() {
        log.info("=== 权限审计开始 ===");
        Map<String, Set<Permission>> allMappings = RolePermissions.getAllMappings();
        for (Map.Entry<String, Set<Permission>> entry : allMappings.entrySet()) {
            log.info("角色 [{}] 权限数量: {}", entry.getKey(), entry.getValue().size());
        }

        Set<Permission> adminOnlyPermissions = EnumSet.of(
            Permission.BOOK_DELETE, Permission.READER_DELETE,
            Permission.CATEGORY_DELETE, Permission.DASHBOARD_READ,
            Permission.AUDIT_LOG_READ, Permission.ADMIN_APPLICATION_REVIEW,
            Permission.BOOK_CREATE, Permission.BOOK_UPDATE,
            Permission.READER_CREATE, Permission.READER_UPDATE,
            Permission.BORROW_CREATE, Permission.BORROW_UPDATE,
            Permission.FILE_CREATE, Permission.FILE_DELETE
        );

        Set<Permission> readerPerms = RolePermissions.getPermissions("READER");
        boolean violation = false;
        for (Permission p : adminOnlyPermissions) {
            if (readerPerms.contains(p)) {
                log.error("权限审计异常: READER角色拥有管理员专属权限 [{}]，违反最小权限原则!", p);
                violation = true;
            }
        }
        if (!violation) {
            log.info("权限审计通过: READER角色符合最小权限原则");
        }
        log.info("=== 权限审计完成 ===");
    }
}
