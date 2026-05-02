import { fileRequest } from '@/utils/request'

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

export function preUploadVideo(payload) {
  return fileRequest({
    url: '/preUploadVideo',
    method: 'post',
    requireAuth: true,
    data: toFormPayload(payload),
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8',
    },
  })
}

export function uploadVideo(payload) {
  const formData = new FormData()
  formData.append('chunkFile', payload.chunkFile)
  formData.append('chunkIndex', String(payload.chunkIndex))
  formData.append('uploadId', payload.uploadId)

  return fileRequest({
    url: '/uploadVideo',
    method: 'post',
    requireAuth: true,
    data: formData,
    onUploadProgress: payload.onUploadProgress,
  })
}

export function deleteUploadVideo(uploadId) {
  return fileRequest({
    url: '/deleteUploadVideo',
    method: 'post',
    requireAuth: true,
    data: toFormPayload({ uploadId }),
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8',
    },
  })
}

export function uploadImage(file, createThumbnail = false) {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('createThumbnail', String(Boolean(createThumbnail)))

  return fileRequest({
    url: '/uploadImage',
    method: 'post',
    requireAuth: true,
    data: formData,
  })
}
