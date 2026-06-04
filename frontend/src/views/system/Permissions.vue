<template>
  <div class="permissions-page">
    <!-- 统计卡片区域 -->
    <div class="stat-row animate__animated animate__fadeInDown">
      <div class="mini-stat" style="--accent: var(--purple)">
        <span class="mini-stat-icon">🔑</span>
        <div class="mini-stat-info">
          <span class="mini-stat-value">{{ permStats.total }}</span>
          <span class="mini-stat-label">权限总数</span>
        </div>
      </div>
      <div class="mini-stat" style="--accent: var(--green)">
        <span class="mini-stat-icon">📋</span>
        <div class="mini-stat-info">
          <span class="mini-stat-value">{{ permStats.menuCount }}</span>
          <span class="mini-stat-label">菜单权限</span>
        </div>
      </div>
      <div class="mini-stat" style="--accent: var(--blue)">
        <span class="mini-stat-icon">🔘</span>
        <div class="mini-stat-info">
          <span class="mini-stat-value">{{ permStats.buttonCount }}</span>
          <span class="mini-stat-label">按钮权限</span>
        </div>
      </div>
      <div class="mini-stat" style="--accent: var(--pink)">
        <span class="mini-stat-icon">📦</span>
        <div class="mini-stat-info">
          <span class="mini-stat-value">{{ permStats.modules }}</span>
          <span class="mini-stat-label">功能模块</span>
        </div>
      </div>
    </div>

    <!-- 搜索栏 -->
    <el-card class="search-card animate__animated animate__fadeInDown" style="animation-delay: 0.1s">
      <div class="search-bar">
        <el-select v-model="selectedModule" placeholder="全部模块" clearable size="large" class="module-select" @change="filterPermissions">
          <el-option v-for="mod in modules" :key="mod" :label="mod" :value="mod" />
        </el-select>
        <el-select v-model="selectedType" placeholder="全部类型" clearable size="large" class="type-select" @change="filterPermissions">
          <el-option label="菜单权限" value="menu" />
          <el-option label="按钮权限" value="button" />
        </el-select>
        <el-input
          v-model="searchKeyword"
          placeholder="搜索权限名称或编码..."
          prefix-icon="Search"
          size="large"
          clearable
          class="search-input"
          @input="filterPermissions"
          @keyup.enter="filterPermissions"
          @clear="filterPermissions"
        />
        <el-button type="primary" size="large" @click="filterPermissions">
          <el-icon><Search /></el-icon>
          搜索
        </el-button>
        <el-button v-permission="'PERMISSION_MANAGE'" type="success" size="large" @click="handleAdd">
          <el-icon><Plus /></el-icon>
          新增权限
        </el-button>
      </div>
    </el-card>

    <!-- 权限模块卡片网格 -->
    <div class="perm-modules" v-loading="loading" element-loading-text="加载权限数据中...">
      <transition-group name="card-list" tag="div" class="modules-grid">
        <el-card
          v-for="(perms, module, index) in filteredGroups"
          :key="module"
          class="module-card animate__animated animate__fadeInUp"
          :style="{ animationDelay: `${index * 0.05}s` }"
        >
          <template #header>
            <div class="module-header">
              <div class="module-title-area">
                <span class="module-icon">📋</span>
                <span class="module-title">{{ module }}</span>
              </div>
              <div class="module-header-right">
                <el-tag size="small" type="success" effect="light" v-if="getMenuCount(perms)">{{ getMenuCount(perms) }} 菜单</el-tag>
                <el-tag size="small" type="warning" effect="light" v-if="getButtonCount(perms)">{{ getButtonCount(perms) }} 按钮</el-tag>
              </div>
            </div>
          </template>
          <div class="perm-list">
            <div v-for="p in perms" :key="p.id" class="perm-item">
              <div class="perm-item-top">
                <div class="perm-item-left">
                  <span class="perm-dot" :class="p.type === 'menu' ? 'dot-menu' : 'dot-button'"></span>
                  <span class="perm-name">{{ p.name }}</span>
                  <el-tag size="small" :type="p.type === 'menu' ? 'success' : 'warning'" class="perm-type-badge">
                    {{ p.type === 'menu' ? '菜单' : '按钮' }}
                  </el-tag>
                  <el-tag v-if="p.builtIn" size="small" type="info" effect="plain" class="perm-builtin-tag">内置</el-tag>
                </div>
                <div class="perm-item-actions">
                  <el-tag size="small" class="perm-code-tag" effect="plain">{{ p.code }}</el-tag>
                  <el-button v-permission="'PERMISSION_MANAGE'" size="small" class="action-btn-edit" @click="handleEdit(p)">
                    <el-icon><Edit /></el-icon>
                  </el-button>
                  <el-button v-permission="'PERMISSION_MANAGE'" size="small" class="action-btn-delete" :disabled="p.builtIn" @click="handleDelete(p)">
                    <el-icon><Delete /></el-icon>
                  </el-button>
                </div>
              </div>
              <div class="perm-item-roles" v-if="p.roles && p.roles.length > 0">
                <span class="perm-roles-label">关联角色：</span>
                <el-tag
                  v-for="role in p.roles" :key="role.id"
                  size="small"
                  :type="role.level >= 100 ? 'danger' : role.level >= 50 ? 'warning' : 'success'"
                  effect="light"
                  class="perm-role-tag"
                >
                  {{ role.name }}
                </el-tag>
              </div>
              <div class="perm-item-roles" v-else>
                <span class="perm-roles-label perm-roles-none">暂无角色关联</span>
              </div>
            </div>
          </div>
        </el-card>
      </transition-group>
    </div>

    <el-empty v-if="!loading && Object.keys(filteredGroups).length === 0" description="未找到匹配的权限">
      <el-button type="primary" @click="clearFilters">清空筛选</el-button>
    </el-empty>

    <!-- 新增/编辑 弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="editingPerm ? '编辑权限' : '新增权限'"
      width="520px"
      :close-on-click-modal="false"
      destroy-on-close
      class="perm-dialog"
    >
      <el-form ref="formRef" :model="permForm" :rules="formRules" label-width="90px" class="perm-form">
        <el-form-item label="权限编码" prop="code">
          <el-input
            v-model="permForm.code"
            placeholder="如 BOOK_EXPORT（大写英文+下划线）"
            :disabled="editingPerm && editingPerm.builtIn"
            @input="permForm.code = permForm.code.toUpperCase()"
          />
        </el-form-item>
        <el-form-item label="权限名称" prop="name">
          <el-input v-model="permForm.name" placeholder="如 导出图书" maxlength="40" show-word-limit />
        </el-form-item>
        <el-form-item label="所属模块" prop="module">
          <el-select v-model="permForm.module" filterable allow-create placeholder="选择或输入模块" style="width: 100%">
            <el-option v-for="mod in modules" :key="mod" :label="mod" :value="mod" />
          </el-select>
        </el-form-item>
        <el-form-item label="权限类型" prop="type">
          <el-radio-group v-model="permForm.type">
            <el-radio value="menu">菜单权限</el-radio>
            <el-radio value="button">按钮权限</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="permForm.description" type="textarea" :rows="2" placeholder="权限用途说明" maxlength="200" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm" :loading="submitLoading">
          {{ submitLoading ? '保存中...' : '确认保存' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getPermissionsWithRoles, getPermissionModules, createPermission, updatePermission, deletePermission } from '@/api'

const loading = ref(false)
const allPermissions = ref([])
const modules = ref([])
const selectedModule = ref('')
const selectedType = ref('')
const searchKeyword = ref('')
const groupedPermissions = ref({})

const permStats = computed(() => {
  const perms = allPermissions.value
  const totalModules = modules.value.length
  return {
    total: perms.length,
    menuCount: perms.filter(p => p.type === 'menu').length,
    buttonCount: perms.filter(p => p.type === 'button').length,
    modules: totalModules
  }
})

function getMenuCount(perms) {
  return perms.filter(p => p.type === 'menu').length
}

function getButtonCount(perms) {
  return perms.filter(p => p.type === 'button').length
}

// Dialog state
const dialogVisible = ref(false)
const editingPerm = ref(null)
const submitLoading = ref(false)
const formRef = ref(null)
const permForm = ref({ code: '', name: '', module: '', type: 'button', description: '' })
const formRules = {
  code: [{ required: true, message: '请输入权限编码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入权限名称', trigger: 'blur' }],
  module: [{ required: true, message: '请选择所属模块', trigger: 'change' }],
  type: [{ required: true, message: '请选择权限类型', trigger: 'change' }]
}

onMounted(() => {
  loadData()
})

async function loadData() {
  loading.value = true
  try {
    const [permRes, modRes] = await Promise.all([getPermissionsWithRoles(), getPermissionModules()])
    allPermissions.value = permRes.data
    modules.value = modRes.data

    const grouped = {}
    permRes.data.forEach(p => {
      const mod = p.module || '其他'
      if (!grouped[mod]) grouped[mod] = []
      grouped[mod].push(p)
    })
    groupedPermissions.value = grouped
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const filteredGroups = computed(() => {
  let result = { ...groupedPermissions.value }

  if (selectedModule.value) {
    const filtered = {}
    if (result[selectedModule.value]) {
      filtered[selectedModule.value] = result[selectedModule.value]
    }
    result = filtered
  }

  if (selectedType.value) {
    const filtered = {}
    for (const [mod, perms] of Object.entries(result)) {
      const matched = perms.filter(p => p.type === selectedType.value)
      if (matched.length > 0) filtered[mod] = matched
    }
    result = filtered
  }

  if (searchKeyword.value) {
    const kw = searchKeyword.value.toLowerCase()
    const filtered = {}
    for (const [mod, perms] of Object.entries(result)) {
      const matched = perms.filter(p =>
        p.name.toLowerCase().includes(kw) || p.code.toLowerCase().includes(kw)
      )
      if (matched.length > 0) filtered[mod] = matched
    }
    result = filtered
  }

  return result
})

function filterPermissions() {}

function clearFilters() {
  selectedModule.value = ''
  selectedType.value = ''
  searchKeyword.value = ''
}

function handleAdd() {
  editingPerm.value = null
  permForm.value = { code: '', name: '', module: '', type: 'button', description: '' }
  dialogVisible.value = true
}

function handleEdit(perm) {
  editingPerm.value = perm
  permForm.value = {
    code: perm.code,
    name: perm.name,
    module: perm.module || '',
    type: perm.type || 'button',
    description: perm.description || ''
  }
  dialogVisible.value = true
}

async function submitForm() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitLoading.value = true
  try {
    if (editingPerm.value) {
      await updatePermission(editingPerm.value.id, permForm.value)
      ElMessage.success('权限更新成功')
    } else {
      await createPermission(permForm.value)
      ElMessage.success('权限创建成功')
    }
    dialogVisible.value = false
    loadData()
  } catch (e) {
    console.error(e)
  } finally {
    submitLoading.value = false
  }
}

async function handleDelete(perm) {
  if (perm.builtIn) {
    ElMessage.warning('内置权限不可删除')
    return
  }
  await ElMessageBox.confirm(`确定要删除权限「${perm.name}」(${perm.code}) 吗？删除后将移除所有角色的该权限关联。`, '警告', { type: 'warning' })
  try {
    await deletePermission(perm.id)
    ElMessage.success('删除成功')
    loadData()
  } catch (e) {
    console.error(e)
  }
}
</script>

<style scoped>
.permissions-page {
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

.module-select {
  width: 160px;
}

.type-select {
  width: 140px;
}

/* 权限模块网格 */
.perm-modules {
  min-height: 200px;
}

.modules-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(480px, 1fr));
  gap: 20px;
}

.module-card {
  transition: all 0.35s cubic-bezier(0.4, 0, 0.2, 1);
}

.module-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 30px rgba(0, 0, 0, 0.1) !important;
}

