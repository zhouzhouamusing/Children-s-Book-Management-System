<template>
  <div class="permissions-page">
    <el-card class="header-card animate__animated animate__fadeInDown">
      <div class="page-header">
        <div class="header-info">
          <h3>权限管理</h3>
          <span class="header-desc">管理系统所有权限，支持增删改查操作</span>
        </div>
        <div class="header-actions">
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
          <el-button v-permission="'permission:add'" type="primary" @click="handleAdd(null)">
            <el-icon><Plus /></el-icon> 新增权限
          </el-button>
        </div>
      </div>
    </el-card>

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
        <el-button v-permission="'permission:view'" text type="warning" @click="checkIntegrity">完整性检查</el-button>
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
                <el-button v-permission="'permission:add'" text type="primary" size="small" @click.stop="handleAdd(data)">
                  <el-icon><Plus /></el-icon>
                </el-button>
                <el-button v-permission="'permission:edit'" text type="warning" size="small" @click.stop="handleEdit(data)">
                  <el-icon><Edit /></el-icon>
                </el-button>
                <el-button v-permission="'permission:delete'" text type="danger" size="small" @click.stop="handleDelete(data)">
                  <el-icon><Delete /></el-icon>
                </el-button>
              </div>
            </div>
          </template>
        </el-tree>

        <el-empty v-if="!loading && permTree.length === 0" description="暂无权限数据" />
      </div>
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑权限' : '新增权限'" width="520px" destroy-on-close>
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="90px">
        <el-form-item label="权限编码" prop="code">
          <el-input v-model="formData.code" placeholder="如: book:add" maxlength="100" />
        </el-form-item>
        <el-form-item label="权限名称" prop="name">
          <el-input v-model="formData.name" placeholder="如: 新增图书" maxlength="100" />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-select v-model="formData.type" style="width: 100%">
            <el-option label="菜单" value="menu" />
            <el-option label="按钮" value="button" />
          </el-select>
        </el-form-item>
        <el-form-item label="父权限">
          <el-tree-select
            v-model="formData.parentId"
            :data="parentOptions"
            :props="{ label: 'name', value: 'id', children: 'children' }"
            check-strictly
            clearable
            placeholder="顶级权限（无父级）"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="路由路径" v-if="formData.type === 'menu'">
          <el-input v-model="formData.path" placeholder="如: /books" />
        </el-form-item>
        <el-form-item label="图标" v-if="formData.type === 'menu'">
          <el-input v-model="formData.icon" placeholder="如: Reading" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="formData.sortOrder" :min="0" :max="999" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="formData.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="禁用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确认</el-button>
      </template>
    </el-dialog>

    <!-- 完整性检查结果 -->
    <el-dialog v-model="integrityVisible" title="权限完整性检查报告" width="600px">
      <div v-if="integrityReport">
        <el-result v-if="integrityReport.healthy" icon="success" title="权限数据完整" sub-title="未发现异常数据" />
        <div v-else>
          <el-alert type="warning" :closable="false" title="发现数据完整性问题" style="margin-bottom: 16px" />
          <el-descriptions :column="1" border>
            <el-descriptions-item label="无效角色-权限关联">{{ integrityReport.orphanRolePermissionCount }} 条</el-descriptions-item>
            <el-descriptions-item label="无效用户-角色关联">{{ integrityReport.orphanUserRoleCount }} 条</el-descriptions-item>
            <el-descriptions-item label="未分配的权限">{{ integrityReport.unassignedPermissionCount }} 条</el-descriptions-item>
          </el-descriptions>
        </div>
      </div>
      <template #footer>
        <el-button @click="integrityVisible = false">关闭</el-button>
        <el-button v-if="integrityReport && !integrityReport.healthy" v-permission="'permission:delete'" type="danger" @click="doRepair">修复</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getPermissionTree, addPermission, updatePermission, deletePermission, checkPermissionIntegrity, repairPermissionIntegrity } from '@/api/index'

const loading = ref(false)
const permTree = ref([])
const filterText = ref('')
const treeRef = ref(null)
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitLoading = ref(false)
const formRef = ref(null)
const editId = ref(null)
const integrityVisible = ref(false)
const integrityReport = ref(null)

const formData = ref({
  code: '', name: '', type: 'button', parentId: null, path: '', icon: '', sortOrder: 0, status: 1
})

const formRules = {
  code: [{ required: true, message: '请输入权限编码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入权限名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }]
}

const treeProps = { label: 'name', children: 'children' }

const parentOptions = computed(() => {
  const topLevel = [{ id: 0, name: '顶级（无父级）', children: null }]
  return topLevel.concat(permTree.value.map(n => ({ id: n.id, name: n.name, children: null })))
})

const totalCount = computed(() => countNodes(permTree.value))
const menuCount = computed(() => countByType(permTree.value, 'menu'))
const buttonCount = computed(() => countByType(permTree.value, 'button'))

onMounted(() => { fetchTree() })

watch(filterText, (val) => { treeRef.value?.filter(val) })

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
  if (nodes) Object.values(nodes).forEach(n => n.expand())
}

function collapseAll() {
  const nodes = treeRef.value?.store?.nodesMap
  if (nodes) Object.values(nodes).forEach(n => n.collapse())
}

function handleAdd(parentNode) {
  isEdit.value = false
  editId.value = null
  formData.value = {
    code: '', name: '', type: parentNode ? 'button' : 'menu',
    parentId: parentNode ? parentNode.id : 0,
    path: '', icon: '', sortOrder: 0, status: 1
  }
  dialogVisible.value = true
}

function handleEdit(data) {
  isEdit.value = true
  editId.value = data.id
  formData.value = {
    code: data.code, name: data.name, type: data.type,
    parentId: data.parentId || 0,
    path: data.path || '', icon: data.icon || '',
    sortOrder: data.sortOrder || 0, status: data.status != null ? data.status : 1
  }
  dialogVisible.value = true
}

async function handleDelete(data) {
  await ElMessageBox.confirm(`确定删除权限「${data.name}」？`, '提示', { type: 'warning' })
  try {
    await deletePermission(data.id)
    ElMessage.success('删除成功')
    fetchTree()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '删除失败')
  }
}

async function handleSubmit() {
  await formRef.value.validate()
  submitLoading.value = true
  try {
    if (isEdit.value) {
      await updatePermission(editId.value, formData.value)
      ElMessage.success('更新成功')
    } else {
      await addPermission(formData.value)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    fetchTree()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '操作失败')
  } finally {
    submitLoading.value = false
  }
}

async function checkIntegrity() {
  try {
    const res = await checkPermissionIntegrity()
    integrityReport.value = res.data
    integrityVisible.value = true
  } catch (e) {
    ElMessage.error('检查失败')
  }
}

async function doRepair() {
  await ElMessageBox.confirm('确定修复？将删除无效的关联数据', '确认修复', { type: 'warning' })
  try {
    const res = await repairPermissionIntegrity()
    integrityReport.value = res.data
    ElMessage.success('修复完成')
    fetchTree()
  } catch (e) {
    ElMessage.error('修复失败')
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

.header-actions {
  display: flex;
  align-items: center;
  gap: 24px;
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
  gap: 8px;
  font-size: 12px;
  color: var(--text-secondary);
}

.node-path, .node-icon-name {
  display: flex;
  align-items: center;
  gap: 4px;
}
</style>
