<template>
  <div class="login-container">
    <div class="login-bg">
      <div class="floating-shape shape-1"></div>
      <div class="floating-shape shape-2"></div>
      <div class="floating-shape shape-3"></div>
      <div class="floating-shape shape-4"></div>
      <div class="floating-shape shape-5"></div>
      <div class="floating-shape shape-6"></div>
    </div>
    <div class="login-wrapper">
      <div class="login-brand animate__animated animate__fadeInLeft">
        <div class="brand-content">
          <div class="brand-logo">📚</div>
          <h2>童书乐园</h2>
          <p class="brand-slogan">让每个孩子爱上阅读</p>
          <div class="brand-features">
            <div class="feature-item">
              <span class="feature-icon">📖</span>
              <span>海量童书资源</span>
            </div>
            <div class="feature-item">
              <span class="feature-icon">🎯</span>
              <span>智能推荐系统</span>
            </div>
            <div class="feature-item">
              <span class="feature-icon">📊</span>
              <span>阅读成长记录</span>
            </div>
          </div>
        </div>
      </div>
      <div class="login-card animate__animated animate__fadeInRight">
        <div class="login-header">
          <h1>欢迎回来</h1>
          <p>登录你的账号以继续使用</p>
        </div>
        <div class="role-toggle">
          <div
            class="role-option"
            :class="{ active: loginRole === 'admin' }"
            @click="loginRole = 'admin'"
          >
            <el-icon><Setting /></el-icon>
            <span>管理员</span>
          </div>
          <div
            class="role-option"
            :class="{ active: loginRole === 'reader' }"
            @click="loginRole = 'reader'"
          >
            <el-icon><UserFilled /></el-icon>
            <span>读者</span>
          </div>
          <div class="role-slider" :class="{ right: loginRole === 'reader' }"></div>
        </div>
        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          class="login-form"
          @keyup.enter="handleLogin"
          hide-required-asterisk
          status-icon
        >
          <el-form-item prop="username">
            <el-input
              v-model="form.username"
              placeholder="请输入用户名"
              prefix-icon="User"
              size="large"
              clearable
              maxlength="20"
              @focus="focusField = 'username'"
              @blur="focusField = ''"
              @input="form.username = form.username.replace(/[^a-zA-Z0-9_一-龥]/g, '')"
            />
            <transition name="hint-fade">
              <div class="input-hint" v-if="focusField === 'username'">
                <span class="hint-icon">💡</span> 3-20位，支持字母、数字、下划线和中文
              </div>
            </transition>
          </el-form-item>
          <el-form-item prop="password">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="请输入密码"
              prefix-icon="Lock"
              size="large"
              show-password
              maxlength="32"
              @focus="focusField = 'password'"
              @blur="focusField = ''"
            />
            <transition name="hint-fade">
              <div class="input-hint" v-if="focusField === 'password'">
                <span class="hint-icon">🔒</span> 密码长度为6-32位
              </div>
            </transition>
          </el-form-item>
          <div class="form-actions">
            <el-checkbox v-model="rememberMe" class="remember-me">记住我</el-checkbox>
            <router-link v-if="loginRole === 'admin'" to="/forgot-password" class="forgot-link">忘记密码？</router-link>
          </div>
          <el-form-item>
            <el-button
              type="primary"
              size="large"
              :loading="loading"
              class="login-btn"
              @click="handleLogin"
            >
              <transition name="btn-text" mode="out-in">
                <span :key="loading">{{ loading ? '登录中...' : '登 录' }}</span>
              </transition>
            </el-button>
          </el-form-item>
        </el-form>
        <div class="login-divider">
          <span>或者</span>
        </div>
        <div class="login-footer">
          <template v-if="loginRole === 'admin'">
            <span>管理员账号由超级管理员分配</span>
          </template>
          <template v-else>
            <span>还没有读者账号？</span>
            <router-link to="/reader-register" class="link">立即注册</router-link>
          </template>
        </div>
        <div class="login-hint">
          <el-icon><InfoFilled /></el-icon>
          <span v-if="loginRole === 'admin'">体验账号：admin / admin123</span>
          <span v-else>体验账号：xiaoming / 123456</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login, readerLogin } from '@/api'

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)
const loginRole = ref('admin')
const focusField = ref('')
const rememberMe = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度为3-20位', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_一-龥]+$/, message: '用户名只能包含字母、数字、下划线和中文', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度为6-32位', trigger: 'blur' }
  ]
}

