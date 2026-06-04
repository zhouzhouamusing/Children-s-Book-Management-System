package com.kidsbook.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kidsbook.dto.LoginRequest;
import com.kidsbook.dto.LoginResponse;
import com.kidsbook.dto.ReaderRegisterRequest;
import com.kidsbook.entity.Reader;
import com.kidsbook.entity.ReaderAccount;
import com.kidsbook.common.BusinessException;
import com.kidsbook.mapper.ReaderAccountMapper;
import com.kidsbook.mapper.ReaderMapper;
import com.kidsbook.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReaderAccountService extends ServiceImpl<ReaderAccountMapper, ReaderAccount> {
    private final ReaderAccountMapper readerAccountMapper;
    private final ReaderMapper readerMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuditLogService auditLogService;
    private final EmailService emailService;
    private final RbacService rbacService;

    private static final int MAX_RESET_ATTEMPTS = 5;
    private static final long RESET_WINDOW_MS = 15 * 60 * 1000;
    private final ConcurrentHashMap<String, long[]> resetAttempts = new ConcurrentHashMap<>();

    public LoginResponse login(LoginRequest request) {
        ReaderAccount account = readerAccountMapper.selectOne(
            new LambdaQueryWrapper<ReaderAccount>().eq(ReaderAccount::getUsername, request.getUsername())
        );

        if (account == null) {
            throw new BusinessException(401, "用户名或密码错误");
        }

        if (!"active".equals(account.getStatus())) {
            throw new BusinessException(401, "账号已被禁用，请联系管理员");
        }

        if (!passwordEncoder.matches(request.getPassword(), account.getPassword())) {
            throw new BusinessException(401, "用户名或密码错误");
        }

        Reader reader = readerMapper.selectById(account.getReaderId());
        if (reader == null) {
            throw new BusinessException(404, "关联读者信息不存在");
        }

        boolean isSuspended = "suspended".equals(reader.getStatus());

        List<String> roles = rbacService.getRolesForUser("READER", account.getId());
        if (roles.isEmpty()) {
            roles = List.of("READER");
        }
        Set<String> permSet = rbacService.getEffectivePermissions("READER", account.getId());
        List<String> permissions = new ArrayList<>(permSet);

        String token = jwtUtil.generateToken(account.getUsername(), roles, permissions, reader.getId());

        auditLogService.logAsync(account.getUsername(), "READER", "READER_LOGIN", "reader", reader.getId(), "读者登录", null);

        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setNickname(reader.getName());
        response.setRole("READER");
        response.setRoles(roles);
        response.setPermissions(permissions);
        response.setReaderId(reader.getId());
        response.setSuspended(isSuspended);
        return response;
    }

    @Transactional
    public void register(ReaderRegisterRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException(400, "两次输入的密码不一致");
        }

        if (request.getPassword() == null || request.getPassword().length() < 8) {
            throw new BusinessException(400, "密码长度不能少于8位");
        }
        if (!request.getPassword().matches(".*[A-Za-z].*") || !request.getPassword().matches(".*\\d.*")) {
            throw new BusinessException(400, "密码必须包含字母和数字");
        }

        String username = request.getUsername().trim();
        String name = sanitize(request.getName().trim());

        ReaderAccount existing = readerAccountMapper.selectOne(
            new LambdaQueryWrapper<ReaderAccount>().eq(ReaderAccount::getUsername, username)
        );
        if (existing != null) {
            throw new BusinessException(400, "用户名已存在");
        }

        Reader reader = new Reader();
        reader.setName(name);
        reader.setParentPhone(request.getParentPhone().trim());
        reader.setParentName(request.getParentName() != null ? sanitize(request.getParentName().trim()) : null);
        reader.setAge(request.getAge());
        reader.setGender(request.getGender());
        reader.setStatus("normal");
        reader.setBorrowCount(0);
        reader.setOverdueCount(0);
        reader.setPoints(0);
        reader.setTotalReadingDays(0);
        reader.setLevel("初级小书虫");
        readerMapper.insert(reader);

        ReaderAccount account = new ReaderAccount();
        account.setUsername(username);
        account.setPassword(passwordEncoder.encode(request.getPassword()));
        account.setReaderId(reader.getId());
        account.setStatus("active");
        readerAccountMapper.insert(account);
        log.info("读者注册成功: username={}, readerId={}", username, reader.getId());
    }

    private String sanitize(String input) {
        if (input == null) return null;
        return input.replaceAll("[<>\"'&;]", "");
    }

    public void sendResetCode(String email) {
        if (email == null || email.isBlank()) {
            throw new BusinessException(400, "请输入邮箱地址");
        }
        ReaderAccount account = readerAccountMapper.selectOne(
            new LambdaQueryWrapper<ReaderAccount>().eq(ReaderAccount::getEmail, email));
        if (account == null) {
            throw new BusinessException(404, "该邮箱未关联任何读者账号");
        }
        emailService.sendVerificationCode(email);
    }

    public void resetPassword(String username, String email, String code,
                              String newPassword, String confirmPassword) {
        if (username == null || username.isBlank()) {
            throw new BusinessException(400, "请输入用户名");
        }
        if (email == null || email.isBlank()) {
            throw new BusinessException(400, "请输入邮箱");
        }

        checkResetRateLimit(username);

        if (newPassword == null || newPassword.length() < 8) {
            throw new BusinessException(400, "密码长度不能少于8位");
        }
        if (!newPassword.matches(".*[A-Za-z].*") || !newPassword.matches(".*\\d.*")) {
            throw new BusinessException(400, "密码必须包含字母和数字");
        }
        if (!newPassword.equals(confirmPassword)) {
            throw new BusinessException(400, "两次输入的密码不一致");
        }

        ReaderAccount account = readerAccountMapper.selectOne(
            new LambdaQueryWrapper<ReaderAccount>().eq(ReaderAccount::getUsername, username));
        if (account == null) {
            throw new BusinessException(404, "用户名不存在");
        }
        if (!"active".equals(account.getStatus())) {
            throw new BusinessException(400, "该账号已被禁用，无法重置密码");
        }
        if (!email.equals(account.getEmail())) {
            throw new BusinessException(400, "邮箱与账号不匹配");
        }

        if (!emailService.verifyCode(email, code)) {
            throw new BusinessException(400, "验证码无效或已过期");
        }

        account.setPassword(passwordEncoder.encode(newPassword));
        readerAccountMapper.updateById(account);
        resetAttempts.remove(username);
        log.info("读者密码重置成功: username={}", username);
    }

    private void checkResetRateLimit(String username) {
        long now = System.currentTimeMillis();
        resetAttempts.compute(username, (key, attempts) -> {
            if (attempts == null) {
                return new long[]{1, now};
            }
            if (now - attempts[1] > RESET_WINDOW_MS) {
                return new long[]{1, now};
            }
            attempts[0]++;
            return attempts;
        });
        long[] current = resetAttempts.get(username);
        if (current != null && current[0] > MAX_RESET_ATTEMPTS) {
            throw new BusinessException(429, "密码重置尝试过于频繁，请15分钟后再试");
        }
    }
}
