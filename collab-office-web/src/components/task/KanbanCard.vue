<!-- Kinetic Sync §6: Task Cards (Kanban) -->
<template>
  <div class="kanban-card" @click="$emit('click')">
    <div class="card-title t-body-md">{{ title }}</div>
    <div v-if="description" class="card-desc t-label-md">{{ description }}</div>
    <div v-if="tags && tags.length" class="card-tags">
      <el-tag
        v-for="tag in tags"
        :key="tag"
        size="small"
        :style="{ background: tagColor(tag), borderColor: tagColor(tag) }"
        class="card-tag"
      >
        {{ tag }}
      </el-tag>
    </div>
    <div class="card-footer">
      <div v-if="assignee" class="card-assignee">
        <AwarenessIndicator
          :name="assignee.name"
          :avatar="assignee.avatar"
          :status="assignee.status"
          :size="20"
        />
        <span class="t-label-md">{{ assignee.name }}</span>
      </div>
      <div v-if="dueDate" class="card-date t-label-md" :class="{ overdue: isOverdue }">
        <el-icon :size="12"><Calendar /></el-icon>
        <span>{{ formatDate(dueDate) }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Calendar } from '@element-plus/icons-vue'

const props = defineProps<{
  title: string
  description?: string
  tags?: string[]
  assignee?: { name: string; avatar?: string; status?: 'online' | 'busy' | 'offline' } | null
  dueDate?: string
  bgColor?: string
}>()

defineEmits(['click'])

const isOverdue = computed(() => {
  if (!props.dueDate) return false
  return new Date(props.dueDate) < new Date()
})

function tagColor(tag: string): string {
  const colors: Record<string, string> = {
    bug: '#FF3B30', feature: '#3370FF', improvement: '#00B578',
    urgent: '#FF8F1F', design: '#8B5CF6', docs: '#6B7280',
  }
  return colors[tag.toLowerCase()] || 'var(--c-text-muted)'
}

function formatDate(date: string): string {
  return new Date(date).toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
}
</script>

<style lang="scss" scoped>
/* Kinetic Sync: white surface, 1px #E5E6EB border, hover → #F9FAFB */
.kanban-card {
  background: var(--c-surface);
  border: 1px solid var(--c-border);
  border-radius: var(--radius-lg);
  padding: 12px;
  cursor: pointer;
  transition: background .15s var(--ease-out), box-shadow .15s var(--ease-out);

  &:hover {
    background: var(--c-surface-hover);
    box-shadow: var(--shadow-sm);
  }

  &:active {
    transform: scale(.99);
  }
}

.card-title {
  font-weight: 600;
  color: var(--c-text);
  margin-bottom: 4px;
}

.card-desc {
  color: var(--c-text-muted);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  margin-bottom: 8px;
}

.card-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-bottom: 8px;
}

.card-tag {
  border-radius: var(--radius) !important;
  padding: 0 6px !important;
  height: 20px !important;
  line-height: 20px !important;
  font-size: 11px !important;
  color: #fff !important;
  border: none !important;
}

.card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.card-assignee {
  display: flex;
  align-items: center;
  gap: 4px;
  color: var(--c-text-secondary);
}

.card-date {
  display: flex;
  align-items: center;
  gap: 4px;
  color: var(--c-text-muted);

  &.overdue { color: var(--c-danger); }
}
</style>
