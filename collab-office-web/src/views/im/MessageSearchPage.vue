<template>
  <div class="msg-search-page">
    <!-- Main content area -->
    <div class="search-layout">
      <!-- Left: Search Content -->
      <main class="search-main">
        <!-- Search Header -->
        <div class="search-top-bar">
          <div class="search-input-wrapper">
            <span class="material-symbols-outlined search-prefix-icon">search</span>
            <el-input
              v-model="keyword"
              placeholder="搜索消息、联系人或文件..."
              size="large"
              clearable
              @keyup.enter="doSearch"
              class="main-search-input"
            />
          </div>
        </div>

        <!-- Recent Search Chips -->
        <div class="recent-searches" v-if="!keyword && !results.length">
          <h3 class="section-title">
            <span class="material-symbols-outlined section-icon">history</span>
            最近搜索
          </h3>
          <div class="chip-row">
            <span class="search-chip">Q4 财务报表</span>
            <span class="search-chip">API 接口文档</span>
            <span class="search-chip">设计规范 v2.0</span>
            <span class="search-chip chip-clear">清除历史</span>
          </div>
        </div>

        <!-- Tabs Navigation -->
        <div class="result-tabs" v-if="keyword">
          <button class="tab-btn active">
            <span class="material-symbols-outlined tab-icon">forum</span>
            消息 ({{ results.length }})
          </button>
          <button class="tab-btn">
            <span class="material-symbols-outlined tab-icon">person</span>
            联系人 (3)
          </button>
          <button class="tab-btn">
            <span class="material-symbols-outlined tab-icon">groups</span>
            群组 (1)
          </button>
          <button class="tab-btn">
            <span class="material-symbols-outlined tab-icon">description</span>
            文件 (12)
          </button>
        </div>

        <!-- Search Results -->
        <div class="results" v-loading="loading">
          <!-- Empty state -->
          <div v-if="!results.length && !loading && keyword" class="empty-result">
            <div class="empty-content">
              <span class="material-symbols-outlined empty-icon">search_off</span>
              <p class="empty-text">未找到相关消息</p>
              <p class="empty-hint">尝试使用其他关键词或调整筛选条件</p>
            </div>
          </div>

          <div v-if="!results.length && !loading && !keyword" class="empty-result">
            <div class="empty-content">
              <span class="material-symbols-outlined empty-icon">manage_search</span>
              <p class="empty-text">输入关键词开始搜索</p>
              <p class="empty-hint">搜索所有会话中的消息、联系人和文件</p>
            </div>
          </div>

          <!-- Result Items -->
          <div
            v-for="msg in results"
            :key="msg.msgId"
            class="result-item"
            @click="jumpToMsg(msg)"
          >
            <div class="result-header">
              <div class="result-sender">
                <div class="sender-avatar-wrapper">
                  <el-avatar :size="40" :style="{ backgroundColor: '#3370FF' }" class="sender-avatar">
                    {{ msg.senderName?.[0] }}
                  </el-avatar>
                  <span class="presence-dot online"></span>
                </div>
                <div class="sender-info">
                  <span class="sender-name">{{ msg.senderName }}</span>
                  <span class="sender-source">
                    来自 <span class="source-highlight">{{ msg.sessionName || '--' }}</span>
                  </span>
                </div>
              </div>
              <span class="result-time">{{ msg.sendTime }}</span>
            </div>

            <div class="result-content" v-html="highlight(msg.content)" />

            <div class="result-footer">
              <span class="footer-meta" v-if="msg.hasAttachment">
                <span class="material-symbols-outlined meta-icon">attachment</span>
                {{ msg.attachmentCount || 2 }} 附件
              </span>
              <span class="footer-dot" v-if="msg.hasAttachment">·</span>
              <span class="footer-meta" v-if="msg.hasReply">
                <span class="material-symbols-outlined meta-icon">reply</span>
                回复于{{ msg.replyTime || '昨天' }}
              </span>
            </div>
          </div>

          <!-- Load More -->
          <div v-if="results.length" class="load-more-row">
            <button class="load-more-btn">
              加载更多搜索结果
              <span class="material-symbols-outlined">expand_more</span>
            </button>
          </div>
        </div>
      </main>

      <!-- Right: Filter Sidebar -->
      <aside class="filter-sidebar">
        <h4 class="sidebar-title">搜索筛选</h4>

        <div class="filter-section">
          <label class="filter-label">时间范围</label>
          <el-select
            v-model="timeRange"
            class="filter-select"
            placeholder="任何时间"
          >
            <el-option label="任何时间" value="any" />
            <el-option label="过去 24 小时" value="24h" />
            <el-option label="过去 7 天" value="7d" />
            <el-option label="过去 30 天" value="30d" />
            <el-option label="选择具体日期..." value="custom" />
          </el-select>
          <div v-if="timeRange === 'custom'" class="custom-date-row">
            <el-date-picker
              v-model="filterDate"
              type="date"
              placeholder="日期"
              size="small"
              class="filter-date"
            />
          </div>
        </div>

        <div class="filter-section">
          <label class="filter-label">会话</label>
          <el-select
            v-model="filterSession"
            placeholder="筛选会话"
            clearable
            class="filter-select"
          >
            <el-option
              v-for="s in sessions"
              :key="s.sessionId"
              :label="s.sessionName || s.name"
              :value="s.sessionId"
            />
          </el-select>
        </div>

        <div class="filter-section">
          <label class="filter-label">消息类型</label>
          <el-select
            v-model="filterType"
            placeholder="消息类型"
            clearable
            class="filter-select"
          >
            <el-option label="文本" :value="1" />
            <el-option label="图片" :value="2" />
            <el-option label="文件" :value="3" />
          </el-select>
        </div>

        <div class="filter-section">
          <label class="filter-label">会话类型</label>
          <div class="checkbox-group">
            <label class="checkbox-item">
              <input type="checkbox" checked class="filter-checkbox" />
              <span class="checkbox-label">群组消息</span>
            </label>
            <label class="checkbox-item">
              <input type="checkbox" checked class="filter-checkbox" />
              <span class="checkbox-label">私聊消息</span>
            </label>
            <label class="checkbox-item">
              <input type="checkbox" class="filter-checkbox" />
              <span class="checkbox-label">频道公告</span>
            </label>
          </div>
        </div>

        <div class="filter-actions">
          <button class="reset-btn" @click="resetFilters">重置所有筛选</button>
        </div>

        <!-- Search Tips -->
        <div class="search-tips">
          <p class="tips-title">
            <span class="material-symbols-outlined tips-icon">tips_and_updates</span>
            搜索小贴士
          </p>
          <p class="tips-text">
            使用 "from:me" 搜索你发送的消息，或者 "has:file" 查找带附件的讨论。
          </p>
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'

