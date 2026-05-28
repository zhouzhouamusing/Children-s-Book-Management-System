-- 儿童图书管理系统数据库初始化脚本
CREATE DATABASE IF NOT EXISTS kids_book_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE kids_book_db;

-- 管理员表
DROP TABLE IF EXISTS `admin`;
CREATE TABLE `admin` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `username` VARCHAR(50) NOT NULL COMMENT '用户名',
    `password` VARCHAR(200) NOT NULL COMMENT '密码(BCrypt加密)',
    `nickname` VARCHAR(50) DEFAULT '管理员' COMMENT '昵称',
    `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理员表';

-- 图书表
DROP TABLE IF EXISTS `book`;
CREATE TABLE `book` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `title` VARCHAR(200) NOT NULL COMMENT '书名',
    `author` VARCHAR(100) NOT NULL COMMENT '作者',
    `publisher` VARCHAR(100) DEFAULT NULL COMMENT '出版社',
    `isbn` VARCHAR(20) DEFAULT NULL COMMENT 'ISBN编号',
    `category` VARCHAR(50) DEFAULT NULL COMMENT '分类',
    `age_range` VARCHAR(20) DEFAULT NULL COMMENT '适读年龄',
    `price` DECIMAL(10,2) DEFAULT NULL COMMENT '价格',
    `stock` INT DEFAULT 0 COMMENT '库存数量',
    `cover_url` VARCHAR(255) DEFAULT NULL COMMENT '封面图片URL',
    `description` TEXT DEFAULT NULL COMMENT '简介',
    `status` TINYINT DEFAULT 1 COMMENT '状态：1-上架 0-下架',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_title` (`title`),
    KEY `idx_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图书表';

-- 注意：管理员账号由应用启动时 DataInitializer 自动创建
-- 默认账号: admin  默认密码: admin123
-- 无需手动插入admin记录，应用会自动处理

-- 示例图书数据
INSERT INTO `book` (`title`, `author`, `publisher`, `isbn`, `category`, `age_range`, `price`, `stock`, `description`, `status`) VALUES
('小王子', '安托万·德·圣-埃克苏佩里', '人民文学出版社', '9787020042494', '童话故事', '6-12岁', 25.00, 50, '一本关于爱与责任的永恒童话', 1),
('窗边的小豆豆', '黑柳彻子', '南海出版公司', '9787544250580', '成长故事', '6-10岁', 29.80, 35, '一个关于成长与教育的温暖故事', 1),
('夏洛的网', 'E.B.怀特', '上海译文出版社', '9787532748556', '童话故事', '8-12岁', 22.00, 40, '一只蜘蛛和一头小猪之间的友谊故事', 1),
('猜猜我有多爱你', '山姆·麦克布雷尼', '明天出版社', '9787533255725', '绘本', '3-6岁', 35.80, 60, '关于爱的表达的温馨绘本', 1),
('好饿的毛毛虫', '艾瑞·卡尔', '明天出版社', '9787533256739', '绘本', '2-5岁', 42.00, 45, '经典认知启蒙绘本', 1),
('草房子', '曹文轩', '江苏凤凰少年儿童出版社', '9787534618727', '成长故事', '8-14岁', 18.00, 30, '一部讲述少年成长的纯美小说', 1),
('安徒生童话', '安徒生', '人民文学出版社', '9787020008735', '童话故事', '5-12岁', 26.00, 55, '世界经典童话集', 1),
('三毛流浪记', '张乐平', '少年儿童出版社', '9787532474585', '漫画', '6-12岁', 28.00, 25, '中国经典漫画作品', 1);

-- 图书分类表
DROP TABLE IF EXISTS `category`;
CREATE TABLE `category` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name` VARCHAR(50) NOT NULL COMMENT '分类名称',
    `icon` VARCHAR(10) DEFAULT NULL COMMENT '分类图标(emoji)',
    `color` VARCHAR(20) DEFAULT NULL COMMENT '分类颜色',
    `age_range_min` INT DEFAULT 0 COMMENT '适龄最小值',
    `age_range_max` INT DEFAULT 14 COMMENT '适龄最大值',
    `sort_order` INT DEFAULT 0 COMMENT '排序权重(越大越靠前)',
    `description` VARCHAR(200) DEFAULT NULL COMMENT '分类描述',
    `status` TINYINT DEFAULT 1 COMMENT '状态：1-启用 0-禁用',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图书分类表';

