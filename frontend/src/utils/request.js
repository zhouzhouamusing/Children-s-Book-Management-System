import axios from 'axios'
import { ElMessage, ElNotification } from 'element-plus'
import router from '@/router'
import { clearAuth } from '@/utils/auth'

const request = axios.create({
  baseURL: '/api',
  timeout: 30000,
  retry: 2,
  retryDelay: 1000
})

request.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  error => Promise.reject(error)
)

let isRedirecting = false

function clearAuthAndRedirect(message) {
  if (isRedirecting) return
  isRedirecting = true
  clearAuth()
  ElMessage.error(message || '登录已过期，请重新登录')
  router.push('/login').finally(() => {
    setTimeout(() => { isRedirecting = false }, 2000)
  })
}

function getModuleContext(url) {
  if (!url) return ''
  if (url.includes('/books')) return '图书管理'
  if (url.includes('/readers')) return '读者管理'
  if (url.includes('/borrows')) return '借阅管理'
  if (url.includes('/categories')) return '分类管理'
  if (url.includes('/reviews') || url.includes('/book-review')) return '评价管理'
  if (url.includes('/files')) return '文件管理'
  if (url.includes('/sys/roles')) return '角色管理'
  if (url.includes('/sys/permissions')) return '权限管理'
  if (url.includes('/sys/user-role')) return '用户角色管理'
  if (url.includes('/admin-application')) return '管理员申请'
  if (url.includes('/appeals')) return '申诉管理'
  if (url.includes('/reader-center')) return '读者中心'
  if (url.includes('/reading-progress')) return '阅读进度'
  return ''
}

request.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code === 200) {
      return res
    }
    if (res.code === 401) {
      clearAuthAndRedirect(res.message)
      return Promise.reject(new Error(res.message || '登录已过期'))
    }
    if (res.code === 403) {
      const context = getModuleContext(response.config?.url)
      const message = res.message || '无权执行此操作'
      ElNotification({
        title: '权限不足',
        message: context ? `[${context}] ${message}` : message,
        type: 'error',
        duration: 5000
      })
      window.dispatchEvent(new CustomEvent('api-permission-denied', {
        detail: { url: response.config?.url, message, context }
      }))
      return Promise.reject(new Error(message))
    }
    ElMessage.error(res.message || '请求失败')
    return Promise.reject(new Error(res.message || '请求失败'))
  },
  error => {
    const config = error.config
    const status = error.response?.status

    if (status === 401) {
      clearAuthAndRedirect('登录已过期，请重新登录')
      return Promise.reject(error)
    }

    if (status === 403) {
      const serverMessage = error.response?.data?.message
      const context = getModuleContext(config?.url)
      const message = serverMessage || '没有权限访问该资源'
      ElNotification({
        title: '权限不足',
        message: context ? `[${context}] ${message}` : message,
        type: 'error',
        duration: 5000
      })
      window.dispatchEvent(new CustomEvent('api-permission-denied', {
        detail: { url: config?.url, message, context }
      }))
      return Promise.reject(error)
    }

    if (!config || !config.retry) {
      ElMessage.error(error.response?.data?.message || error.message || '网络错误，请稍后重试')
      return Promise.reject(error)
    }

    config.__retryCount = config.__retryCount || 0
    if (config.__retryCount >= config.retry) {
      ElMessage.error(error.response?.data?.message || error.message || '网络错误，请稍后重试')
      return Promise.reject(error)
    }

    config.__retryCount += 1
    return new Promise(resolve => {
      setTimeout(() => resolve(request(config)), config.retryDelay || 1000)
    })
  }
)

export default request
