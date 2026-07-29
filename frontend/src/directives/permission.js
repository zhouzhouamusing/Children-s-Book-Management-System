import { usePermissionStore } from '@/stores/permission'

export const vPermission = {
  mounted(el, binding) {
    const store = usePermissionStore()
    store.loadFromStorage()
    const required = binding.value

    let hasAccess = false
    if (Array.isArray(required)) {
      hasAccess = store.hasAnyPermission(required)
    } else {
      hasAccess = store.hasPermission(required)
    }

    if (!hasAccess) {
      el.parentNode?.removeChild(el)
    }
  }
}