const handleLogin = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const loginFn = loginRole.value === 'admin' ? login : readerLogin
    const res = await loginFn(form)
    const { token, nickname, role, readerId, roles, permissions } = res.data
    if (!token) {
      ElMessage.error('登录异常：未获取到凭证')
      return
    }
    localStorage.setItem('token', token)
    localStorage.setItem('nickname', nickname || (loginRole.value === 'admin' ? '管理员' : '读者'))
    localStorage.setItem('role', role || (loginRole.value === 'admin' ? 'ADMIN' : 'READER'))
    if (readerId) {
      localStorage.setItem('readerId', readerId)
    }
    if (roles && permissions) {
      localStorage.setItem('roles', JSON.stringify(roles))
      localStorage.setItem('permissions', JSON.stringify(permissions))
    }
    ElMessage.success('登录成功，欢迎回来！')
    if (role === 'READER' || loginRole.value === 'reader') {
      await router.push('/reader/my-borrows')
    } else {
      await router.push('/dashboard')
    }
  } catch (e) {
    console.error('登录失败:', e)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #FFF5F5 0%, #F0FFF4 50%, #EBF8FF 100%);
  position: relative;
  overflow: hidden;
  padding: 20px;
}

.login-bg {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.floating-shape {
  position: absolute;
  border-radius: 50%;
  opacity: 0.4;
  animation: float 6s ease-in-out infinite;
}

.shape-1 { width: 140px; height: 140px; background: var(--pink-light); top: 8%; left: 8%; animation-delay: 0s; }
.shape-2 { width: 90px; height: 90px; background: var(--green); top: 18%; right: 12%; animation-delay: 1s; }
.shape-3 { width: 110px; height: 110px; background: var(--blue); bottom: 12%; left: 18%; animation-delay: 2s; }
.shape-4 { width: 70px; height: 70px; background: var(--yellow-warm); bottom: 22%; right: 22%; animation-delay: 3s; }
.shape-5 { width: 100px; height: 100px; background: var(--purple-light); top: 55%; left: 5%; animation-delay: 4s; }
.shape-6 { width: 60px; height: 60px; background: var(--peach); top: 40%; right: 5%; animation-delay: 2.5s; }

.login-wrapper {
  display: flex;
  background: white;
  border-radius: var(--radius-lg);
  box-shadow: 0 24px 80px rgba(0, 0, 0, 0.08);
  overflow: hidden;
  position: relative;
  z-index: 1;
  max-width: 880px;
  width: 100%;
}

.login-brand {
  width: 360px;
  background: linear-gradient(135deg, var(--purple) 0%, #7B61A8 100%);
  padding: 48px 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
}

.login-brand::before {
  content: '';
  position: absolute;
  inset: 0;
  background: radial-gradient(circle at 30% 20%, rgba(255,255,255,0.1) 0%, transparent 50%),
              radial-gradient(circle at 80% 80%, rgba(255,255,255,0.05) 0%, transparent 40%);
}

.brand-content {
  position: relative;
  z-index: 1;
  text-align: center;
  color: white;
}

.brand-logo {
  font-size: 64px;
  margin-bottom: 16px;
  animation: float 3s ease-in-out infinite;
}

.brand-content h2 {
  font-size: 28px;
  font-weight: 700;
  margin: 0 0 8px;
}

.brand-slogan {
  font-size: 14px;
  opacity: 0.85;
  margin: 0 0 36px;
}

.brand-features {
  display: flex;
  flex-direction: column;
  gap: 14px;
  text-align: left;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
  opacity: 0.9;
  padding: 10px 16px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: var(--radius-sm);
  transition: background 0.3s;
}

.feature-item:hover {
  background: rgba(255, 255, 255, 0.18);
}

.feature-icon {
  font-size: 18px;
}

.login-card {
  flex: 1;
  padding: 48px 44px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.login-header {
  margin-bottom: 28px;
}

.login-header h1 {
  font-size: 26px;
  color: var(--text-primary);
  margin: 0 0 6px;
  font-weight: 700;
}

.login-header p {
  color: var(--text-secondary);
  font-size: 14px;
  margin: 0;
}

.role-toggle {
  display: flex;
  position: relative;
  background: #f5f7fa;
  border-radius: var(--radius-sm);
  padding: 4px;
  margin-bottom: 28px;
}

.role-option {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 10px 0;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  color: var(--text-secondary);
  transition: color 0.3s;
  position: relative;
  z-index: 1;
  border-radius: calc(var(--radius-sm) - 2px);
}

.role-option.active {
  color: var(--purple);
}

.role-option .el-icon {
  font-size: 16px;
}

.role-slider {
  position: absolute;
  top: 4px;
  left: 4px;
  width: calc(50% - 4px);
  height: calc(100% - 8px);
  background: white;
  border-radius: calc(var(--radius-sm) - 2px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.role-slider.right {
  transform: translateX(100%);
}

.login-form {
  margin-bottom: 8px;
}

.login-form :deep(.el-form-item) {
  margin-bottom: 20px;
}

.login-form :deep(.el-input__wrapper) {
  padding: 4px 12px;
}

.input-hint {
  font-size: 12px;
  color: var(--purple);
  margin-top: 6px;
  padding: 4px 10px;
  background: linear-gradient(135deg, #f8f5ff, #fff9fb);
  border-radius: 6px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.hint-icon {
  font-size: 12px;
}

.hint-fade-enter-active {
  transition: all 0.3s ease;
}

.hint-fade-leave-active {
  transition: all 0.2s ease;
}

.hint-fade-enter-from,
.hint-fade-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}

.form-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  margin-top: -4px;
}

.remember-me {
  color: var(--text-secondary);
}

.remember-me :deep(.el-checkbox__label) {
  font-size: 13px;
  color: var(--text-secondary);
}

.forgot-link {
  color: var(--purple);
  font-size: 13px;
  text-decoration: none;
  transition: color 0.3s;
}

.forgot-link:hover {
  color: var(--pink);
}

.login-btn {
  width: 100%;
  height: 48px;
  font-size: 16px;
  font-weight: 600;
  background: linear-gradient(135deg, var(--purple), #7B61A8) !important;
  border: none !important;
  letter-spacing: 4px;
  color: #fff !important;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1) !important;
  position: relative;
  overflow: hidden;
}

.login-btn::before {
  content: '';
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: radial-gradient(circle, rgba(255,255,255,0.15) 0%, transparent 70%);
  transform: scale(0);
  transition: transform 0.5s;
}

.login-btn:hover::before {
  transform: scale(1);
}

.login-btn:hover {
  background: linear-gradient(135deg, #7B61A8, var(--purple)) !important;
  transform: translateY(-2px) !important;
  box-shadow: 0 8px 24px rgba(149, 125, 173, 0.35) !important;
}

.login-btn:active {
  transform: translateY(0) scale(0.98) !important;
}

.login-divider {
  display: flex;
  align-items: center;
  margin: 20px 0;
  color: var(--text-secondary);
  font-size: 12px;
}

.login-divider::before,
.login-divider::after {
  content: '';
  flex: 1;
  height: 1px;
  background: #eee;
}

.login-divider span {
  padding: 0 16px;
}

.login-footer {
  text-align: center;
  color: var(--text-secondary);
  font-size: 14px;
  margin-bottom: 16px;
}

.login-footer .link {
  color: var(--purple);
  text-decoration: none;
  font-weight: 600;
  margin-left: 6px;
  transition: color 0.3s;
}

.login-footer .link:hover {
  color: var(--pink);
}

.login-hint {
  text-align: center;
  color: #bbb;
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  padding: 10px;
  background: #fafafa;
  border-radius: var(--radius-sm);
}

.login-hint .el-icon {
  font-size: 14px;
}

/* Button text transition */
.btn-text-enter-active,
.btn-text-leave-active {
  transition: all 0.2s ease;
}
.btn-text-enter-from {
  opacity: 0;
  transform: translateY(6px);
}
.btn-text-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}

@media (max-width: 768px) {
  .login-wrapper {
    flex-direction: column;
  }
  .login-brand {
    width: 100%;
    padding: 32px 24px;
  }
  .brand-features {
    display: none;
  }
  .login-card {
    padding: 32px 24px;
  }
}
</style>
