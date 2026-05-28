<template>
  <div class="register-container">
    <div class="register-bg">
      <div class="floating-shape shape-1"></div>
      <div class="floating-shape shape-2"></div>
      <div class="floating-shape shape-3"></div>
      <div class="floating-shape shape-4"></div>
      <div class="floating-shape shape-5"></div>
    </div>
    <div class="register-wrapper">
      <div class="register-brand animate__animated animate__fadeInLeft">
        <div class="brand-content">
          <div class="brand-logo">📖</div>
          <h2>读者注册</h2>
          <p class="brand-slogan">注册账号使用个人中心</p>
          <div class="brand-info">
            <div class="info-card">
              <span class="info-icon">💡</span>
              <span>填写基本信息即可注册</span>
            </div>
            <div class="info-card">
              <span class="info-icon">🔑</span>
              <span>注册后即可登录使用</span>
            </div>
            <div class="info-card">
              <span class="info-icon">📚</span>
              <span>享受图书借阅服务</span>
            </div>
          </div>
        </div>
      </div>
      <div class="register-card animate__animated animate__fadeInRight">
        <div class="register-header">
          <h1>创建读者账号</h1>
          <p>请填写以下信息完成注册</p>
        </div>
        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          class="register-form"
          label-position="top"
          hide-required-asterisk
          status-icon
        >
          <el-form-item prop="username" label="用户名">
            <el-input
              v-model="form.username"
              placeholder="3-20位字符，作为登录凭证"
              prefix-icon="User"
              size="large"
              clearable
            />
          </el-form-item>
          <el-form-item prop="name" label="儿童姓名">
            <el-input
              v-model="form.name"
              placeholder="请输入儿童姓名"
              prefix-icon="UserFilled"
              size="large"
              clearable
            />
          </el-form-item>
          <el-form-item prop="parentPhone" label="家长手机号">
            <el-input
              v-model="form.parentPhone"
              placeholder="请输入家长手机号"
              prefix-icon="Phone"
              size="large"
              clearable
            />
          </el-form-item>
          <el-form-item prop="age" label="儿童年龄">
            <el-input-number
              v-model="form.age"
              :min="1"
              :max="18"
              placeholder="选填"
              size="large"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item prop="gender" label="性别">
            <el-radio-group v-model="form.gender" size="large">
              <el-radio label="男">男</el-radio>
              <el-radio label="女">女</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item prop="password" label="设置密码">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="6-32位字符"
              prefix-icon="Lock"
              size="large"
              show-password
            />
          </el-form-item>
          <el-form-item prop="confirmPassword" label="确认密码">
            <el-input
              v-model="form.confirmPassword"
              type="password"
              placeholder="请再次输入密码"
              prefix-icon="Lock"
              size="large"
              show-password
            />
          </el-form-item>
          <div class="password-match" v-if="form.confirmPassword">
            <el-icon :class="passwordMatch ? 'match' : 'no-match'">
              <CircleCheck v-if="passwordMatch" />
              <CircleClose v-else />
            </el-icon>
            <span :class="passwordMatch ? 'match' : 'no-match'">
              {{ passwordMatch ? '密码一致' : '密码不一致' }}
            </span>
          </div>
          <el-form-item>
            <el-button
              type="primary"
              size="large"
              :loading="loading"
              class="register-btn"
              round
              @click="handleRegister"
            >
              {{ loading ? '注册中...' : '注册账号' }}
            </el-button>
          </el-form-item>
        </el-form>
        <div class="register-footer">
          <span>已有账号？</span>
          <router-link to="/login" class="link">返回登录</router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { readerRegister } from '@/api'

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  username: '',
  name: '',
  parentPhone: '',
  age: null,
  gender: '',
  password: '',
  confirmPassword: ''
})

const validateConfirm = (rule, value, callback) => {
  if (value !== form.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度为3-20位', trigger: 'blur' }
  ],
  name: [{ required: true, message: '请输入儿童姓名', trigger: 'blur' }],
  parentPhone: [
    { required: true, message: '请输入家长手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度为6-32位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirm, trigger: 'blur' }
  ]
}

const passwordMatch = computed(() => {
  return form.password && form.confirmPassword && form.password === form.confirmPassword
})

const handleRegister = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await readerRegister(form)
    ElMessage.success('注册成功！请返回登录')
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
  opacity: 0.4;
  animation: float 6s ease-in-out infinite;
}

