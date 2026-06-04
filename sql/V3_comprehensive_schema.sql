-- V3: Comprehensive Schema - All Tables
-- This is an idempotent migration script covering all 14 tables in the KidsBook system.
-- Safe to run on existing databases (uses CREATE TABLE IF NOT EXISTS).

-- ============================================
-- 1. Admin accounts
-- ============================================
CREATE TABLE IF NOT EXISTS `admin` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(50) NOT NULL,
  `password` VARCHAR(200) NOT NULL,
  `nickname` VARCHAR(50) DEFAULT NULL,
  `email` VARCHAR(100) DEFAULT NULL,
  `avatar` VARCHAR(500) DEFAULT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_admin_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================
-- 2. Reader profiles
-- ============================================
CREATE TABLE IF NOT EXISTS `reader` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(50) NOT NULL,
  `age` INT DEFAULT NULL,
  `gender` VARCHAR(10) DEFAULT NULL,
  `parent_name` VARCHAR(50) DEFAULT NULL,
  `parent_phone` VARCHAR(20) DEFAULT NULL,
  `status` VARCHAR(20) DEFAULT 'normal',
  `suspend_reason` VARCHAR(20) DEFAULT NULL,
  `borrow_count` INT DEFAULT 0,
  `overdue_count` INT DEFAULT 0,
  `points` INT DEFAULT 0,
  `total_reading_days` INT DEFAULT 0,
  `level` VARCHAR(30) DEFAULT NULL,
  `remark` TEXT,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================
-- 3. Reader login accounts
-- ============================================
CREATE TABLE IF NOT EXISTS `reader_account` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(50) NOT NULL,
  `password` VARCHAR(200) NOT NULL,
  `reader_id` BIGINT NOT NULL,
  `email` VARCHAR(100) DEFAULT NULL,
  `status` VARCHAR(20) DEFAULT 'active',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ra_username` (`username`),
  KEY `idx_ra_reader_id` (`reader_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================
-- 4. Book catalog
-- ============================================
CREATE TABLE IF NOT EXISTS `book` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `title` VARCHAR(200) NOT NULL,
  `author` VARCHAR(100) DEFAULT NULL,
  `publisher` VARCHAR(100) DEFAULT NULL,
  `isbn` VARCHAR(30) DEFAULT NULL,
  `category` VARCHAR(50) DEFAULT NULL,
  `age_range` VARCHAR(20) DEFAULT NULL,
  `price` DECIMAL(10,2) DEFAULT NULL,
  `stock` INT DEFAULT 0,
  `cover_url` VARCHAR(500) DEFAULT NULL,
  `description` TEXT,
  `status` INT DEFAULT 1,
  `avg_rating` DECIMAL(2,1) DEFAULT 0.0,
  `review_count` INT DEFAULT 0,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_book_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================
-- 5. Book categories
-- ============================================
CREATE TABLE IF NOT EXISTS `category` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(50) NOT NULL,
  `icon` VARCHAR(50) DEFAULT NULL,
  `color` VARCHAR(20) DEFAULT NULL,
  `age_range_min` INT DEFAULT NULL,
  `age_range_max` INT DEFAULT NULL,
  `sort_order` INT DEFAULT 0,
  `description` VARCHAR(200) DEFAULT NULL,
  `status` INT DEFAULT 1,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_category_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================
