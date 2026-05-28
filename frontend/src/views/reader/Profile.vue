<template>
  <div class="profile-page">
    <div class="profile-header animate__animated animate__fadeInDown">
      <div class="avatar-section">
        <div class="avatar-circle" :class="profile.gender">
          <span>{{ profile.gender === 'male' ? '👦' : '👧' }}</span>
        </div>
        <div class="user-basic">
          <h2>{{ profile.name }}</h2>
          <el-tag :type="profile.status === 'normal' ? 'success' : 'danger'" effect="light" round size="small">
            {{ profile.status === 'normal' ? '正常' : '暂停借阅' }}
          </el-tag>
        </div>
      </div>
      <div class="profile-stats">
        <div class="pstat">
          <span class="pstat-value">{{ profile.borrowCount || 0 }}</span>
          <span class="pstat-label">累计借阅</span>
        </div>
        <div class="pstat">
          <span class="pstat-value">{{ profile.overdueCount || 0 }}</span>
          <span class="pstat-label">逾期次数</span>
        </div>
        <div class="pstat">
          <span class="pstat-value">{{ profile.age || '-' }}</span>
          <span class="pstat-label">年龄</span>
        </div>
      </div>
    </div>

    <div class="profile-form-section animate__animated animate__fadeInUp" style="animation-delay: 0.15s">
      <div class="section-header">
        <h3>个人信息</h3>
        <el-button v-if="!editing" type="primary" text @click="startEdit">
          <el-icon><Edit /></el-icon> 编辑信息
        </el-button>
      </div>
      <el-form
        ref="formRef"
        :model="editForm"
        :rules="rules"
        label-width="100px"
        :disabled="!editing"
        class="profile-form"
      >
        <el-row :gutter="24">
          <el-col :span="12">
            <el-form-item label="姓名" prop="name">
              <el-input v-model="editForm.name" placeholder="请输入姓名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="年龄" prop="age">
              <el-input-number v-model="editForm.age" :min="1" :max="18" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="24">
          <el-col :span="12">
            <el-form-item label="性别" prop="gender">
              <el-radio-group v-model="editForm.gender">
                <el-radio value="male">男</el-radio>
                <el-radio value="female">女</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="家长姓名" prop="parentName">
              <el-input v-model="editForm.parentName" placeholder="请输入家长姓名" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="24">
          <el-col :span="12">
            <el-form-item label="家长电话" prop="parentPhone">
              <el-input v-model="editForm.parentPhone" placeholder="请输入手机号" />
            </el-form-item>
          </el-col>
        </el-row>
        <div class="form-actions" v-if="editing">
          <el-button @click="cancelEdit">取消</el-button>
          <el-button type="primary" :loading="saving" @click="saveProfile">
            保存修改
          </el-button>
        </div>
      </el-form>
    </div>

    <div class="account-section animate__animated animate__fadeInUp" style="animation-delay: 0.3s">
      <div class="section-header">
        <h3>账号信息</h3>
      </div>
      <div class="account-info">
        <div class="account-item">
          <span class="account-label">用户名</span>
          <span class="account-value">{{ localStorage.getItem('nickname') || '-' }}</span>
        </div>
        <div class="account-item">
          <span class="account-label">注册时间</span>
          <span class="account-value">{{ formatDate(profile.createTime) }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getMyProfile, updateMyProfile } from '@/api'

const formRef = ref(null)
const editing = ref(false)
const saving = ref(false)
const profile = reactive({
  name: '',
  age: 0,
  gender: 'male',
  parentName: '',
  parentPhone: '',
  status: 'normal',
  borrowCount: 0,
  overdueCount: 0,
  createTime: ''
})

const editForm = reactive({
  name: '',
  age: 0,
  gender: 'male',
  parentName: '',
  parentPhone: ''
})

const rules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  age: [{ required: true, message: '请输入年龄', trigger: 'blur' }],
  gender: [{ required: true, message: '请选择性别', trigger: 'change' }],
  parentPhone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ]
}

const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  return dateStr.substring(0, 10)
}

const fetchProfile = async () => {
  try {
    const res = await getMyProfile()
    Object.assign(profile, res.data)
    syncEditForm()
  } catch (e) {
    console.error('获取个人信息失败:', e)
  }
}

const syncEditForm = () => {
  editForm.name = profile.name
  editForm.age = profile.age
  editForm.gender = profile.gender
  editForm.parentName = profile.parentName
  editForm.parentPhone = profile.parentPhone
}

const startEdit = () => {
  syncEditForm()
  editing.value = true
}

const cancelEdit = () => {
  editing.value = false
  syncEditForm()
}

const saveProfile = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    await updateMyProfile(editForm)
    Object.assign(profile, editForm)
    editing.value = false
    ElMessage.success('个人信息已更新')
  } catch (e) {
    console.error('更新失败:', e)
  } finally {
    saving.value = false
  }
}

onMounted(fetchProfile)
</script>

<style scoped>
.profile-page {
  max-width: 900px;
  margin: 0 auto;
}

.profile-header {
  background: white;
  border-radius: var(--radius-lg);
  padding: 30px;
  box-shadow: var(--shadow-soft);
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.avatar-section {
  display: flex;
  align-items: center;
  gap: 20px;
}

.avatar-circle {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 36px;
  transition: transform 0.3s;
}

.avatar-circle.male {
  background: linear-gradient(135deg, var(--blue-light), var(--blue));
}

.avatar-circle.female {
  background: linear-gradient(135deg, var(--pink-light), var(--pink));
}

.avatar-circle:hover {
  transform: scale(1.1) rotate(5deg);
}

.user-basic h2 {
  font-size: 22px;
  color: var(--text-primary);
  margin: 0 0 6px;
}

.profile-stats {
  display: flex;
  gap: 32px;
}

.pstat {
  text-align: center;
}

.pstat-value {
  display: block;
  font-size: 24px;
  font-weight: 700;
  color: var(--purple);
}

.pstat-label {
  font-size: 12px;
  color: var(--text-secondary);
  margin-top: 2px;
}

.profile-form-section,
.account-section {
  background: white;
  border-radius: var(--radius-lg);
  padding: 24px 30px;
  box-shadow: var(--shadow-soft);
  margin-bottom: 24px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f5f5f5;
}

.section-header h3 {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
}

.profile-form {
  padding-top: 4px;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 16px;
  border-top: 1px solid #f5f5f5;
  margin-top: 12px;
}

.account-info {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.account-item {
  display: flex;
  align-items: center;
}

.account-label {
  width: 100px;
  font-size: 14px;
  color: var(--text-secondary);
}

.account-value {
  font-size: 14px;
  color: var(--text-primary);
  font-weight: 500;
}

@media (max-width: 768px) {
  .profile-header {
    flex-direction: column;
    gap: 20px;
  }
  .profile-stats {
    width: 100%;
    justify-content: space-around;
  }
}
</style>
