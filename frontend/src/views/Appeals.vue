<template>
  <div class="appeals-page">
    <!-- 统计卡片 -->
    <div class="stats-row animate__animated animate__fadeInDown">
      <div class="stat-card" v-for="(stat, index) in statCards" :key="stat.label" :style="{ animationDelay: `${index * 0.08}s` }">
        <div class="stat-icon" :style="{ background: stat.bg }">{{ stat.icon }}</div>
        <div class="stat-info">
          <div class="stat-value">{{ stat.value }}</div>
          <div class="stat-label">{{ stat.label }}</div>
        </div>
      </div>
    </div>

    <!-- 搜索筛选栏 -->
    <el-card class="filter-card animate__animated animate__fadeInUp">
      <div class="filter-bar">
        <el-select v-model="statusFilter" placeholder="全部状态" clearable @change="handleSearch" style="width: 140px">
          <el-option label="全部" value="" />
          <el-option label="待处理" value="pending" />
          <el-option label="已通过" value="approved" />
          <el-option label="已拒绝" value="rejected" />
        </el-select>
        <el-input v-model="keyword" placeholder="搜索读者姓名/原因..." prefix-icon="Search" clearable style="width: 240px" @keyup.enter="handleSearch" />
        <el-button type="primary" @click="handleSearch">
          <el-icon><Search /></el-icon> 搜索
        </el-button>
      </div>
    </el-card>

    <!-- 数据表格 -->
    <div class="table-section animate__animated animate__fadeInUp" style="animation-delay: 0.1s">
      <el-table :data="records" stripe v-loading="loading" empty-text="暂无申诉记录">
        <el-table-column type="index" label="#" width="50" />
        <el-table-column prop="reader_name" label="读者姓名" width="120" />
        <el-table-column prop="type" label="类型" width="120">
          <template #default="{ row }">
            <el-tag :type="typeTagType(row.type)" effect="light" round size="small">
              {{ typeLabel(row.type) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reason" label="申诉原因" min-width="200" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" effect="light" round size="small">
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="create_time" label="提交时间" width="160">
          <template #default="{ row }">
            {{ formatTime(row.create_time) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'pending'"
              v-permission="'APPEAL_REVIEW'"
              class="action-btn-approve"
              size="small"
              round
              @click="handleReview(row, 'approved')"
            >通过</el-button>
            <el-button
              v-if="row.status === 'pending'"
              v-permission="'APPEAL_REVIEW'"
              class="action-btn-reject"
              size="small"
              round
              @click="handleReview(row, 'rejected')"
            >拒绝</el-button>
            <el-button
              v-permission="'APPEAL_READ'"
              type="info"
              text
              size="small"
              @click="handleDetail(row)"
            >详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
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

    <!-- 审核对话框 -->
    <el-dialog v-model="reviewDialog.visible" title="审核申诉" width="520px" destroy-on-close>
      <div class="review-detail">
        <div class="detail-row">
          <span class="detail-label">读者姓名：</span>
          <span class="detail-value">{{ reviewDialog.data.reader_name }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">申诉类型：</span>
          <el-tag :type="typeTagType(reviewDialog.data.type)" effect="light" round size="small">
            {{ typeLabel(reviewDialog.data.type) }}
          </el-tag>
        </div>
        <div class="detail-row">
          <span class="detail-label">申诉原因：</span>
          <span class="detail-value">{{ reviewDialog.data.reason }}</span>
        </div>
        <div class="detail-row" v-if="reviewDialog.data.evidence">
          <span class="detail-label">证据材料：</span>
          <span class="detail-value">{{ reviewDialog.data.evidence }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">提交时间：</span>
          <span class="detail-value">{{ formatTime(reviewDialog.data.create_time) }}</span>
        </div>
        <div class="detail-row" style="margin-top: 16px;">
          <span class="detail-label">处理反馈：</span>
        </div>
        <el-input
          v-model="reviewDialog.feedback"
          type="textarea"
          :rows="4"
          placeholder="请输入审核反馈意见..."
          style="margin-top: 8px;"
        />
      </div>
      <template #footer>
        <el-button @click="reviewDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="reviewDialog.loading" @click="confirmReview('approved')">通过</el-button>
        <el-button type="danger" :loading="reviewDialog.loading" @click="confirmReview('rejected')">拒绝</el-button>
      </template>
    </el-dialog>

    <!-- 详情对话框 -->
    <el-dialog v-model="detailDialog.visible" title="申诉详情" width="520px" destroy-on-close>
      <div class="review-detail">
        <div class="detail-row">
          <span class="detail-label">读者姓名：</span>
          <span class="detail-value">{{ detailDialog.data.reader_name }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">申诉类型：</span>
          <el-tag :type="typeTagType(detailDialog.data.type)" effect="light" round size="small">
            {{ typeLabel(detailDialog.data.type) }}
          </el-tag>
        </div>
        <div class="detail-row">
          <span class="detail-label">申诉原因：</span>
          <span class="detail-value">{{ detailDialog.data.reason }}</span>
        </div>
        <div class="detail-row" v-if="detailDialog.data.evidence">
          <span class="detail-label">证据材料：</span>
          <span class="detail-value">{{ detailDialog.data.evidence }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">提交时间：</span>
          <span class="detail-value">{{ formatTime(detailDialog.data.create_time) }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">当前状态：</span>
          <el-tag :type="statusTagType(detailDialog.data.status)" effect="light" round size="small">
            {{ statusLabel(detailDialog.data.status) }}
          </el-tag>
        </div>
        <div class="detail-row" v-if="detailDialog.data.feedback">
          <span class="detail-label">管理员反馈：</span>
          <span class="detail-value">{{ detailDialog.data.feedback }}</span>
        </div>
      </div>
      <template #footer>
        <el-button @click="detailDialog.visible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { getAdminAppeals, reviewAppeal } from '@/api'

const loading = ref(false)
const records = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const keyword = ref('')
const statusFilter = ref('')

const stats = reactive({ total: 0, pending: 0, approved: 0, rejected: 0 })

const statCards = computed(() => [
  { icon: '📋', label: '总申诉', value: stats.total, bg: 'linear-gradient(135deg, var(--blue), #89CFF0)' },
  { icon: '⏳', label: '待处理', value: stats.pending, bg: 'linear-gradient(135deg, #F5A623, #FBC02D)', highlight: true },
  { icon: '✅', label: '已通过', value: stats.approved, bg: 'linear-gradient(135deg, var(--green), #8DD5BE)' },
  { icon: '❌', label: '已拒绝', value: stats.rejected, bg: 'linear-gradient(135deg, var(--pink), #F2A7B0)' }
])

const reviewDialog = reactive({
  visible: false,
  loading: false,
  feedback: '',
  action: '',
  data: {}
})

const detailDialog = reactive({
  visible: false,
  data: {}
})

const typeTagType = (type) => {
  const map = { suspension: 'warning', overdue_penalty: 'danger', other: 'info' }
  return map[type] || 'info'
}

const typeLabel = (type) => {
  const map = { suspension: '暂停申诉', overdue_penalty: '逾期申诉', other: '其他' }
  return map[type] || type || '-'
}

const statusTagType = (status) => {
  const map = { pending: 'warning', approved: 'success', rejected: 'danger' }
  return map[status] || 'info'
}

const statusLabel = (status) => {
  const map = { pending: '待处理', approved: '已通过', rejected: '已拒绝' }
  return map[status] || status || '-'
}

const formatTime = (time) => {
  if (!time) return '-'
  return time.replace('T', ' ').substring(0, 16)
}

const handleSearch = () => {
  currentPage.value = 1
  fetchList()
}

const fetchList = async () => {
  loading.value = true
  try {
    const res = await getAdminAppeals({
      page: currentPage.value,
      size: pageSize.value,
      keyword: keyword.value,
      status: statusFilter.value
    })
    records.value = res.data.records || []
    total.value = res.data.total || 0
    stats.total = res.data.totalCount || total.value
    stats.pending = res.data.pendingCount || 0
    stats.approved = res.data.approvedCount || 0
    stats.rejected = res.data.rejectedCount || 0
  } catch (e) {
    console.error('获取申诉列表失败:', e)
  } finally {
    loading.value = false
  }
}

const handleReview = (row, action) => {
  reviewDialog.data = { ...row }
  reviewDialog.feedback = ''
  reviewDialog.action = action
  reviewDialog.visible = true
}

const confirmReview = async (action) => {
  reviewDialog.loading = true
  try {
    await reviewAppeal(reviewDialog.data.id, {
      status: action,
      feedback: reviewDialog.feedback
    })
    ElMessage.success(action === 'approved' ? '已通过申诉' : '已拒绝申诉')
    reviewDialog.visible = false
    fetchList()
  } catch (e) {
    console.error('审核失败:', e)
  } finally {
    reviewDialog.loading = false
  }
}

const handleDetail = (row) => {
  detailDialog.data = { ...row }
  detailDialog.visible = true
}

onMounted(fetchList)
</script>

<style scoped>
.appeals-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

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

.stat-card:hover {
  transform: translateY(-3px);
  box-shadow: var(--shadow-hover);
}

.stat-card:nth-child(2) {
  border: 2px solid #F5A623;
  background: linear-gradient(135deg, #FFFDF5, #FFF8E1);
}

.stat-icon {
  width: 46px;
  height: 46px;
  border-radius: 12px;
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
  margin-top: 2px;
}

.filter-card {
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-soft);
}

.filter-card :deep(.el-card__body) {
  padding: 16px 20px;
}

.filter-bar {
  display: flex;
  align-items: center;
  gap: 12px;
}

.table-section {
  background: white;
  border-radius: var(--radius-md);
  padding: 20px;
  box-shadow: var(--shadow-soft);
}

.pagination-wrapper {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

/* Action button styles */
.action-btn-approve {
  background: linear-gradient(135deg, var(--green), #8DD5BE) !important;
  border: none !important;
  color: #fff !important;
}

.action-btn-approve:hover {
  opacity: 0.85;
  transform: translateY(-1px);
}

.action-btn-reject {
  background: linear-gradient(135deg, var(--btn-delete-from), var(--btn-delete-to)) !important;
  border: none !important;
  color: #fff !important;
}

.action-btn-reject:hover {
  opacity: 0.85;
  transform: translateY(-1px);
}

/* Review/Detail dialog styles */
.review-detail {
  padding: 8px 0;
}

.detail-row {
  display: flex;
  align-items: flex-start;
  margin-bottom: 12px;
  line-height: 1.6;
}

.detail-label {
  font-weight: 600;
  color: var(--text-secondary);
  min-width: 90px;
  flex-shrink: 0;
  font-size: 14px;
}

.detail-value {
  color: var(--text-primary);
  font-size: 14px;
  word-break: break-all;
}

@media (max-width: 768px) {
  .stats-row {
    grid-template-columns: repeat(2, 1fr);
  }

  .filter-bar {
    flex-wrap: wrap;
  }
}
</style>
