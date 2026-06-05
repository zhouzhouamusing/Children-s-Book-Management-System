<template>
  <div class="progress-page">
    <!-- 统计卡片 -->
    <div class="stats-row animate__animated animate__fadeInDown">
      <div class="stat-card">
        <div class="stat-icon">⏱️</div>
        <div class="stat-info">
          <span class="stat-value">{{ stats.totalReadingHours || 0 }}</span>
          <span class="stat-label">阅读时长(小时)</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon">📖</div>
        <div class="stat-info">
          <span class="stat-value">{{ stats.completedBooks || 0 }}</span>
          <span class="stat-label">读完图书</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon">📊</div>
        <div class="stat-info">
          <span class="stat-value">{{ stats.completionRate || 0 }}%</span>
          <span class="stat-label">完成率</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon">📝</div>
        <div class="stat-info">
          <span class="stat-value">{{ stats.noteCount || 0 }}</span>
          <span class="stat-label">阅读笔记</span>
        </div>
      </div>
    </div>

    <!-- 操作栏 -->
    <div class="action-bar animate__animated animate__fadeInUp">
      <div class="tab-group">
        <el-radio-group v-model="activeTab" @change="handleTabChange">
          <el-radio-button value="progress">阅读进度</el-radio-button>
          <el-radio-button value="notes">阅读笔记</el-radio-button>
        </el-radio-group>
      </div>
      <div class="action-right">
        <el-select v-model="statusFilter" placeholder="全部状态" clearable size="default" @change="fetchData" v-if="activeTab === 'progress'">
          <el-option label="阅读中" value="reading" />
          <el-option label="已完成" value="completed" />
          <el-option label="已暂停" value="paused" />
        </el-select>
        <el-button v-permission="'READING_PROGRESS_CREATE'" type="primary" round @click="showAddDialog" class="add-btn">
          <el-icon><Plus /></el-icon>
          {{ activeTab === 'progress' ? '添加进度' : '添加笔记' }}
        </el-button>
      </div>
    </div>

    <!-- 阅读进度列表 -->
    <div v-if="activeTab === 'progress'" v-loading="loading" class="progress-list">
      <TransitionGroup name="card-list" tag="div" class="list-container">
        <div
          v-for="(item, index) in progressList"
          :key="item.id"
          class="progress-card animate__animated animate__fadeInUp"
          :style="{ animationDelay: `${index * 0.06}s` }"
        >
          <div class="progress-card-header">
            <div class="book-info-row">
              <span class="book-emoji">📘</span>
              <div class="book-detail">
                <h4>{{ item.bookTitle || '未知图书' }}</h4>
                <el-tag size="small" round :type="statusType(item.status)">
                  {{ statusLabel(item.status) }}
                </el-tag>
              </div>
            </div>
            <el-dropdown trigger="click" @command="(cmd) => handleCommand(cmd, item)">
              <el-button text class="more-btn">
                <el-icon><MoreFilled /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item v-permission="'READING_PROGRESS_UPDATE'" command="update">更新进度</el-dropdown-item>
                  <el-dropdown-item v-permission="'READING_PROGRESS_UPDATE'" command="complete" v-if="item.status !== 'completed'">标记完成</el-dropdown-item>
                  <el-dropdown-item v-permission="'READING_PROGRESS_UPDATE'" command="pause" v-if="item.status === 'reading'">暂停阅读</el-dropdown-item>
                  <el-dropdown-item v-permission="'READING_PROGRESS_UPDATE'" command="resume" v-if="item.status === 'paused'">继续阅读</el-dropdown-item>
                  <el-dropdown-item v-permission="'READING_PROGRESS_CREATE'" command="addNote">添加笔记</el-dropdown-item>
                  <el-dropdown-item v-permission="'READING_PROGRESS_DELETE'" command="delete" divided>删除</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
          <div class="progress-bar-section">
            <div class="progress-bar-bg">
              <div class="progress-bar-fill" :style="{ width: item.progressPercent + '%' }"></div>
            </div>
            <span class="progress-text">{{ item.progressPercent }}%</span>
          </div>
          <div class="progress-meta">
            <span><el-icon><Document /></el-icon> {{ item.currentPage || 0 }}/{{ item.totalPages || '?' }}页</span>
            <span><el-icon><Timer /></el-icon> {{ formatMinutes(item.readingMinutes) }}</span>
          </div>
        </div>
      </TransitionGroup>

      <div v-if="!loading && progressList.length === 0" class="empty-state">
        <div class="empty-icon">📚</div>
        <p>还没有阅读记录</p>
        <p class="empty-tip">点击"添加进度"开始追踪你的阅读吧！</p>
      </div>

      <div class="pagination-wrapper" v-if="progressTotal > pageSize">
        <el-pagination
          v-model:current-page="currentPage"
          :total="progressTotal"
          :page-size="pageSize"
          layout="prev, pager, next"
          @current-change="fetchProgressList"
        />
      </div>
    </div>

    <!-- 阅读笔记列表 -->
    <div v-if="activeTab === 'notes'" v-loading="loading" class="notes-list">
      <TransitionGroup name="card-list" tag="div" class="list-container">
        <div
          v-for="(note, index) in notesList"
          :key="note.id"
          class="note-card animate__animated animate__fadeInUp"
          :style="{ animationDelay: `${index * 0.06}s` }"
        >
          <div class="note-header">
            <div class="note-book">
              <span class="note-emoji">📝</span>
              <span class="note-book-title">{{ note.bookTitle || '未知图书' }}</span>
              <el-tag size="small" round type="info" v-if="note.pageNumber">第{{ note.pageNumber }}页</el-tag>
            </div>
            <div class="note-actions">
              <el-button v-permission="'READING_PROGRESS_UPDATE'" text size="small" @click="handleEditNote(note)">
                <el-icon><Edit /></el-icon>
              </el-button>
              <el-button v-permission="'READING_PROGRESS_DELETE'" text size="small" type="danger" @click="handleDeleteNote(note)">
                <el-icon><Delete /></el-icon>
              </el-button>
            </div>
          </div>
          <div class="note-content">{{ note.content }}</div>
          <div class="note-time">{{ formatTime(note.createTime) }}</div>
        </div>
      </TransitionGroup>

      <div v-if="!loading && notesList.length === 0" class="empty-state">
        <div class="empty-icon">📝</div>
        <p>还没有阅读笔记</p>
        <p class="empty-tip">在阅读中随时记录你的感想吧！</p>
      </div>

      <div class="pagination-wrapper" v-if="notesTotal > pageSize">
        <el-pagination
          v-model:current-page="notesPage"
          :total="notesTotal"
          :page-size="pageSize"
          layout="prev, pager, next"
          @current-change="fetchNotes"
        />
      </div>
    </div>

    <!-- 添加/更新进度对话框 -->
    <el-dialog v-model="progressDialogVisible" :title="isUpdate ? '更新阅读进度' : '添加阅读进度'" width="450px" class="custom-dialog">
      <el-form :model="progressForm" label-width="80px">
        <el-form-item label="选择图书" v-if="!isUpdate">
          <el-select
            v-model="progressForm.bookId"
            placeholder="搜索图书..."
            filterable
            remote
            :remote-method="searchBooks"
            :loading="bookSearchLoading"
            style="width: 100%"
          >
            <el-option
              v-for="book in bookOptions"
              :key="book.id"
              :label="book.title"
              :value="book.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="总页数">
          <el-input-number v-model="progressForm.totalPages" :min="1" :max="9999" style="width: 100%" />
        </el-form-item>
        <el-form-item label="当前页">
          <el-input-number v-model="progressForm.currentPage" :min="0" :max="progressForm.totalPages || 9999" style="width: 100%" />
        </el-form-item>
        <el-form-item label="阅读时长">
          <el-input-number v-model="progressForm.readingMinutes" :min="0" placeholder="分钟" style="width: 100%" />
          <span class="form-tip">分钟</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="progressDialogVisible = false" round>取消</el-button>
        <el-button type="primary" @click="submitProgress" round :loading="submitting">确定</el-button>
      </template>
    </el-dialog>

    <!-- 添加/编辑笔记对话框 -->
    <el-dialog v-model="noteDialogVisible" :title="editingNote ? '编辑笔记' : '添加阅读笔记'" width="500px" class="custom-dialog">
      <el-form :model="noteForm" label-width="80px">
        <el-form-item label="选择图书" v-if="!editingNote && !noteFromProgress">
          <el-select
            v-model="noteForm.bookId"
            placeholder="搜索图书..."
            filterable
            remote
            :remote-method="searchBooks"
            :loading="bookSearchLoading"
            style="width: 100%"
          >
            <el-option
              v-for="book in bookOptions"
              :key="book.id"
              :label="book.title"
              :value="book.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="页码">
          <el-input-number v-model="noteForm.pageNumber" :min="0" :max="9999" style="width: 100%" />
        </el-form-item>
        <el-form-item label="笔记内容">
          <el-input
            v-model="noteForm.content"
            type="textarea"
            :rows="5"
            placeholder="记录你的阅读感想..."
            maxlength="1000"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="noteDialogVisible = false" round>取消</el-button>
        <el-button type="primary" @click="submitNote" round :loading="submitting">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getReadingProgressList,
  createOrUpdateProgress,
  updateProgressStatus,
  deleteProgress,
  getReadingStatistics,
  getReadingNotes,
  addReadingNote,
  updateReadingNote,
  deleteReadingNote,
  browseBooks
} from '@/api'
import { usePermission } from '@/composables/usePermission'
const { checkWithFeedback } = usePermission()

