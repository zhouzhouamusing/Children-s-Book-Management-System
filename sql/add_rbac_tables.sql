-- RBAC权限管理系统 - 完整初始化脚本
-- 如果遇到表结构不完整的问题，可以手动执行此脚本重建RBAC表
-- 注意：执行此脚本会删除所有已有的角色和权限数据！

USE kids_book_db;

-- 按依赖顺序删除旧表
DROP TABLE IF EXISTS `sys_user_role`;
DROP TABLE IF EXISTS `sys_role_permission`;
DROP TABLE IF EXISTS `sys_role`;
DROP TABLE IF EXISTS `sys_permission`;

-- 系统权限表
CREATE TABLE `sys_permission` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `code` VARCHAR(100) NOT NULL COMMENT '权限编码',
  `name` VARCHAR(100) NOT NULL COMMENT '权限名称',
  `type` VARCHAR(20) NOT NULL DEFAULT 'button' COMMENT '类型: menu/button',
  `parent_id` BIGINT DEFAULT 0 COMMENT '父权限ID，0为顶级',
  `path` VARCHAR(200) DEFAULT NULL COMMENT '前端路由路径',
  `icon` VARCHAR(50) DEFAULT NULL COMMENT '菜单图标名',
  `sort_order` INT DEFAULT 0 COMMENT '排序',
  `status` TINYINT DEFAULT 1 COMMENT '1启用 0禁用',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_code` (`code`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统权限表';

-- 系统角色表
CREATE TABLE `sys_role` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `code` VARCHAR(50) NOT NULL COMMENT '角色编码',
  `name` VARCHAR(50) NOT NULL COMMENT '角色名称',
  `description` VARCHAR(200) DEFAULT NULL COMMENT '描述',
  `status` TINYINT DEFAULT 1 COMMENT '1启用 0禁用',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统角色表';

-- 角色-权限关联表
CREATE TABLE `sys_role_permission` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `role_id` BIGINT NOT NULL,
  `permission_id` BIGINT NOT NULL,
  UNIQUE KEY `uk_role_perm` (`role_id`, `permission_id`),
  KEY `idx_permission_id` (`permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色-权限关联表';

