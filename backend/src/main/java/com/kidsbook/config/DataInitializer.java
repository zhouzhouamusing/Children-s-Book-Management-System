package com.kidsbook.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kidsbook.common.Permission;
import com.kidsbook.common.RolePermissions;
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

import java.util.*;

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
            initRbacTables();
            initAdmin();
            initReaderAccount();
            initRbacData();
        } catch (Exception e) {
            log.error("数据初始化失败，请检查数据库连接和表结构: {}", e.getMessage(), e);
        }
    }

    private void initTables() {
        try {
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `admin` (" +
                "`id` BIGINT NOT NULL AUTO_INCREMENT, " +
                "`username` VARCHAR(50) NOT NULL, " +
                "`password` VARCHAR(200) NOT NULL, " +
                "`nickname` VARCHAR(50) DEFAULT NULL, " +
                "`email` VARCHAR(100) DEFAULT NULL, " +
                "`avatar` VARCHAR(500) DEFAULT NULL, " +
                "`create_time` DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                "`update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
                "PRIMARY KEY (`id`), " +
                "UNIQUE KEY `uk_admin_username` (`username`)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `reader` (" +
                "`id` BIGINT NOT NULL AUTO_INCREMENT, " +
                "`name` VARCHAR(50) NOT NULL, " +
                "`age` INT DEFAULT NULL, " +
                "`gender` VARCHAR(10) DEFAULT NULL, " +
                "`parent_name` VARCHAR(50) DEFAULT NULL, " +
                "`parent_phone` VARCHAR(20) DEFAULT NULL, " +
                "`status` VARCHAR(20) DEFAULT 'normal', " +
                "`suspend_reason` VARCHAR(20) DEFAULT NULL, " +
                "`borrow_count` INT DEFAULT 0, " +
                "`overdue_count` INT DEFAULT 0, " +
                "`points` INT DEFAULT 0, " +
                "`total_reading_days` INT DEFAULT 0, " +
                "`level` VARCHAR(30) DEFAULT NULL, " +
                "`remark` TEXT, " +
                "`create_time` DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                "`update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
                "PRIMARY KEY (`id`)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `reader_account` (" +
                "`id` BIGINT NOT NULL AUTO_INCREMENT, " +
                "`username` VARCHAR(50) NOT NULL, " +
                "`password` VARCHAR(200) NOT NULL, " +
                "`reader_id` BIGINT NOT NULL, " +
                "`email` VARCHAR(100) DEFAULT NULL, " +
                "`status` VARCHAR(20) DEFAULT 'active', " +
                "`create_time` DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                "`update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
                "PRIMARY KEY (`id`), " +
                "UNIQUE KEY `uk_ra_username` (`username`), " +
                "KEY `idx_ra_reader_id` (`reader_id`)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `book` (" +
                "`id` BIGINT NOT NULL AUTO_INCREMENT, " +
                "`title` VARCHAR(200) NOT NULL, " +
                "`author` VARCHAR(100) DEFAULT NULL, " +
                "`publisher` VARCHAR(100) DEFAULT NULL, " +
                "`isbn` VARCHAR(30) DEFAULT NULL, " +
                "`category` VARCHAR(50) DEFAULT NULL, " +
                "`age_range` VARCHAR(20) DEFAULT NULL, " +
                "`price` DECIMAL(10,2) DEFAULT NULL, " +
                "`stock` INT DEFAULT 0, " +
                "`cover_url` VARCHAR(500) DEFAULT NULL, " +
                "`description` TEXT, " +
                "`status` INT DEFAULT 1, " +
                "`avg_rating` DECIMAL(2,1) DEFAULT 0.0, " +
                "`review_count` INT DEFAULT 0, " +
                "`create_time` DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                "`update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
                "PRIMARY KEY (`id`), " +
                "KEY `idx_book_category` (`category`)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `borrow_record` (" +
                "`id` BIGINT NOT NULL AUTO_INCREMENT, " +
                "`reader_id` BIGINT NOT NULL, " +
                "`book_id` BIGINT NOT NULL, " +
                "`book_title` VARCHAR(200) DEFAULT NULL, " +
                "`borrow_date` DATE NOT NULL, " +
                "`due_date` DATE NOT NULL, " +
                "`return_date` DATE DEFAULT NULL, " +
                "`status` VARCHAR(20) NOT NULL DEFAULT 'borrowing', " +
                "`create_time` DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                "`update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
                "PRIMARY KEY (`id`), " +
                "KEY `idx_br_reader_id` (`reader_id`), " +
                "KEY `idx_br_book_id` (`book_id`), " +
                "KEY `idx_br_status` (`status`)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `book_reservation` (" +
                "`id` BIGINT NOT NULL AUTO_INCREMENT, " +
                "`reader_id` BIGINT NOT NULL, " +
                "`book_id` BIGINT NOT NULL, " +
                "`book_title` VARCHAR(200) DEFAULT NULL, " +
                "`reserve_date` DATETIME NOT NULL, " +
                "`expire_date` DATETIME NOT NULL, " +
                "`status` VARCHAR(20) DEFAULT 'pending', " +
                "`remark` VARCHAR(200) DEFAULT NULL, " +
                "`create_time` DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                "`update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
                "PRIMARY KEY (`id`), " +
                "KEY `idx_bres_reader_id` (`reader_id`), " +
                "KEY `idx_bres_book_id` (`book_id`), " +
                "KEY `idx_bres_status` (`status`)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `category` (" +
                "`id` BIGINT NOT NULL AUTO_INCREMENT, " +
                "`name` VARCHAR(50) NOT NULL, " +
                "`icon` VARCHAR(50) DEFAULT NULL, " +
                "`color` VARCHAR(20) DEFAULT NULL, " +
                "`age_range_min` INT DEFAULT NULL, " +
                "`age_range_max` INT DEFAULT NULL, " +
                "`sort_order` INT DEFAULT 0, " +
                "`description` VARCHAR(200) DEFAULT NULL, " +
                "`status` INT DEFAULT 1, " +
                "`create_time` DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                "`update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
                "PRIMARY KEY (`id`), " +
                "UNIQUE KEY `uk_category_name` (`name`)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

            log.info("=== 基础数据表已就绪 ===");
        } catch (Exception e) {
            log.warn("创建基础表时出现警告: {}", e.getMessage());
        }

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

        try {
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `reader_points_log` (" +
                "`id` BIGINT NOT NULL AUTO_INCREMENT, " +
                "`reader_id` BIGINT NOT NULL, " +
                "`points` INT NOT NULL, " +
                "`type` VARCHAR(30) NOT NULL, " +
                "`description` VARCHAR(200) DEFAULT NULL, " +
                "`borrow_record_id` BIGINT DEFAULT NULL, " +
                "`create_time` DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                "PRIMARY KEY (`id`), " +
                "KEY `idx_rpl_reader_id` (`reader_id`)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `reader_monthly_stats` (" +
                "`id` BIGINT NOT NULL AUTO_INCREMENT, " +
                "`reader_id` BIGINT NOT NULL, " +
                "`year_month` VARCHAR(7) NOT NULL, " +
                "`borrow_count` INT DEFAULT 0, " +
                "`return_count` INT DEFAULT 0, " +
                "`reading_days` INT DEFAULT 0, " +
                "`points_earned` INT DEFAULT 0, " +
                "`create_time` DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                "`update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
                "PRIMARY KEY (`id`), " +
                "UNIQUE KEY `uk_reader_month` (`reader_id`, `year_month`)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `audit_log` (" +
                "`id` BIGINT NOT NULL AUTO_INCREMENT, " +
                "`operator_username` VARCHAR(50) DEFAULT NULL, " +
                "`operator_role` VARCHAR(20) DEFAULT NULL, " +
                "`action` VARCHAR(50) NOT NULL, " +
                "`target_type` VARCHAR(50) DEFAULT NULL, " +
                "`target_id` BIGINT DEFAULT NULL, " +
                "`detail` VARCHAR(500) DEFAULT NULL, " +
                "`ip_address` VARCHAR(50) DEFAULT NULL, " +
                "`create_time` DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                "PRIMARY KEY (`id`), " +
                "KEY `idx_al_action` (`action`), " +
                "KEY `idx_al_create_time` (`create_time`)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `admin_application` (" +
                "`id` BIGINT NOT NULL AUTO_INCREMENT, " +
                "`reader_id` BIGINT NOT NULL, " +
                "`reader_name` VARCHAR(50) DEFAULT NULL, " +
                "`username` VARCHAR(50) DEFAULT NULL, " +
                "`reason` TEXT, " +
                "`status` VARCHAR(20) NOT NULL DEFAULT 'pending', " +
                "`reject_reason` VARCHAR(200) DEFAULT NULL, " +
                "`approved_by` VARCHAR(50) DEFAULT NULL, " +
                "`approved_time` DATETIME DEFAULT NULL, " +
                "`create_time` DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                "`update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
                "PRIMARY KEY (`id`), " +
                "KEY `idx_aa_reader_id` (`reader_id`)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

            try {
                jdbcTemplate.execute("ALTER TABLE `reader` ADD COLUMN `suspend_reason` VARCHAR(20) DEFAULT NULL");
            } catch (Exception ignored) {}
            try {
                jdbcTemplate.execute("ALTER TABLE `reader_account` ADD COLUMN `email` VARCHAR(100) DEFAULT NULL");
            } catch (Exception ignored) {}

            log.info("=== 积分、审计、申请相关表已就绪 ===");
        } catch (Exception e) {
            log.warn("创建附加表时出现警告: {}", e.getMessage());
        }

        try {
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `appeal` (" +
                "`id` BIGINT NOT NULL AUTO_INCREMENT, " +
                "`reader_id` BIGINT NOT NULL, " +
                "`reader_name` VARCHAR(50) DEFAULT NULL, " +
                "`type` VARCHAR(30) NOT NULL DEFAULT 'suspension', " +
                "`reason` TEXT NOT NULL, " +
                "`evidence` VARCHAR(500) DEFAULT NULL, " +
                "`status` VARCHAR(20) NOT NULL DEFAULT 'pending', " +
                "`admin_feedback` TEXT DEFAULT NULL, " +
                "`reviewed_by` VARCHAR(50) DEFAULT NULL, " +
                "`reviewed_time` DATETIME DEFAULT NULL, " +
                "`create_time` DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                "`update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
                "PRIMARY KEY (`id`), " +
                "KEY `idx_appeal_reader_id` (`reader_id`), " +
                "KEY `idx_appeal_status` (`status`)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
            log.info("=== 申诉表已就绪 ===");
        } catch (Exception e) {
            log.warn("创建申诉表时出现警告: {}", e.getMessage());
        }
    }

    private void initAdmin() {
        Admin admin = adminMapper.selectOne(
            new LambdaQueryWrapper<Admin>().eq(Admin::getUsername, "admin")
        );
        if (admin == null) {
            admin = new Admin();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setNickname("超级管理员");
            admin.setEmail("admin@kidsbook.com");
            adminMapper.insert(admin);
            log.info("=== 初始管理员账号已创建 === 用户名: admin, 密码: admin123");
        } else {
            boolean updated = false;
            if (admin.getEmail() == null) {
                admin.setEmail("admin@kidsbook.com");
                updated = true;
            }
            if (updated) {
                adminMapper.updateById(admin);
            }
            log.info("=== 默认管理员已存在，跳过密码初始化 ===");
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
            log.info("=== 默认读者账号已存在，跳过密码初始化 ===");
        }
    }

    private void initRbacTables() {
        try {
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `sys_permission` (" +
                "`id` BIGINT NOT NULL AUTO_INCREMENT, " +
                "`code` VARCHAR(60) NOT NULL, " +
                "`name` VARCHAR(80) NOT NULL, " +
                "`module` VARCHAR(40) DEFAULT NULL, " +
                "`description` VARCHAR(200) DEFAULT NULL, " +
                "`type` VARCHAR(20) DEFAULT 'button', " +
                "`create_time` DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                "PRIMARY KEY (`id`), " +
                "UNIQUE KEY `uk_perm_code` (`code`)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

            try {
                jdbcTemplate.execute("ALTER TABLE `sys_permission` ADD COLUMN `type` VARCHAR(20) DEFAULT 'button'");
            } catch (Exception ignored) {}

            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `sys_role` (" +
                "`id` BIGINT NOT NULL AUTO_INCREMENT, " +
                "`code` VARCHAR(40) NOT NULL, " +
                "`name` VARCHAR(60) NOT NULL, " +
                "`level` INT NOT NULL DEFAULT 0, " +
                "`description` VARCHAR(200) DEFAULT NULL, " +
                "`status` INT DEFAULT 1, " +
                "`create_time` DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                "`update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
                "PRIMARY KEY (`id`), " +
                "UNIQUE KEY `uk_role_code` (`code`)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `sys_role_permission` (" +
                "`id` BIGINT NOT NULL AUTO_INCREMENT, " +
                "`role_id` BIGINT NOT NULL, " +
                "`permission_id` BIGINT NOT NULL, " +
                "`create_time` DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                "PRIMARY KEY (`id`), " +
                "UNIQUE KEY `uk_role_perm` (`role_id`, `permission_id`), " +
                "KEY `idx_rp_role_id` (`role_id`), " +
                "KEY `idx_rp_perm_id` (`permission_id`)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `sys_user_role` (" +
                "`id` BIGINT NOT NULL AUTO_INCREMENT, " +
                "`user_type` VARCHAR(20) NOT NULL, " +
                "`user_id` BIGINT NOT NULL, " +
                "`role_id` BIGINT NOT NULL, " +
                "`create_time` DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                "PRIMARY KEY (`id`), " +
                "UNIQUE KEY `uk_user_role` (`user_type`, `user_id`, `role_id`), " +
                "KEY `idx_ur_user` (`user_type`, `user_id`), " +
                "KEY `idx_ur_role` (`role_id`)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `sys_menu` (" +
                "`id` BIGINT NOT NULL AUTO_INCREMENT, " +
                "`parent_id` BIGINT DEFAULT 0, " +
                "`name` VARCHAR(60) NOT NULL, " +
                "`path` VARCHAR(200) DEFAULT NULL, " +
                "`icon` VARCHAR(60) DEFAULT NULL, " +
                "`sort_order` INT DEFAULT 0, " +
                "`permission_code` VARCHAR(60) DEFAULT NULL, " +
                "`type` INT DEFAULT 1, " +
                "`status` INT DEFAULT 1, " +
                "`create_time` DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                "PRIMARY KEY (`id`), " +
                "KEY `idx_menu_parent` (`parent_id`)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

            log.info("=== RBAC 权限管理表已就绪 ===");
        } catch (Exception e) {
            log.warn("创建RBAC表时出现警告: {}", e.getMessage());
        }
    }

    private void initRbacData() {
        try {
            Integer permCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_permission", Integer.class);

            Map<String, String> permNames = new LinkedHashMap<>();
            permNames.put("BOOK_CREATE", "创建图书");
            permNames.put("BOOK_READ", "查看图书");
            permNames.put("BOOK_UPDATE", "编辑图书");
            permNames.put("BOOK_DELETE", "删除图书");
            permNames.put("READER_CREATE", "创建读者");
            permNames.put("READER_READ", "查看读者");
            permNames.put("READER_UPDATE", "编辑读者");
            permNames.put("READER_DELETE", "删除读者");
            permNames.put("CATEGORY_CREATE", "创建分类");
            permNames.put("CATEGORY_READ", "查看分类");
            permNames.put("CATEGORY_UPDATE", "编辑分类");
            permNames.put("CATEGORY_DELETE", "删除分类");
            permNames.put("BORROW_CREATE", "创建借阅");
            permNames.put("BORROW_READ", "查看借阅");
            permNames.put("BORROW_UPDATE", "更新借阅");
            permNames.put("RESERVATION_READ", "查看预约");
            permNames.put("RESERVATION_UPDATE", "审批预约");
            permNames.put("REVIEW_READ", "查看评论");
            permNames.put("REVIEW_UPDATE", "审核评论");
            permNames.put("REVIEW_DELETE", "删除评论");
            permNames.put("FILE_CREATE", "上传文件");
            permNames.put("FILE_READ", "查看文件");
            permNames.put("FILE_DELETE", "删除文件");
            permNames.put("DASHBOARD_READ", "查看仪表盘");
            permNames.put("AUDIT_LOG_READ", "查看审计日志");
            permNames.put("ADMIN_APPLICATION_REVIEW", "审批管理员申请");
            permNames.put("READER_RESERVATION_CREATE", "读者创建预约");
            permNames.put("READER_RESERVATION_READ", "读者查看预约");
            permNames.put("READER_RESERVATION_CANCEL", "读者取消预约");
            permNames.put("READER_REVIEW_CREATE", "读者创建评论");
            permNames.put("READER_REVIEW_READ", "读者查看评论");
            permNames.put("READER_REVIEW_UPDATE", "读者编辑评论");
            permNames.put("READER_REVIEW_DELETE", "读者删除评论");
            permNames.put("READING_PROGRESS_CREATE", "创建阅读进度");
            permNames.put("READING_PROGRESS_READ", "查看阅读进度");
            permNames.put("READING_PROGRESS_UPDATE", "更新阅读进度");
            permNames.put("READING_PROGRESS_DELETE", "删除阅读进度");
            permNames.put("ADMIN_APPLICATION_APPLY", "申请成为管理员");
            permNames.put("ADMIN_APPLICATION_STATUS", "查看申请状态");
            permNames.put("READER_PROFILE_READ", "查看个人中心");
            permNames.put("READER_PROFILE_UPDATE", "编辑个人资料");
            permNames.put("READER_BOOK_BROWSE", "浏览图书");
            permNames.put("READER_CATEGORY_BROWSE", "浏览分类");
            permNames.put("READER_BORROW_READ", "查看借阅记录");
            permNames.put("READER_APPEAL_CREATE", "提交申诉");
            permNames.put("READER_APPEAL_VIEW", "查看我的申诉");
            permNames.put("APPEAL_READ", "查看申诉列表");
            permNames.put("APPEAL_REVIEW", "审核申诉");
            permNames.put("ROLE_MANAGE", "角色管理");
            permNames.put("PERMISSION_MANAGE", "权限管理");
            permNames.put("USER_ROLE_ASSIGN", "用户角色分配");

            Map<String, String> moduleMap = new HashMap<>();
            moduleMap.put("BOOK", "图书管理");
            moduleMap.put("READER_RE", "读者预约");
            moduleMap.put("READER_REVIEW", "读者评论");
            moduleMap.put("READER_PROFILE", "读者中心");
            moduleMap.put("READER_BOOK", "读者浏览");
            moduleMap.put("READER_CATEGORY", "读者浏览");
            moduleMap.put("READER_BORROW", "读者借阅");
            moduleMap.put("READER_APPEAL", "读者申诉");
            moduleMap.put("READER", "读者管理");
            moduleMap.put("CATEGORY", "分类管理");
            moduleMap.put("BORROW", "借阅管理");
            moduleMap.put("RESERVATION", "预约管理");
            moduleMap.put("REVIEW", "评论管理");
            moduleMap.put("FILE", "文件管理");
            moduleMap.put("DASHBOARD", "仪表盘");
            moduleMap.put("AUDIT_LOG", "审计日志");
            moduleMap.put("ADMIN_APPLICATION", "管理员申请");
            moduleMap.put("READING_PROGRESS", "阅读进度");
            moduleMap.put("APPEAL", "申诉管理");
            moduleMap.put("ROLE", "系统管理");
            moduleMap.put("PERMISSION", "系统管理");
            moduleMap.put("USER_ROLE", "系统管理");

            if (permCount != null && permCount > 0) {
                // Incremental mode: insert new permissions that don't exist yet
                int added = 0;
                for (Map.Entry<String, String> entry : permNames.entrySet()) {
                    String code = entry.getKey();
                    String name = entry.getValue();
                    String module = resolveModule(code, moduleMap);
                    String type = inferPermissionType(code);
                    try {
                        jdbcTemplate.update(
                            "INSERT IGNORE INTO sys_permission (code, name, module, type) VALUES (?, ?, ?, ?)",
                            code, name, module, type);
                        added++;
                    } catch (Exception ignored) {}
                }
                // Update type field for existing permissions
                try {
                    jdbcTemplate.update("UPDATE sys_permission SET type = 'menu' WHERE code IN (" +
                        "'BOOK_READ','READER_READ','CATEGORY_READ','BORROW_READ','RESERVATION_READ'," +
                        "'REVIEW_READ','FILE_READ','DASHBOARD_READ','AUDIT_LOG_READ','ROLE_MANAGE'," +
                        "'PERMISSION_MANAGE','USER_ROLE_ASSIGN','READER_RESERVATION_READ','READER_REVIEW_READ'," +
                        "'READING_PROGRESS_READ','ADMIN_APPLICATION_STATUS','READER_PROFILE_READ'," +
                        "'READER_BOOK_BROWSE','READER_CATEGORY_BROWSE','READER_BORROW_READ'," +
                        "'READER_APPEAL_VIEW','APPEAL_READ') AND (type IS NULL OR type = '')");
                    jdbcTemplate.update("UPDATE sys_permission SET type = 'button' WHERE type IS NULL OR type = ''");
                } catch (Exception ignored) {}
                // Ensure SUPER_ADMIN has all permissions
                Long superAdminRoleId = null;
                try {
                    superAdminRoleId = jdbcTemplate.queryForObject(
                        "SELECT id FROM sys_role WHERE code = 'SUPER_ADMIN'", Long.class);
                } catch (Exception ignored) {}
                if (superAdminRoleId != null) {
                    jdbcTemplate.update(
                        "INSERT IGNORE INTO sys_role_permission (role_id, permission_id) " +
                        "SELECT ?, id FROM sys_permission WHERE id NOT IN " +
                        "(SELECT permission_id FROM sys_role_permission WHERE role_id = ?)",
                        superAdminRoleId, superAdminRoleId);
                }
                // Ensure ADMIN role has appeal permissions
                Long adminRoleId = null;
                try {
                    adminRoleId = jdbcTemplate.queryForObject(
                        "SELECT id FROM sys_role WHERE code = 'ADMIN'", Long.class);
                } catch (Exception ignored) {}
                if (adminRoleId != null) {
                    for (Permission p : RolePermissions.getPermissions("ADMIN")) {
                        try {
                            jdbcTemplate.update(
                                "INSERT IGNORE INTO sys_role_permission (role_id, permission_id) " +
                                "SELECT ?, id FROM sys_permission WHERE code = ?",
                                adminRoleId, p.name());
                        } catch (Exception ignored) {}
                    }
                }
                // Ensure READER role has reader permissions
                Long readerRoleId = null;
                try {
                    readerRoleId = jdbcTemplate.queryForObject(
                        "SELECT id FROM sys_role WHERE code = 'READER'", Long.class);
                } catch (Exception ignored) {}
                if (readerRoleId != null) {
                    for (Permission p : RolePermissions.getPermissions("READER")) {
                        try {
                            jdbcTemplate.update(
                                "INSERT IGNORE INTO sys_role_permission (role_id, permission_id) " +
                                "SELECT ?, id FROM sys_permission WHERE code = ?",
                                readerRoleId, p.name());
                        } catch (Exception ignored) {}
                    }
                }
                // Add appeal menu if not exists
                try {
                    Integer menuCount = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM sys_menu WHERE permission_code = 'APPEAL_READ'", Integer.class);
                    if (menuCount == null || menuCount == 0) {
                        jdbcTemplate.update("INSERT INTO sys_menu (parent_id, name, path, icon, sort_order, permission_code, type, status) VALUES (0, '申诉管理', '/appeals', 'Document', 7, 'APPEAL_READ', 1, 1)");
                    }
                } catch (Exception ignored) {}
                log.info("=== RBAC数据已增量更新 ===");
                return;
            }

            for (Map.Entry<String, String> entry : permNames.entrySet()) {
                String code = entry.getKey();
                String name = entry.getValue();
                String module = resolveModule(code, moduleMap);
                String type = inferPermissionType(code);
                jdbcTemplate.update(
                    "INSERT INTO sys_permission (code, name, module, type) VALUES (?, ?, ?, ?)",
                    code, name, module, type);
            }
            log.info("=== 已初始化 {} 个权限 ===", permNames.size());

            jdbcTemplate.update(
                "INSERT INTO sys_role (code, name, level, description, status) VALUES (?, ?, ?, ?, ?)",
                "READER", "读者", 10, "普通读者角色，拥有基本借阅和浏览权限", 1);
            jdbcTemplate.update(
                "INSERT INTO sys_role (code, name, level, description, status) VALUES (?, ?, ?, ?, ?)",
                "ADMIN", "管理员", 50, "系统管理员，拥有图书和读者管理权限", 1);
            jdbcTemplate.update(
                "INSERT INTO sys_role (code, name, level, description, status) VALUES (?, ?, ?, ?, ?)",
                "SUPER_ADMIN", "超级管理员", 100, "最高权限管理员，拥有所有权限包括角色管理", 1);
            log.info("=== 已初始化 3 个角色 ===");

            Long readerRoleId = jdbcTemplate.queryForObject(
                "SELECT id FROM sys_role WHERE code = 'READER'", Long.class);
            Long adminRoleId = jdbcTemplate.queryForObject(
                "SELECT id FROM sys_role WHERE code = 'ADMIN'", Long.class);
            Long superAdminRoleId = jdbcTemplate.queryForObject(
                "SELECT id FROM sys_role WHERE code = 'SUPER_ADMIN'", Long.class);

            Set<Permission> readerPerms = RolePermissions.getPermissions("READER");
            for (Permission p : readerPerms) {
                jdbcTemplate.update(
                    "INSERT INTO sys_role_permission (role_id, permission_id) " +
                    "SELECT ?, id FROM sys_permission WHERE code = ?",
                    readerRoleId, p.name());
            }

            Set<Permission> adminPerms = RolePermissions.getPermissions("ADMIN");
            for (Permission p : adminPerms) {
                jdbcTemplate.update(
                    "INSERT INTO sys_role_permission (role_id, permission_id) " +
                    "SELECT ?, id FROM sys_permission WHERE code = ?",
                    adminRoleId, p.name());
            }

            // SUPER_ADMIN gets all permissions
            jdbcTemplate.update(
                "INSERT INTO sys_role_permission (role_id, permission_id) " +
                "SELECT ?, id FROM sys_permission", superAdminRoleId);
            log.info("=== 已初始化角色权限关联 ===");

            // Assign SUPER_ADMIN to existing admin user
            Admin admin = adminMapper.selectOne(
                new LambdaQueryWrapper<Admin>().eq(Admin::getUsername, "admin"));
            if (admin != null) {
                jdbcTemplate.update(
                    "INSERT INTO sys_user_role (user_type, user_id, role_id) VALUES (?, ?, ?)",
                    "ADMIN", admin.getId(), superAdminRoleId);
            }

            // Assign READER to existing reader account
            ReaderAccount readerAccount = readerAccountMapper.selectOne(
                new LambdaQueryWrapper<ReaderAccount>().eq(ReaderAccount::getUsername, "xiaoming"));
            if (readerAccount != null) {
                jdbcTemplate.update(
                    "INSERT INTO sys_user_role (user_type, user_id, role_id) VALUES (?, ?, ?)",
                    "READER", readerAccount.getId(), readerRoleId);
            }
            log.info("=== 已初始化用户角色分配 ===");

            // Seed menus
            initMenus();

        } catch (Exception e) {
            log.warn("RBAC数据初始化出现警告: {}", e.getMessage());
        }
    }

    private void initMenus() {
        // Admin menus
        jdbcTemplate.update("INSERT INTO sys_menu (parent_id, name, path, icon, sort_order, permission_code, type, status) VALUES (0, '数据概览', '/dashboard', 'DataAnalysis', 1, 'DASHBOARD_READ', 1, 1)");
        jdbcTemplate.update("INSERT INTO sys_menu (parent_id, name, path, icon, sort_order, permission_code, type, status) VALUES (0, '图书管理', '/books', 'Reading', 2, 'BOOK_READ', 1, 1)");
        jdbcTemplate.update("INSERT INTO sys_menu (parent_id, name, path, icon, sort_order, permission_code, type, status) VALUES (0, '分类管理', '/categories', 'FolderOpened', 3, 'CATEGORY_READ', 1, 1)");
        jdbcTemplate.update("INSERT INTO sys_menu (parent_id, name, path, icon, sort_order, permission_code, type, status) VALUES (0, '读者管理', '/readers', 'UserFilled', 4, 'READER_READ', 1, 1)");
        jdbcTemplate.update("INSERT INTO sys_menu (parent_id, name, path, icon, sort_order, permission_code, type, status) VALUES (0, '借阅管理', '/borrows', 'Notebook', 5, 'BORROW_READ', 1, 1)");
        jdbcTemplate.update("INSERT INTO sys_menu (parent_id, name, path, icon, sort_order, permission_code, type, status) VALUES (0, '管理员审批', '/admin-applications', 'Stamp', 6, 'ADMIN_APPLICATION_REVIEW', 1, 1)");
        jdbcTemplate.update("INSERT INTO sys_menu (parent_id, name, path, icon, sort_order, permission_code, type, status) VALUES (0, '申诉管理', '/appeals', 'Document', 7, 'APPEAL_READ', 1, 1)");
        jdbcTemplate.update("INSERT INTO sys_menu (parent_id, name, path, icon, sort_order, permission_code, type, status) VALUES (0, '读者系统', '/reader-view', 'View', 8, 'READER_PROFILE_READ', 1, 1)");
        jdbcTemplate.update("INSERT INTO sys_menu (parent_id, name, path, icon, sort_order, permission_code, type, status) VALUES (0, '资源管理', '/resources', 'Files', 9, 'FILE_READ', 1, 1)");
        jdbcTemplate.update("INSERT INTO sys_menu (parent_id, name, path, icon, sort_order, permission_code, type, status) VALUES (0, '评价管理', '/reviews', 'ChatDotRound', 10, 'REVIEW_READ', 1, 1)");
        jdbcTemplate.update("INSERT INTO sys_menu (parent_id, name, path, icon, sort_order, permission_code, type, status) VALUES (0, '角色管理', '/system/roles', 'Key', 11, 'ROLE_MANAGE', 1, 1)");
        jdbcTemplate.update("INSERT INTO sys_menu (parent_id, name, path, icon, sort_order, permission_code, type, status) VALUES (0, '权限管理', '/system/permissions', 'Lock', 12, 'PERMISSION_MANAGE', 1, 1)");
        jdbcTemplate.update("INSERT INTO sys_menu (parent_id, name, path, icon, sort_order, permission_code, type, status) VALUES (0, '用户角色', '/system/user-roles', 'Avatar', 13, 'USER_ROLE_ASSIGN', 1, 1)");
        log.info("=== 已初始化系统菜单 ===");
    }

    private String inferPermissionType(String code) {
        Set<String> menuCodes = Set.of(
            "BOOK_READ", "READER_READ", "CATEGORY_READ", "BORROW_READ",
            "RESERVATION_READ", "REVIEW_READ", "FILE_READ", "DASHBOARD_READ",
            "AUDIT_LOG_READ", "ROLE_MANAGE", "PERMISSION_MANAGE", "USER_ROLE_ASSIGN",
            "READER_RESERVATION_READ", "READER_REVIEW_READ", "READING_PROGRESS_READ",
            "ADMIN_APPLICATION_STATUS", "READER_PROFILE_READ", "READER_BOOK_BROWSE",
            "READER_CATEGORY_BROWSE", "READER_BORROW_READ", "READER_APPEAL_VIEW",
            "APPEAL_READ"
        );
        return menuCodes.contains(code) ? "menu" : "button";
    }

    private String resolveModule(String code, Map<String, String> moduleMap) {
        // Try longest prefix match first for more specific mappings
        String bestMatch = null;
        int bestLen = 0;
        for (Map.Entry<String, String> m : moduleMap.entrySet()) {
            if (code.startsWith(m.getKey()) && m.getKey().length() > bestLen) {
                bestMatch = m.getValue();
                bestLen = m.getKey().length();
            }
        }
        return bestMatch != null ? bestMatch : "其他";
    }
}
