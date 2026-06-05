<template>
  <div class="reviews-page">
    <el-card class="header-card animate__animated animate__fadeInDown">
      <div class="page-header">
        <div class="header-info">
          <h2>💬 评价管理</h2>
          <p>查看、审核与管理读者的图书评价</p>
        </div>
        <div class="header-stats">
          <div class="stat-item pending">
            <span class="stat-num">{{ pendingCount }}</span>
            <span class="stat-label">待审核</span>
          </div>
          <div class="stat-item approved">
            <span class="stat-num">{{ approvedCount }}</span>
            <span class="stat-label">已通过</span>
          </div>
        </div>
      </div>
    </el-card>

    <el-card class="filter-card animate__animated animate__fadeInUp">
      <div class="filter-bar">
        <el-select v-model="filterStatus" placeholder="全部状态" clearable @change="handleFilter">
          <el-option label="待审核" value="pending" />
          <el-option label="已通过" value="approved" />
          <el-option label="已拒绝" value="rejected" />
        </el-select>
        <el-button v-permission="'REVIEW_READ'" @click="handleFilter">
          <el-icon><Search /></el-icon>
          筛选
        </el-button>
        <el-button v-permission="'REVIEW_BATCH_DELETE'" type="danger" plain @click="handleBatchDelete">
          <el-icon><Delete /></el-icon>
          批量删除
        </el-button>
      </div>
    </el-card>

    <el-card class="table-card" v-loading="loading">
      <transition-group name="card-list" tag="div">
        <div v-for="review in reviews" :key="review.id" class="review-card">
          <div class="review-header">
            <div class="review-info">
              <span class="reviewer-name">👤 {{ review.readerName }}</span>
              <span class="review-book">📖 {{ review.bookTitle }}</span>
            </div>
            <el-tag
              :type="review.status === 'approved' ? 'success' : review.status === 'rejected' ? 'danger' : 'warning'"
              size="small"
              class="status-tag"
            >
              {{ review.status === 'approved' ? '已通过' : review.status === 'rejected' ? '已拒绝' : '待审核' }}
            </el-tag>
          </div>

          <div class="review-rating">
            <el-rate v-model="review.rating" disabled :colors="rateColors" />
            <span class="rating-text">{{ review.rating }}分</span>
          </div>

          <p class="review-content" v-if="review.content">{{ review.content }}</p>
          <p class="review-content empty" v-else>（无文字评价）</p>

          <div v-if="review.adminReply" class="admin-reply">
            <span class="reply-label">管理员回复：</span>
            <span class="reply-text">{{ review.adminReply }}</span>
          </div>

          <div class="review-footer">
            <span class="review-time">{{ review.createTime }}</span>
            <div class="review-actions">
              <el-button
                v-if="review.status === 'pending'"
                v-permission="'REVIEW_UPDATE'"
                type="success"
                size="small"
                plain
                @click="handleApprove(review)"
              >
                通过
              </el-button>
              <el-button
                v-if="review.status === 'pending'"
                v-permission="'REVIEW_UPDATE'"
                type="danger"
                size="small"
                plain
                @click="handleReject(review)"
              >
                拒绝
              </el-button>
              <el-button
                v-permission="'REVIEW_UPDATE'"
                type="primary"
                size="small"
                plain
                @click="handleReply(review)"
              >
                回复
              </el-button>
              <el-button
                v-permission="'REVIEW_DELETE'"
                type="danger"
                size="small"
                text
                @click="handleDelete(review)"
              >
                删除
              </el-button>
            </div>
          </div>
        </div>
      </transition-group>

      <el-empty v-if="!loading && reviews.length === 0" description="暂无评价" />

      <div class="pagination-wrapper" v-if="total > 0">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          background
          @current-change="fetchReviews"
          @size-change="fetchReviews"
        />
      </div>
    </el-card>

    <!-- 回复弹窗 -->
    <el-dialog v-model="replyVisible" title="回复评价" width="500px" class="reply-dialog">
      <div class="reply-info" v-if="currentReview">
        <p><strong>{{ currentReview.readerName }}</strong> 对 <strong>《{{ currentReview.bookTitle }}》</strong> 的评价：</p>
        <p class="reply-original">{{ currentReview.content || '（无文字评价）' }}</p>
      </div>
      <el-input
        v-model="replyContent"
        type="textarea"
        :rows="4"
        placeholder="请输入回复内容..."
        maxlength="500"
        show-word-limit
      />
      <template #footer>
        <el-button @click="replyVisible = false">取消</el-button>
        <el-button v-permission="'REVIEW_UPDATE'" type="primary" :loading="replyLoading" @click="submitReply">发送回复</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAdminReviews, approveReview, rejectReview, replyReview, adminDeleteReview } from '@/api'
import { usePermission } from '@/composables/usePermission'
const { checkWithFeedback } = usePermission()

const loading = ref(false)
const reviews = ref([])
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const filterStatus = ref('')

const replyVisible = ref(false)
const replyContent = ref('')
const replyLoading = ref(false)
const currentReview = ref(null)

const rateColors = ['#FFB3BA', '#FFEAA7', '#B5EAD7']

const pendingCount = computed(() => reviews.value.filter(r => r.status === 'pending').length)
const approvedCount = computed(() => reviews.value.filter(r => r.status === 'approved').length)

const fetchReviews = async () => {
  loading.value = true
  try {
    const params = { page: page.value, size: pageSize.value }
    if (filterStatus.value) params.status = filterStatus.value
    const res = await getAdminReviews(params)
    reviews.value = res.data.records || []
    total.value = res.data.total || 0
  } catch (e) {} finally {
    loading.value = false
  }
}

