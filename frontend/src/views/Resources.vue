<template>
  <div class="resources-page">
    <el-card class="header-card animate__animated animate__fadeInDown">
      <div class="page-header">
        <div class="header-info">
          <h2>📁 资源管理</h2>
          <p>管理图书封面、PDF绘本等资源文件</p>
        </div>
        <div class="header-actions">
          <el-select
            v-model="uploadBookId"
            placeholder="关联图书(可选)"
            clearable
            filterable
            remote
            :remote-method="searchBooks"
            :loading="bookSearchLoading"
            style="width: 200px; margin-right: 12px;"
          >
            <el-option
              v-for="book in bookOptions"
              :key="book.id"
              :label="book.title"
              :value="book.id"
            />
          </el-select>
          <el-upload
            :action="'/api/files/upload'"
            :headers="uploadHeaders"
            :data="uploadData"
            :show-file-list="false"
            :on-success="handleUploadSuccess"
            :before-upload="beforeUpload"
            multiple
          >
            <el-button v-permission="'FILE_CREATE'" type="primary" class="upload-btn">
              <el-icon><Upload /></el-icon>
              上传资源
            </el-button>
          </el-upload>
        </div>
      </div>
    </el-card>

    <el-card class="filter-card animate__animated animate__fadeInUp">
      <div class="filter-bar">
        <el-select v-model="filterType" placeholder="全部类型" clearable @change="fetchResources">
          <el-option label="封面图片" value="cover" />
          <el-option label="PDF绘本" value="pdf" />
          <el-option label="其他" value="other" />
        </el-select>
        <el-select v-model="uploadType" placeholder="上传类型" style="width: 120px;">
          <el-option label="封面" value="cover" />
          <el-option label="PDF" value="pdf" />
          <el-option label="其他" value="other" />
        </el-select>
        <el-input
          v-model="filterBookId"
          placeholder="图书ID筛选"
          clearable
          style="width: 160px;"
          @clear="fetchResources"
          @keyup.enter="fetchResources"
        />
        <el-button @click="fetchResources">
          <el-icon><Search /></el-icon>
          筛选
        </el-button>
      </div>
    </el-card>

    <el-card class="table-card" v-loading="loading">
      <el-table :data="resources" stripe style="width: 100%">
        <el-table-column label="预览" width="100">
          <template #default="{ row }">
            <div class="preview-cell" @click="handlePreview(row)">
              <img
                v-if="row.fileType === 'cover'"
                :src="'/uploads/' + row.filePath"
                class="preview-thumb"
                alt="预览"
              />
              <div v-else class="file-icon">
                <span v-if="row.fileType === 'pdf'">📄</span>
                <span v-else>📎</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="originalName" label="文件名" min-width="200" show-overflow-tooltip />
        <el-table-column label="类型" width="100">
          <template #default="{ row }">
            <el-tag
              :type="row.fileType === 'cover' ? 'success' : row.fileType === 'pdf' ? 'warning' : 'info'"
              size="small"
              class="type-tag"
            >
              {{ row.fileType === 'cover' ? '封面' : row.fileType === 'pdf' ? 'PDF' : '其他' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="大小" width="100">
          <template #default="{ row }">
            {{ formatSize(row.fileSize) }}
          </template>
        </el-table-column>
        <el-table-column prop="bookId" label="关联图书" width="140">
          <template #default="{ row }">
            <span v-if="row.bookId" class="book-link">ID: {{ row.bookId }}</span>
            <el-button v-else size="small" type="warning" plain @click="openLinkDialog(row)">
              关联图书
            </el-button>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="上传时间" width="170" />
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" plain @click="handlePreview(row)">
              预览
            </el-button>
            <el-button v-permission="'FILE_DELETE'" size="small" type="danger" plain @click="handleDelete(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          background
          @current-change="fetchResources"
          @size-change="fetchResources"
        />
      </div>
    </el-card>

    <!-- 预览弹窗 -->
    <el-dialog v-model="previewVisible" title="资源预览" width="700px" class="preview-dialog">
      <div class="preview-content">
        <img
          v-if="previewResource && previewResource.fileType === 'cover'"
          :src="'/uploads/' + previewResource.filePath"
          class="preview-image"
          alt="预览"
        />
        <iframe
          v-else-if="previewResource && previewResource.fileType === 'pdf'"
          :src="'/uploads/' + previewResource.filePath"
          class="preview-pdf"
        ></iframe>
        <div v-else class="preview-placeholder">
          <p>📎 {{ previewResource?.originalName }}</p>
          <el-button type="primary" @click="downloadFile">下载文件</el-button>
        </div>
      </div>
    </el-dialog>

    <!-- 关联图书弹窗 -->
    <el-dialog v-model="linkDialogVisible" title="关联图书" width="420px" class="link-dialog">
      <div class="link-form">
        <p class="link-hint">将资源「{{ linkResource?.originalName }}」关联到图书：</p>
        <el-select
          v-model="linkBookId"
          placeholder="搜索并选择图书"
          filterable
          remote
          :remote-method="searchBooks"
          :loading="bookSearchLoading"
          style="width: 100%;"
        >
          <el-option
            v-for="book in bookOptions"
            :key="book.id"
            :label="book.title + ' - ' + book.author"
            :value="book.id"
          />
        </el-select>
      </div>
      <template #footer>
        <el-button @click="linkDialogVisible = false">取消</el-button>
        <el-button type="primary" :disabled="!linkBookId" @click="confirmLink">确认关联</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getFileList, deleteFile, getBooks } from '@/api'

const loading = ref(false)
const resources = ref([])
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const filterType = ref('')
const filterBookId = ref('')
const uploadType = ref('cover')
const uploadBookId = ref(null)
const previewVisible = ref(false)
const previewResource = ref(null)

const bookOptions = ref([])
const bookSearchLoading = ref(false)

const linkDialogVisible = ref(false)
const linkResource = ref(null)
const linkBookId = ref(null)

const uploadHeaders = computed(() => ({
  Authorization: 'Bearer ' + localStorage.getItem('token')
}))

const uploadData = computed(() => {
  const data = { fileType: uploadType.value }
  if (uploadBookId.value) {
    data.bookId = uploadBookId.value
  }
  return data
})

const searchBooks = async (query) => {
  if (!query) {
    bookOptions.value = []
    return
  }
  bookSearchLoading.value = true
  try {
    const res = await getBooks({ page: 1, size: 20, keyword: query })
    bookOptions.value = res.data.records || []
  } catch (e) {
    bookOptions.value = []
  } finally {
    bookSearchLoading.value = false
  }
}

const openLinkDialog = (row) => {
  linkResource.value = row
  linkBookId.value = null
  linkDialogVisible.value = true
}

const confirmLink = async () => {
  if (!linkResource.value || !linkBookId.value) return
  try {
    await linkResourceToBook(linkResource.value.id, linkBookId.value)
    ElMessage.success('关联成功')
    linkDialogVisible.value = false
    fetchResources()
  } catch (e) {
    ElMessage.error('关联失败')
  }
}

const linkResourceToBook = async (resourceId, bookId) => {
  const { default: request } = await import('@/utils/request')
  return request.put(`/files/${resourceId}/link`, { bookId })
}

const fetchResources = async () => {
  loading.value = true
  try {
    const params = { page: page.value, size: pageSize.value }
    if (filterType.value) params.fileType = filterType.value
    if (filterBookId.value) params.bookId = filterBookId.value
    const res = await getFileList(params)
    resources.value = res.data.records || []
    total.value = res.data.total || 0
  } catch (e) {} finally {
    loading.value = false
  }
}

const formatSize = (bytes) => {
  if (!bytes) return '0 B'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

const handlePreview = (row) => {
  previewResource.value = row
  previewVisible.value = true
}

const downloadFile = () => {
  if (previewResource.value) {
    window.open('/uploads/' + previewResource.value.filePath, '_blank')
  }
}

const handleDelete = (row) => {
  ElMessageBox.confirm(
    `确定删除文件「${row.originalName}」吗？`,
    '删除确认',
    { type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消' }
  ).then(async () => {
    try {
      await deleteFile(row.id)
      ElMessage.success('删除成功')
      fetchResources()
    } catch (e) {}
  }).catch(() => {})
}

const handleUploadSuccess = (response) => {
  if (response.code === 200) {
    ElMessage.success('上传成功')
    fetchResources()
  } else {
    ElMessage.error(response.message || '上传失败')
  }
}

const beforeUpload = (file) => {
  const isLt50M = file.size / 1024 / 1024 < 50
  if (!isLt50M) {
    ElMessage.error('文件大小不能超过50MB')
    return false
  }
  return true
}

onMounted(() => {
  fetchResources()
})
</script>

<style scoped>
.resources-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.header-card {
  background: linear-gradient(135deg, #E0E5F5, #D4F5E9);
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-info h2 {
  margin: 0 0 4px 0;
  font-size: 22px;
  color: var(--text-primary);
}

.header-info p {
  margin: 0;
  color: var(--text-secondary);
  font-size: 14px;
}

.upload-btn {
  background: linear-gradient(135deg, var(--btn-upload-from), var(--btn-upload-to)) !important;
  border: none !important;
  color: #fff !important;
  padding: 12px 24px !important;
  font-size: 15px;
  border-radius: 12px !important;
  transition: all 0.3s ease;
}

.upload-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(124, 92, 252, 0.4);
}

.filter-card {
  background: white;
}

.filter-bar {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}

.table-card {
  background: white;
}

.preview-cell {
  cursor: pointer;
  transition: transform 0.3s ease;
}

.preview-cell:hover {
  transform: scale(1.1);
}

.preview-thumb {
  width: 50px;
  height: 65px;
  object-fit: cover;
  border-radius: 6px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.file-icon {
  width: 50px;
  height: 65px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #F5F5F5;
  border-radius: 6px;
  font-size: 24px;
}

.type-tag {
  border-radius: 12px !important;
}

.text-muted {
  color: #999;
  font-size: 12px;
}

.book-link {
  color: var(--purple, #7C5CFC);
  font-weight: 500;
  font-size: 13px;
}

.link-dialog :deep(.el-dialog__header) {
  background: linear-gradient(135deg, var(--yellow-warm, #FFF3CD), var(--green-light, #D4F5E9));
  padding: 16px 20px;
}

.link-form {
  padding: 8px 0;
}

.link-hint {
  margin: 0 0 16px;
  font-size: 14px;
  color: var(--text-secondary, #666);
}

.header-actions {
  display: flex;
  align-items: center;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid #F5F5F5;
}

.preview-dialog :deep(.el-dialog__header) {
  background: linear-gradient(135deg, var(--blue-light), var(--purple-light));
  padding: 16px 20px;
}

.preview-content {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 300px;
}

.preview-image {
  max-width: 100%;
  max-height: 500px;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0,0,0,0.1);
}

.preview-pdf {
  width: 100%;
  height: 500px;
  border: none;
  border-radius: 8px;
}

.preview-placeholder {
  text-align: center;
  padding: 40px;
  color: var(--text-secondary);
  font-size: 18px;
}
</style>
