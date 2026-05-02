import { webRequest } from '@/utils/request'

export function getSystemSetting() {
  return webRequest({
    url: '/sysSetting/getSetting',
    method: 'get',
  })
}