const router = useRouter()
const keyword = ref('')
const loading = ref(false)
const filterSession = ref('')
const filterType = ref<number | null>(null)
const filterDate = ref('')
const results = ref<any[]>([])
const sessions = ref<any[]>([])
const timeRange = ref('any')

onMounted(() => {
  request.get('/chat/conversations').then((res: any) => {
    if (res.code === 200) sessions.value = res.data?.records || res.data || []
  }).catch(() => {})
})

function doSearch() {
  if (!keyword.value.trim()) return
  loading.value = true
  request.get('/chat/messages/search', {
    params: {
      keyword: keyword.value.trim(),
      sessionId: filterSession.value || undefined,
      type: filterType.value || undefined,
    }
  }).then((res: any) => {
    if (res.code === 200) results.value = res.data?.records || res.data || []
  }).catch(() => {}).finally(() => loading.value = false)
}

function highlight(text: string): string {
  if (!keyword.value) return text
  const escaped = keyword.value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  return text.replace(new RegExp(`(${escaped})`, 'gi'), '<mark>$1</mark>')
}

function jumpToMsg(msg: any) {
  router.push(`/im?session=${msg.sessionId}`)
}

function resetFilters() {
  filterSession.value = ''
  filterType.value = null
  filterDate.value = ''
  timeRange.value = 'any'
}
</script>

<style lang="scss" scoped>
/* ===== Design Tokens ===== */
$c-bg: #f8f9fa;
$c-surface: #ffffff;
$c-surface-low: #f3f4f5;
$c-surface-high: #e7e8e9;
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
$c-warning: #ef4444;
$radius-sm: 4px;
$radius: 8px;
$radius-lg: 12px;
$radius-xl: 16px;

