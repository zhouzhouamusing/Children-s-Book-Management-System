<template>
  <div class="user-roles-page">
    <!-- 搜索区域 -->
    <el-card class="search-card animate__animated animate__fadeInDown">
      <div class="search-bar">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索管理员用户名或昵称..."
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
      </div>
    </el-card>

    <!-- 用户列表 -->
    <el-card class="table-card" v-loading="tableLoading" element-loading-text="正在加载用户数据...">
      <el-table :data="userList" stripe style="width: 100%" row-class-name="table-row">
        <el-table-column type="index" label="#" width="50" />
        <el-table-column prop="username" label="用户名" min-width="120">
          <template #default="{ row }">
            <div class="user-cell">
              <span class="user-icon">👤</span>
              <span>{{ row.username }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="nickname" label="昵称" min-width="120" />
        <el-table-column prop="email" label="邮箱" min-width="180" show-overflow-tooltip />
        <el-table-column label="当前角色" min-width="200">
          <template #default="{ row }">
            <div class="role-tags">
              <el-tag
                v-for="role in row.roles"
                :key="role.id"
                :type="getRoleTagType(role.code)"
                size="small"
                class="role-tag"
              >
                {{ role.name }}
              </el-tag>
              <el-tag v-if="!row.roles || row.roles.length === 0" type="info" size="small">未分配</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button v-permission="'user-role:assign'" class="action-btn-primary" @click="handleAssign(row)">
              <el-icon><UserFilled /></el-icon> 分配角色
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
          @size-change="fetchUsers"
          @current-change="fetchUsers"
        />
      </div>
    </el-card>

    <!-- 分配角色弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      title="分配角色"
      width="480px"
      destroy-on-close
    >
      <div class="assign-header">
        <span>为用户 <strong>{{ currentUser?.nickname || currentUser?.username }}</strong> 分配角色</span>
      </div>
      <div class="role-checkbox-group" v-loading="rolesLoading">
        <el-checkbox-group v-model="selectedRoleIds">
          <div v-for="role in allRoles" :key="role.id" class="role-checkbox-item">
            <el-checkbox :value="role.id">
              <div class="role-checkbox-content">
                <span class="role-checkbox-name">{{ role.name }}</span>
                <span class="role-checkbox-desc">{{ role.description || role.code }}</span>
              </div>
            </el-checkbox>
          </div>
        </el-checkbox-group>
      </div>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSaveRoles" :loading="saveLoading">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getAdminUsersWithRoles, getAllRoles, assignUserRoles } from '@/api/index'

const searchKeyword = ref('')
const tableLoading = ref(false)
const userList = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const dialogVisible = ref(false)
const rolesLoading = ref(false)
const saveLoading = ref(false)
const currentUser = ref(null)
const allRoles = ref([])
const selectedRoleIds = ref([])

onMounted(() => {
  fetchUsers()
})

async function fetchUsers() {
  tableLoading.value = true
  try {
    const res = await getAdminUsersWithRoles({ page: currentPage.value, size: pageSize.value, keyword: searchKeyword.value })
    userList.value = res.data.records
    total.value = res.data.total
  } catch (e) {
    console.error(e)
  } finally {
    tableLoading.value = false
  }
}

function handleSearch() {
  currentPage.value = 1
  fetchUsers()
}

async function handleAssign(row) {
  currentUser.value = row
  selectedRoleIds.value = (row.roles || []).map(r => r.id)
  dialogVisible.value = true
  rolesLoading.value = true
  try {
    const res = await getAllRoles()
    allRoles.value = res.data
  } catch (e) {
    console.error(e)
  } finally {
    rolesLoading.value = false
  }
}

async function handleSaveRoles() {
  saveLoading.value = true
  try {
    await assignUserRoles({
      userType: 'admin',
      userId: currentUser.value.id,
      roleIds: selectedRoleIds.value
    })
    ElMessage.success('角色分配成功')
    dialogVisible.value = false
    fetchUsers()
  } catch (e) {
    console.error(e)
  } finally {
    saveLoading.value = false
  }
}

function getRoleTagType(code) {
  if (code === 'SUPER_ADMIN') return 'danger'
  if (code === 'ADMIN') return 'warning'
  return ''
}
</script>

<style scoped>
.user-roles-page {
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

.user-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.user-icon {
  font-size: 16px;
}

.role-tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.role-tag {
  border-radius: 12px !important;
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

.assign-header {
  margin-bottom: 20px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
  color: var(--text-secondary);
  font-size: 14px;
}

.role-checkbox-group {
  max-height: 350px;
  overflow-y: auto;
}

.role-checkbox-item {
  padding: 12px 16px;
  border-radius: var(--radius-sm);
  margin-bottom: 8px;
  background: #FAFAFA;
  transition: background 0.2s;
}

.role-checkbox-item:hover {
  background: #F0F0FF;
}

.role-checkbox-content {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.role-checkbox-name {
  font-weight: 500;
  color: var(--text-primary);
}

.role-checkbox-desc {
  font-size: 12px;
  color: var(--text-secondary);
}
</style>
