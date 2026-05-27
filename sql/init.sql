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