.module-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.module-title-area {
  display: flex;
  align-items: center;
  gap: 8px;
}

.module-header-right {
  display: flex;
  gap: 6px;
}

.module-icon {
  font-size: 18px;
}

.module-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}

.perm-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.perm-item {
  padding: 12px 14px;
  background: var(--bg-main);
  border-radius: var(--radius-sm);
  transition: all 0.25s ease;
}

.perm-item:hover {
  background: linear-gradient(135deg, var(--green-light), var(--blue-light));
  transform: translateX(4px);
}

.perm-item-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.perm-item-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.perm-item-actions {
  display: flex;
  align-items: center;
  gap: 6px;
}

.perm-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.perm-dot.dot-menu {
  background: var(--green);
  box-shadow: 0 0 6px rgba(181, 234, 215, 0.6);
}

.perm-dot.dot-button {
  background: var(--purple);
  box-shadow: 0 0 6px rgba(149, 125, 173, 0.4);
}

.perm-name {
  font-size: 14px;
  color: var(--text-primary);
  font-weight: 500;
}

.perm-type-badge {
  font-size: 10px !important;
  transform: scale(0.9);
}

.perm-builtin-tag {
  font-size: 10px !important;
  transform: scale(0.85);
}

.perm-code-tag {
  font-family: monospace;
  font-size: 11px;
}

