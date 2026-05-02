import axios from 'axios'
import { ElMessage } from 'element-plus'
import { clearAdminSession, getAdminToken, syncAdminTokenFromCookie } from '@/utils/authToken'

const AUTH_EXPIRED_CODE = 901
let authRedirecting = false

function isSuccessResponse(payload) {
  return payload?.status === 'success' && payload?.code === 200
}

function handleAuthExpired(message = '登录已失效，请重新登录') {
  clearAdminSession()
  if (!authRedirecting) {
    authRedirecting = true
    if (typeof window !== 'undefined') {
      const currentPath = window.location.pathname + window.location.search
      const target = `/login?redirect=${encodeURIComponent(currentPath || '/home')}`
      if (currentPath !== target) {
        window.location.replace(target)
      }
    }
    setTimeout(() => {
      authRedirecting = false
    }, 300)
  }
  ElMessage.error(message)
}

function createService(baseURL) {
  const service = axios.create({
    baseURL,
    timeout: 15000,
    withCredentials: true,
  })

  service.interceptors.request.use((config) => {
    syncAdminTokenFromCookie()
    const token = getAdminToken()
    if (token && !config.skipAuth) {
      config.headers = config.headers || {}
      config.headers.adminToken = token
    }
    return config
  })

  service.interceptors.response.use(
    (response) => {
      const payload = response?.data ?? {}
      if (isSuccessResponse(payload)) {
        return payload.data
      }

      if (payload?.code === AUTH_EXPIRED_CODE) {
        handleAuthExpired(payload?.info)
        const error = new Error(payload?.info || '登录失败')
        error.code = payload?.code
        error.payload = payload
        return Promise.reject(error)
      }

      const error = new Error(payload?.info || '请求失败')
      error.code = payload?.code ?? -1
      error.payload = payload
      ElMessage.error(error.message)
      return Promise.reject(error)
    },
    (error) => {
      const payload = error?.response?.data
      if (payload?.code === AUTH_EXPIRED_CODE) {
        handleAuthExpired(payload?.info)
        return Promise.reject(error)
      }
      const message = payload?.info || error?.message || '网络异常，请稍后重试'
      ElMessage.error(message)
      return Promise.reject(error)
    },
  )

  return service
}

const adminService = createService('/admin')
const fileService = createService('/admin/file')

export function request(config) {
  return adminService(config)
}

export function fileRequest(config) {
  return fileService(config)
}
