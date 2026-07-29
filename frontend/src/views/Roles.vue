<template>
  <div class="roles-page">
    <!-- 搜索区域 -->
    <el-card class="search-card animate__animated animate__fadeInDown">
      <div class="search-bar">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索角色名称或编码..."
          prefix-icon="Search"
          size="large"
          clearable
          class="search-input"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        />
        <el-button type="primary" size="large" @click="handleSearch">
          <el-icon><Search /></el-icon>
          搜索
        </el-button>
        <el-button v-permission="'role:add'" type="success" size="large" @click="handleAdd">
          <el-icon><Plus /></el-icon>
          新增角色
        </el-button>
      </div>
    </el-card>

    <!-- 角色列表 -->
    <el-card class="table-card" v-loading="tableLoading" element-loading-text="正在加载角色数据...">
      <el-table :data="roleList" stripe style="width: 100%" row-class-name="table-row">
        <el-table-column type="index" label="#" width="50" />
        <el-table-column prop="name" label="角色名称" min-width="140">
          <template #default="{ row }">
            <div class="role-name-cell">
              <span class="role-icon">🔑</span>
              <span>{{ row.name }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="code" label="角色编码" min-width="140" />
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170">
          <template #default="{ row }">
            {{ formatTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button v-permission="'permission:assign'" class="action-btn-primary" @click="handleAssignPerms(row)">
              <el-icon><Key /></el-icon> 分配权限
            </el-button>
            <el-button v-permission="'role:edit'" class="action-btn-edit" @click="handleEdit(row)">
              <el-icon><Edit /></el-icon> 编辑
            </el-button>
            <el-button v-permission="'role:delete'" class="action-btn-delete" @click="handleDelete(row)">
              <el-icon><Delete /></el-icon> 删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @size-change="fetchRoles"
          @current-change="fetchRoles"
        />
      </div>
    </el-card>

    <!-- 新增/编辑角色弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑角色' : '新增角色'"
      width="500px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="80px">
        <el-form-item label="角色名称" prop="name">
          <el-input v-model="formData.name" placeholder="请输入角色名称" />
        </el-form-item>
        <el-form-item label="角色编码" prop="code">
          <el-input v-model="formData.code" placeholder="请输入角色编码（如：admin）" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="formData.description" type="textarea" :rows="3" placeholder="请输入角色描述" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-switch v-model="formData.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="禁用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitLoading">确定</el-button>
      </template>
    </el-dialog>

    <!-- 分配权限弹窗 -->
    <el-dialog
      v-model="permDialogVisible"
      title="分配权限"
      width="550px"
      destroy-on-close
    >
      <div class="perm-dialog-header">
        <span class="perm-role-name">当前角色：<strong>{{ currentRole?.name }}</strong></span>
        <el-button text type="primary" @click="handleCheckAll">全选</el-button>
        <el-button text @click="handleUncheckAll">取消全选</el-button>
      </div>
      <div class="perm-tree-wrapper" v-loading="permLoading">
        <el-tree
          ref="permTreeRef"
          :data="permTree"
          show-checkbox
          node-key="id"
          :props="{ label: 'name', children: 'children' }"
          :default-expanded-keys="expandedKeys"
          check-strictly
        />
      </div>
      <template #footer>
        <el-button @click="permDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSavePerms" :loading="permSaveLoading">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getRoles, addRole, updateRole, deleteRole, getRole, getPermissionTree, assignRolePermissions } from '@/api/index'

const searchKeyword = ref('')
const tableLoading = ref(false)
const roleList = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const dialogVisible = ref(false)
const isEdit = ref(false)
const submitLoading = ref(false)
const formRef = ref(null)
const formData = reactive({ name: '', code: '', description: '', status: 1 })
const formRules = {
  name: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  code: [{ required: true, message: '请输入角色编码', trigger: 'blur' }]
}
let editId = null

const permDialogVisible = ref(false)
const permLoading = ref(false)
const permSaveLoading = ref(false)
const permTree = ref([])
const permTreeRef = ref(null)
const expandedKeys = ref([])
const currentRole = ref(null)

onMounted(() => {
  fetchRoles()
})

async function fetchRoles() {
  tableLoading.value = true
  try {
    const res = await getRoles({ page: currentPage.value, size: pageSize.value, keyword: searchKeyword.value })
    roleList.value = res.data.records
    total.value = res.data.total
  } catch (e) {
    console.error(e)
  } finally {
    tableLoading.value = false
  }
}

function handleSearch() {
  currentPage.value = 1
  fetchRoles()
}

function handleAdd() {
  isEdit.value = false
  editId = null
  Object.assign(formData, { name: '', code: '', description: '', status: 1 })
  dialogVisible.value = true
}

function handleEdit(row) {
  isEdit.value = true
  editId = row.id
  Object.assign(formData, { name: row.name, code: row.code, description: row.description, status: row.status })
  dialogVisible.value = true
}

async function handleSubmit() {
  await formRef.value.validate()
  submitLoading.value = true
  try {
    if (isEdit.value) {
      await updateRole(editId, { ...formData })
      ElMessage.success('更新成功')
    } else {
      await addRole({ ...formData })
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchRoles()
  } catch (e) {
    console.error(e)
  } finally {
    submitLoading.value = false
  }
}

function handleDelete(row) {
  ElMessageBox.confirm(`确定删除角色「${row.name}」吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await deleteRole(row.id)
      ElMessage.success('删除成功')
      fetchRoles()
    } catch (e) {
      console.error(e)
    }
  }).catch(() => {})
}

async function handleAssignPerms(row) {
  currentRole.value = row
  permDialogVisible.value = true
  permLoading.value = true
  try {
    const [treeRes, roleRes] = await Promise.all([getPermissionTree(), getRole(row.id)])
    permTree.value = treeRes.data
    expandedKeys.value = permTree.value.map(n => n.id)
    await nextTick()
    const checkedIds = roleRes.data.permissionIds || []
    permTreeRef.value?.setCheckedKeys(checkedIds)
  } catch (e) {
    console.error(e)
  } finally {
    permLoading.value = false
  }
}

function handleCheckAll() {
  const allIds = getAllNodeIds(permTree.value)
  permTreeRef.value?.setCheckedKeys(allIds)
}

function handleUncheckAll() {
  permTreeRef.value?.setCheckedKeys([])
}

function getAllNodeIds(nodes) {
  let ids = []
  for (const node of nodes) {
    ids.push(node.id)
    if (node.children) {
      ids = ids.concat(getAllNodeIds(node.children))
    }
  }
  return ids
}

async function handleSavePerms() {
  permSaveLoading.value = true
  try {
    const checkedIds = permTreeRef.value?.getCheckedKeys() || []
    await assignRolePermissions(currentRole.value.id, checkedIds)
    ElMessage.success('权限分配成功')
    permDialogVisible.value = false
  } catch (e) {
    console.error(e)
  } finally {
    permSaveLoading.value = false
  }
}

function formatTime(time) {
  if (!time) return '-'
  return time.replace('T', ' ').substring(0, 16)
}
</script>

<style scoped>
.roles-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.search-card {
  border-radius: var(--radius-md);
}

.search-bar {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}

.search-input {
  width: 300px;
}

.table-card {
  border-radius: var(--radius-md);
}

.role-name-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.role-icon {
  font-size: 16px;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.action-btn-primary {
  background: linear-gradient(135deg, var(--purple), #B39DDB) !important;
  border: none !important;
  color: #fff !important;
  border-radius: 8px !important;
  font-size: 12px !important;
  padding: 6px 12px !important;
  height: 32px;
}

.action-btn-primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(149, 125, 173, 0.4);
}

.action-btn-edit {
  background: linear-gradient(135deg, var(--blue), #90CAF9) !important;
  border: none !important;
  color: #fff !important;
  border-radius: 8px !important;
  font-size: 12px !important;
  padding: 6px 12px !important;
  height: 32px;
}

.action-btn-edit:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(130, 177, 255, 0.4);
}

.action-btn-delete {
  background: linear-gradient(135deg, #FF6B81, #FF8A9E) !important;
  border: none !important;
  color: #fff !important;
  border-radius: 8px !important;
  font-size: 12px !important;
  padding: 6px 12px !important;
  height: 32px;
}

.action-btn-delete:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(255, 107, 129, 0.4);
}

.perm-dialog-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.perm-role-name {
  flex: 1;
  color: var(--text-secondary);
  font-size: 14px;
}

.perm-tree-wrapper {
  max-height: 400px;
  overflow-y: auto;
  padding: 8px;
  border: 1px solid #f0f0f0;
  border-radius: var(--radius-sm);
}
</style>
