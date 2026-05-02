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

export function loadVideoList(params = {}) {
  return webRequest({
    url: '/video/loadVideoList',
    method: 'get',
    params,
  })
}

export function getVideoInfo(params = {}) {
  return webRequest({
    url: '/video/getVideoInfo',
    method: 'get',
    params,
  })
}

export function loadVideoPList(params = {}) {
  return webRequest({
    url: '/video/loadVideoPList',
    method: 'get',
    params,
  })
}

export function searchVideo(params = {}) {
  return webRequest({
    url: '/video/search',
    method: 'get',
    params,
  })
}

export function getVideoRecommend(params = {}) {
  return webRequest({
    url: '/video/getVideoRecommend',
    method: 'get',
    params,
  })
}

export function getSearchKeywordTop(params = {}) {
  return webRequest({
    url: '/video/getSearchKeywordTop',
    method: 'get',
    params,
  })
}

export function doUserAction(payload = {}) {
  return interactRequest({
    url: '/userAction/doAction',
    method: 'post',
    requireAuth: true,
    data: toFormPayload(payload),
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8',
    },
  })
}
