<template>
  <div class="my-reviews-page">
    <el-card class="header-card animate__animated animate__fadeInDown">
      <div class="page-header">
        <h2>✍️ 我的评价</h2>
        <p>查看和管理我对图书的评分与评价</p>
      </div>
    </el-card>

    <div class="reviews-list" v-loading="loading">
      <transition-group name="card-list" tag="div" class="review-grid">
        <div v-for="review in reviews" :key="review.id" class="review-card animate__animated animate__fadeInUp">
          <div class="card-header">
            <div class="book-info">
              <span class="book-title">📖 {{ review.bookTitle }}</span>
              <el-tag
                :type="review.status === 'approved' ? 'success' : review.status === 'rejected' ? 'danger' : 'warning'"
                size="small"
                class="status-tag"
              >
                {{ statusText(review.status) }}
              </el-tag>
            </div>
          </div>

          <div class="card-rating">
            <el-rate v-model="review.rating" disabled :colors="rateColors" />
          </div>

          <p class="card-content" v-if="review.content">{{ review.content }}</p>

          <div v-if="review.adminReply" class="admin-reply">
            <div class="reply-header">💬 管理员回复</div>
            <p class="reply-content">{{ review.adminReply }}</p>
          </div>

          <div class="card-footer">
            <span class="time-text">{{ review.createTime }}</span>
            <div class="card-actions">
              <el-button
                v-if="review.status !== 'approved'"
                type="primary"
                size="small"
                plain
                @click="handleEdit(review)"
              >
                修改
              </el-button>
              <el-button
                type="danger"
                size="small"
                plain
                @click="handleDelete(review)"
              >
                删除
              </el-button>
            </div>
          </div>
        </div>
      </transition-group>

      <el-empty v-if="!loading && reviews.length === 0" description="还没有评价哦，去浏览图书写下你的第一条评价吧！">
        <el-button type="primary" @click="$router.push('/reader/books')">去浏览图书</el-button>
      </el-empty>

      <div class="pagination-wrapper" v-if="total > pageSize">
        <el-pagination
          v-model:current-page="page"
          :page-size="pageSize"
          :total="total"
          layout="prev, pager, next"
          background
          @current-change="fetchReviews"
        />
      </div>
    </div>

    <!-- 编辑弹窗 -->
    <el-dialog v-model="editVisible" title="修改评价" width="500px" class="edit-dialog">
      <div class="edit-form">
        <div class="edit-book">📖 {{ editForm.bookTitle }}</div>
        <div class="edit-rating">
          <span class="rating-label">评分：</span>
          <el-rate v-model="editForm.rating" :colors="rateColors" show-text :texts="['很差', '较差', '一般', '推荐', '强烈推荐']" />
        </div>
        <el-input
          v-model="editForm.content"
          type="textarea"
          :rows="4"
          placeholder="分享你的阅读感受..."
          maxlength="500"
          show-word-limit
        />
      </div>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="editLoading" @click="submitEdit">保存修改</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getMyReviews, updateReview, deleteMyReview } from '@/api'

const loading = ref(false)
const reviews = ref([])
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)

const editVisible = ref(false)
const editLoading = ref(false)
const editForm = reactive({ id: null, bookId: null, bookTitle: '', rating: 5, content: '' })

const rateColors = ['#FFB3BA', '#FFEAA7', '#B5EAD7']

const statusText = (status) => {
  const map = { pending: '审核中', approved: '已通过', rejected: '未通过' }
  return map[status] || status
}

const fetchReviews = async () => {
  loading.value = true
  try {
    const res = await getMyReviews({ page: page.value, size: pageSize.value })
    reviews.value = res.data.records || []
    total.value = res.data.total || 0
  } catch (e) {} finally {
    loading.value = false
  }
}

const handleEdit = (review) => {
  Object.assign(editForm, {
    id: review.id,
    bookId: review.bookId,
    bookTitle: review.bookTitle,
    rating: review.rating,
    content: review.content || ''
  })
  editVisible.value = true
}

const submitEdit = async () => {
  if (!editForm.rating) {
    ElMessage.warning('请选择评分')
    return
  }
  editLoading.value = true
  try {
    await updateReview(editForm.id, {
      bookId: editForm.bookId,
      rating: editForm.rating,
      content: editForm.content
    })
    ElMessage.success('修改成功，等待审核')
    editVisible.value = false
    fetchReviews()
  } catch (e) {} finally {
    editLoading.value = false
  }
}

const handleDelete = (review) => {
  ElMessageBox.confirm('确定删除这条评价吗？', '删除确认', {
    type: 'warning', confirmButtonText: '确认删除'
  }).then(async () => {
    try {
      await deleteMyReview(review.id)
      ElMessage.success('已删除')
      fetchReviews()
    } catch (e) {}
  }).catch(() => {})
}

onMounted(() => {
  fetchReviews()
})
</script>

<style scoped>
.my-reviews-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.header-card {
  background: linear-gradient(135deg, #FFEAA7, #FFD1D6);
}

.page-header h2 {
  margin: 0 0 4px 0;
  font-size: 22px;
}

.page-header p {
  margin: 0;
  color: var(--text-secondary);
  font-size: 14px;
}

.review-grid {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.review-card {
  background: white;
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.04);
  border: 1px solid #F5F5F5;
  transition: all 0.3s ease;
}

.review-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 24px rgba(0,0,0,0.08);
  border-color: var(--pink-light);
}

.card-header {
  margin-bottom: 10px;
}

.book-info {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.book-title {
  font-weight: 600;
  font-size: 16px;
  color: var(--text-primary);
}

.status-tag {
  border-radius: 12px !important;
}

.card-rating {
  margin-bottom: 10px;
}

.card-content {
  margin: 0 0 12px 0;
  color: var(--text-primary);
  line-height: 1.6;
  font-size: 14px;
}

.admin-reply {
  background: linear-gradient(135deg, #F8F9FA, #E0E5F5);
  padding: 12px 16px;
  border-radius: 12px;
  margin-bottom: 12px;
}

.reply-header {
  font-size: 12px;
  font-weight: 600;
  color: var(--purple);
  margin-bottom: 4px;
}

.reply-content {
  margin: 0;
  font-size: 13px;
  color: var(--text-primary);
  line-height: 1.5;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.time-text {
  font-size: 12px;
  color: #999;
}

.card-actions {
  display: flex;
  gap: 8px;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

.edit-dialog :deep(.el-dialog__header) {
  background: linear-gradient(135deg, var(--yellow-warm), var(--pink-light));
  padding: 16px 20px;
}

.edit-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.edit-book {
  font-weight: 600;
  font-size: 16px;
  color: var(--text-primary);
}

.edit-rating {
  display: flex;
  align-items: center;
  gap: 12px;
}

.rating-label {
  font-size: 14px;
  color: var(--text-secondary);
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
