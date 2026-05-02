export const ADMIN_LOGIN_FLAG_KEY = 'streama_admin_login_flag'
export const ADMIN_TOKEN_KEY = 'streama_admin_token'
export const ADMIN_NAME_KEY = 'streama_admin_name'

export function readAdminTokenFromCookie() {
  if (typeof document === 'undefined') {
    return ''
  }

  const cookie = document.cookie
    .split('; ')
    .find((item) => item.startsWith('adminToken='))

  if (!cookie) {
    return ''
  }

  return decodeURIComponent(cookie.slice('adminToken='.length))
}

export function getAdminToken() {
  if (typeof window === 'undefined') {
    return ''
  }

  const localToken = localStorage.getItem(ADMIN_TOKEN_KEY) || ''
  return localToken || readAdminTokenFromCookie()
}

export function getAdminLoginFlag() {
  if (typeof window === 'undefined') {
    return false
  }
  return localStorage.getItem(ADMIN_LOGIN_FLAG_KEY) === '1'
}

export function getAdminName() {
  if (typeof window === 'undefined') {
    return 'admin'
  }
  return localStorage.getItem(ADMIN_NAME_KEY) || 'admin'
}

export function persistAdminSession(adminName = 'admin') {
  if (typeof window === 'undefined') {
    return
  }

  localStorage.setItem(ADMIN_LOGIN_FLAG_KEY, '1')
  localStorage.setItem(ADMIN_NAME_KEY, adminName || 'admin')

  const cookieToken = readAdminTokenFromCookie()
  if (cookieToken) {
    localStorage.setItem(ADMIN_TOKEN_KEY, cookieToken)
  }
}

export function syncAdminTokenFromCookie() {
  if (typeof window === 'undefined') {
    return
  }
  const cookieToken = readAdminTokenFromCookie()
  if (cookieToken) {
    localStorage.setItem(ADMIN_TOKEN_KEY, cookieToken)
  }
}

export function clearAdminSession() {
  if (typeof window === 'undefined') {
    return
  }
  localStorage.removeItem(ADMIN_LOGIN_FLAG_KEY)
  localStorage.removeItem(ADMIN_TOKEN_KEY)
  localStorage.removeItem(ADMIN_NAME_KEY)
}
