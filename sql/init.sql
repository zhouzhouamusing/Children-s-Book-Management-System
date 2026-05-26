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
    `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
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
