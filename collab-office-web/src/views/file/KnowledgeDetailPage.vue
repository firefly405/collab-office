<template>
  <div class="knowledge-page">
    <!-- Top Header -->
    <header class="top-header">
      <div class="header-left">
        <span class="header-logo">Nexus Office</span>
        <div class="search-box">
          <span class="material-symbols-outlined search-icon">search</span>
          <input class="search-input" type="text" placeholder="搜索文档或知识库..." />
        </div>
      </div>
      <div class="header-right">
        <div class="header-icons">
          <span class="material-symbols-outlined">notifications</span>
          <span class="material-symbols-outlined">group_add</span>
          <span class="material-symbols-outlined">apps</span>
        </div>
        <button class="share-btn">Share</button>
        <div class="user-avatar">
          <div class="avatar-ring" />
        </div>
      </div>
    </header>

    <!-- Body -->
    <div class="knowledge-body">
      <!-- Left Sidebar -->
      <aside class="knowledge-sidebar">
        <div class="sidebar-header">
          <h4 class="sidebar-section-label">知识库目录</h4>
          <div class="sidebar-folder">
            <span class="material-symbols-outlined">folder_open</span>
            <span class="sidebar-folder-name">企业通用规范</span>
          </div>
        </div>

        <nav class="sidebar-nav">
          <div class="toc-item active">
            <span class="material-symbols-outlined">description</span>
            <span class="toc-name">品牌视觉识别系统</span>
          </div>
          <div class="toc-item" v-for="item in toc" :key="item.id" @click="activeSection = item.id">
            <span class="material-symbols-outlined">table_chart</span>
            <span class="toc-name">{{ item.title }}</span>
            <span class="toc-count">{{ item.count }}</span>
          </div>
          <div class="toc-sub-items">
            <div class="toc-sub-item">
              <span class="material-symbols-outlined">draw</span>
              <span>设计风格指南</span>
            </div>
            <div class="toc-sub-item">
              <span class="material-symbols-outlined">draw</span>
              <span>UI组件库</span>
            </div>
          </div>
          <div class="toc-item">
            <span class="material-symbols-outlined">forum</span>
            <span class="toc-name">团队沟通准则</span>
          </div>
          <div class="toc-item">
            <span class="material-symbols-outlined">videocam</span>
            <span class="toc-name">视频会议指南</span>
          </div>
          <div class="toc-item">
            <span class="material-symbols-outlined">check_circle</span>
            <span class="toc-name">入职检查清单</span>
          </div>
        </nav>

        <div class="sidebar-footer">
          <div class="toc-item">
            <span class="material-symbols-outlined">settings</span>
            <span class="toc-name">Settings</span>
          </div>
          <div class="toc-item">
            <span class="material-symbols-outlined">help</span>
            <span class="toc-name">Help</span>
          </div>
        </div>
      </aside>

      <!-- Main Content -->
      <main class="knowledge-main">
        <!-- Sticky Action Bar -->
        <div class="action-bar">
          <nav class="breadcrumb">
            <span class="breadcrumb-link">知识库</span>
            <span class="material-symbols-outlined breadcrumb-sep">chevron_right</span>
            <span class="breadcrumb-link">企业通用规范</span>
            <span class="material-symbols-outlined breadcrumb-sep">chevron_right</span>
            <span class="breadcrumb-current">品牌视觉识别系统</span>
          </nav>
          <div class="action-buttons">
            <button class="action-btn">
              <span class="material-symbols-outlined">edit</span>
              <span>编辑</span>
            </button>
            <button class="action-btn">
              <span class="material-symbols-outlined">star</span>
              <span>收藏</span>
            </button>
            <button class="action-btn">
              <span class="material-symbols-outlined">ios_share</span>
              <span>导出</span>
            </button>
          </div>
        </div>

        <!-- Article Content -->
        <article class="article-area">
          <header class="article-header">
            <h1 class="article-title">品牌视觉识别系统 (VI) 设计规范指南 2024</h1>
            <div class="article-meta">
              <div class="meta-item">
                <div class="meta-avatar" />
                <span>Sarah Chen</span>
              </div>
              <div class="meta-item">
                <span class="material-symbols-outlined">calendar_today</span>
                <span>最后更新于 2024年5月12日</span>
              </div>
              <div class="meta-item">
                <span class="material-symbols-outlined">visibility</span>
                <span>1,240 次阅读</span>
              </div>
            </div>
            <div class="article-tags">
              <span class="tag tag-primary">品牌设计</span>
              <span class="tag">2024核心资产</span>
              <span class="tag">设计规范</span>
            </div>
          </header>

          <div class="article-body" v-loading="loading">
            <section v-for="(sec, i) in sections" :key="i" :id="'sec-' + i">
              <h2 class="section-title">{{ sec.title }}</h2>
              <p class="section-content">{{ sec.content }}</p>
            </section>
          </div>

          <!-- Contributors Footer -->
          <footer class="article-footer">
            <h4 class="footer-label">贡献者</h4>
            <div class="footer-row">
              <div class="footer-avatars">
                <div class="footer-avatar" v-for="i in 3" :key="i" />
              </div>
              <div class="footer-info">
                <p class="footer-team">设计中心 & 品牌组</p>
                <p class="footer-edit">最近由 Michael Zhang 在 2天前编辑</p>
              </div>
            </div>
          </footer>
        </article>
      </main>

      <!-- Right Sidebar -->
      <aside class="knowledge-toc">
        <div class="rtoc-section">
          <h4 class="rtoc-label">目录索引</h4>
          <nav class="rtoc-nav">
            <a class="rtoc-link active" href="#sec-0">1. 标志使用准则</a>
            <a class="rtoc-link" href="#sec-1">2. 标准色彩系统</a>
            <a class="rtoc-link" href="#sec-2">3. 协作光标规范</a>
            <a class="rtoc-link" href="#sec-3">4. 字体排版指南</a>
          </nav>
        </div>
        <div class="rtoc-section">
          <h4 class="rtoc-label">相关条目</h4>
          <div class="related-list">
            <div class="related-item">
              <h5 class="related-title">营销物料模板集</h5>
              <p class="related-sub">知识库 · 资产</p>
            </div>
            <div class="related-item">
              <h5 class="related-title">社交媒体品牌规范</h5>
              <p class="related-sub">知识库 · 渠道</p>
            </div>
            <div class="related-item">
              <h5 class="related-title">UI 图标系统 (v2.0)</h5>
              <p class="related-sub">设计系统 · 开发</p>
            </div>
          </div>
        </div>
        <div class="rtoc-info-card">
          <div class="info-card-header">
            <span class="material-symbols-outlined">info</span>
            <span class="info-card-title">条目信息</span>
          </div>
          <div class="info-card-rows">
            <div class="info-row">
              <span class="info-key">创建于:</span>
              <span class="info-val">2023-10-24</span>
            </div>
            <div class="info-row">
              <span class="info-key">状态:</span>
              <span class="info-val info-status">已发布</span>
            </div>
            <div class="info-row">
              <span class="info-key">权限:</span>
              <span class="info-val">全公司公开</span>
            </div>
          </div>
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import request from '@/utils/request'

