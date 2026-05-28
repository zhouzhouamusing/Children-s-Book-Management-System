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
    <div class="register-wrapper">
      <div class="register-brand animate__animated animate__fadeInLeft">
        <div class="brand-content">
          <div class="brand-logo">🎨</div>
          <h2>加入我们</h2>
          <p class="brand-slogan">创建你的管理员账号</p>
          <div class="brand-steps">
            <div class="step-item" :class="{ active: currentStep >= 1, done: currentStep > 1 }">
              <div class="step-circle">{{ currentStep > 1 ? '✓' : '1' }}</div>
              <span>账号信息</span>
            </div>
            <div class="step-line" :class="{ active: currentStep > 1 }"></div>
            <div class="step-item" :class="{ active: currentStep >= 2, done: currentStep > 2 }">
              <div class="step-circle">{{ currentStep > 2 ? '✓' : '2' }}</div>
              <span>安全设置</span>
            </div>
            <div class="step-line" :class="{ active: currentStep > 2 }"></div>
            <div class="step-item" :class="{ active: currentStep >= 3 }">
              <div class="step-circle">3</div>
              <span>完成注册</span>
            </div>
          </div>
        </div>
      </div>
      <div class="register-card animate__animated animate__fadeInRight">
        <div class="register-header">
          <h1>{{ stepTitle }}</h1>
          <p>{{ stepDesc }}</p>
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
          <transition name="step-slide" mode="out-in">
            <!-- Step 1: Account Info -->
            <div v-if="currentStep === 1" key="step1">
              <el-form-item prop="username" label="用户名">
                <el-input
                  v-model="form.username"
                  placeholder="3-20个字符，将作为登录凭证"
                  prefix-icon="User"
                  size="large"
                  clearable
                  maxlength="20"
                  show-word-limit
                />
              </el-form-item>
              <el-form-item prop="nickname" label="昵称">
                <el-input
                  v-model="form.nickname"
                  placeholder="请输入你的昵称"
                  prefix-icon="Star"
                  size="large"
                  clearable
                  maxlength="30"
                />
              </el-form-item>
              <el-form-item prop="email" label="邮箱">
                <el-input
                  v-model="form.email"
                  placeholder="用于找回密码，请填写有效邮箱"
                  prefix-icon="Message"
                  size="large"
                  clearable
                />
              </el-form-item>
            </div>

            <!-- Step 2: Security -->
            <div v-else-if="currentStep === 2" key="step2">
              <el-form-item prop="password" label="设置密码">
                <el-input
                  v-model="form.password"
                  type="password"
                  placeholder="至少6位字符"
                  prefix-icon="Lock"
                  size="large"
                  show-password
                />
              </el-form-item>
              <div class="password-strength" v-if="form.password">
                <div class="strength-bars">
                  <div class="strength-bar" :class="{ active: passwordStrength >= 1 }"></div>
                  <div class="strength-bar" :class="{ active: passwordStrength >= 2 }"></div>
                  <div class="strength-bar" :class="{ active: passwordStrength >= 3 }"></div>
                </div>
                <span class="strength-text" :class="strengthClass">{{ strengthLabel }}</span>
              </div>
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
            </div>

            <!-- Step 3: Confirm -->
            <div v-else key="step3">
              <div class="confirm-info">
                <div class="confirm-item">
                  <span class="confirm-label">用户名</span>
                  <span class="confirm-value">{{ form.username }}</span>
                </div>
                <div class="confirm-item">
                  <span class="confirm-label">昵称</span>
                  <span class="confirm-value">{{ form.nickname }}</span>
                </div>
                <div class="confirm-item">
                  <span class="confirm-label">邮箱</span>
                  <span class="confirm-value">{{ form.email }}</span>
                </div>
                <div class="confirm-item">
                  <span class="confirm-label">密码</span>
                  <span class="confirm-value">••••••••</span>
                </div>
              </div>
              <div class="confirm-notice">
                <el-icon><InfoFilled /></el-icon>
                <span>请确认以上信息无误后提交注册</span>
              </div>
            </div>
          </transition>

          <div class="form-buttons">
            <el-button
              v-if="currentStep > 1"
              size="large"
              round
              @click="prevStep"
              class="prev-btn"
            >
              上一步
            </el-button>
            <el-button
              v-if="currentStep < 3"
              type="primary"
              size="large"
              round
              @click="nextStep"
              class="next-btn"
            >
              下一步
            </el-button>
            <el-button
              v-if="currentStep === 3"
              type="primary"
              size="large"
              round
              :loading="loading"
              @click="handleRegister"
              class="submit-btn"
            >
              {{ loading ? '注册中...' : '🎉 完成注册' }}
            </el-button>
          </div>
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
import { register } from '@/api'

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)
const currentStep = ref(1)

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

