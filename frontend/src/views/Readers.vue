<template>
  <div class="readers-page">
    <!-- 统计卡片区域 -->
    <div class="stat-row animate__animated animate__fadeInDown">
      <div class="mini-stat" style="--accent: var(--pink)">
        <span class="mini-stat-icon">👧</span>
        <div class="mini-stat-info">
          <span class="mini-stat-value">{{ readerStats.total }}</span>
          <span class="mini-stat-label">读者总数</span>
        </div>
      </div>
      <div class="mini-stat" style="--accent: var(--green)">
        <span class="mini-stat-icon">✅</span>
        <div class="mini-stat-info">
          <span class="mini-stat-value">{{ readerStats.active }}</span>
          <span class="mini-stat-label">正常借阅</span>
        </div>
      </div>
      <div class="mini-stat" style="--accent: var(--yellow)">
        <span class="mini-stat-icon">⚠️</span>
        <div class="mini-stat-info">
          <span class="mini-stat-value">{{ readerStats.suspended }}</span>
          <span class="mini-stat-label">暂停借阅</span>
        </div>
      </div>
      <div class="mini-stat" style="--accent: var(--blue)">
        <span class="mini-stat-icon">📖</span>
        <div class="mini-stat-info">
          <span class="mini-stat-value">{{ readerStats.overdue }}</span>
          <span class="mini-stat-label">逾期未还</span>
        </div>
      </div>
    </div>

    <!-- 搜索与操作栏 -->
    <el-card class="search-card animate__animated animate__fadeInDown" style="animation-delay: 0.1s">
      <div class="search-bar">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索儿童姓名或家长联系方式..."
          prefix-icon="Search"
          size="large"
          clearable
          class="search-input"
          @input="handleSearchDebounced"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        />
        <el-select
          v-model="searchStatus"
          placeholder="全部状态"
          size="large"
          clearable
          class="status-select"
          @change="handleSearch"
        >
          <el-option label="正常" value="normal" />
          <el-option label="暂停借阅" value="suspended" />
        </el-select>
        <el-select
          v-model="searchGender"
          placeholder="全部性别"
          size="large"
          clearable
          class="gender-select"
          @change="handleSearch"
        >
          <el-option label="男" value="male" />
          <el-option label="女" value="female" />
        </el-select>
        <el-button type="primary" size="large" @click="handleSearch">
          <el-icon><Search /></el-icon>
          搜索
        </el-button>
        <el-button v-permission="'READER_CREATE'" type="success" size="large" @click="handleAdd">
          <el-icon><Plus /></el-icon>
          添加读者
        </el-button>
      </div>
    </el-card>

    <!-- 读者卡片网格 -->
    <div class="reader-grid" v-loading="loading" element-loading-text="加载读者数据中...">
      <transition-group name="card-list" tag="div" class="grid-inner">
        <div
          v-for="(reader, index) in readerList"
          :key="reader.id"
          class="reader-card"
          :style="{ animationDelay: `${index * 0.05}s` }"
        >
          <div class="card-color-bar" :style="{ background: reader.status === 'normal' ? '#B5EAD7' : '#FFB3BA' }"></div>
          <div class="card-header">
            <div class="avatar-wrapper" :class="reader.gender">
              <span class="avatar-icon">{{ reader.gender === 'male' ? '👦' : '👧' }}</span>
            </div>
            <div class="card-badges">
              <el-tag
                :type="reader.status === 'normal' ? 'success' : 'danger'"
                size="small"
                effect="light"
                class="status-tag"
              >
                <span class="status-dot" :class="reader.status"></span>
                {{ reader.status === 'normal' ? '正常' : '暂停借阅' }}
              </el-tag>
            </div>
          </div>
          <div class="card-body">
            <h3 class="card-name">{{ reader.name }}</h3>
            <div class="card-info-grid">
              <div class="info-item">
                <el-icon class="info-icon"><Calendar /></el-icon>
                <span>{{ reader.age }}岁</span>
              </div>
              <div class="info-item">
                <el-icon class="info-icon"><User /></el-icon>
                <span>{{ reader.gender === 'male' ? '男孩' : '女孩' }}</span>
              </div>
              <div class="info-item">
                <el-icon class="info-icon"><Phone /></el-icon>
                <span>{{ reader.parentPhone }}</span>
              </div>
              <div class="info-item">
                <el-icon class="info-icon"><Reading /></el-icon>
                <span>借阅 {{ reader.borrowCount || 0 }} 本</span>
              </div>
            </div>
            <div class="overdue-warning" v-if="reader.overdueCount > 0">
              <el-icon><WarningFilled /></el-icon>
              <span>逾期 {{ reader.overdueCount }} 次</span>
              <span v-if="reader.overdueCount >= 3" class="limit-text">（已限制借阅）</span>
            </div>
          </div>
          <div class="card-footer">
            <el-button class="card-btn records" size="small" @click="handleViewRecords(reader)">
              <el-icon><Document /></el-icon> 借阅记录
            </el-button>
            <el-button class="card-btn reading-stats" size="small" @click="handleViewReadingStats(reader)">
              <el-icon><TrendCharts /></el-icon> 阅读进度
            </el-button>
            <el-button v-permission="'READER_UPDATE'" class="card-btn edit" size="small" @click="handleEdit(reader)">
              <el-icon><Edit /></el-icon> 编辑
            </el-button>
            <el-button v-permission="'READER_UPDATE'" class="card-btn status-btn" size="small" @click="handleToggleStatus(reader)">
              <el-icon><Switch /></el-icon> {{ reader.status === 'normal' ? '暂停' : '恢复' }}
            </el-button>
            <el-button v-permission="'READER_DELETE'" class="card-btn delete" size="small" @click="handleDelete(reader)">
              <el-icon><Delete /></el-icon>
            </el-button>
          </div>
        </div>
      </transition-group>

      <el-empty v-if="!loading && readerList.length === 0" description="暂无读者数据，点击上方按钮添加第一位小读者吧~">
        <el-button v-permission="'READER_CREATE'" type="primary" @click="handleAdd">添加读者</el-button>
      </el-empty>
    </div>

    <!-- 分页 -->
    <div class="pagination-wrapper" v-if="total > pageSize">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[12, 24, 48]"
        layout="total, sizes, prev, pager, next"
        background
        @current-change="fetchReaders"
        @size-change="fetchReaders"
      />
    </div>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑读者信息' : '添加新读者'"
      width="520px"
      :close-on-click-modal="false"
      destroy-on-close
      class="reader-dialog"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="110px"
        v-loading="submitLoading"
        element-loading-text="正在保存..."
      >
        <el-form-item label="儿童姓名" prop="name">
          <el-input v-model="form.name" placeholder="请输入儿童姓名" maxlength="20" show-word-limit>
            <template #prefix>
              <el-icon><User /></el-icon>
            </template>
          </el-input>
        </el-form-item>

        <div class="form-row-inline">
          <el-form-item label="年龄" prop="age">
            <el-input-number v-model="form.age" :min="1" :max="18" />
            <span class="form-unit">岁</span>
          </el-form-item>

          <el-form-item label="性别" prop="gender">
            <el-radio-group v-model="form.gender" class="gender-radio">
              <el-radio value="male">
                <span class="gender-label">👦 男孩</span>
              </el-radio>
              <el-radio value="female">
                <span class="gender-label">👧 女孩</span>
              </el-radio>
            </el-radio-group>
          </el-form-item>
        </div>

        <el-form-item label="家长联系方式" prop="parentPhone">
          <el-input v-model="form.parentPhone" placeholder="请输入家长手机号" maxlength="11">
            <template #prefix>
              <el-icon><Phone /></el-icon>
            </template>
          </el-input>
        </el-form-item>

        <el-form-item label="家长姓名" prop="parentName">
          <el-input v-model="form.parentName" placeholder="请输入家长姓名" maxlength="20">
            <template #prefix>
              <el-icon><Avatar /></el-icon>
            </template>
          </el-input>
        </el-form-item>

        <el-form-item label="备注" prop="remark">
          <el-input
            v-model="form.remark"
            type="textarea"
            :rows="2"
            placeholder="可选：过敏信息、特别偏好等"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">
          {{ submitLoading ? '保存中...' : '确认保存' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 借阅记录弹窗 -->
    <el-dialog
      v-model="recordsDialogVisible"
      :title="`${currentReader.name} 的借阅记录`"
      width="700px"
      destroy-on-close
      class="records-dialog"
    >
      <div class="records-header">
        <div class="reader-summary">
          <span class="summary-avatar">{{ currentReader.gender === 'male' ? '👦' : '👧' }}</span>
          <div class="summary-info">
            <h4>{{ currentReader.name }}</h4>
            <span class="summary-meta">{{ currentReader.age }}岁 · 累计借阅 {{ currentReader.borrowCount || 0 }} 本</span>
          </div>
        </div>
        <div class="records-filter">
          <el-radio-group v-model="recordsFilter" size="small" @change="fetchBorrowRecords">
            <el-radio-button value="all">全部</el-radio-button>
            <el-radio-button value="borrowing">借阅中</el-radio-button>
            <el-radio-button value="overdue">逾期未还</el-radio-button>
            <el-radio-button value="returned">已归还</el-radio-button>
          </el-radio-group>
        </div>
      </div>

      <el-table
        :data="borrowRecords"
        v-loading="recordsLoading"
        stripe
        class="records-table"
        empty-text="暂无借阅记录"
      >
        <el-table-column label="图书名称" prop="bookTitle" min-width="150">
          <template #default="{ row }">
            <div class="book-cell">
              <span class="book-icon">📕</span>
              <span>{{ row.bookTitle }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="借阅日期" prop="borrowDate" width="120" />
        <el-table-column label="应还日期" prop="dueDate" width="120" />
        <el-table-column label="归还日期" prop="returnDate" width="120">
          <template #default="{ row }">
            <span v-if="row.returnDate">{{ row.returnDate }}</span>
            <span v-else class="not-returned">未归还</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag
              :type="row.status === 'returned' ? 'success' : row.status === 'overdue' ? 'danger' : 'warning'"
              size="small"
              effect="light"
            >
              {{ row.status === 'returned' ? '已归还' : row.status === 'overdue' ? '逾期' : '借阅中' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>

      <div class="records-pagination" v-if="recordsTotal > 10">
        <el-pagination
          v-model:current-page="recordsPage"
          :total="recordsTotal"
          :page-size="10"
          layout="prev, pager, next"
          small
          background
          @current-change="fetchBorrowRecords"
        />
      </div>
    </el-dialog>

    <!-- 阅读进度统计弹窗 -->
    <el-dialog
      v-model="readingStatsDialogVisible"
      width="680px"
      destroy-on-close
      class="reading-stats-dialog"
    >
      <template #header>
        <div class="rs-dialog-header">
          <span class="rs-dialog-icon">📊</span>
          <div class="rs-dialog-title-group">
            <h3 class="rs-dialog-title">{{ currentReader.name }} 的阅读进度</h3>
            <span class="rs-dialog-subtitle">阅读数据概览与进度追踪</span>
          </div>
        </div>
      </template>
      <div v-loading="readingStatsLoading">
        <div class="reading-stats-summary">
          <div class="rs-stat-item" style="--card-accent: var(--blue-light)">
            <span class="rs-stat-icon">⏱️</span>
            <div class="rs-stat-detail">
              <span class="rs-stat-value">{{ readingStats.totalReadingHours || 0 }}</span>
              <span class="rs-stat-label">阅读时长(小时)</span>
            </div>
          </div>
          <div class="rs-stat-item" style="--card-accent: var(--green-light)">
            <span class="rs-stat-icon">📖</span>
            <div class="rs-stat-detail">
              <span class="rs-stat-value">{{ readingStats.completedBooks || 0 }}</span>
              <span class="rs-stat-label">读完图书</span>
            </div>
          </div>
          <div class="rs-stat-item" style="--card-accent: var(--purple-light)">
            <span class="rs-stat-icon">📊</span>
            <div class="rs-stat-detail">
              <span class="rs-stat-value">{{ readingStats.completionRate || 0 }}%</span>
              <span class="rs-stat-label">完成率</span>
            </div>
          </div>
          <div class="rs-stat-item" style="--card-accent: var(--pink-light)">
            <span class="rs-stat-icon">📝</span>
            <div class="rs-stat-detail">
              <span class="rs-stat-value">{{ readingStats.noteCount || 0 }}</span>
              <span class="rs-stat-label">阅读笔记</span>
            </div>
          </div>
          <div class="rs-stat-item" style="--card-accent: var(--yellow)">
            <span class="rs-stat-icon">📅</span>
            <div class="rs-stat-detail">
              <span class="rs-stat-value">{{ readingStats.totalReadingDays || 0 }}</span>
              <span class="rs-stat-label">阅读天数</span>
            </div>
          </div>
        </div>

        <div class="reading-progress-list" v-if="readerProgressList.length > 0">
          <h4 class="rp-list-title">
            <span class="rp-title-icon">📚</span> 阅读进度列表
          </h4>
          <div class="rp-item" v-for="item in readerProgressList" :key="item.id">
            <div class="rp-item-info">
              <div class="rp-book-info">
                <span class="rp-book-emoji">📘</span>
                <span class="rp-book-title">{{ item.bookTitle || '未知图书' }}</span>
              </div>
              <el-tag size="small" round :type="item.status === 'completed' ? 'success' : item.status === 'paused' ? 'warning' : 'primary'">
                {{ item.status === 'completed' ? '已完成' : item.status === 'paused' ? '已暂停' : '阅读中' }}
              </el-tag>
            </div>
            <div class="rp-item-bar">
              <div class="rp-bar-bg">
                <div class="rp-bar-fill" :style="{ width: item.progressPercent + '%' }"></div>
              </div>
              <span class="rp-percent">{{ item.progressPercent }}%</span>
            </div>
            <div class="rp-item-meta">
              <span>{{ item.currentPage || 0 }}/{{ item.totalPages || '?' }}页</span>
              <span>{{ formatReadingTime(item.readingMinutes) }}</span>
            </div>
          </div>
        </div>
        <el-empty v-else-if="!readingStatsLoading" description="该读者暂无阅读进度记录" />
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getReaders, addReader, updateReader, deleteReader, getReaderBorrowRecords, updateReaderStatus, getReaderReadingStatistics, getReaderReadingProgress } from '@/api'

const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)

const searchKeyword = ref('')
const searchStatus = ref(null)
const searchGender = ref(null)
const page = ref(1)
const pageSize = ref(12)
const total = ref(0)
const readerList = ref([])

let searchTimer = null

// 借阅记录相关
const recordsDialogVisible = ref(false)
const recordsLoading = ref(false)
const borrowRecords = ref([])
const recordsFilter = ref('all')
const recordsPage = ref(1)
const recordsTotal = ref(0)
const currentReader = ref({})

// 阅读进度相关
const readingStatsDialogVisible = ref(false)
const readingStatsLoading = ref(false)
const readingStats = ref({})
const readerProgressList = ref([])

const form = reactive({
  id: null,
  name: '',
  age: 6,
  gender: 'male',
  parentPhone: '',
  parentName: '',
  remark: ''
})

const validatePhone = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请输入家长联系方式'))
  } else if (!/^1[3-9]\d{9}$/.test(value)) {
    callback(new Error('请输入正确的手机号码'))
  } else {
    callback()
  }
}

const rules = {
  name: [{ required: true, message: '请输入儿童姓名', trigger: 'blur' }],
  age: [{ required: true, message: '请输入年龄', trigger: 'change' }],
  gender: [{ required: true, message: '请选择性别', trigger: 'change' }],
  parentPhone: [{ required: true, validator: validatePhone, trigger: 'blur' }]
}

const readerStats = computed(() => {
  return {
    total: total.value,
    active: readerList.value.filter(r => r.status === 'normal').length,
    suspended: readerList.value.filter(r => r.status === 'suspended').length,
    overdue: readerList.value.filter(r => r.overdueCount > 0).length
  }
})

const fetchReaders = async () => {
  loading.value = true
  try {
    const res = await getReaders({
      page: page.value,
      size: pageSize.value,
      keyword: searchKeyword.value,
      status: searchStatus.value,
      gender: searchGender.value
    })
    readerList.value = res.data.records
    total.value = res.data.total
  } catch (e) {
    // handled by interceptor
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  page.value = 1
  fetchReaders()
}

const handleSearchDebounced = () => {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    handleSearch()
  }, 300)
}

