<template>
  <div class="permissions-page">
    <!-- 页面标题 -->
    <el-card class="header-card animate__animated animate__fadeInDown">
      <div class="page-header">
        <div class="header-info">
          <h3>权限列表</h3>
          <span class="header-desc">系统所有权限的树形结构，可在角色管理中为角色分配这些权限</span>
        </div>
        <div class="header-stats">
          <div class="stat-item">
            <span class="stat-num">{{ menuCount }}</span>
            <span class="stat-label">菜单权限</span>
          </div>
          <div class="stat-item">
            <span class="stat-num">{{ buttonCount }}</span>
            <span class="stat-label">按钮权限</span>
          </div>
          <div class="stat-item">
            <span class="stat-num">{{ totalCount }}</span>
            <span class="stat-label">权限总数</span>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 权限树 -->
    <el-card class="tree-card" v-loading="loading" element-loading-text="正在加载权限数据...">
      <div class="tree-toolbar">
        <el-input
          v-model="filterText"
          placeholder="搜索权限名称或编码..."
          prefix-icon="Search"
          clearable
          class="filter-input"
        />
        <el-button text type="primary" @click="expandAll">全部展开</el-button>
        <el-button text @click="collapseAll">全部收起</el-button>
      </div>

      <div class="tree-wrapper">
        <el-tree
          ref="treeRef"
          :data="permTree"
          :props="treeProps"
          node-key="id"
          :default-expand-all="true"
          :filter-node-method="filterNode"
          :expand-on-click-node="false"
        >
          <template #default="{ node, data }">
            <div class="tree-node">
              <div class="node-left">
                <el-tag
                  :type="data.type === 'menu' ? 'success' : 'warning'"
                  size="small"
                  class="type-tag"
                >
                  {{ data.type === 'menu' ? '菜单' : '按钮' }}
                </el-tag>
                <span class="node-name">{{ data.name }}</span>
                <span class="node-code">{{ data.code }}</span>
              </div>
              <div class="node-right">
                <span v-if="data.path" class="node-path">
                  <el-icon><Link /></el-icon> {{ data.path }}
                </span>
                <span v-if="data.icon" class="node-icon-name">
                  <el-icon><component :is="data.icon" /></el-icon> {{ data.icon }}
                </span>
              </div>
            </div>
          </template>
        </el-tree>

        <el-empty v-if="!loading && permTree.length === 0" description="暂无权限数据" />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { getPermissionTree } from '@/api/index'

const loading = ref(false)
const permTree = ref([])
const filterText = ref('')
const treeRef = ref(null)

const treeProps = {
  label: 'name',
  children: 'children'
}

const totalCount = computed(() => countNodes(permTree.value))
const menuCount = computed(() => countByType(permTree.value, 'menu'))
const buttonCount = computed(() => countByType(permTree.value, 'button'))

onMounted(() => {
  fetchTree()
})

watch(filterText, (val) => {
  treeRef.value?.filter(val)
})

async function fetchTree() {
  loading.value = true
  try {
    const res = await getPermissionTree()
    permTree.value = res.data || []
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

function filterNode(value, data) {
  if (!value) return true
  return data.name.includes(value) || data.code.includes(value)
}

function expandAll() {
  const nodes = treeRef.value?.store?.nodesMap
  if (nodes) {
    Object.values(nodes).forEach(n => n.expand())
  }
}

function collapseAll() {
  const nodes = treeRef.value?.store?.nodesMap
  if (nodes) {
    Object.values(nodes).forEach(n => n.collapse())
  }
}

function countNodes(nodes) {
  let count = 0
  for (const n of nodes) {
    count++
    if (n.children) count += countNodes(n.children)
  }
  return count
}

function countByType(nodes, type) {
  let count = 0
  for (const n of nodes) {
    if (n.type === type) count++
    if (n.children) count += countByType(n.children, type)
  }
  return count
}
</script>

<style scoped>
.permissions-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.header-card {
  border-radius: var(--radius-md);
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-info h3 {
  margin: 0 0 4px;
  font-size: 18px;
  color: var(--text-primary);
}

.header-desc {
  font-size: 13px;
  color: var(--text-secondary);
}

.header-stats {
  display: flex;
  gap: 24px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}

.stat-num {
  font-size: 22px;
  font-weight: 700;
  background: linear-gradient(135deg, var(--purple), var(--pink));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.stat-label {
  font-size: 12px;
  color: var(--text-secondary);
}

.tree-card {
  border-radius: var(--radius-md);
}

.tree-toolbar {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
}

.filter-input {
  width: 280px;
}

.tree-wrapper {
  max-height: 600px;
  overflow-y: auto;
  padding: 4px;
}

.tree-node {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding: 4px 8px;
  font-size: 14px;
}

.node-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.type-tag {
  border-radius: 10px !important;
  font-size: 11px !important;
  padding: 0 8px !important;
  height: 20px !important;
  line-height: 20px !important;
}

.node-name {
  font-weight: 500;
  color: var(--text-primary);
}

.node-code {
  font-size: 12px;
  color: #999;
  font-family: monospace;
  background: #f5f5f5;
  padding: 1px 6px;
  border-radius: 4px;
}

.node-right {
  display: flex;
  align-items: center;
  gap: 16px;
  font-size: 12px;
  color: var(--text-secondary);
}

.node-path, .node-icon-name {
  display: flex;
  align-items: center;
  gap: 4px;
}
</style>
