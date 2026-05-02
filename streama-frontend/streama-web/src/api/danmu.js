import { interactRequest } from '@/utils/request'

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

export function loadDanmu(params = {}) {
  return interactRequest({
    url: '/danmu/loadDanmu',
    method: 'get',
    params,
  })
}

export function postDanmu(payload = {}) {
  return interactRequest({
    url: '/danmu/postDanmu',
    method: 'post',
    requireAuth: true,
    data: toFormPayload(payload),
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8',
    },
  })
}
