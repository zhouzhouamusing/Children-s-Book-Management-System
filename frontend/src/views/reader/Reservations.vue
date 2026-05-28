<template>
  <div class="reservations-page">
    <div class="page-header animate__animated animate__fadeInDown">
      <h2 class="section-title">我的预约</h2>
      <el-button type="primary" round @click="$router.push('/reader/books')">
        <el-icon><Plus /></el-icon>
        新建预约
      </el-button>
    </div>

    <div class="filter-section animate__animated animate__fadeInUp">
      <el-radio-group v-model="statusFilter" @change="fetchReservations" class="status-filter">
        <el-radio-button value="all">全部</el-radio-button>
        <el-radio-button value="pending">待取书</el-radio-button>
        <el-radio-button value="fulfilled">已取书</el-radio-button>
        <el-radio-button value="cancelled">已取消</el-radio-button>
        <el-radio-button value="expired">已过期</el-radio-button>
      </el-radio-group>
    </div>

    <div v-loading="loading" class="reservation-grid">
      <TransitionGroup name="card-list" tag="div" class="card-container">
        <div
          v-for="(item, index) in reservations"
          :key="item.id"
          class="reservation-card animate__animated animate__fadeInUp"
          :style="{ animationDelay: `${index * 0.05}s` }"
        >
          <div class="card-header">
            <div class="book-emoji">📚</div>
            <el-tag
              :type="statusTagType(item.status)"
              effect="light"
              round
              size="small"
            >
              {{ statusLabelMap[item.status] }}
            </el-tag>
          </div>
          <h3 class="book-title">{{ item.bookTitle }}</h3>
          <div class="card-info">
            <div class="info-row">
              <el-icon><Calendar /></el-icon>
              <span>预约日期：{{ formatDate(item.reserveDate) }}</span>
            </div>
            <div class="info-row">
              <el-icon><Timer /></el-icon>
              <span>过期日期：{{ formatDate(item.expireDate) }}</span>
            </div>
          </div>
          <div class="card-actions" v-if="item.status === 'pending'">
            <el-button
              type="danger"
              text
              size="small"
              @click="handleCancel(item)"
            >
              取消预约
            </el-button>
          </div>
          <div class="expire-progress" v-if="item.status === 'pending'">
            <el-progress
              :percentage="getExpireProgress(item)"
              :color="getExpireColor(item)"
              :show-text="false"
              :stroke-width="4"
            />
            <span class="expire-text">{{ getExpireText(item) }}</span>
          </div>
        </div>
      </TransitionGroup>
    </div>

    <div v-if="!loading && reservations.length === 0" class="empty-state animate__animated animate__fadeIn">
      <div class="empty-icon">📋</div>
      <p>暂无预约记录</p>
      <el-button type="primary" round @click="$router.push('/reader/books')">去浏览图书</el-button>
    </div>

    <div class="pagination-wrapper" v-if="total > pageSize">
      <el-pagination
        v-model:current-page="currentPage"
        :total="total"
        :page-size="pageSize"
        layout="prev, pager, next"
        @current-change="fetchReservations"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getMyReservations, cancelReservation } from '@/api'

const loading = ref(false)
const reservations = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(12)
const statusFilter = ref('all')

const statusLabelMap = {
  pending: '待取书',
  fulfilled: '已取书',
  cancelled: '已取消',
  expired: '已过期'
}

const statusTagType = (status) => {
  const map = { pending: 'warning', fulfilled: 'success', cancelled: 'info', expired: 'danger' }
  return map[status] || 'info'
}

const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  return dateStr.substring(0, 10)
}

const getExpireProgress = (item) => {
  const now = new Date()
  const reserve = new Date(item.reserveDate)
  const expire = new Date(item.expireDate)
  const totalMs = expire - reserve
  const elapsedMs = now - reserve
  return Math.min(100, Math.max(0, (elapsedMs / totalMs) * 100))
}

const getExpireColor = (item) => {
  const progress = getExpireProgress(item)
  if (progress > 80) return '#FFB3BA'
  if (progress > 50) return '#FFEAA7'
  return '#B5EAD7'
}

const getExpireText = (item) => {
  const now = new Date()
  const expire = new Date(item.expireDate)
  const diff = expire - now
  if (diff <= 0) return '已过期'
  const hours = Math.floor(diff / (1000 * 60 * 60))
  if (hours < 24) return `剩余${hours}小时`
  return `剩余${Math.floor(hours / 24)}天`
}

const fetchReservations = async () => {
  loading.value = true
  try {
    const res = await getMyReservations({
      page: currentPage.value,
      size: pageSize.value,
      status: statusFilter.value === 'all' ? '' : statusFilter.value
    })
    reservations.value = res.data.records
    total.value = res.data.total
  } catch (e) {
    console.error('获取预约列表失败:', e)
  } finally {
    loading.value = false
  }
}

const handleCancel = (item) => {
  ElMessageBox.confirm(`确定要取消预约《${item.bookTitle}》吗？`, '取消预约', {
    confirmButtonText: '确定取消',
    cancelButtonText: '再想想',
    type: 'warning'
  }).then(async () => {
    try {
      await cancelReservation(item.id)
      ElMessage.success('预约已取消')
      fetchReservations()
    } catch (e) {
      console.error('取消预约失败:', e)
    }
  }).catch(() => {})
}

onMounted(fetchReservations)
</script>

<style scoped>
.reservations-page {
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.section-title {
  font-size: 20px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0;
}

.filter-section {
  margin-bottom: 24px;
}

.status-filter :deep(.el-radio-button__inner) {
  border-radius: 20px !important;
  padding: 8px 16px;
  border: none !important;
  box-shadow: none !important;
}

.card-container {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
}

.reservation-card {
  background: white;
  border-radius: var(--radius-md);
  padding: 20px;
  box-shadow: var(--shadow-soft);
  transition: all 0.3s;
  position: relative;
  overflow: hidden;
}

.reservation-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-hover);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.book-emoji {
  font-size: 28px;
}

.book-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 12px;
  line-height: 1.4;
}

.card-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 12px;
}

.info-row {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--text-secondary);
}

.info-row .el-icon {
  color: var(--purple-light);
}

.card-actions {
  border-top: 1px solid #f5f5f5;
  padding-top: 12px;
  display: flex;
  justify-content: flex-end;
}

.expire-progress {
  margin-top: 12px;
}

.expire-text {
  font-size: 11px;
  color: var(--text-secondary);
  margin-top: 4px;
  display: block;
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

.pagination-wrapper {
  margin-top: 24px;
  display: flex;
  justify-content: center;
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
  transform: scale(0.9);
}
</style>
