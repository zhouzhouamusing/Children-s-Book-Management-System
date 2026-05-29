-- ============================================
-- 图书资源上传 & 评分评论功能 - 数据库迁移脚本
-- ============================================

-- 1. 图书资源文件表
CREATE TABLE IF NOT EXISTS `book_resource` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `book_id` BIGINT DEFAULT NULL COMMENT '关联图书ID',
  `file_name` VARCHAR(255) NOT NULL COMMENT '存储文件名(UUID)',
  `original_name` VARCHAR(255) NOT NULL COMMENT '原始文件名',
  `file_path` VARCHAR(500) NOT NULL COMMENT '相对存储路径',
  `file_type` VARCHAR(20) NOT NULL COMMENT '类型:cover/pdf/other',
  `file_size` BIGINT NOT NULL DEFAULT 0 COMMENT '文件大小(bytes)',
  `mime_type` VARCHAR(100) DEFAULT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_book_id` (`book_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图书资源文件表';

-- 2. 图书评价表
CREATE TABLE IF NOT EXISTS `book_review` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `book_id` BIGINT NOT NULL,
  `reader_id` BIGINT NOT NULL,
  `reader_name` VARCHAR(50) DEFAULT NULL,
  `book_title` VARCHAR(200) DEFAULT NULL,
  `rating` INT NOT NULL COMMENT '1-5星评分',
  `content` TEXT COMMENT '评价内容',
  `status` VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT 'pending/approved/rejected',
  `admin_reply` TEXT DEFAULT NULL COMMENT '管理员回复',
  `reply_time` DATETIME DEFAULT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_book_reader` (`book_id`, `reader_id`),
  KEY `idx_reader_id` (`reader_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图书评价表';

-- 3. 给book表添加评分相关字段
ALTER TABLE `book` ADD COLUMN IF NOT EXISTS `avg_rating` DECIMAL(2,1) DEFAULT 0.0 COMMENT '平均评分';
ALTER TABLE `book` ADD COLUMN IF NOT EXISTS `review_count` INT DEFAULT 0 COMMENT '评价数量';
