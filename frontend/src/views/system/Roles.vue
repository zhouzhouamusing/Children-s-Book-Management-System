<template>
  <div class="roles-page">
    <!-- 统计卡片区域 -->
    <div class="stat-row animate__animated animate__fadeInDown">
      <div class="mini-stat" style="--accent: var(--purple)">
        <span class="mini-stat-icon">🛡️</span>
        <div class="mini-stat-info">
          <span class="mini-stat-value">{{ roleStats.total }}</span>
          <span class="mini-stat-label">角色总数</span>
        </div>
      </div>
      <div class="mini-stat" style="--accent: var(--green)">
        <span class="mini-stat-icon">✅</span>
        <div class="mini-stat-info">
          <span class="mini-stat-value">{{ roleStats.active }}</span>
          <span class="mini-stat-label">启用中</span>
        </div>
      </div>
      <div class="mini-stat" style="--accent: var(--blue)">
        <span class="mini-stat-icon">👑</span>
        <div class="mini-stat-info">
          <span class="mini-stat-value">{{ roleStats.maxLevel }}</span>
          <span class="mini-stat-label">最高层级</span>
        </div>
      </div>
      <div class="mini-stat" style="--accent: var(--pink)">
        <span class="mini-stat-icon">👥</span>
        <div class="mini-stat-info">
          <span class="mini-stat-value">{{ roleStats.totalUsers }}</span>
          <span class="mini-stat-label">用户分配</span>
        </div>
      </div>
    </div>

    <!-- 角色层级可视化 -->
    <el-card class="hierarchy-card animate__animated animate__fadeInDown" style="animation-delay: 0.05s">
      <template #header>
        <div class="hierarchy-header">
          <span class="hierarchy-title">🏰 角色继承关系</span>
          <span class="hierarchy-tip">高层级角色自动继承低层级角色的所有权限</span>
        </div>
      </template>
      <div class="hierarchy-chain">
        <div v-for="(role, idx) in sortedRoles" :key="role.id" class="hierarchy-node">
          <div class="hierarchy-badge" :class="getLevelClass(role.level)">
            <span class="hierarchy-level">Lv.{{ role.level }}</span>
            <span class="hierarchy-name">{{ role.name }}</span>
            <span class="hierarchy-user-count">{{ role.userCount || 0 }}人</span>
          </div>
          <div v-if="idx < sortedRoles.length - 1" class="hierarchy-arrow">
            <el-icon><ArrowLeft /></el-icon>
            <span class="hierarchy-arrow-text">继承</span>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 搜索与操作栏 -->
    <el-card class="search-card animate__animated animate__fadeInDown" style="animation-delay: 0.1s">
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
        <el-button v-permission="'ROLE_MANAGE'" type="success" size="large" @click="handleAdd">
          <el-icon><Plus /></el-icon>
          新增角色
        </el-button>
      </div>
    </el-card>

    <!-- 角色表格 -->
    <el-card class="table-card animate__animated animate__fadeInUp" v-loading="loading" element-loading-text="加载角色数据中...">
      <el-table :data="roleList" stripe style="width: 100%" row-class-name="table-row">
        <el-table-column type="index" label="#" width="50" />
        <el-table-column prop="code" label="角色编码" width="150">
          <template #default="{ row }">
            <div class="role-code-cell">
              <span class="role-emoji">🛡️</span>
              <span>{{ row.code }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="角色名称" width="130" />
        <el-table-column prop="level" label="层级" width="90">
          <template #default="{ row }">
            <el-tag :type="row.level >= 100 ? 'danger' : row.level >= 50 ? 'warning' : 'success'" size="small">
              Lv.{{ row.level }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="userCount" label="用户数" width="90">
          <template #default="{ row }">
            <el-tag type="info" size="small" effect="light">{{ row.userCount || 0 }}人</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="180" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small" effect="light">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="320" fixed="right">
          <template #default="{ row }">
            <el-button v-permission="'ROLE_MANAGE'" class="action-btn-perm" size="small" @click="handlePermissions(row)">
              <el-icon><Key /></el-icon> 权限
            </el-button>
            <el-button v-permission="'USER_ROLE_ASSIGN'" class="action-btn-users" size="small" @click="handleUsers(row)">
              <el-icon><User /></el-icon> 用户
            </el-button>
            <el-button v-permission="'ROLE_MANAGE'" class="action-btn-edit" size="small" @click="handleEdit(row)">
              <el-icon><Edit /></el-icon> 编辑
            </el-button>
            <el-button
              v-permission="'ROLE_MANAGE'"
              class="action-btn-delete" size="small"
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
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          background
          @current-change="loadRoles"
          @size-change="loadRoles"
        />
      </div>
    </el-card>

    <!-- Role Form Dialog with Tabs -->
    <el-dialog
      v-model="dialogVisible"
      :title="editingRole ? '编辑角色' : '新增角色'"
      width="720px"
      :close-on-click-modal="false"
      destroy-on-close
      class="role-dialog"
    >
      <el-tabs v-model="activeTab" class="role-tabs">
        <el-tab-pane label="基本信息" name="basic">
          <el-form ref="formRef" :model="roleForm" :rules="formRules" label-width="90px" class="role-form">
            <el-form-item label="编码" prop="code">
              <el-input v-model="roleForm.code" placeholder="如 BOOK_MANAGER" :disabled="!!editingRole" />
            </el-form-item>
            <el-form-item label="名称" prop="name">
              <el-input v-model="roleForm.name" placeholder="如 图书管理员" maxlength="20" show-word-limit />
            </el-form-item>
            <el-form-item label="层级" prop="level">
              <el-input-number v-model="roleForm.level" :min="1" :max="999" />
              <span class="form-tip">数字越大权限越高，高层级自动继承低层级权限</span>
            </el-form-item>
            <el-form-item label="描述">
              <el-input v-model="roleForm.description" type="textarea" :rows="3" placeholder="角色描述" maxlength="200" show-word-limit />
            </el-form-item>
            <el-form-item label="状态">
              <el-switch v-model="roleForm.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="禁用" />
            </el-form-item>
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="权限分配" name="permissions">
          <div class="perm-tab-content">
            <div class="perm-tab-toolbar">
              <el-input v-model="permSearchKeyword" placeholder="搜索权限..." prefix-icon="Search" clearable size="small" style="width: 220px" />
              <el-radio-group v-model="permTypeFilter" size="small">
                <el-radio-button label="">全部</el-radio-button>
                <el-radio-button label="menu">菜单</el-radio-button>
                <el-radio-button label="button">按钮</el-radio-button>
              </el-radio-group>
              <el-button size="small" type="info" plain @click="selectAllReadOnly">
                一键只读
              </el-button>
              <el-button size="small" plain @click="clearAllPermissions">
                清空选择
              </el-button>
            </div>
            <div class="perm-tab-summary">
              <span class="summary-item">
                已选 <strong>{{ selectedMenuCount }}</strong> 个菜单，<strong>{{ selectedButtonCount }}</strong> 个按钮
              </span>
              <span v-if="inheritedPermIds.length > 0" class="inherited-tip">
                （继承 {{ inheritedPermIds.length }} 项）
              </span>
            </div>
            <div class="perm-tab-hint" v-if="roleForm.level > 10">
              <el-alert type="info" :closable="false" show-icon>
                层级 {{ roleForm.level }}，将自动继承所有低层级角色的权限（灰色标识）
              </el-alert>
            </div>
            <div class="perm-tree-wrapper">
              <div v-for="(pageGroup, module) in filteredPageTree" :key="module" class="perm-module-group">
                <div class="perm-module-header">
                  <el-checkbox
                    :model-value="isPageGroupAllChecked(pageGroup)"
                    :indeterminate="isPageGroupIndeterminate(pageGroup)"
                    @change="(val) => togglePageGroup(pageGroup, val)"
                  >
                    <span class="perm-module-title">📋 {{ module }}</span>
                  </el-checkbox>
                  <div class="module-header-right">
                    <el-tag size="small" type="success" effect="light" v-if="getPageGroupMenuCount(pageGroup)">{{ getPageGroupMenuCount(pageGroup) }} 菜单</el-tag>
                    <el-tag size="small" type="warning" effect="light" v-if="getPageGroupButtonCount(pageGroup)">{{ getPageGroupButtonCount(pageGroup) }} 按钮</el-tag>
                  </div>
                </div>
                <div class="perm-page-tree">
                  <div v-for="page in pageGroup.pages" :key="page.menuPerm.id" class="perm-page-node">
                    <!-- 页面级（菜单权限）-->
                    <div class="page-node-header" :class="{ inherited: isInherited(page.menuPerm.id) }">
                      <el-checkbox
                        :model-value="roleForm.permissionIds.includes(page.menuPerm.id) || isInherited(page.menuPerm.id)"
                        :disabled="isInherited(page.menuPerm.id) && !roleForm.permissionIds.includes(page.menuPerm.id)"
                        @change="(val) => togglePagePerm(page, val)"
                      >
                        <span class="page-node-label">
                          <span class="perm-type-dot dot-menu"></span>
                          <span class="page-name">{{ page.menuPerm.name }}</span>
                          <el-tag size="small" type="success" class="perm-type-tag">页面</el-tag>
                        </span>
                      </el-checkbox>
                      <span class="page-btn-count" v-if="page.buttons.length">{{ page.buttons.length }} 个按钮</span>
                    </div>
                    <!-- 按钮级权限（子节点）-->
                    <div class="page-buttons" v-if="page.buttons.length > 0">
                      <div v-for="btn in page.buttons" :key="btn.id" class="button-node" :class="{ inherited: isInherited(btn.id) }">
                        <div class="button-connector"></div>
                        <el-checkbox
                          :model-value="roleForm.permissionIds.includes(btn.id) || isInherited(btn.id)"
                          :disabled="isInherited(btn.id) && !roleForm.permissionIds.includes(btn.id)"
                          @change="(val) => toggleButtonPerm(page, btn.id, val)"
                        >
                          <span class="button-node-label">
                            <span class="perm-type-dot dot-button"></span>
                            <span class="btn-name">{{ btn.name }}</span>
                            <el-tag size="small" type="warning" class="perm-type-tag">按钮</el-tag>
                          </span>
                        </el-checkbox>
                        <span class="btn-page-hint">{{ permPageMap[btn.code] || '' }}</span>
                      </div>
                    </div>
                  </div>
                  <!-- 未归类的按钮（无对应菜单权限的）-->
                  <div v-if="pageGroup.ungrouped && pageGroup.ungrouped.length > 0" class="perm-page-node">
                    <div class="page-node-header ungrouped-header">
                      <span class="page-node-label">
                        <span class="perm-type-dot dot-button"></span>
                        <span class="page-name">其他操作权限</span>
                      </span>
                    </div>
                    <div class="page-buttons">
                      <div v-for="btn in pageGroup.ungrouped" :key="btn.id" class="button-node" :class="{ inherited: isInherited(btn.id) }">
                        <div class="button-connector"></div>
                        <el-checkbox
                          :model-value="roleForm.permissionIds.includes(btn.id) || isInherited(btn.id)"
                          :disabled="isInherited(btn.id) && !roleForm.permissionIds.includes(btn.id)"
                          @change="(val) => togglePerm(btn.id, val)"
                        >
                          <span class="button-node-label">
                            <span class="perm-type-dot dot-button"></span>
                            <span class="btn-name">{{ btn.name }}</span>
                            <el-tag size="small" type="warning" class="perm-type-tag">按钮</el-tag>
                          </span>
                        </el-checkbox>
                        <span class="btn-page-hint">{{ permPageMap[btn.code] || '' }}</span>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm" :loading="submitLoading">
          {{ submitLoading ? '保存中...' : '确认保存' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- User Assignment Drawer -->
    <el-drawer
      v-model="userDrawerVisible"
      :title="`分配用户 - ${currentRole?.name || ''}`"
      size="560px"
      destroy-on-close
      class="user-drawer"
    >
      <div class="user-drawer-content" v-loading="userLoading" element-loading-text="加载用户中...">
        <div class="user-drawer-info">
          <el-alert type="info" :closable="false" show-icon>
            当前角色已分配给 {{ roleUsers.length }} 个用户
          </el-alert>
        </div>
        <div class="user-list" v-if="roleUsers.length > 0">
          <div v-for="u in roleUsers" :key="u.userType + '-' + u.userId" class="user-item">
            <div class="user-item-info">
              <el-icon class="user-item-icon"><User /></el-icon>
              <span class="user-item-name">{{ u.displayName || u.username }}</span>
              <el-tag size="small" :type="u.userType === 'ADMIN' ? 'warning' : 'success'">
                {{ u.userType === 'ADMIN' ? '管理员' : '读者' }}
              </el-tag>
            </div>
            <el-button v-permission="'USER_ROLE_ASSIGN'" type="danger" size="small" text @click="removeUserFromRole(u)">
              <el-icon><Delete /></el-icon> 移除
            </el-button>
          </div>
        </div>
        <el-empty v-else description="暂无用户分配此角色" :image-size="80" />
      </div>
    </el-drawer>

    <!-- Permission Assignment Drawer (legacy quick access) -->
    <el-drawer
      v-model="permDrawerVisible"
      :title="`分配权限 - ${currentRole?.name || ''}`"
      size="520px"
      destroy-on-close
      class="perm-drawer"
    >
      <div class="perm-drawer-content" v-loading="permLoading" element-loading-text="加载权限中...">
        <div class="perm-info" v-if="currentRole && currentRole.level > 10">
          <el-alert type="info" :closable="false" show-icon>
            <template #default>
              层级为 {{ currentRole.level }}，将自动继承所有低层级角色的权限（灰色标识）
            </template>
          </el-alert>
        </div>
        <div v-for="(perms, module) in groupedPermissions" :key="module" class="perm-group">
          <div class="perm-group-title">
            <span class="perm-group-icon">📋</span>
            {{ module }}
            <el-tag size="small" type="info" class="perm-group-count">{{ perms.length }}</el-tag>
          </div>
          <div class="perm-group-items">
            <el-checkbox
              v-for="p in perms" :key="p.id"
              :model-value="selectedPermIds.includes(p.id)"
              :disabled="drawerInheritedPermIds.includes(p.id) && !selectedPermIds.includes(p.id)"
              @change="(val) => toggleDrawerPerm(p.id, val)"
              class="perm-checkbox"
              :class="{ inherited: drawerInheritedPermIds.includes(p.id) && !ownPermIds.includes(p.id) }"
            >
              {{ p.name }}
              <el-tag size="small" :type="p.type === 'menu' ? 'success' : 'warning'" class="perm-inline-tag">
                {{ p.type === 'menu' ? '菜单' : '按钮' }}
              </el-tag>
            </el-checkbox>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="permDrawerVisible = false">取消</el-button>
        <el-button type="primary" @click="savePermissions" :loading="permSaveLoading">
          {{ permSaveLoading ? '保存中...' : '保存权限' }}
        </el-button>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getRoles, createRole, updateRole, deleteRole,
  getPermissions, getRolePermissions, getEffectivePermissions, assignRolePermissions,
  getAllUsersWithRoles, getRoleUsers, assignUserRoles, getUserRoles
} from '@/api'

const loading = ref(false)
const roleList = ref([])
const searchKeyword = ref('')
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)

const roleStats = computed(() => {
  const list = roleList.value
  return {
    total: total.value,
    active: list.filter(r => r.status === 1).length,
    maxLevel: list.reduce((max, r) => Math.max(max, r.level || 0), 0),
    totalUsers: list.reduce((sum, r) => sum + (r.userCount || 0), 0)
  }
})

const sortedRoles = computed(() => {
  return [...roleList.value].sort((a, b) => (b.level || 0) - (a.level || 0))
})

function getLevelClass(level) {
  if (level >= 100) return 'level-super'
  if (level >= 50) return 'level-admin'
  return 'level-reader'
}

// Dialog state
const dialogVisible = ref(false)
const activeTab = ref('basic')
const editingRole = ref(null)
const submitLoading = ref(false)
const formRef = ref(null)
const roleForm = reactive({ code: '', name: '', level: 20, description: '', status: 1, permissionIds: [] })
const formRules = {
  code: [{ required: true, message: '请输入角色编码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  level: [{ required: true, message: '请设置层级', trigger: 'blur' }]
}

const permPageMap = {
  'BOOK_CREATE': '图书管理 → 新增图书',
  'BOOK_READ': '图书管理 → 页面访问',
  'BOOK_UPDATE': '图书管理 → 编辑按钮',
  'BOOK_DELETE': '图书管理 → 删除按钮',
  'READER_CREATE': '读者管理 → 新增读者',
  'READER_READ': '读者管理 → 页面访问',
  'READER_UPDATE': '读者管理 → 编辑/状态按钮',
  'READER_DELETE': '读者管理 → 删除按钮',
  'CATEGORY_CREATE': '分类管理 → 新增分类',
  'CATEGORY_READ': '分类管理 → 页面访问',
  'CATEGORY_UPDATE': '分类管理 → 编辑按钮',
  'CATEGORY_DELETE': '分类管理 → 删除按钮',
  'BORROW_CREATE': '借阅管理 → 新建借阅',
  'BORROW_READ': '借阅管理 → 页面访问',
  'BORROW_UPDATE': '借阅管理 → 归还/续借',
  'RESERVATION_READ': '预约管理 → 页面访问',
  'RESERVATION_UPDATE': '预约管理 → 审批按钮',
  'REVIEW_READ': '评价管理 → 页面访问',
  'REVIEW_UPDATE': '评价管理 → 通过/拒绝/回复',
  'REVIEW_DELETE': '评价管理 → 删除按钮',
  'FILE_CREATE': '资源管理 → 上传文件',
  'FILE_READ': '资源管理 → 页面访问',
  'FILE_DELETE': '资源管理 → 删除按钮',
  'DASHBOARD_READ': '数据概览 → 页面访问',
  'AUDIT_LOG_READ': '审计日志 → 页面访问',
  'ADMIN_APPLICATION_REVIEW': '管理员审批 → 通过/拒绝',
  'APPEAL_READ': '申诉管理 → 页面访问',
  'APPEAL_REVIEW': '申诉管理 → 审核按钮',
  'ROLE_MANAGE': '角色管理 → 页面及操作',
  'PERMISSION_MANAGE': '权限管理 → 页面及操作',
  'USER_ROLE_ASSIGN': '用户角色 → 分配操作',
  'READER_RESERVATION_CREATE': '读者端 → 预约图书',
  'READER_RESERVATION_READ': '读者端 → 预约列表',
  'READER_RESERVATION_CANCEL': '读者端 → 取消预约',
  'READER_REVIEW_CREATE': '读者端 → 写评价',
  'READER_REVIEW_READ': '读者端 → 评价列表',
  'READER_REVIEW_UPDATE': '读者端 → 修改评价',
  'READER_REVIEW_DELETE': '读者端 → 删除评价',
  'READING_PROGRESS_CREATE': '读者端 → 添加进度',
  'READING_PROGRESS_READ': '读者端 → 进度列表',
  'READING_PROGRESS_UPDATE': '读者端 → 编辑进度',
  'READING_PROGRESS_DELETE': '读者端 → 删除进度',
  'READER_PROFILE_READ': '读者端 → 个人中心',
  'READER_PROFILE_UPDATE': '读者端 → 编辑资料',
  'READER_BOOK_BROWSE': '读者端 → 图书浏览',
  'READER_CATEGORY_BROWSE': '读者端 → 分类浏览',
  'READER_BORROW_READ': '读者端 → 借阅记录',
  'READER_APPEAL_CREATE': '读者端 → 提交申诉',
  'READER_APPEAL_VIEW': '读者端 → 申诉列表',
  'ADMIN_APPLICATION_APPLY': '读者端 → 申请管理员',
  'ADMIN_APPLICATION_STATUS': '读者端 → 申请状态',
}

// Permission tab state
const allPermissions = ref([])
const permSearchKeyword = ref('')
const permTypeFilter = ref('')
const inheritedPermIds = ref([])

const filteredTreePermissions = computed(() => {
  let perms = allPermissions.value
  if (permTypeFilter.value) {
    perms = perms.filter(p => p.type === permTypeFilter.value)
  }
  if (permSearchKeyword.value) {
    const kw = permSearchKeyword.value.toLowerCase()
    perms = perms.filter(p => p.name.toLowerCase().includes(kw) || p.code.toLowerCase().includes(kw))
  }
  const grouped = {}
  perms.forEach(p => {
    const mod = p.module || '其他'
    if (!grouped[mod]) grouped[mod] = []
    grouped[mod].push(p)
  })
  return grouped
})

function isInherited(permId) {
  return inheritedPermIds.value.includes(permId) && !roleForm.permissionIds.includes(permId)
}

function isModuleAllChecked(module, perms) {
  return perms.every(p => roleForm.permissionIds.includes(p.id) || isInherited(p.id))
}

function isModuleIndeterminate(module, perms) {
  const checked = perms.filter(p => roleForm.permissionIds.includes(p.id) || isInherited(p.id))
  return checked.length > 0 && checked.length < perms.length
}

function toggleModule(module, perms, checked) {
  if (checked) {
    perms.forEach(p => {
      if (!roleForm.permissionIds.includes(p.id) && !isInherited(p.id)) {
        roleForm.permissionIds.push(p.id)
      }
    })
  } else {
    const ids = perms.map(p => p.id)
    roleForm.permissionIds = roleForm.permissionIds.filter(id => !ids.includes(id))
  }
}

function togglePerm(permId, checked) {
  if (checked) {
    if (!roleForm.permissionIds.includes(permId)) {
      roleForm.permissionIds.push(permId)
    }
  } else {
    roleForm.permissionIds = roleForm.permissionIds.filter(id => id !== permId)
  }
}

// Page→Button tree structure for permission tab
const filteredPageTree = computed(() => {
  let perms = allPermissions.value
  if (permTypeFilter.value) {
    perms = perms.filter(p => p.type === permTypeFilter.value)
  }
  if (permSearchKeyword.value) {
    const kw = permSearchKeyword.value.toLowerCase()
    perms = perms.filter(p => p.name.toLowerCase().includes(kw) || p.code.toLowerCase().includes(kw))
  }

  const grouped = {}
  perms.forEach(p => {
    const mod = p.module || '其他'
    if (!grouped[mod]) grouped[mod] = { pages: [], ungrouped: [] }
  })

  const menuPerms = perms.filter(p => p.type === 'menu')
  const buttonPerms = perms.filter(p => p.type === 'button')

  const moduleMenuMap = {}
  menuPerms.forEach(p => {
    const mod = p.module || '其他'
    if (!moduleMenuMap[mod]) moduleMenuMap[mod] = []
    moduleMenuMap[mod].push(p)
  })

  const assignedButtonIds = new Set()

  Object.keys(grouped).forEach(mod => {
    const menus = moduleMenuMap[mod] || []
    const modButtons = buttonPerms.filter(p => (p.module || '其他') === mod)

    const pages = menus.map(menu => {
      const menuCodePrefix = menu.code.replace(/_READ$/, '')
      const children = modButtons.filter(btn => {
        if (assignedButtonIds.has(btn.id)) return false
        const btnPrefix = btn.code.replace(/_(CREATE|UPDATE|DELETE|CANCEL|REVIEW|ASSIGN|MANAGE|APPLY|STATUS|BROWSE)$/, '')
        return btnPrefix === menuCodePrefix
      })
      children.forEach(c => assignedButtonIds.add(c.id))
      return { menuPerm: menu, buttons: children }
    })

    const ungrouped = modButtons.filter(btn => !assignedButtonIds.has(btn.id))
    ungrouped.forEach(btn => assignedButtonIds.add(btn.id))

    grouped[mod] = { pages, ungrouped }
  })

  // Remove empty modules
  Object.keys(grouped).forEach(mod => {
    if (grouped[mod].pages.length === 0 && (!grouped[mod].ungrouped || grouped[mod].ungrouped.length === 0)) {
      delete grouped[mod]
    }
  })

  return grouped
})

const selectedMenuCount = computed(() => {
  const menuIds = allPermissions.value.filter(p => p.type === 'menu').map(p => p.id)
  return roleForm.permissionIds.filter(id => menuIds.includes(id)).length
})

const selectedButtonCount = computed(() => {
  const btnIds = allPermissions.value.filter(p => p.type === 'button').map(p => p.id)
  return roleForm.permissionIds.filter(id => btnIds.includes(id)).length
})

function isPageGroupAllChecked(pageGroup) {
  const allIds = getAllPageGroupPermIds(pageGroup)
  return allIds.length > 0 && allIds.every(id => roleForm.permissionIds.includes(id) || isInherited(id))
}

function isPageGroupIndeterminate(pageGroup) {
  const allIds = getAllPageGroupPermIds(pageGroup)
  const checked = allIds.filter(id => roleForm.permissionIds.includes(id) || isInherited(id))
  return checked.length > 0 && checked.length < allIds.length
}

function getAllPageGroupPermIds(pageGroup) {
  const ids = []
  pageGroup.pages.forEach(page => {
    ids.push(page.menuPerm.id)
    page.buttons.forEach(btn => ids.push(btn.id))
  })
  if (pageGroup.ungrouped) {
    pageGroup.ungrouped.forEach(btn => ids.push(btn.id))
  }
  return ids
}

function togglePageGroup(pageGroup, checked) {
  const allIds = getAllPageGroupPermIds(pageGroup)
  if (checked) {
    allIds.forEach(id => {
      if (!roleForm.permissionIds.includes(id) && !isInherited(id)) {
        roleForm.permissionIds.push(id)
      }
    })
  } else {
    roleForm.permissionIds = roleForm.permissionIds.filter(id => !allIds.includes(id))
  }
}

function getPageGroupMenuCount(pageGroup) {
  return pageGroup.pages.length
}

function getPageGroupButtonCount(pageGroup) {
  let count = 0
  pageGroup.pages.forEach(page => { count += page.buttons.length })
  if (pageGroup.ungrouped) count += pageGroup.ungrouped.length
  return count
}

function togglePagePerm(page, checked) {
  if (checked) {
    if (!roleForm.permissionIds.includes(page.menuPerm.id)) {
      roleForm.permissionIds.push(page.menuPerm.id)
    }
  } else {
    roleForm.permissionIds = roleForm.permissionIds.filter(id => id !== page.menuPerm.id)
    // Uncheck all child buttons when unchecking page
    const btnIds = page.buttons.map(b => b.id)
    roleForm.permissionIds = roleForm.permissionIds.filter(id => !btnIds.includes(id))
  }
}

function toggleButtonPerm(page, btnId, checked) {
  if (checked) {
    if (!roleForm.permissionIds.includes(btnId)) {
      roleForm.permissionIds.push(btnId)
    }
    // Auto-check parent page permission
    if (!roleForm.permissionIds.includes(page.menuPerm.id) && !isInherited(page.menuPerm.id)) {
      roleForm.permissionIds.push(page.menuPerm.id)
    }
  } else {
    roleForm.permissionIds = roleForm.permissionIds.filter(id => id !== btnId)
  }
}

function selectAllReadOnly() {
  const readPerms = allPermissions.value.filter(p => p.type === 'menu' || p.code.endsWith('_READ'))
  readPerms.forEach(p => {
    if (!roleForm.permissionIds.includes(p.id) && !isInherited(p.id)) {
      roleForm.permissionIds.push(p.id)
    }
  })
}

function clearAllPermissions() {
  roleForm.permissionIds = []
}

// Permission drawer state
const permDrawerVisible = ref(false)
const permLoading = ref(false)
const permSaveLoading = ref(false)
const currentRole = ref(null)
const groupedPermissions = ref({})
const selectedPermIds = ref([])
const ownPermIds = ref([])
const drawerInheritedPermIds = ref([])

// User drawer state
const userDrawerVisible = ref(false)
const userLoading = ref(false)
const roleUsers = ref([])

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

async function loadInheritedPermissions(level) {
  if (level <= 10) {
    inheritedPermIds.value = []
    return
  }
  try {
    const allRolesRes = await getRoles({ page: 1, size: 100 })
    const allRoles = allRolesRes.data.records || allRolesRes.data
    const lowerRoles = allRoles.filter(r => r.level < level && r.status === 1)
    const inherited = new Set()
    for (const role of lowerRoles) {
      const permRes = await getRolePermissions(role.id)
      const permIds = permRes.data || []
      permIds.forEach(id => inherited.add(id))
    }
    inheritedPermIds.value = [...inherited]
  } catch (e) {
    inheritedPermIds.value = []
  }
}

function handleAdd() {
  editingRole.value = null
  Object.assign(roleForm, { code: '', name: '', level: 20, description: '', status: 1, permissionIds: [] })
  activeTab.value = 'basic'
  inheritedPermIds.value = []
  dialogVisible.value = true
  loadInheritedPermissions(20)
}

async function handleEdit(row) {
  editingRole.value = row
  Object.assign(roleForm, { code: row.code, name: row.name, level: row.level, description: row.description, status: row.status, permissionIds: [] })
  activeTab.value = 'basic'
  dialogVisible.value = true
  // Load existing permission assignment
  try {
    const permRes = await getRolePermissions(row.id)
    roleForm.permissionIds = permRes.data || []
  } catch (e) {
    roleForm.permissionIds = []
  }
  loadInheritedPermissions(row.level)
}

async function submitForm() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) {
    activeTab.value = 'basic'
    return
  }
  submitLoading.value = true
  try {
    const data = {
      code: roleForm.code,
      name: roleForm.name,
      level: roleForm.level,
      description: roleForm.description,
      status: roleForm.status,
      permissionIds: roleForm.permissionIds
    }
    if (editingRole.value) {
      await updateRole(editingRole.value.id, data)
      ElMessage.success('更新成功')
    } else {
      await createRole(data)
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
    drawerInheritedPermIds.value = allEffective.filter(id => !ownPermIds.value.includes(id))
  } catch (e) {
    console.error(e)
  } finally {
    permLoading.value = false
  }
}

function toggleDrawerPerm(permId, checked) {
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

async function handleUsers(row) {
  currentRole.value = row
  userDrawerVisible.value = true
  userLoading.value = true
  try {
    const res = await getRoleUsers(row.id)
    const userRoleEntries = res.data || []
    // Resolve display names
    const usersRes = await getAllUsersWithRoles({ page: 1, size: 200 })
    const allUsers = usersRes.data.records || []
    const userMap = {}
    allUsers.forEach(u => { userMap[`${u.userType}-${u.userId}`] = u })
    roleUsers.value = userRoleEntries.map(ur => {
      const full = userMap[`${ur.userType}-${ur.userId}`]
      return {
        ...ur,
        displayName: full?.displayName || '',
        username: full?.username || ''
      }
    })
  } catch (e) {
    console.error(e)
    roleUsers.value = []
  } finally {
    userLoading.value = false
  }
}

async function removeUserFromRole(user) {
  await ElMessageBox.confirm(`确定要移除该用户的「${currentRole.value.name}」角色吗？`, '提示', { type: 'warning' })
  try {
    const rolesRes = await getUserRoles(user.userType, user.userId)
    const currentRoleIds = rolesRes.data.roleIds || []
    const newRoleIds = currentRoleIds.filter(id => id !== currentRole.value.id)
    await assignUserRoles(user.userType, user.userId, { roleIds: newRoleIds })
    ElMessage.success('已移除')
    handleUsers(currentRole.value)
    loadRoles()
  } catch (e) {
    console.error(e)
  }
}
</script>

<style scoped>
.roles-page {
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

/* 层级可视化 */
.hierarchy-card {
  background: white;
}

.hierarchy-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.hierarchy-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
}

.hierarchy-tip {
  font-size: 12px;
  color: var(--text-secondary);
}

.hierarchy-chain {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  flex-wrap: wrap;
  padding: 12px 0;
}

.hierarchy-node {
  display: flex;
  align-items: center;
  gap: 8px;
}

.hierarchy-badge {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 500;
}

.hierarchy-badge.level-super {
  background: linear-gradient(135deg, var(--pink-light), var(--pink));
  color: #c0392b;
}

.hierarchy-badge.level-admin {
  background: linear-gradient(135deg, var(--yellow), var(--yellow-warm));
  color: #b8860b;
}

.hierarchy-badge.level-reader {
  background: linear-gradient(135deg, var(--green-light), var(--green));
  color: #2d8a56;
}

.hierarchy-level {
  font-weight: 700;
  font-size: 12px;
}

.hierarchy-name {
  font-weight: 600;
}

.hierarchy-user-count {
  font-size: 11px;
  opacity: 0.7;
}

.hierarchy-arrow {
  display: flex;
  align-items: center;
  gap: 2px;
  color: var(--text-secondary);
  font-size: 11px;
}

.hierarchy-arrow-text {
  font-size: 10px;
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

/* 表格卡片 */
.table-card {
  background: white;
}

.role-code-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.role-emoji {
  font-size: 18px;
}

.table-row {
  transition: all 0.3s ease;
}

/* 操作按钮 */
.action-btn-perm {
  background: linear-gradient(135deg, var(--green), #8DD5BE) !important;
  border: none !important;
  color: #fff !important;
  font-size: 12px;
  padding: 5px 10px !important;
  height: 30px;
}

.action-btn-perm:hover {
  background: linear-gradient(135deg, #8DD5BE, var(--green)) !important;
  box-shadow: 0 3px 8px rgba(181, 234, 215, 0.5);
}

.action-btn-users {
  background: linear-gradient(135deg, var(--blue), #A8B5E0) !important;
  border: none !important;
  color: #fff !important;
  font-size: 12px;
  padding: 5px 10px !important;
  height: 30px;
}

.action-btn-users:hover {
  background: linear-gradient(135deg, #A8B5E0, var(--blue)) !important;
  box-shadow: 0 3px 8px rgba(199, 206, 234, 0.5);
}

.action-btn-edit {
  background: linear-gradient(135deg, var(--btn-edit-from), var(--btn-edit-to)) !important;
  border: none !important;
  color: #fff !important;
  font-size: 12px;
  padding: 5px 10px !important;
  height: 30px;
}

.action-btn-edit:hover {
  background: linear-gradient(135deg, var(--btn-edit-to), var(--btn-edit-from)) !important;
  box-shadow: 0 3px 8px rgba(167, 139, 250, 0.3);
}

.action-btn-delete {
  background: linear-gradient(135deg, var(--btn-delete-from), var(--btn-delete-to)) !important;
  border: none !important;
  color: #fff !important;
  font-size: 12px;
  padding: 5px 10px !important;
  height: 30px;
}

.action-btn-delete:hover {
  background: linear-gradient(135deg, var(--btn-delete-to), var(--btn-delete-from)) !important;
  box-shadow: 0 3px 8px rgba(255, 179, 186, 0.4);
}

.action-btn-delete:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* 分页 */
.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid #F5F5F5;
}

/* 弹窗样式 */
.role-dialog :deep(.el-dialog__header) {
  background: linear-gradient(135deg, var(--green-light), var(--blue-light));
  margin-right: 0;
  padding: 20px 24px;
}

.role-dialog :deep(.el-dialog__title) {
  font-weight: 600;
  font-size: 18px;
}

.role-tabs :deep(.el-tabs__header) {
  margin-bottom: 16px;
}

.role-form {
  padding: 8px 0;
}

.form-tip {
  margin-left: 12px;
  font-size: 12px;
  color: var(--text-secondary);
}

/* 权限Tab */
.perm-tab-content {
  max-height: 450px;
  display: flex;
  flex-direction: column;
}

.perm-tab-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.perm-tab-stats {
  margin-left: auto;
  font-size: 13px;
  color: var(--text-secondary);
}

.inherited-tip {
  color: var(--purple);
}

.perm-tab-hint {
  margin-bottom: 12px;
}

.perm-tree-wrapper {
  overflow-y: auto;
  max-height: 360px;
  padding-right: 4px;
}

.perm-module-group {
  margin-bottom: 16px;
  background: var(--bg-main);
  border-radius: var(--radius-md);
  padding: 14px;
}

.perm-module-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
  padding-bottom: 8px;
  border-bottom: 1px solid #F0F0F0;
}

.perm-module-title {
  font-weight: 600;
  font-size: 14px;
}

.perm-module-items {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 6px 12px;
}

.perm-tree-item {
  display: flex;
  align-items: center;
  padding: 4px 8px;
  border-radius: 6px;
  transition: background 0.2s;
}

.perm-tree-item:hover {
  background: rgba(149, 125, 173, 0.06);
}

.perm-tree-item.inherited {
  opacity: 0.55;
}

.perm-tree-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
}

.perm-type-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  flex-shrink: 0;
}

.perm-type-dot.dot-menu {
  background: var(--green);
}

.perm-type-dot.dot-button {
  background: var(--purple);
}

.perm-type-tag {
  margin-left: 6px;
  font-size: 10px !important;
  transform: scale(0.85);
}

.perm-page-hint {
  display: block;
  font-size: 11px;
  color: var(--text-secondary);
  margin-top: 2px;
  opacity: 0.7;
  font-weight: 400;
}

/* 权限抽屉 */
.perm-drawer :deep(.el-drawer__header) {
  background: linear-gradient(135deg, var(--blue-light), var(--purple-light));
  padding: 16px 20px;
  margin-bottom: 0;
}

.perm-drawer-content {
  padding: 16px;
}

.perm-info {
  margin-bottom: 16px;
}

.perm-group {
  margin-bottom: 20px;
  background: var(--bg-main);
  border-radius: var(--radius-md);
  padding: 16px;
  transition: all 0.2s ease;
}

.perm-group:hover {
  box-shadow: var(--shadow-soft);
}

.perm-group-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid #F0F0F0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.perm-group-icon {
  font-size: 16px;
}

.perm-group-count {
  margin-left: auto;
}

.perm-group-items {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 16px;
}

.perm-checkbox {
  margin-right: 0;
  transition: opacity 0.2s ease;
}

.perm-checkbox.inherited {
  opacity: 0.55;
}

.perm-inline-tag {
  margin-left: 4px;
  font-size: 10px !important;
  transform: scale(0.8);
}

/* 用户抽屉 */
.user-drawer :deep(.el-drawer__header) {
  background: linear-gradient(135deg, var(--green-light), var(--blue-light));
  padding: 16px 20px;
  margin-bottom: 0;
}

.user-drawer-content {
  padding: 16px;
}

.user-drawer-info {
  margin-bottom: 16px;
}

.user-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.user-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: var(--bg-main);
  border-radius: var(--radius-sm);
  transition: all 0.2s ease;
}

.user-item:hover {
  background: linear-gradient(135deg, var(--green-light), var(--blue-light));
  transform: translateX(4px);
}

.user-item-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.user-item-icon {
  color: var(--purple);
}

.user-item-name {
  font-size: 14px;
  font-weight: 500;
}

/* Page→Button tree structure */
.perm-tab-summary {
  font-size: 13px;
  color: var(--text-secondary);
  margin-bottom: 10px;
  padding: 8px 12px;
  background: linear-gradient(135deg, var(--green-light), var(--blue-light));
  border-radius: 8px;
}

.perm-tab-summary .summary-item strong {
  color: var(--purple);
  font-size: 15px;
}

.module-header-right {
  display: flex;
  gap: 6px;
}

.perm-page-tree {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.perm-page-node {
  border: 1px solid #F0F0F0;
  border-radius: 10px;
  padding: 10px 14px;
  background: white;
  transition: all 0.2s ease;
}

.perm-page-node:hover {
  border-color: var(--purple-light);
  box-shadow: 0 2px 8px rgba(149, 125, 173, 0.08);
}

.page-node-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.page-node-header.inherited {
  opacity: 0.55;
}

.page-node-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
}

.page-name {
  font-weight: 600;
  color: var(--text-primary);
}

.page-btn-count {
  font-size: 11px;
  color: var(--text-secondary);
  background: var(--bg-main);
  padding: 2px 8px;
  border-radius: 10px;
}

.page-buttons {
  margin-top: 8px;
  margin-left: 20px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.button-node {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  border-radius: 6px;
  transition: background 0.2s ease;
  position: relative;
}

.button-node:hover {
  background: rgba(149, 125, 173, 0.06);
}

.button-node.inherited {
  opacity: 0.55;
}

.button-connector {
  position: absolute;
  left: -8px;
  top: 50%;
  width: 8px;
  height: 1px;
  background: #E0E0E0;
}

.button-connector::before {
  content: '';
  position: absolute;
  left: 0;
  top: -12px;
  width: 1px;
  height: 24px;
  background: #E0E0E0;
}

.button-node-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
}

.btn-name {
  color: var(--text-primary);
}

.btn-page-hint {
  font-size: 11px;
  color: var(--text-secondary);
  margin-left: auto;
  opacity: 0.7;
}

.ungrouped-header {
  padding: 4px 0;
  border-bottom: 1px dashed #E8E8E8;
  margin-bottom: 4px;
}

@media (max-width: 1200px) {
  .stat-row {
    grid-template-columns: repeat(2, 1fr);
  }
  .perm-module-items {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .stat-row {
    grid-template-columns: 1fr;
  }
}
</style>
