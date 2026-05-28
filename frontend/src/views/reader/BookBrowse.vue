<template>
  <div class="book-browse-page">
    <div class="search-section animate__animated animate__fadeInDown">
      <div class="search-bar">
        <el-input
          v-model="keyword"
          placeholder="搜索图书名称或作者..."
          prefix-icon="Search"
          size="large"
          clearable
          @input="debouncedSearch"
          class="search-input"
        />
        <el-select
          v-model="categoryFilter"
          placeholder="所有分类"
          clearable
          size="large"
          @change="fetchBooks"
          class="category-select"
        >
          <el-option
            v-for="cat in categories"
            :key="cat"
            :label="cat"
            :value="cat"
          />
        </el-select>
      </div>
    </div>

    <div v-loading="loading" class="book-grid">
      <TransitionGroup name="card-list" tag="div" class="grid-container">
        <div
          v-for="(book, index) in books"
          :key="book.id"
          class="book-card animate__animated animate__fadeInUp"
          :style="{ animationDelay: `${index * 0.05}s` }"
        >
          <div class="book-cover">
            <span class="cover-emoji">📖</span>
            <div class="stock-badge" :class="{ 'low-stock': book.stock <= 3 }">
              {{ book.stock > 0 ? `余${book.stock}册` : '已借完' }}
            </div>
          </div>
          <div class="book-info">
            <h3 class="book-title">{{ book.title }}</h3>
            <p class="book-author">{{ book.author }}</p>
            <div class="book-meta">
              <el-tag size="small" effect="plain" round class="category-tag">
                {{ book.category }}
              </el-tag>
              <span class="age-range" v-if="book.ageRange">{{ book.ageRange }}</span>
            </div>
            <p class="book-desc" v-if="book.description">{{ book.description }}</p>
          </div>
          <div class="book-action">
            <el-button
              type="primary"
              round
              :disabled="book.stock <= 0 || reservedBookIds.has(book.id)"
              @click="handleReserve(book)"
              :loading="reservingId === book.id"
              class="reserve-btn"
            >
              <template v-if="reservedBookIds.has(book.id)">已预约</template>
              <template v-else-if="book.stock <= 0">暂无库存</template>
              <template v-else>预约此书</template>
            </el-button>
          </div>
        </div>
      </TransitionGroup>
    </div>

    <div v-if="!loading && books.length === 0" class="empty-state animate__animated animate__fadeIn">
      <div class="empty-icon">🔍</div>
      <p>未找到相关图书</p>
    </div>

    <div class="pagination-wrapper" v-if="total > pageSize">
      <el-pagination
        v-model:current-page="currentPage"
        :total="total"
        :page-size="pageSize"
        layout="prev, pager, next"
        @current-change="fetchBooks"
      />
    </div>

    <el-dialog
      v-model="showSuccess"
      width="360px"
      :show-close="false"
      class="success-dialog"
      center
    >
      <div class="success-content">
        <div class="success-icon animate__animated animate__bounceIn">✅</div>
        <h3>预约成功！</h3>
        <p>《{{ lastReservedBook }}》已加入您的预约列表</p>
        <p class="hint">请在3天内到馆取书</p>
        <div class="success-actions">
          <el-button round @click="showSuccess = false">继续浏览</el-button>
          <el-button type="primary" round @click="$router.push('/reader/reservations')">查看预约</el-button>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { browseBooks, createReservation, getMyReservations, getAllCategories } from '@/api'

const loading = ref(false)
const books = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(12)
const keyword = ref('')
const categoryFilter = ref('')
const categories = ref([])
const reservedBookIds = ref(new Set())
const reservingId = ref(null)
const showSuccess = ref(false)
const lastReservedBook = ref('')

let searchTimer = null
const debouncedSearch = () => {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    currentPage.value = 1
    fetchBooks()
  }, 300)
}

