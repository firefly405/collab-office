<!-- Kinetic Sync §2: Awareness Indicators -->
<template>
  <div class="awareness" :title="tooltip">
    <el-avatar :size="size" :src="avatar" class="av-avatar">
      {{ initials }}
    </el-avatar>
    <span class="av-ring" :class="statusClass" />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(defineProps<{
  name?: string
  avatar?: string
  status?: 'online' | 'busy' | 'offline'
  size?: number
}>(), {
  status: 'offline',
  size: 32,
  name: '',
})

const initials = computed(() => {
  if (!props.name) return ''
  return props.name.slice(0, 1).toUpperCase()
})

const statusClass = computed(() => `av-${props.status}`)

const tooltip = computed(() => {
  const s = { online: '在线', busy: '忙碌', offline: '离线' }[props.status]
  return `${props.name || '未知用户'} · ${s}`
})
</script>

<style lang="scss" scoped>
.awareness {
  position: relative;
  display: inline-flex;
  flex-shrink: 0;
}

// 2px background-color cutout + 3px status ring
.av-avatar {
  outline: 2px solid var(--c-surface);
}

.av-ring {
  position: absolute;
  bottom: 0;
  right: 0;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  border: 2px solid var(--c-surface);
  box-sizing: content-box;
}

.av-online { background: var(--c-online); }
.av-busy   { background: var(--c-busy); }
.av-offline { background: var(--c-offline); }
</style>
