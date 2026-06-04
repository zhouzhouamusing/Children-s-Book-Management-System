<template>
  <div class="appeals-page">
    <!-- Stat Cards Row -->
    <div class="stat-row animate__animated animate__fadeInDown">
      <div class="mini-stat" style="--accent: var(--purple)">
        <span class="mini-stat-icon">📋</span>
        <div class="mini-stat-info">
          <span class="mini-stat-value">{{ statsData.total }}</span>
          <span class="mini-stat-label">总申诉</span>
        </div>
      </div>
      <div class="mini-stat" style="--accent: var(--blue)">
        <span class="mini-stat-icon">⏳</span>
        <div class="mini-stat-info">
          <span class="mini-stat-value">{{ statsData.pending }}</span>
          <span class="mini-stat-label">待审核</span>
        </div>
      </div>
      <div class="mini-stat" style="--accent: var(--green)">
        <span class="mini-stat-icon">✅</span>
        <div class="mini-stat-info">
          <span class="mini-stat-value">{{ statsData.approved }}</span>
          <span class="mini-stat-label">已通过</span>
        </div>
      </div>
      <div class="mini-stat" style="--accent: var(--pink)">
        <span class="mini-stat-icon">❌</span>
        <div class="mini-stat-info">
          <span class="mini-stat-value">{{ statsData.rejected }}</span>
          <span class="mini-stat-label">已驳回</span>
        </div>
      </div>
    </div>

    <!-- Submit Appeal Button -->
    <div class="action-bar animate__animated animate__fadeInUp">
      <el-button
        v-permission="'READER_APPEAL_CREATE'"
        class="submit-btn"
        type="primary"
        round
        @click="dialogVisible = true"
      >
        <span class="btn-icon">📝</span> 提交新申诉
      </el-button>
    </div>

    <!-- Appeal History List -->
    <div class="appeals-list" v-loading="loading">
      <transition-group name="card-list" tag="div" class="appeal-grid">
        <div
          v-for="(appeal, index) in appeals"
          :key="appeal.id"
          class="appeal-card animate__animated animate__fadeInUp"
          :style="{ animationDelay: `${index * 0.08}s` }"
        >
          <div class="card-top">
            <div class="card-tags">
              <el-tag class="type-tag" effect="plain" round size="small">
                {{ typeLabel(appeal.type) }}
              </el-tag>
              <el-tag
                :type="statusType(appeal.status)"
                effect="light"
                round
                size="small"
                class="status-tag"
              >
                {{ statusLabel(appeal.status) }}
              </el-tag>
            </div>
            <span class="card-time">{{ appeal.createTime }}</span>
          </div>

          <div class="card-reason">
            <p>{{ appeal.reason }}</p>
          </div>

          <div v-if="appeal.evidence" class="card-evidence">
            <span class="evidence-label">📎 证据材料：</span>
            <span class="evidence-text">{{ appeal.evidence }}</span>
          </div>

          <div v-if="appeal.feedback" class="card-feedback">
            <div class="feedback-header">💬 管理员反馈</div>
            <p class="feedback-content">{{ appeal.feedback }}</p>
            <div class="feedback-meta" v-if="appeal.reviewerName || appeal.reviewTime">
              <span v-if="appeal.reviewerName" class="reviewer">审核人：{{ appeal.reviewerName }}</span>
              <span v-if="appeal.reviewTime" class="review-time">{{ appeal.reviewTime }}</span>
            </div>
          </div>
        </div>
      </transition-group>

      <el-empty
        v-if="!loading && appeals.length === 0"
        description="暂无申诉记录"
        class="empty-state"
      >
        <el-button v-permission="'READER_APPEAL_CREATE'" type="primary" round @click="dialogVisible = true">提交第一条申诉</el-button>
      </el-empty>

      <!-- Pagination -->
      <div class="pagination-wrapper" v-if="total > pageSize">
        <el-pagination
          v-model:current-page="currentPage"
          :page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, prev, pager, next"
          background
          @current-change="fetchAppeals"
        />
      </div>
    </div>

    <!-- Submit Appeal Dialog -->
    <el-dialog
      v-model="dialogVisible"
      title="提交申诉"
      width="520px"
      class="appeal-dialog"
      destroy-on-close
    >
      <div class="dialog-form">
        <div class="form-item">
          <label class="form-label">申诉类型</label>
          <el-select
            v-model="form.type"
            placeholder="请选择申诉类型"
            class="form-select"
            style="width: 100%"
          >
            <el-option label="账号暂停" value="suspension" />
            <el-option label="逾期处罚" value="overdue_penalty" />
            <el-option label="其他" value="other" />
          </el-select>
        </div>
        <div class="form-item">
          <label class="form-label">申诉原因</label>
          <el-input
            v-model="form.reason"
            type="textarea"
            :rows="5"
            placeholder="请详细描述您的申诉原因..."
            maxlength="500"
            show-word-limit
          />
        </div>
        <div class="form-item">
          <label class="form-label">证据材料 <span class="form-optional">（选填）</span></label>
          <el-input
            v-model="form.evidence"
            type="textarea"
            :rows="3"
            placeholder="请提供相关证据描述或链接，如截图链接、借阅凭证编号等..."
            maxlength="500"
            show-word-limit
          />
        </div>
      </div>
      <template #footer>
        <el-button round @click="dialogVisible = false">取消</el-button>
        <el-button
          type="primary"
          round
          :loading="submitLoading"
          @click="handleSubmit"
          class="confirm-btn"
        >
          提交申诉
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { submitAppeal, getMyAppeals } from '@/api'

