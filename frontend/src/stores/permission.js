import { defineStore } from 'pinia'
import { ref } from 'vue'

export const usePermissionStore = defineStore('permission', () => {
  const roles = ref([])
  const permissions = ref([])

  function setAuth(data) {
    roles.value = data.roles || []
    permissions.value = data.permissions || []
    localStorage.setItem('roles', JSON.stringify(roles.value))
    localStorage.setItem('permissions', JSON.stringify(permissions.value))
  }

  function loadFromStorage() {
    try {
      const storedRoles = localStorage.getItem('roles')
      const storedPerms = localStorage.getItem('permissions')
      if (storedRoles) roles.value = JSON.parse(storedRoles)
      if (storedPerms) permissions.value = JSON.parse(storedPerms)
    } catch (e) {
      roles.value = []
      permissions.value = []
    }
  }

  function hasPermission(code) {
    return permissions.value.includes(code)
  }

  function hasAnyPermission(codes) {
    return codes.some(c => permissions.value.includes(c))
  }

  function hasRole(role) {
    return roles.value.includes(role)
  }

  function hasAnyRole(roleList) {
    return roleList.some(r => roles.value.includes(r))
  }

  function clear() {
    roles.value = []
    permissions.value = []
    localStorage.removeItem('roles')
    localStorage.removeItem('permissions')
  }

  return { roles, permissions, setAuth, loadFromStorage, hasPermission, hasAnyPermission, hasRole, hasAnyRole, clear }
})
