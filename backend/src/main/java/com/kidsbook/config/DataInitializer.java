package com.kidsbook.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kidsbook.entity.Admin;
import com.kidsbook.entity.ReaderAccount;
import com.kidsbook.mapper.AdminMapper;
import com.kidsbook.mapper.ReaderAccountMapper;
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
    private final ReaderAccountMapper readerAccountMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        try {
            initAdmin();
            initReaderAccount();
        } catch (Exception e) {
            log.error("数据初始化失败，请检查数据库连接和表结构: {}", e.getMessage(), e);
        }
    }

    private void initAdmin() {
        String encodedPassword = passwordEncoder.encode("admin123");
        Admin admin = adminMapper.selectOne(
            new LambdaQueryWrapper<Admin>().eq(Admin::getUsername, "admin")
        );
        if (admin == null) {
            admin = new Admin();
            admin.setUsername("admin");
            admin.setPassword(encodedPassword);
            admin.setNickname("超级管理员");
            admin.setEmail("admin@kidsbook.com");
            adminMapper.insert(admin);
            log.info("=== 初始管理员账号已创建 === 用户名: admin, 密码: admin123");
        } else {
            admin.setPassword(encodedPassword);
            if (admin.getEmail() == null) {
                admin.setEmail("admin@kidsbook.com");
            }
            adminMapper.updateById(admin);
            log.info("=== 管理员密码已更新 === 用户名: admin, 密码: admin123");
        }
    }

    private void initReaderAccount() {
        ReaderAccount account = readerAccountMapper.selectOne(
            new LambdaQueryWrapper<ReaderAccount>().eq(ReaderAccount::getUsername, "xiaoming")
        );
        if (account == null) {
            account = new ReaderAccount();
            account.setUsername("xiaoming");
            account.setPassword(passwordEncoder.encode("123456"));
            account.setReaderId(1L);
            account.setStatus("active");
            readerAccountMapper.insert(account);
            log.info("=== 默认读者账号已创建 === 用户名: xiaoming, 密码: 123456");
        }
    }
}
