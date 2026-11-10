<template>
  <div class="auth-page">
    <!-- Top Header Bar -->
    <header class="top-header">
      <span class="brand">Nexus Office</span>
      <button class="help-btn">帮助中心</button>
    </header>

    <!-- Background Decoration -->
    <div class="bg-decor">
      <div class="bg-blur bg-blur-1" />
      <div class="bg-blur bg-blur-2" />
    </div>

    <!-- Main Content -->
    <main class="auth-main">
      <div class="auth-card">
        <!-- Success Icon -->
        <div class="success-icon-circle">
          <span
            class="material-symbols-outlined success-icon-inner"
            style="font-variation-settings: 'FILL' 1;"
          >check_circle</span>
        </div>

        <!-- Title & Description -->
        <h1 class="card-title">密码重置成功</h1>
        <p class="card-desc">您的新密码已生效，请使用新密码重新登录您的账户。</p>

        <!-- Action Button -->
        <button class="login-btn" @click="goLogin">
          立即登录
          <span class="material-symbols-outlined login-btn-icon">arrow_forward</span>
        </button>

        <!-- Countdown Timer -->
        <div class="countdown-section">
          <p class="countdown-text">
            <span class="countdown-num">{{ countdown }}</span> 秒后自动跳转至登录页
          </p>
          <div class="progress-bar">
            <div
              class="progress-bar-fill"
              :style="{ width: (countdown / 3 * 100) + '%' }"
            />
          </div>
        </div>

        <!-- Support Footer -->
        <div class="support-footer">
          <p>遇到问题？<a href="#" class="support-link">联系技术支持</a></p>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const countdown = ref(3)
let timer: number | undefined

onMounted(() => {
  timer = window.setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) goLogin()
  }, 1000)
})

onUnmounted(() => {
  if (timer) window.clearInterval(timer)
})

function goLogin() {
  if (timer) window.clearInterval(timer)
  router.push('/login')
}
</script>

<style lang="scss" scoped>
// === Page Layout ===
.auth-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--c-bg);
  position: relative;
  font-family: var(--font-sans);
}

// === Top Header Bar ===
.top-header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 50;
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 var(--gutter);
  background: var(--c-surface);
  border-bottom: 1px solid var(--c-border);

  .brand {
    font-size: 20px;
    font-weight: 700;
    line-height: 28px;
    color: var(--c-brand);
  }

  .help-btn {
    font-size: 12px;
    font-weight: 500;
    line-height: 16px;
    letter-spacing: 0.01em;
    color: var(--c-text-secondary);
    background: none;
    border: none;
    cursor: pointer;
    padding: 8px 12px;
    border-radius: var(--radius);
    transition: color 0.2s var(--ease-out);

    &:hover {
      color: var(--c-brand);
    }
  }
}

// === Background Decoration ===
.bg-decor {
  position: fixed;
  inset: 0;
  z-index: -10;
  overflow: hidden;
  pointer-events: none;
  opacity: 0.2;
}

.bg-blur {
  position: absolute;
  border-radius: 50%;
}

.bg-blur-1 {
  width: 400px;
  height: 400px;
  top: -10%;
  right: -5%;
  background: var(--c-brand);
  filter: blur(120px);
}

.bg-blur-2 {
  width: 300px;
  height: 300px;
  bottom: -10%;
  left: -5%;
  background: #dfe2eb;
  filter: blur(100px);
}

// === Main Content Area ===
.auth-main {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--gutter);
  margin-top: 64px; // offset for fixed header
}

// === Card ===
.auth-card {
  width: 100%;
  max-width: 480px;
  padding: 40px;
  background: var(--c-surface);
  border: 1px solid var(--c-border);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-sm);
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;

  // Fade-in scale animation
  animation: card-scale-in 0.5s cubic-bezier(0.16, 1, 0.3, 1) forwards;
}

@keyframes card-scale-in {
  0% {
    transform: scale(0.9);
    opacity: 0;
  }
  100% {
    transform: scale(1);
    opacity: 1;
  }
}

// === Success Icon ===
.success-icon-circle {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: rgba(34, 197, 94, 0.1); // --c-online at 10%
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 24px;
}

.success-icon-inner {
  font-size: 48px;
  color: var(--c-online);
}

// === Title & Description ===
.card-title {
  font-size: 32px;
  font-weight: 700;
  line-height: 40px;
  letter-spacing: -0.02em;
  color: var(--c-text);
  margin-bottom: 8px;
}

.card-desc {
  font-size: 16px;
  font-weight: 400;
  line-height: 24px;
  color: var(--c-text-secondary);
  margin-bottom: 32px;
}

// === Login Button ===
.login-btn {
  width: 100%;
  padding: 16px 0;
  background: var(--c-brand);
  color: #fff;
  border: none;
  border-radius: var(--radius-lg);
  font-size: 16px;
  font-weight: 600;
  line-height: 24px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transition: all 0.2s var(--ease-out);

  &:hover {
    opacity: 0.9;
    transform: translateY(-1px);
    box-shadow: var(--shadow-sm);
  }

  &:active {
    transform: scale(0.98);
  }
}

.login-btn-icon {
  font-size: 20px;
}

// === Countdown Section ===
.countdown-section {
  margin-top: 32px;
  width: 100%;
}

.countdown-text {
  font-size: 12px;
  font-weight: 500;
  line-height: 16px;
  letter-spacing: 0.01em;
  color: var(--c-text-secondary);
}

.countdown-num {
  display: inline;
}

// === Progress Bar ===
.progress-bar {
  height: 4px;
  background: var(--c-border);
  border-radius: 2px;
  overflow: hidden;
  width: 100%;
  margin-top: 24px;
}

.progress-bar-fill {
  height: 100%;
  background: var(--c-brand);
  border-radius: 2px;
  transition: width 1s linear;
}

// === Support Footer ===
.support-footer {
  margin-top: 48px;
  padding-top: 32px;
  border-top: 1px solid var(--c-border);
  width: 100%;

  p {
    font-size: 12px;
    font-weight: 500;
    line-height: 16px;
    letter-spacing: 0.01em;
    color: var(--c-text-secondary);
  }
}

.support-link {
  color: var(--c-brand);
  text-decoration: none;

  &:hover {
    text-decoration: underline;
  }
}
</style>