const route = useRoute()
const itemId = route.params.itemId as string
const activeSection = ref(1)
const loading = ref(false)

const article = ref<any>({})
const toc = ref<any[]>([])
const sections = ref<any[]>([])

onMounted(() => {
  if (itemId) {
    loading.value = true
    request.get(`/knowledge/${itemId}`).then((res: any) => {
      if (res.code === 200 && res.data) {
        const d = res.data
        article.value = d
        // Build sections from content
        if (d.content) {
          const parts = d.content.split(/\n(?=##?\s)/)
          sections.value = parts.map((p: string, i: number) => {
            const match = p.match(/^(##?\s+)(.+)/)
            return { title: match ? match[2] : `章节 ${i + 1}`, content: p.replace(/^##?\s+.+\n?/, '') }
          })
        }
      }
    }).catch(() => {}).finally(() => loading.value = false)
  }
})

</script>

<style lang="scss" scoped>
/* ===== CSS Variables ===== */
$c-bg: #f8f9fa;
$c-surface: #f8f9fa;
$c-surface-low: #f3f4f5;
$c-surface-lowest: #ffffff;
$c-surface-high: #e7e8e9;
$c-surface-dim: #d9dadb;
$c-border: #E5E6EB;
$c-border-variant: #c3c6d8;
$c-primary: #0050d6;
$c-primary-container: #2a6af9;
$c-on-primary: #ffffff;
$c-text: #191c1d;
$c-text-secondary: #5b5e65;
$c-text-muted: #424655;
$c-text-ghost: #737687;
$c-selection: rgba(51, 112, 255, 0.1);
$c-presence: #22C55E;
$c-error: #ba1a1a;
$c-inverse-surface: #2e3132;
$c-inverse-on-surface: #f0f1f2;
$c-secondary-container: #dfe2eb;
$c-primary-fixed: #dbe1ff;
$radius-sm: 4px;
$radius-md: 8px;
$radius-lg: 12px;
$radius-xl: 16px;
$sidebar-width: 280px;

.knowledge-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: $c-bg;
  color: $c-text;
  font-family: 'Inter Variable', sans-serif;
  overflow: hidden;
}

/* ===== Top Header ===== */
.top-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  height: 64px;
  background: $c-surface;
  border-bottom: 1px solid $c-border;
  flex-shrink: 0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.header-logo {
  font-size: 20px;
  line-height: 28px;
  font-weight: 600;
  color: $c-primary;
}

.search-box {
  display: flex;
  align-items: center;
  background: $c-surface-high;
  padding: 6px 12px;
  border-radius: $radius-md;
  width: 320px;
}

.search-icon {
  color: $c-text-muted;
  font-size: 18px;
  margin-right: 8px;
}

.search-input {
  flex: 1;
  background: transparent;
  border: none;
  outline: none;
  font-size: 14px;
  line-height: 22px;
  color: $c-text;

  &::placeholder { color: $c-text-ghost; }
}

.header-right {
  display: flex;
  align-items: center;
  gap: 24px;
}

.header-icons {
  display: flex;
  gap: 16px;
  color: $c-text-secondary;

  .material-symbols-outlined {
    cursor: pointer;
    transition: color 0.15s;

    &:hover { color: $c-primary; }
  }
}

.share-btn {
  background: $c-primary;
  color: $c-on-primary;
  border: none;
  padding: 6px 16px;
  border-radius: $radius-md;
  font-size: 12px;
  font-weight: 500;
  letter-spacing: 0.01em;
  cursor: pointer;
  transition: transform 0.1s;

  &:active { transform: scale(0.95); }
}

.user-avatar {
  position: relative;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: $c-border-variant;
  border: 2px solid $c-surface;
  overflow: hidden;
}

.avatar-ring {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  box-shadow: inset 0 0 0 2px $c-presence;
}

/* ===== Knowledge Body ===== */
.knowledge-body {
  flex: 1;
  display: flex;
  overflow: hidden;
}

/* ===== Left Sidebar ===== */
.knowledge-sidebar {
  display: flex;
  flex-direction: column;
  width: $sidebar-width;
  background: $c-surface-low;
  border-right: 1px solid $c-border;
  padding: 16px 8px;
  flex-shrink: 0;
  overflow-y: auto;
}

.sidebar-header {
  padding: 0 16px;
  margin-bottom: 24px;
}

.sidebar-section-label {
  font-size: 12px;
  line-height: 16px;
  font-weight: 500;
  letter-spacing: 0.01em;
  color: $c-text-secondary;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  margin-bottom: 8px;
}

.sidebar-folder {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px;
  border-radius: $radius-sm;
  cursor: pointer;
  transition: background-color 0.15s;
  font-weight: 700;

  .material-symbols-outlined { color: $c-primary; }

  &:hover { background: $c-surface-high; }
}

.sidebar-folder-name {
  font-size: 14px;
  line-height: 22px;
}

.sidebar-nav {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.toc-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 16px;
  color: $c-text-secondary;
  border-radius: $radius-sm;
  cursor: pointer;
  transition: background-color 0.15s, color 0.15s;
  font-size: 14px;
  line-height: 22px;

  &:hover { background: $c-surface-high; }

  &.active {
    background: $c-selection;
    color: $c-primary;
    border-left: 4px solid $c-primary;
    font-weight: 700;
  }
}

.toc-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.toc-count {
  font-size: 12px;
  color: $c-text-ghost;
}

.toc-sub-items {
  margin-left: 20px;
  padding-left: 16px;
  border-left: 1px solid $c-border;
  display: flex;
  flex-direction: column;
  gap: 2px;
  margin-top: 4px;
  margin-bottom: 4px;
}

.toc-sub-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 6px 16px;
  color: $c-text-secondary;
  font-size: 13px;
  cursor: pointer;
  border-radius: $radius-sm;
  transition: background-color 0.15s;

  &:hover { background: $c-surface-high; }
}

.sidebar-footer {
  margin-top: auto;
  padding-top: 16px;
  border-top: 1px solid $c-border;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

/* ===== Main Content ===== */
.knowledge-main {
  flex: 1;
  overflow-y: auto;
  background: $c-surface;
  position: relative;
}

/* Sticky Action Bar */
.action-bar {
  position: sticky;
  top: 0;
  z-index: 10;
  background: rgba($c-surface-lowest, 0.8);
  backdrop-filter: blur(8px);
  border-bottom: 1px solid $c-border;
  padding: 12px 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.breadcrumb {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  font-weight: 500;
  letter-spacing: 0.01em;
  color: $c-text-secondary;
}

.breadcrumb-link {
  color: $c-text-secondary;
  cursor: pointer;

  &:hover { color: $c-primary; }
}

.breadcrumb-sep {
  font-size: 14px;
}

.breadcrumb-current {
  color: $c-text;
}

.action-buttons {
  display: flex;
  align-items: center;
  gap: 8px;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  background: transparent;
  border: 1px solid $c-border;
  border-radius: $radius-sm;
  font-size: 12px;
  font-weight: 500;
  letter-spacing: 0.01em;
  color: $c-text-secondary;
  cursor: pointer;
  transition: background-color 0.15s, color 0.15s;

  &:hover { background: $c-surface-low; }
}

/* Article */
.article-area {
  max-width: 800px;
  margin: 0 auto;
  padding: 48px 16px;
}

.article-header {
  margin-bottom: 40px;
}

.article-title {
  font-size: 32px;
  line-height: 40px;
  font-weight: 700;
  letter-spacing: -0.02em;
  color: $c-text;
  margin-bottom: 24px;
}

.article-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 24px;
  color: $c-text-muted;
  margin-bottom: 24px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  font-weight: 500;
  letter-spacing: 0.01em;
}

.meta-avatar {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: $c-border-variant;
  flex-shrink: 0;
}

.article-tags {
  display: flex;
  gap: 8px;
}

.tag {
  font-size: 12px;
  font-weight: 500;
  letter-spacing: 0.01em;
  padding: 2px 8px;
  border-radius: $radius-sm;
  background: rgba($c-text-secondary, 0.1);
  color: $c-text-secondary;
  border: 1px solid $c-border;

  &.tag-primary {
    background: rgba($c-primary-container, 0.1);
    color: $c-primary-container;
    border-color: rgba($c-primary-container, 0.2);
  }
}

/* Article Body */
.article-body {
  display: flex;
  flex-direction: column;
  gap: 48px;
}

.section-title {
  font-size: 24px;
  line-height: 32px;
  font-weight: 600;
  letter-spacing: -0.01em;
  color: $c-text;
  margin-bottom: 16px;
  padding-bottom: 8px;
  border-bottom: 1px solid $c-border;
}

.section-content {
  font-size: 14px;
  line-height: 1.8;
  color: $c-text-secondary;
}

/* Article Footer */
.article-footer {
  margin-top: 80px;
  padding-top: 32px;
  border-top: 1px solid $c-border;
}

.footer-label {
  font-size: 12px;
  font-weight: 500;
  letter-spacing: 0.01em;
  color: $c-text-secondary;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  margin-bottom: 16px;
}

.footer-row {
  display: flex;
  gap: 16px;
}

.footer-avatars {
  display: flex;

  .footer-avatar {
    width: 40px;
    height: 40px;
    border-radius: 50%;
    background: $c-border-variant;
    border: 2px solid $c-surface;
    margin-left: -12px;

    &:first-child { margin-left: 0; }
  }
}

.footer-info {
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.footer-team {
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.01em;
}

.footer-edit {
  font-size: 12px;
  color: $c-text-secondary;
  letter-spacing: 0.01em;
}

/* ===== Right Sidebar ===== */
.knowledge-toc {
  display: none;
  width: 288px;
  background: $c-surface;
  border-left: 1px solid $c-border;
  padding: 16px;
  overflow-y: auto;
  flex-shrink: 0;

  @media (min-width: 1280px) {
    display: block;
  }
}

.rtoc-section {
  margin-bottom: 32px;
}

.rtoc-label {
  font-size: 12px;
  font-weight: 500;
  letter-spacing: 0.01em;
  color: $c-text-secondary;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  margin-bottom: 16px;
}

.rtoc-nav {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.rtoc-link {
  display: block;
  font-size: 14px;
  line-height: 22px;
  color: $c-text-muted;
  text-decoration: none;
  padding-left: 12px;
  border-left: 2px solid transparent;
  transition: color 0.15s, border-color 0.15s;

  &:hover { color: $c-primary; }

  &.active {
    color: $c-primary;
    font-weight: 700;
    border-left-color: $c-primary;
  }
}

.related-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.related-item {
  cursor: pointer;
}

.related-title {
  font-size: 14px;
  font-weight: 700;
  margin: 0 0 2px;
  transition: color 0.15s;

  .related-item:hover & { color: $c-primary; }
}

.related-sub {
  font-size: 12px;
  color: $c-text-secondary;
  letter-spacing: 0.01em;
  margin: 0;
}

.rtoc-info-card {
  padding: 16px;
  background: $c-surface-low;
  border-radius: $radius-xl;
  border: 1px solid $c-border;
}

.info-card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;

  .material-symbols-outlined {
    color: $c-primary;
    font-size: 18px;
  }
}

.info-card-title {
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.01em;
}

.info-card-rows {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.info-row {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  letter-spacing: 0.01em;
}

.info-key {
  color: $c-text-secondary;
}

.info-val {
  color: $c-text;
}

.info-status {
  color: $c-presence;
  font-weight: 700;
}
</style>
