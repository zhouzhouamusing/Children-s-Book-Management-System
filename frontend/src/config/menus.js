export const adminMenus = [
  { path: '/dashboard', name: '数据概览', icon: 'DataAnalysis', permission: 'DASHBOARD_READ' },
  { path: '/books', name: '图书管理', icon: 'Reading', permission: 'BOOK_READ' },
  { path: '/categories', name: '分类管理', icon: 'FolderOpened', permission: 'CATEGORY_READ' },
  { path: '/readers', name: '读者管理', icon: 'UserFilled', permission: 'READER_READ' },
  { path: '/borrows', name: '借阅管理', icon: 'Notebook', permission: 'BORROW_READ' },
  { path: '/admin-applications', name: '管理员审批', icon: 'Stamp', permission: 'ADMIN_APPLICATION_REVIEW' },
  { path: '/reader-view', name: '读者系统', icon: 'View', permission: 'READER_PROFILE_READ' },
  { path: '/resources', name: '资源管理', icon: 'Files', permission: 'FILE_READ' },
  { path: '/reviews', name: '评价管理', icon: 'ChatDotRound', permission: 'REVIEW_READ' },
  { path: '/system/roles', name: '角色管理', icon: 'Key', permission: 'ROLE_MANAGE' },
  { path: '/system/permissions', name: '权限管理', icon: 'Lock', permission: 'PERMISSION_MANAGE' },
  { path: '/system/user-roles', name: '用户角色', icon: 'Avatar', permission: 'USER_ROLE_ASSIGN' },
]

export const readerMenus = [
  { path: '/reader/my-borrows', name: '我的借阅', icon: 'Reading', permission: 'READER_BORROW_READ' },
  { path: '/reader/reservations', name: '预约图书', icon: 'Calendar', permission: 'READER_RESERVATION_READ' },
  { path: '/reader/books', name: '图书浏览', icon: 'Search', permission: 'READER_BOOK_BROWSE' },
  { path: '/reader/recommend', name: '图书推荐', icon: 'Star', permission: 'READER_BOOK_BROWSE' },
  { path: '/reader/reading-progress', name: '阅读进度', icon: 'TrendCharts', permission: 'READING_PROGRESS_READ' },
  { path: '/reader/my-reviews', name: '我的评价', icon: 'ChatLineRound', permission: 'READER_REVIEW_READ' },
  { path: '/reader/profile', name: '个人中心', icon: 'User', permission: 'READER_PROFILE_READ' },
]
