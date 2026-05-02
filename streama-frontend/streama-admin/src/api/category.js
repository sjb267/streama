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

export function loadCategory(params = {}) {
  return request({
    url: '/category/loadCategory',
    method: 'get',
    params,
    requireAuth: true,
  })
}

export function saveCategory(payload) {
  return request({
    url: '/category/saveCategory',
    method: 'post',
    requireAuth: true,
    data: toFormPayload(payload),
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8',
    },
  })
}

export function delCategory(categoryId) {
  return request({
    url: '/category/delCategory',
    method: 'post',
    requireAuth: true,
    data: toFormPayload({ categoryId }),
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8',
    },
  })
}

export function changeSort(payload) {
  return request({
    url: '/category/changeSort',
    method: 'post',
    requireAuth: true,
    data: toFormPayload(payload),
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8',
    },
  })
}