const resetForm = () => {
  Object.assign(form, {
    id: null, name: '', age: 6, gender: 'male',
    parentPhone: '', parentName: '', remark: ''
  })
}

const handleAdd = () => {
  isEdit.value = false
  resetForm()
  dialogVisible.value = true
}

const handleEdit = (reader) => {
  isEdit.value = true
  Object.assign(form, { ...reader })
  dialogVisible.value = true
}

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    if (isEdit.value) {
      await updateReader(form.id, form)
      ElMessage.success('读者信息更新成功！')
    } else {
      await addReader(form)
      ElMessage.success('读者添加成功！')
    }
    dialogVisible.value = false
    fetchReaders()
  } catch (e) {
    // handled by interceptor
  } finally {
    submitLoading.value = false
  }
}

const handleToggleStatus = (reader) => {
  const newStatus = reader.status === 'normal' ? 'suspended' : 'normal'
  const actionText = newStatus === 'suspended' ? '暂停借阅' : '恢复借阅'

  ElMessageBox.confirm(
    `确定要将 "${reader.name}" 的状态变更为${actionText}吗？`,
    '状态变更确认',
    {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      await updateReaderStatus(reader.id, newStatus)
      reader.status = newStatus
      ElMessage.success(`已${actionText}`)
    } catch (e) {
      // handled by interceptor
    }
  }).catch(() => {})
}

