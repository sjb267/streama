import { fileRequest } from '@/utils/request'

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
