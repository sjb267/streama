import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { adminLogin, adminLogout } from '@/api/account'
import {
  clearAdminSession,
  getAdminLoginFlag,
  getAdminName,
  getAdminToken,
  persistAdminSession,
  syncAdminTokenFromCookie,
} from '@/utils/authToken'

export const useAdminAuthStore = defineStore('admin-auth', () => {
  const adminName = ref(getAdminName())
  const loginFlag = ref(getAdminLoginFlag())
  const checked = ref(false)

  const isLoggedIn = computed(() => {
    return loginFlag.value || Boolean(getAdminToken())
  })

  function restore() {
    syncAdminTokenFromCookie()
    adminName.value = getAdminName()
    loginFlag.value = getAdminLoginFlag() || Boolean(getAdminToken())
    checked.value = true
  }

  async function login(payload) {
    const data = await adminLogin(payload)
    const resolvedName = typeof data === 'string' && data ? data : '管理员'
    persistAdminSession(resolvedName)
    restore()
    return data
  }

  async function logout() {
    try {
      await adminLogout()
    } catch (_error) {
      // Keep UI responsive even if network cleanup fails.
    } finally {
      clearAdminSession()
      restore()
    }
  }

  function clearLocalSession() {
    clearAdminSession()
    restore()
  }

  return {
    adminName,
    isLoggedIn,
    checked,
    restore,
    login,
    logout,
    clearLocalSession,
  }
})
