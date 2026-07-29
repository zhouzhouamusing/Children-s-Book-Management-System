import { defineStore } from 'pinia'
import { ref } from 'vue'

export const usePermissionStore = defineStore('permission', () => {
  const permissions = ref([])
  const roles = ref([])

  function setPermissions(perms) {
    permissions.value = perms || []
    localStorage.setItem('permissions', JSON.stringify(perms || []))
  }

  function setRoles(r) {
    roles.value = r || []
    localStorage.setItem('roles', JSON.stringify(r || []))
  }

  function loadFromStorage() {
    try {
      permissions.value = JSON.parse(localStorage.getItem('permissions') || '[]')
      roles.value = JSON.parse(localStorage.getItem('roles') || '[]')
    } catch {
      permissions.value = []
      roles.value = []
    }
  }

  function hasPermission(code) {
    if (roles.value.includes('super:admin')) return true
    return permissions.value.includes(code)
  }

  function hasAnyPermission(codes) {
    if (roles.value.includes('super:admin')) return true
    return codes.some(c => permissions.value.includes(c))
  }

  function clear() {
    permissions.value = []
    roles.value = []
    localStorage.removeItem('permissions')
    localStorage.removeItem('roles')
  }

  return { permissions, roles, setPermissions, setRoles, loadFromStorage, hasPermission, hasAnyPermission, clear }
})
