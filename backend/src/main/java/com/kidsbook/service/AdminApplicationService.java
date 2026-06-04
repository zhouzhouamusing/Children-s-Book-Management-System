package com.kidsbook.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kidsbook.common.BusinessException;
import com.kidsbook.entity.Admin;
import com.kidsbook.entity.AdminApplication;
import com.kidsbook.entity.Reader;
import com.kidsbook.entity.ReaderAccount;
import com.kidsbook.mapper.AdminApplicationMapper;
import com.kidsbook.mapper.AdminMapper;
import com.kidsbook.mapper.ReaderAccountMapper;
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
public class AdminApplicationService {
    private final AdminApplicationMapper applicationMapper;
    private final AdminMapper adminMapper;
    private final ReaderMapper readerMapper;
    private final ReaderAccountMapper readerAccountMapper;
    private final AuditLogService auditLogService;

    @Transactional
    public void approve(Long applicationId) {
        AdminApplication application = applicationMapper.selectById(applicationId);
        if (application == null) {
            throw new BusinessException(404, "申请不存在");
        }
        if (!"pending".equals(application.getStatus())) {
            throw new BusinessException(400, "该申请已处理");
        }

        ReaderAccount account = readerAccountMapper.selectOne(
                new LambdaQueryWrapper<ReaderAccount>().eq(ReaderAccount::getReaderId, application.getReaderId()));
        if (account == null) {
            throw new BusinessException(404, "读者账号不存在");
        }

        LambdaQueryWrapper<Admin> existAdmin = new LambdaQueryWrapper<>();
        existAdmin.eq(Admin::getUsername, account.getUsername());
        if (adminMapper.selectCount(existAdmin) > 0) {
            throw new BusinessException(400, "该用户名已存在管理员账号");
        }

        Admin admin = new Admin();
        admin.setUsername(account.getUsername());
        admin.setPassword(account.getPassword());
        admin.setNickname(application.getReaderName());
        admin.setCreateTime(LocalDateTime.now());
        admin.setUpdateTime(LocalDateTime.now());
        adminMapper.insert(admin);

        // Disable original reader account to prevent dual-login
        account.setStatus("disabled");
        readerAccountMapper.updateById(account);

        // Mark reader as promoted
        Reader reader = readerMapper.selectById(application.getReaderId());
        if (reader != null) {
            reader.setStatus("promoted");
            readerMapper.updateById(reader);
        }

        application.setStatus("approved");
        application.setApprovedBy(getCurrentUsername());
        application.setApprovedTime(LocalDateTime.now());
        application.setUpdateTime(LocalDateTime.now());
        applicationMapper.updateById(application);

        log.info("管理员申请已批准: readerId={}, username={}", application.getReaderId(), account.getUsername());
        auditLogService.log("APPROVE_ADMIN_APP", "admin_application", applicationId,
            "批准用户[" + account.getUsername() + "]的管理员申请");
    }

    @Transactional
    public void reject(Long applicationId, String rejectReason) {
        AdminApplication application = applicationMapper.selectById(applicationId);
        if (application == null) {
            throw new BusinessException(404, "申请不存在");
        }
        if (!"pending".equals(application.getStatus())) {
            throw new BusinessException(400, "该申请已处理");
        }

        application.setStatus("rejected");
        application.setRejectReason(rejectReason);
        application.setApprovedTime(LocalDateTime.now());
        application.setUpdateTime(LocalDateTime.now());
        applicationMapper.updateById(application);

        log.info("管理员申请已拒绝: applicationId={}", applicationId);
        auditLogService.log("REJECT_ADMIN_APP", "admin_application", applicationId,
            "拒绝管理员申请，原因: " + rejectReason);
    }

    private String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : null;
    }
}
