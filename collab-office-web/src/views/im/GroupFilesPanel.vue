<template>
  <div class="group-files-page">
    <!-- Breadcrumb -->
    <nav class="breadcrumb">
      <span class="breadcrumb-item">Messages</span>
      <span class="material-symbols-outlined breadcrumb-sep">chevron_right</span>
      <span class="breadcrumb-item">Design Team Sync</span>
      <span class="material-symbols-outlined breadcrumb-sep">chevron_right</span>
      <span class="breadcrumb-item active">Group Files</span>
    </nav>

    <!-- Page Header -->
    <div class="page-header">
      <h1 class="page-title">群文件汇总</h1>
      <div class="header-actions">
        <button class="upload-btn">
          <span class="material-symbols-outlined">cloud_upload</span>
          <span>上传新文件</span>
        </button>
      </div>
    </div>

    <!-- Filters & Search Bar -->
    <div class="filter-bar">
      <div class="filter-tabs">
        <button
          class="filter-tab"
          :class="{ active: filterType === 'all' }"
          @click="filterType = 'all'"
        >
          全部
        </button>
        <button
          class="filter-tab"
          :class="{ active: filterType === 'doc' }"
          @click="filterType = 'doc'"
        >
          文档
        </button>
        <button
          class="filter-tab"
          :class="{ active: filterType === 'sheet' }"
          @click="filterType = 'sheet'"
        >
          表格
        </button>
        <button
          class="filter-tab"
          :class="{ active: filterType === 'image' }"
          @click="filterType = 'image'"
        >
          图片
        </button>
        <button
          class="filter-tab"
          :class="{ active: filterType === 'archive' }"
          @click="filterType = 'archive'"
        >
          压缩包
        </button>
      </div>
      <div class="search-box">
        <span class="material-symbols-outlined search-icon">search</span>
        <input
          v-model="fileSearch"
          class="search-input"
          type="text"
          placeholder="按文件名搜索..."
        />
      </div>
    </div>

    <!-- File Table -->
    <div class="file-table-card">
      <table class="file-table">
        <thead>
          <tr class="table-header-row">
            <th class="col-name">文件名</th>
            <th class="col-size">大小</th>
            <th class="col-sender">发送者</th>
            <th class="col-date">发送日期</th>
            <th class="col-actions">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="f in searchedFiles"
            :key="f.fileId"
            class="table-row"
            @click="preview(f)"
          >
            <td class="col-name">
              <div class="file-name-cell">
                <div class="file-icon-box" :class="'icon-' + getFileCategory(f)">
                  <span class="material-symbols-outlined">{{ getFileIcon(f) }}</span>
                </div>
                <div class="file-name-info">
                  <div class="file-name">{{ f.fileName }}</div>
                  <div class="file-type-label">{{ getFileTypeLabel(f) }}</div>
                </div>
              </div>
            </td>
            <td class="col-size">
              <span class="size-text">{{ formatSize(f.fileSize) }}</span>
            </td>
            <td class="col-sender">
              <div class="sender-cell">
                <div class="sender-avatar-sm">
                  <span class="sender-initial">{{ f.uploaderName?.[0] || '?' }}</span>
                </div>
                <span class="sender-name">{{ f.uploaderName || '--' }}</span>
              </div>
            </td>
            <td class="col-date">
              <span class="date-text">{{ f.createTime?.substring(0, 16) || '--' }}</span>
            </td>
            <td class="col-actions">
              <div class="action-btns">
                <button class="action-btn" title="下载" @click.stop="handleDownload(f)">
                  <span class="material-symbols-outlined">download</span>
                </button>
                <button class="action-btn" title="转发" @click.stop>
                  <span class="material-symbols-outlined">forward</span>
                </button>
                <button class="action-btn" title="更多" @click.stop>
                  <span class="material-symbols-outlined">more_vert</span>
                </button>
              </div>
            </td>
          </tr>
          <tr v-if="!searchedFiles.length">
            <td colspan="5" class="empty-row">
              <div class="empty-state">
                <span class="material-symbols-outlined empty-icon">folder_off</span>
                <p class="empty-text">暂无文件</p>
              </div>
            </td>
          </tr>
        </tbody>
      </table>

      <!-- Pagination -->
      <div class="pagination-bar" v-if="filteredFiles.length">
        <span class="pagination-info">显示 {{ searchedFiles.length }} / 共 {{ filteredFiles.length }} 个文件</span>
        <div class="pagination-btns">
          <button class="page-btn" disabled>
            <span class="material-symbols-outlined">chevron_left</span>
          </button>
          <button class="page-num active">1</button>
          <button class="page-num">2</button>
          <button class="page-num">3</button>
          <span class="page-ellipsis">...</span>
          <button class="page-num">12</button>
          <button class="page-btn">
            <span class="material-symbols-outlined">chevron_right</span>
          </button>
        </div>
      </div>
    </div>

    <!-- Bottom Statistics -->
    <div class="bottom-stats">
      <div class="stat-card storage-card">
        <div class="stat-icon-box icon-cloud">
          <span class="material-symbols-outlined">cloud</span>
        </div>
        <div class="stat-content">
          <div class="stat-title">群存储空间</div>
          <div class="storage-bar">
            <div class="storage-bar-fill" :style="{ width: storagePercent + '%' }"></div>
          </div>
          <div class="stat-meta">已使用 {{ usedStorage }} / 总计 {{ totalStorage }}</div>
        </div>
      </div>

      <div class="stat-card activity-card">
        <div class="stat-icon-box icon-activity">
          <span class="material-symbols-outlined">update</span>
        </div>
        <div class="stat-content">
          <div class="stat-title">最近更新</div>
          <div class="stat-meta">{{ lastActivityText }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'

const props = defineProps<{ visible: boolean; sessionId: string }>()
defineEmits<{ (e: 'update:visible', v: boolean): void }>()

const router = useRouter()
const filterType = ref('all')
const files = ref<any[]>([])
const fileSearch = ref('')

watch(() => props.visible, (v) => { if (v && props.sessionId) loadFiles() })

const usedStorage = ref('1.2GB')
const totalStorage = ref('5GB')
const storagePercent = ref(24)
const lastActivityText = ref('15 分钟前 David Chen 上传了 1 个文件')

function loadFiles() {
  request.get('/file/list', { params: { spaceId: 1 } }).then((res: any) => {
    if (res.code === 200) files.value = res.data?.records || res.data || []
  }).catch(() => {})
}

const filteredFiles = computed(() => {
  let result = files.value
  if (filterType.value === 'image') {
    result = result.filter((f: any) => /\.(png|jpg|jpeg|gif|svg|webp)$/i.test(f.fileName || ''))
  } else if (filterType.value === 'doc') {
    result = result.filter((f: any) => /\.(doc|docx|pdf|txt|md)$/i.test(f.fileName || ''))
  } else if (filterType.value === 'sheet') {
    result = result.filter((f: any) => /\.(xlsx|xls|csv)$/i.test(f.fileName || ''))
  } else if (filterType.value === 'archive') {
    result = result.filter((f: any) => /\.(zip|rar|7z|tar|gz)$/i.test(f.fileName || ''))
  }
  // 'all' returns everything
  return result
})

const searchedFiles = computed(() => {
  if (!fileSearch.value.trim()) return filteredFiles.value
  const kw = fileSearch.value.toLowerCase()
  return filteredFiles.value.filter((f: any) =>
    (f.fileName || '').toLowerCase().includes(kw)
  )
})

function getFileCategory(f: any): string {
  const name = (f.fileName || '').toLowerCase()
  if (/\.(png|jpg|jpeg|gif|svg|webp)$/i.test(name)) return 'image'
  if (/\.(xlsx|xls|csv)$/i.test(name)) return 'sheet'
  if (/\.(zip|rar|7z|tar|gz)$/i.test(name)) return 'archive'
  return 'doc'
}

function getFileIcon(f: any): string {
  const name = (f.fileName || '').toLowerCase()
  if (/\.(png|jpg|jpeg|gif|svg|webp)$/i.test(name)) return 'image'
  if (/\.(xlsx|xls|csv)$/i.test(name)) return 'table_chart'
  if (/\.(zip|rar|7z|tar|gz)$/i.test(name)) return 'folder_zip'
  if (/\.pdf$/i.test(name)) return 'picture_as_pdf'
  return 'description'
}

function getFileTypeLabel(f: any): string {
  const name = (f.fileName || '').toLowerCase()
  if (/\.pdf$/i.test(name)) return 'PDF 文档'
  if (/\.(doc|docx)$/i.test(name)) return 'Word 文档'
  if (/\.(xlsx|xls)$/i.test(name)) return 'Excel 表格'
  if (/\.csv$/i.test(name)) return 'CSV 表格'
  if (/\.(png|jpg|jpeg)$/i.test(name)) return 'PNG 图像'
  if (/\.(gif|svg|webp)$/i.test(name)) return '图像文件'
  if (/\.(zip|rar|7z|tar|gz)$/i.test(name)) return 'ZIP 压缩包'
  if (/\.(txt|md)$/i.test(name)) return '文本文档'
  return f.fileType || '文件'
}

function preview(f: any) {
  router.push(`/files/preview/${f.fileId}`)
}

function handleDownload(f: any) {
  window.open(`/api/v1/file/download/${f.fileId}`, '_blank')
}

function formatSize(b: number): string {
  if (!b) return '--'
  const u = ['B','KB','MB','GB']; let i = 0; let s = b
  while (s >= 1024 && i < 3) { s /= 1024; i++ }
  return `${s.toFixed(1)} ${u[i]}`
}
</script>

<style lang="scss" scoped>
/* ===== Design Tokens ===== */
$c-bg: #f8f9fa;
$c-surface: #ffffff;
$c-surface-low: #f3f4f5;
$c-surface-high: #e7e8e9;
$c-surface-highest: #e1e3e4;
$c-text: #191c1d;
$c-text-secondary: #5b5e65;
$c-text-muted: #424655;
$c-text-ghost: #737687;
$c-brand: #0050d6;
$c-brand-light: rgba(51, 112, 255, 0.1);
$c-brand-container: #2a6af9;
$c-border: #e5e6eb;
$c-error: #ba1a1a;
$c-success: #22c55e;
$radius-sm: 4px;
$radius: 8px;
$radius-lg: 12px;
$radius-xl: 16px;

.material-symbols-outlined {
  font-variation-settings: 'FILL' 0, 'wght' 400, 'GRAD' 0, 'opsz' 24;
  vertical-align: middle;
}

/* ===== Page Layout ===== */
.group-files-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
  max-width: 1200px;
  margin: 0 auto;
  padding: 16px;
  width: 100%;
  background: $c-bg;
}

