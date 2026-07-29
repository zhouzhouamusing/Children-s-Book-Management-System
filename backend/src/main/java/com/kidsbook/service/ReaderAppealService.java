package com.kidsbook.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kidsbook.dto.AppealHandleRequest;
import com.kidsbook.dto.AppealRequest;
import com.kidsbook.entity.ReaderAppeal;
import com.kidsbook.mapper.ReaderAppealMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReaderAppealService {

    private final ReaderAppealMapper appealMapper;

    private static final List<String> VALID_TYPES = Arrays.asList(
            "borrow_dispute", "account_suspended", "review_rejected", "other");

    private static final List<String> VALID_HANDLE_STATUS = Arrays.asList("resolved", "rejected");

    public void submitAppeal(Long readerId, String readerName, AppealRequest request) {
        if (!VALID_TYPES.contains(request.getType())) {
            throw new RuntimeException("无效的申诉类型");
        }

        Long pendingCount = appealMapper.selectCount(
                new LambdaQueryWrapper<ReaderAppeal>()
                        .eq(ReaderAppeal::getReaderId, readerId)
                        .eq(ReaderAppeal::getStatus, "pending"));
        if (pendingCount >= 5) {
            throw new RuntimeException("您有过多未处理的申诉，请等待处理后再提交");
        }

        if (request.getRelatedId() != null) {
            Long duplicateCount = appealMapper.selectCount(
                    new LambdaQueryWrapper<ReaderAppeal>()
                            .eq(ReaderAppeal::getReaderId, readerId)
                            .eq(ReaderAppeal::getType, request.getType())
                            .eq(ReaderAppeal::getRelatedId, request.getRelatedId())
                            .in(ReaderAppeal::getStatus, "pending", "processing"));
            if (duplicateCount > 0) {
                throw new RuntimeException("该记录已有进行中的申诉，请勿重复提交");
            }
        }

        ReaderAppeal appeal = new ReaderAppeal();
        appeal.setReaderId(readerId);
        appeal.setReaderName(readerName);
        appeal.setType(request.getType());
        appeal.setRelatedId(request.getRelatedId());
        appeal.setTitle(request.getTitle());
        appeal.setContent(request.getContent());
        appeal.setEvidenceUrls(request.getEvidenceUrls());
        appeal.setStatus("pending");
        appealMapper.insert(appeal);
    }

    public Page<ReaderAppeal> getMyAppeals(Long readerId, int page, int size) {
        return appealMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<ReaderAppeal>()
                        .eq(ReaderAppeal::getReaderId, readerId)
                        .orderByDesc(ReaderAppeal::getCreateTime));
    }

    public ReaderAppeal getAppealDetail(Long id) {
        ReaderAppeal appeal = appealMapper.selectById(id);
        if (appeal == null) {
            throw new RuntimeException("申诉记录不存在");
        }
        return appeal;
    }

    public Page<ReaderAppeal> listAppeals(int page, int size, String status, String type) {
        LambdaQueryWrapper<ReaderAppeal> wrapper = new LambdaQueryWrapper<ReaderAppeal>()
                .orderByDesc(ReaderAppeal::getCreateTime);
        if (status != null && !status.isEmpty()) {
            wrapper.eq(ReaderAppeal::getStatus, status);
        }
        if (type != null && !type.isEmpty()) {
            wrapper.eq(ReaderAppeal::getType, type);
        }
        return appealMapper.selectPage(new Page<>(page, size), wrapper);
    }

    public void handleAppeal(Long id, Long adminId, AppealHandleRequest request) {
        if (!VALID_HANDLE_STATUS.contains(request.getStatus())) {
            throw new RuntimeException("处理状态无效，只能为resolved或rejected");
        }

        ReaderAppeal appeal = appealMapper.selectById(id);
        if (appeal == null) {
            throw new RuntimeException("申诉记录不存在");
        }
        if (!"pending".equals(appeal.getStatus()) && !"processing".equals(appeal.getStatus())) {
            throw new RuntimeException("该申诉已处理，无法重复操作");
        }

        appeal.setStatus(request.getStatus());
        appeal.setAdminId(adminId);
        appeal.setAdminReply(request.getAdminReply());
        appeal.setResolveTime(LocalDateTime.now());
        appealMapper.updateById(appeal);
    }
}
