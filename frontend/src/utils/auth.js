export function isTokenExpired(token) {
  if (!token) return true
  try {
    const payload = parseTokenPayload(token)
    if (!payload || !payload.exp) return false
    return payload.exp * 1000 < Date.now() - 60000
  } catch (e) {
    return true
  }
}

export function getRoleFromToken(token) {
  if (!token) return null
  try {
    const payload = parseTokenPayload(token)
    if (!payload) return null
    const role = payload.role || null
    if (role && role !== 'ADMIN' && role !== 'READER' && role !== 'SUPER_ADMIN') return null
    return role === 'SUPER_ADMIN' ? 'ADMIN' : role
  } catch (e) {
    return null
  }
}

export function getRolesFromToken(token) {
  if (!token) return []
  try {
    const payload = parseTokenPayload(token)
    if (!payload) return []
    return payload.roles || (payload.role ? [payload.role] : [])
  } catch (e) {
    return []
  }
}

export function getPermissionsFromToken(token) {
  if (!token) return []
  try {
    const payload = parseTokenPayload(token)
    if (!payload) return []
    return payload.permissions || []
  } catch (e) {
    return []
  }
}

function parseTokenPayload(token) {
  const parts = token.split('.')
  if (parts.length !== 3) return null
  const base64Url = parts[1]
  if (!base64Url) return null
  const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/')
  return JSON.parse(decodeURIComponent(atob(base64).split('').map(c =>
    '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2)
  ).join('')))
}

export function clearAuth() {
  localStorage.removeItem('token')
  localStorage.removeItem('nickname')
  localStorage.removeItem('role')
  localStorage.removeItem('roles')
  localStorage.removeItem('permissions')
  localStorage.removeItem('readerId')
  localStorage.removeItem('suspended')
  resetValidation()
}

export let lastValidated = 0
export const VALIDATION_INTERVAL = 2 * 60 * 1000

export function markValidated() {
  lastValidated = Date.now()
}

export function resetValidation() {
  lastValidated = 0
}
