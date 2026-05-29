<template>
  <div class="layout" :class="{ 'is-collapsed': isCollapse }">
    <el-aside class="sidebar" :width="isCollapse ? '64px' : '220px'">
      <div class="sidebar-header">
        <span class="sidebar-logo">📖</span>
        <transition name="fade">
          <div v-if="!isCollapse" class="sidebar-title-group">
            <span class="sidebar-title">童书乐园</span>
            <span class="sidebar-subtitle">读者中心</span>
          </div>
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
        <el-menu-item index="/reader/my-borrows">
          <el-icon><Reading /></el-icon>
          <template #title>我的借阅</template>
        </el-menu-item>
        <el-menu-item index="/reader/reservations">
          <el-icon><Calendar /></el-icon>
          <template #title>预约图书</template>
        </el-menu-item>
        <el-menu-item index="/reader/books">
          <el-icon><Search /></el-icon>
          <template #title>图书浏览</template>
        </el-menu-item>
        <el-menu-item index="/reader/recommend">
          <el-icon><Star /></el-icon>
          <template #title>图书推荐</template>
        </el-menu-item>
        <el-menu-item index="/reader/reading-progress">
          <el-icon><TrendCharts /></el-icon>
          <template #title>阅读进度</template>
        </el-menu-item>
        <el-menu-item index="/reader/my-reviews">
          <el-icon><ChatLineRound /></el-icon>
          <template #title>我的评价</template>
        </el-menu-item>
        <el-menu-item index="/reader/profile">
          <el-icon><User /></el-icon>
          <template #title>个人中心</template>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <div class="main-container">
      <el-header class="header">
        <div class="header-left">
          <el-button text @click="isCollapse = !isCollapse" class="collapse-btn">
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
            <keep-alive :max="5">
              <div :key="childRoute.path" class="page-wrapper">
                <component :is="Component" />
              </div>
            </keep-alive>
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
const nickname = ref(localStorage.getItem('nickname') || '读者')

const handleLogout = () => {
  ElMessageBox.confirm('确定要退出登录吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    localStorage.removeItem('token')
    localStorage.removeItem('nickname')
    localStorage.removeItem('role')
    localStorage.removeItem('readerId')
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

.sidebar-title-group {
  display: flex;
  flex-direction: column;
  line-height: 1.2;
}

.sidebar-title {
  font-size: 18px;
  font-weight: 700;
  background: linear-gradient(135deg, var(--purple), var(--pink));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.sidebar-subtitle {
  font-size: 11px;
  color: var(--text-secondary);
  letter-spacing: 2px;
}

.sidebar-menu {
  border-right: none;
  padding: 12px 8px;
}

.main-container {
  flex: 1;
  margin-left: 220px;
  transition: margin-left 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

.is-collapsed .main-container {
  margin-left: 64px;
}

.header {
  height: 64px;
  background: white;
  border-bottom: 1px solid #F5F5F5;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  position: sticky;
  top: 0;
  z-index: 99;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.collapse-btn {
  padding: 8px;
  color: var(--text-secondary);
}

.page-title {
  font-size: 16px;
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
  background: linear-gradient(135deg, #FFB3BA, #FF8A9B) !important;
  border: none !important;
  color: white !important;
  border-radius: 20px !important;
  padding: 8px 16px !important;
  font-size: 13px;
}

.logout-btn:hover {
  background: linear-gradient(135deg, #FF8A9B, #FFB3BA) !important;
  transform: translateY(-1px);
}

.content {
  flex: 1;
  padding: 20px;
  background: var(--bg-main);
  overflow-x: hidden;
}

.page-wrapper {
  width: 100%;
  max-width: 100%;
  animation: slideInUp 0.3s ease;
}

.page-fade-enter-active,
.page-fade-leave-active {
  transition: all 0.3s ease;
}

.page-fade-enter-from {
  opacity: 0;
  transform: translateY(10px);
}

.page-fade-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

@keyframes slideInUp {
  from {
    opacity: 0;
    transform: translateY(16px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
