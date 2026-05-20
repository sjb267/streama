<script setup>
import { computed } from 'vue'
import IconFont from '@/components/IconFont.vue'
import { useAuthStore } from '@/stores/auth'

const props = defineProps({
  block: {
    type: Boolean,
    default: false,
  },
})

const authStore = useAuthStore()

const coinCount = computed(() => authStore.currentCoinCount)
const showBadge = computed(() => authStore.isLoggedIn)
const ariaLabel = computed(() => `当前硬币余额 ${coinCount.value}`)
</script>

<template>
  <span
    v-if="showBadge"
    class="user-coin-badge"
    :class="{ 'is-block': props.block }"
    :aria-label="ariaLabel"
  >
    <IconFont name="icon-dashang" size="17px" />
    <span class="coin-label">硬币</span>
    <span class="coin-count">{{ coinCount }}</span>
  </span>
</template>

<style scoped>
.user-coin-badge {
  min-height: 40px;
  padding: 0 12px;
  border: 1px solid #f0d395;
  border-radius: 999px;
  background: linear-gradient(180deg, #fffaf0, #ffffff);
  color: #3d3421;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 700;
  line-height: 1;
  white-space: nowrap;
  box-shadow: 0 8px 18px rgba(210, 145, 34, 0.12);
}

.user-coin-badge :deep(.iconfont-svg) {
  color: #e99a16;
  flex: 0 0 auto;
}

.coin-label {
  color: #80642c;
}

.coin-count {
  min-width: 1ch;
  font-variant-numeric: tabular-nums;
}

.user-coin-badge.is-block {
  width: 100%;
}

@media (max-width: 640px) {
  .user-coin-badge {
    min-height: 38px;
    padding: 0 10px;
  }
}
</style>
