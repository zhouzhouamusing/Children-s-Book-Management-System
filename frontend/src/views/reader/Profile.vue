<template>
  <div class="profile-page">
    <!-- Profile Header Card -->
    <div class="profile-header animate__animated animate__fadeInDown">
      <div class="avatar-section">
        <div class="avatar-circle" :class="profile.gender">
          <span>{{ profile.gender === 'male' ? '👦' : '👧' }}</span>
          <div class="avatar-ring"></div>
        </div>
        <div class="user-basic">
          <h2>{{ profile.name || '小读者' }}</h2>
          <el-tag :type="profile.status === 'normal' ? 'success' : 'danger'" effect="light" round size="small">
            {{ profile.status === 'normal' ? '正常' : '暂停借阅' }}
          </el-tag>
          <p class="join-info">加入时间：{{ formatDate(profile.createTime) }}</p>
        </div>
      </div>
      <div class="profile-stats">
        <div class="pstat" v-for="(stat, idx) in headerStats" :key="stat.label">
          <transition name="count" mode="out-in">
            <span class="pstat-value" :key="stat.value" :style="{ color: stat.color }">{{ stat.value }}</span>
          </transition>
          <span class="pstat-label">{{ stat.label }}</span>
        </div>
      </div>
    </div>

    <!-- Points & Achievements Section -->
    <div class="points-section animate__animated animate__fadeInUp" style="animation-delay: 0.1s">
      <div class="section-header">
        <h3><span class="section-icon">⭐</span> 阅读积分</h3>
      </div>
      <div class="points-content">
        <div class="points-display">
          <div class="points-number">
            <span class="points-value">{{ statistics.totalPoints }}</span>
            <span class="points-unit">积分</span>
          </div>
          <div class="points-level">
            <div class="level-badge" :style="{ background: levelInfo.bg }">
              <span class="level-icon">{{ levelInfo.icon }}</span>
              <span class="level-text">{{ levelInfo.name }}</span>
            </div>
            <div class="level-progress">
              <el-progress
                :percentage="levelInfo.progress"
                :stroke-width="8"
                :color="levelInfo.barColor"
                :show-text="false"
              />
              <span class="level-hint">距离下一等级还需 {{ levelInfo.nextNeed }} 积分</span>
            </div>
          </div>
        </div>
        <div class="achievements-grid">
          <div
            v-for="badge in badges"
            :key="badge.name"
            class="badge-item"
            :class="{ achieved: badge.achieved }"
          >
            <span class="badge-icon">{{ badge.icon }}</span>
            <span class="badge-name">{{ badge.name }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- Reading Statistics Section -->
    <div class="stats-section animate__animated animate__fadeInUp" style="animation-delay: 0.2s">
      <div class="section-header">
        <h3><span class="section-icon">📊</span> 阅读统计</h3>
      </div>
      <div class="stats-grid">
        <div class="stat-card" v-for="stat in readingStats" :key="stat.label">
          <div class="stat-card-icon" :style="{ background: stat.bg }">{{ stat.icon }}</div>
          <div class="stat-card-info">
            <span class="stat-card-value">{{ stat.value }}</span>
            <span class="stat-card-label">{{ stat.label }}</span>
          </div>
        </div>
      </div>
      <div class="category-chart">
        <h4>阅读偏好分布</h4>
        <div class="category-bars">
          <div
            v-for="cat in statistics.categoryDistribution"
            :key="cat.name"
            class="category-bar-item"
          >
            <div class="category-bar-label">
              <span>{{ cat.name }}</span>
              <span class="category-bar-count">{{ cat.count }}本</span>
            </div>
            <div class="category-bar-bg">
              <div
                class="category-bar-fill"
                :style="{ width: cat.percentage + '%', background: cat.color }"
              ></div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Personal Info Form Section -->
    <div class="profile-form-section animate__animated animate__fadeInUp" style="animation-delay: 0.3s">
      <div class="section-header">
        <h3><span class="section-icon">📝</span> 个人信息</h3>
        <el-button v-if="!editing" v-permission="'READER_PROFILE_UPDATE'" type="primary" text @click="startEdit">
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
          <el-col :span="12">
            <el-form-item label="用户名">
              <el-input :value="profile.username || localStorage.getItem('nickname')" disabled />
            </el-form-item>
          </el-col>
        </el-row>
        <transition name="slide-fade">
          <div class="form-actions" v-if="editing">
            <el-button @click="cancelEdit" round>取消</el-button>
            <el-button v-permission="'READER_PROFILE_UPDATE'" type="primary" :loading="saving" @click="saveProfile" round>
              保存修改
            </el-button>
          </div>
        </transition>
      </el-form>
    </div>

    <!-- Apply for Admin Section -->
    <div class="admin-apply-section animate__animated animate__fadeInUp" style="animation-delay: 0.4s">
      <div class="section-header">
        <h3><span class="section-icon">🔑</span> 申请成为管理员</h3>
      </div>
      <div class="apply-content">
        <div v-if="adminApp.loading" class="apply-loading">
          <el-icon class="is-loading"><Loading /></el-icon> 加载中...
        </div>
        <div v-else-if="adminApp.hasApplication && adminApp.status === 'pending'" class="apply-status pending">
          <div class="status-icon">⏳</div>
          <div class="status-info">
            <h4>申请审核中</h4>
            <p>您的管理员申请已提交，请等待管理员审批。</p>
            <p class="apply-time">申请时间：{{ formatDate(adminApp.createTime) }}</p>
          </div>
        </div>
        <div v-else-if="adminApp.hasApplication && adminApp.status === 'approved'" class="apply-status approved">
          <div class="status-icon">✅</div>
          <div class="status-info">
            <h4>申请已通过</h4>
            <p>恭喜！您的管理员申请已通过，请使用同一账号以管理员身份登录。</p>
          </div>
        </div>
        <div v-else-if="adminApp.hasApplication && adminApp.status === 'rejected'" class="apply-status rejected">
          <div class="status-icon">❌</div>
          <div class="status-info">
            <h4>申请被拒绝</h4>
            <p v-if="adminApp.rejectReason">原因：{{ adminApp.rejectReason }}</p>
            <el-button v-permission="'ADMIN_APPLICATION_APPLY'" type="primary" size="small" round @click="adminApp.showForm = true" style="margin-top: 8px">
              重新申请
            </el-button>
          </div>
        </div>
        <div v-else class="apply-form-area">
          <p class="apply-desc">如果您希望参与图书馆的管理工作，可以提交申请。审核通过后您的账号将获得管理员权限。</p>
          <el-button v-permission="'ADMIN_APPLICATION_APPLY'" type="primary" round @click="adminApp.showForm = true" v-if="!adminApp.showForm">
            提交申请
          </el-button>
        </div>
        <transition name="slide-fade">
          <div v-if="adminApp.showForm" class="apply-form">
            <el-input
              v-model="adminApp.reason"
              type="textarea"
              :rows="3"
              placeholder="请简要说明您申请成为管理员的理由..."
              maxlength="200"
              show-word-limit
            />
            <div class="apply-form-actions">
              <el-button round @click="adminApp.showForm = false">取消</el-button>
              <el-button v-permission="'ADMIN_APPLICATION_APPLY'" type="primary" round :loading="adminApp.submitting" @click="submitAdminApplication">
                提交申请
              </el-button>
            </div>
          </div>
        </transition>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getMyProfile, updateMyProfile, getMyStatistics, getMyPoints, applyForAdmin, getMyApplicationStatus } from '@/api'