/* ===== Material Symbols ===== */
.material-symbols-outlined {
  font-variation-settings: 'FILL' 0, 'wght' 400, 'GRAD' 0, 'opsz' 24;
  vertical-align: middle;
}

/* ===== Page Layout ===== */
.msg-search-page {
  height: 100%;
  background: $c-surface;
  overflow: hidden;
}

.search-layout {
  display: flex;
  height: 100%;
  overflow: hidden;
}

/* ===== Main Search Area ===== */
.search-main {
  flex: 1;
  overflow-y: auto;
  padding: 24px 32px;
  max-width: 900px;
}

.search-top-bar {
  margin-bottom: 24px;
}

.search-input-wrapper {
  position: relative;

  .search-prefix-icon {
    position: absolute;
    left: 16px;
    top: 50%;
    transform: translateY(-50%);
    color: $c-text-ghost;
    font-size: 22px;
    z-index: 2;
  }

  .main-search-input {
    :deep(.el-input__wrapper) {
      padding-left: 48px;
      background: $c-surface-low;
      border: none;
      border-radius: $radius-lg;
      box-shadow: none;

      &:focus,
      &.is-focus {
        box-shadow: 0 0 0 2px rgba(0, 80, 214, 0.2);
      }
    }

    :deep(.el-input__inner) {
      font-size: 14px;
      color: $c-text;
    }
  }
}

/* ===== Recent Searches ===== */
.recent-searches {
  margin-bottom: 24px;
}

.section-title {
  font-size: 20px;
  font-weight: 600;
  color: $c-text;
  margin: 0 0 12px 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.section-icon {
  font-size: 20px;
  color: $c-brand;
}

.chip-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.search-chip {
  display: inline-block;
  padding: 6px 16px;
  background: $c-surface-high;
  border-radius: 999px;
  font-size: 12px;
  color: $c-text-muted;
  cursor: pointer;
  transition: background 0.15s;

  &:hover {
    background: #dfe2eb;
  }

  &.chip-clear {
    color: $c-error;
    font-weight: 500;
  }
}

/* ===== Result Tabs ===== */
.result-tabs {
  display: flex;
  border-bottom: 1px solid $c-border;
  margin-bottom: 20px;
  position: sticky;
  top: 0;
  background: $c-surface;
  z-index: 10;
  padding-top: 4px;
}

.tab-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 12px 20px;
  background: none;
  border: none;
  border-bottom: 2px solid transparent;
  font-size: 14px;
  color: $c-text-secondary;
  cursor: pointer;
  transition: all 0.15s;

  &:hover {
    color: $c-text;
  }

  &.active {
    color: $c-brand;
    border-bottom-color: $c-brand;
    font-weight: 600;
  }

  .tab-icon {
    font-size: 18px;
  }
}

/* ===== Results ===== */
.results {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 300px;
}

/* ===== Empty State ===== */
.empty-result {
  display: flex;
  justify-content: center;
  padding: 80px 0;
}

.empty-content {
  text-align: center;
}

.empty-icon {
  font-size: 48px;
  color: $c-text-ghost;
  margin-bottom: 12px;
}

.empty-text {
  font-size: 16px;
  color: $c-text;
  font-weight: 500;
  margin: 0 0 4px 0;
}

.empty-hint {
  font-size: 13px;
  color: $c-text-ghost;
  margin: 0;
}

/* ===== Result Item ===== */
.result-item {
  padding: 16px;
  background: $c-surface;
  border: 1px solid $c-border;
  border-radius: $radius-xl;
  cursor: pointer;
  transition: all 0.15s;

  &:hover {
    background: $c-surface-low;
  }

  &:active {
    transform: scale(0.98);
  }
}

.result-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 8px;
}

.result-sender {
  display: flex;
  align-items: center;
  gap: 12px;
}

.sender-avatar-wrapper {
  position: relative;
  flex-shrink: 0;
}

.sender-avatar {
  border: 2px solid $c-bg;
}