-- 6. Borrow records
-- ============================================
CREATE TABLE IF NOT EXISTS `borrow_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `reader_id` BIGINT NOT NULL,
  `book_id` BIGINT NOT NULL,
  `book_title` VARCHAR(200) DEFAULT NULL,
  `borrow_date` DATE NOT NULL,
  `due_date` DATE NOT NULL,
  `return_date` DATE DEFAULT NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'borrowing',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_br_reader_id` (`reader_id`),
  KEY `idx_br_book_id` (`book_id`),
  KEY `idx_br_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================
-- 7. Book reservations
-- ============================================
CREATE TABLE IF NOT EXISTS `book_reservation` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `reader_id` BIGINT NOT NULL,
  `book_id` BIGINT NOT NULL,
  `book_title` VARCHAR(200) DEFAULT NULL,
  `reserve_date` DATETIME NOT NULL,
  `expire_date` DATETIME NOT NULL,
  `status` VARCHAR(20) DEFAULT 'pending',
  `remark` VARCHAR(200) DEFAULT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_bres_reader_id` (`reader_id`),
  KEY `idx_bres_book_id` (`book_id`),
  KEY `idx_bres_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================
-- 8. Reading progress
-- ============================================
CREATE TABLE IF NOT EXISTS `reading_progress` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `reader_id` BIGINT NOT NULL,
  `book_id` BIGINT NOT NULL,
  `book_title` VARCHAR(200) DEFAULT '',
  `total_pages` INT DEFAULT 0,
  `current_page` INT DEFAULT 0,
  `progress_percent` INT DEFAULT 0,
  `reading_minutes` INT DEFAULT 0,
  `status` VARCHAR(20) DEFAULT 'reading',
  `notes` TEXT,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_rp_reader_id` (`reader_id`),
  KEY `idx_rp_book_id` (`book_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================
-- 9. Reading notes
-- ============================================
CREATE TABLE IF NOT EXISTS `reading_note` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `reader_id` BIGINT NOT NULL,
  `book_id` BIGINT NOT NULL,
  `progress_id` BIGINT DEFAULT NULL,
  `book_title` VARCHAR(200) DEFAULT '',
  `content` TEXT NOT NULL,
  `page_number` INT DEFAULT 0,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_rn_reader_id` (`reader_id`),
  KEY `idx_rn_book_id` (`book_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================
-- 10. Book resources (uploaded files)
-- ============================================
CREATE TABLE IF NOT EXISTS `book_resource` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `book_id` BIGINT DEFAULT NULL,
  `file_name` VARCHAR(255) NOT NULL,
  `original_name` VARCHAR(255) NOT NULL,
  `file_path` VARCHAR(500) NOT NULL,
  `file_type` VARCHAR(20) NOT NULL,
  `file_size` BIGINT NOT NULL DEFAULT 0,
  `mime_type` VARCHAR(100) DEFAULT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_bres_book_id` (`book_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================
-- 11. Book reviews
-- ============================================
CREATE TABLE IF NOT EXISTS `book_review` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `book_id` BIGINT NOT NULL,
  `reader_id` BIGINT NOT NULL,
  `reader_name` VARCHAR(50) DEFAULT NULL,
  `book_title` VARCHAR(200) DEFAULT NULL,
  `rating` INT NOT NULL,
  `content` TEXT,
  `status` VARCHAR(20) NOT NULL DEFAULT 'pending',
  `admin_reply` TEXT DEFAULT NULL,
  `reply_time` DATETIME DEFAULT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_book_reader` (`book_id`, `reader_id`),
  KEY `idx_brev_reader_id` (`reader_id`),
  KEY `idx_brev_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================
-- 12. Reader points log
-- ============================================
CREATE TABLE IF NOT EXISTS `reader_points_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `reader_id` BIGINT NOT NULL,
  `points` INT NOT NULL,
  `type` VARCHAR(30) NOT NULL,
  `description` VARCHAR(200) DEFAULT NULL,
  `borrow_record_id` BIGINT DEFAULT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_rpl_reader_id` (`reader_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================
-- 13. Reader monthly statistics
-- ============================================
CREATE TABLE IF NOT EXISTS `reader_monthly_stats` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `reader_id` BIGINT NOT NULL,
  `year_month` VARCHAR(7) NOT NULL,
  `borrow_count` INT DEFAULT 0,
  `return_count` INT DEFAULT 0,
  `reading_days` INT DEFAULT 0,
  `points_earned` INT DEFAULT 0,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_reader_month` (`reader_id`, `year_month`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================
-- 14. Audit log
-- ============================================
CREATE TABLE IF NOT EXISTS `audit_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `operator_username` VARCHAR(50) DEFAULT NULL,
  `operator_role` VARCHAR(20) DEFAULT NULL,
  `action` VARCHAR(50) NOT NULL,
  `target_type` VARCHAR(50) DEFAULT NULL,
  `target_id` BIGINT DEFAULT NULL,
  `detail` VARCHAR(500) DEFAULT NULL,
  `ip_address` VARCHAR(50) DEFAULT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_al_action` (`action`),
  KEY `idx_al_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================
-- 15. Admin applications
-- ============================================
CREATE TABLE IF NOT EXISTS `admin_application` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `reader_id` BIGINT NOT NULL,
  `reader_name` VARCHAR(50) DEFAULT NULL,
  `username` VARCHAR(50) DEFAULT NULL,
  `reason` TEXT,
  `status` VARCHAR(20) NOT NULL DEFAULT 'pending',
  `reject_reason` VARCHAR(200) DEFAULT NULL,
  `approved_by` VARCHAR(50) DEFAULT NULL,
  `approved_time` DATETIME DEFAULT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_aa_reader_id` (`reader_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