/* ===== Breadcrumb ===== */
.breadcrumb {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: $c-text-ghost;
  margin-bottom: 4px;
}

.breadcrumb-item {
  cursor: pointer;

  &:hover {
    color: $c-brand;
  }

  &.active {
    color: $c-brand;
    font-weight: 600;
  }
}

.breadcrumb-sep {
  font-size: 14px;
  color: $c-text-ghost;
}

/* ===== Page Header ===== */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
}

.page-title {
  font-size: 32px;
  font-weight: 700;
  line-height: 40px;
  letter-spacing: -0.02em;
  color: $c-text;
  margin: 0;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.upload-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 18px;
  background: $c-surface-highest;
  border: none;
  border-radius: $radius;
  font-size: 12px;
  font-weight: 500;
  color: $c-text;
  cursor: pointer;
  transition: background 0.15s;

  &:hover {
    background: #d9dadb;
  }

  .material-symbols-outlined {
    font-size: 20px;
  }
}

/* ===== Filter Bar ===== */
.filter-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  background: $c-surface;
  border: 1px solid $c-border;
  border-radius: $radius-xl;
  padding: 16px 20px;
  flex-wrap: wrap;
}

.filter-tabs {
  display: flex;
  align-items: center;
  background: $c-surface-low;
  border-radius: $radius;
  padding: 3px;
  gap: 2px;
}

