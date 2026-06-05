<template>
  <div class="user-roles-page">
    <!-- 统计卡片区域 -->
    <div class="stat-row animate__animated animate__fadeInDown">
      <div class="mini-stat" style="--accent: var(--purple)">
        <span class="mini-stat-icon">👤</span>
        <div class="mini-stat-info">
          <span class="mini-stat-value">{{ stats.totalUsers }}</span>
          <span class="mini-stat-label">用户总数</span>
        </div>
      </div>
      <div class="mini-stat" style="--accent: var(--green)">
        <span class="mini-stat-icon">🛡️</span>
        <div class="mini-stat-info">
          <span class="mini-stat-value">{{ stats.admins }}</span>
          <span class="mini-stat-label">管理员</span>
        </div>
      </div>
      <div class="mini-stat" style="--accent: var(--blue)">
        <span class="mini-stat-icon">📖</span>
        <div class="mini-stat-info">
          <span class="mini-stat-value">{{ stats.readers }}</span>
          <span class="mini-stat-label">读者</span>
        </div>
      </div>
      <div class="mini-stat" style="--accent: var(--pink)">
        <span class="mini-stat-icon">⚠️</span>
        <div class="mini-stat-info">
          <span class="mini-stat-value">{{ stats.noRole }}</span>
          <span class="mini-stat-label">无角色用户</span>
        </div>
      </div>
    </div>

    <!-- 搜索与操作栏 -->
    <el-card class="search-card animate__animated animate__fadeInDown" style="animation-delay: 0.1s">
      <div class="search-bar">
        <el-select v-model="filterType" placeholder="全部类型" clearable size="large" class="type-select" @change="loadUsers">
          <el-option label="管理员" value="ADMIN" />
          <el-option label="读者" value="READER" />
        </el-select>
        <el-select v-model="filterRoleStatus" placeholder="角色筛选" clearable size="large" class="type-select" @change="applyLocalFilter">
          <el-option label="无角色用户" value="no_role" />
          <el-option label="多角色用户" value="multi_role" />
        </el-select>
        <el-input
          v-model="searchKeyword"
          placeholder="搜索用户名或昵称..."
          prefix-icon="Search"
          size="large"
          clearable
          class="search-input"
          @keyup.enter="loadUsers"
          @clear="loadUsers"
        />
        <el-button type="primary" size="large" @click="loadUsers">
          <el-icon><Search /></el-icon>
          搜索
        </el-button>
      </div>
    </el-card>

    <!-- 用户表格 -->
    <el-card class="table-card animate__animated animate__fadeInUp" v-loading="loading" element-loading-text="加载用户数据中...">
      <el-table :data="displayList" stripe style="width: 100%" row-class-name="table-row">
        <el-table-column type="index" label="#" width="50" />
        <el-table-column prop="username" label="用户名" width="140">
          <template #default="{ row }">
            <div class="user-cell">
              <el-icon class="user-cell-icon"><User /></el-icon>
              <span>{{ row.username }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="displayName" label="昵称/姓名" width="140" />
        <el-table-column prop="userType" label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="row.userType === 'ADMIN' ? 'warning' : 'success'" size="small" effect="light">
              {{ row.userType === 'ADMIN' ? '管理员' : '读者' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="已分配角色" min-width="220">
          <template #default="{ row }">
            <div class="role-tags" v-if="row.roles && row.roles.length > 0">
              <el-tag
                v-for="role in row.roles" :key="role.id"
                size="small"
                :type="role.level >= 100 ? 'danger' : role.level >= 50 ? 'warning' : 'success'"
                effect="light"
                class="role-tag-item"
              >
                {{ role.name }}
                <span class="role-level-badge">Lv.{{ role.level }}</span>
              </el-tag>
            </div>
            <span v-else class="no-role-text">未分配角色</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button v-permission="'USER_ROLE_ASSIGN'" class="action-btn-edit" size="small" @click="handleEditRoles(row)">
              <el-icon><Edit /></el-icon> 编辑角色
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper" v-if="total > pageSize">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          background
          @current-change="loadUsers"
          @size-change="loadUsers"
        />
      </div>
    </el-card>

    <!-- Role Assignment Dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="`编辑角色 - ${editingUser?.displayName || editingUser?.username || ''}`"
      width="500px"
      :close-on-click-modal="false"
      destroy-on-close
      class="assign-dialog"
    >
      <div class="assign-dialog-content" v-loading="assignLoading" element-loading-text="加载中...">
        <div class="assign-user-info">
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="用户名">{{ editingUser?.username }}</el-descriptions-item>
            <el-descriptions-item label="类型">
              <el-tag :type="editingUser?.userType === 'ADMIN' ? 'warning' : 'success'" size="small">
                {{ editingUser?.userType === 'ADMIN' ? '管理员' : '读者' }}
              </el-tag>
            </el-descriptions-item>
          </el-descriptions>
        </div>
        <div class="assign-roles-section">
          <div class="assign-section-title">选择角色：</div>
          <el-checkbox-group v-model="selectedRoleIds" class="role-checkbox-group">
            <div v-for="role in allRoles" :key="role.id" class="role-checkbox-item">
              <el-checkbox :value="role.id">
                <div class="role-check-label">
                  <span class="role-check-name">{{ role.name }}</span>
                  <el-tag size="small" :type="role.level >= 100 ? 'danger' : role.level >= 50 ? 'warning' : 'success'">
                    Lv.{{ role.level }}
                  </el-tag>
                  <span class="role-check-desc" v-if="role.description">{{ role.description }}</span>
                </div>
              </el-checkbox>
            </div>
          </el-checkbox-group>
        </div>
        <!-- 权限预览区域 -->
        <div class="perm-preview-section" v-if="selectedRoleIds.length > 0">
          <div class="assign-section-title">
            权限预览
            <el-tag size="small" type="info" effect="light" class="perm-preview-count">{{ effectivePermCount }} 项</el-tag>
          </div>
          <div class="perm-preview-modules">
            <div v-for="(perms, mod) in effectivePermsByModule" :key="mod" class="perm-preview-module">
              <div class="perm-preview-mod-title">{{ mod }}</div>
              <div class="perm-preview-tags">
                <el-tag
                  v-for="p in perms" :key="p.id"
                  size="small"
                  :type="p.type === 'menu' ? 'success' : 'warning'"
                  :effect="p.inherited ? 'plain' : 'light'"
                  class="perm-preview-tag"
                >
                  {{ p.name }}
                  <span v-if="p.inherited" class="inherited-mark">继承</span>
                </el-tag>
              </div>
            </div>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button v-permission="'USER_ROLE_ASSIGN'" type="primary" @click="saveUserRoles" :loading="saveLoading">
          {{ saveLoading ? '保存中...' : '确认保存' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { usePermission } from '@/composables/usePermission'
import { getAllUsersWithRoles, getAllRoles, assignUserRoles, getUserRoles, getPermissions, getRolePermissions } from '@/api'

const { checkWithFeedback } = usePermission()

const loading = ref(false)
const userList = ref([])
const searchKeyword = ref('')
const filterType = ref('')
const filterRoleStatus = ref('')
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)

const stats = computed(() => {
  return {
    totalUsers: total.value,
    admins: userList.value.filter(u => u.userType === 'ADMIN').length,
    readers: userList.value.filter(u => u.userType === 'READER').length,
    noRole: userList.value.filter(u => !u.roles || u.roles.length === 0).length
  }
})

const displayList = computed(() => {
  if (!filterRoleStatus.value) return userList.value
  if (filterRoleStatus.value === 'no_role') {
    return userList.value.filter(u => !u.roles || u.roles.length === 0)
  }
  if (filterRoleStatus.value === 'multi_role') {
    return userList.value.filter(u => u.roles && u.roles.length > 1)
  }
  return userList.value
})

function applyLocalFilter() {}

// Dialog state
const dialogVisible = ref(false)
const assignLoading = ref(false)
const saveLoading = ref(false)
const editingUser = ref(null)
const selectedRoleIds = ref([])
const allRoles = ref([])
const allPermissions = ref([])
const rolePermCache = ref({})

const effectivePermsByModule = computed(() => {
  if (selectedRoleIds.value.length === 0) return {}
  const selectedRoles = allRoles.value.filter(r => selectedRoleIds.value.includes(r.id))
  const maxLevel = Math.max(...selectedRoles.map(r => r.level || 0), 0)
  const inheritedRoles = allRoles.value.filter(r => r.level < maxLevel && r.status === 1 && !selectedRoleIds.value.includes(r.id))

  const directPermIds = new Set()
  const inheritedPermIds = new Set()

  selectedRoles.forEach(role => {
    const permIds = rolePermCache.value[role.id] || []
    permIds.forEach(id => directPermIds.add(id))
  })

  inheritedRoles.forEach(role => {
    const permIds = rolePermCache.value[role.id] || []
    permIds.forEach(id => {
      if (!directPermIds.has(id)) inheritedPermIds.add(id)
    })
  })

  const allEffectiveIds = new Set([...directPermIds, ...inheritedPermIds])
  const grouped = {}
  allPermissions.value.forEach(p => {
    if (!allEffectiveIds.has(p.id)) return
    const mod = p.module || '其他'
    if (!grouped[mod]) grouped[mod] = []
    grouped[mod].push({ ...p, inherited: inheritedPermIds.has(p.id) })
  })
  return grouped
})

const effectivePermCount = computed(() => {
  let count = 0
  Object.values(effectivePermsByModule.value).forEach(perms => { count += perms.length })
  return count
})

watch(selectedRoleIds, async (newIds) => {
  const selectedRoles = allRoles.value.filter(r => newIds.includes(r.id))
  const maxLevel = Math.max(...selectedRoles.map(r => r.level || 0), 0)
  const relevantRoles = allRoles.value.filter(r => newIds.includes(r.id) || (r.level < maxLevel && r.status === 1))
  for (const role of relevantRoles) {
    if (!rolePermCache.value[role.id]) {
      try {
        const res = await getRolePermissions(role.id)
        rolePermCache.value[role.id] = res.data || []
      } catch (e) {
        rolePermCache.value[role.id] = []
      }
    }
  }
}, { deep: true })

onMounted(() => {
  loadUsers()
  loadAllRoles()
  loadAllPermissions()
})

async function loadUsers() {
  loading.value = true
  try {
    const params = { page: currentPage.value, size: pageSize.value }
    if (searchKeyword.value) params.keyword = searchKeyword.value
    if (filterType.value) params.userType = filterType.value
    const res = await getAllUsersWithRoles(params)
    userList.value = res.data.records || []
    total.value = res.data.total || 0
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

async function loadAllRoles() {
  try {
    const res = await getAllRoles()
    allRoles.value = res.data || []
  } catch (e) {
    console.error(e)
  }
}

async function loadAllPermissions() {
  try {
    const res = await getPermissions()
    allPermissions.value = res.data || []
  } catch (e) {
    console.error(e)
  }
}

async function handleEditRoles(user) {
  if (!checkWithFeedback('USER_ROLE_ASSIGN')) return
  editingUser.value = user
  dialogVisible.value = true
  assignLoading.value = true
  try {
    const res = await getUserRoles(user.userType, user.userId)
    selectedRoleIds.value = res.data.roleIds || []
  } catch (e) {
    selectedRoleIds.value = []
  } finally {
    assignLoading.value = false
  }
}

async function saveUserRoles() {
  if (!checkWithFeedback('USER_ROLE_ASSIGN')) return
  saveLoading.value = true
  try {
    await assignUserRoles(editingUser.value.userType, editingUser.value.userId, { roleIds: selectedRoleIds.value })
    ElMessage.success('角色分配成功')
    dialogVisible.value = false
    loadUsers()
  } catch (e) {
    console.error(e)
  } finally {
    saveLoading.value = false
  }
}
</script>

<style scoped>
.user-roles-page {
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

.type-select {
  width: 140px;
}

/* 表格卡片 */
.table-card {
  background: white;
}

.user-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.user-cell-icon {
  color: var(--purple);
  font-size: 16px;
}

.role-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.role-tag-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.role-level-badge {
  font-size: 10px;
  opacity: 0.7;
  font-weight: 600;
}

.no-role-text {
  font-size: 13px;
  color: var(--text-secondary);
  font-style: italic;
}

/* 操作按钮 */
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

/* 分页 */
.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid #F5F5F5;
}

/* 弹窗 */
.assign-dialog :deep(.el-dialog__header) {
  background: linear-gradient(135deg, var(--purple-light), var(--blue-light));
  margin-right: 0;
  padding: 20px 24px;
}

.assign-dialog :deep(.el-dialog__title) {
  font-weight: 600;
  font-size: 18px;
}

.assign-dialog-content {
  padding: 8px 0;
}

.assign-user-info {
  margin-bottom: 20px;
}

.assign-section-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 12px;
}

.role-checkbox-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.role-checkbox-item {
  padding: 10px 14px;
  background: var(--bg-main);
  border-radius: var(--radius-sm);
  transition: all 0.2s ease;
}

.role-checkbox-item:hover {
  background: linear-gradient(135deg, var(--green-light), var(--blue-light));
}

.role-check-label {
  display: flex;
  align-items: center;
  gap: 8px;
}

.role-check-name {
  font-weight: 500;
  font-size: 14px;
}

.role-check-desc {
  font-size: 12px;
  color: var(--text-secondary);
  margin-left: 4px;
}

/* 权限预览 */
.perm-preview-section {
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px dashed #E8E8E8;
}

.perm-preview-count {
  margin-left: 8px;
}

.perm-preview-modules {
  max-height: 200px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding-right: 4px;
}

.perm-preview-module {
  padding: 10px 12px;
  background: var(--bg-main);
  border-radius: var(--radius-sm);
}

.perm-preview-mod-title {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-secondary);
  margin-bottom: 6px;
}

.perm-preview-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.perm-preview-tag {
  font-size: 11px !important;
}

.inherited-mark {
  font-size: 9px;
  margin-left: 3px;
  opacity: 0.7;
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
}
</style>
