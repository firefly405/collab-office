<template>
  <div class="wizard-root">
    <!-- Top Navigation Bar -->
    <header class="top-bar">
      <span class="brand">Nexus Office</span>
      <div class="header-right">
        <button class="logout-btn" @click="$router.push('/login')">退出登录</button>
        <div class="avatar">U</div>
      </div>
    </header>

    <!-- Main Content -->
    <main class="main-area">
      <!-- Step 1: Welcome / Choose -->
      <div v-if="choice === ''" class="step-view step-welcome">
        <div class="step-header">
          <h1>注册成功！开启您的协作空间</h1>
          <p class="subtitle">选择一个方式开始高效工作</p>
        </div>
        <div class="card-grid">
          <!-- Create Space Card -->
          <div class="option-card" @click="choice = 'create'">
            <div class="card-icon-box card-icon-box--primary">
              <span class="material-symbols-outlined">add_business</span>
            </div>
            <h2>创建新空间</h2>
            <p>为您的团队、部门或公司打造一个独立的数字办公场所，自定义一切规则。</p>
            <div class="card-link">
              立即创建 <span class="material-symbols-outlined">arrow_forward</span>
            </div>
          </div>
          <!-- Join Space Card -->
          <div class="option-card" @click="choice = 'join'">
            <div class="card-icon-box card-icon-box--secondary">
              <span class="material-symbols-outlined">group_add</span>
            </div>
            <h2>加入现有空间</h2>
            <p>通过空间 ID、团队邀请码或扫描二维码，立即加入已经存在的协作流程。</p>
            <div class="card-link">
              输入代码 <span class="material-symbols-outlined">arrow_forward</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Step 2: Create Space Form -->
      <div v-if="choice === 'create'" class="step-view step-form">
        <button class="back-btn" @click="choice = ''">
          <span class="material-symbols-outlined">arrow_back</span> 返回选择
        </button>
        <div class="form-card">
          <h2>设置空间详情</h2>
          <p class="form-desc">这些信息将帮助我们为您配置最合适的协作模板。</p>
          <div class="form-fields">
            <div class="field">
              <label>空间名称</label>
              <input
                v-model="spaceName"
                type="text"
                placeholder="例如：Nexus 核心研发团队"
              />
            </div>
            <div class="field-row">
              <div class="field">
                <label>所属行业</label>
                <select v-model="industry">
                  <option value="">选择行业</option>
                  <option value="tech">互联网/技术</option>
                  <option value="finance">金融/保险</option>
                  <option value="edu">教育/咨询</option>
                  <option value="design">创意/设计</option>
                  <option value="manufacture">制造/贸易</option>
                </select>
              </div>
              <div class="field">
                <label>团队规模</label>
                <select v-model="teamSize">
                  <option value="">选择规模</option>
                  <option value="1-10">1-10 人</option>
                  <option value="11-50">11-50 人</option>
                  <option value="51-200">51-200 人</option>
                  <option value="200+">200+ 人</option>
                </select>
              </div>
            </div>
            <button
              class="submit-btn"
              :disabled="loading"
              @click="submit"
            >
              {{ loading ? '创建中...' : '完成创建并进入' }}
            </button>
          </div>
        </div>
      </div>

      <!-- Step 3: Join Space Form -->
      <div v-if="choice === 'join'" class="step-view step-form">
        <button class="back-btn" @click="choice = ''">
          <span class="material-symbols-outlined">arrow_back</span> 返回选择
        </button>
        <div class="form-card">
          <h2>加入已有空间</h2>
          <p class="form-desc">请输入管理员分享给您的 8 位邀请码或空间 ID。</p>
          <div class="form-fields">
            <div class="field">
              <label>输入邀请代码</label>
              <div class="invite-row">
                <input
                  v-model="inviteCode"
                  type="text"
                  maxlength="8"
                  placeholder="NX-88888"
                  class="invite-input"
                />
                <button class="verify-btn" @click="submit">验证</button>
              </div>
            </div>
            <div class="or-divider">
              <span>或者</span>
            </div>
            <div class="qr-area">
              <div class="qr-box">
                <span class="material-symbols-outlined qr-icon">qr_code_scanner</span>
                <div class="qr-overlay">
                  <span>点击上传二维码</span>
                </div>
              </div>
              <p class="qr-hint">扫描二维码名片或上传截图</p>
            </div>
          </div>
        </div>
      </div>
    </main>

    <!-- Background Decorative Blobs -->
    <div class="blob blob--tr"></div>
    <div class="blob blob--bl"></div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { createSpace } from '@/api/auth'

