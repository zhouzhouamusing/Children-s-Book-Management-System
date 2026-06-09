<template>
  <div class="appeals-page">
    <el-card class="header-card">
      <div class="page-header">
        <div class="header-info">
          <h3>申诉管理</h3>
          <span class="header-desc">处理读者提交的各类申诉</span>
        </div>
        <div class="header-filters">
          <el-select v-model="filterStatus" placeholder="状态筛选" clearable style="width: 130px" @change="fetchList">
            <el-option label="待处理" value="pending" />
            <el-option label="处理中" value="processing" />
            <el-option label="已解决" value="resolved" />
            <el-option label="已驳回" value="rejected" />
          </el-select>
          <el-select v-model="filterType" placeholder="类型筛选" clearable style="width: 130px" @change="fetchList">
            <el-option label="借阅纠纷" value="borrow_dispute" />
            <el-option label="账号封禁" value="account_suspended" />
            <el-option label="评价被拒" value="review_rejected" />
            <el-option label="其他" value="other" />
          </el-select>
        </div>
      </div>
    </el-card>

    <el-card v-loading="loading">
      <el-table :data="appeals" style="width: 100%">
        <el-table-column prop="readerName" label="读者" width="100" />
        <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
        <el-table-column prop="type" label="类型" width="120">
          <template #default="{ row }">
            <el-tag :type="typeTagMap[row.type]" size="small">{{ typeNameMap[row.type] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagMap[row.status]" size="small">{{ statusNameMap[row.status] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="提交时间" width="170" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click="viewDetail(row)">详情</el-button>
            <el-button
              v-permission="'appeal:handle'"
              v-if="row.status === 'pending' || row.status === 'processing'"
              text type="success" size="small"
              @click="showHandleDialog(row)"
            >处理</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-if="total > 0"
        class="pagination"
        layout="total, sizes, prev, pager, next"
        :total="total"
        :page-sizes="[10, 20, 50]"
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        @current-change="fetchList"
        @size-change="fetchList"
      />
      <el-empty v-if="!loading && appeals.length === 0" description="暂无申诉记录" />
    </el-card>

    <!-- 详情对话框 -->
    <el-dialog v-model="detailVisible" title="申诉详情" width="600px">
      <el-descriptions :column="1" border v-if="currentAppeal">
        <el-descriptions-item label="读者">{{ currentAppeal.readerName }}</el-descriptions-item>
        <el-descriptions-item label="标题">{{ currentAppeal.title }}</el-descriptions-item>
        <el-descriptions-item label="类型">
          <el-tag :type="typeTagMap[currentAppeal.type]" size="small">{{ typeNameMap[currentAppeal.type] }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusTagMap[currentAppeal.status]" size="small">{{ statusNameMap[currentAppeal.status] }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="申诉内容">{{ currentAppeal.content }}</el-descriptions-item>
        <el-descriptions-item v-if="currentAppeal.relatedId" label="关联ID">{{ currentAppeal.relatedId }}</el-descriptions-item>
        <el-descriptions-item label="提交时间">{{ currentAppeal.createTime }}</el-descriptions-item>
        <el-descriptions-item v-if="currentAppeal.adminReply" label="处理回复">{{ currentAppeal.adminReply }}</el-descriptions-item>
        <el-descriptions-item v-if="currentAppeal.resolveTime" label="处理时间">{{ currentAppeal.resolveTime }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <!-- 处理对话框 -->
    <el-dialog v-model="handleVisible" title="处理申诉" width="500px" destroy-on-close>
      <el-form ref="handleFormRef" :model="handleForm" :rules="handleRules" label-width="80px">
        <el-form-item label="处理结果" prop="status">
          <el-radio-group v-model="handleForm.status">
            <el-radio value="resolved">同意（解决）</el-radio>
            <el-radio value="rejected">驳回</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="回复内容" prop="adminReply">
          <el-input v-model="handleForm.adminReply" type="textarea" :rows="4" maxlength="2000" show-word-limit placeholder="请输入处理意见" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="handleVisible = false">取消</el-button>
        <el-button type="primary" :loading="handleLoading" @click="doHandle">确认处理</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getAdminAppeals, handleAppeal } from '@/api/index'

const loading = ref(false)
const appeals = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const filterStatus = ref('')
const filterType = ref('')

const detailVisible = ref(false)
const currentAppeal = ref(null)
const handleVisible = ref(false)
const handleLoading = ref(false)
const handleFormRef = ref(null)
const handleForm = ref({ status: 'resolved', adminReply: '' })
const handleId = ref(null)

const handleRules = {
  status: [{ required: true, message: '请选择处理结果', trigger: 'change' }],
  adminReply: [{ required: true, message: '请输入回复内容', trigger: 'blur' }]
}

const typeNameMap = { borrow_dispute: '借阅纠纷', account_suspended: '账号封禁', review_rejected: '评价被拒', other: '其他' }
const typeTagMap = { borrow_dispute: 'warning', account_suspended: 'danger', review_rejected: 'info', other: '' }
const statusNameMap = { pending: '待处理', processing: '处理中', resolved: '已解决', rejected: '已驳回' }
const statusTagMap = { pending: 'warning', processing: '', resolved: 'success', rejected: 'danger' }

onMounted(() => { fetchList() })

async function fetchList() {
  loading.value = true
  try {
    const res = await getAdminAppeals({
      page: currentPage.value,
      size: pageSize.value,
      status: filterStatus.value || undefined,
      type: filterType.value || undefined
    })
    appeals.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

function viewDetail(row) {
  currentAppeal.value = row
  detailVisible.value = true
}

function showHandleDialog(row) {
  handleId.value = row.id
  handleForm.value = { status: 'resolved', adminReply: '' }
  handleVisible.value = true
}

async function doHandle() {
  await handleFormRef.value.validate()
  handleLoading.value = true
  try {
    await handleAppeal(handleId.value, handleForm.value)
    ElMessage.success('处理成功')
    handleVisible.value = false
    fetchList()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '处理失败')
  } finally {
    handleLoading.value = false
  }
}
</script>

<style scoped>
.appeals-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.header-info h3 {
  margin: 0 0 4px;
  font-size: 18px;
}
.header-desc {
  font-size: 13px;
  color: var(--text-secondary);
}
.header-filters {
  display: flex;
  gap: 12px;
}
.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
