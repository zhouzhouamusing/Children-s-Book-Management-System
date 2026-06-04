package com.kidsbook.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kidsbook.common.BusinessException;
import com.kidsbook.common.PageResult;
import com.kidsbook.common.Permission;
import com.kidsbook.common.RequirePermission;
import com.kidsbook.common.Result;
import com.kidsbook.entity.AdminApplication;
import com.kidsbook.entity.Reader;
import com.kidsbook.entity.ReaderAccount;
import com.kidsbook.mapper.AdminApplicationMapper;
import com.kidsbook.mapper.ReaderAccountMapper;
import com.kidsbook.mapper.ReaderMapper;
import com.kidsbook.service.AdminApplicationService;
import com.kidsbook.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin-application")
@RequiredArgsConstructor
public class AdminApplicationController {
    private final AdminApplicationMapper applicationMapper;
    private final ReaderMapper readerMapper;
    private final ReaderAccountMapper readerAccountMapper;
    private final AdminApplicationService adminApplicationService;
    private final JwtUtil jwtUtil;

    @PostMapping("/apply")
    @RequirePermission(Permission.ADMIN_APPLICATION_APPLY)
    public Result<Void> apply(@RequestBody Map<String, String> request) {
        Long readerId = getCurrentReaderId();
        String reason = request.get("reason");

        if (reason == null || reason.trim().isEmpty()) {
            throw new BusinessException(400, "请填写申请理由");
        }

        LambdaQueryWrapper<AdminApplication> existWrapper = new LambdaQueryWrapper<>();
        existWrapper.eq(AdminApplication::getReaderId, readerId)
                .eq(AdminApplication::getStatus, "pending");
        if (applicationMapper.selectCount(existWrapper) > 0) {
            throw new BusinessException(400, "您已有待审批的申请，请勿重复提交");
        }

        Reader reader = readerMapper.selectById(readerId);
        ReaderAccount account = readerAccountMapper.selectOne(
                new LambdaQueryWrapper<ReaderAccount>().eq(ReaderAccount::getReaderId, readerId));

        AdminApplication application = new AdminApplication();
        application.setReaderId(readerId);
        application.setReaderName(reader != null ? reader.getName() : "未知");
        application.setUsername(account != null ? account.getUsername() : "未知");
        application.setReason(reason.trim());
        application.setStatus("pending");
        application.setCreateTime(LocalDateTime.now());
        application.setUpdateTime(LocalDateTime.now());
        applicationMapper.insert(application);

        return Result.success(null);
    }

    @GetMapping("/my-status")
    @RequirePermission(Permission.ADMIN_APPLICATION_STATUS)
    public Result<Map<String, Object>> getMyApplicationStatus() {
        Long readerId = null;
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String token = (String) auth.getCredentials();
            readerId = jwtUtil.getReaderIdFromToken(token);
        } catch (Exception e) {
            // token parsing failed
        }

        Map<String, Object> data = new HashMap<>();
        if (readerId == null) {
            data.put("hasApplication", false);
            return Result.success(data);
        }

        try {
            LambdaQueryWrapper<AdminApplication> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(AdminApplication::getReaderId, readerId)
                    .orderByDesc(AdminApplication::getCreateTime)
                    .last("LIMIT 1");
            AdminApplication latest = applicationMapper.selectOne(wrapper);

            if (latest != null) {
                data.put("hasApplication", true);
                data.put("status", latest.getStatus());
                data.put("reason", latest.getReason());
                data.put("rejectReason", latest.getRejectReason());
                data.put("createTime", latest.getCreateTime());
                data.put("approvedTime", latest.getApprovedTime());
            } else {
                data.put("hasApplication", false);
            }
        } catch (Exception e) {
            data.put("hasApplication", false);
        }
        return Result.success(data);
    }

    @GetMapping("/list")
    @RequirePermission(Permission.ADMIN_APPLICATION_REVIEW)
    public Result<PageResult<AdminApplication>> getApplicationList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {
        LambdaQueryWrapper<AdminApplication> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isEmpty()) {
            wrapper.eq(AdminApplication::getStatus, status);
        }
        wrapper.orderByDesc(AdminApplication::getCreateTime);
        IPage<AdminApplication> result = applicationMapper.selectPage(new Page<>(page, size), wrapper);

        LambdaQueryWrapper<AdminApplication> pendingWrapper = new LambdaQueryWrapper<>();
        pendingWrapper.eq(AdminApplication::getStatus, "pending");
        long pendingCount = applicationMapper.selectCount(pendingWrapper);

        return Result.success(PageResult.of(result).withExtra("pendingCount", pendingCount));
    }

    @PutMapping("/{id}/approve")
    @RequirePermission(Permission.ADMIN_APPLICATION_REVIEW)
    public Result<Void> approve(@PathVariable Long id) {
        adminApplicationService.approve(id);
        return Result.success(null);
    }

    @PutMapping("/{id}/reject")
    @RequirePermission(Permission.ADMIN_APPLICATION_REVIEW)
    public Result<Void> reject(@PathVariable Long id, @RequestBody Map<String, String> request) {
        adminApplicationService.reject(id, request.get("rejectReason"));
        return Result.success(null);
    }

    private Long getCurrentReaderId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getCredentials() == null) {
            throw new BusinessException(401, "无法获取读者信息，请重新登录");
        }
        try {
            String token = auth.getCredentials().toString();
            Long readerId = jwtUtil.getReaderIdFromToken(token);
            if (readerId == null) {
                throw new BusinessException(401, "无法获取读者信息");
            }
            return readerId;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(401, "无法获取读者信息，请重新登录");
        }
    }
}
