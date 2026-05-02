import { request } from '@/utils/request'

function toFormPayload(payload = {}) {
  const formData = new URLSearchParams()

  Object.entries(payload).forEach(([key, value]) => {
    if (value === null || value === undefined || value === '') {
      return
    }
    formData.append(key, String(value))
  })

  return formData
}

export function getCheckCode() {
  return request({
    url: '/account/checkCode',
    method: 'get',
    skipAuth: true,
  })
}

export function adminLogin(payload) {
  return request({
    url: '/account/login',
    method: 'post',
    skipAuth: true,
    data: toFormPayload(payload),
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8',
    },
  })
}

export function adminLogout() {
  return request({
    url: '/account/logout',
    method: 'post',
  })
}
