<template>
  <router-view />
</template>

<script setup>
import { onMounted, onUnmounted } from 'vue'
import { ElNotification } from 'element-plus'
import { PERMISSION_LABELS } from '@/constants/permissions'

let permHandler = null
let apiPermHandler = null

onMounted(() => {
  permHandler = (e) => {
    const { reason, requiredPermission } = e.detail || {}
    let message = reason || '您没有访问该页面的权限'
    if (requiredPermission && typeof requiredPermission === 'string') {
      const label = PERMISSION_LABELS[requiredPermission]
      if (label) message = `需要「${label}」权限才能访问该页面`
    }
    ElNotification({
      title: '访问受限',
      message,
      type: 'warning',
      duration: 4000
    })
  }

  apiPermHandler = (e) => {
    const { context, message } = e.detail || {}
    ElNotification({
      title: '操作被拒绝',
      message: context ? `[${context}] ${message}` : message,
      type: 'error',
      duration: 5000
    })
  }

  window.addEventListener('permission-denied', permHandler)
  window.addEventListener('api-permission-denied', apiPermHandler)
})

onUnmounted(() => {
  if (permHandler) window.removeEventListener('permission-denied', permHandler)
  if (apiPermHandler) window.removeEventListener('api-permission-denied', apiPermHandler)
})
</script>
