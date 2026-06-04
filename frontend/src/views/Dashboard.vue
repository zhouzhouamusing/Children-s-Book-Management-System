<template>
  <div class="dashboard" v-loading="loading">
    <!-- 统计卡片 -->
    <div class="stat-cards">
      <div
        v-for="(card, index) in statCards"
        :key="card.label"
        class="stat-card animate-slide-in"
        :style="{ animationDelay: `${index * 0.1}s`, background: card.bg }"
      >
        <div class="stat-icon">{{ card.icon }}</div>
        <div class="stat-info">
          <div class="stat-value">{{ card.value }}</div>
          <div class="stat-label">{{ card.label }}</div>
        </div>
      </div>
    </div>

    <!-- 图表区域第一行 -->
    <div class="chart-section">
      <el-card class="chart-card">
        <template #header>
          <div class="chart-header">
            <span>借阅趋势</span>
          </div>
        </template>
        <div ref="trendChartRef" class="chart-container"></div>
      </el-card>

      <el-card class="chart-card">
        <template #header>
          <div class="chart-header">
            <span>图书分类统计</span>
          </div>
        </template>
        <div ref="chartRef" class="chart-container"></div>
      </el-card>
    </div>

    <!-- 图表区域第二行 -->
    <div class="chart-section" style="margin-top: 20px;">
      <el-card class="chart-card">
        <template #header>
          <div class="chart-header">
            <span>逾期统计</span>
          </div>
        </template>
        <div ref="overdueChartRef" class="chart-container"></div>
      </el-card>

      <el-card class="recent-card">
        <template #header>
          <div class="chart-header">
            <span>活跃读者 TOP 10</span>
          </div>
        </template>
        <div class="top-readers-list">
          <div v-for="(reader, idx) in topReaders" :key="idx" class="reader-row">
            <span class="rank" :class="'rank-' + (idx + 1)">{{ idx + 1 }}</span>
            <span class="reader-name">{{ reader.name }}</span>
            <span class="reader-level">{{ reader.level }}</span>
            <span class="reader-count">{{ reader.borrowCount }}次</span>
          </div>
          <el-empty v-if="!topReaders.length" description="暂无数据" :image-size="60" />
        </div>
      </el-card>
    </div>

    <!-- 图表区域第三行 -->
    <div class="chart-section" style="margin-top: 20px;">
      <el-card class="chart-card">
        <template #header>
          <div class="chart-header">
            <span>读者增长趋势</span>
          </div>
        </template>
        <div ref="readerGrowthChartRef" class="chart-container"></div>
      </el-card>

      <el-card class="chart-card">
        <template #header>
          <div class="chart-header">
            <span>审计日志（最近操作）</span>
          </div>
        </template>
        <div class="top-readers-list">
          <div v-for="(log, idx) in recentLogs" :key="idx" class="reader-row">
            <span class="log-action">{{ log.action }}</span>
            <span class="reader-name">{{ log.operatorUsername || '系统' }}</span>
            <span class="reader-count">{{ log.detail }}</span>
          </div>
          <el-empty v-if="!recentLogs.length" description="暂无日志" :image-size="60" />
        </div>
      </el-card>
    </div>

    <!-- 快捷操作 -->
    <el-card class="quick-card" style="margin-top: 20px;">
      <template #header>
        <div class="chart-header"><span>图书利用率 TOP 10</span></div>
      </template>
      <div class="top-readers-list">
        <div v-for="(book, idx) in bookUtilization" :key="idx" class="reader-row">
          <span class="rank" :class="'rank-' + (idx + 1)">{{ idx + 1 }}</span>
          <span class="reader-name">{{ book.title }}</span>
          <span class="reader-level">库存:{{ book.stock }}</span>
          <span class="reader-count">借出{{ book.totalBorrows }}次</span>
        </div>
        <el-empty v-if="!bookUtilization.length" description="暂无数据" :image-size="60" />
      </div>
    </el-card>

    <!-- 快捷操作 -->
    <el-card class="quick-card" style="margin-top: 20px;">
      <template #header>
        <div class="chart-header"><span>快捷操作</span></div>
      </template>
      <div class="quick-actions">
        <div class="action-item" @click="$router.push('/books')">
          <div class="action-icon" style="background: var(--green-light)">📚</div>
          <span>图书列表</span>
        </div>
        <div class="action-item" @click="$router.push('/borrows')">
          <div class="action-icon" style="background: #F3EEFF">📖</div>
          <span>借阅管理</span>
        </div>
        <div class="action-item" @click="$router.push('/readers')">
          <div class="action-icon" style="background: var(--pink-light)">👦</div>
          <span>读者管理</span>
        </div>
        <div class="action-item" @click="$router.push('/categories')">
          <div class="action-icon" style="background: var(--blue-light)">📂</div>
          <span>分类管理</span>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, nextTick } from 'vue'
