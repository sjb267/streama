<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import IconFont from '@/components/IconFont.vue'
import { getCheckCode, login, registerAccount } from '@/api/account'
import { useAuthStore } from '@/stores/auth'

const REGISTER_PASSWORD_PATTERN = /^(?=.*\d)(?=.*[a-zA-Z])[\da-zA-Z~!@#$%^&*_]{8,18}$/

const authStore = useAuthStore()

const dialogVisible = computed({
  get: () => authStore.authDialogVisible,
  set: (value) => {
    if (value) {
      authStore.openAuthDialog(authStore.authTab)
      return
    }
    authStore.closeAuthDialog()
  },
})

const activeTab = computed({
  get: () => authStore.authTab,
  set: (value) => {
    authStore.setAuthTab(value)
  },
})

const loginFormRef = ref(null)
const registerFormRef = ref(null)

const captchaLoading = ref(false)
const loginLoading = ref(false)
const registerLoading = ref(false)

const checkCodeData = reactive({
  checkCode: '',
  checkCodeKey: '',
})

const loginForm = reactive({
  email: '',
  password: '',
  checkCode: '',
})

const registerForm = reactive({
  email: '',
  nickname: '',
  registerPassword: '',
  checkCode: '',
})

const loginRules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: ['blur', 'change'] },
    { type: 'email', message: '邮箱格式不正确', trigger: ['blur', 'change'] },
  ],
  password: [{ required: true, message: '请输入密码', trigger: ['blur', 'change'] }],
  checkCode: [{ required: true, message: '请输入验证码', trigger: ['blur', 'change'] }],
}

const registerRules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: ['blur', 'change'] },
    { type: 'email', message: '邮箱格式不正确', trigger: ['blur', 'change'] },
  ],
  nickname: [
    { required: true, message: '请输入昵称', trigger: ['blur', 'change'] },
    { min: 1, max: 20, message: '昵称长度应为 1-20 个字符', trigger: ['blur', 'change'] },
  ],
  registerPassword: [
    { required: true, message: '请输入注册密码', trigger: ['blur', 'change'] },
    {
      pattern: REGISTER_PASSWORD_PATTERN,
      message: '密码需为 8-18 位，且同时包含字母和数字',
      trigger: ['blur', 'change'],
    },
  ],
  checkCode: [{ required: true, message: '请输入验证码', trigger: ['blur', 'change'] }],
}

function resetCheckCodeInput() {
  loginForm.checkCode = ''
  registerForm.checkCode = ''
}

function clearFormValidateState() {
  loginFormRef.value?.clearValidate()
  registerFormRef.value?.clearValidate()
}

async function refreshCheckCode() {
  captchaLoading.value = true
  try {
    const data = await getCheckCode()
    checkCodeData.checkCode = data?.checkCode || ''
    checkCodeData.checkCodeKey = data?.checkCodeKey || ''
  } finally {
    captchaLoading.value = false
  }
}

async function prepareDialog() {
  resetCheckCodeInput()
  clearFormValidateState()
  await refreshCheckCode()
}

async function handleSubmitLogin() {
  if (!loginFormRef.value) {
    return
  }

  await loginFormRef.value.validate()
  loginLoading.value = true

  try {
    const payload = {
      email: loginForm.email.trim(),
      password: loginForm.password,
      checkCodeKey: checkCodeData.checkCodeKey,
      checkCode: loginForm.checkCode.trim(),
    }
    const data = await login(payload)
    authStore.setUserInfo(data)
    await authStore.refreshUserCountInfo()
    authStore.closeAuthDialog()
    ElMessage.success('登录成功')
    loginForm.password = ''
  } catch (_error) {
    resetCheckCodeInput()
    await refreshCheckCode()
  } finally {
    loginLoading.value = false
  }
}

async function handleSubmitRegister() {
  if (!registerFormRef.value) {
    return
  }

  await registerFormRef.value.validate()
  registerLoading.value = true

  try {
    const payload = {
      email: registerForm.email.trim(),
      nickname: registerForm.nickname.trim(),
      registerPassword: registerForm.registerPassword,
      checkCodeKey: checkCodeData.checkCodeKey,
      checkCode: registerForm.checkCode.trim(),
    }

    await registerAccount(payload)
    ElMessage.success('注册成功，请登录')
    activeTab.value = 'login'
    loginForm.email = registerForm.email.trim()
    loginForm.password = ''
    registerForm.registerPassword = ''
    registerForm.checkCode = ''
    await refreshCheckCode()
  } catch (_error) {
    resetCheckCodeInput()
    await refreshCheckCode()
  } finally {
    registerLoading.value = false
  }
}

