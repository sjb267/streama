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

export function loadVideoList(params = {}) {
  return request({
    url: '/videoInfo/loadVideoList',
    method: 'get',
    params,
    requireAuth: true,
  })
}

export function getAiAuditSummary(videoId) {
  return request({
    url: '/videoInfo/getAiAuditSummary',
    method: 'get',
    params: { videoId },
    requireAuth: true,
  })
}

export function getAiAuditItems(videoId) {
  return request({
    url: '/videoInfo/getAiAuditItems',
    method: 'get',
    params: { videoId },
    requireAuth: true,
  })
}

export function getVideoPostDetail(videoId) {
  return request({
    url: '/videoInfo/getVideoPostDetail',
    method: 'get',
    params: { videoId },
    requireAuth: true,
  })
}

export function auditVideo(payload) {
  return request({
    url: '/videoInfo/auditVideo',
    method: 'post',
    requireAuth: true,
    data: toFormPayload(payload),
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8',
    },
  })
}
