export const adminMenus = [
  { index: '/dashboard', title: '数据概览', icon: 'DataAnalysis', permission: 'dashboard:view' },
  { index: '/books', title: '图书管理', icon: 'Reading', permission: 'book:view' },
  { index: '/categories', title: '分类管理', icon: 'FolderOpened', permission: 'category:view' },
  { index: '/readers', title: '读者管理', icon: 'UserFilled', permission: 'reader:view' },
  { index: '/borrows', title: '借阅管理', icon: 'Notebook', permission: 'borrow:view' },
  { index: '/admin-applications', title: '管理员审批', icon: 'Stamp', permission: 'admin-app:view' },
  { index: '/reader-view', title: '读者系统', icon: 'View', permission: 'dashboard:view' },
  { index: '/resources', title: '资源管理', icon: 'FolderOpened', permission: 'resource:view' },
  { index: '/reviews', title: '评价管理', icon: 'ChatDotRound', permission: 'review:view' },
  { index: '/roles', title: '角色管理', icon: 'Key', permission: 'role:view' },
  { index: '/permissions', title: '权限管理', icon: 'Lock', permission: 'permission:view' },
  { index: '/user-roles', title: '用户授权', icon: 'Avatar', permission: 'user-role:view' }
]
