export const adminMenus = [
  { index: '/dashboard', title: '数据概览', icon: 'DataAnalysis', permission: 'dashboard:view' },
  { index: '/books', title: '图书管理', icon: 'Reading', permission: 'book:view' },
  { index: '/categories', title: '分类管理', icon: 'Grid', permission: 'category:view' },
  { index: '/readers', title: '读者管理', icon: 'UserFilled', permission: 'reader:view' },
  { index: '/borrows', title: '借阅管理', icon: 'Notebook', permission: 'borrow:view' },
  { index: '/admin-applications', title: '管理员审批', icon: 'Stamp', permission: 'admin-app:view' },
  { index: '/appeals', title: '申诉管理', icon: 'Warning', permission: 'appeal:view' },
  { index: '/reader-view', title: '读者系统', icon: 'Monitor', permission: 'reader-view:access' },
  { index: '/resources', title: '资源管理', icon: 'Files', permission: 'resource:view' },
  { index: '/reviews', title: '评价管理', icon: 'ChatDotRound', permission: 'review:view' },
  { index: '/roles', title: '角色管理', icon: 'Key', permission: 'role:view' },
  { index: '/permissions', title: '权限管理', icon: 'Lock', permission: 'permission:view' },
  { index: '/user-roles', title: '用户授权', icon: 'Avatar', permission: 'user-role:view' }
]

export const readerMenus = [
  { index: '/reader/my-borrows', title: '我的借阅', icon: 'Reading', permission: 'reader-center:borrow' },
  { index: '/reader/reservations', title: '预约图书', icon: 'Calendar', permission: 'reader-center:reservation' },
  { index: '/reader/books', title: '图书浏览', icon: 'Search', permission: 'reader-center:browse' },
  { index: '/reader/recommend', title: '图书推荐', icon: 'Star', permission: 'reader-center:recommend' },
  { index: '/reader/reading-progress', title: '阅读进度', icon: 'TrendCharts', permission: 'reader-center:progress' },
  { index: '/reader/my-reviews', title: '我的评价', icon: 'ChatLineRound', permission: 'reader-center:review' },
  { index: '/reader/appeals', title: '我的申诉', icon: 'Warning', permission: 'reader-center:appeal' },
  { index: '/reader/profile', title: '个人中心', icon: 'User', permission: 'reader-center:profile' }
]
