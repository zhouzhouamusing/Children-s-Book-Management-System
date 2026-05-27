<template>
  <div class="login-container">
    <div class="login-bg">
      <div class="floating-shape shape-1"></div>
      <div class="floating-shape shape-2"></div>
      <div class="floating-shape shape-3"></div>
      <div class="floating-shape shape-4"></div>
      <div class="floating-shape shape-5"></div>
    </div>
    <div class="login-card animate__animated animate__bounceIn">
      <div class="login-header">
        <div class="logo animate-float">📚</div>
        <h1>童书乐园</h1>
        <p>儿童图书管理系统</p>
      </div>
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        class="login-form"
        @keyup.enter="handleLogin"
      >
        <el-form-item prop="username">
          <el-input
            v-model="form.username"
            placeholder="请输入用户名"
            prefix-icon="User"
            size="large"
          />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            prefix-icon="Lock"
            size="large"
            show-password
          />
        </el-form-item>
        <div class="form-actions">
          <router-link to="/forgot-password" class="forgot-link">忘记密码？</router-link>
        </div>
        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            class="login-btn"
            @click="handleLogin"
          >
            {{ loading ? '登录中...' : '🚀 欢迎进入' }}
          </el-button>
        </el-form-item>
      </el-form>
      <div class="login-footer">
        <span>还没有账号？</span>
        <router-link to="/register" class="link">立即注册</router-link>
      </div>
      <div class="login-hint">
        <span>默认体验账号：admin / admin123</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login } from '@/api'

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' }
  ]
}

const handleLogin = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const res = await login(form)
    const { token, nickname } = res.data
    if (!token) {
      ElMessage.error('登录异常：未获取到凭证')
      return
    }
    localStorage.setItem('token', token)
    localStorage.setItem('nickname', nickname || '管理员')
    ElMessage.success('登录成功，欢迎回来！')
    await router.push('/dashboard')
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
}

.login-bg {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.floating-shape {
  position: absolute;
  border-radius: 50%;
  opacity: 0.5;
  animation: float 6s ease-in-out infinite;
}

.shape-1 {
  width: 120px;
  height: 120px;
  background: var(--pink-light);
  top: 10%;
  left: 10%;
  animation-delay: 0s;
}

.shape-2 {
  width: 80px;
  height: 80px;
  background: var(--green);
  top: 20%;
  right: 15%;
  animation-delay: 1s;
}

.shape-3 {
  width: 100px;
  height: 100px;
  background: var(--blue);
  bottom: 15%;
  left: 20%;
  animation-delay: 2s;
}

.shape-4 {
  width: 60px;
  height: 60px;
  background: var(--yellow-warm);
  bottom: 25%;
  right: 25%;
  animation-delay: 3s;
}

.shape-5 {
  width: 90px;
  height: 90px;
  background: var(--purple-light);
  top: 50%;
  left: 50%;
  animation-delay: 4s;
}

.login-card {
  background: white;
  border-radius: var(--radius-lg);
  padding: 50px 40px 35px;
  width: 420px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.08);
  position: relative;
  z-index: 1;
}

.login-header {
  text-align: center;
  margin-bottom: 36px;
}

.logo {
  font-size: 56px;
  margin-bottom: 12px;
}

.login-header h1 {
  font-size: 28px;
  color: var(--text-primary);
  margin-bottom: 8px;
  background: linear-gradient(135deg, var(--purple), var(--pink));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.login-header p {
  color: var(--text-secondary);
  font-size: 14px;
}

.login-form {
  margin-bottom: 20px;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 12px;
  margin-top: -6px;
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
  background: linear-gradient(135deg, var(--green), #8DD5BE) !important;
  border: none !important;
  letter-spacing: 2px;
  color: #fff !important;
}

.login-btn:hover {
  background: linear-gradient(135deg, #8DD5BE, var(--green)) !important;
}

.login-footer {
  text-align: center;
  color: var(--text-secondary);
  font-size: 14px;
  margin-bottom: 12px;
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
  color: #BBB;
  font-size: 12px;
}
</style>