-- 示例分类数据
INSERT INTO `category` (`name`, `icon`, `color`, `age_range_min`, `age_range_max`, `sort_order`, `description`, `status`) VALUES
('童话故事', '🧚', '#FFB3BA', 3, 12, 100, '经典童话与奇幻故事，激发孩子想象力', 1),
('成长故事', '🌱', '#B5EAD7', 6, 14, 90, '关于成长、友谊和勇气的温暖故事', 1),
('绘本', '🎨', '#C7CEEA', 0, 6, 95, '图文并茂的启蒙读物，适合亲子共读', 1),
('漫画', '💫', '#FFDAC1', 6, 14, 80, '趣味漫画作品，寓教于乐', 1),
('科普百科', '🔬', '#957DAD', 5, 14, 85, '探索自然与科学的奥秘', 1),
('国学经典', '📜', '#FFFFD1', 6, 14, 75, '传统文化与古典文学启蒙', 1),
('英文绘本', '🌍', '#E8F5E9', 2, 10, 70, '英语启蒙与双语阅读', 1);

-- 如果admin表已存在但缺少email字段，执行以下语句：
-- ALTER TABLE `admin` ADD COLUMN `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱' AFTER `nickname`;
-- ALTER TABLE `admin` ADD UNIQUE KEY `uk_email` (`email`);

-- 读者表
DROP TABLE IF EXISTS `reader`;
CREATE TABLE `reader` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name` VARCHAR(50) NOT NULL COMMENT '儿童姓名',
    `age` INT NOT NULL COMMENT '年龄',
    `gender` VARCHAR(10) NOT NULL COMMENT '性别：male-男 female-女',
    `parent_name` VARCHAR(50) DEFAULT NULL COMMENT '家长姓名',
    `parent_phone` VARCHAR(20) NOT NULL COMMENT '家长联系方式',
    `status` VARCHAR(20) DEFAULT 'normal' COMMENT '状态：normal-正常 suspended-暂停借阅',
    `borrow_count` INT DEFAULT 0 COMMENT '累计借阅数',
    `overdue_count` INT DEFAULT 0 COMMENT '逾期次数',
    `points` INT DEFAULT 0 COMMENT '积分',
    `total_reading_days` INT DEFAULT 0 COMMENT '累计阅读天数',
    `level` VARCHAR(20) DEFAULT '新手读者' COMMENT '等级：新手读者/小书虫/阅读达人/阅读大师',
    `remark` VARCHAR(200) DEFAULT NULL COMMENT '备注',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_name` (`name`),
    KEY `idx_parent_phone` (`parent_phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='读者表';

