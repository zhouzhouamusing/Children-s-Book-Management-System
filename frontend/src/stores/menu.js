import { ref } from 'vue'
import { defineStore } from 'pinia'
import { getMenusByPermission } from '@/api/index'
import { adminMenus } from '@/config/menus'
import { usePermissionStore } from './permission'

export const useMenuStore = defineStore('menu', () => {
  const menus = ref([])
  const loaded = ref(false)

  async function fetchMenus() {
    try {
      const res = await getMenusByPermission()
      if (res.data && res.data.length > 0) {
        menus.value = res.data.map(p => ({
          index: p.path,
          title: p.name,
          icon: p.icon,
          permission: p.code
        }))
        loaded.value = true
        return
      }
    } catch (e) {
      console.warn('Failed to fetch menus from API, using local fallback', e)
    }
    loadFromLocal()
  }

  function loadFromLocal() {
    const permStore = usePermissionStore()
    permStore.loadFromStorage()
    menus.value = adminMenus.filter(m => permStore.hasPermission(m.permission))
    loaded.value = true
  }

  function clear() {
    menus.value = []
    loaded.value = false
  }

  return { menus, loaded, fetchMenus, loadFromLocal, clear }
})
