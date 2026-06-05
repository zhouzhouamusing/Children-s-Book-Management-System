<template>
  <div class="categories-page">
    <!-- 统计卡片区域 -->
    <div class="stat-row animate__animated animate__fadeInDown">
      <div class="mini-stat" style="--accent: var(--pink)">
        <span class="mini-stat-icon">📂</span>
        <div class="mini-stat-info">
          <span class="mini-stat-value">{{ categoryStats.total }}</span>
          <span class="mini-stat-label">分类总数</span>
        </div>
      </div>
      <div class="mini-stat" style="--accent: var(--green)">
        <span class="mini-stat-icon">✅</span>
        <div class="mini-stat-info">
          <span class="mini-stat-value">{{ categoryStats.active }}</span>
          <span class="mini-stat-label">启用中</span>
        </div>
      </div>
      <div class="mini-stat" style="--accent: var(--blue)">
        <span class="mini-stat-icon">📚</span>
        <div class="mini-stat-info">
          <span class="mini-stat-value">{{ categoryStats.totalBooks }}</span>
          <span class="mini-stat-label">关联图书</span>
        </div>
      </div>
      <div class="mini-stat" style="--accent: var(--yellow)">
        <span class="mini-stat-icon">⚠️</span>
        <div class="mini-stat-info">
          <span class="mini-stat-value">{{ categoryStats.empty }}</span>
          <span class="mini-stat-label">空分类</span>
        </div>
      </div>
    </div>

    <!-- 搜索与操作栏 -->
    <el-card class="search-card animate__animated animate__fadeInDown" style="animation-delay: 0.1s">
      <div class="search-bar">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索分类名称..."
          prefix-icon="Search"
          size="large"
          clearable
          class="search-input"
          @input="handleSearchDebounced"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        />
        <el-select
          v-model="searchStatus"
          placeholder="全部状态"
          size="large"
          clearable
          class="status-select"
          @change="handleSearch"
        >
          <el-option label="启用" :value="1" />
          <el-option label="禁用" :value="0" />
        </el-select>
        <el-button type="primary" size="large" @click="handleSearch">
          <el-icon><Search /></el-icon>
          搜索
        </el-button>
        <el-button v-permission="'CATEGORY_CREATE'" type="success" size="large" @click="handleAdd">
          <el-icon><Plus /></el-icon>
          新增分类
        </el-button>
      </div>
    </el-card>

    <!-- 分类卡片网格 -->
    <div class="category-grid" v-loading="loading" element-loading-text="加载分类数据中...">
      <transition-group name="card-list" tag="div" class="grid-inner">
        <div
          v-for="(cat, index) in categoryList"
          :key="cat.id"
          class="category-card"
          :style="{ animationDelay: `${index * 0.05}s`, '--card-color': cat.color || '#C7CEEA' }"
        >
          <div class="card-color-bar" :style="{ background: cat.color || '#C7CEEA' }"></div>
          <div class="card-header">
            <div class="card-icon-wrapper" :style="{ background: (cat.color || '#C7CEEA') + '30' }">
              <span class="card-icon">{{ cat.icon || '📁' }}</span>
            </div>
            <div class="card-badges">
              <el-tag :type="cat.status === 1 ? 'success' : 'info'" size="small" effect="light">
                {{ cat.status === 1 ? '启用' : '禁用' }}
              </el-tag>
              <el-tag type="warning" size="small" effect="plain" v-if="cat.sortOrder > 0">
                排序: {{ cat.sortOrder }}
              </el-tag>
            </div>
          </div>
          <div class="card-body" @click="handleViewBooks(cat)">
            <h3 class="card-title">{{ cat.name }}</h3>
            <p class="card-desc">{{ cat.description || '暂无描述' }}</p>
            <div class="card-meta">
              <span class="meta-item">
                <el-icon><User /></el-icon>
                {{ cat.ageRangeMin }}-{{ cat.ageRangeMax }}岁
              </span>
              <span class="meta-item meta-books">
                <el-icon><Reading /></el-icon>
                {{ cat.bookCount || 0 }}本图书
              </span>
            </div>
            <div class="card-view-hint">
              <el-icon><Right /></el-icon>
              <span>点击查看该分类图书</span>
            </div>
          </div>
          <div class="card-footer">
            <el-button v-permission="'CATEGORY_READ'" class="card-btn view" size="small" @click="handleViewBooks(cat)">
              <el-icon><View /></el-icon> 查看图书
            </el-button>
            <el-button v-permission="'CATEGORY_UPDATE'" class="card-btn edit" size="small" @click="handleEdit(cat)">
              <el-icon><Edit /></el-icon> 编辑
            </el-button>
            <el-button v-permission="'CATEGORY_DELETE'" class="card-btn delete" size="small" @click="handleDelete(cat)">
              <el-icon><Delete /></el-icon> 删除
            </el-button>
          </div>
        </div>
      </transition-group>

      <el-empty v-if="!loading && categoryList.length === 0" description="暂无分类数据，点击上方按钮创建第一个分类吧~">
        <el-button v-permission="'CATEGORY_CREATE'" type="primary" @click="handleAdd">立即创建</el-button>
      </el-empty>
    </div>

    <!-- 分页 -->
    <div class="pagination-wrapper" v-if="total > pageSize">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[12, 24, 48]"
        layout="total, sizes, prev, pager, next"
        background
        @current-change="fetchCategories"
        @size-change="fetchCategories"
      />
    </div>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑分类' : '新增分类'"
      width="560px"
      :close-on-click-modal="false"
      destroy-on-close
      class="category-dialog"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
        v-loading="submitLoading"
        element-loading-text="正在保存..."
      >
        <el-form-item label="分类名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入分类名称" maxlength="20" show-word-limit />
        </el-form-item>

        <div class="form-row">
          <el-form-item label="图标" prop="icon">
            <div class="icon-picker">
              <span
                v-for="emoji in emojiOptions"
                :key="emoji"
                class="emoji-option"
                :class="{ active: form.icon === emoji }"
                @click="form.icon = emoji"
              >{{ emoji }}</span>
            </div>
          </el-form-item>
        </div>

        <el-form-item label="主题色" prop="color">
          <div class="color-picker">
            <span
              v-for="color in colorOptions"
              :key="color"
              class="color-option"
              :class="{ active: form.color === color }"
              :style="{ background: color }"
              @click="form.color = color"
            ></span>
          </div>
        </el-form-item>

        <el-form-item label="适龄区间" prop="ageRange">
          <div class="age-range-row">
            <el-input-number v-model="form.ageRangeMin" :min="0" :max="18" placeholder="最小" />
            <span class="age-separator">~</span>
            <el-input-number v-model="form.ageRangeMax" :min="0" :max="18" placeholder="最大" />
            <span class="age-unit">岁</span>
          </div>
        </el-form-item>

        <el-form-item label="排序权重" prop="sortOrder">
          <el-input-number v-model="form.sortOrder" :min="0" :max="999" />
          <span class="form-tip">数值越大，排序越靠前</span>
        </el-form-item>

        <el-form-item label="状态" prop="status">
          <el-switch
            v-model="form.status"
            :active-value="1"
            :inactive-value="0"
            active-text="启用"
            inactive-text="禁用"
          />
        </el-form-item>

        <el-form-item label="描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            placeholder="请输入分类描述"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">
          {{ submitLoading ? '保存中...' : '确认保存' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getCategoryList, addCategory, updateCategory, deleteCategory } from '@/api'
import { usePermission } from '@/composables/usePermission'
const { checkWithFeedback } = usePermission()

const router = useRouter()
const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)