const loading = ref(false)
const submitting = ref(false)
const activeTab = ref('progress')
const statusFilter = ref('')
const currentPage = ref(1)
const notesPage = ref(1)
const pageSize = ref(10)
const progressList = ref([])
const progressTotal = ref(0)
const notesList = ref([])
const notesTotal = ref(0)
const stats = ref({})

const progressDialogVisible = ref(false)
const noteDialogVisible = ref(false)
const isUpdate = ref(false)
const editingNote = ref(null)
const noteFromProgress = ref(null)
const bookSearchLoading = ref(false)
const bookOptions = ref([])

const progressForm = ref({
  bookId: null,
  totalPages: 100,
  currentPage: 0,
  readingMinutes: 0
})

const noteForm = ref({
  bookId: null,
  progressId: null,
  content: '',
  pageNumber: 0
})

const fetchData = () => {
  if (activeTab.value === 'progress') {
    fetchProgressList()
  } else {
    fetchNotes()
  }
}

const handleTabChange = () => {
  fetchData()
}

const fetchProgressList = async () => {
  loading.value = true
  try {
    const res = await getReadingProgressList({
      page: currentPage.value,
      size: pageSize.value,
      status: statusFilter.value
    })
    progressList.value = res.data?.records || []
    progressTotal.value = res.data?.total || 0
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const fetchNotes = async () => {
  loading.value = true
  try {
    const res = await getReadingNotes({
      page: notesPage.value,
      size: pageSize.value
    })
    notesList.value = res.data?.records || []
    notesTotal.value = res.data?.total || 0
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const fetchStats = async () => {
  try {
    const res = await getReadingStatistics()
    stats.value = res.data || {}
  } catch (e) {
    console.error(e)
  }
}

const searchBooks = async (query) => {
  if (!query) {
    bookOptions.value = []
    return
  }
  bookSearchLoading.value = true
  try {
    const res = await browseBooks({ page: 1, size: 20, keyword: query })
    bookOptions.value = res.data?.records || []
  } catch (e) {
    bookOptions.value = []
  } finally {
    bookSearchLoading.value = false
  }
}

const showAddDialog = () => {
  if (!checkWithFeedback('READING_PROGRESS_CREATE')) return
  if (activeTab.value === 'progress') {
    isUpdate.value = false
    progressForm.value = { bookId: null, totalPages: 100, currentPage: 0, readingMinutes: 0 }
    progressDialogVisible.value = true
  } else {
    editingNote.value = null
    noteFromProgress.value = null
    noteForm.value = { bookId: null, progressId: null, content: '', pageNumber: 0 }
    noteDialogVisible.value = true
  }
}

const submitProgress = async () => {
  if (!checkWithFeedback('READING_PROGRESS_CREATE')) return
  if (!isUpdate.value && !progressForm.value.bookId) {
    ElMessage.warning('请选择图书')
    return
  }
  submitting.value = true
  try {
    await createOrUpdateProgress(progressForm.value)
    ElMessage.success(isUpdate.value ? '更新成功' : '添加成功')
    progressDialogVisible.value = false
    fetchProgressList()
    fetchStats()
  } catch (e) {
    console.error(e)
  } finally {
    submitting.value = false
  }
}

const submitNote = async () => {
  if (!noteForm.value.content) {
    ElMessage.warning('请输入笔记内容')
    return
  }
  if (!editingNote.value && !noteForm.value.bookId && !noteFromProgress.value) {
    ElMessage.warning('请选择图书')
    return
  }
  submitting.value = true
  try {
    if (editingNote.value) {
      await updateReadingNote(editingNote.value.id, {
        content: noteForm.value.content,
        pageNumber: noteForm.value.pageNumber
      })
      ElMessage.success('更新成功')
    } else {
      await addReadingNote(noteForm.value)
      ElMessage.success('添加成功')
    }
    noteDialogVisible.value = false
    fetchNotes()
    fetchStats()
  } catch (e) {
    console.error(e)
  } finally {
    submitting.value = false
  }
}

const handleCommand = async (cmd, item) => {
  if (cmd === 'addNote') {
    if (!checkWithFeedback('READING_PROGRESS_CREATE')) return
  } else if (cmd === 'update' || cmd === 'complete' || cmd === 'pause' || cmd === 'resume') {
    if (!checkWithFeedback('READING_PROGRESS_UPDATE')) return
  } else if (cmd === 'delete') {
    if (!checkWithFeedback('READING_PROGRESS_DELETE')) return
  }
  if (cmd === 'update') {
    isUpdate.value = true
    progressForm.value = {
      bookId: item.bookId,
      totalPages: item.totalPages,
      currentPage: item.currentPage,
      readingMinutes: 0
    }
    progressDialogVisible.value = true
  } else if (cmd === 'complete') {
    await updateProgressStatus(item.id, { status: 'completed' })
    ElMessage.success('已标记完成')
    fetchProgressList()
    fetchStats()
  } else if (cmd === 'pause') {
    await updateProgressStatus(item.id, { status: 'paused' })
    ElMessage.success('已暂停')
    fetchProgressList()
  } else if (cmd === 'resume') {
    await updateProgressStatus(item.id, { status: 'reading' })
    ElMessage.success('已恢复阅读')
    fetchProgressList()
  } else if (cmd === 'addNote') {
    editingNote.value = null
    noteFromProgress.value = item
    noteForm.value = {
      bookId: item.bookId,
      progressId: item.id,
      content: '',
      pageNumber: item.currentPage || 0
    }
    noteDialogVisible.value = true
  } else if (cmd === 'delete') {
    ElMessageBox.confirm('确定删除此阅读进度吗？相关笔记也会被删除。', '确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }).then(async () => {
      await deleteProgress(item.id)
      ElMessage.success('已删除')
      fetchProgressList()
      fetchStats()
    }).catch(() => {})
  }
}

const handleEditNote = (note) => {
  if (!checkWithFeedback('READING_PROGRESS_UPDATE')) return
  editingNote.value = note
  noteFromProgress.value = null
  noteForm.value = {
    bookId: note.bookId,
    progressId: note.progressId,
    content: note.content,
    pageNumber: note.pageNumber
  }
  noteDialogVisible.value = true
}

const handleDeleteNote = (note) => {
  if (!checkWithFeedback('READING_PROGRESS_DELETE')) return
  ElMessageBox.confirm('确定删除此笔记吗？', '确认', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    await deleteReadingNote(note.id)
    ElMessage.success('已删除')
    fetchNotes()
    fetchStats()
  }).catch(() => {})
}

const statusLabel = (s) => {
  const map = { reading: '阅读中', completed: '已完成', paused: '已暂停' }
  return map[s] || s
}

const statusType = (s) => {
  const map = { reading: 'primary', completed: 'success', paused: 'warning' }
  return map[s] || 'info'
}

const formatMinutes = (minutes) => {
  if (!minutes) return '0分钟'
  if (minutes < 60) return `${minutes}分钟`
  const h = Math.floor(minutes / 60)
  const m = minutes % 60
  return m > 0 ? `${h}小时${m}分钟` : `${h}小时`
}

const formatTime = (time) => {
  if (!time) return ''
  return time.replace('T', ' ').substring(0, 16)
}

onMounted(() => {
  fetchProgressList()
  fetchStats()
})
</script>

<style scoped>
.progress-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 18px;
}

.stat-card {
  background: white;
  border-radius: var(--radius-md);
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 14px;
  box-shadow: var(--shadow-soft);
  transition: all 0.3s ease;
}

.stat-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 24px rgba(149, 125, 173, 0.12);
}