.filter-tab {
  padding: 8px 20px;
  background: transparent;
  border: none;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 500;
  color: $c-text-secondary;
  cursor: pointer;
  transition: all 0.15s;

  &:hover {
    color: $c-text;
  }

  &.active {
    background: $c-surface;
    color: $c-brand;
    font-weight: 600;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  }
}

.search-box {
  position: relative;
  min-width: 280px;
}

.search-icon {
  position: absolute;
  left: 12px;
  top: 50%;
  transform: translateY(-50%);
  color: $c-text-ghost;
  font-size: 18px;
}

.search-input {
  width: 100%;
  padding: 10px 12px 10px 38px;
  background: $c-bg;
  border: 1px solid $c-border;
  border-radius: $radius;
  font-size: 14px;
  color: $c-text;
  outline: none;
  box-sizing: border-box;

  &:focus {
    border-color: $c-brand;
  }

  &::placeholder {
    color: $c-text-ghost;
  }
}

/* ===== File Table ===== */
.file-table-card {
  background: $c-surface;
  border: 1px solid $c-border;
  border-radius: $radius-xl;
  overflow: hidden;
}

.file-table {
  width: 100%;
  border-collapse: collapse;
  table-layout: fixed;
}

.table-header-row {
  background: $c-surface-low;
  border-bottom: 1px solid $c-border;

  th {
    padding: 14px 20px;
    font-size: 12px;
    font-weight: 600;
    color: $c-text-secondary;
    text-transform: uppercase;
    letter-spacing: 0.04em;
    text-align: left;
  }
}

.col-name { width: 35%; }
.col-size { width: 12%; }
.col-sender { width: 18%; }
.col-date { width: 20%; }
.col-actions { width: 15%; text-align: right; }

.table-row {
  border-bottom: 1px solid $c-border;
  cursor: pointer;
  transition: background 0.15s;

  &:hover {
    background: $c-surface-low;
  }

  td {
    padding: 14px 20px;
    vertical-align: middle;
  }
}