const handleDelete = (reader) => {
  ElMessageBox.confirm(
    `确定要删除读者"${reader.name}"的信息吗？此操作不可恢复。`,
    '删除确认',
    {
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
      type: 'warning',
      confirmButtonClass: 'el-button--danger'
    }
  ).then(async () => {
    try {
      await deleteReader(reader.id)
      readerList.value = readerList.value.filter(r => r.id !== reader.id)
      total.value--
      ElMessage.success('删除成功！')
    } catch (e) {
      // handled by interceptor
    }
  }).catch(() => {})
}

const handleViewRecords = (reader) => {
  currentReader.value = reader
  recordsFilter.value = 'all'
  recordsPage.value = 1
  recordsDialogVisible.value = true
  fetchBorrowRecords()
}

const fetchBorrowRecords = async () => {
  recordsLoading.value = true
  try {
    const res = await getReaderBorrowRecords(currentReader.value.id, {
      page: recordsPage.value,
      size: 10,
      status: recordsFilter.value === 'all' ? null : recordsFilter.value
    })
    borrowRecords.value = res.data.records
    recordsTotal.value = res.data.total
  } catch (e) {
    // handled by interceptor
  } finally {
    recordsLoading.value = false
  }
}

const handleViewReadingStats = async (reader) => {
  currentReader.value = reader
  readingStatsDialogVisible.value = true
  readingStatsLoading.value = true
  try {
    const [statsRes, progressRes] = await Promise.all([
      getReaderReadingStatistics(reader.id),
      getReaderReadingProgress(reader.id, { page: 1, size: 20 })
    ])
    readingStats.value = statsRes.data || {}
    readerProgressList.value = progressRes.data?.records || []
  } catch (e) {
    readingStats.value = {}
    readerProgressList.value = []
  } finally {
    readingStatsLoading.value = false
  }
}

