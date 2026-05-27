<template>
  <div class="forgot-container">
    <div class="forgot-bg">
      <div class="floating-shape shape-1"></div>
      <div class="floating-shape shape-2"></div>
      <div class="floating-shape shape-3"></div>
      <div class="floating-shape shape-4"></div>
      <div class="floating-shape shape-5"></div>
    </div>
    <div class="forgot-card animate__animated animate__bounceIn">
      <div class="forgot-header">
        <div class="logo animate-float">🔑</div>
        <h1>找回密码</h1>
        <p>请输入注册时的用户名和邮箱来重置密码</p>
      </div>

      <!-- 步骤指示器 -->
      <div class="steps-indicator">
        <div class="step" :class="{ active: step >= 1, done: step > 1 }">
          <span class="step-num">{{ step > 1 ? '✓' : '1' }}</span>
          <span class="step-label">验证身份</span>
        </div>
        <div class="step-line" :class="{ active: step > 1 }"></div>
        <div class="step" :class="{ active: step >= 2 }">
          <span class="step-num">2</span>
          <span class="step-label">设置新密码</span>
        </div>
      </div>

      <!-- 步骤1: 验证身份 -->
      <el-form
        v-if="step === 1"
        ref="verifyFormRef"
        :model="form"
        :rules="verifyRules"
        class="forgot-form"
        @keyup.enter="handleVerify"
      >
        <el-form-item prop="username">
          <el-input
            v-model="form.username"
            placeholder="请输入用户名"
            prefix-icon="User"
            size="large"
          />
        </el-form-item>
        <el-form-item prop="email">
          <el-input
            v-model="form.email"
            placeholder="请输入注册时的邮箱"
            prefix-icon="Message"
            size="large"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            size="large"
            class="forgot-btn"
            @click="handleVerify"
          >
            下一步 →
          </el-button>
        </el-form-item>
      </el-form>

      <!-- 步骤2: 设置新密码 -->
      <el-form
        v-else
        ref="resetFormRef"
        :model="form"
        :rules="resetRules"
        class="forgot-form"
        @keyup.enter="handleReset"
      >
        <div class="verified-info">
          <el-tag type="success" effect="light" size="large">
            ✓ 身份已验证：{{ form.username }}
          </el-tag>
        </div>
        <el-form-item prop="newPassword">
          <el-input
            v-model="form.newPassword"
            type="password"
            placeholder="请输入新密码（至少6位）"
            prefix-icon="Lock"
            size="large"
            show-password
          />
        </el-form-item>
        <el-form-item prop="confirmPassword">
          <el-input
            v-model="form.confirmPassword"
            type="password"
            placeholder="请再次输入新密码"
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
            class="forgot-btn"
            @click="handleReset"
          >
            {{ loading ? '重置中...' : '🔐 重置密码' }}
          </el-button>
        </el-form-item>
        <el-form-item>
          <el-button
            size="large"
            class="back-step-btn"
            @click="step = 1"
          >
            ← 返回上一步
          </el-button>
        </el-form-item>
      </el-form>

      <div class="forgot-footer">
        <router-link to="/login" class="link">← 返回登录</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { resetPassword } from '@/api'

const router = useRouter()
const verifyFormRef = ref(null)
const resetFormRef = ref(null)
const loading = ref(false)
const step = ref(1)

const form = reactive({
  username: '',
  email: '',
  newPassword: '',
  confirmPassword: ''
})

const verifyRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ]
}

const validateConfirmPassword = (rule, value, callback) => {
  if (value !== form.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const resetRules = {
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度需在6-32个字符之间', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

const handleVerify = async () => {
  const valid = await verifyFormRef.value.validate().catch(() => false)
  if (!valid) return
  step.value = 2
}

const handleReset = async () => {
  const valid = await resetFormRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await resetPassword(form)
    ElMessage.success('密码重置成功！请使用新密码登录')
    await router.push('/login')
  } catch (e) {
    console.error('密码重置失败:', e)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.forgot-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #FFF5F5 0%, #F0FFF4 50%, #EBF8FF 100%);
  position: relative;
  overflow: hidden;
  padding: 20px;
}

.forgot-bg {
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
  width: 110px;
  height: 110px;
  background: var(--yellow-warm);
  top: 10%;
  left: 12%;
  animation-delay: 0s;
}

.shape-2 {
  width: 75px;
  height: 75px;
  background: var(--pink-light);
  top: 18%;
  right: 10%;
  animation-delay: 1.5s;
}

.shape-3 {
  width: 95px;
  height: 95px;
  background: var(--green);
  bottom: 15%;
  left: 18%;
  animation-delay: 2.5s;
}

.shape-4 {
  width: 65px;
  height: 65px;
  background: var(--blue);
  bottom: 25%;
  right: 15%;
  animation-delay: 3.5s;
}

.shape-5 {
  width: 85px;
  height: 85px;
  background: var(--purple-light);
  top: 55%;
  left: 6%;
  animation-delay: 4.5s;
}

.forgot-card {
  background: white;
  border-radius: var(--radius-lg);
  padding: 40px 40px 30px;
  width: 450px;
  max-width: 100%;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.08);
  position: relative;
  z-index: 1;
}

.forgot-header {
  text-align: center;
  margin-bottom: 24px;
}

.logo {
  font-size: 50px;
  margin-bottom: 10px;
}

.forgot-header h1 {
  font-size: 26px;
  color: var(--text-primary);
  margin-bottom: 8px;
  background: linear-gradient(135deg, var(--purple), var(--pink));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.forgot-header p {
  color: var(--text-secondary);
  font-size: 14px;
}

.steps-indicator {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 28px;
  gap: 0;
}

.step {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
}

.step-num {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
  background: #F0F0F0;
  color: #999;
  transition: all 0.3s;
}

.step.active .step-num {
  background: linear-gradient(135deg, var(--green), #8DD5BE);
  color: white;
}

.step.done .step-num {
  background: linear-gradient(135deg, var(--green), #8DD5BE);
  color: white;
}

.step-label {
  font-size: 12px;
  color: #999;
  transition: color 0.3s;
}

.step.active .step-label {
  color: var(--text-primary);
  font-weight: 500;
}

.step-line {
  width: 60px;
  height: 3px;
  background: #F0F0F0;
  border-radius: 2px;
  margin: 0 12px;
  margin-bottom: 20px;
  transition: background 0.3s;
}

.step-line.active {
  background: linear-gradient(135deg, var(--green), #8DD5BE);
}

.forgot-form {
  margin-bottom: 16px;
}

.verified-info {
  text-align: center;
  margin-bottom: 20px;
}

.forgot-btn {
  width: 100%;
  height: 48px;
  font-size: 16px;
  background: linear-gradient(135deg, var(--green), #8DD5BE) !important;
  border: none !important;
  letter-spacing: 2px;
  color: #fff !important;
}

.forgot-btn:hover {
  background: linear-gradient(135deg, #8DD5BE, var(--green)) !important;
}

.back-step-btn {
  width: 100%;
  height: 42px;
  font-size: 14px;
  background: transparent !important;
  border: 2px solid var(--green) !important;
  color: var(--text-primary) !important;
}

.back-step-btn:hover {
  background: var(--green-light) !important;
}

.forgot-footer {
  text-align: center;
  color: var(--text-secondary);
  font-size: 14px;
}

.forgot-footer .link {
  color: var(--purple);
  text-decoration: none;
  font-weight: 600;
  transition: color 0.3s;
}

.forgot-footer .link:hover {
  color: var(--pink);
}
</style>
