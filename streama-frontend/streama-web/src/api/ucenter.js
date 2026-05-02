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

export function postVideo(payload) {
  return webRequest({
    url: '/ucenter/postVideo',
    method: 'post',
    requireAuth: true,
    data: toFormPayload(payload),
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8',
    },
  })
}

export function loadVideoPostList(params = {}) {
  return webRequest({
    url: '/ucenter/loadVideoPostList',
    method: 'get',
    requireAuth: true,
    params,
  })
}

export function getVideoCountInfo() {
  return webRequest({
    url: '/ucenter/getVideoCountInfo',
    method: 'get',
    requireAuth: true,
  })
}

export function getVideoByVideoId(params = {}) {
  return webRequest({
    url: '/ucenter/getVideoByVideoId',
    method: 'get',
    requireAuth: true,
    params,
  })
}

export function saveVideoInteraction(payload = {}) {
  const formData = new URLSearchParams()
  const videoId = payload?.videoId
  if (videoId !== null && videoId !== undefined && videoId !== '') {
    formData.append('videoId', String(videoId))
  }
  if (Object.prototype.hasOwnProperty.call(payload, 'interaction')) {
    const interaction = payload?.interaction
    formData.append('interaction', interaction === null || interaction === undefined ? '' : String(interaction))
  }
  return webRequest({
    url: '/ucenter/saveVideoInteraction',
    method: 'post',
    requireAuth: true,
    data: formData,
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8',
    },
  })
}

export function loadAllVideo() {
  return webRequest({
    url: '/ucenter/loadAllVideo',
    method: 'get',
    requireAuth: true,
  })
}

export function loadComment(params = {}) {
  return interactRequest({
    url: '/ucenter/loadComment',
    method: 'get',
    requireAuth: true,
    params,
  })
}

export function delComment(payload = {}) {
  return interactRequest({
    url: '/ucenter/delComment',
    method: 'post',
    requireAuth: true,
    data: toFormPayload(payload),
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8',
    },
  })
}

export function loadDanmu(params = {}) {
  return interactRequest({
    url: '/ucenter/loadDanmu',
    method: 'get',
    requireAuth: true,
    params,
  })
}

export function delDanmu(payload = {}) {
  return interactRequest({
    url: '/ucenter/delDanmu',
    method: 'post',
    requireAuth: true,
    data: toFormPayload(payload),
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8',
    },
  })
}

export function deleteVideo(payload = {}) {
  return webRequest({
    url: '/ucenter/deleteVideo',
    method: 'post',
    requireAuth: true,
    data: toFormPayload(payload),
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8',
    },
  })
}
