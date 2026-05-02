<script setup>
import { computed } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { useAdminAuthStore } from '@/stores/adminAuth'

const route = useRoute()
const router = useRouter()
const authStore = useAdminAuthStore()

const activePath = computed(() => route.meta?.activeMenu || route.path || '/home')
const pageTitle = computed(() => route.meta?.title || 'Streama 管理后台')

async function handleLogout() {
  await authStore.logout()
  ElMessage.success('已退出登录')
  router.replace('/login')
}
</script>

<template>
  <div class="admin-shell">
    <aside class="sidebar">
      <div class="brand">
        <p class="brand-title">Streama 管理后台</p>
        <p class="brand-subtitle">内容审核工作台</p>
      </div>

      <el-menu :default-active="activePath" class="menu" router>
        <el-menu-item index="/home">控制台</el-menu-item>
        <el-sub-menu index="/content">
          <template #title>内容管理</template>
          <el-menu-item index="/content/category">分类管理</el-menu-item>
          <el-menu-item index="/content/videos">稿件管理</el-menu-item>
        </el-sub-menu>
      </el-menu>
    </aside>

    <main class="content-wrap">
      <header class="topbar">
        <div class="page-meta">
          <h1>{{ pageTitle }}</h1>
          <p>Streama 管理工作台</p>
        </div>
        <div class="admin-actions">
          <span>{{ authStore.adminName }}</span>
          <el-button type="danger" plain @click="handleLogout">退出登录</el-button>
        </div>
      </header>

      <section class="content-panel">
        <router-view />
      </section>
    </main>
  </div>
</template>

<style scoped>
.admin-shell {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 240px 1fr;
  background:
    radial-gradient(1000px 380px at -8% -15%, rgba(94, 114, 255, 0.2), transparent 72%),
    radial-gradient(860px 360px at 108% 8%, rgba(35, 198, 146, 0.16), transparent 70%),
    linear-gradient(180deg, #f4f8ff 0%, #edf3ff 100%);
}

.sidebar {
  border-right: 1px solid #dce5fb;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(5px);
  padding: 16px 12px;
}

.brand {
  border: 1px solid #d8e2fa;
  border-radius: 14px;
  background: linear-gradient(145deg, #ffffff, #f3f7ff);
  padding: 12px 12px 10px;
}

.brand-title {
  margin: 0;
  font-size: 17px;
  font-weight: 700;
  color: #1e2a46;
}

.brand-subtitle {
  margin: 5px 0 0;
  font-size: 12px;
  color: #7485a7;
}

.menu {
  margin-top: 12px;
  border: none;
  background: transparent;
}

.menu :deep(.el-menu-item),
.menu :deep(.el-sub-menu__title) {
  border-radius: 10px;
  margin-bottom: 6px;
  color: #34486d;
}

.menu :deep(.el-menu-item.is-active) {
  background: linear-gradient(135deg, #5c77ff, #3d8fff);
  color: #ffffff;
}

.content-wrap {
  padding: 20px;
}

.topbar {
  border: 1px solid #d5e0fb;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 12px 28px rgba(55, 74, 131, 0.08);
  padding: 16px 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.page-meta h1 {
  margin: 0;
  font-size: 24px;
  color: #1f2a44;
}

.page-meta p {
  margin: 6px 0 0;
  font-size: 13px;
  color: #6f7f9f;
}

.admin-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #3a4e74;
  font-size: 13px;
}

.content-panel {
  margin-top: 14px;
  border: 1px solid #dbe4fa;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.94);
  min-height: calc(100vh - 144px);
  box-shadow: 0 12px 28px rgba(55, 74, 131, 0.06);
  padding: 20px;
}

@media (max-width: 980px) {
  .admin-shell {
    grid-template-columns: 1fr;
  }

  .sidebar {
    border-right: none;
    border-bottom: 1px solid #dce5fb;
  }

  .content-wrap {
    padding: 14px;
  }

  .topbar {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
