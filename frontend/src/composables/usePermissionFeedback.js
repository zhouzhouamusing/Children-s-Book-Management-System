import { ElMessage, ElNotification } from 'element-plus'
import { PERMISSION_LABELS } from '@/constants/permissions'

export function usePermissionFeedback() {
  function showDenied(code, options = {}) {
    const label = PERMISSION_LABELS[code] || code
    const message = options.message || `您没有「${label}」的操作权限`

    ElMessage({
      message,
      type: 'warning',
      duration: 3000,
      showClose: true
    })
  }

  function showDeniedNotification(code, options = {}) {
    const label = PERMISSION_LABELS[code] || code
    ElNotification({
      title: '权限不足',
      message: options.message || `您没有「${label}」的操作权限，请联系管理员`,
      type: 'warning',
      duration: 5000
    })
  }

  function handlePermissionError(error) {
    const status = error?.response?.status || error?.code
    const serverMessage = error?.response?.data?.message || error?.message

    if (status === 403) {
      ElNotification({
        title: '操作被拒绝',
        message: serverMessage || '您没有执行此操作的权限，请联系管理员',
        type: 'error',
        duration: 5000
      })
      return true
    }
    return false
  }

  return { showDenied, showDeniedNotification, handlePermissionError }
}
