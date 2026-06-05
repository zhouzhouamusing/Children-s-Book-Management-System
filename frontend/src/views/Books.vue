<template>
  <div class="books-page">
    <!-- 搜索区域 -->
    <el-card class="search-card animate__animated animate__fadeInDown">
      <div class="search-bar">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索书名、作者或ISBN..."
          prefix-icon="Search"
          size="large"
          clearable
          class="search-input"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        />
        <el-select
          v-model="searchCategory"
          placeholder="全部分类"
          size="large"
          clearable
          class="category-select"
          @change="handleSearch"
        >
          <el-option
            v-for="cat in categories"
            :key="cat"
            :label="(getCategoryIcon(cat) || '') + ' ' + cat"
            :value="cat"
          />
        </el-select>
        <el-button type="primary" size="large" @click="handleSearch">
          <el-icon><Search /></el-icon>
          搜索
        </el-button>
        <el-button v-permission="'BOOK_CREATE'" type="success" size="large" @click="handleAdd">
          <el-icon><Plus /></el-icon>
          新增图书
        </el-button>
        <el-button v-permission="'BOOK_EXPORT'" type="info" size="large" plain @click="handleExport">
          <el-icon><Download /></el-icon>
          导出数据
        </el-button>
      </div>
    </el-card>

    <!-- 图书列表 -->
    <el-card class="table-card" v-loading="tableLoading" element-loading-text="正在加载图书数据...">
      <el-table
        :data="bookList"
        stripe
        style="width: 100%"
        row-class-name="table-row"
      >
        <el-table-column type="index" label="#" width="50" />
        <el-table-column prop="title" label="书名" min-width="160">
          <template #default="{ row }">
            <div class="book-title-cell">
              <span class="book-emoji">📖</span>
              <span>{{ row.title }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="author" label="作者" width="120" />
        <el-table-column prop="category" label="分类" width="130">
          <template #default="{ row }">
            <span
              class="category-badge"
              :style="getCategoryColor(row.category) ? { background: getCategoryColor(row.category) + '25', borderColor: getCategoryColor(row.category) } : {}"
            >
              <span v-if="getCategoryIcon(row.category)" class="category-badge-icon">{{ getCategoryIcon(row.category) }}</span>
              <span class="category-badge-text">{{ row.category || '未分类' }}</span>
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="ageRange" label="适读年龄" width="100" />
        <el-table-column prop="price" label="价格" width="90">
          <template #default="{ row }">
            <span class="price">¥{{ row.price || '0.00' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="stock" label="库存" width="80">
          <template #default="{ row }">
            <el-tag :type="row.stock > 10 ? 'success' : row.stock > 0 ? 'warning' : 'danger'" size="small">
              {{ row.stock }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '上架' : '下架' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button v-permission="'BOOK_UPDATE'" class="action-btn-edit" @click="handleEdit(row)">
              <el-icon><Edit /></el-icon> 编辑
            </el-button>
            <el-button v-permission="'BOOK_DELETE'" class="action-btn-delete" @click="handleDelete(row)">
              <el-icon><Delete /></el-icon> 删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @current-change="fetchBooks"
          @size-change="fetchBooks"
        />
      </div>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑图书' : '新增图书'"
      width="600px"
      :close-on-click-modal="false"
      destroy-on-close
      class="book-dialog"
    >
      <el-form
        ref="bookFormRef"
        :model="bookForm"
        :rules="bookRules"
        label-width="90px"
        v-loading="submitLoading"
        element-loading-text="正在保存..."
      >
        <el-form-item label="书名" prop="title">
          <el-input v-model="bookForm.title" placeholder="请输入书名" />
        </el-form-item>
        <el-form-item label="作者" prop="author">
          <el-input v-model="bookForm.author" placeholder="请输入作者" />
        </el-form-item>
        <div class="form-row">
          <el-form-item label="出版社" prop="publisher">
            <el-input v-model="bookForm.publisher" placeholder="出版社" />
          </el-form-item>
          <el-form-item label="ISBN" prop="isbn">
            <el-input v-model="bookForm.isbn" placeholder="ISBN编号" />
          </el-form-item>
        </div>
        <div class="form-row">
          <el-form-item label="分类" prop="category">
            <el-select v-model="bookForm.category" placeholder="选择分类" allow-create filterable>
              <el-option v-for="cat in categories" :key="cat" :label="(getCategoryIcon(cat) || '') + ' ' + cat" :value="cat" />
            </el-select>
          </el-form-item>
          <el-form-item label="适读年龄" prop="ageRange">
            <el-select v-model="bookForm.ageRange" placeholder="适读年龄">
              <el-option label="0-3岁" value="0-3岁" />
              <el-option label="2-5岁" value="2-5岁" />
              <el-option label="3-6岁" value="3-6岁" />
              <el-option label="5-8岁" value="5-8岁" />
              <el-option label="6-10岁" value="6-10岁" />
              <el-option label="6-12岁" value="6-12岁" />
              <el-option label="8-12岁" value="8-12岁" />
              <el-option label="8-14岁" value="8-14岁" />
              <el-option label="10岁以上" value="10岁以上" />
            </el-select>
          </el-form-item>
        </div>
        <div class="form-row">
          <el-form-item label="价格" prop="price">
            <el-input-number v-model="bookForm.price" :min="0" :precision="2" :step="1" />
          </el-form-item>
          <el-form-item label="库存" prop="stock">
            <el-input-number v-model="bookForm.stock" :min="0" :step="1" />
          </el-form-item>
        </div>
        <el-form-item label="状态" prop="status">
          <el-switch
            v-model="bookForm.status"
            :active-value="1"
            :inactive-value="0"
            active-text="上架"
            inactive-text="下架"
          />
        </el-form-item>
        <el-form-item label="简介" prop="description">
          <el-input
            v-model="bookForm.description"
            type="textarea"
            :rows="3"
            placeholder="请输入图书简介"
          />
        </el-form-item>
        <el-form-item label="封面上传">
          <div class="upload-area">
            <el-upload
              v-if="bookForm.id"
              class="cover-upload"
              :action="'/api/files/upload'"
              :headers="uploadHeaders"
              :data="uploadCoverData"
              :show-file-list="false"
              :on-success="handleCoverSuccess"
              :before-upload="beforeCoverUpload"
              accept="image/jpeg,image/png,image/gif"
            >
              <div v-if="bookForm.coverUrl" class="cover-preview">
                <img :src="bookForm.coverUrl" alt="封面" />
                <div class="cover-mask">
                  <el-icon><Plus /></el-icon>
                  <span>更换封面</span>
                </div>
              </div>
              <div v-else class="cover-placeholder">
                <el-icon :size="32"><Plus /></el-icon>
                <span>上传封面</span>
              </div>
            </el-upload>
            <div v-else class="cover-placeholder-hint">
              <el-icon :size="24"><InfoFilled /></el-icon>
              <span>请先保存图书后再上传封面</span>
            </div>
            <div class="upload-tip">支持 JPG/PNG/GIF，不超过10MB</div>
          </div>
        </el-form-item>
        <el-form-item label="PDF绘本" v-if="bookForm.id">
          <el-upload
            class="pdf-upload"
            :action="'/api/files/upload'"
            :headers="uploadHeaders"
            :data="uploadPdfData"
            :on-success="handlePdfSuccess"
            :before-upload="beforePdfUpload"
            :file-list="pdfFileList"
            accept="application/pdf"
            :limit="5"
          >
            <el-button type="primary" plain>
              <el-icon><Upload /></el-icon>
              上传PDF
            </el-button>
            <template #tip>
              <div class="upload-tip">支持PDF格式，单个不超过50MB，最多5个</div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button v-permission="['BOOK_CREATE', 'BOOK_UPDATE']" type="primary" :loading="submitLoading" @click="handleSubmit">
          {{ submitLoading ? '保存中...' : '确认保存' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { usePermission } from '@/composables/usePermission'
import { getBooks, addBook, updateBook, deleteBook, getCategories, getAllCategories, getBookFiles } from '@/api'

const { checkWithFeedback } = usePermission()
const route = useRoute()
const tableLoading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const bookFormRef = ref(null)

const searchKeyword = ref('')
const searchCategory = ref('')
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const bookList = ref([])
const categories = ref([])
const categoryDetails = ref([])
const pdfFileList = ref([])

const uploadHeaders = computed(() => ({
  Authorization: 'Bearer ' + localStorage.getItem('token')
}))

const uploadCoverData = computed(() => ({
  fileType: 'cover',
  bookId: bookForm.id
}))

const uploadPdfData = computed(() => ({
  fileType: 'pdf',
  bookId: bookForm.id
}))

const bookForm = reactive({
  id: null,
  title: '',
  author: '',
  publisher: '',
  isbn: '',
  category: '',
  ageRange: '',
  price: 0,
  stock: 0,
  coverUrl: '',
  description: '',
  status: 1
})

const bookRules = {
  title: [{ required: true, message: '请输入书名', trigger: 'blur' }],
  author: [{ required: true, message: '请输入作者', trigger: 'blur' }],
  stock: [{ type: 'number', min: 0, message: '库存不能为负数', trigger: 'change' }]
}

const categoryTypes = ['', 'success', 'warning', 'danger', 'info']
const getCategoryType = (category) => {
  if (!category) return 'info'
  const index = category.length % categoryTypes.length
  return categoryTypes[index]
}

const fetchBooks = async () => {
  tableLoading.value = true
  try {
    const res = await getBooks({
      page: page.value,
      size: pageSize.value,
      keyword: searchKeyword.value,
      category: searchCategory.value
    })
    bookList.value = res.data.records
    total.value = res.data.total
  } catch (e) {
    // handled by interceptor
  } finally {
    tableLoading.value = false
  }
}

const fetchCategories = async () => {
  try {
    const res = await getCategories()
    const list = res.data || []
    categories.value = list.map(c => typeof c === 'string' ? c : c?.name).filter(Boolean)
  } catch (e) {}
  try {
    const res = await getAllCategories()
    const list = res.data || []
    categoryDetails.value = list.filter(c => c && typeof c === 'object' && c.name)
  } catch (e) {}
}

const getCategoryColor = (categoryName) => {
  const cat = categoryDetails.value.find(c => c.name === categoryName)
  return cat ? cat.color : null
}

const getCategoryIcon = (categoryName) => {
  const cat = categoryDetails.value.find(c => c.name === categoryName)
  return cat ? cat.icon : null
}

const handleSearch = () => {
  page.value = 1
  fetchBooks()
}

const resetForm = () => {
  Object.assign(bookForm, {
    id: null, title: '', author: '', publisher: '', isbn: '',
    category: '', ageRange: '', price: 0, stock: 0,
    coverUrl: '', description: '', status: 1
  })
}

const handleAdd = () => {
  if (!checkWithFeedback('BOOK_CREATE')) return
  isEdit.value = false
  resetForm()
  pdfFileList.value = []
  dialogVisible.value = true
}

const handleEdit = (row) => {
  if (!checkWithFeedback('BOOK_UPDATE')) return
  isEdit.value = true
  Object.assign(bookForm, { ...row })
  pdfFileList.value = []
  dialogVisible.value = true
  if (row.id) {
    loadBookFiles(row.id)
  }
}

const loadBookFiles = async (bookId) => {
  try {
    const res = await getBookFiles(bookId)
    const files = res.data || []
    pdfFileList.value = files
      .filter(f => f.fileType === 'pdf')
      .map(f => ({ name: f.originalName, url: '/uploads/' + f.filePath }))
  } catch (e) {}
}

const handleCoverSuccess = (response) => {
  if (response.code === 200 && response.data) {
    bookForm.coverUrl = '/uploads/' + response.data.filePath
    ElMessage.success('封面上传成功')
  } else {
    ElMessage.error(response.message || '上传失败')
  }
}

const beforeCoverUpload = (file) => {
  const isImage = ['image/jpeg', 'image/png', 'image/gif'].includes(file.type)
  const isLt10M = file.size / 1024 / 1024 < 10
  if (!isImage) {
    ElMessage.error('仅支持 JPG/PNG/GIF 格式的图片')
    return false
  }
  if (!isLt10M) {
    ElMessage.error('图片大小不能超过10MB')
    return false
  }
  return true
}

const handlePdfSuccess = (response) => {
  if (response.code === 200) {
    ElMessage.success('PDF上传成功')
  } else {
    ElMessage.error(response.message || '上传失败')
  }
}

const beforePdfUpload = (file) => {
  const isPdf = file.type === 'application/pdf'
  const isLt50M = file.size / 1024 / 1024 < 50
  if (!isPdf) {
    ElMessage.error('仅支持PDF格式')
    return false
  }
  if (!isLt50M) {
    ElMessage.error('文件大小不能超过50MB')
    return false
  }
  return true
}

const handleSubmit = async () => {
  if (!checkWithFeedback(isEdit.value ? 'BOOK_UPDATE' : 'BOOK_CREATE')) return
  const valid = await bookFormRef.value.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    if (isEdit.value) {
      await updateBook(bookForm.id, bookForm)
      ElMessage.success('图书更新成功！')
      dialogVisible.value = false
    } else {
      const res = await addBook(bookForm)
      const newId = res.data?.id
      if (newId) {
        bookForm.id = newId
        isEdit.value = true
        ElMessage.success('图书添加成功！现在可以上传封面和PDF了')
      } else {
        ElMessage.success('图书添加成功！')
        dialogVisible.value = false
      }
    }
    fetchBooks()
    fetchCategories()
  } catch (e) {
    // handled by interceptor
  } finally {
    submitLoading.value = false
  }
}

const handleDelete = (row) => {
  if (!checkWithFeedback('BOOK_DELETE')) return
  ElMessageBox.confirm(
    `确定要删除《${row.title}》吗？此操作不可恢复。`,
    '删除确认',
    {
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
      type: 'warning',
      confirmButtonClass: 'el-button--danger'
    }
  ).then(async () => {
    tableLoading.value = true
    try {
      await deleteBook(row.id)
      ElMessage.success('删除成功！')
      fetchBooks()
      fetchCategories()
    } catch (e) {
      // handled by interceptor
    } finally {
      tableLoading.value = false
    }
  }).catch(() => {})
}

const handleExport = async () => {
  if (!checkWithFeedback('BOOK_EXPORT')) return
  try {
    const res = await getBooks({ page: 1, size: 10000, keyword: searchKeyword.value, category: searchCategory.value })
    const books = res.data?.records || res.data || []
    if (books.length === 0) {
      ElMessage.warning('暂无数据可导出')
      return
    }
    const headers = ['书名', '作者', '分类', '适读年龄', '价格', '库存', '状态']
    const rows = books.map(b => [b.title, b.author, b.category, b.ageRange, b.price, b.stock, b.status === 1 ? '上架' : '下架'])
    const csv = [headers.join(','), ...rows.map(r => r.join(','))].join('\n')
    const blob = new Blob(['﻿' + csv], { type: 'text/csv;charset=utf-8;' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `图书列表_${new Date().toLocaleDateString()}.csv`
    a.click()
    URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch (e) {
    // handled by interceptor
  }
}

onMounted(() => {
  if (route.query.category) {
    searchCategory.value = route.query.category
  }
  fetchBooks()
  fetchCategories()
})
</script>

<style scoped>
.books-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

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
  min-width: 240px;
}

.category-select {
  width: 160px;
}

.table-card {
  background: white;
}

.book-title-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.book-emoji {
  font-size: 18px;
}

.price {
  color: #E74C3C;
  font-weight: 600;
}

.category-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  border-radius: 20px;
  background: #F5F5F5;
  border: 1.5px solid #E0E0E0;
  font-size: 13px;
  line-height: 1.4;
}

.category-badge-icon {
  font-size: 13px;
}

.category-badge-text {
  color: var(--text-primary);
  font-weight: 500;
}

.table-row {
  transition: all 0.3s ease;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid #F5F5F5;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 20px;
}

.book-dialog :deep(.el-dialog__header) {
  background: linear-gradient(135deg, var(--green-light), var(--blue-light));
  margin-right: 0;
  padding: 20px 24px;
}

.book-dialog :deep(.el-dialog__title) {
  font-weight: 600;
  font-size: 18px;
}

.action-btn-edit {
  background: linear-gradient(135deg, var(--btn-edit-from), var(--btn-edit-to)) !important;
  border: none !important;
  color: #fff !important;
  font-size: 13px;
  padding: 6px 14px !important;
  height: 32px;
}

.action-btn-edit:hover {
  background: linear-gradient(135deg, var(--btn-edit-to), var(--btn-edit-from)) !important;
  box-shadow: 0 3px 8px rgba(167, 139, 250, 0.3);
}

.action-btn-delete {
  background: linear-gradient(135deg, var(--btn-delete-from), var(--btn-delete-to)) !important;
  border: none !important;
  color: #fff !important;
  font-size: 13px;
  padding: 6px 14px !important;
  height: 32px;
}

.action-btn-delete:hover {
  background: linear-gradient(135deg, var(--btn-delete-to), var(--btn-delete-from)) !important;
  box-shadow: 0 3px 8px rgba(255, 179, 186, 0.4);
}

.upload-area {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.cover-upload :deep(.el-upload) {
  border: 2px dashed #E0E5F5;
  border-radius: 12px;
  cursor: pointer;
  overflow: hidden;
  transition: all 0.3s ease;
}

.cover-upload :deep(.el-upload:hover) {
  border-color: var(--purple);
  box-shadow: 0 4px 12px rgba(149, 125, 173, 0.2);
}

.cover-preview {
  position: relative;
  width: 120px;
  height: 160px;
}

.cover-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cover-mask {
  position: absolute;
  inset: 0;
  background: rgba(0,0,0,0.5);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  color: #fff;
  opacity: 0;
  transition: opacity 0.3s ease;
  font-size: 12px;
}

.cover-preview:hover .cover-mask {
  opacity: 1;
}

.cover-placeholder {
  width: 120px;
  height: 160px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #999;
  font-size: 13px;
}

.upload-tip {
  font-size: 12px;
  color: #999;
}

.pdf-upload :deep(.el-upload-list__item) {
  border-radius: 8px;
  transition: all 0.3s ease;
}
</style>
