<template>
  <div class="my-appeals-page">
    <el-card class="header-card">
      <div class="page-header">
        <div class="header-info">
          <h3>我的申诉</h3>
          <span class="header-desc">对借阅纠纷、账号问题、评价审核等进行申诉</span>
        </div>
        <el-button type="primary" @click="showSubmitDialog">
          <el-icon><Plus /></el-icon> 提交申诉
        </el-button>
      </div>
    </el-card>

    <el-card v-loading="loading">
      <el-table :data="appeals" style="width: 100%">
        <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
        <el-table-column prop="type" label="类型" width="130">
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
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click="viewDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-if="total > 0"
        class="pagination"
        layout="total, prev, pager, next"
        :total="total"
        :page-size="pageSize"
        v-model:current-page="currentPage"
        @current-change="fetchAppeals"
      />
      <el-empty v-if="!loading && appeals.length === 0" description="暂无申诉记录" />
    </el-card>

    <!-- 提交申诉对话框 -->
    <el-dialog v-model="submitVisible" title="提交申诉" width="560px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="类型" prop="type">
          <el-select v-model="form.type" placeholder="请选择申诉类型" style="width: 100%">
            <el-option label="借阅纠纷" value="borrow_dispute" />
            <el-option label="账号封禁" value="account_suspended" />
            <el-option label="评价被拒" value="review_rejected" />
            <el-option label="其他" value="other" />
          </el-select>
        </el-form-item>
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" maxlength="200" show-word-limit placeholder="请简要描述您的申诉" />
        </el-form-item>
        <el-form-item label="内容" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="5" maxlength="2000" show-word-limit placeholder="请详细描述您的申诉理由" />
        </el-form-item>
        <el-form-item label="关联ID">
          <el-input v-model.number="form.relatedId" placeholder="相关借阅/评价记录ID（选填）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="submitVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">提交</el-button>
      </template>
    </el-dialog>

    <!-- 详情对话框 -->
    <el-dialog v-model="detailVisible" title="申诉详情" width="560px">
      <el-descriptions :column="1" border v-if="currentAppeal">
        <el-descriptions-item label="标题">{{ currentAppeal.title }}</el-descriptions-item>
        <el-descriptions-item label="类型">
          <el-tag :type="typeTagMap[currentAppeal.type]" size="small">{{ typeNameMap[currentAppeal.type] }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusTagMap[currentAppeal.status]" size="small">{{ statusNameMap[currentAppeal.status] }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="内容">{{ currentAppeal.content }}</el-descriptions-item>
        <el-descriptions-item label="提交时间">{{ currentAppeal.createTime }}</el-descriptions-item>
        <el-descriptions-item v-if="currentAppeal.adminReply" label="处理回复">{{ currentAppeal.adminReply }}</el-descriptions-item>
        <el-descriptions-item v-if="currentAppeal.resolveTime" label="处理时间">{{ currentAppeal.resolveTime }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getMyAppeals, submitAppeal } from '@/api/index'

const loading = ref(false)
const appeals = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const submitVisible = ref(false)
const submitLoading = ref(false)
const detailVisible = ref(false)
const currentAppeal = ref(null)
const formRef = ref(null)

const form = ref({ type: '', title: '', content: '', relatedId: null })

const rules = {
  type: [{ required: true, message: '请选择申诉类型', trigger: 'change' }],
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入申诉内容', trigger: 'blur' }]
}

const typeNameMap = { borrow_dispute: '借阅纠纷', account_suspended: '账号封禁', review_rejected: '评价被拒', other: '其他' }
const typeTagMap = { borrow_dispute: 'warning', account_suspended: 'danger', review_rejected: 'info', other: '' }
const statusNameMap = { pending: '待处理', processing: '处理中', resolved: '已解决', rejected: '已驳回' }
const statusTagMap = { pending: 'warning', processing: '', resolved: 'success', rejected: 'danger' }

onMounted(() => { fetchAppeals() })

async function fetchAppeals() {
  loading.value = true
  try {
    const res = await getMyAppeals({ page: currentPage.value, size: pageSize.value })
    appeals.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

function showSubmitDialog() {
  form.value = { type: '', title: '', content: '', relatedId: null }
  submitVisible.value = true
}

async function handleSubmit() {
  await formRef.value.validate()
  submitLoading.value = true
  try {
    const data = { ...form.value }
    if (!data.relatedId) delete data.relatedId
    await submitAppeal(data)
    ElMessage.success('申诉提交成功')
    submitVisible.value = false
    fetchAppeals()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '提交失败')
  } finally {
    submitLoading.value = false
  }
}

function viewDetail(row) {
  currentAppeal.value = row
  detailVisible.value = true
}
</script>

<style scoped>
.my-appeals-page {
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
.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