const formRef = ref(null)
const editing = ref(false)
const saving = ref(false)
const profile = reactive({
  name: '',
  age: 0,
  gender: 'male',
  parentName: '',
  parentPhone: '',
  username: '',
  status: 'normal',
  borrowCount: 0,
  overdueCount: 0,
  createTime: ''
})

const statistics = reactive({
  totalPoints: 0,
  totalBorrows: 0,
  thisMonthBorrows: 0,
  totalBooks: 0,
  readingDays: 0,
  level: '',
  categoryDistribution: []
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

const headerStats = computed(() => [
  { label: '累计借阅', value: profile.borrowCount || 0, color: '#957DAD' },
  { label: '逾期次数', value: profile.overdueCount || 0, color: '#FF8A9E' },
  { label: '年龄', value: profile.age || '-', color: '#B5EAD7' }
])

const levelInfo = computed(() => {
  const pts = statistics.totalPoints
  if (pts >= 500) return { name: '阅读大师', icon: '👑', bg: 'linear-gradient(135deg, #FFD700, #FFA500)', barColor: '#FFD700', progress: 100, nextNeed: 0 }
  if (pts >= 200) return { name: '阅读达人', icon: '🌟', bg: 'linear-gradient(135deg, #957DAD, #C4B3D4)', barColor: '#957DAD', progress: ((pts - 200) / 300) * 100, nextNeed: 500 - pts }
  if (pts >= 50) return { name: '小书虫', icon: '📖', bg: 'linear-gradient(135deg, #B5EAD7, #8DD5BE)', barColor: '#B5EAD7', progress: ((pts - 50) / 150) * 100, nextNeed: 200 - pts }
  return { name: '新手读者', icon: '🌱', bg: 'linear-gradient(135deg, #C7CEEA, #E0E5F5)', barColor: '#C7CEEA', progress: (pts / 50) * 100, nextNeed: 50 - pts }
})

const badges = computed(() => [
  { name: '首次借阅', icon: '📚', achieved: profile.borrowCount > 0 },
  { name: '阅读10本', icon: '🎯', achieved: statistics.totalBooks >= 10 },
  { name: '连续7天', icon: '🔥', achieved: statistics.readingDays >= 7 },
  { name: '全品类', icon: '🌈', achieved: statistics.categoryDistribution.length >= 5 },
  { name: '零逾期', icon: '✨', achieved: profile.borrowCount > 5 && profile.overdueCount === 0 },
  { name: '月度之星', icon: '⭐', achieved: statistics.thisMonthBorrows >= 5 }
])

const readingStats = computed(() => [
  { icon: '📚', label: '本月借阅', value: statistics.thisMonthBorrows, bg: 'linear-gradient(135deg, var(--blue-light), var(--blue))' },
  { icon: '📖', label: '累计阅读', value: `${statistics.totalBooks}本`, bg: 'linear-gradient(135deg, var(--green-light), var(--green))' },
  { icon: '📅', label: '阅读天数', value: `${statistics.readingDays}天`, bg: 'linear-gradient(135deg, var(--yellow), var(--yellow-warm))' },
  { icon: '🏆', label: '总积分', value: statistics.totalPoints, bg: 'linear-gradient(135deg, var(--pink-light), var(--pink))' }
])

const colors = ['#957DAD', '#FFB3BA', '#B5EAD7', '#C7CEEA', '#FFEAA7', '#FFDAC1']

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

const fetchStatistics = async () => {
  try {
    const res = await getMyStatistics()
    const data = res.data || {}
    statistics.totalBorrows = data.totalBorrows || 0
    statistics.thisMonthBorrows = data.thisMonthBorrows || 0
    statistics.totalBooks = data.totalBooks || 0
    statistics.readingDays = data.readingDays || 0
    statistics.totalPoints = data.totalPoints || 0
    statistics.level = data.level || ''
    const cats = data.categoryDistribution || []
    const maxCount = Math.max(...cats.map(c => c.count), 1)
    statistics.categoryDistribution = cats.map((c, i) => ({
      ...c,
      percentage: (c.count / maxCount) * 100,
      color: colors[i % colors.length]
    }))
  } catch (e) {
    statistics.totalPoints = profile.borrowCount * 10
    statistics.totalBorrows = profile.borrowCount
    statistics.thisMonthBorrows = 0
    statistics.totalBooks = profile.borrowCount
    statistics.readingDays = Math.min(profile.borrowCount * 3, 30)
    statistics.categoryDistribution = [
      { name: '绘本', count: 3, percentage: 100, color: colors[0] },
      { name: '科普', count: 2, percentage: 67, color: colors[1] },
      { name: '故事', count: 1, percentage: 33, color: colors[2] }
    ]
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

const adminApp = reactive({
  loading: false,
  hasApplication: false,
  status: '',
  reason: '',
  rejectReason: '',
  createTime: '',
  showForm: false,
  submitting: false
})

const fetchAdminAppStatus = async () => {
  adminApp.loading = true
  try {
    const res = await getMyApplicationStatus()
    const data = res.data
    adminApp.hasApplication = data.hasApplication
    adminApp.status = data.status || ''
    adminApp.rejectReason = data.rejectReason || ''
    adminApp.createTime = data.createTime || ''
  } catch (e) {
    console.error('获取申请状态失败:', e)
  } finally {
    adminApp.loading = false
  }
}

const submitAdminApplication = async () => {
  if (!adminApp.reason || adminApp.reason.trim().length < 5) {
    ElMessage.warning('请填写至少5个字的申请理由')
    return
  }
  adminApp.submitting = true
  try {
    await applyForAdmin({ reason: adminApp.reason })
    ElMessage.success('申请已提交，请等待管理员审批')
    adminApp.showForm = false
    adminApp.hasApplication = true
    adminApp.status = 'pending'
  } catch (e) {
    console.error('提交申请失败:', e)
  } finally {
    adminApp.submitting = false
  }
}

onMounted(() => {
  fetchProfile()
  fetchStatistics()
  fetchAdminAppStatus()
})
</script>

<style scoped>
.profile-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
  width: 100%;
  max-width: 100%;
}

.profile-header {
  background: white;
  border-radius: var(--radius-lg);
  padding: 28px;
  box-shadow: var(--shadow-soft);
  display: flex;
  justify-content: space-between;
  align-items: center;
  position: relative;
  overflow: hidden;
}

.profile-header::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, var(--pink), var(--purple), var(--blue), var(--green));
}

.avatar-section {
  display: flex;
  align-items: center;
  gap: 20px;
}

.avatar-circle {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 40px;
  position: relative;
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

.avatar-ring {
  position: absolute;
  inset: -4px;
  border-radius: 50%;
  border: 2px dashed var(--purple-light);
  animation: spin 12s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.user-basic h2 {
  font-size: 22px;
  color: var(--text-primary);
  margin: 0 0 6px;
}

.join-info {
  font-size: 12px;
  color: var(--text-secondary);
  margin: 6px 0 0;
}

.profile-stats {
  display: flex;
  gap: 36px;
}

.pstat {
  text-align: center;
}

.pstat-value {
  display: block;
  font-size: 26px;
  font-weight: 700;
}

.pstat-label {
  font-size: 12px;
  color: var(--text-secondary);
  margin-top: 4px;
}

/* Points Section */
.points-section,
.stats-section,
.profile-form-section {
  background: white;
  border-radius: var(--radius-lg);
  padding: 24px 28px;
  box-shadow: var(--shadow-soft);
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 14px;
  border-bottom: 1px solid #f5f5f5;
}

.section-header h3 {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
  display: flex;
  align-items: center;
  gap: 6px;
}

.section-icon {
  font-size: 18px;
}

.points-content {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.points-display {
  display: flex;
  align-items: center;
  gap: 32px;
}

.points-number {
  display: flex;
  align-items: baseline;
  gap: 4px;
}

.points-value {
  font-size: 42px;
  font-weight: 800;
  background: linear-gradient(135deg, var(--purple), var(--pink));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.points-unit {
  font-size: 14px;
  color: var(--text-secondary);
}

.points-level {
  flex: 1;
}

.level-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  border-radius: 20px;
  margin-bottom: 10px;
}

.level-icon {
  font-size: 16px;
}

.level-text {
  font-size: 13px;
  font-weight: 600;
  color: white;
}

.level-progress {
  max-width: 240px;
}

.level-hint {
  font-size: 11px;
  color: var(--text-secondary);
  margin-top: 4px;
  display: block;
}

.achievements-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 12px;
}

.badge-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 14px 8px;
  border-radius: var(--radius-sm);
  background: #f9f9f9;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  opacity: 0.4;
  filter: grayscale(1);
  cursor: default;
}

.badge-item.achieved {
  opacity: 1;
  filter: none;
  background: linear-gradient(135deg, #fff9f0, #fff5f5);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  animation: fadeInScale 0.5s ease backwards;
}

.badge-item.achieved:nth-child(1) { animation-delay: 0.1s; }
.badge-item.achieved:nth-child(2) { animation-delay: 0.2s; }
.badge-item.achieved:nth-child(3) { animation-delay: 0.3s; }
.badge-item.achieved:nth-child(4) { animation-delay: 0.4s; }
.badge-item.achieved:nth-child(5) { animation-delay: 0.5s; }
.badge-item.achieved:nth-child(6) { animation-delay: 0.6s; }

.badge-item.achieved:hover {
  transform: translateY(-4px) scale(1.1);
  box-shadow: 0 6px 16px rgba(149, 125, 173, 0.15);
}

.badge-item.achieved:hover .badge-icon {
  animation: pulse-soft 0.6s ease;
}

.badge-icon {
  font-size: 24px;
}

.badge-name {
  font-size: 11px;
  color: var(--text-secondary);
  text-align: center;
}

/* Reading Stats */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  background: #fafafa;
  border-radius: var(--radius-sm);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
}

.stat-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(149, 125, 173, 0.03), transparent);
  transition: left 0.5s ease;
}