.stat-icon {
  font-size: 32px;
  animation: float 3s ease-in-out infinite;
}

.stat-info {
  display: flex;
  flex-direction: column;
}

.stat-value {
  font-size: 22px;
  font-weight: 700;
  color: var(--text-primary);
}

.stat-label {
  font-size: 12px;
  color: var(--text-secondary);
  margin-top: 2px;
}

.action-bar {
  background: white;
  border-radius: var(--radius-md);
  padding: 16px 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: var(--shadow-soft);
}

.tab-group :deep(.el-radio-button) {
  --el-radio-button-checked-bg-color: transparent;
  --el-radio-button-checked-border-color: transparent;
}

.tab-group :deep(.el-radio-button__inner) {
  border-radius: 24px !important;
  padding: 10px 24px;
  border: 2px solid #f0f0f0 !important;
  box-shadow: none !important;
  font-weight: 500;
  font-size: 13px;
  color: var(--text-secondary);
  background: #fafafa;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.tab-group :deep(.el-radio-button__inner:hover) {
  border-color: var(--purple-light) !important;
  color: var(--purple);
  background: linear-gradient(135deg, #f8f5ff, #fff5f8);
  transform: translateY(-1px);
}

.tab-group :deep(.el-radio-button.is-active .el-radio-button__inner) {
  background: linear-gradient(135deg, var(--purple-light), var(--pink-light)) !important;
  border-color: transparent !important;
  color: #fff !important;
  font-weight: 600;
  box-shadow: 0 4px 12px rgba(149, 125, 173, 0.3) !important;
  transform: translateY(-1px);
}

.tab-group :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
  background: linear-gradient(135deg, var(--purple-light), var(--pink-light)) !important;
  border-color: transparent !important;
  color: #fff !important;
  font-weight: 600;
}

