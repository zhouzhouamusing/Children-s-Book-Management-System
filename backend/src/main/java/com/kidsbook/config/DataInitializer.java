package com.kidsbook.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kidsbook.entity.*;
import com.kidsbook.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {
    private final AdminMapper adminMapper;
    private final ReaderAccountMapper readerAccountMapper;
    private final ReaderMapper readerMapper;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;
    private final SysPermissionMapper permissionMapper;
    private final SysRoleMapper roleMapper;
    private final SysRolePermissionMapper rolePermissionMapper;
    private final SysUserRoleMapper userRoleMapper;

    @Override
    public void run(String... args) {
        try {
            initTables();
            initRbacTables();
            initForeignKeys();
            initAdmin();
            initReaderAccount();
            initRbacData();
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

        try {
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `reader_appeal` (" +
                "`id` BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                "`reader_id` BIGINT NOT NULL, " +
                "`reader_name` VARCHAR(50) DEFAULT NULL, " +
                "`type` VARCHAR(30) NOT NULL COMMENT '申诉类型:borrow_dispute/account_suspended/review_rejected/other', " +
                "`related_id` BIGINT DEFAULT NULL COMMENT '关联记录ID', " +
                "`title` VARCHAR(200) NOT NULL, " +
                "`content` TEXT NOT NULL, " +
                "`evidence_urls` VARCHAR(1000) DEFAULT NULL COMMENT '证据文件路径', " +
                "`status` VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT 'pending/processing/resolved/rejected', " +
                "`admin_id` BIGINT DEFAULT NULL, " +
                "`admin_reply` TEXT DEFAULT NULL, " +
                "`resolve_time` DATETIME DEFAULT NULL, " +
                "`create_time` DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                "`update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
                "INDEX `idx_appeal_reader_id` (`reader_id`), " +
                "INDEX `idx_appeal_status` (`status`), " +
                "INDEX `idx_appeal_type` (`type`)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='读者申诉表'");
            log.info("=== 读者申诉表已就绪 ===");
        } catch (Exception e) {
            log.warn("创建申诉表时出现警告: {}", e.getMessage());
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

    private void initRbacTables() {
        try {
            // 创建表（如果不存在）
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `sys_permission` (" +
                "`id` BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                "`code` VARCHAR(100) NOT NULL COMMENT '权限编码', " +
                "`name` VARCHAR(100) NOT NULL COMMENT '权限名称', " +
                "`type` VARCHAR(20) NOT NULL DEFAULT 'button' COMMENT '类型:menu/button', " +
                "`parent_id` BIGINT DEFAULT 0 COMMENT '父权限ID', " +
                "`path` VARCHAR(200) DEFAULT NULL COMMENT '前端路由路径', " +
                "`icon` VARCHAR(50) DEFAULT NULL COMMENT '菜单图标', " +
                "`sort_order` INT DEFAULT 0 COMMENT '排序', " +
                "`status` TINYINT DEFAULT 1 COMMENT '1启用0禁用', " +
                "`create_time` DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                "`update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
                "UNIQUE KEY `uk_code` (`code`)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统权限表'");

            // 确保旧表有所有必须的列（兼容已存在但结构不完整的表）
            safeAddColumn("sys_permission", "parent_id", "BIGINT DEFAULT 0 COMMENT '父权限ID'");
            safeAddColumn("sys_permission", "path", "VARCHAR(200) DEFAULT NULL COMMENT '前端路由路径'");
            safeAddColumn("sys_permission", "icon", "VARCHAR(50) DEFAULT NULL COMMENT '菜单图标'");
            safeAddColumn("sys_permission", "sort_order", "INT DEFAULT 0 COMMENT '排序'");
            safeAddColumn("sys_permission", "type", "VARCHAR(20) NOT NULL DEFAULT 'button' COMMENT '类型:menu/button'");
            safeAddColumn("sys_permission", "status", "TINYINT DEFAULT 1 COMMENT '1启用0禁用'");
            safeAddColumn("sys_permission", "create_time", "DATETIME DEFAULT CURRENT_TIMESTAMP");
            safeAddColumn("sys_permission", "update_time", "DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP");

            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `sys_role` (" +
                "`id` BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                "`code` VARCHAR(50) NOT NULL COMMENT '角色编码', " +
                "`name` VARCHAR(50) NOT NULL COMMENT '角色名称', " +
                "`description` VARCHAR(200) DEFAULT NULL COMMENT '描述', " +
                "`status` TINYINT DEFAULT 1 COMMENT '1启用0禁用', " +
                "`create_time` DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                "`update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
                "UNIQUE KEY `uk_code` (`code`)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统角色表'");

            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `sys_role_permission` (" +
                "`id` BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                "`role_id` BIGINT NOT NULL, " +
                "`permission_id` BIGINT NOT NULL, " +
                "UNIQUE KEY `uk_role_perm` (`role_id`, `permission_id`), " +
                "KEY `idx_permission_id` (`permission_id`)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色-权限关联表'");

            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `sys_user_role` (" +
                "`id` BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                "`user_type` VARCHAR(20) NOT NULL COMMENT '用户类型:admin/reader', " +
                "`user_id` BIGINT NOT NULL, " +
                "`role_id` BIGINT NOT NULL, " +
                "UNIQUE KEY `uk_user_role` (`user_type`, `user_id`, `role_id`), " +
                "KEY `idx_role_id` (`role_id`)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户-角色关联表'");

            log.info("=== RBAC权限表已就绪 ===");
        } catch (Exception e) {
            log.error("创建RBAC表失败: {}", e.getMessage(), e);
        }
    }

    private void safeAddColumn(String table, String column, String definition) {
        try {
            jdbcTemplate.execute("ALTER TABLE `" + table + "` ADD COLUMN `" + column + "` " + definition);
        } catch (Exception ignored) {
            // 列已存在，忽略
        }
    }

    private void initForeignKeys() {
        safeAddForeignKey("sys_role_permission", "fk_srp_role", "role_id", "sys_role", "id", "CASCADE");
        safeAddForeignKey("sys_role_permission", "fk_srp_perm", "permission_id", "sys_permission", "id", "CASCADE");
        safeAddForeignKey("sys_user_role", "fk_sur_role", "role_id", "sys_role", "id", "CASCADE");
        safeAddForeignKey("reader_account", "fk_ra_reader", "reader_id", "reader", "id", "CASCADE");
        safeAddForeignKey("borrow_record", "fk_br_reader", "reader_id", "reader", "id", "RESTRICT");
        safeAddForeignKey("book_review", "fk_brev_reader", "reader_id", "reader", "id", "CASCADE");
        safeAddForeignKey("book_review", "fk_brev_book", "book_id", "book", "id", "CASCADE");
        safeAddForeignKey("book_resource", "fk_bres_book", "book_id", "book", "id", "SET NULL");
        safeAddForeignKey("reading_progress", "fk_rp_reader", "reader_id", "reader", "id", "CASCADE");
        safeAddForeignKey("reading_progress", "fk_rp_book", "book_id", "book", "id", "CASCADE");
        safeAddForeignKey("reading_note", "fk_rn_reader", "reader_id", "reader", "id", "CASCADE");
        safeAddForeignKey("reading_note", "fk_rn_book", "book_id", "book", "id", "CASCADE");
        safeAddForeignKey("admin_application", "fk_aa_reader", "reader_id", "reader", "id", "CASCADE");
        safeAddForeignKey("reader_appeal", "fk_rap_reader", "reader_id", "reader", "id", "CASCADE");
        log.info("=== 外键约束已就绪 ===");
    }

    private void safeAddForeignKey(String table, String fkName, String column, String refTable, String refColumn, String onDelete) {
        try {
            jdbcTemplate.execute("ALTER TABLE `" + table + "` ADD CONSTRAINT `" + fkName +
                "` FOREIGN KEY (`" + column + "`) REFERENCES `" + refTable + "`(`" + refColumn + "`) ON DELETE " + onDelete);
        } catch (Exception ignored) {
        }
    }

    private void initRbacData() {
        try {
            Long existingCount = permissionMapper.selectCount(new LambdaQueryWrapper<>());
            if (existingCount > 0) {
                log.info("=== RBAC数据已存在({}条权限)，跳过初始化 ===", existingCount);
                return;
            }

            // 插入权限数据 - 菜单级（管理端）
            insertPermission("dashboard:view", "数据概览", "menu", 0L, "/dashboard", "DataAnalysis", 1);
        insertPermission("book:manage", "图书管理", "menu", 0L, "/books", "Reading", 2);
        insertPermission("category:manage", "分类管理", "menu", 0L, "/categories", "Grid", 3);
        insertPermission("reader:manage", "读者管理", "menu", 0L, "/readers", "UserFilled", 4);
        insertPermission("borrow:manage", "借阅管理", "menu", 0L, "/borrows", "Notebook", 5);
        insertPermission("review:manage", "评价管理", "menu", 0L, "/reviews", "ChatDotRound", 6);
        insertPermission("resource:manage", "资源管理", "menu", 0L, "/resources", "Files", 7);
        insertPermission("admin-app:manage", "管理员审批", "menu", 0L, "/admin-applications", "Stamp", 8);
        insertPermission("appeal:manage", "申诉管理", "menu", 0L, "/appeals", "Warning", 9);
        insertPermission("reader-view:access", "读者系统", "menu", 0L, "/reader-view", "Monitor", 10);
        insertPermission("system:manage", "系统管理", "menu", 0L, "/roles", "Setting", 11);

        // 插入权限数据 - 菜单级（读者端）
        insertPermission("reader-center:manage", "读者中心", "menu", 0L, null, "House", 20);
        Long readerCenterParent = getPermissionId("reader-center:manage");
        insertPermission("reader-center:borrow", "我的借阅", "menu", readerCenterParent, "/reader/my-borrows", "Reading", 1);
        insertPermission("reader-center:reservation", "预约图书", "menu", readerCenterParent, "/reader/reservations", "Calendar", 2);
        insertPermission("reader-center:browse", "图书浏览", "menu", readerCenterParent, "/reader/books", "Search", 3);
        insertPermission("reader-center:recommend", "图书推荐", "menu", readerCenterParent, "/reader/recommend", "Star", 4);
        insertPermission("reader-center:progress", "阅读进度", "menu", readerCenterParent, "/reader/reading-progress", "TrendCharts", 5);
        insertPermission("reader-center:review", "我的评价", "menu", readerCenterParent, "/reader/my-reviews", "ChatLineRound", 6);
        insertPermission("reader-center:appeal", "我的申诉", "menu", readerCenterParent, "/reader/appeals", "Warning", 7);
        insertPermission("reader-center:profile", "个人中心", "menu", readerCenterParent, "/reader/profile", "User", 8);

        // 获取父权限ID
        Long bookParent = getPermissionId("book:manage");
        Long catParent = getPermissionId("category:manage");
        Long readerParent = getPermissionId("reader:manage");
        Long borrowParent = getPermissionId("borrow:manage");
        Long reviewParent = getPermissionId("review:manage");
        Long resourceParent = getPermissionId("resource:manage");
        Long adminAppParent = getPermissionId("admin-app:manage");
        Long systemParent = getPermissionId("system:manage");
        Long appealParent = getPermissionId("appeal:manage");

        // 图书按钮权限
        insertPermission("book:view", "查看图书", "button", bookParent, null, null, 1);
        insertPermission("book:add", "新增图书", "button", bookParent, null, null, 2);
        insertPermission("book:edit", "编辑图书", "button", bookParent, null, null, 3);
        insertPermission("book:delete", "删除图书", "button", bookParent, null, null, 4);

        // 分类按钮权限
        insertPermission("category:view", "查看分类", "button", catParent, null, null, 1);
        insertPermission("category:add", "新增分类", "button", catParent, null, null, 2);
        insertPermission("category:edit", "编辑分类", "button", catParent, null, null, 3);
        insertPermission("category:delete", "删除分类", "button", catParent, null, null, 4);

        // 读者按钮权限
        insertPermission("reader:view", "查看读者", "button", readerParent, null, null, 1);
        insertPermission("reader:add", "新增读者", "button", readerParent, null, null, 2);
        insertPermission("reader:edit", "编辑读者", "button", readerParent, null, null, 3);
        insertPermission("reader:delete", "删除读者", "button", readerParent, null, null, 4);
        insertPermission("reader:status", "变更读者状态", "button", readerParent, null, null, 5);

        // 借阅按钮权限
        insertPermission("borrow:view", "查看借阅", "button", borrowParent, null, null, 1);
        insertPermission("borrow:create", "创建借阅", "button", borrowParent, null, null, 2);
        insertPermission("borrow:return", "归还操作", "button", borrowParent, null, null, 3);
        insertPermission("borrow:renew", "续借操作", "button", borrowParent, null, null, 4);

        // 评价按钮权限
        insertPermission("review:view", "查看评价", "button", reviewParent, null, null, 1);
        insertPermission("review:approve", "审核通过", "button", reviewParent, null, null, 2);
        insertPermission("review:reject", "审核拒绝", "button", reviewParent, null, null, 3);
        insertPermission("review:reply", "回复评价", "button", reviewParent, null, null, 4);
        insertPermission("review:delete", "删除评价", "button", reviewParent, null, null, 5);

        // 资源按钮权限
        insertPermission("resource:view", "查看资源", "button", resourceParent, null, null, 1);
        insertPermission("resource:upload", "上传资源", "button", resourceParent, null, null, 2);
        insertPermission("resource:delete", "删除资源", "button", resourceParent, null, null, 3);

        // 管理员审批按钮权限
        insertPermission("admin-app:view", "查看申请", "button", adminAppParent, null, null, 1);
        insertPermission("admin-app:approve", "批准申请", "button", adminAppParent, null, null, 2);
        insertPermission("admin-app:reject", "拒绝申请", "button", adminAppParent, null, null, 3);

        // 申诉管理按钮权限
        insertPermission("appeal:view", "查看申诉", "button", appealParent, null, null, 1);
        insertPermission("appeal:handle", "处理申诉", "button", appealParent, null, null, 2);

        // 系统管理按钮权限
        insertPermission("role:view", "查看角色", "button", systemParent, null, null, 1);
        insertPermission("role:add", "新增角色", "button", systemParent, null, null, 2);
        insertPermission("role:edit", "编辑角色", "button", systemParent, null, null, 3);
        insertPermission("role:delete", "删除角色", "button", systemParent, null, null, 4);
        insertPermission("permission:view", "查看权限", "button", systemParent, null, null, 5);
        insertPermission("permission:add", "新增权限", "button", systemParent, null, null, 6);
        insertPermission("permission:edit", "编辑权限", "button", systemParent, null, null, 7);
        insertPermission("permission:delete", "删除权限", "button", systemParent, null, null, 8);
        insertPermission("permission:assign", "分配权限", "button", systemParent, null, null, 9);
        insertPermission("user-role:view", "查看用户角色", "button", systemParent, null, null, 10);
        insertPermission("user-role:assign", "分配用户角色", "button", systemParent, null, null, 11);

        // 创建角色
        SysRole superAdmin = new SysRole();
        superAdmin.setCode("super:admin");
        superAdmin.setName("超级管理员");
        superAdmin.setDescription("拥有系统全部权限");
        superAdmin.setStatus(1);
        roleMapper.insert(superAdmin);

        SysRole adminRole = new SysRole();
        adminRole.setCode("admin");
        adminRole.setName("管理员");
        adminRole.setDescription("拥有业务管理权限，无系统管理权限");
        adminRole.setStatus(1);
        roleMapper.insert(adminRole);

        SysRole readerRole = new SysRole();
        readerRole.setCode("reader");
        readerRole.setName("读者");
        readerRole.setDescription("拥有读者中心所有功能权限");
        readerRole.setStatus(1);
        roleMapper.insert(readerRole);

        // 为超级管理员分配全部权限
        List<SysPermission> allPerms = permissionMapper.selectList(new LambdaQueryWrapper<>());
        for (SysPermission perm : allPerms) {
            SysRolePermission rp = new SysRolePermission();
            rp.setRoleId(superAdmin.getId());
            rp.setPermissionId(perm.getId());
            rolePermissionMapper.insert(rp);
        }

        // 为管理员分配除系统管理和读者中心外的全部权限
        List<SysPermission> adminPerms = permissionMapper.selectList(
            new LambdaQueryWrapper<SysPermission>()
                .ne(SysPermission::getCode, "system:manage")
                .notLike(SysPermission::getCode, "role:")
                .notLike(SysPermission::getCode, "permission:")
                .notLike(SysPermission::getCode, "user-role:")
                .notLike(SysPermission::getCode, "reader-center:"));
        for (SysPermission perm : adminPerms) {
            SysRolePermission rp = new SysRolePermission();
            rp.setRoleId(adminRole.getId());
            rp.setPermissionId(perm.getId());
            rolePermissionMapper.insert(rp);
        }

        // 为读者角色分配读者中心相关权限
        List<SysPermission> readerPerms = permissionMapper.selectList(
            new LambdaQueryWrapper<SysPermission>()
                .likeRight(SysPermission::getCode, "reader-center:"));
        for (SysPermission perm : readerPerms) {
            SysRolePermission rp = new SysRolePermission();
            rp.setRoleId(readerRole.getId());
            rp.setPermissionId(perm.getId());
            rolePermissionMapper.insert(rp);
        }

        // 为默认admin用户分配超级管理员角色
        Admin admin = adminMapper.selectOne(
            new LambdaQueryWrapper<Admin>().eq(Admin::getUsername, "admin"));
        if (admin != null) {
            SysUserRole ur = new SysUserRole();
            ur.setUserType("admin");
            ur.setUserId(admin.getId());
            ur.setRoleId(superAdmin.getId());
            userRoleMapper.insert(ur);
        }

        // 为默认读者分配读者角色
        Reader defaultReader = readerMapper.selectOne(
            new LambdaQueryWrapper<Reader>().eq(Reader::getName, "小明"));
        if (defaultReader != null) {
            SysUserRole ur = new SysUserRole();
            ur.setUserType("reader");
            ur.setUserId(defaultReader.getId());
            ur.setRoleId(readerRole.getId());
            userRoleMapper.insert(ur);
        }

        log.info("=== RBAC权限数据初始化完成 ===");
        } catch (Exception e) {
            log.error("RBAC数据初始化失败: {}", e.getMessage(), e);
        }
    }

    private void insertPermission(String code, String name, String type, Long parentId, String path, String icon, int sortOrder) {
        SysPermission p = new SysPermission();
        p.setCode(code);
        p.setName(name);
        p.setType(type);
        p.setParentId(parentId);
        p.setPath(path);
        p.setIcon(icon);
        p.setSortOrder(sortOrder);
        p.setStatus(1);
        permissionMapper.insert(p);
    }

    private Long getPermissionId(String code) {
        SysPermission p = permissionMapper.selectOne(
            new LambdaQueryWrapper<SysPermission>().eq(SysPermission::getCode, code));
        return p != null ? p.getId() : 0L;
    }
}