.stat-card:hover::before {
  left: 100%;
}

.stat-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 6px 20px rgba(149, 125, 173, 0.1);
  background: white;
}

.stat-card:hover .stat-card-icon {
  transform: scale(1.1) rotate(5deg);
}

.stat-card-icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  flex-shrink: 0;
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.stat-card-value {
  font-size: 20px;
  font-weight: 700;
  color: var(--text-primary);
  display: block;
}

.stat-card-label {
  font-size: 12px;
  color: var(--text-secondary);
}

.category-chart h4 {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 14px;
}

.category-bars {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.category-bar-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.category-bar-label {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  color: var(--text-primary);
}

.category-bar-count {
  color: var(--text-secondary);
  font-size: 12px;
}

.category-bar-bg {
  height: 8px;
  background: #f0f0f0;
  border-radius: 4px;
  overflow: hidden;
}

.category-bar-fill {
  height: 100%;
  border-radius: 4px;
  transition: width 1s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
}

.category-bar-fill::after {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(90deg, transparent, rgba(255,255,255,0.3), transparent);
  animation: shimmer 2s infinite;
}

@keyframes shimmer {
  0% { transform: translateX(-100%); }
  100% { transform: translateX(100%); }
}

@keyframes fadeInScale {
  from { opacity: 0; transform: scale(0.8); }
  to { opacity: 1; transform: scale(1); }
}

@keyframes pulse-soft {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.2); }
}

