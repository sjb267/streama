<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { getCheckCode } from '@/api/account'
import { useAdminAuthStore } from '@/stores/adminAuth'

const route = useRoute()
const router = useRouter()
const authStore = useAdminAuthStore()

const formRef = ref(null)
const captchaLoading = ref(false)
const submitting = ref(false)

const captchaData = reactive({
  checkCode: '',
  checkCodeKey: '',
})

const form = reactive({
  account: '',
  password: '',
  checkCode: '',
})

const rules = {
  account: [{ required: true, message: '请输入管理员账号', trigger: ['blur', 'change'] }],
  password: [{ required: true, message: '请输入登录密码', trigger: ['blur', 'change'] }],
  checkCode: [{ required: true, message: '请输入验证码', trigger: ['blur', 'change'] }],
}

async function refreshCheckCode() {
  captchaLoading.value = true
  try {
    const data = await getCheckCode()
    captchaData.checkCode = data?.checkCode || ''
    captchaData.checkCodeKey = data?.checkCodeKey || ''
  } finally {
    captchaLoading.value = false
  }
}

async function handleSubmit() {
  if (!formRef.value) {
    return
  }

  await formRef.value.validate()
  submitting.value = true

  try {
    await authStore.login({
      account: form.account.trim(),
      password: form.password,
      checkCode: form.checkCode.trim(),
      checkCodeKey: captchaData.checkCodeKey,
    })

    ElMessage.success('登录成功')
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/home'
    router.replace(redirect || '/home')
  } catch (_error) {
    form.checkCode = ''
    await refreshCheckCode()
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  refreshCheckCode()
})
</script>

<template>
  <div class="login-page">
    <section class="login-panel">
      <header class="login-header">
        <p class="brand">Streama 管理后台</p>
        <h1>欢迎登录</h1>
        <p>登录后可进入 Streama 管理后台，处理分类管理、稿件审核和日常运营工作。</p>
      </header>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        class="login-form"
      >
        <el-form-item label="管理员账号" prop="account">
          <el-input v-model="form.account" placeholder="请输入管理员账号" />
        </el-form-item>

        <el-form-item label="登录密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            show-password
            placeholder="请输入登录密码"
          />
        </el-form-item>

        <el-form-item label="验证码" prop="checkCode">
          <div class="check-code-row">
            <el-input v-model="form.checkCode" maxlength="6" placeholder="请输入验证码" />
            <button
              class="check-code-trigger"
              type="button"
              :disabled="captchaLoading"
              @click="refreshCheckCode"
            >
              <img v-if="captchaData.checkCode" :src="captchaData.checkCode" alt="验证码" />
              <span v-else>加载中...</span>
            </button>
          </div>
        </el-form-item>

        <el-button type="primary" class="submit-btn" :loading="submitting" @click="handleSubmit">
          登录
        </el-button>
      </el-form>
    </section>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background:
    radial-gradient(1000px 400px at 12% -10%, rgba(72, 107, 255, 0.2), transparent 72%),
    radial-gradient(900px 360px at 88% 8%, rgba(30, 190, 145, 0.15), transparent 68%),
    linear-gradient(180deg, #f5f8ff 0%, #edf3ff 100%);
}

.login-panel {
  width: min(460px, 100%);
  border: 1px solid #d7e1fb;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 14px 30px rgba(45, 67, 122, 0.1);
  padding: 24px;
}

.login-header .brand {
  margin: 0;
  color: #4f6eff;
  font-size: 13px;
  font-weight: 600;
}

.login-header h1 {
  margin: 8px 0 0;
  font-size: 28px;
  color: #1d2a45;
}

.login-header > p:last-child {
  margin: 8px 0 0;
  color: #6b7da2;
  font-size: 13px;
  line-height: 1.7;
}

.login-form {
  margin-top: 14px;
}

.check-code-row {
  display: flex;
  gap: 10px;
  width: 100%;
}

.check-code-trigger {
  width: 130px;
  border: 1px solid #d6dff7;
  border-radius: 10px;
  background: #f8faff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #60719a;
  cursor: pointer;
  overflow: hidden;
}

.check-code-trigger img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.submit-btn {
  width: 100%;
  margin-top: 8px;
  height: 40px;
}
</style>
