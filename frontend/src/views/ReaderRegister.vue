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
        <div class="logo animate-float">📖</div>
        <h1>读者注册</h1>
        <p>注册读者账号以使用个人中心</p>
      </div>
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        class="login-form"
        label-position="top"
      >
        <el-form-item prop="username" label="用户名">
          <el-input v-model="form.username" placeholder="3-20位字符" prefix-icon="User" size="large" />
        </el-form-item>
        <el-form-item prop="name" label="儿童姓名">
          <el-input v-model="form.name" placeholder="请输入注册时的儿童姓名" prefix-icon="UserFilled" size="large" />
        </el-form-item>
        <el-form-item prop="parentPhone" label="家长手机号">
          <el-input v-model="form.parentPhone" placeholder="请输入注册时的家长手机号" prefix-icon="Phone" size="large" />
        </el-form-item>
        <el-form-item prop="password" label="密码">
          <el-input v-model="form.password" type="password" placeholder="6-32位字符" prefix-icon="Lock" size="large" show-password />
        </el-form-item>
        <el-form-item prop="confirmPassword" label="确认密码">
          <el-input v-model="form.confirmPassword" type="password" placeholder="请再次输入密码" prefix-icon="Lock" size="large" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" :loading="loading" class="login-btn" @click="handleRegister">
            {{ loading ? '注册中...' : '注册账号' }}
          </el-button>
        </el-form-item>
      </el-form>
      <div class="login-footer">
        <span>已有账号？</span>
        <router-link to="/login" class="link">返回登录</router-link>
      </div>
      <div class="login-hint">
        <span>需先由管理员添加读者信息后才可注册</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
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

.shape-1 { width: 120px; height: 120px; background: var(--pink-light); top: 10%; left: 10%; animation-delay: 0s; }
.shape-2 { width: 80px; height: 80px; background: var(--green); top: 20%; right: 15%; animation-delay: 1s; }
.shape-3 { width: 100px; height: 100px; background: var(--blue); bottom: 15%; left: 20%; animation-delay: 2s; }
.shape-4 { width: 60px; height: 60px; background: var(--yellow-warm); bottom: 25%; right: 25%; animation-delay: 3s; }
.shape-5 { width: 90px; height: 90px; background: var(--purple-light); top: 50%; left: 50%; animation-delay: 4s; }

.login-card {
  background: white;
  border-radius: var(--radius-lg);
  padding: 40px 40px 30px;
  width: 420px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.08);
  position: relative;
  z-index: 1;
}

.login-header {
  text-align: center;
  margin-bottom: 24px;
}

.logo { font-size: 48px; margin-bottom: 10px; }

.login-header h1 {
  font-size: 24px;
  color: var(--text-primary);
  margin-bottom: 6px;
  background: linear-gradient(135deg, var(--purple), var(--pink));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.login-header p { color: var(--text-secondary); font-size: 13px; }

.login-form { margin-bottom: 16px; }

.login-btn {
  width: 100%;
  height: 46px;
  font-size: 15px;
  background: linear-gradient(135deg, var(--purple), var(--pink)) !important;
  border: none !important;
  letter-spacing: 2px;
  color: #fff !important;
}

.login-btn:hover {
  background: linear-gradient(135deg, var(--pink), var(--purple)) !important;
}

.login-footer { text-align: center; color: var(--text-secondary); font-size: 14px; margin-bottom: 12px; }
.login-footer .link { color: var(--purple); text-decoration: none; font-weight: 600; margin-left: 6px; transition: color 0.3s; }
.login-footer .link:hover { color: var(--pink); }
.login-hint { text-align: center; color: #BBB; font-size: 12px; }
</style>
