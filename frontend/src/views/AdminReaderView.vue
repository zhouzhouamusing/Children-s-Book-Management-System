<template>
  <div class="admin-reader-view">
    <div class="view-tabs">
      <el-radio-group v-model="activeTab" @change="handleTabChange" class="tab-group">
        <el-radio-button value="books">图书浏览</el-radio-button>
        <el-radio-button value="borrows">借阅记录</el-radio-button>
        <el-radio-button value="profile">个人中心</el-radio-button>
      </el-radio-group>
    </div>

    <!-- Books browsing tab -->
    <div v-if="activeTab === 'books'" class="tab-content">
      <div class="search-bar">
        <el-input
          v-model="bookKeyword"
          placeholder="搜索图书名称或作者..."
          prefix-icon="Search"
          size="large"
          clearable
          @input="debouncedBookSearch"
          class="search-input"
        />
        <el-select v-model="bookCategory" placeholder="所有分类" clearable size="large" @change="fetchBooks">
          <el-option v-for="cat in categories" :key="cat" :label="cat" :value="cat" />
        </el-select>
      </div>
      <div v-loading="booksLoading" class="book-grid">
        <div v-for="book in books" :key="book.id" class="book-card">
          <div class="book-cover">
            <span class="cover-emoji">📖</span>
            <div class="stock-badge" :class="{ 'low-stock': book.stock <= 3 }">
              {{ book.stock > 0 ? `余${book.stock}册` : '已借完' }}
            </div>
          </div>
          <div class="book-info">
            <h3>{{ book.title }}</h3>
            <p class="book-author">{{ book.author }}</p>
            <div class="book-meta">
              <el-tag size="small" effect="plain" round>{{ book.category }}</el-tag>
              <span class="age-range" v-if="book.ageRange">{{ book.ageRange }}</span>
            </div>
            <p class="book-desc" v-if="book.description">{{ book.description }}</p>
          </div>
        </div>
      </div>
      <div v-if="!booksLoading && books.length === 0" class="empty-state">
        <div class="empty-icon">🔍</div>
        <p>未找到相关图书</p>
      </div>
      <div class="pagination-wrapper" v-if="booksTotal > bookPageSize">
        <el-pagination
          v-model:current-page="bookPage"
          :total="booksTotal"
          :page-size="bookPageSize"
          layout="prev, pager, next"
          @current-change="fetchBooks"
        />
      </div>
    </div>

    <!-- Borrows tab -->
    <div v-if="activeTab === 'borrows'" class="tab-content">
      <div class="stats-row">
        <div class="stat-card" v-for="stat in borrowStats" :key="stat.label">
          <div class="stat-icon" :style="{ background: stat.bg }">{{ stat.icon }}</div>
          <div class="stat-info">
            <div class="stat-value">{{ stat.value }}</div>
            <div class="stat-label">{{ stat.label }}</div>
          </div>
        </div>
      </div>
      <div class="filter-section">
        <el-radio-group v-model="borrowStatus" @change="fetchBorrows">
          <el-radio-button value="all">全部</el-radio-button>
          <el-radio-button value="borrowing">借阅中</el-radio-button>
          <el-radio-button value="returned">已归还</el-radio-button>
          <el-radio-button value="overdue">逾期</el-radio-button>
        </el-radio-group>
      </div>
      <div class="table-section" v-loading="borrowsLoading">
        <el-table :data="borrowRecords" stripe empty-text="暂无借阅记录">
          <el-table-column type="index" label="#" width="50" />
          <el-table-column prop="bookTitle" label="图书名称" min-width="180">
            <template #default="{ row }">
              <span>📚 {{ row.bookTitle }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="borrowDate" label="借阅日期" width="120" />
          <el-table-column prop="dueDate" label="应还日期" width="120" />
          <el-table-column prop="returnDate" label="归还日期" width="120">
            <template #default="{ row }">{{ row.returnDate || '-' }}</template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="borrowStatusType(row.status)" effect="light" round size="small">
                {{ borrowStatusLabel(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <div v-if="!borrowsLoading && borrowRecords.length === 0" class="empty-state">
        <div class="empty-icon">📭</div>
        <p>暂无借阅记录</p>
      </div>
    </div>

    <!-- Profile tab -->
    <div v-if="activeTab === 'profile'" class="tab-content">
      <div class="admin-profile-card">
        <div class="profile-info">
          <div class="profile-avatar">👤</div>
          <div class="profile-details">
            <h3>{{ nickname }}</h3>
            <el-tag type="success" effect="light" round size="small">管理员</el-tag>
            <p class="profile-hint">您当前以管理员身份浏览读者系统</p>
          </div>
        </div>
        <div class="admin-stats">
          <div class="admin-stat-item">
            <span class="admin-stat-value">{{ borrowStatsData.totalBorrows }}</span>
            <span class="admin-stat-label">总借阅</span>
          </div>
          <div class="admin-stat-item">
            <span class="admin-stat-value">{{ borrowStatsData.overdueCount }}</span>
            <span class="admin-stat-label">逾期</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { browseBooks, getAllCategories, getBorrows } from '@/api'

const activeTab = ref('books')
const nickname = ref(localStorage.getItem('nickname') || '管理员')

// Books
const booksLoading = ref(false)
const books = ref([])
const booksTotal = ref(0)
const bookPage = ref(1)
const bookPageSize = ref(12)
const bookKeyword = ref('')
const bookCategory = ref('')
const categories = ref([])

let bookSearchTimer = null
const debouncedBookSearch = () => {
  clearTimeout(bookSearchTimer)
  bookSearchTimer = setTimeout(() => {
    bookPage.value = 1
    fetchBooks()
  }, 300)
}

const fetchBooks = async () => {
  booksLoading.value = true
  try {
    const res = await browseBooks({
      page: bookPage.value,
      size: bookPageSize.value,
      keyword: bookKeyword.value,
      category: bookCategory.value
    })
    books.value = res.data.records
    booksTotal.value = res.data.total
  } catch (e) {
    console.error('获取图书失败:', e)
  } finally {
    booksLoading.value = false
  }
}

const fetchCategories = async () => {
  try {
    const res = await getAllCategories()
    categories.value = res.data.map(c => c.name)
  } catch (e) {
    console.error(e)
  }
}

// Borrows
const borrowsLoading = ref(false)
const borrowRecords = ref([])
const borrowStatus = ref('all')
const borrowStatsData = reactive({
  totalBorrows: 0,
  borrowingCount: 0,
  returnedCount: 0,
  overdueCount: 0
})

const borrowStats = computed(() => [
  { icon: '📚', label: '总借阅', value: borrowStatsData.totalBorrows, bg: 'linear-gradient(135deg, var(--blue-light), var(--blue))' },
  { icon: '📖', label: '借阅中', value: borrowStatsData.borrowingCount, bg: 'linear-gradient(135deg, var(--green-light), var(--green))' },
  { icon: '✅', label: '已归还', value: borrowStatsData.returnedCount, bg: 'linear-gradient(135deg, var(--yellow), var(--yellow-warm))' },
  { icon: '⏰', label: '逾期', value: borrowStatsData.overdueCount, bg: 'linear-gradient(135deg, var(--pink-light), var(--pink))' }
])

const borrowStatusType = (status) => {
  const map = { borrowing: 'primary', returned: 'success', overdue: 'danger' }
  return map[status] || 'info'
}

const borrowStatusLabel = (status) => {
  const map = { borrowing: '借阅中', returned: '已归还', overdue: '逾期' }
  return map[status] || status
}

const fetchBorrows = async () => {
  borrowsLoading.value = true
  try {
    const res = await getBorrows({
      page: 1,
      size: 50,
      status: borrowStatus.value === 'all' ? '' : borrowStatus.value
    })
    borrowRecords.value = res.data.records || res.data || []
    if (res.data.total != null) {
      borrowStatsData.totalBorrows = res.data.total
    }
    if (Array.isArray(res.data)) {
      borrowStatsData.totalBorrows = res.data.length
      borrowStatsData.borrowingCount = res.data.filter(r => r.status === 'borrowing').length
      borrowStatsData.overdueCount = res.data.filter(r => r.status === 'overdue').length
      borrowStatsData.returnedCount = res.data.filter(r => r.status === 'returned').length
    }
  } catch (e) {
    console.error('获取借阅记录失败:', e)
  } finally {
    borrowsLoading.value = false
  }
}

const handleTabChange = (tab) => {
  if (tab === 'books' && books.value.length === 0) fetchBooks()
  if (tab === 'borrows' && borrowRecords.value.length === 0) fetchBorrows()
}

onMounted(() => {
  fetchBooks()
  fetchCategories()
  fetchBorrows()
})
</script>

<style scoped>
.admin-reader-view {
  display: flex;
  flex-direction: column;
  gap: 20px;
  width: 100%;
}

.view-tabs {
  margin-bottom: 4px;
}

.tab-group :deep(.el-radio-button__inner) {
  border-radius: 20px !important;
  padding: 10px 24px;
  border: none !important;
  box-shadow: none !important;
}

.tab-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* Books */
.search-bar {
  display: flex;
  gap: 12px;
}

.search-input {
  flex: 1;
}

.book-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}

.book-card {
  background: white;
  border-radius: var(--radius-md);
  padding: 16px;
  box-shadow: var(--shadow-soft);
  transition: all 0.3s;
}

.book-card:hover {
  transform: translateY(-3px);
  box-shadow: var(--shadow-hover);
}

.book-cover {
  position: relative;
  background: linear-gradient(135deg, #f8f9ff, #fff5f5);
  border-radius: var(--radius-sm);
  height: 70px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 12px;
}

.cover-emoji {
  font-size: 32px;
}

.stock-badge {
  position: absolute;
  top: 8px;
  right: 8px;
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 10px;
  background: var(--green-light);
  color: #2d8a56;
}

.stock-badge.low-stock {
  background: var(--pink-light);
  color: #c0392b;
}

.book-info h3 {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 4px;
}

.book-author {
  font-size: 12px;
  color: var(--text-secondary);
  margin: 0 0 8px;
}

.book-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.age-range {
  font-size: 11px;
  color: var(--text-secondary);
  background: var(--yellow);
  padding: 2px 8px;
  border-radius: 8px;
}

.book-desc {
  font-size: 12px;
  color: var(--text-secondary);
  margin: 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* Borrows */
.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.stat-card {
  background: white;
  border-radius: var(--radius-md);
  padding: 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  box-shadow: var(--shadow-soft);
}

.stat-icon {
  width: 42px;
  height: 42px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
}

.stat-value {
  font-size: 22px;
  font-weight: 700;
  color: var(--text-primary);
}

.stat-label {
  font-size: 12px;
  color: var(--text-secondary);
}

.filter-section {
  margin: 4px 0;
}

.table-section {
  background: white;
  border-radius: var(--radius-md);
  padding: 16px;
  box-shadow: var(--shadow-soft);
}

/* Profile */
.admin-profile-card {
  background: white;
  border-radius: var(--radius-lg);
  padding: 32px;
  box-shadow: var(--shadow-soft);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.profile-info {
  display: flex;
  align-items: center;
  gap: 20px;
}

.profile-avatar {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--blue-light), var(--purple-light));
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 36px;
}

.profile-details h3 {
  font-size: 20px;
  margin: 0 0 6px;
  color: var(--text-primary);
}

.profile-hint {
  font-size: 13px;
  color: var(--text-secondary);
  margin: 8px 0 0;
}

.admin-stats {
  display: flex;
  gap: 32px;
}

.admin-stat-item {
  text-align: center;
}

.admin-stat-value {
  display: block;
  font-size: 24px;
  font-weight: 700;
  color: var(--purple);
}

.admin-stat-label {
  font-size: 12px;
  color: var(--text-secondary);
}

/* Common */
.empty-state {
  text-align: center;
  padding: 40px 0;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 12px;
}

.empty-state p {
  color: var(--text-secondary);
  font-size: 14px;
}

.pagination-wrapper {
  margin-top: 16px;
  display: flex;
  justify-content: center;
}

@media (max-width: 768px) {
  .stats-row {
    grid-template-columns: repeat(2, 1fr);
  }
  .admin-profile-card {
    flex-direction: column;
    gap: 20px;
  }
}
</style>
