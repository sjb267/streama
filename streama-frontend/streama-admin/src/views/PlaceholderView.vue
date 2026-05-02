<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()

const title = computed(() => route.meta?.title || '功能开发中')
</script>

<template>
  <div class="placeholder-view">
    <h2>{{ title }}</h2>
    <p>当前页面功能正在整理中，后续会在这里补充完整的后台能力与操作入口。</p>
    <el-empty description="敬请期待" />
  </div>
</template>

<style scoped>
.placeholder-view {
  border: 1px dashed #c9d7fb;
  border-radius: 14px;
  background: #f9fbff;
  padding: 20px;
}

h2 {
  margin: 0;
  color: #1f2f4e;
}

p {
  margin: 10px 0 16px;
  color: #6d7ea2;
  font-size: 14px;
  line-height: 1.7;
}
</style>