.presence-dot {
  position: absolute;
  bottom: 0;
  right: 0;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  border: 2px solid $c-surface;

  &.online {
    background: $c-success;
  }
}

.sender-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.sender-name {
  font-size: 14px;
  font-weight: 600;
  color: $c-text;
}

.sender-source {
  font-size: 12px;
  color: $c-text-secondary;

  .source-highlight {
    color: $c-brand;
    font-weight: 500;
  }
}

.result-time {
  font-size: 12px;
  color: $c-text-ghost;
  flex-shrink: 0;
}

.result-content {
  padding-left: 52px;
  font-size: 14px;
  color: $c-text-muted;
  line-height: 1.6;

  :deep(mark) {
    background: rgba(42, 106, 249, 0.15);
    color: $c-brand;
    font-weight: 600;
    padding: 0 2px;
    border-radius: 2px;
  }
}

.result-footer {
  padding-left: 52px;
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 10px;
  font-size: 12px;
  color: $c-text-ghost;
}

.footer-meta {
  display: flex;
  align-items: center;
  gap: 4px;
}

.meta-icon {
  font-size: 16px;
}

.footer-dot {
  color: $c-text-ghost;
}

/* ===== Load More ===== */
.load-more-row {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}

.load-more-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 24px;
  background: none;
  border: 1px solid $c-border;
  border-radius: 999px;
  font-size: 14px;
  color: $c-text-secondary;
  cursor: pointer;
  transition: all 0.15s;

  &:hover {
    background: $c-surface-high;
    color: $c-text;
  }

  .material-symbols-outlined {
    font-size: 20px;
  }
}

/* ===== Filter Sidebar ===== */
.filter-sidebar {
  width: 280px;
  flex-shrink: 0;
  padding: 24px 20px;
  border-left: 1px solid $c-border;
  background: $c-surface;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.sidebar-title {
  font-size: 20px;
  font-weight: 600;
  color: $c-text;
  margin: 0 0 4px 0;
}

.filter-section {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.filter-label {
  font-size: 12px;
  font-weight: 600;
  color: $c-text-secondary;
  letter-spacing: 0.01em;
}

.filter-select {
  :deep(.el-input__wrapper) {
    background: $c-surface-low;
    border: none;
    border-radius: $radius;
    box-shadow: none;
  }
}

.custom-date-row {
  margin-top: 4px;
}

.filter-date {
  width: 100%;
}

/* ===== Checkbox Group ===== */
.checkbox-group {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.checkbox-item {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 6px 8px;
  border-radius: $radius;
  transition: background 0.15s;

  &:hover {
    background: $c-surface-low;
  }
}

.filter-checkbox {
  width: 16px;
  height: 16px;
  border-radius: 4px;
  accent-color: $c-brand;
  cursor: pointer;
}

.checkbox-label {
  font-size: 14px;
  color: $c-text;
}

/* ===== Filter Actions ===== */
.filter-actions {
  padding-top: 4px;
}

.reset-btn {
  width: 100%;
  padding: 10px 0;
  background: #dfe2eb;
  color: #181c22;
  border: none;
  border-radius: $radius;
  font-weight: 600;
  font-size: 14px;
  cursor: pointer;
  transition: background 0.15s;

  &:hover {
    background: #c3c6ce;
  }
}

/* ===== Search Tips ===== */
.search-tips {
  margin-top: auto;
  padding: 16px;
  background: $c-surface-high;
  border: 1px solid $c-border;
  border-radius: $radius-xl;
}

.tips-title {
  font-size: 12px;
  font-weight: 600;
  color: $c-brand;
  margin: 0 0 8px 0;
  display: flex;
  align-items: center;
  gap: 4px;
}

.tips-icon {
  font-size: 16px;
}

.tips-text {
  font-size: 12px;
  color: $c-text-secondary;
  line-height: 1.5;
  margin: 0;
}

/* ===== Scrollbar ===== */
.search-main,
.filter-sidebar {
  &::-webkit-scrollbar {
    width: 6px;
  }

  &::-webkit-scrollbar-track {
    background: transparent;
  }

  &::-webkit-scrollbar-thumb {
    background: #e1e3e4;
    border-radius: 10px;

    &:hover {
      background: #c3c6d8;
    }
  }
}
</style>