.perm-item-roles {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
  margin-top: 4px;
}

.perm-roles-label {
  font-size: 11px;
  color: var(--text-secondary);
}

.perm-roles-none {
  font-style: italic;
  opacity: 0.6;
}

.perm-role-tag {
  font-size: 10px !important;
  height: 18px !important;
  line-height: 18px !important;
  padding: 0 6px !important;
}

.action-btn-edit {
  background: linear-gradient(135deg, var(--btn-edit-from), var(--btn-edit-to)) !important;
  border: none !important;
  color: #fff !important;
  font-size: 12px;
  padding: 4px 8px !important;
  height: 26px;
  width: 26px;
}

.action-btn-edit:hover {
  box-shadow: 0 3px 8px rgba(167, 139, 250, 0.3);
}

.action-btn-delete {
  background: linear-gradient(135deg, var(--btn-delete-from), var(--btn-delete-to)) !important;
  border: none !important;
  color: #fff !important;
  font-size: 12px;
  padding: 4px 8px !important;
  height: 26px;
  width: 26px;
}

.action-btn-delete:hover {
  box-shadow: 0 3px 8px rgba(255, 179, 186, 0.4);
}

.action-btn-delete:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

/* 弹窗 */
.perm-dialog :deep(.el-dialog__header) {
  background: linear-gradient(135deg, var(--purple-light), var(--blue-light));
  margin-right: 0;
  padding: 20px 24px;
}

.perm-dialog :deep(.el-dialog__title) {
  font-weight: 600;
  font-size: 18px;
}

.perm-form {
  padding: 8px 0;
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
  transform: scale(0.95);
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
  .modules-grid {
    grid-template-columns: 1fr;
  }
}
</style>