const stepTitle = computed(() => {
  const titles = { 1: '创建账号', 2: '设置密码', 3: '确认信息' }
  return titles[currentStep.value]
})

const stepDesc = computed(() => {
  const descs = { 1: '填写基本账号信息', 2: '设置安全的登录密码', 3: '检查信息并完成注册' }
  return descs[currentStep.value]
})

const passwordStrength = computed(() => {
  const p = form.password
  if (!p) return 0
  let score = 0
  if (p.length >= 6) score++
  if (/[A-Z]/.test(p) && /[a-z]/.test(p)) score++
  if (/[0-9]/.test(p) && /[^A-Za-z0-9]/.test(p)) score++
  return score
})

const strengthLabel = computed(() => {
  const labels = ['', '弱', '中', '强']
  return labels[passwordStrength.value]
})

const strengthClass = computed(() => {
  const classes = ['', 'weak', 'medium', 'strong']
  return classes[passwordStrength.value]
})

const passwordMatch = computed(() => {
  return form.password && form.confirmPassword && form.password === form.confirmPassword
})

const nextStep = async () => {
  if (currentStep.value === 1) {
    const fields = ['username', 'nickname', 'email']
    try {
      await formRef.value.validateField(fields)
      currentStep.value = 2
    } catch (e) {}
  } else if (currentStep.value === 2) {
    const fields = ['password', 'confirmPassword']
    try {
      await formRef.value.validateField(fields)
      currentStep.value = 3
    } catch (e) {}
  }
}

const prevStep = () => {
  if (currentStep.value > 1) currentStep.value--
}

const handleRegister = async () => {
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
  opacity: 0.4;
  animation: float 6s ease-in-out infinite;
}

.shape-1 { width: 120px; height: 120px; background: var(--peach); top: 6%; left: 6%; animation-delay: 0s; }
.shape-2 { width: 80px; height: 80px; background: var(--green); top: 14%; right: 10%; animation-delay: 1s; }
.shape-3 { width: 100px; height: 100px; background: var(--blue); bottom: 10%; left: 14%; animation-delay: 2s; }
.shape-4 { width: 60px; height: 60px; background: var(--yellow-warm); bottom: 18%; right: 18%; animation-delay: 3s; }
.shape-5 { width: 90px; height: 90px; background: var(--purple-light); top: 42%; left: 4%; animation-delay: 4s; }
.shape-6 { width: 70px; height: 70px; background: var(--pink-light); top: 58%; right: 6%; animation-delay: 5s; }

