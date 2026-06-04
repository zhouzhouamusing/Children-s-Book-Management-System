<template>
  <div class="roles-page">
    <el-card class="search-card animate__animated animate__fadeInDown">
      <div class="search-bar">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索角色名称或编码..."
          prefix-icon="Search"
          size="large"
          clearable
          class="search-input"
          @keyup.enter="loadRoles"
          @clear="loadRoles"
        />
        <el-button type="primary" size="large" @click="loadRoles">
          <el-icon><Search /></el-icon>
          搜索
        </el-button>
        <el-button type="success" size="large" @click="handleAdd">
          <el-icon><Plus /></el-icon>
          新增角色
        </el-button>
      </div>
    </el-card>

    <el-card class="table-card" v-loading="loading">
      <el-table :data="roleList" stripe style="width: 100%">
        <el-table-column prop="code" label="角色编码" width="150" />
        <el-table-column prop="name" label="角色名称" width="150" />
        <el-table-column prop="level" label="层级" width="100">
          <template #default="{ row }">
            <el-tag :type="row.level >= 100 ? 'danger' : row.level >= 50 ? 'warning' : 'success'" size="small">
              {{ row.level }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="200" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button class="action-btn action-btn-perm" size="small" @click="handlePermissions(row)">
              <el-icon><Key /></el-icon> 分配权限
            </el-button>
            <el-button class="action-btn action-btn-edit" size="small" @click="handleEdit(row)">
              <el-icon><Edit /></el-icon> 编辑
            </el-button>
            <el-button
              class="action-btn action-btn-delete" size="small"
              @click="handleDelete(row)"
              :disabled="['ADMIN','READER','SUPER_ADMIN'].includes(row.code)"
            >
              <el-icon><Delete /></el-icon> 删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper" v-if="total > pageSize">
        <el-pagination
          v-model:current-page="currentPage"
          :page-size="pageSize"
          :total="total"
          layout="prev, pager, next"
          @current-change="loadRoles"
        />
      </div>
    </el-card>

    <!-- Role Form Dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="editingRole ? '编辑角色' : '新增角色'"
      width="500px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="roleForm" :rules="formRules" label-width="80px">
        <el-form-item label="编码" prop="code">
          <el-input v-model="roleForm.code" placeholder="如 BOOK_MANAGER" :disabled="!!editingRole" />
        </el-form-item>
        <el-form-item label="名称" prop="name">
          <el-input v-model="roleForm.name" placeholder="如 图书管理员" />
        </el-form-item>
        <el-form-item label="层级" prop="level">
          <el-input-number v-model="roleForm.level" :min="1" :max="999" />
          <span class="form-hint">数字越大权限越高，高层级角色自动继承低层级权限</span>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="roleForm.description" type="textarea" :rows="3" placeholder="角色描述" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="roleForm.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm" :loading="submitLoading">确定</el-button>
      </template>
    </el-dialog>

    <!-- Permission Assignment Drawer -->
    <el-drawer
      v-model="permDrawerVisible"
      :title="`分配权限 - ${currentRole?.name || ''}`"
      size="500px"
      destroy-on-close
    >
      <div class="perm-drawer-content" v-loading="permLoading">
        <div class="perm-info">
          <el-alert
            v-if="currentRole && currentRole.level > 10"
            type="info"
            :closable="false"
            show-icon
          >
            <template #default>
              层级为 {{ currentRole.level }}，将自动继承所有低层级角色的权限（灰色标识）
            </template>
          </el-alert>
        </div>
        <div v-for="(perms, module) in groupedPermissions" :key="module" class="perm-group">
          <div class="perm-group-title">{{ module }}</div>
          <div class="perm-group-items">
            <el-checkbox
              v-for="p in perms" :key="p.id"
              :model-value="selectedPermIds.includes(p.id)"
              :disabled="inheritedPermIds.includes(p.id) && !selectedPermIds.includes(p.id)"
              @change="(val) => togglePerm(p.id, val)"
              class="perm-checkbox"
              :class="{ inherited: inheritedPermIds.includes(p.id) && !ownPermIds.includes(p.id) }"
            >
              {{ p.name }}
              <span class="perm-code">({{ p.code }})</span>
            </el-checkbox>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="permDrawerVisible = false">取消</el-button>
        <el-button type="primary" @click="savePermissions" :loading="permSaveLoading">保存</el-button>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getRoles, createRole, updateRole, deleteRole,
  getPermissions, getRolePermissions, getEffectivePermissions, assignRolePermissions
} from '@/api'

const loading = ref(false)
const roleList = ref([])
const searchKeyword = ref('')
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)