const fetchBooks = async () => {
  loading.value = true
  try {
    const res = await browseBooks({
      page: currentPage.value,
      size: pageSize.value,
      keyword: keyword.value,
      category: categoryFilter.value
    })
    books.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch (e) {
    console.error('获取图书列表失败:', e)
    books.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const fetchCategories = async () => {
  try {
    const res = await getAllCategories()
    categories.value = (res.data || []).map(c => c.name)
  } catch (e) {
    console.error('获取分类失败:', e)
    categories.value = []
  }
}

const fetchMyReservations = async () => {
  try {
    const res = await getMyReservations({ page: 1, size: 100, status: 'pending' })
    reservedBookIds.value = new Set((res.data?.records || []).map(r => r.bookId))
  } catch (e) {
    console.error(e)
    reservedBookIds.value = new Set()
  }
}

const handleReserve = async (book) => {
  reservingId.value = book.id
  try {
    await createReservation({ bookId: book.id })
    reservedBookIds.value.add(book.id)
    lastReservedBook.value = book.title
    showSuccess.value = true
  } catch (e) {
    console.error('预约失败:', e)
  } finally {
    reservingId.value = null
  }
}

onMounted(() => {
  fetchBooks()
  fetchCategories()
  fetchMyReservations()
})
</script>

<style scoped>
.book-browse-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
  width: 100%;
  max-width: 100%;
}

.search-section {
}

.search-bar {
  display: flex;
  gap: 12px;
}

.search-input {
  flex: 1;
}

.category-select {
  width: 160px;
}

.grid-container {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}

.book-card {
  background: white;
  border-radius: var(--radius-md);
  padding: 20px;
  box-shadow: var(--shadow-soft);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  display: flex;
  flex-direction: column;
  position: relative;
  overflow: hidden;
}

.book-card::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: linear-gradient(90deg, var(--green), var(--blue), var(--purple-light));
  transform: scaleX(0);
  transition: transform 0.3s ease;
}

.book-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 12px 36px rgba(149, 125, 173, 0.15);
}

.book-card:hover::after {
  transform: scaleX(1);
}

.book-card:hover .cover-emoji {
  transform: scale(1.2) rotate(5deg);
}

.book-cover {
  position: relative;
  background: linear-gradient(135deg, #f8f9ff, #fff5f5);
  border-radius: var(--radius-sm);
  height: 80px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 16px;
}

.cover-emoji {
  font-size: 36px;
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.stock-badge {
  position: absolute;
  top: 8px;
  right: 8px;
  font-size: 11px;
  padding: 3px 8px;
  border-radius: 10px;
  background: var(--green-light);
  color: #2d8a56;
  font-weight: 500;
}

.stock-badge.low-stock {
  background: var(--pink-light);
  color: #c0392b;
}

.book-info {
  flex: 1;
}

.book-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 6px;
  line-height: 1.4;
}

.book-author {
  font-size: 13px;
  color: var(--text-secondary);
  margin: 0 0 10px;
}

.book-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.category-tag {
  font-size: 11px;
  background: linear-gradient(135deg, var(--blue-light), var(--purple-light)) !important;
  color: var(--purple) !important;
}

.age-range {
  font-size: 11px;
  color: var(--text-secondary);
  background: var(--yellow);
  padding: 2px 8px;
  border-radius: 8px;
}

.book-desc {
  font-size: 12px;
  color: var(--text-secondary);
  margin: 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.book-action {
  margin-top: 14px;
  padding-top: 14px;
  border-top: 1px solid #f5f5f5;
}

.reserve-btn {
  width: 100%;
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
  font-size: 16px;
}

.pagination-wrapper {
  margin-top: 24px;
  display: flex;
  justify-content: center;
}

.success-content {
  text-align: center;
  padding: 20px 0;
}

.success-icon {
  font-size: 56px;
  margin-bottom: 16px;
}

.success-content h3 {
  font-size: 20px;
  color: var(--text-primary);
  margin: 0 0 8px;
}

.success-content p {
  color: var(--text-secondary);
  margin: 0 0 4px;
  font-size: 14px;
}

.success-content .hint {
  color: var(--purple);
  font-size: 12px;
  margin-bottom: 20px;
}

.success-actions {
  display: flex;
  gap: 12px;
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
