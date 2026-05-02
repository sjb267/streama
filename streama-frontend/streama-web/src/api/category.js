import { webRequest } from '@/utils/request'

export function loadAllCategory() {
  return webRequest({
    url: '/category/loadAllCategory',
    method: 'get',
  })
}