import { getStatistics, getBorrowTrends, getOverdueAnalytics, getTopReaders, getReaderGrowth, getAuditLogs, getBookUtilization } from '@/api'
import * as echarts from 'echarts'

const loading = ref(true)
const chartRef = ref(null)
const trendChartRef = ref(null)
const overdueChartRef = ref(null)
const readerGrowthChartRef = ref(null)
const stats = ref({ totalBooks: 0, totalStock: 0, categoryStats: [], totalBorrows: 0, activeBorrows: 0, overdueBorrows: 0, totalReaders: 0, activeReaders: 0 })
const topReaders = ref([])
const recentLogs = ref([])
const bookUtilization = ref([])

const statCards = computed(() => [
  {
    icon: '📚',
    label: '图书种类',
    value: stats.value.totalBooks,
    bg: 'linear-gradient(135deg, #FFE5E8, #FFF0F2)'
  },
  {
    icon: '📦',
    label: '库存总量',
    value: stats.value.totalStock,
    bg: 'linear-gradient(135deg, #E8F5E9, #F1FFF3)'
  },
  {
    icon: '📖',
    label: '借阅中',
    value: stats.value.activeBorrows,
    bg: 'linear-gradient(135deg, #E3F2FD, #F0F7FF)'
  },
  {
    icon: '👦',
    label: '注册读者',
    value: stats.value.totalReaders,
    bg: 'linear-gradient(135deg, #FFF9C4, #FFFDE7)'
  }
])

const initCategoryChart = () => {
  if (!chartRef.value || !stats.value.categoryStats?.length) return
  const chart = echarts.init(chartRef.value)
  const data = stats.value.categoryStats.map(item => ({
    name: item.category || '未分类',
    value: item.count
  }))

  chart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c}本 ({d}%)' },
    legend: { bottom: '5%', left: 'center' },
    series: [
      {
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: false,
        itemStyle: { borderRadius: 10, borderColor: '#fff', borderWidth: 2 },
        label: { show: false },
        emphasis: { label: { show: true, fontSize: 16, fontWeight: 'bold' } },
        data,
        color: ['#FFB3BA', '#B5EAD7', '#C7CEEA', '#FFDAC1', '#957DAD', '#FFFFD1']
      }
    ]
  })
  window.addEventListener('resize', () => chart.resize())
}

const initTrendChart = async () => {
  if (!trendChartRef.value) return
  try {
    const res = await getBorrowTrends({ months: 6 })
    const trends = res.data || []
    const chart = echarts.init(trendChartRef.value)
    chart.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: ['借出', '归还'], bottom: '5%' },
      grid: { top: '10%', left: '3%', right: '4%', bottom: '18%', containLabel: true },
      xAxis: { type: 'category', data: trends.map(t => t.month), axisLabel: { fontSize: 11 } },
      yAxis: { type: 'value', minInterval: 1 },
      series: [
        { name: '借出', type: 'line', smooth: true, data: trends.map(t => t.borrowCount), itemStyle: { color: '#957DAD' }, areaStyle: { color: 'rgba(149,125,173,0.1)' } },
        { name: '归还', type: 'line', smooth: true, data: trends.map(t => t.returnCount), itemStyle: { color: '#B5EAD7' }, areaStyle: { color: 'rgba(181,234,215,0.1)' } }
      ]
    })
    window.addEventListener('resize', () => chart.resize())
  } catch (e) {
    console.warn('加载借阅趋势失败', e)
  }
}

const initOverdueChart = async () => {
  if (!overdueChartRef.value) return
  try {
    const res = await getOverdueAnalytics({ months: 6 })
    const data = res.data || []
    const chart = echarts.init(overdueChartRef.value)
    chart.setOption({
      tooltip: { trigger: 'axis' },
      grid: { top: '10%', left: '3%', right: '4%', bottom: '12%', containLabel: true },
      xAxis: { type: 'category', data: data.map(d => d.month), axisLabel: { fontSize: 11 } },
      yAxis: { type: 'value', minInterval: 1 },
      series: [
        { name: '逾期数', type: 'bar', data: data.map(d => d.overdueCount), itemStyle: { color: '#FFB3BA', borderRadius: [4, 4, 0, 0] } }
      ]
    })
    window.addEventListener('resize', () => chart.resize())
  } catch (e) {
    console.warn('加载逾期统计失败', e)
  }
}

const loadTopReaders = async () => {
  try {
    const res = await getTopReaders({ limit: 10 })
    topReaders.value = res.data || []
  } catch (e) {
    console.warn('加载读者排行失败', e)
  }
}