-- 用户-角色关联表
CREATE TABLE `sys_user_role` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `user_type` VARCHAR(20) NOT NULL COMMENT '用户类型: admin/reader',
  `user_id` BIGINT NOT NULL,
  `role_id` BIGINT NOT NULL,
  UNIQUE KEY `uk_user_role` (`user_type`, `user_id`, `role_id`),
  KEY `idx_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户-角色关联表';

-- ==================== 插入权限种子数据 ====================

-- 菜单权限
INSERT INTO `sys_permission` (`code`, `name`, `type`, `parent_id`, `path`, `icon`, `sort_order`) VALUES
('dashboard:view', '数据概览', 'menu', 0, '/dashboard', 'DataAnalysis', 1),
('book:manage', '图书管理', 'menu', 0, '/books', 'Reading', 2),
('category:manage', '分类管理', 'menu', 0, '/categories', 'FolderOpened', 3),
('reader:manage', '读者管理', 'menu', 0, '/readers', 'UserFilled', 4),
('borrow:manage', '借阅管理', 'menu', 0, '/borrows', 'Notebook', 5),
('review:manage', '评价管理', 'menu', 0, '/reviews', 'ChatDotRound', 6),
('resource:manage', '资源管理', 'menu', 0, '/resources', 'FolderOpened', 7),
('admin-app:manage', '管理员审批', 'menu', 0, '/admin-applications', 'Stamp', 8),
('reader-view:access', '读者系统', 'menu', 0, '/reader-view', 'View', 9),
('system:manage', '系统管理', 'menu', 0, NULL, 'Setting', 10);

-- 图书按钮权限
INSERT INTO `sys_permission` (`code`, `name`, `type`, `parent_id`, `sort_order`) VALUES
('book:view', '查看图书', 'button', (SELECT id FROM (SELECT id FROM sys_permission WHERE code='book:manage') t), 1),
('book:add', '新增图书', 'button', (SELECT id FROM (SELECT id FROM sys_permission WHERE code='book:manage') t), 2),
('book:edit', '编辑图书', 'button', (SELECT id FROM (SELECT id FROM sys_permission WHERE code='book:manage') t), 3),
('book:delete', '删除图书', 'button', (SELECT id FROM (SELECT id FROM sys_permission WHERE code='book:manage') t), 4);

-- 分类按钮权限
INSERT INTO `sys_permission` (`code`, `name`, `type`, `parent_id`, `sort_order`) VALUES
('category:view', '查看分类', 'button', (SELECT id FROM (SELECT id FROM sys_permission WHERE code='category:manage') t), 1),
('category:add', '新增分类', 'button', (SELECT id FROM (SELECT id FROM sys_permission WHERE code='category:manage') t), 2),
('category:edit', '编辑分类', 'button', (SELECT id FROM (SELECT id FROM sys_permission WHERE code='category:manage') t), 3),
('category:delete', '删除分类', 'button', (SELECT id FROM (SELECT id FROM sys_permission WHERE code='category:manage') t), 4);

-- 读者按钮权限
INSERT INTO `sys_permission` (`code`, `name`, `type`, `parent_id`, `sort_order`) VALUES
('reader:view', '查看读者', 'button', (SELECT id FROM (SELECT id FROM sys_permission WHERE code='reader:manage') t), 1),
('reader:add', '新增读者', 'button', (SELECT id FROM (SELECT id FROM sys_permission WHERE code='reader:manage') t), 2),
('reader:edit', '编辑读者', 'button', (SELECT id FROM (SELECT id FROM sys_permission WHERE code='reader:manage') t), 3),
('reader:delete', '删除读者', 'button', (SELECT id FROM (SELECT id FROM sys_permission WHERE code='reader:manage') t), 4),
('reader:status', '变更读者状态', 'button', (SELECT id FROM (SELECT id FROM sys_permission WHERE code='reader:manage') t), 5);

-- 借阅按钮权限
INSERT INTO `sys_permission` (`code`, `name`, `type`, `parent_id`, `sort_order`) VALUES
('borrow:view', '查看借阅', 'button', (SELECT id FROM (SELECT id FROM sys_permission WHERE code='borrow:manage') t), 1),
('borrow:create', '创建借阅', 'button', (SELECT id FROM (SELECT id FROM sys_permission WHERE code='borrow:manage') t), 2),
('borrow:return', '归还操作', 'button', (SELECT id FROM (SELECT id FROM sys_permission WHERE code='borrow:manage') t), 3),
('borrow:renew', '续借操作', 'button', (SELECT id FROM (SELECT id FROM sys_permission WHERE code='borrow:manage') t), 4);

-- 评价按钮权限
INSERT INTO `sys_permission` (`code`, `name`, `type`, `parent_id`, `sort_order`) VALUES
('review:view', '查看评价', 'button', (SELECT id FROM (SELECT id FROM sys_permission WHERE code='review:manage') t), 1),
('review:approve', '审核通过', 'button', (SELECT id FROM (SELECT id FROM sys_permission WHERE code='review:manage') t), 2),
('review:reject', '审核拒绝', 'button', (SELECT id FROM (SELECT id FROM sys_permission WHERE code='review:manage') t), 3),
('review:reply', '回复评价', 'button', (SELECT id FROM (SELECT id FROM sys_permission WHERE code='review:manage') t), 4),
('review:delete', '删除评价', 'button', (SELECT id FROM (SELECT id FROM sys_permission WHERE code='review:manage') t), 5);

-- 资源按钮权限
INSERT INTO `sys_permission` (`code`, `name`, `type`, `parent_id`, `sort_order`) VALUES
('resource:view', '查看资源', 'button', (SELECT id FROM (SELECT id FROM sys_permission WHERE code='resource:manage') t), 1),
('resource:upload', '上传资源', 'button', (SELECT id FROM (SELECT id FROM sys_permission WHERE code='resource:manage') t), 2),
('resource:delete', '删除资源', 'button', (SELECT id FROM (SELECT id FROM sys_permission WHERE code='resource:manage') t), 3);

-- 管理员审批按钮权限
INSERT INTO `sys_permission` (`code`, `name`, `type`, `parent_id`, `sort_order`) VALUES
('admin-app:view', '查看申请', 'button', (SELECT id FROM (SELECT id FROM sys_permission WHERE code='admin-app:manage') t), 1),
('admin-app:approve', '批准申请', 'button', (SELECT id FROM (SELECT id FROM sys_permission WHERE code='admin-app:manage') t), 2),
('admin-app:reject', '拒绝申请', 'button', (SELECT id FROM (SELECT id FROM sys_permission WHERE code='admin-app:manage') t), 3);

-- 系统管理按钮权限
INSERT INTO `sys_permission` (`code`, `name`, `type`, `parent_id`, `sort_order`) VALUES
('role:view', '查看角色', 'button', (SELECT id FROM (SELECT id FROM sys_permission WHERE code='system:manage') t), 1),
('role:add', '新增角色', 'button', (SELECT id FROM (SELECT id FROM sys_permission WHERE code='system:manage') t), 2),
('role:edit', '编辑角色', 'button', (SELECT id FROM (SELECT id FROM sys_permission WHERE code='system:manage') t), 3),
('role:delete', '删除角色', 'button', (SELECT id FROM (SELECT id FROM sys_permission WHERE code='system:manage') t), 4),
('permission:view', '查看权限', 'button', (SELECT id FROM (SELECT id FROM sys_permission WHERE code='system:manage') t), 5),
('permission:assign', '分配权限', 'button', (SELECT id FROM (SELECT id FROM sys_permission WHERE code='system:manage') t), 6),
('user-role:view', '查看用户角色', 'button', (SELECT id FROM (SELECT id FROM sys_permission WHERE code='system:manage') t), 7),
('user-role:assign', '分配用户角色', 'button', (SELECT id FROM (SELECT id FROM sys_permission WHERE code='system:manage') t), 8);

-- ==================== 创建默认角色 ====================

INSERT INTO `sys_role` (`code`, `name`, `description`) VALUES
('SUPER_ADMIN', '超级管理员', '拥有系统全部权限'),
('ADMIN', '管理员', '拥有业务管理权限，无系统管理权限');

-- 超级管理员拥有全部权限
INSERT INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT (SELECT id FROM sys_role WHERE code='SUPER_ADMIN'), id FROM sys_permission;

-- 管理员拥有除系统管理外的全部权限
INSERT INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT (SELECT id FROM sys_role WHERE code='ADMIN'), id FROM sys_permission
WHERE code NOT IN ('system:manage', 'role:view', 'role:add', 'role:edit', 'role:delete',
                   'permission:view', 'permission:assign', 'user-role:view', 'user-role:assign');

-- 将超级管理员角色分配给admin用户
INSERT INTO `sys_user_role` (`user_type`, `user_id`, `role_id`)
SELECT 'admin', id, (SELECT id FROM sys_role WHERE code='SUPER_ADMIN')
FROM admin WHERE username='admin';
