package com.kidsbook.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kidsbook.common.BusinessException;
import com.kidsbook.dto.ReaderRequest;
import com.kidsbook.entity.BorrowRecord;
import com.kidsbook.entity.Reader;
import com.kidsbook.entity.ReaderAccount;
import com.kidsbook.mapper.BookReservationMapper;
import com.kidsbook.mapper.BorrowRecordMapper;
import com.kidsbook.mapper.ReaderAccountMapper;
import com.kidsbook.mapper.ReaderMapper;
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
public class ReaderService extends ServiceImpl<ReaderMapper, Reader> {

    private final BorrowRecordMapper borrowRecordMapper;
    private final ReaderAccountMapper readerAccountMapper;
    private final BookReservationMapper bookReservationMapper;
    private final EmailService emailService;

    public Page<Reader> listReaders(int page, int size, String keyword, String status, String gender) {
        Page<Reader> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Reader> wrapper = new LambdaQueryWrapper<>();

        if (StrUtil.isNotBlank(keyword)) {
            wrapper.and(w -> w
                    .like(Reader::getName, keyword)
                    .or()
                    .like(Reader::getParentPhone, keyword)
                    .or()
                    .like(Reader::getParentName, keyword)
            );
        }

        if (StrUtil.isNotBlank(status)) {
            wrapper.eq(Reader::getStatus, status);
        }

        if (StrUtil.isNotBlank(gender)) {
            wrapper.eq(Reader::getGender, gender);
        }

        wrapper.orderByDesc(Reader::getCreateTime);
        return this.page(pageParam, wrapper);
    }

    public void addReader(ReaderRequest request) {
        Reader reader = new Reader();
        BeanUtil.copyProperties(request, reader);
        reader.setStatus("normal");
        reader.setBorrowCount(0);
        reader.setOverdueCount(0);
        this.save(reader);
    }

    public void updateReader(Long id, ReaderRequest request) {
        Reader reader = this.getById(id);
        if (reader == null) {
            throw new BusinessException(404, "读者不存在");
        }
        BeanUtil.copyProperties(request, reader, "id", "status", "borrowCount", "overdueCount");
        this.updateById(reader);
    }

    @Transactional
    public void deleteReader(Long id) {
        Reader reader = this.getById(id);
        if (reader == null) {
            throw new BusinessException(404, "读者不存在");
        }
        LambdaQueryWrapper<BorrowRecord> activeCheck = new LambdaQueryWrapper<>();
        activeCheck.eq(BorrowRecord::getReaderId, id)
                .in(BorrowRecord::getStatus, "borrowing", "overdue");
        if (borrowRecordMapper.selectCount(activeCheck) > 0) {
            throw new BusinessException(400, "该读者有未归还的借阅记录，不能删除");
        }
        if (bookReservationMapper.countActiveByReaderId(id) > 0) {
            throw new BusinessException(400, "该读者有待处理的预约记录，不能删除");
        }
        readerAccountMapper.delete(
            new LambdaQueryWrapper<ReaderAccount>().eq(ReaderAccount::getReaderId, id));
        this.removeById(id);
    }

    @Transactional
    public void updateStatus(Long id, String status) {
        Reader reader = this.getById(id);
        if (reader == null) {
            throw new BusinessException(404, "读者不存在");
        }
        reader.setStatus(status);
        if ("suspended".equals(status)) {
            reader.setSuspendReason("manual");
        } else if ("normal".equals(status)) {
            reader.setSuspendReason(null);
        }
        this.updateById(reader);
    }

    public Page<BorrowRecord> getBorrowRecords(Long readerId, int page, int size, String status) {
        Page<BorrowRecord> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<BorrowRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BorrowRecord::getReaderId, readerId);

        if (StrUtil.isNotBlank(status) && !"all".equals(status)) {
            wrapper.eq(BorrowRecord::getStatus, status);
        }

        wrapper.orderByDesc(BorrowRecord::getBorrowDate);
        return borrowRecordMapper.selectPage(pageParam, wrapper);
    }

    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", this.count());
        stats.put("active", baseMapper.countActive());
        stats.put("suspended", baseMapper.countSuspended());
        stats.put("overdue", baseMapper.countWithOverdue());
        return stats;
    }

    @Transactional
    public void checkAndSuspend(Long readerId) {
        Reader reader = this.getById(readerId);
        if (reader != null && reader.getOverdueCount() >= 3 && "normal".equals(reader.getStatus())) {
            reader.setStatus("suspended");
            reader.setSuspendReason("overdue");
            this.updateById(reader);

            try {
                ReaderAccount account = readerAccountMapper.selectOne(
                    new LambdaQueryWrapper<ReaderAccount>().eq(ReaderAccount::getReaderId, readerId));
                if (account != null && account.getEmail() != null && !account.getEmail().isEmpty()) {
                    emailService.sendSuspensionNotification(account.getEmail(), reader.getName(), "overdue");
                }
            } catch (Exception e) {
                // email failure should not break suspension logic
            }
        }
    }

    @Transactional
    public void checkAndRestorePermissions() {
        LambdaQueryWrapper<Reader> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Reader::getStatus, "suspended")
               .eq(Reader::getSuspendReason, "overdue");
        List<Reader> suspendedReaders = this.list(wrapper);

        int restored = 0;
        for (Reader reader : suspendedReaders) {
            LambdaQueryWrapper<BorrowRecord> overdueCheck = new LambdaQueryWrapper<>();
            overdueCheck.eq(BorrowRecord::getReaderId, reader.getId())
                        .eq(BorrowRecord::getStatus, "overdue");
            if (borrowRecordMapper.selectCount(overdueCheck) == 0) {
                reader.setStatus("normal");
                reader.setSuspendReason(null);
                this.updateById(reader);
                restored++;
            }
        }
        if (restored > 0) {
            log.info("定时任务: 已恢复{}名读者的借阅权限", restored);
        }
    }
}
