<template>
  <div class="applications-page">
    <div class="page-header">
      <h2>管理员申请审批</h2>
      <div class="header-stats">
        <el-tag type="warning" effect="light" round>待审批: {{ pendingCount }}</el-tag>
      </div>
    </div>

    <div class="filter-section">
      <el-radio-group v-model="statusFilter" @change="fetchList" class="status-filter">
        <el-radio-button value="">全部</el-radio-button>
        <el-radio-button value="pending">待审批</el-radio-button>
        <el-radio-button value="approved">已通过</el-radio-button>
        <el-radio-button value="rejected">已拒绝</el-radio-button>
      </el-radio-group>
    </div>

    <div class="table-section">
      <el-table :data="records" stripe v-loading="loading" empty-text="暂无申请记录">
        <el-table-column type="index" label="#" width="50" />
        <el-table-column prop="readerName" label="读者姓名" width="120" />
        <el-table-column prop="username" label="账号" width="120" />
        <el-table-column prop="reason" label="申请理由" min-width="200">
          <template #default="{ row }">
            <el-tooltip :content="row.reason" placement="top" :disabled="row.reason.length < 30">
              <span class="reason-text">{{ row.reason }}</span>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" effect="light" round size="small">
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="申请时间" width="160">
          <template #default="{ row }">
            {{ formatTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 'pending'">
              <el-button v-permission="'ADMIN_APPLICATION_REVIEW'" type="success" size="small" round @click="handleApprove(row)">通过</el-button>
              <el-button v-permission="'ADMIN_APPLICATION_REVIEW'" type="danger" size="small" round @click="handleReject(row)">拒绝</el-button>
            </template>
            <span v-else class="processed-text">
              {{ row.status === 'approved' ? '已通过' : '已拒绝' }}
            </span>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper" v-if="total > 10">
        <el-pagination
          v-model:current-page="currentPage"
          :total="total"
          :page-size="pageSize"
          layout="total, prev, pager, next"
          @current-change="fetchList"
        />
      </div>
    </div>

    <el-dialog v-model="rejectDialog.visible" title="拒绝申请" width="400px">
      <el-input
        v-model="rejectDialog.reason"
        type="textarea"
        :rows="3"
        placeholder="请输入拒绝原因（可选）"
      />
      <template #footer>
        <el-button @click="rejectDialog.visible = false">取消</el-button>
        <el-button type="danger" :loading="rejectDialog.loading" @click="confirmReject">确认拒绝</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAdminApplications, approveApplication, rejectApplication } from '@/api'
import { usePermission } from '@/composables/usePermission'
const { checkWithFeedback } = usePermission()

const loading = ref(false)
const records = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const statusFilter = ref('')
const pendingCount = ref(0)

const rejectDialog = reactive({
  visible: false,
  loading: false,
  reason: '',
  id: null
})

const statusTagType = (status) => {
  const map = { pending: 'warning', approved: 'success', rejected: 'danger' }
  return map[status] || 'info'
}

const statusLabel = (status) => {
  const map = { pending: '待审批', approved: '已通过', rejected: '已拒绝' }
  return map[status] || status
}

const formatTime = (time) => {
  if (!time) return '-'
  return time.replace('T', ' ').substring(0, 16)
}

const fetchList = async () => {
  loading.value = true
  try {
    const res = await getAdminApplications({
      page: currentPage.value,
      size: pageSize.value,
      status: statusFilter.value
    })
    records.value = res.data.records
    total.value = res.data.total
    pendingCount.value = res.data.pendingCount || 0
  } catch (e) {
    console.error('获取申请列表失败:', e)
  } finally {
    loading.value = false
  }
}

const handleApprove = (row) => {
  if (!checkWithFeedback('ADMIN_APPLICATION_REVIEW')) return
  ElMessageBox.confirm(
    `确定通过「${row.readerName}」的管理员申请吗？通过后该用户将获得管理员权限。`,
    '审批确认',
    { confirmButtonText: '确认通过', cancelButtonText: '取消', type: 'warning' }
  ).then(async () => {
    try {
      await approveApplication(row.id)
      ElMessage.success('已通过申请')
      fetchList()
    } catch (e) {
      console.error('审批失败:', e)
    }
  }).catch(() => {})
}

const handleReject = (row) => {
  if (!checkWithFeedback('ADMIN_APPLICATION_REVIEW')) return
  rejectDialog.id = row.id
  rejectDialog.reason = ''
  rejectDialog.visible = true
}

const confirmReject = async () => {
  if (!checkWithFeedback('ADMIN_APPLICATION_REVIEW')) return
  rejectDialog.loading = true
  try {
    await rejectApplication(rejectDialog.id, { rejectReason: rejectDialog.reason })
    ElMessage.success('已拒绝申请')
    rejectDialog.visible = false
    fetchList()
  } catch (e) {
    console.error('拒绝失败:', e)
  } finally {
    rejectDialog.loading = false
  }
}

onMounted(fetchList)
</script>

<style scoped>
.applications-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.page-header h2 {
  font-size: 20px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0;
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

.table-section {
  background: white;
  border-radius: var(--radius-md);
  padding: 20px;
  box-shadow: var(--shadow-soft);
}

.reason-text {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  font-size: 13px;
}

.processed-text {
  font-size: 13px;
  color: var(--text-secondary);
}

.pagination-wrapper {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
