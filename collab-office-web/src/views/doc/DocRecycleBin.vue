<template>
  <div class="recycle-page">
    <div class="page-header">
      <div class="header-left">
        <span class="material-symbols-outlined header-icon">delete</span>
        <div>
          <h2 class="t-h2" style="color: var(--c-text)">回收站</h2>
          <p class="t-body-md" style="color: var(--c-text-muted)">
            回收站中的文件将在 30 天后自动删除 &middot; 剩余 {{ files.length }} 个文件
          </p>
        </div>
      </div>
      <button class="clear-all-btn" :disabled="files.length === 0" @click="clearAll">
        <span class="material-symbols-outlined">delete_sweep</span>
        清空回收站
      </button>
    </div>

    <!-- File list -->
    <div class="file-list" v-loading="loading">
      <div v-for="f in files" :key="f.id" class="file-row">
        <div class="file-icon" :style="{ background: typeBg(f.type) }">
          <span class="material-symbols-outlined" :style="{ color: typeColor(f.type), fontSize: '20px' }">
            {{ typeIcon(f.type) }}
          </span>
        </div>
        <div class="file-info">
          <div class="file-name">{{ f.title || f.name }}</div>
          <div class="file-meta t-label-md">
            <span>{{ f.fileSize || f.size || '' }}</span>
            <span class="meta-sep" v-if="f.fileSize || f.size">&middot;</span>
            <span>删除者：{{ f.deletedBy || f.creatorName || '--' }}</span>
          </div>
        </div>
        <div class="file-actions">
          <button class="action-btn restore-btn" @click="restore(f)">
            <span class="material-symbols-outlined">undo</span>
            恢复
          </button>
          <button class="action-btn delete-btn" @click="permanentDelete(f)">
            <span class="material-symbols-outlined">delete_forever</span>
            彻底删除
          </button>
        </div>
      </div>

      <div v-if="files.length === 0" class="empty-state">
        <span class="material-symbols-outlined empty-icon">delete_outline</span>
        <p class="t-body-md" style="color: var(--c-text-secondary); font-weight: 500">回收站为空</p>
        <p class="t-label-md" style="color: var(--c-text-muted)">删除的文件会出现在这里，30天内可恢复</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { recoverDoc, deleteDoc } from '@/api/doc'
import request from '@/utils/request'

const files = ref<any[]>([])
const loading = ref(false)

onMounted(() => fetchDeleted())

function fetchDeleted() {
  loading.value = true
  request.get('/document/list', { params: { status: 0 } })
    .then((res: any) => {
      if (res.code === 200) files.value = res.data?.records || res.data || []
    })
    .catch(() => {})
    .finally(() => loading.value = false)
}

function typeIcon(type: string) {
  return { document: 'description', spreadsheet: 'table', mindmap: 'account_tree' }[type] || 'description'
}

function typeColor(type: string) {
  return { document: 'var(--c-brand)', spreadsheet: 'var(--c-success)', mindmap: '#8B5CF6' }[type] || 'var(--c-text-muted)'
}

function typeBg(type: string) {
  return { document: 'var(--c-brand-light)', spreadsheet: 'var(--c-success-light)', mindmap: 'rgba(139,92,246,.1)' }[type] || 'var(--c-bg)'
}

async function restore(f: any) {
  recoverDoc(f.docId || f.id).then(() => {
    files.value = files.value.filter(x => x.id !== f.id)
    ElMessage.success(`已恢复`)
  }).catch(() => ElMessage.error('恢复失败'))
}

async function permanentDelete(f: any) {
  await ElMessageBox.confirm(`"${f.title || f.name}" 将被永久删除且无法恢复`, '彻底删除', { type: 'warning' })
  deleteDoc(f.docId || f.id).then(() => {
    files.value = files.value.filter(x => x.id !== f.id)
    ElMessage.success('已彻底删除')
  }).catch(() => ElMessage.error('删除失败'))
}

async function clearAll() {
  await ElMessageBox.confirm('清空后所有文件将被永久删除且无法恢复', '清空回收站', { type: 'warning' })
  files.value = []
  ElMessage.success('回收站已清空')
}
</script>

<style lang="scss" scoped>
.recycle-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}
.header-left { display: flex; align-items: flex-start; gap: 14px; }
.header-icon {
  font-size: 36px; color: var(--c-text-muted);
  background: var(--c-surface-dim); padding: 10px; border-radius: var(--radius-lg);
}

.clear-all-btn {
  display: flex; align-items: center; gap: 6px; flex-shrink: 0;
  padding: 8px 16px;
  border: 1px solid var(--c-danger); border-radius: var(--radius-lg);
  background: transparent; color: var(--c-danger);
  font-size: 13px; font-weight: 500; cursor: pointer;
  transition: all .15s var(--ease-out);
  &:hover:not(:disabled) { background: var(--c-danger-light); }
  &:disabled { opacity: .35; cursor: not-allowed; }
  .material-symbols-outlined { font-size: 18px; }
}

// -- File list --
.file-list {
  background: var(--c-surface);
  border: 1px solid var(--c-border);
  border-radius: var(--radius-xl);
  overflow: hidden;
}

.file-row {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px 20px;
  border-bottom: 1px solid var(--c-border-light);
  transition: background .15s var(--ease-out);

  &:hover { background: var(--c-surface-hover); }
  &:last-child { border-bottom: 0; }
}

.file-icon {
  width: 40px; height: 40px;
  border-radius: var(--radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.file-info { flex: 1; min-width: 0; }
.file-name { font-size: 14px; font-weight: 600; color: var(--c-text); }
.file-meta { color: var(--c-text-muted); margin-top: 2px; display: flex; align-items: center; gap: 4px; }
.meta-sep { color: var(--c-text-ghost); }

.file-actions { display: flex; gap: 8px; flex-shrink: 0; }

.action-btn {
  display: inline-flex; align-items: center; gap: 4px;
  padding: 6px 14px; border-radius: var(--radius-lg);
  font-size: 13px; font-weight: 500; cursor: pointer;
  transition: all .15s var(--ease-out); border: 1px solid transparent;
  .material-symbols-outlined { font-size: 16px; }
}

.restore-btn {
  background: var(--c-brand-light); color: var(--c-brand); border-color: transparent;
  &:hover { background: var(--c-brand); color: #fff; }
}

.delete-btn {
  background: transparent; color: var(--c-text-muted); border-color: var(--c-border);
  &:hover { color: var(--c-danger); border-color: var(--c-danger); background: var(--c-danger-light); }
}

// -- Empty state --
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  gap: 8px;
}
.empty-icon { font-size: 56px; color: var(--c-text-ghost); margin-bottom: 4px; }
</style>
