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

    <!-- 图表区域 -->
    <div class="chart-section">
      <el-card class="chart-card">
        <template #header>
          <div class="chart-header">
            <span>📊 图书分类统计</span>
          </div>
        </template>
        <div ref="chartRef" class="chart-container"></div>
      </el-card>

      <el-card class="recent-card">
        <template #header>
          <div class="chart-header">
            <span>📖 快捷操作</span>
          </div>
        </template>
        <div class="quick-actions">
          <div class="action-item" @click="$router.push('/books')">
            <div class="action-icon" style="background: var(--green-light)">📚</div>
            <span>图书列表</span>
          </div>
          <div class="action-item" @click="$router.push('/categories')">
            <div class="action-icon" style="background: #F3EEFF">📂</div>
            <span>分类管理</span>
          </div>
          <div class="action-item" @click="$router.push('/books')">
            <div class="action-icon" style="background: var(--pink-light)">➕</div>
            <span>新增图书</span>
          </div>
          <div class="action-item" @click="$router.push('/books')">
            <div class="action-icon" style="background: var(--blue-light)">🔍</div>
            <span>搜索图书</span>
          </div>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, nextTick } from 'vue'
import { getStatistics } from '@/api'
import * as echarts from 'echarts'

const loading = ref(true)
const chartRef = ref(null)
const stats = ref({ totalBooks: 0, totalStock: 0, categoryStats: [] })

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
    icon: '📂',
    label: '图书分类',
    value: stats.value.categoryStats?.length || 0,
    bg: 'linear-gradient(135deg, #E3F2FD, #F0F7FF)'
  },
  {
    icon: '⭐',
    label: '系统状态',
    value: '正常',
    bg: 'linear-gradient(135deg, #FFF9C4, #FFFDE7)'
  }
])

const initChart = () => {
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
        itemStyle: {
          borderRadius: 10,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: { show: false },
        emphasis: {
          label: { show: true, fontSize: 16, fontWeight: 'bold' }
        },
        data,
        color: ['#FFB3BA', '#B5EAD7', '#C7CEEA', '#FFDAC1', '#957DAD', '#FFFFD1']
      }
    ]
  })

  window.addEventListener('resize', () => chart.resize())
}

onMounted(async () => {
  try {
    const res = await getStatistics()
    stats.value = res.data || { totalBooks: 0, totalStock: 0, categoryStats: [] }
  } catch (e) {
    console.warn('获取统计数据失败，使用默认值', e)
    stats.value = { totalBooks: 0, totalStock: 0, categoryStats: [] }
  } finally {
    loading.value = false
    await nextTick()
    initChart()
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
  grid-template-columns: 1.5fr 1fr;
  gap: 20px;
}

.chart-header {
  font-size: 16px;
  font-weight: 600;
}

.chart-container {
  height: 320px;
}

.quick-actions {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
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

@media (max-width: 1200px) {
  .stat-cards {
    grid-template-columns: repeat(2, 1fr);
  }
  .chart-section {
    grid-template-columns: 1fr;
  }
}
</style>