const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)

const appeals = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const statsData = reactive({
  total: 0,
  pending: 0,
  approved: 0,
  rejected: 0
})

const form = reactive({
  type: '',
  reason: '',
  evidence: ''
})

const typeLabel = (type) => {
  const map = { suspension: '账号暂停', overdue_penalty: '逾期处罚', other: '其他' }
  return map[type] || type
}

const statusType = (status) => {
  const map = { pending: 'warning', approved: 'success', rejected: 'danger' }
  return map[status] || 'info'
}

const statusLabel = (status) => {
  const map = { pending: '待审核', approved: '已通过', rejected: '已驳回' }
  return map[status] || status
}

const fetchAppeals = async () => {
  loading.value = true
  try {
    const res = await getMyAppeals({ page: currentPage.value, size: pageSize.value })
    appeals.value = res.data?.records || []
    total.value = res.data?.total || 0
    statsData.total = res.data?.totalCount || total.value
    statsData.pending = res.data?.pendingCount || 0
    statsData.approved = res.data?.approvedCount || 0
    statsData.rejected = res.data?.rejectedCount || 0
  } catch (e) {
    console.error('获取申诉记录失败:', e)
    appeals.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const handleSubmit = async () => {
  if (!form.type) {
    ElMessage.warning('请选择申诉类型')
    return
  }
  if (!form.reason || form.reason.trim().length < 10) {
    ElMessage.warning('申诉原因不能少于10个字')
    return
  }
  submitLoading.value = true
  try {
    await submitAppeal({ type: form.type, reason: form.reason.trim(), evidence: form.evidence.trim() || undefined })
    ElMessage.success('申诉提交成功，请耐心等待审核')
    dialogVisible.value = false
    form.type = ''
    form.reason = ''
    form.evidence = ''
    currentPage.value = 1
    fetchAppeals()
  } catch (e) {
    console.error('提交申诉失败:', e)
  } finally {
    submitLoading.value = false
  }
}

onMounted(() => {
  fetchAppeals()
})
</script>

<style scoped>
.appeals-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
  width: 100%;
  max-width: 100%;
}

/* Stat Row */
.stat-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.mini-stat {
  background: white;
  border-radius: var(--radius-md);
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 14px;
  box-shadow: var(--shadow-soft);
  border-left: 4px solid var(--accent);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
}

.mini-stat::after {
  content: '';
  position: absolute;
  top: 0;
  right: 0;
  width: 60px;
  height: 60px;
  background: var(--accent);
  opacity: 0.05;
  border-radius: 50%;
  transform: translate(20px, -20px);
  transition: all 0.3s;
}

.mini-stat:hover {
  transform: translateY(-4px) scale(1.02);
  box-shadow: var(--shadow-hover);
}

.mini-stat:hover::after {
  opacity: 0.1;
  transform: translate(10px, -10px) scale(1.3);
}

.mini-stat-icon {
  font-size: 28px;
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
  background: linear-gradient(135deg, color-mix(in srgb, var(--accent) 15%, white), color-mix(in srgb, var(--accent) 8%, white));
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.mini-stat:hover .mini-stat-icon {
  transform: scale(1.15) rotate(5deg);
}

.mini-stat-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.mini-stat-value {
  font-size: 24px;
  font-weight: 700;
  color: var(--text-primary);
}

.mini-stat-label {
  font-size: 13px;
  color: var(--text-secondary);
}

/* Action Bar */
.action-bar {
  display: flex;
  justify-content: flex-end;
}

.submit-btn {
  background: linear-gradient(135deg, var(--btn-edit-from), var(--btn-edit-to));
  border: none;
  padding: 12px 28px;
  font-size: 15px;
  font-weight: 600;
  box-shadow: 0 4px 14px rgba(149, 125, 173, 0.3);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.submit-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(149, 125, 173, 0.45);
}

.submit-btn .btn-icon {
  margin-right: 6px;
}

/* Appeal List */
.appeals-list {
  min-height: 200px;
}

.appeal-grid {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.appeal-card {
  background: white;
  border-radius: var(--radius-md);
  padding: 22px 24px;
  box-shadow: var(--shadow-soft);
  border: 1px solid #f5f5f5;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.appeal-card:hover {
  transform: translateY(-3px);
  box-shadow: var(--shadow-hover);
  border-color: color-mix(in srgb, var(--purple) 20%, white);
}

.card-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.card-tags {
  display: flex;
  gap: 8px;
  align-items: center;
}

.type-tag {
  border-radius: 12px !important;
  font-weight: 500;
}

.status-tag {
  border-radius: 12px !important;
  font-weight: 500;
}

.card-time {
  font-size: 12px;
  color: var(--text-secondary);
}

.card-reason {
  margin-bottom: 14px;
}

.card-reason p {
  margin: 0;
  color: var(--text-primary);
  line-height: 1.7;
  font-size: 14px;
  word-break: break-word;
}

/* Evidence Section */
.card-evidence {
  background: linear-gradient(135deg, #FFF8E1, #FFFDF5);
  padding: 10px 14px;
  border-radius: var(--radius-sm);
  margin-bottom: 14px;
  border-left: 3px solid #F5A623;
  font-size: 13px;
}

.evidence-label {
  font-weight: 600;
  color: #F5A623;
}

.evidence-text {
  color: var(--text-primary);
  word-break: break-word;
}

.form-optional {
  font-weight: 400;
  font-size: 12px;
  color: var(--text-secondary);
}

/* Feedback Section */
.card-feedback {
  background: linear-gradient(135deg, #f8f9fa, #eef0f8);
  padding: 14px 18px;
  border-radius: var(--radius-sm);
  border-left: 3px solid var(--purple);
}

.feedback-header {
  font-size: 12px;
  font-weight: 600;
  color: var(--purple);
  margin-bottom: 6px;
}

.feedback-content {
  margin: 0 0 8px 0;
  font-size: 13px;
  color: var(--text-primary);
  line-height: 1.6;
}

.feedback-meta {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: var(--text-secondary);
}

.feedback-meta .reviewer {
  font-weight: 500;
}

/* Empty State */
.empty-state {
  padding: 40px 0;
}

/* Pagination */
.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}

/* Dialog Styles */
.appeal-dialog :deep(.el-dialog__header) {
  background: linear-gradient(135deg, var(--purple), var(--pink));
  padding: 18px 24px;
}

.appeal-dialog :deep(.el-dialog__title) {
  color: #fff;
  font-weight: 600;
  font-size: 17px;
}

.appeal-dialog :deep(.el-dialog__headerbtn .el-dialog__close) {
  color: #fff;
}

.appeal-dialog :deep(.el-dialog__body) {
  padding: 28px 24px;
}

.dialog-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.form-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-label {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}

.form-select :deep(.el-input__wrapper) {
  border-radius: var(--radius-sm);
}

.confirm-btn {
  background: linear-gradient(135deg, var(--btn-edit-from), var(--btn-edit-to));
  border: none;
}

/* Transitions */
.card-list-enter-active,
.card-list-leave-active {
  transition: all 0.4s ease;
}

.card-list-enter-from {
  opacity: 0;
  transform: translateY(20px);
}

.card-list-leave-to {
  opacity: 0;
  transform: translateX(-20px);
}

/* Responsive Breakpoints */
@media (max-width: 1200px) {
  .stat-row {
    grid-template-columns: repeat(4, 1fr);
    gap: 12px;
  }

  .mini-stat {
    padding: 16px;
  }

  .mini-stat-value {
    font-size: 20px;
  }
}

@media (max-width: 768px) {
  .stat-row {
    grid-template-columns: repeat(2, 1fr);
    gap: 10px;
  }

  .mini-stat {
    padding: 14px;
  }

  .mini-stat-icon {
    width: 40px;
    height: 40px;
    font-size: 22px;
  }

  .mini-stat-value {
    font-size: 18px;
  }

  .appeal-card {
    padding: 16px;
  }

  .card-top {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }

  .action-bar {
    justify-content: stretch;
  }

  .submit-btn {
    width: 100%;
  }

  .appeal-dialog :deep(.el-dialog) {
    width: 90% !important;
  }
}
</style>
