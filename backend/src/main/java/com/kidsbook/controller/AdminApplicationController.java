package com.kidsbook.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kidsbook.annotation.RequirePermission;
import com.kidsbook.common.Result;
import com.kidsbook.entity.Admin;
import com.kidsbook.entity.AdminApplication;
import com.kidsbook.entity.Reader;
import com.kidsbook.entity.ReaderAccount;
import com.kidsbook.mapper.AdminApplicationMapper;
import com.kidsbook.mapper.AdminMapper;
import com.kidsbook.mapper.ReaderAccountMapper;
import com.kidsbook.mapper.ReaderMapper;
import com.kidsbook.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin-application")
@RequiredArgsConstructor
public class AdminApplicationController {
    private final AdminApplicationMapper applicationMapper;
    private final AdminMapper adminMapper;
    private final ReaderMapper readerMapper;
    private final ReaderAccountMapper readerAccountMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @PostMapping("/apply")
    public Result<Void> apply(@RequestBody Map<String, String> request) {
        Long readerId = getCurrentReaderId();
        String reason = request.get("reason");

        if (reason == null || reason.trim().isEmpty()) {
            throw new RuntimeException("请填写申请理由");
        }

        LambdaQueryWrapper<AdminApplication> existWrapper = new LambdaQueryWrapper<>();
        existWrapper.eq(AdminApplication::getReaderId, readerId)
                .eq(AdminApplication::getStatus, "pending");
        if (applicationMapper.selectCount(existWrapper) > 0) {
            throw new RuntimeException("您已有待审批的申请，请勿重复提交");
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
    @RequirePermission("admin-app:view")
    public Result<Map<String, Object>> getApplicationList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {
        LambdaQueryWrapper<AdminApplication> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isEmpty()) {
            wrapper.eq(AdminApplication::getStatus, status);
        }
        wrapper.orderByDesc(AdminApplication::getCreateTime);
        IPage<AdminApplication> result = applicationMapper.selectPage(new Page<>(page, size), wrapper);

        Map<String, Object> data = new HashMap<>();
        data.put("records", result.getRecords());
        data.put("total", result.getTotal());

        LambdaQueryWrapper<AdminApplication> pendingWrapper = new LambdaQueryWrapper<>();
        pendingWrapper.eq(AdminApplication::getStatus, "pending");
        data.put("pendingCount", applicationMapper.selectCount(pendingWrapper));

        return Result.success(data);
    }

    @PutMapping("/{id}/approve")
    @RequirePermission("admin-app:approve")
    public Result<Void> approve(@PathVariable Long id) {
        AdminApplication application = applicationMapper.selectById(id);
        if (application == null) {
            throw new RuntimeException("申请不存在");
        }
        if (!"pending".equals(application.getStatus())) {
            throw new RuntimeException("该申请已处理");
        }

        ReaderAccount account = readerAccountMapper.selectOne(
                new LambdaQueryWrapper<ReaderAccount>().eq(ReaderAccount::getReaderId, application.getReaderId()));
        if (account == null) {
            throw new RuntimeException("读者账号不存在");
        }

        LambdaQueryWrapper<Admin> existAdmin = new LambdaQueryWrapper<>();
        existAdmin.eq(Admin::getUsername, account.getUsername());
        if (adminMapper.selectCount(existAdmin) > 0) {
            throw new RuntimeException("该用户名已存在管理员账号");
        }

        Admin admin = new Admin();
        admin.setUsername(account.getUsername());
        admin.setPassword(account.getPassword());
        admin.setNickname(application.getReaderName());
        admin.setCreateTime(LocalDateTime.now());
        admin.setUpdateTime(LocalDateTime.now());
        adminMapper.insert(admin);

        application.setStatus("approved");
        application.setApprovedTime(LocalDateTime.now());
        application.setUpdateTime(LocalDateTime.now());
        applicationMapper.updateById(application);

        return Result.success(null);
    }

    @PutMapping("/{id}/reject")
    @RequirePermission("admin-app:reject")
    public Result<Void> reject(@PathVariable Long id, @RequestBody Map<String, String> request) {
        AdminApplication application = applicationMapper.selectById(id);
        if (application == null) {
            throw new RuntimeException("申请不存在");
        }
        if (!"pending".equals(application.getStatus())) {
            throw new RuntimeException("该申请已处理");
        }

        application.setStatus("rejected");
        application.setRejectReason(request.get("rejectReason"));
        application.setApprovedTime(LocalDateTime.now());
        application.setUpdateTime(LocalDateTime.now());
        applicationMapper.updateById(application);

        return Result.success(null);
    }

    private Long getCurrentReaderId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String token = (String) auth.getCredentials();
        Long readerId = jwtUtil.getReaderIdFromToken(token);
        if (readerId == null) {
            throw new RuntimeException("无法获取读者信息");
        }
        return readerId;
    }
}