const dialogVisible = ref(false)
const editingRole = ref(null)
const submitLoading = ref(false)
const formRef = ref(null)
const roleForm = reactive({ code: '', name: '', level: 20, description: '', status: 1 })
const formRules = {
  code: [{ required: true, message: '请输入角色编码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  level: [{ required: true, message: '请设置层级', trigger: 'blur' }]
}

const permDrawerVisible = ref(false)
const permLoading = ref(false)
const permSaveLoading = ref(false)
const currentRole = ref(null)
const allPermissions = ref([])
const groupedPermissions = ref({})
const selectedPermIds = ref([])
const ownPermIds = ref([])
const inheritedPermIds = ref([])

onMounted(() => {
  loadRoles()
  loadAllPermissions()
})

async function loadRoles() {
  loading.value = true
  try {
    const res = await getRoles({ page: currentPage.value, size: pageSize.value, keyword: searchKeyword.value })
    roleList.value = res.data.records || res.data
    total.value = res.data.total || roleList.value.length
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

async function loadAllPermissions() {
  try {
    const res = await getPermissions()
    allPermissions.value = res.data
    const grouped = {}
    res.data.forEach(p => {
      const mod = p.module || '其他'
      if (!grouped[mod]) grouped[mod] = []
      grouped[mod].push(p)
    })
    groupedPermissions.value = grouped
  } catch (e) {
    console.error(e)
  }
}

function handleAdd() {
  editingRole.value = null
  Object.assign(roleForm, { code: '', name: '', level: 20, description: '', status: 1 })
  dialogVisible.value = true
}

function handleEdit(row) {
  editingRole.value = row
  Object.assign(roleForm, { code: row.code, name: row.name, level: row.level, description: row.description, status: row.status })
  dialogVisible.value = true
}

async function submitForm() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitLoading.value = true
  try {
    if (editingRole.value) {
      await updateRole(editingRole.value.id, roleForm)
      ElMessage.success('更新成功')
    } else {
      await createRole(roleForm)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadRoles()
  } catch (e) {
    console.error(e)
  } finally {
    submitLoading.value = false
  }
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确定要删除角色「${row.name}」吗？`, '警告', { type: 'warning' })
  try {
    await deleteRole(row.id)
    ElMessage.success('删除成功')
    loadRoles()
  } catch (e) {
    console.error(e)
  }
}

async function handlePermissions(row) {
  currentRole.value = row
  permDrawerVisible.value = true
  permLoading.value = true
  try {
    const [ownRes, effectiveRes] = await Promise.all([
      getRolePermissions(row.id),
      getEffectivePermissions(row.id)
    ])
    ownPermIds.value = ownRes.data || []
    selectedPermIds.value = [...ownPermIds.value]
    const effectiveData = effectiveRes.data
    const allEffective = (effectiveData.effectivePermissions || []).map(p => p.id)
    inheritedPermIds.value = allEffective.filter(id => !ownPermIds.value.includes(id))
  } catch (e) {
    console.error(e)
  } finally {
    permLoading.value = false
  }
}

function togglePerm(permId, checked) {
  if (checked) {
    if (!selectedPermIds.value.includes(permId)) {
      selectedPermIds.value.push(permId)
    }
  } else {
    selectedPermIds.value = selectedPermIds.value.filter(id => id !== permId)
  }
}

async function savePermissions() {
  permSaveLoading.value = true
  try {
    await assignRolePermissions(currentRole.value.id, { permissionIds: selectedPermIds.value })
    ElMessage.success('权限分配成功')
    permDrawerVisible.value = false
  } catch (e) {
    console.error(e)
  } finally {
    permSaveLoading.value = false
  }
}
</script>

<style scoped>
.roles-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.search-card {
  border-radius: var(--radius-lg);
}

.search-bar {
  display: flex;
  gap: 12px;
  align-items: center;
}

.search-input {
  flex: 1;
  max-width: 400px;
}

.table-card {
  border-radius: var(--radius-lg);
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

.action-btn {
  border: none;
  color: #fff;
  font-size: 13px;
  padding: 6px 12px;
  height: 32px;
  border-radius: 8px;
}

.action-btn-perm {
  background: linear-gradient(135deg, var(--green), #8DD5BE);
}
.action-btn-perm:hover {
  background: linear-gradient(135deg, #8DD5BE, var(--green));
}

.action-btn-edit {
  background: linear-gradient(135deg, #A78BFA, #C4B3D4);
}
.action-btn-edit:hover {
  background: linear-gradient(135deg, #C4B3D4, #A78BFA);
}

.action-btn-delete {
  background: linear-gradient(135deg, #FFB3BA, #FFCDD2);
}
.action-btn-delete:hover {
  background: linear-gradient(135deg, #FFCDD2, #FFB3BA);
}

.form-hint {
  font-size: 12px;
  color: var(--text-secondary);
  margin-left: 8px;
}

.perm-drawer-content {
  padding: 0 10px;
}

.perm-info {
  margin-bottom: 16px;
}

.perm-group {
  margin-bottom: 20px;
}

.perm-group-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 10px;
  padding-bottom: 6px;
  border-bottom: 1px solid #f0f0f0;
}

.perm-group-items {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.perm-checkbox {
  margin-right: 0;
}

.perm-checkbox.inherited {
  opacity: 0.6;
}

.perm-code {
  font-size: 11px;
  color: var(--text-secondary);
}
</style>
