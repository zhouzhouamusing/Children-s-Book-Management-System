<template>
  <div class="borrows-page">
    <div class="stats-row animate__animated animate__fadeInDown">
      <div class="stat-card" v-for="(stat, index) in statCards" :key="stat.label" :style="{ animationDelay: `${index * 0.08}s` }">
        <div class="stat-icon" :style="{ background: stat.bg }">{{ stat.icon }}</div>
        <div class="stat-info">
          <div class="stat-value">{{ stat.value }}</div>
          <div class="stat-label">{{ stat.label }}</div>
        </div>
      </div>
    </div>

    <div class="toolbar animate__animated animate__fadeInUp">
      <div class="toolbar-left">
        <el-input v-model="keyword" placeholder="搜索图书名称..." prefix-icon="Search" clearable @input="debouncedSearch" style="width: 240px" />
        <el-select v-model="statusFilter" placeholder="状态筛选" clearable @change="fetchList" style="width: 130px">
          <el-option label="全部" value="" />
          <el-option label="借阅中" value="borrowing" />
          <el-option label="已归还" value="returned" />
          <el-option label="逾期" value="overdue" />
        </el-select>
      </div>
      <el-button type="primary" @click="showBorrowDialog = true">
        <el-icon><Plus /></el-icon> 新增借阅
      </el-button>
    </div>

    <div class="table-section animate__animated animate__fadeInUp" style="animation-delay: 0.1s">
      <el-table :data="records" stripe v-loading="loading" empty-text="暂无借阅记录">
        <el-table-column type="index" label="#" width="50" />
        <el-table-column prop="bookTitle" label="图书名称" min-width="160">
          <template #default="{ row }">
            <span class="book-name">📚 {{ row.bookTitle }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="readerId" label="读者ID" width="80" />
        <el-table-column prop="borrowDate" label="借阅日期" width="110" />
        <el-table-column prop="dueDate" label="应还日期" width="110">
          <template #default="{ row }">
            <span :class="{ 'text-danger': isOverdue(row) }">{{ row.dueDate }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="returnDate" label="归还日期" width="110">
          <template #default="{ row }">{{ row.returnDate || '-' }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" effect="light" round>{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 'borrowing' || row.status === 'overdue'">
              <el-button type="success" text size="small" @click="handleReturn(row)">归还</el-button>
              <el-button v-if="row.status === 'borrowing'" type="primary" text size="small" @click="handleRenew(row)">续借</el-button>
            </template>
            <span v-else class="text-muted">已完成</span>
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
          @current-change="fetchList"
          @size-change="fetchList"
        />
      </div>
    </div>

    <!-- 新增借阅对话框 -->
    <el-dialog v-model="showBorrowDialog" title="新增借阅" width="480px" destroy-on-close>
      <el-form ref="borrowFormRef" :model="borrowForm" :rules="borrowRules" label-width="80px">
        <el-form-item label="读者ID" prop="readerId">
          <el-input-number v-model="borrowForm.readerId" :min="1" style="width: 100%" placeholder="请输入读者ID" />
        </el-form-item>
        <el-form-item label="图书ID" prop="bookId">
          <el-input-number v-model="borrowForm.bookId" :min="1" style="width: 100%" placeholder="请输入图书ID" />
        </el-form-item>
        <el-form-item label="借阅天数" prop="borrowDays">
          <el-input-number v-model="borrowForm.borrowDays" :min="7" :max="60" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showBorrowDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleBorrow">确认借出</el-button>
      </template>
    </el-dialog>

    <!-- 续借对话框 -->
    <el-dialog v-model="showRenewDialog" title="续借" width="400px" destroy-on-close>
      <p>确认为《{{ renewTarget?.bookTitle }}》续借？</p>
      <el-form-item label="续借天数">
        <el-input-number v-model="renewDays" :min="7" :max="30" />
      </el-form-item>
      <template #footer>
        <el-button @click="showRenewDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="confirmRenew">确认续借</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getBorrows, createBorrow, returnBorrow, renewBorrow, getBorrowStatistics } from '@/api'

const loading = ref(false)
const submitting = ref(false)
const records = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const keyword = ref('')
const statusFilter = ref('')
const showBorrowDialog = ref(false)
const showRenewDialog = ref(false)
const renewTarget = ref(null)
const renewDays = ref(14)
const borrowFormRef = ref(null)

const stats = reactive({ total: 0, borrowing: 0, overdue: 0, returned: 0, todayBorrows: 0 })

const statCards = computed(() => [
  { icon: '📊', label: '总记录', value: stats.total, bg: 'linear-gradient(135deg, var(--blue-light), var(--blue))' },
  { icon: '📖', label: '借阅中', value: stats.borrowing, bg: 'linear-gradient(135deg, var(--green-light), var(--green))' },
  { icon: '⏰', label: '逾期', value: stats.overdue, bg: 'linear-gradient(135deg, var(--pink-light), var(--pink))' },
  { icon: '📅', label: '今日借出', value: stats.todayBorrows, bg: 'linear-gradient(135deg, var(--yellow), var(--yellow-warm))' }
])

const borrowForm = reactive({ readerId: null, bookId: null, borrowDays: 14 })
const borrowRules = {
  readerId: [{ required: true, message: '请输入读者ID', trigger: 'blur' }],
  bookId: [{ required: true, message: '请输入图书ID', trigger: 'blur' }]
}

const statusType = (s) => ({ borrowing: 'primary', returned: 'success', overdue: 'danger' }[s] || 'info')
const statusLabel = (s) => ({ borrowing: '借阅中', returned: '已归还', overdue: '逾期' }[s] || s)
const isOverdue = (row) => row.status === 'overdue' || (row.status === 'borrowing' && new Date(row.dueDate) < new Date())

let searchTimer = null
const debouncedSearch = () => {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(() => { currentPage.value = 1; fetchList() }, 300)
}

const fetchList = async () => {
  loading.value = true
  try {
    const res = await getBorrows({ page: currentPage.value, size: pageSize.value, keyword: keyword.value, status: statusFilter.value })
    records.value = res.data.records
    total.value = res.data.total
  } catch (e) { console.error(e) }
  finally { loading.value = false }
}

const fetchStats = async () => {
  try {
    const res = await getBorrowStatistics()
    Object.assign(stats, res.data)
  } catch (e) { console.error(e) }
}

const handleBorrow = async () => {
  const valid = await borrowFormRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    await createBorrow(borrowForm)
    ElMessage.success('借阅成功')
    showBorrowDialog.value = false
    borrowForm.readerId = null
    borrowForm.bookId = null
    borrowForm.borrowDays = 14
    fetchList()
    fetchStats()
  } catch (e) { console.error(e) }
  finally { submitting.value = false }
}

const handleReturn = (row) => {
  ElMessageBox.confirm(`确认归还《${row.bookTitle}》？`, '确认归还', {
    confirmButtonText: '确认', cancelButtonText: '取消', type: 'info'
  }).then(async () => {
    try {
      await returnBorrow(row.id)
      ElMessage.success('归还成功')
      fetchList()
      fetchStats()
    } catch (e) { console.error(e) }
  }).catch(() => {})
}

const handleRenew = (row) => {
  renewTarget.value = row
  renewDays.value = 14
  showRenewDialog.value = true
}

const confirmRenew = async () => {
  submitting.value = true
  try {
    await renewBorrow(renewTarget.value.id, renewDays.value)
    ElMessage.success('续借成功')
    showRenewDialog.value = false
    fetchList()
  } catch (e) { console.error(e) }
  finally { submitting.value = false }
}

onMounted(() => { fetchList(); fetchStats() })
</script>

<style scoped>
.borrows-page { display: flex; flex-direction: column; gap: 20px; }

.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.stat-card {
  background: white;
  border-radius: var(--radius-md);
  padding: 18px;
  display: flex;
  align-items: center;
  gap: 14px;
  box-shadow: var(--shadow-soft);
  transition: all 0.3s;
}

.stat-card:hover { transform: translateY(-3px); box-shadow: var(--shadow-hover); }

.stat-icon {
  width: 46px; height: 46px;
  border-radius: 12px;
  display: flex; align-items: center; justify-content: center;
  font-size: 20px;
}

.stat-value { font-size: 22px; font-weight: 700; color: var(--text-primary); }
.stat-label { font-size: 12px; color: var(--text-secondary); margin-top: 2px; }

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.toolbar-left { display: flex; gap: 12px; }

.table-section {
  background: white;
  border-radius: var(--radius-md);
  padding: 20px;
  box-shadow: var(--shadow-soft);
}

.book-name { font-weight: 500; }
.text-danger { color: #e74c3c; font-weight: 500; }
.text-muted { color: var(--text-secondary); font-size: 13px; }
.pagination-wrapper { margin-top: 20px; display: flex; justify-content: flex-end; }

@media (max-width: 768px) {
  .stats-row { grid-template-columns: repeat(2, 1fr); }
}
</style>