/* Form */
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

/* Admin Application */
.admin-apply-section {
  background: white;
  border-radius: var(--radius-lg);
  padding: 24px 28px;
  box-shadow: var(--shadow-soft);
}

.apply-content {
  padding-top: 4px;
}

.apply-loading {
  color: var(--text-secondary);
  font-size: 14px;
  padding: 12px 0;
}

.apply-status {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 16px;
  border-radius: var(--radius-sm);
}

.apply-status.pending {
  background: #FFF8E1;
}

.apply-status.approved {
  background: #E8F5E9;
}

.apply-status.rejected {
  background: #FFEBEE;
}

.status-icon {
  font-size: 32px;
  flex-shrink: 0;
}

.status-info h4 {
  margin: 0 0 4px;
  font-size: 15px;
  color: var(--text-primary);
}

.status-info p {
  margin: 0;
  font-size: 13px;
  color: var(--text-secondary);
}

.apply-time {
  margin-top: 4px;
  font-size: 12px !important;
}

.apply-desc {
  font-size: 14px;
  color: var(--text-secondary);
  margin: 0 0 16px;
}

.apply-form {
  margin-top: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.apply-form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.apply-form-area {
  padding: 4px 0;
}

/* Transitions */
.slide-fade-enter-active {
  transition: all 0.3s ease;
}
.slide-fade-leave-active {
  transition: all 0.2s ease;
}
.slide-fade-enter-from,
.slide-fade-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

.count-enter-active,
.count-leave-active {
  transition: all 0.3s;
}
.count-enter-from {
  opacity: 0;
  transform: translateY(-10px);
}
.count-leave-to {
  opacity: 0;
  transform: translateY(10px);
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
  .points-display {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }
  .achievements-grid {
    grid-template-columns: repeat(3, 1fr);
  }
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
