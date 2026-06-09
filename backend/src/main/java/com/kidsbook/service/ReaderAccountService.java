package com.kidsbook.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kidsbook.dto.LoginRequest;
import com.kidsbook.dto.LoginResponse;
import com.kidsbook.dto.ReaderRegisterRequest;
import com.kidsbook.entity.Reader;
import com.kidsbook.entity.ReaderAccount;
import com.kidsbook.entity.SysRole;
import com.kidsbook.entity.SysUserRole;
import com.kidsbook.mapper.ReaderAccountMapper;
import com.kidsbook.mapper.ReaderMapper;
import com.kidsbook.mapper.SysRoleMapper;
import com.kidsbook.mapper.SysUserRoleMapper;
import com.kidsbook.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReaderAccountService {
    private final ReaderAccountMapper readerAccountMapper;
    private final ReaderMapper readerMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final PermissionCacheService permissionCacheService;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;

    public LoginResponse login(LoginRequest request) {
        ReaderAccount account = readerAccountMapper.selectOne(
            new LambdaQueryWrapper<ReaderAccount>().eq(ReaderAccount::getUsername, request.getUsername())
        );

        if (account == null) {
            throw new RuntimeException("用户名或密码错误");
        }

        if (!"active".equals(account.getStatus())) {
            throw new RuntimeException("账号已被禁用，请联系管理员");
        }

        if (!passwordEncoder.matches(request.getPassword(), account.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }

        Reader reader = readerMapper.selectById(account.getReaderId());
        if (reader == null) {
            throw new RuntimeException("关联读者信息不存在");
        }

        if ("suspended".equals(reader.getStatus())) {
            throw new RuntimeException("您的借阅权限已被暂停，请联系管理员");
        }

        String token = jwtUtil.generateToken(account.getUsername(), "READER", reader.getId(), "reader", reader.getId());

        java.util.List<String> roleCodes = permissionCacheService.getRoleCodes("reader", reader.getId());
        java.util.List<String> permissions = permissionCacheService.getPermissions("reader", reader.getId());

        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setNickname(reader.getName());
        response.setRole("READER");
        response.setReaderId(reader.getId());
        response.setRoles(roleCodes);
        response.setPermissions(permissions);
        return response;
    }

    @Transactional
    public void register(ReaderRegisterRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("两次输入的密码不一致");
        }

        String username = request.getUsername().trim();
        String name = sanitize(request.getName().trim());

        ReaderAccount existing = readerAccountMapper.selectOne(
            new LambdaQueryWrapper<ReaderAccount>().eq(ReaderAccount::getUsername, username)
        );
        if (existing != null) {
            throw new RuntimeException("用户名已存在");
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

        // 自动分配读者角色
        SysRole readerRole = roleMapper.selectOne(
            new LambdaQueryWrapper<SysRole>().eq(SysRole::getCode, "reader"));
        if (readerRole != null) {
            SysUserRole ur = new SysUserRole();
            ur.setUserType("reader");
            ur.setUserId(reader.getId());
            ur.setRoleId(readerRole.getId());
            userRoleMapper.insert(ur);
        }

        log.info("读者注册成功: username={}, readerId={}", username, reader.getId());
    }

    private String sanitize(String input) {
        if (input == null) return null;
        return input.replaceAll("[<>\"'&;]", "");
    }
}
