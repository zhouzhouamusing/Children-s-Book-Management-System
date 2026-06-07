import { createRouter, createWebHashHistory } from 'vue-router'
import { ElMessage } from 'element-plus'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录', public: true }
  },
  {
    path: '/register',
    name: 'Register',
    redirect: '/login'
  },
  {
    path: '/reader-register',
    name: 'ReaderRegister',
    component: () => import('@/views/ReaderRegister.vue'),
    meta: { title: '读者注册', public: true }
  },
  {
    path: '/forgot-password',
    name: 'ForgotPassword',
    component: () => import('@/views/ForgotPassword.vue'),
    meta: { title: '找回密码', public: true }
  },
  {
    path: '/',
    component: () => import('@/layout/MainLayout.vue'),
    redirect: '/dashboard',
    meta: { role: 'ADMIN' },
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard.vue'),
        meta: { title: '数据概览', role: 'ADMIN', permission: 'dashboard:view' }
      },
      {
        path: 'books',
        name: 'Books',
        component: () => import('@/views/Books.vue'),
        meta: { title: '图书管理', role: 'ADMIN', permission: 'book:view' }
      },
      {
        path: 'categories',
        name: 'Categories',
        component: () => import('@/views/Categories.vue'),
        meta: { title: '分类管理', role: 'ADMIN', permission: 'category:view' }
      },
      {
        path: 'readers',
        name: 'Readers',
        component: () => import('@/views/Readers.vue'),
        meta: { title: '读者管理', role: 'ADMIN', permission: 'reader:view' }
      },
      {
        path: 'borrows',
        name: 'Borrows',
        component: () => import('@/views/Borrows.vue'),
        meta: { title: '借阅管理', role: 'ADMIN', permission: 'borrow:view' }
      },
      {
        path: 'admin-applications',
        name: 'AdminApplications',
        component: () => import('@/views/AdminApplications.vue'),
        meta: { title: '管理员审批', role: 'ADMIN', permission: 'admin-app:view' }
      },
      {
        path: 'reader-view',
        name: 'AdminReaderView',
        component: () => import('@/views/AdminReaderView.vue'),
        meta: { title: '读者系统', role: 'ADMIN', permission: 'dashboard:view' }
      },
      {
        path: 'resources',
        name: 'Resources',
        component: () => import('@/views/Resources.vue'),
        meta: { title: '资源管理', role: 'ADMIN', permission: 'resource:view' }
      },
      {
        path: 'reviews',
        name: 'Reviews',
        component: () => import('@/views/Reviews.vue'),
        meta: { title: '评价管理', role: 'ADMIN', permission: 'review:view' }
      },
      {
        path: 'roles',
        name: 'Roles',
        component: () => import('@/views/Roles.vue'),
        meta: { title: '角色管理', role: 'ADMIN', permission: 'role:view' }
      },
      {
        path: 'permissions',
        name: 'Permissions',
        component: () => import('@/views/Permissions.vue'),
        meta: { title: '权限管理', role: 'ADMIN', permission: 'permission:view' }
      },
      {
        path: 'user-roles',
        name: 'UserRoles',
        component: () => import('@/views/UserRoles.vue'),
        meta: { title: '用户授权', role: 'ADMIN', permission: 'user-role:view' }
      }
    ]
  },
  {
    path: '/reader',
    component: () => import('@/layout/ReaderLayout.vue'),
    redirect: '/reader/my-borrows',
    meta: { role: 'READER' },
    children: [
      {
        path: 'my-borrows',
        name: 'MyBorrows',
        component: () => import('@/views/reader/MyBorrows.vue'),
        meta: { title: '我的借阅', role: 'READER' }
      },
      {
        path: 'reservations',
        name: 'Reservations',
        component: () => import('@/views/reader/Reservations.vue'),
        meta: { title: '预约图书', role: 'READER' }
      },
      {
        path: 'books',
        name: 'ReaderBooks',
        component: () => import('@/views/reader/BookBrowse.vue'),
        meta: { title: '图书浏览', role: 'READER' }
      },
      {
        path: 'recommend',
        name: 'BookRecommend',
        component: () => import('@/views/reader/BookRecommend.vue'),
        meta: { title: '图书推荐', role: 'READER' }
      },
      {
        path: 'reading-progress',
        name: 'ReadingProgress',
        component: () => import('@/views/reader/ReadingProgress.vue'),
        meta: { title: '阅读进度', role: 'READER' }
      },
      {
        path: 'my-reviews',
        name: 'MyReviews',
        component: () => import('@/views/reader/MyReviews.vue'),
        meta: { title: '我的评价', role: 'READER' }
      },
      {
        path: 'profile',
        name: 'ReaderProfile',
        component: () => import('@/views/reader/Profile.vue'),
        meta: { title: '个人中心', role: 'READER' }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/login'
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

function hasPermission(permCode) {
  try {
    const roles = JSON.parse(localStorage.getItem('roles') || '[]')
    if (roles.includes('SUPER_ADMIN')) return true
    const permissions = JSON.parse(localStorage.getItem('permissions') || '[]')
    return permissions.includes(permCode)
  } catch {
    return false
  }
}

router.beforeEach((to, from) => {
  const token = localStorage.getItem('token')
  const role = localStorage.getItem('role')

  if (to.meta.public) {
    if (token && to.path === '/login') {
      return role === 'READER' ? '/reader/my-borrows' : '/dashboard'
    }
    return true
  }

  if (!token) {
    if (to.path === '/login') return true
    return '/login'
  }

  if (to.meta.role && to.meta.role !== role) {
    if (role === 'READER') {
      return to.path === '/reader/my-borrows' ? true : '/reader/my-borrows'
    }
    if (role === 'ADMIN') {
      return to.path === '/dashboard' ? true : '/dashboard'
    }
    return '/login'
  }

  if (to.meta.permission && role === 'ADMIN') {
    if (!hasPermission(to.meta.permission)) {
      ElMessage.error('没有权限访问该页面')
      return from.path && from.path !== '/' ? from.path : '/dashboard'
    }
  }

  return true
})

router.onError((error) => {
  if (error.message.includes('Failed to fetch dynamically imported module') ||
      error.message.includes('Importing a module script failed') ||
      error.message.includes('Loading chunk') ||
      error.message.includes('Loading CSS chunk')) {
    const currentPath = window.location.hash.slice(1) || '/'
    console.warn('Dynamic import failed, reloading...', error.message)
    setTimeout(() => {
      window.location.reload()
    }, 100)
  }
})

export default router
