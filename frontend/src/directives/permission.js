import { watchEffect } from 'vue'
import { usePermissionStore } from '@/stores/permission'

export const vPermission = {
  mounted(el, binding) {
    const store = usePermissionStore()
    el._v_perm_display = el.style.display || ''

    el._v_perm_stop = watchEffect(() => {
      // 订阅响应式依赖：permissions 数组和版本号
      void store.permissions
      void store.version

      const required = binding.value
      if (!required) return

      const codes = Array.isArray(required) ? required : [required]
      const hasAny = codes.some(c => store.hasPermission(c))

      if (binding.modifiers && binding.modifiers.disable) {
        el.disabled = !hasAny
        el.classList.toggle('is-permission-disabled', !hasAny)
        if (!hasAny) {
          el.setAttribute('title', `需要权限: ${codes.join(', ')}`)
        } else {
          el.removeAttribute('title')
        }
      } else {
        el.style.display = hasAny ? el._v_perm_display : 'none'
      }
    })
  },

  updated(el, binding) {
    if (binding.value !== binding.oldValue && el._v_perm_stop) {
      el._v_perm_stop()
      const store = usePermissionStore()

      el._v_perm_stop = watchEffect(() => {
        void store.permissions
        void store.version

        const required = binding.value
        if (!required) return

        const codes = Array.isArray(required) ? required : [required]
        const hasAny = codes.some(c => store.hasPermission(c))

        if (binding.modifiers && binding.modifiers.disable) {
          el.disabled = !hasAny
          el.classList.toggle('is-permission-disabled', !hasAny)
          if (!hasAny) {
            el.setAttribute('title', `需要权限: ${codes.join(', ')}`)
          } else {
            el.removeAttribute('title')
          }
        } else {
          el.style.display = hasAny ? el._v_perm_display : 'none'
        }
      })
    }
  },

  beforeUnmount(el) {
    if (el._v_perm_stop) {
      el._v_perm_stop()
      el._v_perm_stop = null
    }
  }
}
