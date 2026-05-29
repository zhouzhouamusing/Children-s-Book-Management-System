<template>
  <div class="recommend-page">
    <div class="page-header animate__animated animate__fadeInDown">
      <h2 class="section-title">
        <span class="title-icon">🌟</span>
        为你推荐
      </h2>
      <p class="section-desc">根据你的阅读偏好，为你精选好书</p>
    </div>

    <div v-loading="loading" class="recommend-content">
      <!-- 基于借阅历史推荐 -->
      <div class="recommend-section animate__animated animate__fadeInUp">
        <div class="section-header">
          <h3><span class="header-icon">📚</span> 猜你喜欢</h3>
          <span class="header-tip">基于你的借阅记录推荐</span>
        </div>
        <div class="book-scroll" v-if="byHistory.length">
          <div
            v-for="(book, index) in byHistory"
            :key="'h-' + book.id"
            class="book-card"
            :style="{ animationDelay: `${index * 0.08}s` }"
          >
            <div class="book-cover">
              <span class="cover-emoji">📖</span>
            </div>
            <div class="book-info">
              <h4 class="book-title">{{ book.title }}</h4>
              <p class="book-author">{{ book.author }}</p>
              <div class="book-tags">
                <el-tag size="small" round effect="plain" class="cat-tag">{{ book.category }}</el-tag>
                <span class="age-badge" v-if="book.ageRange">{{ book.ageRange }}</span>
              </div>
            </div>
          </div>
        </div>
        <div v-else class="empty-tip">暂无推荐，多借阅图书可以获得更精准的推荐哦~</div>
      </div>

      <!-- 基于年龄推荐 -->
      <div class="recommend-section animate__animated animate__fadeInUp" style="animation-delay: 0.1s">
        <div class="section-header">
          <h3><span class="header-icon">🎯</span> 适龄推荐</h3>
          <span class="header-tip">根据你的年龄段精选</span>
        </div>
        <div class="book-scroll" v-if="byAge.length">
          <div
            v-for="(book, index) in byAge"
            :key="'a-' + book.id"
            class="book-card"
            :style="{ animationDelay: `${index * 0.08}s` }"
          >
            <div class="book-cover age-cover">
              <span class="cover-emoji">📕</span>
            </div>
            <div class="book-info">
              <h4 class="book-title">{{ book.title }}</h4>
              <p class="book-author">{{ book.author }}</p>
              <div class="book-tags">
                <el-tag size="small" round effect="plain" class="cat-tag">{{ book.category }}</el-tag>
                <span class="age-badge" v-if="book.ageRange">{{ book.ageRange }}</span>
              </div>
            </div>
          </div>
        </div>
        <div v-else class="empty-tip">暂无适龄推荐</div>
      </div>

      <!-- TOP10 热门图书 -->
      <div class="recommend-section animate__animated animate__fadeInUp" style="animation-delay: 0.2s">
        <div class="section-header">
          <h3><span class="header-icon">🔥</span> 热门 TOP10</h3>
          <span class="header-tip">大家都在读的好书</span>
        </div>
        <div class="top10-list" v-if="top10.length">
          <div
            v-for="(item, index) in top10"
            :key="'t-' + item.id"
            class="top10-item"
            :style="{ animationDelay: `${index * 0.06}s` }"
          >
            <div class="rank-badge" :class="getRankClass(index)">
              {{ index + 1 }}
            </div>
            <div class="top10-cover">
              <span class="cover-emoji-sm">📗</span>
            </div>
            <div class="top10-info">
              <h4>{{ item.title }}</h4>
              <p>{{ item.author }}</p>
            </div>
            <div class="top10-meta">
              <el-tag size="small" round effect="plain" class="cat-tag" v-if="item.category">{{ item.category }}</el-tag>
              <span class="borrow-count">
                <el-icon><Reading /></el-icon>
                {{ item.borrowCount }}次借阅
              </span>
            </div>
          </div>
        </div>
        <div v-else class="empty-tip">暂无数据</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getAllRecommendations } from '@/api'

const loading = ref(false)
const byHistory = ref([])
const byAge = ref([])
const top10 = ref([])

