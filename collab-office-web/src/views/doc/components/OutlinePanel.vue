<!-- 文档大纲导航面板 -->
<template>
  <div class="outline-panel">
    <div class="outline-header">
      <span class="t-label-md">大纲</span>
      <el-button size="small" text @click="$emit('refresh')">
        <el-icon :size="14"><Refresh /></el-icon>
      </el-button>
    </div>
    <el-divider style="margin: 4px 0 10px" />
    <div v-if="items.length === 0" class="outline-empty t-body-md">
      暂无标题
    </div>
    <div
      v-for="(item, idx) in items"
      :key="idx"
      class="outline-item t-body-md"
      :class="{ active: activeIndex === item.index }"
      :style="{ paddingLeft: (item.level - 1) * 14 + 'px' }"
      @click="scrollTo(item)"
    >
      <span class="outline-h-indicator">H{{ item.level }}</span>
      <span class="outline-text">{{ item.title || '无标题' }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Refresh } from '@element-plus/icons-vue'

interface OutlineItem {
  level: number
  title: string
  index: number
  position: number
}

defineProps<{
  items: OutlineItem[]
  activeIndex?: number
}>()

const emit = defineEmits<{
  (e: 'scroll-to', item: OutlineItem): void
  (e: 'refresh'): void
}>()

function scrollTo(item: OutlineItem) {
  emit('scroll-to', item)
}
</script>

<style lang="scss" scoped>
.outline-panel {
  width: 220px;
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: 12px;
  border-right: 1px solid var(--c-border);
  background: var(--c-surface-dim);
  overflow-y: auto;
  flex-shrink: 0;
}

.outline-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 4px;
  color: var(--c-text-secondary);
  font-weight: 600;
}

.outline-empty {
  padding: 20px 4px;
  color: var(--c-text-ghost);
  text-align: center;
}

.outline-item {
  display: flex;
  align-items: center;
  gap: 4px;
  cursor: pointer;
  padding: 4px 0;
  border-radius: var(--radius);
  color: var(--c-text-secondary);
  transition: all .15s var(--ease-out);
  border-left: 2px solid transparent;

  &:hover {
    color: var(--c-brand);
    background: var(--c-brand-light);
  }

  &.active {
    color: var(--c-brand);
    font-weight: 600;
    border-left-color: var(--c-brand);
    background: var(--c-brand-light);
  }
}

.outline-h-indicator {
  font-size: 10px;
  font-weight: 600;
  color: var(--c-text-muted);
  flex-shrink: 0;
  width: 18px;
  text-align: center;
}

.outline-text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
