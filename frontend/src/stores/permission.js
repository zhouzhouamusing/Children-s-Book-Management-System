import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const usePermissionStore = defineStore('permission', () => {
  const roles = ref([])
  const permissions = ref([])
  const version = ref(0)

  const permissionSet = computed(() => new Set(permissions.value))

  function setAuth(data) {
    const newRoles = data.roles || []
    const newPerms = data.permissions || []
    const rolesChanged = JSON.stringify(newRoles) !== JSON.stringify(roles.value)
    const permsChanged = JSON.stringify(newPerms) !== JSON.stringify(permissions.value)

    if (rolesChanged || permsChanged) {
      roles.value = newRoles
      permissions.value = newPerms
      localStorage.setItem('roles', JSON.stringify(newRoles))
      localStorage.setItem('permissions', JSON.stringify(newPerms))
      version.value++
    }
  }

  function loadFromStorage() {
    try {
      const storedRoles = localStorage.getItem('roles')
      const storedPerms = localStorage.getItem('permissions')
      const newRoles = storedRoles ? JSON.parse(storedRoles) : []
      const newPerms = storedPerms ? JSON.parse(storedPerms) : []
      const changed = JSON.stringify(newRoles) !== JSON.stringify(roles.value) ||
        JSON.stringify(newPerms) !== JSON.stringify(permissions.value)
      roles.value = newRoles
      permissions.value = newPerms
      if (changed) version.value++
    } catch (e) {
      roles.value = []
      permissions.value = []
    }
  }

  function hasPermission(code) {
    return permissionSet.value.has(code)
  }

  function hasAnyPermission(codes) {
    const set = permissionSet.value
    return codes.some(c => set.has(c))
  }

  function hasAllPermissions(codes) {
    const set = permissionSet.value
    return codes.every(c => set.has(c))
  }

  function hasRole(role) {
    return roles.value.includes(role)
  }

  function hasAnyRole(roleList) {
    return roleList.some(r => roles.value.includes(r))
  }

  async function refreshFromServer() {
    try {
      const { default: request } = await import('@/utils/request')
      const res = await request.get('/auth/validate')
      if (res.data) {
        if (res.data.roles || res.data.permissions) {
          setAuth({
            roles: res.data.roles || roles.value,
            permissions: res.data.permissions || permissions.value
          })
        }
        if (res.data.suspended !== undefined) {
          localStorage.setItem('suspended', res.data.suspended ? 'true' : 'false')
        }
      }
    } catch (e) {
      if (e?.response?.status === 401) {
        clear()
      }
    }
  }

  function clear() {
    roles.value = []
    permissions.value = []
    localStorage.removeItem('roles')
    localStorage.removeItem('permissions')
    version.value++
  }

  // 跨标签页同步
  if (typeof window !== 'undefined') {
    window.addEventListener('storage', (e) => {
      if (e.key === 'permissions' || e.key === 'roles') {
        loadFromStorage()
      }
    })
  }

  return {
    roles, permissions, version, permissionSet,
    setAuth, loadFromStorage, hasPermission, hasAnyPermission, hasAllPermissions,
    hasRole, hasAnyRole, refreshFromServer, clear
  }
})
