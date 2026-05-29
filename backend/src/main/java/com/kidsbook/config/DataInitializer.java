package com.kidsbook.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kidsbook.entity.Admin;
import com.kidsbook.entity.Reader;
import com.kidsbook.entity.ReaderAccount;
import com.kidsbook.mapper.AdminMapper;
import com.kidsbook.mapper.ReaderAccountMapper;
import com.kidsbook.mapper.ReaderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {
    private final AdminMapper adminMapper;
    private final ReaderAccountMapper readerAccountMapper;
    private final ReaderMapper readerMapper;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        try {
            initTables();
            initAdmin();
            initReaderAccount();
        } catch (Exception e) {
            log.error("数据初始化失败，请检查数据库连接和表结构: {}", e.getMessage(), e);
        }
    }

    private void initTables() {
        try {
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `reading_progress` (" +
                "`id` BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                "`reader_id` BIGINT NOT NULL, " +
                "`book_id` BIGINT NOT NULL, " +
                "`book_title` VARCHAR(200) DEFAULT '', " +
                "`total_pages` INT DEFAULT 0, " +
                "`current_page` INT DEFAULT 0, " +
                "`progress_percent` INT DEFAULT 0, " +
                "`reading_minutes` INT DEFAULT 0, " +
                "`status` VARCHAR(20) DEFAULT 'reading', " +
                "`notes` TEXT, " +
                "`create_time` DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                "`update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
                "INDEX `idx_rp_reader_id` (`reader_id`), " +
                "INDEX `idx_rp_book_id` (`book_id`)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `reading_note` (" +
                "`id` BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                "`reader_id` BIGINT NOT NULL, " +
                "`book_id` BIGINT NOT NULL, " +
                "`progress_id` BIGINT, " +
                "`book_title` VARCHAR(200) DEFAULT '', " +
                "`content` TEXT NOT NULL, " +
                "`page_number` INT DEFAULT 0, " +
                "`create_time` DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                "`update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
                "INDEX `idx_rn_reader_id` (`reader_id`), " +
                "INDEX `idx_rn_book_id` (`book_id`)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

            log.info("=== 阅读进度相关表已就绪 ===");
        } catch (Exception e) {
            log.warn("创建阅读进度表时出现警告: {}", e.getMessage());
        }

        try {
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `book_resource` (" +
                "`id` BIGINT NOT NULL AUTO_INCREMENT, " +
                "`book_id` BIGINT DEFAULT NULL, " +
                "`file_name` VARCHAR(255) NOT NULL, " +
                "`original_name` VARCHAR(255) NOT NULL, " +
                "`file_path` VARCHAR(500) NOT NULL, " +
                "`file_type` VARCHAR(20) NOT NULL, " +
                "`file_size` BIGINT NOT NULL DEFAULT 0, " +
                "`mime_type` VARCHAR(100) DEFAULT NULL, " +
                "`create_time` DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                "PRIMARY KEY (`id`), " +
                "KEY `idx_br_book_id` (`book_id`)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `book_review` (" +
                "`id` BIGINT NOT NULL AUTO_INCREMENT, " +
                "`book_id` BIGINT NOT NULL, " +
                "`reader_id` BIGINT NOT NULL, " +
                "`reader_name` VARCHAR(50) DEFAULT NULL, " +
                "`book_title` VARCHAR(200) DEFAULT NULL, " +
                "`rating` INT NOT NULL, " +
                "`content` TEXT, " +
                "`status` VARCHAR(20) NOT NULL DEFAULT 'pending', " +
                "`admin_reply` TEXT DEFAULT NULL, " +
                "`reply_time` DATETIME DEFAULT NULL, " +
                "`create_time` DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                "`update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
                "PRIMARY KEY (`id`), " +
                "UNIQUE KEY `uk_book_reader` (`book_id`, `reader_id`), " +
                "KEY `idx_brev_reader_id` (`reader_id`), " +
                "KEY `idx_brev_status` (`status`)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

            try {
                jdbcTemplate.execute("ALTER TABLE `book` ADD COLUMN `avg_rating` DECIMAL(2,1) DEFAULT 0.0");
            } catch (Exception ignored) {}
            try {
                jdbcTemplate.execute("ALTER TABLE `book` ADD COLUMN `review_count` INT DEFAULT 0");
            } catch (Exception ignored) {}

            log.info("=== 图书资源与评价相关表已就绪 ===");
        } catch (Exception e) {
            log.warn("创建资源/评价表时出现警告: {}", e.getMessage());
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
            log.info("=== 管理员密码已重置 === 用户名: admin, 密码: admin123");
        }
    }

    private void initReaderAccount() {
        Reader reader = readerMapper.selectOne(
            new LambdaQueryWrapper<Reader>().eq(Reader::getName, "小明")
        );
        if (reader == null) {
            reader = new Reader();
            reader.setName("小明");
            reader.setAge(8);
            reader.setGender("male");
            reader.setParentName("张先生");
            reader.setParentPhone("13800138001");
            reader.setStatus("normal");
            reader.setBorrowCount(0);
            reader.setOverdueCount(0);
            reader.setPoints(0);
            reader.setTotalReadingDays(0);
            reader.setLevel("新手读者");
            readerMapper.insert(reader);
            log.info("=== 默认读者已创建 === 姓名: 小明, ID: {}", reader.getId());
        }

        ReaderAccount account = readerAccountMapper.selectOne(
            new LambdaQueryWrapper<ReaderAccount>().eq(ReaderAccount::getUsername, "xiaoming")
        );
        if (account == null) {
            account = new ReaderAccount();
            account.setUsername("xiaoming");
            account.setPassword(passwordEncoder.encode("123456"));
            account.setReaderId(reader.getId());
            account.setStatus("active");
            readerAccountMapper.insert(account);
            log.info("=== 默认读者账号已创建 === 用户名: xiaoming, 密码: 123456");
        } else {
            account.setPassword(passwordEncoder.encode("123456"));
            account.setReaderId(reader.getId());
            account.setStatus("active");
            readerAccountMapper.updateById(account);
            log.info("=== 读者账号密码已重置 === 用户名: xiaoming, 密码: 123456");
        }
    }
}
