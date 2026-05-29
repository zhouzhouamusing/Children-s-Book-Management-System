-- 阅读进度表
CREATE TABLE IF NOT EXISTS `reading_progress` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `reader_id` BIGINT NOT NULL,
    `book_id` BIGINT NOT NULL,
    `book_title` VARCHAR(200) DEFAULT '',
    `total_pages` INT DEFAULT 0,
    `current_page` INT DEFAULT 0,
    `progress_percent` INT DEFAULT 0,
    `reading_minutes` INT DEFAULT 0,
    `status` VARCHAR(20) DEFAULT 'reading' COMMENT 'reading/completed/paused',
    `notes` TEXT,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_reader_id` (`reader_id`),
    INDEX `idx_book_id` (`book_id`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='阅读进度表';

-- 阅读笔记表
CREATE TABLE IF NOT EXISTS `reading_note` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `reader_id` BIGINT NOT NULL,
    `book_id` BIGINT NOT NULL,
    `progress_id` BIGINT,
    `book_title` VARCHAR(200) DEFAULT '',
    `content` TEXT NOT NULL,
    `page_number` INT DEFAULT 0,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_reader_id` (`reader_id`),
    INDEX `idx_book_id` (`book_id`),
    INDEX `idx_progress_id` (`progress_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='阅读笔记表';
