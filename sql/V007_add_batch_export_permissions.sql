-- V007: 新增批量操作和数据导出权限
-- 为管理端增加细粒度的批量删除和数据导出权限控制

-- 插入新权限记录
INSERT INTO sys_permission (code, name, module, type, description, create_time) VALUES
('BOOK_BATCH_DELETE', '批量删除图书', '图书管理', 'button', '允许批量删除图书', NOW()),
('BOOK_EXPORT', '导出图书数据', '图书管理', 'button', '允许导出图书CSV/Excel数据', NOW()),
('READER_BATCH_DELETE', '批量删除读者', '读者管理', 'button', '允许批量删除读者', NOW()),
('READER_EXPORT', '导出读者数据', '读者管理', 'button', '允许导出读者数据', NOW()),
('BORROW_EXPORT', '导出借阅数据', '借阅管理', 'button', '允许导出借阅记录数据', NOW()),
('PERMISSION_BATCH_DELETE', '批量删除权限', '系统管理', 'button', '允许批量删除权限', NOW()),
('PERMISSION_EXPORT', '导出权限清单', '系统管理', 'button', '允许导出权限数据CSV', NOW()),
('REVIEW_BATCH_DELETE', '批量删除评价', '评价管理', 'button', '允许批量删除评价', NOW());

-- 将新权限分配给管理员角色（假设 ADMIN 角色 ID 为 1）
-- 实际执行时请根据 sys_role 表中的实际 ID 调整
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r, sys_permission p
WHERE r.code = 'ADMIN'
AND p.code IN (
    'BOOK_BATCH_DELETE', 'BOOK_EXPORT',
    'READER_BATCH_DELETE', 'READER_EXPORT',
    'BORROW_EXPORT',
    'PERMISSION_BATCH_DELETE', 'PERMISSION_EXPORT',
    'REVIEW_BATCH_DELETE'
);

-- 同样分配给 SUPER_ADMIN（如果存在）
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r, sys_permission p
WHERE r.code = 'SUPER_ADMIN'
AND p.code IN (
    'BOOK_BATCH_DELETE', 'BOOK_EXPORT',
    'READER_BATCH_DELETE', 'READER_EXPORT',
    'BORROW_EXPORT',
    'PERMISSION_BATCH_DELETE', 'PERMISSION_EXPORT',
    'REVIEW_BATCH_DELETE'
);