.action-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.add-btn {
  background: linear-gradient(135deg, var(--purple), #B48EE0) !important;
  border: none !important;
  color: white !important;
}

.add-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(149, 125, 173, 0.3);
}

.list-container {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.progress-card {
  background: white;
  border-radius: var(--radius-md);
  padding: 20px;
  box-shadow: var(--shadow-soft);
  transition: all 0.3s ease;
}

.progress-card:hover {
  transform: translateX(4px);
  box-shadow: 0 6px 20px rgba(149, 125, 173, 0.1);
}

.progress-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.book-info-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.book-emoji {
  font-size: 28px;
}

.book-detail h4 {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 4px;
}

.more-btn {
  color: var(--text-secondary);
}

.progress-bar-section {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.progress-bar-bg {
  flex: 1;
  height: 8px;
  border-radius: 4px;
  background: #f0f0f5;
  overflow: hidden;
}

.progress-bar-fill {
  height: 100%;
  border-radius: 4px;
  background: linear-gradient(90deg, var(--green), var(--blue), var(--purple));
  transition: width 0.6s cubic-bezier(0.4, 0, 0.2, 1);
}

.progress-text {
  font-size: 13px;
  font-weight: 600;
  color: var(--purple);
  min-width: 40px;
  text-align: right;
}

.progress-meta {
  display: flex;
  gap: 20px;
  color: var(--text-secondary);
  font-size: 12px;
}

.progress-meta span {
  display: flex;
  align-items: center;
  gap: 4px;
}

/* 笔记卡片 */
.note-card {
  background: white;
  border-radius: var(--radius-md);
  padding: 18px 20px;
  box-shadow: var(--shadow-soft);
  transition: all 0.3s ease;
  border-left: 3px solid transparent;
}

.note-card:hover {
  border-left-color: var(--purple);
  transform: translateX(4px);
}

.note-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.note-book {
  display: flex;
  align-items: center;
  gap: 8px;
}

.note-emoji {
  font-size: 18px;
}

.note-book-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}

.note-actions {
  display: flex;
  gap: 4px;
}

.note-content {
  font-size: 14px;
  color: var(--text-primary);
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}

.note-time {
  margin-top: 10px;
  font-size: 11px;
  color: var(--text-secondary);
}

/* 通用 */
.empty-state {
  text-align: center;
  padding: 60px 0;
}

.empty-icon {
  font-size: 56px;
  margin-bottom: 12px;
  animation: float 3s ease-in-out infinite;
}

.empty-state p {
  color: var(--text-secondary);
  font-size: 15px;
  margin: 0;
}

.empty-tip {
  font-size: 13px !important;
  margin-top: 6px !important;
}

.pagination-wrapper {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}

.form-tip {
  margin-left: 8px;
  font-size: 12px;
  color: var(--text-secondary);
}

.card-list-enter-active,
.card-list-leave-active {
  transition: all 0.4s ease;
}

.card-list-enter-from {
  opacity: 0;
  transform: translateY(16px);
}

.card-list-leave-to {
  opacity: 0;
  transform: scale(0.95);
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-6px); }
}

@media (max-width: 768px) {
  .stats-row {
    grid-template-columns: repeat(2, 1fr);
  }

  .action-bar {
    flex-direction: column;
    gap: 12px;
    align-items: stretch;
  }

  .action-right {
    justify-content: flex-end;
  }
}
</style>
