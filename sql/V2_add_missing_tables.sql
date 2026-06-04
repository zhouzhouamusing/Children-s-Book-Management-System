-- ============================================
-- V2 幂等迁移脚本 - 为已有数据库补充缺失表和字段
-- 适用于已执行 init.sql 但未执行后续迁移脚本的数据库
-- 可安全重复执行
-- ============================================

USE kids_book_db;

-- 1. book 表添加评分字段
ALTER TABLE `book` ADD COLUMN IF NOT EXISTS `avg_rating` DECIMAL(2,1) DEFAULT 0.0 COMMENT '平均评分';
ALTER TABLE `book` ADD COLUMN IF NOT EXISTS `review_count` INT DEFAULT 0 COMMENT '评价数量';

-- 2. reader 表添加积分相关字段（如果缺失）
ALTER TABLE `reader` ADD COLUMN IF NOT EXISTS `points` INT DEFAULT 0 COMMENT '积分';
ALTER TABLE `reader` ADD COLUMN IF NOT EXISTS `total_reading_days` INT DEFAULT 0 COMMENT '累计阅读天数';
ALTER TABLE `reader` ADD COLUMN IF NOT EXISTS `level` VARCHAR(20) DEFAULT '新手读者' COMMENT '等级';

-- 3. 阅读进度表
CREATE TABLE IF NOT EXISTS `reading_progress` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `reader_id` BIGINT NOT NULL COMMENT '读者ID',
    `book_id` BIGINT NOT NULL COMMENT '图书ID',
    `book_title` VARCHAR(200) DEFAULT '' COMMENT '图书名称',
    `total_pages` INT DEFAULT 0 COMMENT '总页数',
    `current_page` INT DEFAULT 0 COMMENT '当前页码',
    `progress_percent` INT DEFAULT 0 COMMENT '进度百分比',
    `reading_minutes` INT DEFAULT 0 COMMENT '累计阅读分钟数',
    `status` VARCHAR(20) DEFAULT 'reading' COMMENT '状态：reading/completed/paused',
    `notes` TEXT COMMENT '备注',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_reader_id` (`reader_id`),
    INDEX `idx_book_id` (`book_id`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='阅读进度表';

-- 4. 阅读笔记表
CREATE TABLE IF NOT EXISTS `reading_note` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `reader_id` BIGINT NOT NULL COMMENT '读者ID',
    `book_id` BIGINT NOT NULL COMMENT '图书ID',
    `progress_id` BIGINT COMMENT '关联阅读进度ID',
    `book_title` VARCHAR(200) DEFAULT '' COMMENT '图书名称',
    `content` TEXT NOT NULL COMMENT '笔记内容',
    `page_number` INT DEFAULT 0 COMMENT '所在页码',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_reader_id` (`reader_id`),
    INDEX `idx_book_id` (`book_id`),
    INDEX `idx_progress_id` (`progress_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='阅读笔记表';

-- 5. 图书资源文件表
CREATE TABLE IF NOT EXISTS `book_resource` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `book_id` BIGINT DEFAULT NULL COMMENT '关联图书ID',
    `file_name` VARCHAR(255) NOT NULL COMMENT '存储文件名(UUID)',
    `original_name` VARCHAR(255) NOT NULL COMMENT '原始文件名',
    `file_path` VARCHAR(500) NOT NULL COMMENT '相对存储路径',
    `file_type` VARCHAR(20) NOT NULL COMMENT '类型：cover/pdf/other',
    `file_size` BIGINT NOT NULL DEFAULT 0 COMMENT '文件大小(bytes)',
    `mime_type` VARCHAR(100) DEFAULT NULL COMMENT 'MIME类型',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_book_id` (`book_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图书资源文件表';

-- 6. 图书评价表
CREATE TABLE IF NOT EXISTS `book_review` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `book_id` BIGINT NOT NULL COMMENT '图书ID',
    `reader_id` BIGINT NOT NULL COMMENT '读者ID',
    `reader_name` VARCHAR(50) DEFAULT NULL COMMENT '读者姓名',
    `book_title` VARCHAR(200) DEFAULT NULL COMMENT '图书名称',
    `rating` INT NOT NULL COMMENT '1-5星评分',
    `content` TEXT COMMENT '评价内容',
    `status` VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '状态：pending/approved/rejected',
    `admin_reply` TEXT DEFAULT NULL COMMENT '管理员回复',
    `reply_time` DATETIME DEFAULT NULL COMMENT '回复时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_book_reader` (`book_id`, `reader_id`),
    KEY `idx_reader_id` (`reader_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图书评价表';

-- 7. 积分记录表（如果缺失）
CREATE TABLE IF NOT EXISTS `reader_points_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `reader_id` BIGINT NOT NULL COMMENT '读者ID',
    `points` INT NOT NULL COMMENT '积分变动值',
    `type` VARCHAR(30) NOT NULL COMMENT '类型',
    `description` VARCHAR(200) DEFAULT NULL COMMENT '描述',
    `borrow_record_id` BIGINT DEFAULT NULL COMMENT '关联借阅记录ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_reader_id` (`reader_id`),
    KEY `idx_type` (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='积分记录表';

-- 8. 月度统计表（如果缺失）
CREATE TABLE IF NOT EXISTS `reader_monthly_stats` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `reader_id` BIGINT NOT NULL COMMENT '读者ID',
    `year_month` VARCHAR(7) NOT NULL COMMENT '年月',
    `borrow_count` INT DEFAULT 0,
    `return_count` INT DEFAULT 0,
    `reading_days` INT DEFAULT 0,
    `points_earned` INT DEFAULT 0,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_reader_month` (`reader_id`, `year_month`),
    KEY `idx_reader_id` (`reader_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='读者月度统计表';

-- 9. 审计日志表
CREATE TABLE IF NOT EXISTS `audit_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `operator_username` VARCHAR(50) DEFAULT NULL COMMENT '操作人用户名',
    `operator_role` VARCHAR(20) DEFAULT NULL COMMENT '操作人角色',
    `action` VARCHAR(50) NOT NULL COMMENT '操作类型',
    `target_type` VARCHAR(50) DEFAULT NULL COMMENT '目标类型',
    `target_id` BIGINT DEFAULT NULL COMMENT '目标ID',
    `detail` VARCHAR(500) DEFAULT NULL COMMENT '详情',
    `ip_address` VARCHAR(50) DEFAULT NULL COMMENT 'IP地址',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_action` (`action`),
    KEY `idx_operator` (`operator_username`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审计日志表';

-- 10. 管理员申请表
CREATE TABLE IF NOT EXISTS `admin_application` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `reader_id` BIGINT NOT NULL COMMENT '读者ID',
    `reader_name` VARCHAR(50) DEFAULT NULL COMMENT '读者姓名',
    `username` VARCHAR(50) DEFAULT NULL COMMENT '申请用户名',
    `reason` TEXT COMMENT '申请理由',
    `status` VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '状态：pending/approved/rejected',
    `reject_reason` VARCHAR(200) DEFAULT NULL COMMENT '拒绝原因',
    `approved_by` VARCHAR(50) DEFAULT NULL COMMENT '审批人',
    `approved_time` DATETIME DEFAULT NULL COMMENT '审批时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_reader_id` (`reader_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理员申请表';

-- 11. reader表添加suspend_reason字段
ALTER TABLE `reader` ADD COLUMN IF NOT EXISTS `suspend_reason` VARCHAR(20) DEFAULT NULL COMMENT '暂停原因：overdue/manual';

-- 12. reader_account表添加email字段
ALTER TABLE `reader_account` ADD COLUMN IF NOT EXISTS `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱地址';