const searchKeyword = ref('')
const searchStatus = ref(null)
const page = ref(1)
const pageSize = ref(12)
const total = ref(0)
const categoryList = ref([])

let searchTimer = null

const emojiOptions = ['📁', '🧚', '🌱', '🎨', '💫', '🔬', '📜', '🌍', '🎭', '🚀', '🦄', '🌈', '🎵', '🏰', '🐾', '🌸']
const colorOptions = ['#FFB3BA', '#B5EAD7', '#C7CEEA', '#FFDAC1', '#957DAD', '#FFFFD1', '#E8F5E9', '#FFE0B2', '#B3E5FC', '#F8BBD0', '#DCEDC8', '#D1C4E9']

const form = reactive({
  id: null,
  name: '',
  icon: '📁',
  color: '#C7CEEA',
  ageRangeMin: 0,
  ageRangeMax: 14,
  sortOrder: 0,
  description: '',
  status: 1
})

const rules = {
  name: [{ required: true, message: '请输入分类名称', trigger: 'blur' }]
}

const categoryStats = computed(() => {
  const list = categoryList.value
  return {
    total: total.value,
    active: list.filter(c => c.status === 1).length,
    totalBooks: list.reduce((sum, c) => sum + (c.bookCount || 0), 0),
    empty: list.filter(c => !c.bookCount || c.bookCount === 0).length
  }
})

const fetchCategories = async () => {
  loading.value = true
  try {
    const res = await getCategoryList({
      page: page.value,
      size: pageSize.value,
      keyword: searchKeyword.value,
      status: searchStatus.value
    })
    categoryList.value = res.data.records
    total.value = res.data.total
  } catch (e) {
    // handled by interceptor
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  page.value = 1
  fetchCategories()
}

const handleSearchDebounced = () => {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    handleSearch()
  }, 300)
}