const handleFilter = () => {
  page.value = 1
  fetchReviews()
}

const handleApprove = async (review) => {
  if (!checkWithFeedback('REVIEW_UPDATE')) return
  try {
    await approveReview(review.id)
    ElMessage.success('已通过审核')
    fetchReviews()
  } catch (e) {}
}

const handleReject = (review) => {
  if (!checkWithFeedback('REVIEW_UPDATE')) return
  ElMessageBox.confirm('确定拒绝该评价吗？', '拒绝确认', { type: 'warning' })
    .then(async () => {
      try {
        await rejectReview(review.id)
        ElMessage.success('已拒绝')
        fetchReviews()
      } catch (e) {}
    }).catch(() => {})
}

const handleReply = (review) => {
  currentReview.value = review
  replyContent.value = review.adminReply || ''
  replyVisible.value = true
}

const submitReply = async () => {
  if (!checkWithFeedback('REVIEW_UPDATE')) return
  if (!replyContent.value.trim()) {
    ElMessage.warning('请输入回复内容')
    return
  }
  replyLoading.value = true
  try {
    await replyReview(currentReview.value.id, { reply: replyContent.value })
    ElMessage.success('回复成功')
    replyVisible.value = false
    fetchReviews()
  } catch (e) {} finally {
    replyLoading.value = false
  }
}

const handleDelete = (review) => {
  if (!checkWithFeedback('REVIEW_DELETE')) return
  ElMessageBox.confirm('确定删除该评价吗？此操作不可恢复。', '删除确认', {
    type: 'warning', confirmButtonText: '确认删除'
  }).then(async () => {
    try {
      await adminDeleteReview(review.id)
      ElMessage.success('已删除')
      fetchReviews()
    } catch (e) {}
  }).catch(() => {})
}

const handleBatchDelete = () => {
  if (!checkWithFeedback('REVIEW_BATCH_DELETE')) return
  const rejectedReviews = reviews.value.filter(r => r.status === 'rejected')
  if (rejectedReviews.length === 0) {
    ElMessage.warning('没有可批量删除的评价（仅支持删除已拒绝的评价）')
    return
  }
  ElMessageBox.confirm(
    `确定批量删除 ${rejectedReviews.length} 条已拒绝的评价吗？此操作不可恢复。`,
    '批量删除确认',
    { type: 'warning', confirmButtonText: '确认删除' }
  ).then(async () => {
    try {
      const ids = rejectedReviews.map(r => r.id)
      await Promise.all(ids.map(id => adminDeleteReview(id)))
      ElMessage.success(`已删除 ${ids.length} 条评价`)
      fetchReviews()
    } catch (e) {}
  }).catch(() => {})
}

onMounted(() => {
  fetchReviews()
})
</script>

<style scoped>
.reviews-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.header-card {
  background: linear-gradient(135deg, #FFD1D6, #E0E5F5);
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-info h2 {
  margin: 0 0 4px 0;
  font-size: 22px;
}

.header-info p {
  margin: 0;
  color: var(--text-secondary);
  font-size: 14px;
}

.header-stats {
  display: flex;
  gap: 20px;
}

.stat-item {
  text-align: center;
  padding: 8px 16px;
  border-radius: 12px;
  background: rgba(255,255,255,0.7);
}

.stat-item.pending { border: 2px solid #FFEAA7; }
.stat-item.approved { border: 2px solid #B5EAD7; }

.stat-num {
  display: block;
  font-size: 20px;
  font-weight: 700;
  color: var(--text-primary);
}

.stat-label {
  font-size: 12px;
  color: var(--text-secondary);
}

.filter-card { background: white; }

.filter-bar {
  display: flex;
  gap: 12px;
  align-items: center;
}

.table-card { background: white; }

.review-card {
  padding: 20px;
  margin-bottom: 16px;
  border: 1px solid #F0F0F0;
  border-radius: 16px;
  transition: all 0.3s ease;
}

.review-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(0,0,0,0.06);
  border-color: var(--purple-light);
}

.review-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.review-info {
  display: flex;
  gap: 16px;
  align-items: center;
}

.reviewer-name {
  font-weight: 600;
  color: var(--text-primary);
}

.review-book {
  color: var(--text-secondary);
  font-size: 13px;
}

.status-tag {
  border-radius: 12px !important;
}

.review-rating {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}

.rating-text {
  font-size: 14px;
  color: var(--text-secondary);
}

.review-content {
  margin: 0 0 12px 0;
  color: var(--text-primary);
  line-height: 1.6;
  font-size: 14px;
}

.review-content.empty {
  color: #CCC;
  font-style: italic;
}

.admin-reply {
  background: #F8F9FA;
  padding: 10px 14px;
  border-radius: 10px;
  margin-bottom: 12px;
  border-left: 3px solid var(--purple);
}

.reply-label {
  font-weight: 600;
  color: var(--purple);
  font-size: 13px;
}

.reply-text {
  color: var(--text-primary);
  font-size: 13px;
}

.review-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.review-time {
  font-size: 12px;
  color: #999;
}

.review-actions {
  display: flex;
  gap: 8px;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid #F5F5F5;
}

.reply-dialog :deep(.el-dialog__header) {
  background: linear-gradient(135deg, var(--purple-light), var(--blue-light));
  padding: 16px 20px;
}

.reply-info {
  margin-bottom: 16px;
}

.reply-original {
  background: #F8F9FA;
  padding: 10px 14px;
  border-radius: 8px;
  color: var(--text-secondary);
  font-size: 13px;
}

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
</style>