const fetchRecommendations = async () => {
  loading.value = true
  try {
    const res = await getAllRecommendations()
    const data = res.data || {}
    byHistory.value = data.byHistory || []
    byAge.value = data.byAge || []
    top10.value = data.top10 || []
  } catch (e) {
    console.error('获取推荐失败:', e)
  } finally {
    loading.value = false
  }
}

const getRankClass = (index) => {
  if (index === 0) return 'rank-gold'
  if (index === 1) return 'rank-silver'
  if (index === 2) return 'rank-bronze'
  return ''
}

onMounted(() => {
  fetchRecommendations()
})
</script>

<style scoped>
.recommend-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.page-header {
  text-align: center;
  padding: 20px 0 10px;
}

.section-title {
  font-size: 24px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0 0 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.title-icon {
  font-size: 28px;
  animation: float 3s ease-in-out infinite;
}

.section-desc {
  color: var(--text-secondary);
  font-size: 14px;
  margin: 0;
}

.recommend-section {
  background: white;
  border-radius: var(--radius-md);
  padding: 24px;
  box-shadow: var(--shadow-soft);
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f5f5f5;
}

.section-header h3 {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-icon {
  font-size: 20px;
}

.header-tip {
  font-size: 12px;
  color: var(--text-secondary);
  background: var(--purple-light);
  padding: 4px 12px;
  border-radius: 12px;
}

.book-scroll {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 16px;
}

.book-card {
  background: linear-gradient(135deg, #fafbff, #fff8fa);
  border-radius: var(--radius-sm);
  padding: 16px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  cursor: pointer;
  animation: fadeInUp 0.4s ease both;
}

.book-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(149, 125, 173, 0.12);
}

.book-cover {
  background: linear-gradient(135deg, var(--blue-light), var(--purple-light));
  border-radius: 8px;
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 12px;
}

.age-cover {
  background: linear-gradient(135deg, var(--green-light), var(--blue-light));
}

.cover-emoji {
  font-size: 28px;
  transition: transform 0.3s ease;
}

.book-card:hover .cover-emoji {
  transform: scale(1.2) rotate(5deg);
}

.book-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.book-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.book-author {
  font-size: 12px;
  color: var(--text-secondary);
  margin: 0;
}

.book-tags {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 6px;
}

.cat-tag {
  font-size: 10px !important;
  background: linear-gradient(135deg, var(--blue-light), var(--purple-light)) !important;
  color: var(--purple) !important;
  border: none !important;
}

.age-badge {
  font-size: 10px;
  color: var(--text-secondary);
  background: var(--yellow);
  padding: 2px 6px;
  border-radius: 6px;
}

/* TOP10 列表 */
.top10-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.top10-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px 16px;
  border-radius: var(--radius-sm);
  background: linear-gradient(135deg, #fafbff, #fff9fc);
  transition: all 0.3s ease;
  animation: fadeInUp 0.4s ease both;
}

.top10-item:hover {
  transform: translateX(4px);
  box-shadow: 0 4px 16px rgba(149, 125, 173, 0.1);
}

.rank-badge {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 700;
  color: white;
  background: #ccc;
  flex-shrink: 0;
}

.rank-gold {
  background: linear-gradient(135deg, #FFD700, #FFA500);
  box-shadow: 0 2px 8px rgba(255, 215, 0, 0.4);
}

.rank-silver {
  background: linear-gradient(135deg, #C0C0C0, #A0A0A0);
  box-shadow: 0 2px 8px rgba(192, 192, 192, 0.4);
}

.rank-bronze {
  background: linear-gradient(135deg, #CD7F32, #B8860B);
  box-shadow: 0 2px 8px rgba(205, 127, 50, 0.3);
}

.top10-cover {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  background: linear-gradient(135deg, var(--green-light), var(--blue-light));
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.cover-emoji-sm {
  font-size: 20px;
}

.top10-info {
  flex: 1;
  min-width: 0;
}

.top10-info h4 {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 2px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.top10-info p {
  font-size: 12px;
  color: var(--text-secondary);
  margin: 0;
}

.top10-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

.borrow-count {
  font-size: 12px;
  color: var(--purple);
  display: flex;
  align-items: center;
  gap: 4px;
  font-weight: 500;
}

.empty-tip {
  text-align: center;
  padding: 30px 0;
  color: var(--text-secondary);
  font-size: 14px;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(12px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-6px); }
}
</style>