const formatReadingTime = (minutes) => {
  if (!minutes) return '0分钟'
  if (minutes < 60) return `${minutes}分钟`
  const h = Math.floor(minutes / 60)
  const m = minutes % 60
  return m > 0 ? `${h}h${m}m` : `${h}小时`
}

onMounted(() => {
  fetchReaders()
})
</script>

<style scoped>
.readers-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* 统计卡片 */
.stat-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.mini-stat {
  background: white;
  border-radius: var(--radius-md);
  padding: 18px 20px;
  display: flex;
  align-items: center;
  gap: 14px;
  box-shadow: var(--shadow-soft);
  transition: all 0.3s ease;
  border-left: 4px solid var(--accent);
}

.mini-stat:hover {
  transform: translateY(-3px);
  box-shadow: var(--shadow-hover);
}

.mini-stat-icon {
  font-size: 28px;
  animation: float 3s ease-in-out infinite;
}

.mini-stat-info {
  display: flex;
  flex-direction: column;
}

.mini-stat-value {
  font-size: 22px;
  font-weight: 700;
  color: var(--text-primary);
}

.mini-stat-label {
  font-size: 12px;
  color: var(--text-secondary);
  margin-top: 2px;
}

/* 搜索栏 */
.search-card {
  background: white;
}

.search-bar {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}

