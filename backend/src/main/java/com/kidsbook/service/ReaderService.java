package com.kidsbook.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kidsbook.dto.ReaderRequest;
import com.kidsbook.entity.BorrowRecord;
import com.kidsbook.entity.Reader;
import com.kidsbook.mapper.BorrowRecordMapper;
import com.kidsbook.mapper.ReaderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReaderService extends ServiceImpl<ReaderMapper, Reader> {

    private final BorrowRecordMapper borrowRecordMapper;

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
            throw new RuntimeException("读者不存在");
        }
        BeanUtil.copyProperties(request, reader, "id", "status", "borrowCount", "overdueCount");
        this.updateById(reader);
    }

    public void deleteReader(Long id) {
        Reader reader = this.getById(id);
        if (reader == null) {
            throw new RuntimeException("读者不存在");
        }
        this.removeById(id);
    }

    public void updateStatus(Long id, String status) {
        Reader reader = this.getById(id);
        if (reader == null) {
            throw new RuntimeException("读者不存在");
        }
        reader.setStatus(status);
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

    public void checkAndSuspend(Long readerId) {
        Reader reader = this.getById(readerId);
        if (reader != null && reader.getOverdueCount() >= 3 && "normal".equals(reader.getStatus())) {
            reader.setStatus("suspended");
            this.updateById(reader);
        }
    }
}
