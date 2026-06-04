import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import 'animate.css'
import App from './App.vue'
import router from './router'
import './styles/global.css'
import { validateToken } from './api'
import { isTokenExpired, clearAuth } from './utils/auth'
import { vPermission } from './directives/permission'

const app = createApp(App)
const pinia = createPinia()

for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(pinia)
app.use(router)
app.use(ElementPlus, { size: 'default' })
app.directive('permission', vPermission)

app.config.errorHandler = (err, instance, info) => {
  console.error('Global error:', err, info)
  if (err.message && (
    err.message.includes('Failed to fetch dynamically imported module') ||
    err.message.includes('Loading chunk')
  )) {
    window.location.reload()
  }
}

// Restore permission store from localStorage on app init
import { usePermissionStore } from './stores/permission'
const permStore = usePermissionStore()
permStore.loadFromStorage()

const token = localStorage.getItem('token')
if (token && !isTokenExpired(token)) {
  validateToken().catch(() => {
    clearAuth()
    permStore.clear()
    router.push('/login')
  })
}

app.mount('#app')