.search-input {
  flex: 1;
  min-width: 200px;
}

.status-select {
  width: 140px;
}

.gender-select {
  width: 120px;
}

/* 读者卡片网格 */
.reader-grid {
  min-height: 200px;
}

.grid-inner {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
}

.reader-card {
  background: white;
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-soft);
  overflow: hidden;
  position: relative;
  transition: all 0.35s cubic-bezier(0.4, 0, 0.2, 1);
  animation: cardFadeIn 0.4s ease forwards;
  opacity: 0;
}

.reader-card:hover {
  transform: translateY(-6px) scale(1.01);
  box-shadow: 0 16px 40px rgba(0, 0, 0, 0.12);
}

.reader-card:hover .card-color-bar {
  height: 5px;
}

.reader-card:hover .avatar-wrapper {
  transform: scale(1.1) rotate(5deg);
}

@keyframes cardFadeIn {
  from {
    opacity: 0;
    transform: translateY(24px) scale(0.96);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-4px); }
}

.card-color-bar {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  transition: height 0.3s ease;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 20px 20px 12px;
}

.avatar-wrapper {
  width: 52px;
  height: 52px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.avatar-wrapper.male {
  background: linear-gradient(135deg, #C7CEEA, #E0E5F5);
}

.avatar-wrapper.female {
  background: linear-gradient(135deg, #FFB3BA, #FFD1D6);
}

.avatar-icon {
  font-size: 28px;
}

.status-tag {
  display: flex;
  align-items: center;
  gap: 4px;
}

.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  display: inline-block;
}

.status-dot.normal {
  background: #67c23a;
  box-shadow: 0 0 6px rgba(103, 194, 58, 0.5);
  animation: pulse-dot 2s ease-in-out infinite;
}

.status-dot.suspended {
  background: #f56c6c;
}

@keyframes pulse-dot {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.card-body {
  padding: 0 20px 16px;
}

.card-name {
  font-size: 17px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 12px;
}

.card-info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--text-secondary);
}

.info-icon {
  color: var(--purple-light);
  font-size: 14px;
}

.overdue-warning {
  margin-top: 12px;
  padding: 8px 12px;
  background: linear-gradient(135deg, #FFF3F3, #FFE8E8);
  border-radius: var(--radius-sm);
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #E74C3C;
  animation: shake 0.5s ease;
}

.limit-text {
  font-weight: 600;
  color: #C0392B;
}

@keyframes shake {
  0%, 100% { transform: translateX(0); }
  25% { transform: translateX(-2px); }
  75% { transform: translateX(2px); }
}

.card-footer {
  padding: 12px 20px;
  border-top: 1px solid #F5F5F5;
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.card-btn.records {
  background: linear-gradient(135deg, var(--blue), #A8B4E0) !important;
  border: none !important;
  color: #fff !important;
  font-size: 12px;
}

.card-btn.records:hover {
  background: linear-gradient(135deg, #A8B4E0, var(--blue)) !important;
  box-shadow: 0 3px 8px rgba(199, 206, 234, 0.5);
}

.card-btn.edit {
  background: linear-gradient(135deg, var(--btn-edit-from), var(--btn-edit-to)) !important;
  border: none !important;
  color: #fff !important;
  font-size: 12px;
}

.card-btn.edit:hover {
  background: linear-gradient(135deg, var(--btn-edit-to), var(--btn-edit-from)) !important;
  box-shadow: 0 3px 8px rgba(167, 139, 250, 0.3);
}

.card-btn.status-btn {
  background: linear-gradient(135deg, var(--yellow-warm), #FFD166) !important;
  border: none !important;
  color: #4A4A4A !important;
  font-size: 12px;
}

.card-btn.status-btn:hover {
  background: linear-gradient(135deg, #FFD166, var(--yellow-warm)) !important;
  box-shadow: 0 3px 8px rgba(255, 234, 167, 0.5);
}

.card-btn.delete {
  background: linear-gradient(135deg, var(--btn-delete-from), var(--btn-delete-to)) !important;
  border: none !important;
  color: #fff !important;
  font-size: 12px;
}

.card-btn.delete:hover {
  background: linear-gradient(135deg, var(--btn-delete-to), var(--btn-delete-from)) !important;
  box-shadow: 0 3px 8px rgba(255, 179, 186, 0.4);
}

/* 弹窗样式 */
.reader-dialog :deep(.el-dialog__header) {
  background: linear-gradient(135deg, var(--pink-light), var(--blue-light));
  margin-right: 0;
  padding: 20px 24px;
}

.reader-dialog :deep(.el-dialog__title) {
  font-weight: 600;
  font-size: 18px;
}

.form-row-inline {
  display: flex;
  gap: 20px;
}

.form-row-inline .el-form-item {
  flex: 1;
}

.form-unit {
  margin-left: 8px;
  color: var(--text-secondary);
  font-size: 14px;
}

.gender-radio :deep(.el-radio__label) {
  font-size: 14px;
}

.gender-label {
  font-size: 14px;
}

/* 借阅记录弹窗 */
.records-dialog :deep(.el-dialog__header) {
  background: linear-gradient(135deg, var(--green-light), var(--blue-light));
  margin-right: 0;
  padding: 20px 24px;
}

.records-dialog :deep(.el-dialog__title) {
  font-weight: 600;
  font-size: 18px;
}

.records-filter :deep(.el-radio-button) {
  --el-radio-button-checked-bg-color: transparent;
  --el-radio-button-checked-border-color: transparent;
}

.records-filter :deep(.el-radio-button__inner) {
  border-radius: 20px !important;
  padding: 6px 16px;
  border: 2px solid #f0f0f0 !important;
  box-shadow: none !important;
  font-weight: 500;
  font-size: 12px;
  color: var(--text-secondary);
  background: #fafafa;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.records-filter :deep(.el-radio-button__inner:hover) {
  border-color: var(--purple-light) !important;
  color: var(--purple);
  background: linear-gradient(135deg, #f8f5ff, #fff5f8);
}

.records-filter :deep(.el-radio-button.is-active .el-radio-button__inner) {
  background: linear-gradient(135deg, var(--purple-light), var(--pink-light)) !important;
  border-color: transparent !important;
  color: #fff !important;
  font-weight: 600;
  box-shadow: 0 3px 10px rgba(149, 125, 173, 0.25) !important;
}

.records-filter :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
  background: linear-gradient(135deg, var(--purple-light), var(--pink-light)) !important;
  border-color: transparent !important;
  color: #fff !important;
  font-weight: 600;
  box-shadow: 0 3px 10px rgba(149, 125, 173, 0.25) !important;
}

.records-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #F5F5F5;
}

.reader-summary {
  display: flex;
  align-items: center;
  gap: 12px;
}

.summary-avatar {
  font-size: 32px;
}

.summary-info h4 {
  margin: 0;
  font-size: 16px;
  color: var(--text-primary);
}

.summary-meta {
  font-size: 13px;
  color: var(--text-secondary);
}

.book-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.book-icon {
  font-size: 16px;
}

.not-returned {
  color: var(--text-secondary);
  font-style: italic;
}

.records-pagination {
  display: flex;
  justify-content: center;
  margin-top: 16px;
}

/* 分页 */
.pagination-wrapper {
  display: flex;
  justify-content: center;
  padding-top: 8px;
}

/* 列表过渡动画 */
.card-list-enter-active {
  transition: all 0.4s ease;
}

.card-list-leave-active {
  transition: all 0.3s ease;
}

.card-list-enter-from {
  opacity: 0;
  transform: translateY(20px);
}

.card-list-leave-to {
  opacity: 0;
  transform: scale(0.9);
}

.card-list-move {
  transition: transform 0.4s ease;
}

@media (max-width: 1200px) {
  .stat-row {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .stat-row {
    grid-template-columns: 1fr;
  }
  .grid-inner {
    grid-template-columns: 1fr;
  }
  .form-row-inline {
    flex-direction: column;
    gap: 0;
  }
}

/* 阅读进度统计弹窗 */
.rs-dialog-header {
  display: flex;
  align-items: center;
  gap: 14px;
}

.rs-dialog-icon {
  font-size: 32px;
}

.rs-dialog-title-group {
  display: flex;
  flex-direction: column;
}

.rs-dialog-title {
  font-size: 18px;
  font-weight: 700;
  margin: 0;
  background: linear-gradient(135deg, var(--purple), #B48EE0);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.rs-dialog-subtitle {
  font-size: 12px;
  color: var(--text-secondary);
  margin-top: 2px;
}

.reading-stats-summary {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 12px;
  margin-bottom: 28px;
}

.rs-stat-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 16px 12px;
  background: var(--card-accent, #fafbff);
  border-radius: var(--radius-sm);
  transition: all 0.3s ease;
  border: 1px solid rgba(0, 0, 0, 0.03);
}

.rs-stat-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(149, 125, 173, 0.1);
}

.rs-stat-icon {
  font-size: 24px;
}

.rs-stat-detail {
  display: flex;
  flex-direction: column;
}

.rs-stat-value {
  font-size: 18px;
  font-weight: 700;
  color: var(--text-primary);
}

.rs-stat-label {
  font-size: 11px;
  color: var(--text-secondary);
  margin-top: 2px;
}

.reading-progress-list {
  background: linear-gradient(135deg, #fafbff, #fff9fc);
  border-radius: var(--radius-md);
  padding: 20px;
}

.rp-list-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f5;
  display: flex;
  align-items: center;
  gap: 8px;
}

.rp-title-icon {
  font-size: 16px;
}

.rp-item {
  padding: 14px 0;
  border-bottom: 1px solid #f5f5fa;
}

.rp-item:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.rp-item-info {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.rp-book-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.rp-book-emoji {
  font-size: 16px;
}

.rp-book-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}

.rp-item-bar {
  display: flex;
  align-items: center;
  gap: 10px;
}

.rp-bar-bg {
  flex: 1;
  height: 8px;
  border-radius: 4px;
  background: #f0f0f5;
  overflow: hidden;
}

.rp-bar-fill {
  height: 100%;
  border-radius: 4px;
  background: linear-gradient(90deg, var(--green), var(--blue), var(--purple));
  transition: width 0.6s cubic-bezier(0.4, 0, 0.2, 1);
}

.rp-percent {
  font-size: 13px;
  font-weight: 600;
  color: var(--purple);
  min-width: 40px;
  text-align: right;
}

.rp-item-meta {
  display: flex;
  gap: 16px;
  margin-top: 6px;
  font-size: 12px;
  color: var(--text-secondary);
}

.card-btn.reading-stats {
  background: linear-gradient(135deg, var(--blue-light), var(--purple-light)) !important;
  color: var(--purple) !important;
  border: none !important;
}
</style>
