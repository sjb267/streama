<script setup>
import { computed } from 'vue'

const props = defineProps({
  name: {
    type: String,
    required: true,
  },
  size: {
    type: [String, Number],
    default: '1em',
  },
  color: {
    type: String,
    default: '',
  },
})

const iconStyle = computed(() => {
  const normalizedSize =
    typeof props.size === 'number' ? `${props.size}px` : props.size

  return {
    fontSize: normalizedSize,
    color: props.color || undefined,
  }
})
</script>

<template>
  <svg class="iconfont-svg" aria-hidden="true" :style="iconStyle">
    <use :xlink:href="`#${name}`"></use>
  </svg>
</template>

<style scoped>
.iconfont-svg {
  width: 1em;
  height: 1em;
  fill: currentColor;
  vertical-align: middle;
  flex-shrink: 0;
}
</style>

