package com.kidsbook.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kidsbook.entity.Admin;
import com.kidsbook.mapper.AdminMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {
    private final AdminMapper adminMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        try {
            String encodedPassword = passwordEncoder.encode("admin123");
            log.info("生成的BCrypt密码哈希: {}", encodedPassword);

            Admin admin = adminMapper.selectOne(
                new LambdaQueryWrapper<Admin>().eq(Admin::getUsername, "admin")
            );
            if (admin == null) {
                admin = new Admin();
                admin.setUsername("admin");
                admin.setPassword(encodedPassword);
                admin.setNickname("超级管理员");
                adminMapper.insert(admin);
                log.info("=== 初始管理员账号已创建 === 用户名: admin, 密码: admin123");
            } else {
                admin.setPassword(encodedPassword);
                adminMapper.updateById(admin);
                log.info("=== 管理员密码已更新 === 用户名: admin, 密码: admin123");
            }

            boolean matches = passwordEncoder.matches("admin123", encodedPassword);
            log.info("密码验证测试: {}", matches ? "通过" : "失败");
        } catch (Exception e) {
            log.error("数据初始化失败，请检查数据库连接和表结构: {}", e.getMessage(), e);
        }
    }
}
