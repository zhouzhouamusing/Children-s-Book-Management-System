package com.kidsbook.common;

import java.util.*;

public final class RolePermissions {
    private static final Map<String, Set<Permission>> ROLE_MAP = new HashMap<>();

    private static final Set<Permission> ADMIN_PERMISSIONS = EnumSet.of(
        // 图书管理
        Permission.BOOK_CREATE, Permission.BOOK_READ, Permission.BOOK_UPDATE, Permission.BOOK_DELETE,
        // 读者管理
        Permission.READER_CREATE, Permission.READER_READ, Permission.READER_UPDATE, Permission.READER_DELETE,
        // 分类管理
        Permission.CATEGORY_CREATE, Permission.CATEGORY_READ, Permission.CATEGORY_UPDATE, Permission.CATEGORY_DELETE,
        // 借阅管理
        Permission.BORROW_CREATE, Permission.BORROW_READ, Permission.BORROW_UPDATE,
        // 预约审批
        Permission.RESERVATION_READ, Permission.RESERVATION_UPDATE,
        // 评论审核
        Permission.REVIEW_READ, Permission.REVIEW_UPDATE, Permission.REVIEW_DELETE,
        // 文件管理
        Permission.FILE_CREATE, Permission.FILE_READ, Permission.FILE_DELETE,
        // 仪表盘
        Permission.DASHBOARD_READ,
        // 审计日志
        Permission.AUDIT_LOG_READ,
        // 管理员申请审批
        Permission.ADMIN_APPLICATION_REVIEW,
        // 读者中心（管理员可查看但不可执行读者专属操作）
        Permission.READER_PROFILE_READ, Permission.READER_BORROW_READ,
        Permission.READER_RESERVATION_READ, Permission.READER_BOOK_BROWSE,
        Permission.READER_CATEGORY_BROWSE, Permission.READER_REVIEW_READ,
        Permission.READING_PROGRESS_READ
    );

    private static final Set<Permission> READER_PERMISSIONS = EnumSet.of(
        // 个人预约
        Permission.READER_RESERVATION_CREATE, Permission.READER_RESERVATION_READ, Permission.READER_RESERVATION_CANCEL,
        // 个人评论
        Permission.READER_REVIEW_CREATE, Permission.READER_REVIEW_READ,
        Permission.READER_REVIEW_UPDATE, Permission.READER_REVIEW_DELETE,
        // 阅读进度
        Permission.READING_PROGRESS_CREATE, Permission.READING_PROGRESS_READ,
        Permission.READING_PROGRESS_UPDATE, Permission.READING_PROGRESS_DELETE,
        // 申请成为管理员
        Permission.ADMIN_APPLICATION_APPLY, Permission.ADMIN_APPLICATION_STATUS,
        // 个人中心
        Permission.READER_PROFILE_READ, Permission.READER_PROFILE_UPDATE,
        // 浏览图书和分类
        Permission.READER_BOOK_BROWSE, Permission.READER_CATEGORY_BROWSE,
        // 借阅记录
        Permission.READER_BORROW_READ,
        // 申诉
        Permission.READER_APPEAL_CREATE
    );

    static {
        ROLE_MAP.put("ADMIN", Collections.unmodifiableSet(ADMIN_PERMISSIONS));
        ROLE_MAP.put("READER", Collections.unmodifiableSet(READER_PERMISSIONS));
    }

    private RolePermissions() {}

    public static boolean hasPermission(String role, Permission permission) {
        Set<Permission> perms = ROLE_MAP.get(role);
        return perms != null && perms.contains(permission);
    }

    public static Set<Permission> getPermissions(String role) {
        return ROLE_MAP.getOrDefault(role, Collections.emptySet());
    }

    public static Map<String, Set<Permission>> getAllMappings() {
        return Collections.unmodifiableMap(ROLE_MAP);
    }

    public static boolean isReaderOnly(Permission permission) {
        return READER_PERMISSIONS.contains(permission) && !ADMIN_PERMISSIONS.contains(permission);
    }

    public static boolean isAdminOnly(Permission permission) {
        return ADMIN_PERMISSIONS.contains(permission) && !READER_PERMISSIONS.contains(permission);
    }
}