const router = useRouter()
const choice = ref<'create' | 'join' | ''>('')
const spaceName = ref('')
const industry = ref('')
const teamSize = ref('')
const inviteCode = ref('')
const loading = ref(false)

function submit() {
  loading.value = true
  if (choice.value === 'create' && spaceName.value.trim()) {
    createSpace(spaceName.value).then((res: any) => {
      if (res.code === 200) {
        ElMessage.success('空间已创建')
        router.push('/dashboard')
      }
    }).catch(() => ElMessage.error('创建失败')).finally(() => loading.value = false)
  } else if (choice.value === 'join' && inviteCode.value.trim()) {
    // Join by invite code - can extend with joinSpace API
    ElMessage.success('已加入空间')
    router.push('/dashboard')
    loading.value = false
  } else {
    ElMessage.warning('请选择操作并填写信息')
    loading.value = false
  }
}
</script>

<style lang="scss" scoped>
/* ------------------------------------------------------------------ */
/* Material Symbols font (ensure loaded globally in your app)          */
/* ------------------------------------------------------------------ */
.material-symbols-outlined {
  font-variation-settings: 'FILL' 0, 'wght' 400, 'GRAD' 0, 'opsz' 24;
  vertical-align: middle;
  user-select: none;
}

/* ------------------------------------------------------------------ */
/* Root: dot-grid canvas                                               */
/* ------------------------------------------------------------------ */
.wizard-root {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: #f8f9fa;
  background-image: radial-gradient(#e5e7eb 1px, transparent 1px);
  background-size: 24px 24px;
  position: relative;
  overflow: hidden;
  font-family: var(--font-sans, 'Inter Variable', system-ui, -apple-system, sans-serif);
  color: var(--c-text, #191c1d);
}

/* ------------------------------------------------------------------ */
/* Top bar                                                             */
/* ------------------------------------------------------------------ */
.top-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 64px;
  padding: 0 2rem;
  background: #fff;
  border-bottom: 1px solid #e5e6eb;
  flex-shrink: 0;
  position: relative;
  z-index: 2;
}

.brand {
  font-size: 20px;
  font-weight: 600;
  color: var(--c-brand, #0050d6);
  letter-spacing: -0.01em;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.logout-btn {
  background: none;
  border: none;
  padding: 0;
  color: var(--c-text-secondary, #5b5e65);
  font-size: 12px;
  font-weight: 500;
  letter-spacing: 0.01em;
  cursor: pointer;
  transition: color 0.15s var(--ease-out, ease);
  font-family: inherit;

  &:hover {
    color: var(--c-brand, #0050d6);
  }
}

.avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #dbe1ff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 14px;
  color: #00174b;
  flex-shrink: 0;
}

/* ------------------------------------------------------------------ */
/* Main area                                                           */
/* ------------------------------------------------------------------ */
.main-area {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1rem;
  position: relative;
  z-index: 1;
}

/* ------------------------------------------------------------------ */
/* Step views                                                          */
/* ------------------------------------------------------------------ */
.step-view {
  width: 100%;
}

.step-welcome {
  max-width: 900px;
}

.step-form {
  max-width: 580px;
}

/* ------------------------------------------------------------------ */
/* Welcome header                                                      */
/* ------------------------------------------------------------------ */
.step-header {
  text-align: center;
  margin-bottom: 48px;

  h1 {
    font-size: 32px;
    font-weight: 700;
    line-height: 40px;
    letter-spacing: -0.02em;
    color: var(--c-text, #191c1d);
    margin: 0 0 8px;
  }

  .subtitle {
    font-size: 16px;
    line-height: 24px;
    color: var(--c-text-secondary, #5b5e65);
    margin: 0;
  }
}

/* ------------------------------------------------------------------ */
/* Card grid                                                           */
/* ------------------------------------------------------------------ */
.card-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 32px;
  margin: 0 auto;

  @media (max-width: 768px) {
    grid-template-columns: 1fr;
    gap: 16px;
  }
}

/* ------------------------------------------------------------------ */
/* Option cards                                                        */
/* ------------------------------------------------------------------ */
.option-card {
  background: #fff;
  border: 1px solid var(--c-border, #e5e6eb);
  border-radius: var(--radius-xl, 0.5rem);
  padding: 32px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);

  &:hover {
    transform: translateY(-4px);
    border-color: var(--c-brand, #0050d6);
    box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1),
                0 8px 10px -6px rgba(0, 0, 0, 0.1);
  }

  h2 {
    font-size: 24px;
    font-weight: 600;
    line-height: 32px;
    letter-spacing: -0.01em;
    color: var(--c-text, #191c1d);
    margin: 0 0 16px;
  }

  p {
    font-size: 14px;
    line-height: 22px;
    color: var(--c-text-secondary, #5b5e65);
    margin: 0 0 32px;
  }
}

/* Icon box inside cards */
.card-icon-box {
  width: 64px;
  height: 64px;
  border-radius: var(--radius-lg, 0.25rem);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 24px;
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);

  .option-card:hover & {
    transform: scale(1.1);
  }

  .material-symbols-outlined {
    font-size: 36px;
  }
}

.card-icon-box--primary {
  background: var(--c-brand-light, rgba(0, 80, 214, 0.12));

  .material-symbols-outlined {
    color: var(--c-brand, #0050d6);
  }
}

.card-icon-box--secondary {
  background: #dfe2eb;

  .material-symbols-outlined {
    color: #61646c;
  }
}

/* Card action link */
.card-link {
  display: inline-flex;
  align-items: center;
  color: var(--c-brand, #0050d6);
  font-weight: 700;
  font-size: 14px;
  gap: 8px;

  .material-symbols-outlined {
    font-size: 20px;
  }
}

/* ------------------------------------------------------------------ */
/* Back button                                                         */
/* ------------------------------------------------------------------ */
.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  background: none;
  border: none;
  padding: 0;
  color: var(--c-text-secondary, #5b5e65);
  font-size: 14px;
  font-family: inherit;
  cursor: pointer;
  margin-bottom: 32px;
  transition: color 0.15s var(--ease-out, ease);

  &:hover {
    color: var(--c-brand, #0050d6);
  }

  .material-symbols-outlined {
    font-size: 20px;
  }
}

/* ------------------------------------------------------------------ */
/* Form card                                                           */
/* ------------------------------------------------------------------ */
.form-card {
  background: #fff;
  border: 1px solid var(--c-border, #e5e6eb);
  border-radius: var(--radius-xl, 0.5rem);
  padding: 40px;
  box-shadow: var(--shadow-sm, 0 1px 2px rgba(0, 0, 0, 0.05));

  h2 {
    font-size: 24px;
    font-weight: 600;
    line-height: 32px;
    letter-spacing: -0.01em;
    color: var(--c-text, #191c1d);
    margin: 0 0 8px;
  }
}

.form-desc {
  font-size: 14px;
  line-height: 22px;
  color: var(--c-text-secondary, #5b5e65);
  margin: 0 0 32px;
}

.form-fields {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

/* ------------------------------------------------------------------ */
/* Form fields                                                         */
/* ------------------------------------------------------------------ */
.field {
  display: flex;
  flex-direction: column;
  text-align: left;

  label {
    font-size: 12px;
    font-weight: 500;
    letter-spacing: 0.01em;
    color: var(--c-text-muted, #424655);
    margin-bottom: 8px;
  }

  input,
  select {
    width: 100%;
    padding: 12px 16px;
    background: var(--c-surface-dim, #f3f4f5);
    border: 1px solid var(--c-border, #e5e6eb);
    border-radius: var(--radius-lg, 0.25rem);
    font-size: 14px;
    font-family: inherit;
    color: var(--c-text, #191c1d);
    outline: none;
    transition: border-color 0.15s var(--ease-out, ease),
                box-shadow 0.15s var(--ease-out, ease);
    box-sizing: border-box;

    &:focus {
      border-color: var(--c-brand, #0050d6);
      box-shadow: 0 0 0 2px rgba(0, 80, 214, 0.2);
    }

    &::placeholder {
      color: var(--c-text-muted, #737687);
    }
  }

  select {
    appearance: none;
    background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 12 12'%3E%3Cpath fill='%23737687' d='M6 8L1 3h10z'/%3E%3C/svg%3E");
    background-repeat: no-repeat;
    background-position: right 12px center;
    padding-right: 36px;
    cursor: pointer;
  }
}

/* Two-column row inside form */
.field-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;

  @media (max-width: 480px) {
    grid-template-columns: 1fr;
  }
}

/* ------------------------------------------------------------------ */
/* Invite code input                                                   */
/* ------------------------------------------------------------------ */
.invite-row {
  display: flex;
  gap: 8px;
}

.invite-input {
  flex: 1;
  font-family: var(--font-mono, 'JetBrains Mono Variable', monospace) !important;
  text-align: center;
  font-size: 18px !important;
  letter-spacing: 0.1em;
  text-transform: uppercase;

  &::placeholder {
    text-transform: none;
    letter-spacing: normal;
    font-size: 14px !important;
  }
}

.verify-btn {
  padding: 0 24px;
  background: var(--c-brand, #0050d6);
  color: #fff;
  font-weight: 700;
  font-size: 14px;
  font-family: inherit;
  border: none;
  border-radius: var(--radius-lg, 0.25rem);
  cursor: pointer;
  transition: background-color 0.15s var(--ease-out, ease);
  white-space: nowrap;
  flex-shrink: 0;

  &:hover {
    background: #0053db;
  }

  &:active {
    transform: scale(0.98);
  }
}

/* ------------------------------------------------------------------ */
/* OR divider                                                          */
/* ------------------------------------------------------------------ */
.or-divider {
  display: flex;
  align-items: center;

  &::before {
    content: '';
    flex: 1;
    border-top: 1px solid var(--c-border, #e5e6eb);
  }

  span {
    padding: 0 16px;
    background: #fff;
    color: var(--c-text-secondary, #5b5e65);
    font-size: 14px;
  }
}

/* ------------------------------------------------------------------ */
/* QR code area                                                        */
/* ------------------------------------------------------------------ */
.qr-area {
  text-align: center;
}

.qr-box {
  width: 192px;
  height: 192px;
  margin: 0 auto 16px;
  background: var(--c-surface-dim, #f3f4f5);
  border: 1px solid var(--c-border, #e5e6eb);
  border-radius: var(--radius-xl, 0.5rem);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
}

.qr-icon {
  font-size: 48px;
  color: #c3c6d8;
}

.qr-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 80, 214, 0.1);
  opacity: 0;
  transition: opacity 0.15s var(--ease-out, ease);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;

  &:hover {
    opacity: 1;
  }

  span {
    color: var(--c-brand, #0050d6);
    font-weight: 700;
    font-size: 14px;
  }
}

.qr-hint {
  color: var(--c-text-secondary, #5b5e65);
  font-size: 14px;
  margin: 0;
}

/* ------------------------------------------------------------------ */
/* Submit button                                                       */
/* ------------------------------------------------------------------ */
.submit-btn {
  width: 100%;
  padding: 16px;
  background: var(--c-brand, #0050d6);
  color: #fff;
  font-weight: 700;
  font-size: 16px;
  font-family: inherit;
  border: none;
  border-radius: var(--radius-lg, 0.25rem);
  cursor: pointer;
  transition: background-color 0.15s var(--ease-out, ease),
              transform 0.15s var(--ease-out, ease);
  margin-top: 8px;

  &:hover {
    background: #0053db;
  }

  &:active {
    transform: scale(0.98);
  }

  &:disabled {
    opacity: 0.6;
    cursor: not-allowed;
    transform: none;
  }
}

/* ------------------------------------------------------------------ */
/* Background decorative blobs                                         */
/* ------------------------------------------------------------------ */
.blob {
  position: fixed;
  border-radius: 50%;
  z-index: 0;
  pointer-events: none;
}

.blob--tr {
  top: 80px;
  right: -10%;
  width: 40%;
  height: 40%;
  background: rgba(0, 80, 214, 0.05);
  filter: blur(100px);
}

.blob--bl {
  bottom: -10%;
  left: -5%;
  width: 30%;
  height: 30%;
  background: rgba(223, 226, 235, 0.1);
  filter: blur(80px);
}
</style>