/* ===== File Name Cell ===== */
.file-name-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.file-icon-box {
  width: 40px;
  height: 40px;
  border-radius: $radius;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;

  .material-symbols-outlined {
    font-size: 22px;
    font-variation-settings: 'FILL' 1, 'wght' 400, 'GRAD' 0, 'opsz' 24;
  }

  &.icon-doc {
    background: #eff6ff;
    color: #2563eb;
  }

  &.icon-sheet {
    background: #f0fdf4;
    color: #16a34a;
  }

  &.icon-image {
    background: $c-surface-low;
    overflow: hidden;
  }

  &.icon-archive {
    background: #fff7ed;
    color: #ea580c;
  }
}

.file-name-info {
  min-width: 0;
}

.file-name {
  font-size: 14px;
  font-weight: 600;
  color: $c-text;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-type-label {
  font-size: 12px;
  color: $c-text-ghost;
}

/* ===== Other Columns ===== */
.size-text,
.date-text {
  font-size: 14px;
  color: $c-text-secondary;
}

.sender-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.sender-avatar-sm {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: $c-surface-high;
  border: 1px solid $c-border;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  flex-shrink: 0;
}

.sender-initial {
  font-size: 11px;
  font-weight: 600;
  color: $c-text-muted;
}

.sender-name {
  font-size: 14px;
  color: $c-text;
}

/* ===== Action Buttons ===== */
.action-btns {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 4px;
}

.action-btn {
  padding: 8px;
  background: none;
  border: none;
  color: $c-text-secondary;
  cursor: pointer;
  border-radius: $radius;
  transition: all 0.15s;

  &:hover {
    color: $c-brand;
    background: $c-surface;
  }

  .material-symbols-outlined {
    font-size: 20px;
  }
}

/* ===== Empty State ===== */
.empty-row {
  text-align: center;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 60px 0;
  color: $c-text-ghost;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 8px;
}

.empty-text {
  font-size: 14px;
  margin: 0;
}

/* ===== Pagination ===== */
.pagination-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 20px;
  border-top: 1px solid $c-border;
  background: $c-surface;
}

.pagination-info {
  font-size: 12px;
  color: $c-text-secondary;
}

.pagination-btns {
  display: flex;
  align-items: center;
  gap: 2px;
}

.page-btn {
  padding: 6px;
  background: none;
  border: none;
  border-radius: $radius;
  color: $c-text-secondary;
  cursor: pointer;
  display: flex;
  align-items: center;

  &:hover:not(:disabled) {
    background: $c-surface-low;
  }

  &:disabled {
    opacity: 0.3;
    cursor: not-allowed;
  }

  .material-symbols-outlined {
    font-size: 18px;
  }
}

.page-num {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: none;
  border: none;
  border-radius: $radius;
  font-size: 12px;
  font-weight: 500;
  color: $c-text-secondary;
  cursor: pointer;

  &:hover {
    background: $c-surface-low;
  }

  &.active {
    background: $c-brand;
    color: #fff;
    font-weight: 600;
  }
}

.page-ellipsis {
  padding: 0 4px;
  font-size: 12px;
  color: $c-text-secondary;
}

/* ===== Bottom Stats ===== */
.bottom-stats {
  display: flex;
  gap: 16px;
}

.stat-card {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 20px;
  border-radius: $radius-xl;
}

.storage-card {
  background: rgba(219, 234, 254, 0.4);
  border: 1px solid rgba(147, 197, 253, 0.6);
}

.activity-card {
  background: $c-surface;
  border: 1px solid $c-border;
}

.stat-icon-box {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;

  .material-symbols-outlined {
    font-size: 24px;
  }

  &.icon-cloud {
    background: rgba(0, 80, 214, 0.1);
    color: $c-brand;
  }

  &.icon-activity {
    background: #dde3ed;
    color: #565c65;
  }
}

.stat-content {
  min-width: 0;
  flex: 1;
}

.stat-title {
  font-size: 14px;
  font-weight: 600;
  color: $c-text;
  margin-bottom: 6px;
}

.storage-bar {
  width: 100%;
  height: 8px;
  background: $c-surface-highest;
  border-radius: 4px;
  overflow: hidden;
  margin-bottom: 6px;
  max-width: 220px;
}

.storage-bar-fill {
  height: 100%;
  background: $c-brand;
  border-radius: 4px;
  transition: width 0.4s ease;
}

.stat-meta {
  font-size: 12px;
  color: $c-text-secondary;
}

/* ===== Scrollbar ===== */
.group-files-page {
  &::-webkit-scrollbar {
    width: 6px;
    height: 6px;
  }

  &::-webkit-scrollbar-track {
    background: transparent;
  }

  &::-webkit-scrollbar-thumb {
    background: #e1e3e4;
    border-radius: 10px;

    &:hover {
      background: #c3c6ce;
    }
  }
}
</style>
