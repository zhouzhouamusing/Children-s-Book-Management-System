import { createRouter, createWebHashHistory } from 'vue-router'
import { isTokenExpired, getRoleFromToken, clearAuth, VALIDATION_INTERVAL, markValidated } from '@/utils/auth'
import * as authState from '@/utils/auth'

// 权限缓存 - 避免每次导航重复解析JSON
let permCacheStr = ''
let permCacheSet = new Set()
let rolesCacheStr = ''
let rolesCacheArr = []

function getPermissionsSet() {
  const raw = localStorage.getItem('permissions') || '[]'
  if (raw !== permCacheStr) {
    permCacheStr = raw
    try { permCacheSet = new Set(JSON.parse(raw)) } catch { permCacheSet = new Set() }
  }
  return permCacheSet
}

function hasPermission(code) {
  return getPermissionsSet().has(code)
}

function hasAnyPermission(codes) {
  const perms = getPermissionsSet()
  return codes.some(c => perms.has(c))
}

function getUserRoles() {
  const raw = localStorage.getItem('roles') || '[]'
  if (raw !== rolesCacheStr) {
    rolesCacheStr = raw
    try { rolesCacheArr = JSON.parse(raw) } catch { rolesCacheArr = [] }
  }
  return rolesCacheArr
}

// 路由权限检查失败时通知前端展示提示
function emitPermDenied(to, reason) {
  window.dispatchEvent(new CustomEvent('permission-denied', {
    detail: { path: to.path, reason }
  }))
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
        meta: { title: '管理员审批', roles: ['ADMIN', 'SUPER_ADMIN'], permission: 'ADMIN_APPLICATION_REVIEW' }
      },
      {
        path: 'reader-view',
        name: 'AdminReaderView',
        component: () => import('@/views/AdminReaderView.vue'),
        meta: { title: '读者系统', roles: ['ADMIN', 'SUPER_ADMIN'], permission: 'READER_PROFILE_READ' }
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
        meta: { title: '我的借阅', roles: ['READER'], permission: 'READER_BORROW_READ' }
      },
      {
        path: 'reservations',
        name: 'Reservations',
        component: () => import('@/views/reader/Reservations.vue'),
        meta: { title: '预约图书', roles: ['READER'], permission: 'READER_RESERVATION_READ' }
      },
      {
        path: 'books',
        name: 'ReaderBooks',
        component: () => import('@/views/reader/BookBrowse.vue'),
        meta: { title: '图书浏览', roles: ['READER'], permission: 'READER_BOOK_BROWSE' }
      },
      {
        path: 'recommend',
        name: 'BookRecommend',
        component: () => import('@/views/reader/BookRecommend.vue'),
        meta: { title: '图书推荐', roles: ['READER'], permission: 'READER_BOOK_BROWSE' }
      },
      {
        path: 'reading-progress',
        name: 'ReadingProgress',
        component: () => import('@/views/reader/ReadingProgress.vue'),
        meta: { title: '阅读进度', roles: ['READER'], permission: 'READING_PROGRESS_READ' }
      },
      {
        path: 'my-reviews',
        name: 'MyReviews',
        component: () => import('@/views/reader/MyReviews.vue'),
        meta: { title: '我的评价', roles: ['READER'], permission: 'READER_REVIEW_READ' }
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
        meta: { title: '个人中心', roles: ['READER'], permission: 'READER_PROFILE_READ' }
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

  // 公开路由无需验证
  if (to.meta.public) {
    if (token && !isTokenExpired(token) && to.path === '/login') {
      const role = getRoleFromToken(token)
      if (!role) { clearAuth(); return true }
      return role === 'READER' ? '/reader/my-borrows' : '/dashboard'
    }
    return true
  }

  // Token不存在或已过期
  if (!token || isTokenExpired(token)) {
    clearAuth()
    return '/login'
  }

  const role = getRoleFromToken(token)
  if (!role || (role !== 'ADMIN' && role !== 'READER')) {
    clearAuth()
    return '/login'
  }

  // 定期向服务器验证token有效性并刷新权限数据
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
          rolesCacheStr = ''
        }
        if (res.data.permissions) {
          localStorage.setItem('permissions', JSON.stringify(res.data.permissions))
          permCacheStr = ''
        }
        const { usePermissionStore } = await import('@/stores/permission')
        const permStore = usePermissionStore()
        permStore.loadFromStorage()
      }
    } catch (e) {
      if (e?.response?.status === 401 || e?.response?.status === 403 || e?.response?.data?.code === 403) {
        localStorage.setItem('authError', '账号已被禁用或权限不足')
        clearAuth()
        return '/login'
      }
    }
  }

  localStorage.setItem('role', role)

  // 第一层检查：基于角色的路由访问控制
  if (to.meta.roles) {
    const userRoles = getUserRoles()
    const hasRoleAccess = to.meta.roles.some(r => {
      if (r === 'ADMIN') return role === 'ADMIN' || userRoles.includes('ADMIN') || userRoles.includes('SUPER_ADMIN')
      if (r === 'SUPER_ADMIN') return userRoles.includes('SUPER_ADMIN')
      return r === role || userRoles.includes(r)
    })
    if (!hasRoleAccess) {
      emitPermDenied(to, '角色权限不足')
      return role === 'READER' ? '/reader/my-borrows' : '/dashboard'
    }
  }

  // 第二层检查：基于权限码的路由访问控制
  // 支持 meta.permission (单个) 和 meta.permissions (数组，满足任一即可)
  const requiredPerm = to.meta.permission
  const requiredPerms = to.meta.permissions

  if (requiredPerm || requiredPerms) {
    let hasAccess = false
    if (requiredPerm) {
      hasAccess = hasPermission(requiredPerm)
    }
    if (!hasAccess && requiredPerms && Array.isArray(requiredPerms)) {
      hasAccess = hasAnyPermission(requiredPerms)
    }

    if (!hasAccess) {
      emitPermDenied(to, `缺少权限: ${requiredPerm || requiredPerms.join('/')}`)
      // 查找用户有权限的第一个同级路由作为fallback
      const parent = to.matched[to.matched.length - 2]
      if (parent && parent.children) {
        const fallback = parent.children.find(child => {
          if (child.path === to.matched[to.matched.length - 1]?.path) return false
          if (!child.meta?.permission && !child.meta?.permissions) return true
          if (child.meta.permission) return hasPermission(child.meta.permission)
          if (child.meta.permissions) return hasAnyPermission(child.meta.permissions)
          return false
        })
        if (fallback) {
          const fallbackPath = parent.path
            ? `${parent.path}/${fallback.path}`.replace(/\/\//g, '/')
            : `/${fallback.path}`
          if (fallbackPath !== to.path) return fallbackPath
        }
      }
      return role === 'READER' ? '/reader/my-borrows' : '/dashboard'
    }
  }

  // 第三层检查：被封禁的读者只能访问个人中心和申诉页
  if (role === 'READER' && localStorage.getItem('suspended') === 'true') {
    const allowedPaths = ['/reader/profile', '/reader/appeals']
    const currentPath = to.path
    if (!allowedPaths.some(p => currentPath.startsWith(p))) {
      emitPermDenied(to, '账号已被暂停，仅可访问个人中心和申诉')
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
