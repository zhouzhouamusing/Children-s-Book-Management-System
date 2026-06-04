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
                </div>
                <el-tag size="small" class="perm-code-tag" effect="plain">{{ p.code }}</el-tag>
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
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { getPermissionsWithRoles, getPermissionModules } from '@/api'

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

onMounted(async () => {
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
})

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
  grid-template-columns: repeat(auto-fill, minmax(420px, 1fr));
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
