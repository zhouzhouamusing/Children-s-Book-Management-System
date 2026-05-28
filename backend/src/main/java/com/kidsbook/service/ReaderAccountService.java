package com.kidsbook.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kidsbook.dto.LoginRequest;
import com.kidsbook.dto.LoginResponse;
import com.kidsbook.dto.ReaderRegisterRequest;
import com.kidsbook.entity.Reader;
import com.kidsbook.entity.ReaderAccount;
import com.kidsbook.mapper.ReaderAccountMapper;
import com.kidsbook.mapper.ReaderMapper;
import com.kidsbook.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReaderAccountService {
    private final ReaderAccountMapper readerAccountMapper;
    private final ReaderMapper readerMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

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

        String token = jwtUtil.generateToken(account.getUsername(), "READER", reader.getId());

        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setNickname(reader.getName());
        response.setRole("READER");
        response.setReaderId(reader.getId());
        return response;
    }

    public void register(ReaderRegisterRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("两次输入的密码不一致");
        }

        ReaderAccount existing = readerAccountMapper.selectOne(
            new LambdaQueryWrapper<ReaderAccount>().eq(ReaderAccount::getUsername, request.getUsername())
        );
        if (existing != null) {
            throw new RuntimeException("用户名已存在");
        }

        Reader reader = readerMapper.selectOne(
            new LambdaQueryWrapper<Reader>()
                .eq(Reader::getParentPhone, request.getParentPhone())
                .eq(Reader::getName, request.getName())
        );
        if (reader == null) {
            throw new RuntimeException("未找到匹配的读者信息，请确认姓名和家长手机号是否正确");
        }

        ReaderAccount existingLink = readerAccountMapper.selectOne(
            new LambdaQueryWrapper<ReaderAccount>().eq(ReaderAccount::getReaderId, reader.getId())
        );
        if (existingLink != null) {
            throw new RuntimeException("该读者已有关联账号");
        }

        ReaderAccount account = new ReaderAccount();
        account.setUsername(request.getUsername());
        account.setPassword(passwordEncoder.encode(request.getPassword()));
        account.setReaderId(reader.getId());
        account.setStatus("active");
        readerAccountMapper.insert(account);
        log.info("读者注册成功: username={}, readerId={}", request.getUsername(), reader.getId());
    }
}
