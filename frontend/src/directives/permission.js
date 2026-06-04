import { usePermissionStore } from '@/stores/permission'

export const vPermission = {
  mounted(el, binding) {
    const store = usePermissionStore()
    const required = binding.value
    if (!required) return

    const codes = Array.isArray(required) ? required : [required]
    const hasAny = codes.some(c => store.hasPermission(c))
    if (!hasAny) {
      el.parentNode?.removeChild(el)
    }
  }
}
