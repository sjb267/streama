import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { autoLogin, getUserCountInfo, logout } from '@/api/account'

export const useAuthStore = defineStore('auth', () => {
  const userInfo = ref(null)
  const authDialogVisible = ref(false)
  const authTab = ref('login')
  const autoLoginChecked = ref(false)
  const autoLoginLoading = ref(false)
  const userCountLoading = ref(false)
  let userCountRequestId = 0

  const isLoggedIn = computed(() => {
    return Boolean(userInfo.value?.userId || userInfo.value?.token)
  })

  const currentCoinCount = computed(() => normalizeCoinCount(userInfo.value?.currentCoinCount))

  function setUserInfo(value) {
    userInfo.value = normalizeUserInfo(value)
  }

  function clearUserInfo() {
    userCountRequestId += 1
    userCountLoading.value = false
    userInfo.value = null
  }

  function normalizeCoinCount(value) {
    const numericValue = Number(value)
    if (!Number.isFinite(numericValue) || numericValue < 0) {
      return 0
    }
    return Math.floor(numericValue)
  }

  function normalizeUserInfo(value) {
    if (!value) {
      return null
    }

    return {
      ...value,
      currentCoinCount: normalizeCoinCount(value.currentCoinCount),
    }
  }

  function patchUserInfo(value = {}) {
    if (!userInfo.value) {
      return
    }
    setUserInfo({
      ...userInfo.value,
      ...value,
    })
  }

  async function refreshUserCountInfo({ force = false } = {}) {
    if (!isLoggedIn.value || (userCountLoading.value && !force)) {
      return null
    }

    const requestId = ++userCountRequestId
    userCountLoading.value = true
    try {
      const data = await getUserCountInfo()
      if (!isLoggedIn.value || requestId !== userCountRequestId) {
        return data
      }
      const nextInfo = {}
      if (data && Object.prototype.hasOwnProperty.call(data, 'currentCoinCount')) {
        nextInfo.currentCoinCount = data.currentCoinCount
      }
      if (data && Object.prototype.hasOwnProperty.call(data, 'fansCount')) {
        nextInfo.fansCount = data.fansCount
      }
      if (data && Object.prototype.hasOwnProperty.call(data, 'focusCount')) {
        nextInfo.focusCount = data.focusCount
      }
      if (Object.keys(nextInfo).length > 0) {
        patchUserInfo(nextInfo)
      }
      return data
    } catch (_error) {
      return null
    } finally {
      if (requestId === userCountRequestId) {
        userCountLoading.value = false
      }
    }
  }

  function setAuthTab(tab) {
    authTab.value = tab === 'register' ? 'register' : 'login'
  }

  function openAuthDialog(tab = 'login') {
    setAuthTab(tab)
    authDialogVisible.value = true
  }

  function closeAuthDialog() {
    authDialogVisible.value = false
  }

  async function initAutoLogin() {
    if (autoLoginLoading.value) {
      return
    }
    if (autoLoginChecked.value) {
      await refreshUserCountInfo()
      return
    }

    autoLoginLoading.value = true
    try {
      const data = await autoLogin()
      setUserInfo(data)
      await refreshUserCountInfo()
    } catch (_error) {
      clearUserInfo()
    } finally {
      autoLoginLoading.value = false
      autoLoginChecked.value = true
    }
  }

  async function signOut() {
    try {
      await logout()
    } catch (_error) {
      // Keep UI logout responsive even if network request fails.
    } finally {
      clearUserInfo()
    }
  }

  return {
    userInfo,
    isLoggedIn,
    currentCoinCount,
    authDialogVisible,
    authTab,
    autoLoginChecked,
    autoLoginLoading,
    userCountLoading,
    setUserInfo,
    clearUserInfo,
    refreshUserCountInfo,
    setAuthTab,
    openAuthDialog,
    closeAuthDialog,
    initAutoLogin,
    signOut,
  }
})
