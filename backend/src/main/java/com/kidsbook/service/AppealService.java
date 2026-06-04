package com.kidsbook.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kidsbook.common.BusinessException;
import com.kidsbook.entity.Appeal;
import com.kidsbook.entity.Reader;
import com.kidsbook.mapper.AppealMapper;
import com.kidsbook.mapper.ReaderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppealService {
    private final AppealMapper appealMapper;
    private final ReaderMapper readerMapper;
    private final AuditLogService auditLogService;

    @Transactional
    public Appeal submitAppeal(Long readerId, String type, String reason, String evidence) {
        Reader reader = readerMapper.selectById(readerId);
        if (reader == null) {
            throw new BusinessException(404, "读者信息不存在");
        }
        if ("suspension".equals(type) && !"suspended".equals(reader.getStatus())) {
            throw new BusinessException(400, "当前未被暂停，无需申诉");
        }

        LambdaQueryWrapper<Appeal> pendingCheck = new LambdaQueryWrapper<>();
        pendingCheck.eq(Appeal::getReaderId, readerId)
                    .eq(Appeal::getType, type)
                    .eq(Appeal::getStatus, "pending");
        Long pendingCount = appealMapper.selectCount(pendingCheck);
        if (pendingCount > 0) {
            throw new BusinessException(400, "您已有待处理的同类型申诉，请等待审核结果");
        }

        Appeal appeal = new Appeal();
        appeal.setReaderId(readerId);
        appeal.setReaderName(reader.getName());
        appeal.setType(type);
        appeal.setReason(reason);
        appeal.setEvidence(evidence);
        appeal.setStatus("pending");
        appeal.setCreateTime(LocalDateTime.now());
        appeal.setUpdateTime(LocalDateTime.now());
        appealMapper.insert(appeal);

        auditLogService.log("APPEAL_SUBMIT", "reader", readerId,
            "读者[" + reader.getName() + "]提交" + type + "申诉: " + reason);
        return appeal;
    }

    public IPage<Appeal> getMyAppeals(Long readerId, int page, int size) {
        LambdaQueryWrapper<Appeal> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Appeal::getReaderId, readerId)
               .orderByDesc(Appeal::getCreateTime);
        return appealMapper.selectPage(new Page<>(page, size), wrapper);
    }

    public IPage<Appeal> getAllAppeals(int page, int size, String status, String keyword) {
        LambdaQueryWrapper<Appeal> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isEmpty()) {
            wrapper.eq(Appeal::getStatus, status);
        }
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(Appeal::getReaderName, keyword)
                             .or().like(Appeal::getReason, keyword));
        }
        wrapper.orderByDesc(Appeal::getCreateTime);
        return appealMapper.selectPage(new Page<>(page, size), wrapper);
    }

    public Appeal getAppealById(Long id) {
        Appeal appeal = appealMapper.selectById(id);
        if (appeal == null) {
            throw new BusinessException(404, "申诉记录不存在");
        }
        return appeal;
    }

    @Transactional
    public void reviewAppeal(Long appealId, String action, String feedback) {
        Appeal appeal = appealMapper.selectById(appealId);
        if (appeal == null) {
            throw new BusinessException(404, "申诉记录不存在");
        }
        if (!"pending".equals(appeal.getStatus())) {
            throw new BusinessException(400, "该申诉已处理");
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String operator = auth != null ? auth.getName() : "system";

        if ("approve".equals(action)) {
            appeal.setStatus("approved");
            if ("suspension".equals(appeal.getType())) {
                Reader reader = readerMapper.selectById(appeal.getReaderId());
                if (reader != null && "suspended".equals(reader.getStatus())) {
                    reader.setStatus("normal");
                    reader.setSuspendReason(null);
                    readerMapper.updateById(reader);
                }
            }
        } else if ("reject".equals(action)) {
            appeal.setStatus("rejected");
        } else {
            throw new BusinessException(400, "无效的操作类型，应为 approve 或 reject");
        }

        appeal.setAdminFeedback(feedback);
        appeal.setReviewedBy(operator);
        appeal.setReviewedTime(LocalDateTime.now());
        appeal.setUpdateTime(LocalDateTime.now());
        appealMapper.updateById(appeal);

        auditLogService.log("APPEAL_REVIEW", "appeal", appealId,
            operator + " " + action + "了读者[" + appeal.getReaderName() + "]的申诉");
    }
}
