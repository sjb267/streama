import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

export const AUTH_REQUIRED_CODE = 'AUTH_REQUIRED'
const WEB_TOKEN_HEADER = 'token'
const WEB_TOKEN_COOKIE_KEY = 'token='
const AUTH_HTTP_STATUS = new Set([401, 403])
const AUTH_BIZ_CODE = new Set([401, 403, 901])
const AUTH_MESSAGE_PATTERNS = [
  'token',
  'unauthorized',
  'forbidden',
  'session',
  'login',
  'signin',
  'not login',
  'not logged in',
  'invalid token',
  'expired token',
  '未登录',
  '请先登录',
  '登录失效',
  '登录过期',
  '重新登录',
]

function createService(baseURL) {
  const service = axios.create({
    baseURL,
    timeout: 15000,
    withCredentials: true,
  })

  service.interceptors.request.use((config) => {
    const authStore = useAuthStore()
    const token = resolveRequestToken(authStore)

    attachTokenHeader(config, token)

    if (config.requireAuth && !token) {
      handleAuthRequired({
        clearUserInfo: true,
      })
      const error = new Error('Auth required')
      error.code = AUTH_REQUIRED_CODE
      return Promise.reject(error)
    }

    return config
  })

  service.interceptors.response.use(
    (response) => {
      const payload = response?.data ?? {}

      if (isSuccessResponse(payload)) {
        return payload.data
      }

      if (response.config?.requireAuth && containsAuthError(payload, response?.status)) {
        handleAuthRequired({
          clearUserInfo: true,
        })
      }

      const error = new Error(payload?.info || 'Request failed')
      error.code = payload?.code ?? -1
      error.payload = payload
      ElMessage.error(error.message)
      return Promise.reject(error)
    },
    (error) => {
      if (error?.code === AUTH_REQUIRED_CODE) {
        return Promise.reject(error)
      }

      const payload = error?.response?.data
      if (error?.config?.requireAuth && containsAuthError(payload, error?.response?.status)) {
        handleAuthRequired({
          clearUserInfo: true,
        })
      }

      const message = payload?.info || error?.message || 'Network error, please retry'
      ElMessage.error(message)
      return Promise.reject(error)
    },
  )

  return service
}

const webService = createService('/web')
const interactService = createService('/interact')
const fileService = createService('/file')

function isSuccessResponse(payload) {
  return payload?.status === 'success' && payload?.code === 200
}

function containsAuthError(payload, httpStatus) {
  const status = Number(httpStatus)
  if (AUTH_HTTP_STATUS.has(status)) {
    return true
  }

  const code = Number(payload?.code)
  if (AUTH_BIZ_CODE.has(code)) {
    return true
  }

  // Some backend endpoints reuse `code=500` for auth failures.
  if (code !== 500) {
    return false
  }

  const message = String(payload?.info || payload?.message || '').toLowerCase().trim()
  if (!message) {
    return false
  }

  return AUTH_MESSAGE_PATTERNS.some((pattern) => message.includes(pattern))
}

function readTokenFromCookie() {
  if (typeof document === 'undefined') {
    return ''
  }

  const tokenCookie = document.cookie
    .split('; ')
    .find((item) => item.startsWith(WEB_TOKEN_COOKIE_KEY))

  if (!tokenCookie) {
    return ''
  }

  const rawToken = tokenCookie.slice(WEB_TOKEN_COOKIE_KEY.length)
  if (!rawToken) {
    return ''
  }

  try {
    return decodeURIComponent(rawToken).trim()
  } catch (_error) {
    return rawToken.trim()
  }
}

function resolveRequestToken(authStore) {
  const storeToken = String(authStore?.userInfo?.token || '').trim()
  if (storeToken) {
    return storeToken
  }
  return readTokenFromCookie()
}

function attachTokenHeader(config, token) {
  if (!token) {
    return
  }

  if (!config.headers) {
    config.headers = {
      [WEB_TOKEN_HEADER]: token,
    }
    return
  }

  if (typeof config.headers.set === 'function') {
    if (!config.headers.get(WEB_TOKEN_HEADER)) {
      config.headers.set(WEB_TOKEN_HEADER, token)
    }
    return
  }

  if (!config.headers[WEB_TOKEN_HEADER]) {
    config.headers[WEB_TOKEN_HEADER] = token
  }
}

function handleAuthRequired({ clearUserInfo = false } = {}) {
  const authStore = useAuthStore()
  if (clearUserInfo) {
    authStore.clearUserInfo()
  }
  authStore.openAuthDialog('login')
}

export function webRequest(config) {
  return webService(config)
}

export function interactRequest(config) {
  return interactService(config)
}

export function fileRequest(config) {
  return fileService(config)
}
