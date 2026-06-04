import { createRouter, createWebHashHistory } from 'vue-router'
import { isTokenExpired, getRoleFromToken, clearAuth, VALIDATION_INTERVAL, markValidated } from '@/utils/auth'
import * as authState from '@/utils/auth'

function hasPermission(code) {
  try {
    const perms = JSON.parse(localStorage.getItem('permissions') || '[]')
    return perms.includes(code)
  } catch { return false }
}

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
    meta: { roles: ['ADMIN', 'SUPER_ADMIN'] },
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard.vue'),
        meta: { title: '数据概览', roles: ['ADMIN', 'SUPER_ADMIN'], permission: 'DASHBOARD_READ' }
      },
      {
        path: 'books',
        name: 'Books',
        component: () => import('@/views/Books.vue'),
        meta: { title: '图书管理', roles: ['ADMIN', 'SUPER_ADMIN'], permission: 'BOOK_READ' }
      },
      {
        path: 'categories',
        name: 'Categories',
        component: () => import('@/views/Categories.vue'),
        meta: { title: '分类管理', roles: ['ADMIN', 'SUPER_ADMIN'], permission: 'CATEGORY_READ' }
      },
      {
        path: 'readers',
        name: 'Readers',
        component: () => import('@/views/Readers.vue'),
        meta: { title: '读者管理', roles: ['ADMIN', 'SUPER_ADMIN'], permission: 'READER_READ' }
      },
      {
        path: 'borrows',
        name: 'Borrows',
        component: () => import('@/views/Borrows.vue'),
        meta: { title: '借阅管理', roles: ['ADMIN', 'SUPER_ADMIN'], permission: 'BORROW_READ' }
      },
      {
        path: 'admin-applications',
        name: 'AdminApplications',
        component: () => import('@/views/AdminApplications.vue'),
        meta: { title: '管理员审批', roles: ['ADMIN', 'SUPER_ADMIN'], permission: 'APPLICATION_READ' }
      },
      {
        path: 'reader-view',
        name: 'AdminReaderView',
        component: () => import('@/views/AdminReaderView.vue'),
        meta: { title: '读者系统', roles: ['ADMIN', 'SUPER_ADMIN'] }
      },
      {
        path: 'resources',
        name: 'Resources',
        component: () => import('@/views/Resources.vue'),
        meta: { title: '资源管理', roles: ['ADMIN', 'SUPER_ADMIN'], permission: 'FILE_READ' }
      },
      {
        path: 'reviews',
        name: 'Reviews',
        component: () => import('@/views/Reviews.vue'),
        meta: { title: '评价管理', roles: ['ADMIN', 'SUPER_ADMIN'], permission: 'REVIEW_READ' }
      },
      {
        path: 'appeals',
        name: 'Appeals',
        component: () => import('@/views/Appeals.vue'),
        meta: { title: '申诉管理', roles: ['ADMIN', 'SUPER_ADMIN'], permission: 'APPEAL_READ' }
      },
      {
        path: 'system/roles',
        name: 'SystemRoles',
        component: () => import('@/views/system/Roles.vue'),
        meta: { title: '角色管理', roles: ['ADMIN', 'SUPER_ADMIN'], permission: 'ROLE_MANAGE' }
      },
      {
        path: 'system/permissions',
        name: 'SystemPermissions',
        component: () => import('@/views/system/Permissions.vue'),
        meta: { title: '权限管理', roles: ['ADMIN', 'SUPER_ADMIN'], permission: 'PERMISSION_MANAGE' }
      },
      {
        path: 'system/user-roles',
        name: 'SystemUserRoles',
        component: () => import('@/views/system/UserRoles.vue'),
        meta: { title: '用户角色', roles: ['ADMIN', 'SUPER_ADMIN'], permission: 'USER_ROLE_ASSIGN' }
      }
    ]
  },
  {
    path: '/reader',
    component: () => import('@/layout/ReaderLayout.vue'),
    redirect: '/reader/my-borrows',
    meta: { roles: ['READER'] },
    children: [
      {
        path: 'my-borrows',
        name: 'MyBorrows',
        component: () => import('@/views/reader/MyBorrows.vue'),
        meta: { title: '我的借阅', roles: ['READER'], permission: 'READER_BORROW' }
      },
      {
        path: 'reservations',
        name: 'Reservations',
        component: () => import('@/views/reader/Reservations.vue'),
        meta: { title: '预约图书', roles: ['READER'], permission: 'READER_RESERVE' }
      },
      {
        path: 'books',
        name: 'ReaderBooks',
        component: () => import('@/views/reader/BookBrowse.vue'),
        meta: { title: '图书浏览', roles: ['READER'], permission: 'READER_BROWSE' }
      },
      {
        path: 'recommend',
        name: 'BookRecommend',
        component: () => import('@/views/reader/BookRecommend.vue'),
        meta: { title: '图书推荐', roles: ['READER'], permission: 'READER_RECOMMEND' }
      },
      {
        path: 'reading-progress',
        name: 'ReadingProgress',
        component: () => import('@/views/reader/ReadingProgress.vue'),
        meta: { title: '阅读进度', roles: ['READER'], permission: 'READER_PROGRESS' }
      },
      {
        path: 'my-reviews',
        name: 'MyReviews',
        component: () => import('@/views/reader/MyReviews.vue'),
        meta: { title: '我的评价', roles: ['READER'], permission: 'READER_REVIEW' }
      },
      {
        path: 'appeals',
        name: 'ReaderAppeals',
        component: () => import('@/views/reader/Appeals.vue'),
        meta: { title: '我的申诉', roles: ['READER'], permission: 'READER_APPEAL_CREATE' }
      },
      {
        path: 'profile',
        name: 'ReaderProfile',
        component: () => import('@/views/reader/Profile.vue'),
        meta: { title: '个人中心', roles: ['READER'], permission: 'READER_PROFILE' }
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

router.beforeEach(async (to, from) => {
  const token = localStorage.getItem('token')

  if (to.meta.public) {
    if (token && !isTokenExpired(token) && to.path === '/login') {
      const role = getRoleFromToken(token)
      if (!role) { clearAuth(); return true }
      return role === 'READER' ? '/reader/my-borrows' : '/dashboard'
    }
    return true
  }

  if (!token || isTokenExpired(token)) {
    clearAuth()
    return '/login'
  }

  const role = getRoleFromToken(token)
  if (!role || (role !== 'ADMIN' && role !== 'READER')) {
    clearAuth()
    return '/login'
  }

  if (Date.now() - authState.lastValidated > VALIDATION_INTERVAL) {
    try {
      const { default: request } = await import('@/utils/request')
      const res = await request.get('/auth/validate')
      markValidated()
      if (res.data) {
        if (res.data.suspended !== undefined) {
          localStorage.setItem('suspended', res.data.suspended ? 'true' : 'false')
        }
        if (res.data.roles) {
          localStorage.setItem('roles', JSON.stringify(res.data.roles))
        }
        if (res.data.permissions) {
          localStorage.setItem('permissions', JSON.stringify(res.data.permissions))
        }
        const { usePermissionStore } = await import('@/stores/permission')
        const permStore = usePermissionStore()
        permStore.loadFromStorage()
      }
    } catch (e) {
      if (e?.response?.status === 403 || e?.response?.data?.code === 403) {
        localStorage.setItem('authError', '账号已被禁用或权限不足')
      }
      clearAuth()
      return '/login'
    }
  }

  localStorage.setItem('role', role)

  // Check route role access
  if (to.meta.roles) {
    const userRoles = JSON.parse(localStorage.getItem('roles') || '[]')
    const effectiveRole = role
    const hasAccess = to.meta.roles.some(r => {
      if (r === 'ADMIN') return effectiveRole === 'ADMIN' || userRoles.includes('ADMIN') || userRoles.includes('SUPER_ADMIN')
      if (r === 'SUPER_ADMIN') return userRoles.includes('SUPER_ADMIN')
      return r === effectiveRole || userRoles.includes(r)
    })
    if (!hasAccess) {
      return role === 'READER' ? '/reader/my-borrows' : '/dashboard'
    }
  }

  // Check route-level permission
  if (to.meta.permission) {
    if (!hasPermission(to.meta.permission)) {
      return role === 'READER' ? '/reader/my-borrows' : '/dashboard'
    }
  }

  if (role === 'READER' && localStorage.getItem('suspended') === 'true') {
    const allowedPaths = ['/reader/profile']
    const currentPath = to.path
    if (!allowedPaths.some(p => currentPath.startsWith(p))) {
      return '/reader/profile'
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
