<template>
  <div class="my-borrows-page">
    <div class="stats-row animate__animated animate__fadeInDown">
      <div class="stat-card" v-for="(stat, index) in stats" :key="stat.label" :style="{ animationDelay: `${index * 0.1}s` }">
        <div class="stat-icon" :style="{ background: stat.bg }">{{ stat.icon }}</div>
        <div class="stat-info">
          <div class="stat-value">{{ stat.value }}</div>
          <div class="stat-label">{{ stat.label }}</div>
        </div>
      </div>
    </div>

    <div class="filter-section animate__animated animate__fadeInUp">
      <el-radio-group v-model="statusFilter" @change="fetchRecords" class="status-filter">
        <el-radio-button value="all">全部</el-radio-button>
        <el-radio-button value="borrowing">借阅中</el-radio-button>
        <el-radio-button value="returned">已归还</el-radio-button>
        <el-radio-button value="overdue">逾期</el-radio-button>
      </el-radio-group>
    </div>

    <div class="records-table animate__animated animate__fadeInUp" style="animation-delay: 0.2s">
      <el-table :data="records" stripe style="width: 100%" v-loading="loading" empty-text="暂无借阅记录">
        <el-table-column type="index" label="#" width="50" />
        <el-table-column prop="bookTitle" label="图书名称" min-width="180">
          <template #default="{ row }">
            <span class="book-title">📚 {{ row.bookTitle }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="borrowDate" label="借阅日期" width="120" />
        <el-table-column prop="dueDate" label="应还日期" width="120" />
        <el-table-column prop="returnDate" label="归还日期" width="120">
          <template #default="{ row }">
            <span>{{ row.returnDate || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag
              :type="statusType(row.status)"
              effect="light"
              round
              class="status-tag"
            >
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper" v-if="total > 0">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @current-change="fetchRecords"
          @size-change="fetchRecords"
        />
      </div>
    </div>

    <div v-if="!loading && records.length === 0" class="empty-state animate__animated animate__fadeIn">
      <div class="empty-icon">📭</div>
      <p>暂无借阅记录</p>
      <el-button v-permission="'READER_BOOK_BROWSE'" type="primary" round @click="$router.push('/reader/books')">去浏览图书</el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { getMyBorrowRecords } from '@/api'

const loading = ref(false)
const records = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const statusFilter = ref('all')

const statsData = reactive({
  totalBorrows: 0,
  borrowingCount: 0,
  returnedCount: 0,
  overdueCount: 0
})

const stats = computed(() => [
  { icon: '📚', label: '总借阅', value: statsData.totalBorrows, bg: 'linear-gradient(135deg, var(--blue-light), var(--blue))' },
  { icon: '📖', label: '借阅中', value: statsData.borrowingCount, bg: 'linear-gradient(135deg, var(--green-light), var(--green))' },
  { icon: '✅', label: '已归还', value: statsData.returnedCount, bg: 'linear-gradient(135deg, var(--yellow), var(--yellow-warm))' },
  { icon: '⏰', label: '逾期', value: statsData.overdueCount, bg: 'linear-gradient(135deg, var(--pink-light), var(--pink))' }
])

const statusType = (status) => {
  const map = { borrowing: 'primary', returned: 'success', overdue: 'danger' }
  return map[status] || 'info'
}

const statusLabel = (status) => {
  const map = { borrowing: '借阅中', returned: '已归还', overdue: '逾期' }
  return map[status] || status
}

const fetchRecords = async () => {
  loading.value = true
  try {
    const res = await getMyBorrowRecords({
      page: currentPage.value,
      size: pageSize.value,
      status: statusFilter.value === 'all' ? '' : statusFilter.value
    })
    records.value = res.data?.records || []
    total.value = res.data?.total || 0
    statsData.totalBorrows = res.data?.totalBorrows || 0
    statsData.borrowingCount = res.data?.borrowingCount || 0
    statsData.returnedCount = res.data?.returnedCount || 0
    statsData.overdueCount = res.data?.overdueCount || 0
  } catch (e) {
    console.error('获取借阅记录失败:', e)
    records.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

onMounted(fetchRecords)
</script>

<style scoped>
.my-borrows-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
  width: 100%;
  max-width: 100%;
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.stat-card {
  background: white;
  border-radius: var(--radius-md);
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 14px;
  box-shadow: var(--shadow-soft);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
}

.stat-card::after {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: linear-gradient(90deg, var(--purple-light), var(--pink-light));
  opacity: 0;
  transition: opacity 0.3s;
}

.stat-card:hover {
  transform: translateY(-4px) scale(1.02);
  box-shadow: var(--shadow-hover);
}

.stat-card:hover::after {
  opacity: 1;
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.stat-card:hover .stat-icon {
  transform: scale(1.15) rotate(5deg);
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: var(--text-primary);
}

.stat-label {
  font-size: 13px;
  color: var(--text-secondary);
  margin-top: 2px;
}

.filter-section {
  background: white;
  border-radius: var(--radius-md);
  padding: 16px 20px;
  box-shadow: var(--shadow-soft);
}

.status-filter {
  display: flex;
  gap: 10px;
}

.status-filter :deep(.el-radio-button) {
  --el-radio-button-checked-bg-color: transparent;
  --el-radio-button-checked-border-color: transparent;
}

.status-filter :deep(.el-radio-button__inner) {
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

.status-filter :deep(.el-radio-button__inner:hover) {
  border-color: var(--purple-light) !important;
  color: var(--purple);
  background: linear-gradient(135deg, #f8f5ff, #fff5f8);
  transform: translateY(-1px);
}

.status-filter :deep(.el-radio-button.is-active .el-radio-button__inner) {
  background: linear-gradient(135deg, var(--purple-light), var(--pink-light)) !important;
  border-color: transparent !important;
  color: #fff !important;
  font-weight: 600;
  box-shadow: 0 4px 12px rgba(149, 125, 173, 0.3) !important;
  transform: translateY(-1px);
}

.status-filter :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
  background: linear-gradient(135deg, var(--purple-light), var(--pink-light)) !important;
  border-color: transparent !important;
  color: #fff !important;
  font-weight: 600;
  box-shadow: 0 4px 12px rgba(149, 125, 173, 0.3) !important;
}

.records-table {
  background: white;
  border-radius: var(--radius-md);
  padding: 20px;
  box-shadow: var(--shadow-soft);
  overflow-x: auto;
}

.book-title {
  font-weight: 500;
  color: var(--text-primary);
}

.status-tag {
  font-weight: 500;
}

.pagination-wrapper {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.empty-state {
  text-align: center;
  padding: 60px 0;
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 16px;
  animation: float 3s ease-in-out infinite;
}

.empty-state p {
  color: var(--text-secondary);
  margin-bottom: 20px;
  font-size: 16px;
}

@media (max-width: 768px) {
  .stats-row {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