const initReaderGrowthChart = async () => {
  if (!readerGrowthChartRef.value) return
  try {
    const res = await getReaderGrowth({ months: 12 })
    const data = res.data || []
    const chart = echarts.init(readerGrowthChartRef.value)
    chart.setOption({
      tooltip: { trigger: 'axis' },
      grid: { top: '10%', left: '3%', right: '4%', bottom: '12%', containLabel: true },
      xAxis: { type: 'category', data: data.map(d => d.month), axisLabel: { fontSize: 11 } },
      yAxis: { type: 'value', minInterval: 1 },
      series: [
        { name: '新增读者', type: 'bar', data: data.map(d => d.newReaders), itemStyle: { color: '#C7CEEA', borderRadius: [4, 4, 0, 0] } }
      ]
    })
    window.addEventListener('resize', () => chart.resize())
  } catch (e) {
    console.warn('加载读者增长趋势失败', e)
  }
}

const loadRecentLogs = async () => {
  try {
    const res = await getAuditLogs({ page: 1, size: 10 })
    recentLogs.value = res.data?.records || []
  } catch (e) {
    console.warn('加载审计日志失败', e)
  }
}

const loadBookUtilization = async () => {
  try {
    const res = await getBookUtilization({ limit: 10 })
    bookUtilization.value = res.data || []
  } catch (e) {
    console.warn('加载图书利用率失败', e)
  }
}

onMounted(async () => {
  try {
    const res = await getStatistics()
    stats.value = res.data || { totalBooks: 0, totalStock: 0, categoryStats: [], totalBorrows: 0, activeBorrows: 0, overdueBorrows: 0, totalReaders: 0 }
  } catch (e) {
    console.warn('获取统计数据失败，使用默认值', e)
    stats.value = { totalBooks: 0, totalStock: 0, categoryStats: [], totalBorrows: 0, activeBorrows: 0, overdueBorrows: 0, totalReaders: 0 }
  } finally {
    loading.value = false
    await nextTick()
    initCategoryChart()
    initTrendChart()
    initOverdueChart()
    initReaderGrowthChart()
    loadTopReaders()
    loadRecentLogs()
    loadBookUtilization()
  }
})
</script>

<style scoped>
.dashboard {
  min-height: calc(100vh - 112px);
}

.stat-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 24px;
}

.stat-card {
  padding: 24px;
  border-radius: var(--radius-lg);
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: var(--shadow-soft);
  transition: all 0.3s ease;
  cursor: pointer;
  opacity: 0;
}

.stat-card:hover {
  transform: translateY(-5px);
  box-shadow: var(--shadow-hover);
}

.stat-icon {
  font-size: 40px;
}

.stat-info {
  display: flex;
  flex-direction: column;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: var(--text-primary);
}

.stat-label {
  font-size: 14px;
  color: var(--text-secondary);
  margin-top: 4px;
}

.chart-section {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.chart-header {
  font-size: 16px;
  font-weight: 600;
}

.chart-container {
  height: 300px;
}

.quick-actions {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  padding: 10px 0;
}

.action-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 20px;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all 0.3s ease;
}

.action-item:hover {
  background: #F9F9F9;
  transform: scale(1.05);
}

.action-icon {
  width: 56px;
  height: 56px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
}

.action-item span {
  font-size: 13px;
  color: var(--text-secondary);
}

.top-readers-list {
  max-height: 300px;
  overflow-y: auto;
}

.reader-row {
  display: flex;
  align-items: center;
  padding: 10px 8px;
  border-bottom: 1px solid #f5f5f5;
  gap: 12px;
}

.reader-row:last-child {
  border-bottom: none;
}

.rank {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: bold;
  background: #f0f0f0;
  color: #666;
}

.rank-1 { background: #FFD700; color: #fff; }
.rank-2 { background: #C0C0C0; color: #fff; }
.rank-3 { background: #CD7F32; color: #fff; }

.reader-name {
  flex: 1;
  font-size: 14px;
  font-weight: 500;
}

.reader-level {
  font-size: 12px;
  color: #957DAD;
  background: #F3EEFF;
  padding: 2px 8px;
  border-radius: 10px;
}

.reader-count {
  font-size: 13px;
  color: var(--text-secondary);
  min-width: 50px;
  text-align: right;
}

.log-action {
  font-size: 12px;
  color: #409EFF;
  background: #ECF5FF;
  padding: 2px 8px;
  border-radius: 10px;
  white-space: nowrap;
}

@media (max-width: 1200px) {
  .stat-cards {
    grid-template-columns: repeat(2, 1fr);
  }
  .chart-section {
    grid-template-columns: 1fr;
  }
  .quick-actions {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
