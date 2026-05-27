<template>
  <div class="register-container">
    <div class="register-bg">
      <div class="floating-shape shape-1"></div>
      <div class="floating-shape shape-2"></div>
      <div class="floating-shape shape-3"></div>
      <div class="floating-shape shape-4"></div>
      <div class="floating-shape shape-5"></div>
      <div class="floating-shape shape-6"></div>
    </div>
    <div class="register-card animate__animated animate__bounceIn">
      <div class="register-header">
        <div class="logo animate-float">🎨</div>
        <h1>加入童书乐园</h1>
        <p>创建你的管理员账号，开启阅读之旅</p>
      </div>
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        class="register-form"
        label-position="top"
        @keyup.enter="handleRegister"
      >
        <el-form-item prop="username">
          <el-input
            v-model="form.username"
            placeholder="请输入用户名（3-20个字符）"
            prefix-icon="User"
            size="large"
            maxlength="20"
          />
        </el-form-item>
        <el-form-item prop="nickname">
          <el-input
            v-model="form.nickname"
            placeholder="请输入昵称"
            prefix-icon="Star"
            size="large"
            maxlength="30"
          />
        </el-form-item>
        <el-form-item prop="email">
          <el-input
            v-model="form.email"
            placeholder="请输入邮箱（用于找回密码）"
            prefix-icon="Message"
            size="large"
          />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码（至少6位）"
            prefix-icon="Lock"
            size="large"
            show-password
          />
        </el-form-item>
        <el-form-item prop="confirmPassword">
          <el-input
            v-model="form.confirmPassword"
            type="password"
            placeholder="请再次输入密码"
            prefix-icon="Lock"
            size="large"
            show-password
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            class="register-btn"
            @click="handleRegister"
          >
            {{ loading ? '注册中...' : '🎉 立即注册' }}
          </el-button>
        </el-form-item>
      </el-form>
      <div class="register-footer">
        <span>已有账号？</span>
        <router-link to="/login" class="link">返回登录</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { register } from '@/api'

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  username: '',
  nickname: '',
  email: '',
  password: '',
  confirmPassword: ''
})

const validateConfirmPassword = (rule, value, callback) => {
  if (value !== form.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度需在3-20个字符之间', trigger: 'blur' }
  ],
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
    { max: 30, message: '昵称不能超过30个字符', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度需在6-32个字符之间', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

const handleRegister = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await register(form)
    ElMessage.success('注册成功！请使用新账号登录')
    await router.push('/login')
  } catch (e) {
    console.error('注册失败:', e)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #FFF5F5 0%, #F0FFF4 50%, #EBF8FF 100%);
  position: relative;
  overflow: hidden;
  padding: 20px;
}

.register-bg {
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
  width: 100px;
  height: 100px;
  background: var(--peach);
  top: 8%;
  left: 8%;
  animation-delay: 0s;
}

.shape-2 {
  width: 70px;
  height: 70px;
  background: var(--green);
  top: 15%;
  right: 12%;
  animation-delay: 1s;
}

.shape-3 {
  width: 90px;
  height: 90px;
  background: var(--blue);
  bottom: 12%;
  left: 15%;
  animation-delay: 2s;
}

.shape-4 {
  width: 50px;
  height: 50px;
  background: var(--yellow-warm);
  bottom: 20%;
  right: 20%;
  animation-delay: 3s;
}

.shape-5 {
  width: 80px;
  height: 80px;
  background: var(--purple-light);
  top: 45%;
  left: 5%;
  animation-delay: 4s;
}

.shape-6 {
  width: 60px;
  height: 60px;
  background: var(--pink-light);
  top: 60%;
  right: 8%;
  animation-delay: 5s;
}

.register-card {
  background: white;
  border-radius: var(--radius-lg);
  padding: 40px 40px 30px;
  width: 450px;
  max-width: 100%;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.08);
  position: relative;
  z-index: 1;
}

.register-header {
  text-align: center;
  margin-bottom: 30px;
}

.logo {
  font-size: 50px;
  margin-bottom: 10px;
}

.register-header h1 {
  font-size: 26px;
  color: var(--text-primary);
  margin-bottom: 8px;
  background: linear-gradient(135deg, var(--purple), var(--pink));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.register-header p {
  color: var(--text-secondary);
  font-size: 14px;
}

.register-form {
  margin-bottom: 16px;
}

.register-btn {
  width: 100%;
  height: 48px;
  font-size: 16px;
  background: linear-gradient(135deg, var(--purple), var(--pink)) !important;
  border: none !important;
  letter-spacing: 2px;
  color: #fff !important;
}

.register-btn:hover {
  background: linear-gradient(135deg, var(--pink), var(--purple)) !important;
}

.register-footer {
  text-align: center;
  color: var(--text-secondary);
  font-size: 14px;
}

.register-footer .link {
  color: var(--purple);
  text-decoration: none;
  font-weight: 600;
  margin-left: 6px;
  transition: color 0.3s;
}

.register-footer .link:hover {
  color: var(--pink);
}
</style>
