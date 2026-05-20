import { webRequest } from '@/utils/request'

function toFormPayload(payload = {}) {
  const formData = new URLSearchParams()

  Object.entries(payload).forEach(([key, value]) => {
    if (value === null || value === undefined) {
      return
    }
    formData.append(key, String(value))
  })

  return formData
}

export function getCheckCode() {
  return webRequest({
    url: '/account/checkCode',
    method: 'get',
  })
}

export function login(payload) {
  return webRequest({
    url: '/account/login',
    method: 'post',
    data: toFormPayload(payload),
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8',
    },
  })
}

export function registerAccount(payload) {
  return webRequest({
    url: '/account/register',
    method: 'post',
    data: toFormPayload(payload),
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8',
    },
  })
}

export function autoLogin() {
  return webRequest({
    url: '/account/autoLogin',
    method: 'get',
  })
}

export function getUserCountInfo() {
  return webRequest({
    url: '/account/getUserCountInfo',
    method: 'get',
    requireAuth: true,
  })
}

export function logout() {
  return webRequest({
    url: '/account/logout',
    method: 'post',
  })
}

