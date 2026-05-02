import { interactRequest, webRequest } from '@/utils/request'

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

export function getUserInfo(params = {}) {
  return webRequest({
    url: '/home/getUserInfo',
    method: 'get',
    params,
  })
}

export function updateUserInfo(payload = {}) {
  return webRequest({
    url: '/home/updateUserInfo',
    method: 'post',
    requireAuth: true,
    data: toFormPayload(payload),
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8',
    },
  })
}

export function focusUser(payload = {}) {
  return webRequest({
    url: '/home/focus',
    method: 'post',
    requireAuth: true,
    data: toFormPayload(payload),
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8',
    },
  })
}

export function cancelFocusUser(payload = {}) {
  return webRequest({
    url: '/home/cancelFocus',
    method: 'post',
    requireAuth: true,
    data: toFormPayload(payload),
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8',
    },
  })
}

export function loadFocusList(params = {}) {
  return webRequest({
    url: '/home/loadFocusList',
    method: 'get',
    requireAuth: true,
    params,
  })
}

export function loadFansList(params = {}) {
  return webRequest({
    url: '/home/loadFansList',
    method: 'get',
    requireAuth: true,
    params,
  })
}

export function loadHomeVideoList(params = {}) {
  return webRequest({
    url: '/home/loadVideoList',
    method: 'get',
    params,
  })
}

export function loadUserCollection(params = {}) {
  return interactRequest({
    url: '/home/loadUserCollection',
    method: 'get',
    params,
  })
}
