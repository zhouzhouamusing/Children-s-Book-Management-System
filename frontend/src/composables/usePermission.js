import { computed } from 'vue'
import { usePermissionStore } from '@/stores/permission'
import { PERMISSION_LABELS } from '@/constants/permissions'
import { ElMessage } from 'element-plus'

export function usePermission() {
  const store = usePermissionStore()

  const has = (code) => computed(() => store.hasPermission(code))

  const hasAny = (codes) => computed(() => store.hasAnyPermission(codes))

  const hasAll = (codes) => computed(() => store.hasAllPermissions(codes))

  const hasRole = (role) => computed(() => store.hasRole(role))

  function check(code) {
    return store.hasPermission(code)
  }

  function checkAny(codes) {
    return store.hasAnyPermission(codes)
  }

  function checkWithFeedback(code) {
    if (store.hasPermission(code)) return true
    const label = PERMISSION_LABELS[code] || code
    ElMessage({
      message: `您没有「${label}」的操作权限`,
      type: 'warning',
      duration: 3000,
      showClose: true
    })
    return false
  }

  function checkAnyWithFeedback(codes) {
    if (store.hasAnyPermission(codes)) return true
    const labels = codes.map(c => PERMISSION_LABELS[c] || c).join('、')
    ElMessage({
      message: `您缺少以下权限之一：${labels}`,
      type: 'warning',
      duration: 3000,
      showClose: true
    })
    return false
  }

  return { has, hasAny, hasAll, hasRole, check, checkAny, checkWithFeedback, checkAnyWithFeedback }
}