.register-wrapper {
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

.register-brand {
  width: 320px;
  background: linear-gradient(135deg, #FF8A9E 0%, var(--pink) 50%, var(--purple-light) 100%);
  padding: 48px 32px;
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
  font-size: 56px;
  margin-bottom: 14px;
  animation: float 3s ease-in-out infinite;
}

.brand-content h2 {
  font-size: 26px;
  font-weight: 700;
  margin: 0 0 8px;
}

.brand-slogan {
  font-size: 14px;
  opacity: 0.85;
  margin: 0 0 40px;
}

.brand-steps {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0;
}

.step-item {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
  opacity: 0.5;
  transition: all 0.3s;
}

.step-item.active {
  opacity: 1;
}

.step-circle {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 600;
  transition: all 0.3s;
}

.step-item.active .step-circle {
  background: white;
  color: var(--purple);
}

.step-item.done .step-circle {
  background: #B5EAD7;
  color: #2d8a56;
}

.step-line {
  width: 2px;
  height: 20px;
  background: rgba(255, 255, 255, 0.2);
  margin: 6px 0 6px 14px;
  transition: background 0.3s;
}

.step-line.active {
  background: rgba(255, 255, 255, 0.7);
}

.register-card {
  flex: 1;
  padding: 44px 40px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.register-header {
  margin-bottom: 28px;
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

.register-form {
  margin-bottom: 16px;
}

.register-form :deep(.el-form-item__label) {
  font-weight: 500;
  color: var(--text-primary);
  padding-bottom: 4px;
}

.password-strength {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: -12px;
  margin-bottom: 16px;
}

.strength-bars {
  display: flex;
  gap: 4px;
}

.strength-bar {
  width: 40px;
  height: 4px;
  border-radius: 2px;
  background: #eee;
  transition: background 0.3s;
}

.strength-bar.active:nth-child(1) { background: #FF6B81; }
.strength-bar.active:nth-child(2) { background: #FFEAA7; }
.strength-bar.active:nth-child(3) { background: #B5EAD7; }

.strength-text {
  font-size: 12px;
  font-weight: 500;
}
.strength-text.weak { color: #FF6B81; }
.strength-text.medium { color: #F39C12; }
.strength-text.strong { color: #27AE60; }

.password-match {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  margin-top: -8px;
  margin-bottom: 12px;
}

.password-match .match { color: #27AE60; }
.password-match .no-match { color: #FF6B81; }

.confirm-info {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 20px;
  background: #fafafa;
  border-radius: var(--radius-sm);
  margin-bottom: 16px;
}

.confirm-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.confirm-label {
  font-size: 14px;
  color: var(--text-secondary);
}

.confirm-value {
  font-size: 14px;
  color: var(--text-primary);
  font-weight: 500;
}

.confirm-notice {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 12px 16px;
  background: #f0f9ff;
  border-radius: var(--radius-sm);
  font-size: 13px;
  color: var(--purple);
  margin-bottom: 20px;
}

.form-buttons {
  display: flex;
  gap: 12px;
  margin-top: 8px;
}

.prev-btn {
  flex: 1;
  height: 46px;
  font-size: 15px;
}

.next-btn,
.submit-btn {
  flex: 2;
  height: 46px;
  font-size: 15px;
  background: linear-gradient(135deg, var(--purple), var(--pink)) !important;
  border: none !important;
  color: white !important;
}

.next-btn:hover,
.submit-btn:hover {
  background: linear-gradient(135deg, var(--pink), var(--purple)) !important;
  transform: translateY(-2px) !important;
  box-shadow: 0 8px 24px rgba(149, 125, 173, 0.3) !important;
}

.register-footer {
  text-align: center;
  color: var(--text-secondary);
  font-size: 14px;
  margin-top: 20px;
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

/* Step transition */
.step-slide-enter-active {
  transition: all 0.3s ease;
}
.step-slide-leave-active {
  transition: all 0.2s ease;
}
.step-slide-enter-from {
  opacity: 0;
  transform: translateX(20px);
}
.step-slide-leave-to {
  opacity: 0;
  transform: translateX(-20px);
}

@media (max-width: 768px) {
  .register-wrapper {
    flex-direction: column;
  }
  .register-brand {
    width: 100%;
    padding: 28px 24px;
  }
  .brand-steps {
    flex-direction: row;
    gap: 0;
  }
  .step-line {
    width: 20px;
    height: 2px;
    margin: 0 6px;
  }
  .register-card {
    padding: 28px 24px;
  }
}
</style>
