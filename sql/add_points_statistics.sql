-- 积分和阅读统计功能数据库迁移脚本
USE kids_book_db;

-- 为reader表添加积分和阅读天数字段
ALTER TABLE `reader`
    ADD COLUMN `points` INT DEFAULT 0 COMMENT '积分' AFTER `overdue_count`,
    ADD COLUMN `total_reading_days` INT DEFAULT 0 COMMENT '累计阅读天数' AFTER `points`,
    ADD COLUMN `level` VARCHAR(20) DEFAULT '新手读者' COMMENT '等级：新手读者/小书虫/阅读达人/阅读大师' AFTER `total_reading_days`;

-- 积分记录表（记录每次积分变动）
DROP TABLE IF EXISTS `reader_points_log`;
CREATE TABLE `reader_points_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `reader_id` BIGINT NOT NULL COMMENT '读者ID',
    `points` INT NOT NULL COMMENT '积分变动值（正为获得，负为消耗）',
    `type` VARCHAR(30) NOT NULL COMMENT '类型：borrow-借书 return_ontime-按时还书 return_early-提前还书 overdue_penalty-逾期扣分 reservation-预约取书 daily_read-每日阅读',
    `description` VARCHAR(200) DEFAULT NULL COMMENT '描述',
    `borrow_record_id` BIGINT DEFAULT NULL COMMENT '关联借阅记录ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_reader_id` (`reader_id`),
    KEY `idx_type` (`type`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='积分记录表';

-- 阅读统计表（按月统计）
DROP TABLE IF EXISTS `reader_monthly_stats`;
CREATE TABLE `reader_monthly_stats` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `reader_id` BIGINT NOT NULL COMMENT '读者ID',
    `year_month` VARCHAR(7) NOT NULL COMMENT '年月（格式：2026-05）',
    `borrow_count` INT DEFAULT 0 COMMENT '当月借阅次数',
    `return_count` INT DEFAULT 0 COMMENT '当月归还次数',
    `reading_days` INT DEFAULT 0 COMMENT '当月阅读天数',
    `points_earned` INT DEFAULT 0 COMMENT '当月获得积分',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_reader_month` (`reader_id`, `year_month`),
    KEY `idx_reader_id` (`reader_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='读者月度统计表';

-- 初始化现有读者的积分（基于历史借阅数据）
UPDATE `reader` SET `points` = `borrow_count` * 10, `total_reading_days` = `borrow_count`;

-- 设置等级
UPDATE `reader` SET `level` = CASE
    WHEN `points` >= 500 THEN '阅读大师'
    WHEN `points` >= 200 THEN '阅读达人'
    WHEN `points` >= 50 THEN '小书虫'
    ELSE '新手读者'
END;

-- 为现有借阅记录生成积分日志
INSERT INTO `reader_points_log` (`reader_id`, `points`, `type`, `description`, `borrow_record_id`, `create_time`)
SELECT `reader_id`, 10, 'borrow', CONCAT('借阅《', `book_title`, '》获得积分'), `id`, `borrow_date`
FROM `borrow_record`;

-- 为按时归还的记录额外加分
INSERT INTO `reader_points_log` (`reader_id`, `points`, `type`, `description`, `borrow_record_id`, `create_time`)
SELECT `reader_id`, 5, 'return_ontime', CONCAT('按时归还《', `book_title`, '》获得积分'), `id`, `return_date`
FROM `borrow_record`
WHERE `status` = 'returned' AND `return_date` <= `due_date`;

-- 逾期记录扣分
INSERT INTO `reader_points_log` (`reader_id`, `points`, `type`, `description`, `borrow_record_id`, `create_time`)
SELECT `reader_id`, -10, 'overdue_penalty', CONCAT('逾期归还《', `book_title`, '》扣除积分'), `id`, COALESCE(`return_date`, NOW())
FROM `borrow_record`
WHERE `status` IN ('overdue', 'returned') AND (`return_date` > `due_date` OR (`return_date` IS NULL AND `status` = 'overdue'));

-- 重新计算积分总额
UPDATE `reader` r SET `points` = (
    SELECT COALESCE(SUM(`points`), 0) FROM `reader_points_log` WHERE `reader_id` = r.`id`
);

-- 重新设置等级
UPDATE `reader` SET `level` = CASE
    WHEN `points` >= 500 THEN '阅读大师'
    WHEN `points` >= 200 THEN '阅读达人'
    WHEN `points` >= 50 THEN '小书虫'
    ELSE '新手读者'
END;