const resetForm = () => {
  Object.assign(form, {
    id: null, name: '', icon: '📁', color: '#C7CEEA',
    ageRangeMin: 0, ageRangeMax: 14, sortOrder: 0,
    description: '', status: 1
  })
}

const handleAdd = () => {
  if (!checkWithFeedback('CATEGORY_CREATE')) return
  isEdit.value = false
  resetForm()
  dialogVisible.value = true
}

const handleEdit = (row) => {
  if (!checkWithFeedback('CATEGORY_UPDATE')) return
  isEdit.value = true
  Object.assign(form, { ...row })
  dialogVisible.value = true
}

const handleViewBooks = (cat) => {
  router.push({ path: '/books', query: { category: cat.name } })
}

const handleSubmit = async () => {
  if (!checkWithFeedback(isEdit.value ? 'CATEGORY_UPDATE' : 'CATEGORY_CREATE')) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  if (form.ageRangeMin > form.ageRangeMax) {
    ElMessage.warning('适龄区间的最小值不能大于最大值')
    return
  }

  submitLoading.value = true
  try {
    if (isEdit.value) {
      await updateCategory(form.id, form)
      ElMessage.success('分类更新成功！')
    } else {
      await addCategory(form)
      ElMessage.success('分类创建成功！')
    }
    dialogVisible.value = false
    fetchCategories()
  } catch (e) {
    // handled by interceptor
  } finally {
    submitLoading.value = false
  }
}

const handleDelete = (row) => {
  if (!checkWithFeedback('CATEGORY_DELETE')) return
  const bookCount = row.bookCount || 0
  if (bookCount > 0) {
    ElMessageBox.alert(
      `该分类下有 ${bookCount} 本图书，请先将这些图书移至其他分类后再删除。`,
      '无法删除',
      { confirmButtonText: '我知道了', type: 'warning' }
    )
    return
  }
  ElMessageBox.confirm(
    `确定要删除分类"${row.name}"吗？此操作不可恢复。`,
    '删除确认',
    {
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
      type: 'warning',
      confirmButtonClass: 'el-button--danger'
    }
  ).then(async () => {
    loading.value = true
    try {
      await deleteCategory(row.id)
      ElMessage.success('删除成功！')
      fetchCategories()
    } catch (e) {
      // handled by interceptor
    } finally {
      loading.value = false
    }
  }).catch(() => {})
}

onMounted(() => {
  fetchCategories()
})
</script>

<style scoped>
.categories-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* 统计卡片 */
.stat-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.mini-stat {
  background: white;
  border-radius: var(--radius-md);
  padding: 18px 20px;
  display: flex;
  align-items: center;
  gap: 14px;
  box-shadow: var(--shadow-soft);
  transition: all 0.3s ease;
  border-left: 4px solid var(--accent);
}

.mini-stat:hover {
  transform: translateY(-3px);
  box-shadow: var(--shadow-hover);
}

.mini-stat-icon {
  font-size: 28px;
}

.mini-stat-info {
  display: flex;
  flex-direction: column;
}

.mini-stat-value {
  font-size: 22px;
  font-weight: 700;
  color: var(--text-primary);
}

.mini-stat-label {
  font-size: 12px;
  color: var(--text-secondary);
  margin-top: 2px;
}

/* 搜索栏 */
.search-card {
  background: white;
}

.search-bar {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}

.search-input {
  flex: 1;
  min-width: 200px;
}

.status-select {
  width: 140px;
}

/* 分类卡片网格 */
.category-grid {
  min-height: 200px;
}

.grid-inner {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
}

.category-card {
  background: white;
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-soft);
  overflow: hidden;
  position: relative;
  transition: all 0.35s cubic-bezier(0.4, 0, 0.2, 1);
  animation: cardFadeIn 0.4s ease forwards;
  opacity: 0;
  cursor: default;
}

.category-card:hover {
  transform: translateY(-6px) scale(1.01);
  box-shadow: 0 16px 40px rgba(0, 0, 0, 0.12);
}

.category-card:hover .card-color-bar {
  height: 5px;
}

.category-card:hover .card-view-hint {
  opacity: 1;
  transform: translateX(0);
}

