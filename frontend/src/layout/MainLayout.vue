<template>
  <div class="layout" :class="{ 'is-collapsed': isCollapse }">
    <el-aside class="sidebar" :width="isCollapse ? '64px' : '220px'">
      <div class="sidebar-header">
        <span class="sidebar-logo">📚</span>
        <transition name="fade">
          <span v-if="!isCollapse" class="sidebar-title">童书乐园</span>
        </transition>
      </div>
      <el-menu
        :default-active="route.path"
        :collapse="isCollapse"
        router
        class="sidebar-menu"
        background-color="transparent"
        text-color="#4A4A4A"
        active-text-color="#957DAD"
      >
        <el-menu-item index="/dashboard">
          <el-icon><DataAnalysis /></el-icon>
          <template #title>数据概览</template>
        </el-menu-item>
        <el-menu-item index="/books">
          <el-icon><Reading /></el-icon>
          <template #title>图书管理</template>
        </el-menu-item>
        <el-menu-item index="/categories">
          <el-icon><FolderOpened /></el-icon>
          <template #title>分类管理</template>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <div class="main-container">
      <el-header class="header">
        <div class="header-left">
          <el-button
            text
            @click="isCollapse = !isCollapse"
            class="collapse-btn"
          >
            <el-icon :size="20">
              <Fold v-if="!isCollapse" />
              <Expand v-else />
            </el-icon>
          </el-button>
          <span class="page-title">{{ route.meta.title }}</span>
        </div>
        <div class="header-right">
          <span class="welcome">你好，{{ nickname }} 👋</span>
          <el-button class="logout-btn" @click="handleLogout">
            <el-icon><SwitchButton /></el-icon>
            退出登录
          </el-button>
        </div>
      </el-header>

      <el-main class="content">
        <router-view v-slot="{ Component, route: childRoute }">
          <transition name="page-fade" mode="out-in">
            <component :is="Component" :key="childRoute.path" />
          </transition>
        </router-view>
      </el-main>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox, ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const isCollapse = ref(false)
const nickname = ref(localStorage.getItem('nickname') || '管理员')

const handleLogout = () => {
  ElMessageBox.confirm('确定要退出登录吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    localStorage.removeItem('token')
    localStorage.removeItem('nickname')
    ElMessage.success('已安全退出')
    router.push('/login')
  }).catch(() => {})
}
</script>

<style scoped>
.layout {
  display: flex;
  min-height: 100vh;
}

.sidebar {
  background: white;
  box-shadow: 4px 0 20px rgba(0, 0, 0, 0.04);
  transition: width 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
  position: fixed;
  left: 0;
  top: 0;
  bottom: 0;
  z-index: 100;
}

.sidebar-header {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  border-bottom: 1px solid #F5F5F5;
}

.sidebar-logo {
  font-size: 28px;
}

.sidebar-title {
  font-size: 18px;
  font-weight: 700;
  background: linear-gradient(135deg, var(--purple), var(--pink));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  white-space: nowrap;
}

.sidebar-menu {
  border-right: none !important;
  padding: 10px 8px;
}

.sidebar-menu .el-menu-item {
  border-radius: var(--radius-sm);
  margin-bottom: 4px;
  height: 48px;
  line-height: 48px;
}

.sidebar-menu .el-menu-item.is-active {
  background: linear-gradient(135deg, var(--green-light), var(--blue-light)) !important;
}

.main-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  margin-left: 220px;
  transition: margin-left 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  min-height: 100vh;
}

.is-collapsed .main-container {
  margin-left: 64px;
}

.header {
  height: 64px;
  background: white;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.04);
  position: sticky;
  top: 0;
  z-index: 50;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.collapse-btn {
  font-size: 20px;
}

.page-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.welcome {
  color: var(--text-secondary);
  font-size: 14px;
}

.logout-btn {
  background: linear-gradient(135deg, #FF6B81, #FF8A9E) !important;
  border: none !important;
  color: #fff !important;
  font-size: 13px;
  padding: 8px 16px !important;
  height: 36px;
}

.logout-btn:hover {
  background: linear-gradient(135deg, #FF4757, #FF6B81) !important;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(255, 107, 129, 0.4);
}

.content {
  padding: 24px;
  background: var(--bg-main);
  flex: 1;
  overflow-x: hidden;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.page-fade-enter-active {
  transition: opacity 0.25s ease-in;
}
.page-fade-leave-active {
  transition: opacity 0.15s ease-out;
}
.page-fade-enter-from,
.page-fade-leave-to {
  opacity: 0;
}
</style>