.shape-1 { width: 120px; height: 120px; background: var(--green-light); top: 8%; left: 8%; animation-delay: 0s; }
.shape-2 { width: 80px; height: 80px; background: var(--pink); top: 16%; right: 12%; animation-delay: 1s; }
.shape-3 { width: 100px; height: 100px; background: var(--blue); bottom: 12%; left: 16%; animation-delay: 2s; }
.shape-4 { width: 60px; height: 60px; background: var(--yellow-warm); bottom: 20%; right: 20%; animation-delay: 3s; }
.shape-5 { width: 90px; height: 90px; background: var(--purple-light); top: 50%; left: 4%; animation-delay: 4s; }

.register-wrapper {
  display: flex;
  background: white;
  border-radius: var(--radius-lg);
  box-shadow: 0 24px 80px rgba(0, 0, 0, 0.08);
  overflow: hidden;
  position: relative;
  z-index: 1;
  max-width: 860px;
  width: 100%;
}

.register-brand {
  width: 300px;
  background: linear-gradient(135deg, var(--green) 0%, #8DD5BE 50%, #6BC4A8 100%);
  padding: 44px 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
}

.register-brand::before {
  content: '';
  position: absolute;
  inset: 0;
  background: radial-gradient(circle at 30% 20%, rgba(255,255,255,0.12) 0%, transparent 50%);
}

.brand-content {
  position: relative;
  z-index: 1;
  text-align: center;
  color: white;
}

.brand-logo {
  font-size: 52px;
  margin-bottom: 14px;
  animation: float 3s ease-in-out infinite;
}

.brand-content h2 {
  font-size: 24px;
  font-weight: 700;
  margin: 0 0 6px;
}

.brand-slogan {
  font-size: 13px;
  opacity: 0.85;
  margin: 0 0 32px;
}

.brand-info {
  display: flex;
  flex-direction: column;
  gap: 10px;
  text-align: left;
}

.info-card {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
  padding: 10px 14px;
  background: rgba(255, 255, 255, 0.15);
  border-radius: var(--radius-sm);
  transition: background 0.3s;
}

.info-card:hover {
  background: rgba(255, 255, 255, 0.25);
}

.info-icon {
  font-size: 16px;
}

.register-card {
  flex: 1;
  padding: 40px 36px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.register-header {
  margin-bottom: 24px;
}

.register-header h1 {
  font-size: 24px;
  color: var(--text-primary);
  margin: 0 0 6px;
  font-weight: 700;
}

.register-header p {
  color: var(--text-secondary);
  font-size: 14px;
  margin: 0;
}

.register-form :deep(.el-form-item__label) {
  font-weight: 500;
  color: var(--text-primary);
  padding-bottom: 4px;
}

.register-form :deep(.el-form-item) {
  margin-bottom: 18px;
}

.password-match {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  margin-top: -10px;
  margin-bottom: 14px;
}

.password-match .match { color: #27AE60; }
.password-match .no-match { color: #FF6B81; }

.register-btn {
  width: 100%;
  height: 46px;
  font-size: 15px;
  font-weight: 600;
  background: linear-gradient(135deg, var(--green), #8DD5BE) !important;
  border: none !important;
  letter-spacing: 2px;
  color: #fff !important;
}

.register-btn:hover {
  background: linear-gradient(135deg, #8DD5BE, var(--green)) !important;
  transform: translateY(-2px) !important;
  box-shadow: 0 8px 24px rgba(181, 234, 215, 0.4) !important;
}

.register-footer {
  text-align: center;
  color: var(--text-secondary);
  font-size: 14px;
  margin-top: 8px;
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

@media (max-width: 768px) {
  .register-wrapper {
    flex-direction: column;
  }
  .register-brand {
    width: 100%;
    padding: 28px 24px;
  }
  .brand-info {
    display: none;
  }
  .register-card {
    padding: 28px 24px;
  }
}
</style>
