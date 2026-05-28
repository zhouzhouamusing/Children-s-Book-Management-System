import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

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
  localStorage.removeItem('token')
  localStorage.removeItem('nickname')
  localStorage.removeItem('role')
  localStorage.removeItem('readerId')
  ElMessage.error(message || '登录已过期，请重新登录')
  router.push('/login').finally(() => {
    setTimeout(() => { isRedirecting = false }, 2000)
  })
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
      ElMessage.error('没有权限访问该资源')
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
