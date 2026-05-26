package com.kidsbook.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kidsbook.entity.Admin;
import com.kidsbook.mapper.AdminMapper;
import com.kidsbook.dto.LoginRequest;
import com.kidsbook.dto.LoginResponse;
import com.kidsbook.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {
    private final AdminMapper adminMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public LoginResponse login(LoginRequest request) {
        Admin admin = adminMapper.selectOne(
            new LambdaQueryWrapper<Admin>().eq(Admin::getUsername, request.getUsername())
        );

        if (admin == null) {
            log.warn("用户不存在: {}", request.getUsername());
            throw new RuntimeException("用户名或密码错误");
        }

        log.info("查询到用户: id={}, username={}, password长度={}", admin.getId(), admin.getUsername(),
                admin.getPassword() != null ? admin.getPassword().length() : 0);

        boolean passwordMatch = passwordEncoder.matches(request.getPassword(), admin.getPassword());
        log.info("密码匹配结果: {}", passwordMatch);

        if (!passwordMatch) {
            throw new RuntimeException("用户名或密码错误");
        }

        String token = jwtUtil.generateToken(admin.getUsername());
        log.info("JWT Token已生成, 长度: {}", token.length());

        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setNickname(admin.getNickname());
        response.setAvatar(admin.getAvatar());
        return response;
    }
}
