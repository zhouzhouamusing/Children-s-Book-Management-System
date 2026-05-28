import { createRouter, createWebHashHistory } from 'vue-router'

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
    component: () => import('@/views/Register.vue'),
    meta: { title: '注册', public: true }
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
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard.vue'),
        meta: { title: '数据概览', role: 'ADMIN' }
      },
      {
        path: 'books',
        name: 'Books',
        component: () => import('@/views/Books.vue'),
        meta: { title: '图书管理', role: 'ADMIN' }
      },
      {
        path: 'categories',
        name: 'Categories',
        component: () => import('@/views/Categories.vue'),
        meta: { title: '分类管理', role: 'ADMIN' }
      },
      {
        path: 'readers',
        name: 'Readers',
        component: () => import('@/views/Readers.vue'),
        meta: { title: '读者管理', role: 'ADMIN' }
      },
      {
        path: 'borrows',
        name: 'Borrows',
        component: () => import('@/views/Borrows.vue'),
        meta: { title: '借阅管理', role: 'ADMIN' }
      }
    ]
  },
  {
    path: '/reader',
    component: () => import('@/layout/ReaderLayout.vue'),
    redirect: '/reader/my-borrows',
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

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  const role = localStorage.getItem('role')

  if (to.meta.public) {
    if (token && to.path === '/login') {
      next(role === 'READER' ? '/reader/my-borrows' : '/dashboard')
    } else {
      next()
    }
  } else if (!token) {
    next('/login')
  } else if (to.meta.role && to.meta.role !== role) {
    next(role === 'READER' ? '/reader/my-borrows' : '/dashboard')
  } else {
    next()
  }
})

router.onError((error) => {
  if (error.message.includes('Failed to fetch dynamically imported module') ||
      error.message.includes('Importing a module script failed')) {
    window.location.reload()
  }
})

export default router