-- 借阅记录表
DROP TABLE IF EXISTS `borrow_record`;
CREATE TABLE `borrow_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `reader_id` BIGINT NOT NULL COMMENT '读者ID',
    `book_id` BIGINT DEFAULT NULL COMMENT '图书ID',
    `book_title` VARCHAR(200) NOT NULL COMMENT '图书名称',
    `borrow_date` DATE NOT NULL COMMENT '借阅日期',
    `due_date` DATE NOT NULL COMMENT '应还日期',
    `return_date` DATE DEFAULT NULL COMMENT '归还日期',
    `status` VARCHAR(20) DEFAULT 'borrowing' COMMENT '状态：borrowing-借阅中 returned-已归还 overdue-逾期',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_reader_id` (`reader_id`),
    KEY `idx_book_id` (`book_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='借阅记录表';

-- 示例读者数据
INSERT INTO `reader` (`name`, `age`, `gender`, `parent_name`, `parent_phone`, `status`, `borrow_count`, `overdue_count`, `points`, `total_reading_days`, `level`, `remark`) VALUES
('小明', 8, 'male', '张先生', '13800138001', 'normal', 12, 0, 120, 12, '小书虫', ''),
('小红', 6, 'female', '李女士', '13800138002', 'normal', 8, 1, 70, 8, '小书虫', '喜欢绘本'),
('小刚', 10, 'male', '王先生', '13800138003', 'suspended', 15, 4, 110, 15, '小书虫', '多次逾期，已暂停借阅'),
('小美', 7, 'female', '赵女士', '13800138004', 'normal', 5, 0, 75, 5, '小书虫', '对科普类感兴趣'),
('小杰', 9, 'male', '刘先生', '13800138005', 'normal', 20, 2, 180, 20, '小书虫', ''),
('小雪', 5, 'female', '陈女士', '13800138006', 'normal', 3, 0, 45, 3, '新手读者', '刚入学');

-- 示例借阅记录
INSERT INTO `borrow_record` (`reader_id`, `book_id`, `book_title`, `borrow_date`, `due_date`, `return_date`, `status`) VALUES
(1, 1, '小王子', '2025-12-01', '2025-12-15', '2025-12-14', 'returned'),
(1, 7, '安徒生童话', '2026-01-05', '2026-01-19', '2026-01-18', 'returned'),
(1, 5, '好饿的毛毛虫', '2026-03-01', '2026-03-15', '2026-03-10', 'returned'),
(2, 4, '猜猜我有多爱你', '2025-11-20', '2025-12-04', '2025-12-03', 'returned'),
(2, 5, '好饿的毛毛虫', '2026-01-10', '2026-01-24', '2026-01-28', 'returned'),
(2, 3, '夏洛的网', '2026-04-01', '2026-04-15', NULL, 'borrowing'),
(3, 1, '小王子', '2025-10-01', '2025-10-15', '2025-10-20', 'returned'),
(3, 6, '草房子', '2025-11-01', '2025-11-15', '2025-11-25', 'returned'),
(3, 2, '窗边的小豆豆', '2025-12-01', '2025-12-15', '2025-12-28', 'returned'),
(3, 7, '安徒生童话', '2026-01-10', '2026-01-24', NULL, 'overdue'),
(4, 5, '好饿的毛毛虫', '2026-02-10', '2026-02-24', '2026-02-20', 'returned'),
(4, 4, '猜猜我有多爱你', '2026-04-05', '2026-04-19', NULL, 'borrowing'),
(5, 6, '草房子', '2025-12-15', '2025-12-29', '2025-12-28', 'returned'),
(5, 8, '三毛流浪记', '2026-01-20', '2026-02-03', '2026-02-10', 'returned'),
(5, 1, '小王子', '2026-03-10', '2026-03-24', NULL, 'overdue'),
(6, 4, '猜猜我有多爱你', '2026-03-15', '2026-03-29', '2026-03-25', 'returned');

-- 读者账号表（用于读者登录）
DROP TABLE IF EXISTS `reader_account`;
CREATE TABLE `reader_account` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `username` VARCHAR(50) NOT NULL COMMENT '用户名',
    `password` VARCHAR(200) NOT NULL COMMENT '密码(BCrypt加密)',
    `reader_id` BIGINT NOT NULL COMMENT '关联读者ID',
    `status` VARCHAR(20) DEFAULT 'active' COMMENT '状态：active-正常 disabled-禁用',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_reader_id` (`reader_id`),
    KEY `idx_reader_id` (`reader_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='读者账号表';

-- 默认读者账号: xiaoming / 123456 (关联读者ID=1 小明)
-- 密码为BCrypt加密后的123456
INSERT INTO `reader_account` (`username`, `password`, `reader_id`, `status`) VALUES
('xiaoming', '$2a$10$N.ZOn9G6/YOoTISRkp3v0.J7j6GfKOdJOWfiJNGtZDyA9C6RiAHmO', 1, 'active'),
('xiaohong', '$2a$10$N.ZOn9G6/YOoTISRkp3v0.J7j6GfKOdJOWfiJNGtZDyA9C6RiAHmO', 2, 'active');

-- 图书预约表
DROP TABLE IF EXISTS `book_reservation`;
CREATE TABLE `book_reservation` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `reader_id` BIGINT NOT NULL COMMENT '读者ID',
    `book_id` BIGINT NOT NULL COMMENT '图书ID',
    `book_title` VARCHAR(200) NOT NULL COMMENT '图书名称',
    `reserve_date` DATETIME NOT NULL COMMENT '预约日期',
    `expire_date` DATETIME NOT NULL COMMENT '过期日期',
    `status` VARCHAR(20) DEFAULT 'pending' COMMENT '状态：pending-待取书 fulfilled-已取书 cancelled-已取消 expired-已过期',
    `remark` VARCHAR(200) DEFAULT NULL COMMENT '备注',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_reader_id` (`reader_id`),
    KEY `idx_book_id` (`book_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图书预约表';

-- 示例预约数据
INSERT INTO `book_reservation` (`reader_id`, `book_id`, `book_title`, `reserve_date`, `expire_date`, `status`) VALUES
(1, 3, '夏洛的网', '2026-05-25 10:00:00', '2026-05-28 10:00:00', 'pending'),
(1, 6, '草房子', '2026-05-20 14:00:00', '2026-05-23 14:00:00', 'fulfilled'),
(2, 1, '小王子', '2026-05-26 09:00:00', '2026-05-29 09:00:00', 'pending');

-- 积分记录表
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

-- 读者月度统计表
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

-- 管理员申请表
DROP TABLE IF EXISTS `admin_application`;
CREATE TABLE `admin_application` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `reader_id` BIGINT NOT NULL COMMENT '读者ID',
    `reader_name` VARCHAR(50) DEFAULT NULL COMMENT '读者姓名',
    `username` VARCHAR(50) DEFAULT NULL COMMENT '读者账号用户名',
    `reason` VARCHAR(500) NOT NULL COMMENT '申请理由',
    `status` VARCHAR(20) DEFAULT 'pending' COMMENT '状态：pending-待审批 approved-已通过 rejected-已拒绝',
    `reject_reason` VARCHAR(200) DEFAULT NULL COMMENT '拒绝原因',
    `approved_by` BIGINT DEFAULT NULL COMMENT '审批人ID',
    `approved_time` DATETIME DEFAULT NULL COMMENT '审批时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_reader_id` (`reader_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理员申请表';

-- 示例积分记录
INSERT INTO `reader_points_log` (`reader_id`, `points`, `type`, `description`, `borrow_record_id`, `create_time`) VALUES
(1, 10, 'borrow', '借阅《小王子》', 1, '2025-12-01 10:00:00'),
(1, 5, 'return_ontime', '按时归还《小王子》', 1, '2025-12-14 10:00:00'),
(1, 10, 'borrow', '借阅《安徒生童话》', 2, '2026-01-05 10:00:00'),
(1, 5, 'return_ontime', '按时归还《安徒生童话》', 2, '2026-01-18 10:00:00'),
(1, 10, 'borrow', '借阅《好饿的毛毛虫》', 3, '2026-03-01 10:00:00'),
(1, 5, 'return_ontime', '按时归还《好饿的毛毛虫》', 3, '2026-03-10 10:00:00'),
(2, 10, 'borrow', '借阅《猜猜我有多爱你》', 4, '2025-11-20 10:00:00'),
(2, 5, 'return_ontime', '按时归还《猜猜我有多爱你》', 4, '2025-12-03 10:00:00'),
(2, 10, 'borrow', '借阅《好饿的毛毛虫》', 5, '2026-01-10 10:00:00'),
(2, -10, 'overdue_penalty', '逾期归还《好饿的毛毛虫》', 5, '2026-01-28 10:00:00'),
(2, 10, 'borrow', '借阅《夏洛的网》', 6, '2026-04-01 10:00:00');