watch(
  () => dialogVisible.value,
  async (visible) => {
    if (!visible) {
      return
    }
    await prepareDialog()
  },
)
</script>

<template>
  <el-dialog
    v-model="dialogVisible"
    width="480px"
    :close-on-click-modal="false"
    align-center
    title="账号登录"
  >
    <el-tabs v-model="activeTab" stretch class="auth-tabs">
      <el-tab-pane label="登录" name="login">
        <el-form
          ref="loginFormRef"
          :model="loginForm"
          :rules="loginRules"
          label-position="top"
          class="auth-form"
        >
          <el-form-item label="邮箱" prop="email">
            <el-input v-model="loginForm.email" placeholder="请输入邮箱" autocomplete="email">
              <template #prefix>
                <IconFont name="icon-youxiang" />
              </template>
            </el-input>
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input
              v-model="loginForm.password"
              placeholder="请输入密码"
              type="password"
              show-password
              autocomplete="current-password"
            >
              <template #prefix>
                <IconFont name="icon-suoding" />
              </template>
            </el-input>
          </el-form-item>
          <el-form-item label="验证码" prop="checkCode">
            <div class="check-code-row">
              <el-input
                v-model="loginForm.checkCode"
                class="check-code-input"
                placeholder="请输入验证码"
                maxlength="6"
              />
              <button
                class="check-code-trigger"
                type="button"
                :disabled="captchaLoading"
                @click="refreshCheckCode"
              >
                <img v-if="checkCodeData.checkCode" :src="checkCodeData.checkCode" alt="验证码" />
                <span v-else>加载中...</span>
              </button>
            </div>
          </el-form-item>
          <el-button type="primary" class="submit-btn" :loading="loginLoading" @click="handleSubmitLogin">
            登录
          </el-button>
        </el-form>
      </el-tab-pane>

      <el-tab-pane label="注册" name="register">
        <el-form
          ref="registerFormRef"
          :model="registerForm"
          :rules="registerRules"
          label-position="top"
          class="auth-form"
        >
          <el-form-item label="邮箱" prop="email">
            <el-input v-model="registerForm.email" placeholder="请输入邮箱" autocomplete="email">
              <template #prefix>
                <IconFont name="icon-youxiang" />
              </template>
            </el-input>
          </el-form-item>
          <el-form-item label="昵称" prop="nickname">
            <el-input v-model="registerForm.nickname" placeholder="请输入昵称（最多20字）">
              <template #prefix>
                <IconFont name="icon-gerenzhanghu" />
              </template>
            </el-input>
          </el-form-item>
          <el-form-item label="注册密码" prop="registerPassword">
            <el-input
              v-model="registerForm.registerPassword"
              placeholder="8-18位，需包含字母和数字"
              type="password"
              show-password
              autocomplete="new-password"
            >
              <template #prefix>
                <IconFont name="icon-suoding" />
              </template>
            </el-input>
          </el-form-item>
          <el-form-item label="验证码" prop="checkCode">
            <div class="check-code-row">
              <el-input
                v-model="registerForm.checkCode"
                class="check-code-input"
                placeholder="请输入验证码"
                maxlength="6"
              />
              <button
                class="check-code-trigger"
                type="button"
                :disabled="captchaLoading"
                @click="refreshCheckCode"
              >
                <img v-if="checkCodeData.checkCode" :src="checkCodeData.checkCode" alt="验证码" />
                <span v-else>加载中...</span>
              </button>
            </div>
          </el-form-item>
          <el-button
            type="primary"
            class="submit-btn"
            :loading="registerLoading"
            @click="handleSubmitRegister"
          >
            注册
          </el-button>
        </el-form>
      </el-tab-pane>
    </el-tabs>
  </el-dialog>
</template>

<style scoped>
.auth-tabs {
  margin-top: -8px;
}

.auth-form {
  padding-top: 8px;
}

.check-code-row {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 12px;
}

.check-code-input {
  flex: 1;
}

.check-code-trigger {
  width: 130px;
  height: 40px;
  padding: 0;
  border: 1px solid #d9e0f2;
  border-radius: 10px;
  background: #f8faff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #5d6b8a;
  cursor: pointer;
  transition: border-color 0.2s ease, transform 0.2s ease;
}

.check-code-trigger:hover:not(:disabled) {
  border-color: #6f89ff;
  transform: translateY(-1px);
}

.check-code-trigger:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.check-code-trigger img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 10px;
}

.submit-btn {
  width: 100%;
  margin-top: 4px;
  height: 40px;
}
</style>