@keyframes cardFadeIn {
  from {
    opacity: 0;
    transform: translateY(24px) scale(0.96);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.card-color-bar {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  transition: height 0.3s ease;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 20px 20px 12px;
}

.card-icon-wrapper {
  width: 48px;
  height: 48px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

.category-card:hover .card-icon-wrapper {
  transform: scale(1.15) rotate(8deg);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.card-icon {
  font-size: 24px;
  transition: transform 0.3s ease;
}

.category-card:hover .card-icon {
  animation: iconBounce 0.5s ease;
}

@keyframes iconBounce {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.2); }
}

.card-badges {
  display: flex;
  gap: 6px;
}

.card-body {
  padding: 0 20px 16px;
  cursor: pointer;
  transition: background 0.2s ease;
  border-radius: 0 0 8px 8px;
}

.card-body:hover {
  background: #FAFAFA;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 8px;
  transition: color 0.2s ease;
}

.card-body:hover .card-title {
  color: var(--purple);
}

.card-desc {
  font-size: 13px;
  color: var(--text-secondary);
  margin: 0 0 12px;
  line-height: 1.5;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.card-meta {
  display: flex;
  gap: 16px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--text-secondary);
}

.meta-books {
  font-weight: 600;
  color: var(--purple);
}

.card-view-hint {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 10px;
  font-size: 12px;
  color: var(--purple);
  opacity: 0;
  transform: translateX(-8px);
  transition: all 0.3s ease;
}

.card-footer {
  padding: 12px 20px;
  border-top: 1px solid #F5F5F5;
  display: flex;
  gap: 8px;
}

.card-btn.view {
  background: linear-gradient(135deg, var(--green), #8DD5BE) !important;
  border: none !important;
  color: #fff !important;
  font-size: 12px;
}

.card-btn.view:hover {
  background: linear-gradient(135deg, #8DD5BE, var(--green)) !important;
  box-shadow: 0 3px 8px rgba(181, 234, 215, 0.5);
}

.card-btn.edit {
  background: linear-gradient(135deg, var(--btn-edit-from), var(--btn-edit-to)) !important;
  border: none !important;
  color: #fff !important;
  font-size: 12px;
}

.card-btn.edit:hover {
  background: linear-gradient(135deg, var(--btn-edit-to), var(--btn-edit-from)) !important;
  box-shadow: 0 3px 8px rgba(167, 139, 250, 0.3);
}

.card-btn.delete {
  background: linear-gradient(135deg, var(--btn-delete-from), var(--btn-delete-to)) !important;
  border: none !important;
  color: #fff !important;
  font-size: 12px;
}

.card-btn.delete:hover {
  background: linear-gradient(135deg, var(--btn-delete-to), var(--btn-delete-from)) !important;
  box-shadow: 0 3px 8px rgba(255, 179, 186, 0.4);
}

/* 分页 */
.pagination-wrapper {
  display: flex;
  justify-content: center;
  padding-top: 8px;
}

/* 弹窗样式 */
.category-dialog :deep(.el-dialog__header) {
  background: linear-gradient(135deg, var(--green-light), var(--blue-light));
  margin-right: 0;
  padding: 20px 24px;
}

.category-dialog :deep(.el-dialog__title) {
  font-weight: 600;
  font-size: 18px;
}

.form-row {
  margin-bottom: 0;
}

/* 图标选择器 */
.icon-picker {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.emoji-option {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s ease;
  border: 2px solid transparent;
  background: #F9F9F9;
}

.emoji-option:hover {
  background: #F0F0F0;
  transform: scale(1.15);
}

.emoji-option.active {
  border-color: var(--purple);
  background: rgba(149, 125, 173, 0.1);
  transform: scale(1.15);
}

/* 颜色选择器 */
.color-picker {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.color-option {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  cursor: pointer;
  transition: all 0.2s ease;
  border: 3px solid transparent;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.1);
}

.color-option:hover {
  transform: scale(1.2);
}

.color-option.active {
  border-color: #4A4A4A;
  transform: scale(1.2);
  box-shadow: 0 3px 10px rgba(0, 0, 0, 0.2);
}

/* 适龄区间 */
.age-range-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.age-separator {
  color: var(--text-secondary);
  font-size: 16px;
}

.age-unit {
  color: var(--text-secondary);
  font-size: 14px;
}

.form-tip {
  margin-left: 12px;
  font-size: 12px;
  color: var(--text-muted);
}

/* 列表过渡动画 */
.card-list-enter-active {
  transition: all 0.4s ease;
}

.card-list-leave-active {
  transition: all 0.3s ease;
}

.card-list-enter-from {
  opacity: 0;
  transform: translateY(20px);
}

.card-list-leave-to {
  opacity: 0;
  transform: scale(0.9);
}

.card-list-move {
  transition: transform 0.4s ease;
}

@media (max-width: 1200px) {
  .stat-row {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .stat-row {
    grid-template-columns: 1fr;
  }
  .grid-inner {
    grid-template-columns: 1fr;
  }
}
</style>
