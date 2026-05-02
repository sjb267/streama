import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { autoLogin, logout } from '@/api/account'

export const useAuthStore = defineStore('auth', () => {
  const userInfo = ref(null)
  const authDialogVisible = ref(false)
  const authTab = ref('login')
  const autoLoginChecked = ref(false)
  const autoLoginLoading = ref(false)

  const isLoggedIn = computed(() => {
    return Boolean(userInfo.value?.userId || userInfo.value?.token)
  })

  function setUserInfo(value) {
    userInfo.value = value || null
  }

  function clearUserInfo() {
    userInfo.value = null
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
    if (autoLoginChecked.value || autoLoginLoading.value) {
      return
    }

    autoLoginLoading.value = true
    try {
      const data = await autoLogin()
      setUserInfo(data)
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
    authDialogVisible,
    authTab,
    autoLoginChecked,
    autoLoginLoading,
    setUserInfo,
    clearUserInfo,
    setAuthTab,
    openAuthDialog,
    closeAuthDialog,
    initAutoLogin,
    signOut,
  }
})

