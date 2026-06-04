<template>
  <div class="permissions-page">
    <el-card class="search-card animate__animated animate__fadeInDown">
      <div class="search-bar">
        <el-select v-model="selectedModule" placeholder="全部模块" clearable size="large" @change="filterPermissions">
          <el-option v-for="mod in modules" :key="mod" :label="mod" :value="mod" />
        </el-select>
        <el-input
          v-model="searchKeyword"
          placeholder="搜索权限名称或编码..."
          prefix-icon="Search"
          size="large"
          clearable
          class="search-input"
          @input="filterPermissions"
        />
      </div>
    </el-card>

    <div class="perm-modules" v-loading="loading">
      <el-card v-for="(perms, module) in filteredGroups" :key="module" class="module-card">
        <template #header>
          <div class="module-header">
            <span class="module-title">{{ module }}</span>
            <el-tag size="small" type="info">{{ perms.length }} 项</el-tag>
          </div>
        </template>
        <div class="perm-list">
          <div v-for="p in perms" :key="p.id" class="perm-item">
            <div class="perm-name">{{ p.name }}</div>
            <div class="perm-code">{{ p.code }}</div>
          </div>
        </div>
      </el-card>
    </div>

    <el-empty v-if="!loading && Object.keys(filteredGroups).length === 0" description="未找到匹配的权限" />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { getPermissions, getPermissionModules } from '@/api'

const loading = ref(false)
const allPermissions = ref([])
const modules = ref([])
const selectedModule = ref('')
const searchKeyword = ref('')
const groupedPermissions = ref({})

onMounted(async () => {
  loading.value = true
  try {
    const [permRes, modRes] = await Promise.all([getPermissions(), getPermissionModules()])
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
</script>

<style scoped>
.permissions-page {
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

.perm-modules {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(360px, 1fr));
  gap: 16px;
}

.module-card {
  border-radius: var(--radius-lg);
}

.module-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.module-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}

.perm-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.perm-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background: var(--bg-main);
  border-radius: var(--radius-sm);
  transition: all 0.2s ease;
}

.perm-item:hover {
  background: linear-gradient(135deg, var(--green-light), var(--blue-light));
}

.perm-name {
  font-size: 14px;
  color: var(--text-primary);
  font-weight: 500;
}

.perm-code {
  font-size: 12px;
  color: var(--text-secondary);
  font-family: monospace;
}
</style>
