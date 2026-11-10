<template>
  <div class="forget-password-page">
    <!-- Background Decorations -->
    <div class="bg-decor">
      <div class="bg-blur bg-blur-1" />
      <div class="bg-blur bg-blur-2" />
      <div class="bg-blur bg-blur-3" />
    </div>

    <!-- Top Header Bar -->
    <header class="top-bar">
      <div class="top-bar-inner">
        <span class="brand-name">Nexus Office</span>
        <div class="top-bar-actions">
          <router-link to="/login" class="top-bar-link">返回登录</router-link>
        </div>
      </div>
    </header>

    <!-- Main Content -->
    <main class="main-content">
      <div class="card-wrapper">

        <!-- ==================== Step 0: Verify Identity ==================== -->
        <template v-if="step === 0">
          <!-- Circle Stepper -->
          <div class="stepper-circles">
            <div class="stepper-track">
              <div class="stepper-line-bg" />
              <div class="stepper-line-active" style="width: 0%;" />
            </div>
            <div class="stepper-step" v-for="(s, i) in stepperSteps" :key="i"
                 :class="{ active: i === 0 }">
              <div class="step-circle">{{ i + 1 }}</div>
              <span class="step-label">{{ s }}</span>
            </div>
          </div>

          <!-- Card -->
          <div class="card">
            <div class="card-header-text">
              <h2 class="card-title">找回您的密码</h2>
              <p class="card-desc">请输入您注册时使用的手机号或邮箱地址，我们将为您发送验证码。</p>
            </div>

            <form class="card-form" @submit.prevent="handleVerify">
              <!-- Identifier -->
              <div class="field">
                <label class="field-label" for="identifier">手机号 / 邮箱</label>
                <div class="input-wrap">
                  <span class="material-symbols-outlined input-icon">person</span>
                  <input
                    id="identifier"
                    v-model="verifyForm.phone"
                    class="native-input"
                    type="text"
                    placeholder="请输入手机号"
                  />
                </div>
              </div>

              <!-- SMS Code -->
              <div class="field">
                <label class="field-label" for="smsCode">验证码</label>
                <div class="input-row">
                  <div class="input-wrap input-wrap-grow">
                    <span class="material-symbols-outlined input-icon">verified_user</span>
                    <input
                      id="smsCode"
                      v-model="verifyForm.smsCode"
                      class="native-input"
                      type="text"
                      placeholder="6位验证码"
                    />
                  </div>
                  <button
                    type="button"
                    class="sms-btn"
                    :disabled="countdown > 0"
                    @click="sendSms"
                  >
                    {{ countdown > 0 ? `${countdown}s 后重试` : '获取验证码' }}
                  </button>
                </div>
              </div>

              <!-- Submit -->
              <button
                type="submit"
                class="submit-btn"
                :disabled="loading"
              >
                <template v-if="loading">
                  <span class="btn-spinner" />
                  正在验证...
                </template>
                <template v-else>下一步</template>
              </button>
            </form>

            <!-- Card Footer -->
            <div class="card-footer-divider" />
            <div class="card-footer-link">
              遇到问题？ <a href="#" class="support-link">联系技术支持</a>
            </div>
          </div>

          <!-- Social Proof -->
          <div class="social-proof">
            <div class="social-avatars">
              <div class="social-avatar" v-for="c in ['#3370FF','#22C55E','#FF8F1F']" :key="c"
                   :style="{ background: c }" />
              <div class="social-avatar-count">+12k</div>
            </div>
            <div class="social-text">
              <span class="social-text-primary">全球超过 12,000+ 团队</span>
              <span class="social-text-secondary">选择 Nexus Office 协同办公</span>
            </div>
          </div>
        </template>

        <!-- ==================== Step 1: Reset Password ==================== -->
        <template v-if="step === 1">
          <!-- Progress Bar Stepper -->
          <div class="stepper-bar">
            <div class="stepper-bar-labels">
              <span class="bar-label done">
                <span class="material-symbols-outlined label-icon">check_circle</span>
                身份验证
              </span>
              <span class="bar-label active">重置密码</span>
              <span class="bar-label dimmed">完成</span>
            </div>
            <div class="stepper-bar-track">
              <div class="stepper-bar-fill" style="width: 66%;" />
            </div>
          </div>

          <!-- Card -->
          <div class="card">
            <div class="card-header-text card-header-center">
              <h2 class="card-title">设置新密码</h2>
              <p class="card-desc">请为您的账户创建一个新的、安全性高的密码。</p>
            </div>

            <form class="card-form" @submit.prevent="handleReset">
              <!-- New Password -->
              <div class="field">
                <label class="field-label field-label-row" for="newPassword">
                  新密码
                  <button type="button" class="toggle-password-btn" @click="togglePasswordVisibility">
                    <span class="material-symbols-outlined toggle-icon">
                      {{ showPassword ? 'visibility_off' : 'visibility' }}
                    </span>
                    显示
                  </button>
                </label>
                <input
                  id="newPassword"
                  :type="showPassword ? 'text' : 'password'"
                  v-model="resetForm.password"
                  class="native-input"
                  placeholder="请输入 8-20 位字符"
                  @input="checkPasswordStrength"
                />

                <!-- Strength Meter -->
                <div class="strength-meter">
                  <div class="strength-bars">
                    <div
                      v-for="i in 4"
                      :key="i"
                      class="strength-bar"
                      :class="getStrengthBarClass(i)"
                    />
                  </div>
                  <p class="strength-text" :class="strengthTextClass">{{ strengthText }}</p>
                </div>

                <!-- Rules Checklist -->
                <ul class="rules-checklist">
                  <li class="rule-item" :class="{ met: lenRuleMet }">
                    <span class="material-symbols-outlined rule-icon">
                      {{ lenRuleMet ? 'check_circle' : 'circle' }}
                    </span>
                    包含 8 到 20 个字符
                  </li>
                  <li class="rule-item" :class="{ met: charRuleMet }">
                    <span class="material-symbols-outlined rule-icon">
                      {{ charRuleMet ? 'check_circle' : 'circle' }}
                    </span>
                    包含字母、数字或特殊符号
                  </li>
                </ul>
              </div>

              <!-- Confirm Password -->
              <div class="field">
                <label class="field-label" for="confirmPassword">再次确认新密码</label>
                <input
                  id="confirmPassword"
                  v-model="resetForm.confirmPassword"
                  class="native-input"
                  type="password"
                  placeholder="请再次输入新密码"
                  @input="checkMatch"
                />
                <p v-if="showMismatchError" class="mismatch-error">
                  <span class="material-symbols-outlined error-icon">error</span>
                  两次输入的密码不一致
                </p>
              </div>

              <!-- Submit -->
              <button
                type="submit"
                class="submit-btn"
                :disabled="loading || !isResetFormValid"
              >
                <template v-if="loading">
                  <span class="material-symbols-outlined btn-spin-icon">sync</span>
                  重置中...
                </template>
                <template v-else>确认重置</template>
              </button>
            </form>

            <!-- Card Footer -->
            <div class="card-footer-link card-footer-center">
              <router-link to="/login" class="back-login-link">
                <span class="material-symbols-outlined back-arrow">arrow_back</span>
                返回登录
              </router-link>
            </div>
          </div>
        </template>

        <!-- ==================== Step 2: Success ==================== -->
        <template v-if="step === 2">
          <div class="card success-card">
            <div class="success-icon-wrap">
              <span class="material-symbols-outlined success-icon">check_circle</span>
            </div>
            <h2 class="success-title">密码重置成功</h2>
            <p class="success-desc">{{ redirectCountdown }} 秒后自动跳转到登录页</p>
            <button class="submit-btn" @click="goLogin">立即登录</button>
          </div>
        </template>

      </div>
    </main>

    <!-- Footer -->
    <footer class="page-footer">
      &copy; 2024 Nexus Office Collaborative Team. 保留所有权利。
    </footer>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, computed, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { sendSmsCode } from '@/api/auth'
import request from '@/utils/request'

const router = useRouter()
const step = ref(0)
const loading = ref(false)
const countdown = ref(0)
const redirectCountdown = ref(3)
let timer: ReturnType<typeof setInterval> | null = null
let redirectTimer: number | undefined

const stepperSteps = ['身份验证', '重置密码', '完成']

const verifyForm = reactive({ phone: '', smsCode: '' })
const resetForm = reactive({ password: '', confirmPassword: '' })

// ------ Password visibility toggle ------
const showPassword = ref(false)

function togglePasswordVisibility() {
  showPassword.value = !showPassword.value
}

// ------ Password strength meter ------
const passwordStrength = ref(0) // 0=none, 1=weak, 2=medium, 3=strong, 4=excellent

const strengthText = computed(() => {
  const map = ['密码强度：未输入', '密码强度：弱', '密码强度：中', '密码强度：强', '密码强度：极佳']
  return map[passwordStrength.value] || map[0]
})

const strengthTextClass = computed(() => {
  const map = ['', 'strength-weak', 'strength-medium', 'strength-strong', 'strength-excellent']
  return map[passwordStrength.value] || ''
})

function getStrengthBarClass(index: number) {
  if (passwordStrength.value < 1 || passwordStrength.value < index) return ''
  const map = ['', 'bar-weak', 'bar-medium', 'bar-strong', 'bar-excellent']
  return map[passwordStrength.value] || ''
}

const lenRuleMet = computed(() => {
  const p = resetForm.password
  return p.length >= 8 && p.length <= 20
})

const charRuleMet = computed(() => {
  const p = resetForm.password
  return /[a-zA-Z]/.test(p) && (/\d/.test(p) || /[^a-zA-Z0-9]/.test(p))
})

const showMismatchError = ref(false)

const isResetFormValid = computed(() => {
  return (
    resetForm.password.length >= 8 &&
    resetForm.password.length <= 20 &&
    resetForm.password === resetForm.confirmPassword
  )
})

function checkPasswordStrength() {
  const val = resetForm.password
  if (!val) {
    passwordStrength.value = 0
    checkMatch()
    return
  }
  let strength = 0
  if (val.length >= 8) strength++
  if (/[a-z]/.test(val) && /\d/.test(val)) strength++
  if (/[A-Z]/.test(val)) strength++
  if (/[^a-zA-Z0-9]/.test(val)) strength++
  passwordStrength.value = Math.min(strength, 4)
  checkMatch()
}

function checkMatch() {
  showMismatchError.value =
    resetForm.confirmPassword.length > 0 &&
    resetForm.password !== resetForm.confirmPassword
}

// ------ SMS ------
function sendSms() {
  if (!verifyForm.phone) {
    ElMessage.warning('请输入手机号')
    return
  }
  sendSmsCode(verifyForm.phone)
    .then((res: any) => {
      if (res.code === 200) {
        countdown.value = 60
        timer = setInterval(() => {
          countdown.value--
          if (countdown.value <= 0 && timer) clearInterval(timer)
        }, 1000)
      } else {
        ElMessage.error(res.message || '发送失败')
      }
    })
    .catch(() => ElMessage.error('网络错误，请稍后重试'))
}

// ------ Step 0: Verify identity ------
async function handleVerify() {
  if (!verifyForm.phone) {
    ElMessage.warning('请输入手机号')
    return
  }
  if (!verifyForm.smsCode) {
    ElMessage.warning('请输入验证码')
    return
  }
  loading.value = true
  try {
    const res: any = await request.post('/auth/verify-sms-code', {
      phone: verifyForm.phone,
      code: verifyForm.smsCode,
    })
    if (res.code === 200) {
      step.value = 1
    } else {
      ElMessage.error(res.message || '验证码错误')
    }
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '验证失败')
  } finally {
    loading.value = false
  }
}

// ------ Step 1: Reset password ------
async function handleReset() {
  if (!resetForm.password) {
    ElMessage.warning('请输入新密码')
    return
  }
  if (resetForm.password.length < 8 || resetForm.password.length > 20) {
    ElMessage.warning('密码长度8-20位')
    return
  }
  if (resetForm.password !== resetForm.confirmPassword) {
    ElMessage.warning('两次密码不一致')
    return
  }
  loading.value = true
  try {
    const res: any = await request.post('/auth/reset-password', {
      phone: verifyForm.phone,
      code: verifyForm.smsCode,
      newPassword: resetForm.password,
    })
    if (res.code === 200) {
      step.value = 2
      redirectTimer = window.setInterval(() => {
        redirectCountdown.value--
        if (redirectCountdown.value <= 0) goLogin()
      }, 1000)
    } else {
      ElMessage.error(res.message || '重置失败')
    }
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '网络错误')
  } finally {
    loading.value = false
  }
}

function goLogin() {
  if (redirectTimer) window.clearInterval(redirectTimer)
  router.push('/login')
}

onUnmounted(() => {
  if (timer) clearInterval(timer)
  if (redirectTimer) window.clearInterval(redirectTimer)
})
</script>

<style lang="scss" scoped>
/* ======================== Layout ======================== */
.forget-password-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--c-bg, #F5F6F7);
  position: relative;
  font-family: var(--font-sans, 'Inter Variable', sans-serif);
}

/* ======================== Background Decorations ======================== */
.bg-decor {
  position: fixed;
  inset: 0;
  overflow: hidden;
  z-index: 0;
  pointer-events: none;
}
.bg-blur {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.03;
}
.bg-blur-1 {
  width: 400px; height: 400px;
  background: var(--c-brand, #3370FF);
  top: 10%; right: 10%;
}
.bg-blur-2 {
  width: 300px; height: 300px;
  background: var(--c-success, #22C55E);
  bottom: 5%; left: 5%;
}
.bg-blur-3 {
  width: 50%; height: 50%;
  background: rgba(51, 112, 255, .04);
  top: -25%; left: -25%;
  filter: blur(120px);
  opacity: 0.04;
}

/* ======================== Top Header Bar ======================== */
.top-bar {
  position: relative;
  z-index: 1;
  background: var(--c-surface, #FFFFFF);
  border-bottom: 1px solid var(--c-border, #E5E6EB);
  height: 64px;
  display: flex;
  align-items: center;
  padding: 0 var(--gutter, 1rem);
}
.top-bar-inner {
  width: 100%;
  max-width: var(--content-max, 1200px);
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.brand-name {
  font-size: 20px;
  font-weight: 700;
  color: var(--c-brand, #3370FF);
  letter-spacing: -0.01em;
}
.top-bar-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}
.top-bar-link {
  font-size: 12px;
  font-weight: 500;
  line-height: 16px;
  letter-spacing: 0.01em;
  color: var(--c-text-secondary, #4E5969);
  text-decoration: none;
  transition: color 0.2s var(--ease-out, ease);
  &:hover {
    color: var(--c-brand, #3370FF);
  }
}

/* ======================== Main Content Area ======================== */
.main-content {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--gutter, 1rem);
  position: relative;
  z-index: 1;
}
.card-wrapper {
  width: 100%;
  max-width: 448px;
}

/* ======================== Circle Stepper (Step 0) ======================== */
.stepper-circles {
  display: flex;
  align-items: center;
  justify-content: space-between;
  position: relative;
  margin-bottom: 48px;
}
.stepper-track {
  position: absolute;
  top: 50%;
  left: 0;
  width: 100%;
  transform: translateY(-50%);
  z-index: 0;
}
.stepper-line-bg {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 2px;
  background: var(--c-surface-dim, #E7E8E9);
  border-radius: 1px;
}
.stepper-line-active {
  position: absolute;
  top: 0;
  left: 0;
  height: 2px;
  background: var(--c-brand, #3370FF);
  border-radius: 1px;
  transition: width 0.35s var(--ease-out, ease);
}
.stepper-step {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  background: var(--c-bg, #F5F6F7);
  padding: 0 8px;
}
.step-circle {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
  background: var(--c-surface-dim, #E7E8E9);
  color: var(--c-text-secondary, #4E5969);
  transition: all 0.3s var(--ease-out, ease);
}
.stepper-step.active .step-circle {
  background: var(--c-brand, #3370FF);
  color: #fff;
}
.step-label {
  font-size: 12px;
  font-weight: 500;
  line-height: 16px;
  letter-spacing: 0.01em;
  color: var(--c-text-secondary, #4E5969);
  transition: color 0.3s var(--ease-out, ease);
}
.stepper-step.active .step-label {
  color: var(--c-brand, #3370FF);
  font-weight: 600;
}

/* ======================== Progress Bar Stepper (Step 1) ======================== */
.stepper-bar {
  margin-bottom: 40px;
}
.stepper-bar-labels {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}
.bar-label {
  font-size: 12px;
  font-weight: 500;
  line-height: 16px;
  letter-spacing: 0.01em;
  color: var(--c-text-secondary, #4E5969);
  display: flex;
  align-items: center;
  gap: 4px;
  transition: color 0.3s var(--ease-out, ease);
}
.bar-label.done {
  color: var(--c-success, #00B578);
}
.bar-label.active {
  color: var(--c-brand, #3370FF);
  font-weight: 700;
}
.bar-label.dimmed {
  opacity: 0.4;
}
.label-icon {
  font-size: 14px;
}
.stepper-bar-track {
  width: 100%;
  height: 4px;
  background: var(--c-surface-dim, #EDEEEF);
  border-radius: 9999px;
  overflow: hidden;
}
.stepper-bar-fill {
  height: 100%;
  background: var(--c-brand, #3370FF);
  border-radius: 9999px;
  transition: width 0.5s var(--ease-out, ease);
}

/* ======================== Card ======================== */
.card {
  background: var(--c-surface, #FFFFFF);
  border: 1px solid var(--c-border, #E5E6EB);
  border-radius: var(--radius-xl, 12px);
  padding: 40px;
  box-shadow: var(--shadow-sm, 0 1px 3px rgba(0,0,0,.06));
}
.card-header-text {
  margin-bottom: 32px;
}
.card-header-center {
  text-align: center;
}
.card-title {
  font-size: 24px;
  font-weight: 600;
  line-height: 32px;
  letter-spacing: -0.01em;
  color: var(--c-text, #1D2129);
  margin-bottom: 8px;
}
.card-desc {
  font-size: 14px;
  font-weight: 400;
  line-height: 22px;
  color: var(--c-text-secondary, #4E5969);
}
.card-form {
  display: flex;
  flex-direction: column;
  gap: 24px;
}
.card-footer-divider {
  margin-top: 32px;
  margin-bottom: 24px;
  border-top: 1px solid var(--c-border, #E5E6EB);
}
.card-footer-link {
  text-align: center;
  font-size: 14px;
  line-height: 22px;
  color: var(--c-text-secondary, #4E5969);
}
.support-link {
  color: var(--c-brand, #3370FF);
  text-decoration: none;
  &:hover {
    text-decoration: underline;
  }
}
.card-footer-center {
  margin-top: 32px;
}
.back-login-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 14px;
  line-height: 22px;
  color: var(--c-text-secondary, #4E5969);
  text-decoration: none;
  transition: color 0.2s var(--ease-out, ease);
  &:hover {
    color: var(--c-brand, #3370FF);
  }
}
.back-arrow {
  font-size: 16px;
  transition: transform 0.2s var(--ease-out, ease);
}
.back-login-link:hover .back-arrow {
  transform: translateX(-4px);
}

/* ======================== Form Fields ======================== */
.field {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.field-label {
  font-size: 12px;
  font-weight: 500;
  line-height: 16px;
  letter-spacing: 0.01em;
  color: var(--c-text-secondary, #4E5969);
  display: block;
}
.field-label-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.toggle-password-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  font-weight: 500;
  color: var(--c-text-secondary, #4E5969);
  background: none;
  border: none;
  cursor: pointer;
  padding: 0;
  transition: color 0.2s var(--ease-out, ease);
  &:hover {
    color: var(--c-brand, #3370FF);
  }
}
.toggle-icon {
  font-size: 16px;
}

/* ======================== Native Input ======================== */
.input-wrap {
  position: relative;
  display: flex;
  align-items: center;
}
.input-wrap-grow {
  flex: 1;
}
.input-icon {
  position: absolute;
  left: 12px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 20px;
  color: var(--c-text-muted, #86909C);
  pointer-events: none;
  z-index: 1;
}
.native-input {
  width: 100%;
  padding: 12px 16px;
  padding-left: 40px;
  font-size: 14px;
  font-weight: 400;
  line-height: 22px;
  font-family: var(--font-sans, 'Inter Variable', sans-serif);
  color: var(--c-text, #1D2129);
  background: var(--c-bg, #F5F6F7);
  border: 1px solid var(--c-border, #E5E6EB);
  border-radius: var(--radius-lg, 8px);
  outline: none;
  transition: all 0.2s var(--ease-out, ease);
  &::placeholder {
    color: var(--c-text-ghost, #C9CDD4);
  }
  &:focus {
    border-color: var(--c-brand, #3370FF);
    box-shadow: 0 0 0 3px rgba(51, 112, 255, .1);
  }
}

/* Input row (code + button) */
.input-row {
  display: flex;
  gap: 12px;
}
.sms-btn {
  padding: 12px 16px;
  font-size: 12px;
  font-weight: 500;
  line-height: 16px;
  letter-spacing: 0.01em;
  color: var(--c-text-secondary, #4E5969);
  background: var(--c-surface-dim, #EDEEEF);
  border: none;
  border-radius: var(--radius-lg, 8px);
  white-space: nowrap;
  cursor: pointer;
  transition: all 0.2s var(--ease-out, ease);
  &:hover:not(:disabled) {
    background: var(--c-border-light, #F0F1F3);
    color: var(--c-brand, #3370FF);
  }
  &:active:not(:disabled) {
    transform: scale(0.96);
  }
  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
}

/* ======================== Submit Button ======================== */
.submit-btn {
  width: 100%;
  padding: 16px 0;
  font-size: 16px;
  font-weight: 600;
  line-height: 24px;
  font-family: var(--font-sans, 'Inter Variable', sans-serif);
  color: #fff;
  background: var(--c-brand, #3370FF);
  border: none;
  border-radius: var(--radius-lg, 8px);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  box-shadow: 0 4px 16px rgba(51, 112, 255, .15);
  transition: all 0.2s var(--ease-out, ease);
  &:hover:not(:disabled) {
    background: var(--c-brand-hover, #2A5FE0);
    box-shadow: 0 6px 20px rgba(51, 112, 255, .25);
    transform: translateY(-1px);
  }
  &:active:not(:disabled) {
    transform: scale(0.98);
  }
  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
    transform: none;
  }
}
.btn-spinner {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255,255,255,.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin-slow 0.8s linear infinite;
}
.btn-spin-icon {
  font-size: 18px;
  animation: spin-slow 1s linear infinite;
}

/* ======================== Password Strength Meter ======================== */
.strength-meter {
  margin-top: 4px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.strength-bars {
  display: flex;
  gap: 4px;
}
.strength-bar {
  flex: 1;
  height: 4px;
  border-radius: 9999px;
  background: var(--c-surface-dim, #EDEEEF);
  transition: background 0.3s var(--ease-out, ease);
}
.strength-bar.bar-weak {
  background: var(--c-busy, #EF4444);
}
.strength-bar.bar-medium {
  background: #FB923C;
}
.strength-bar.bar-strong {
  background: var(--c-brand, #3370FF);
}
.strength-bar.bar-excellent {
  background: var(--c-online, #22C55E);
}
.strength-text {
  font-size: 12px;
  font-weight: 500;
  line-height: 16px;
  color: var(--c-text-secondary, #4E5969);
  font-style: italic;
  transition: color 0.3s var(--ease-out, ease);
}
.strength-text.strength-weak { color: var(--c-busy, #EF4444); }
.strength-text.strength-medium { color: #FB923C; }
.strength-text.strength-strong { color: var(--c-brand, #3370FF); }
.strength-text.strength-excellent { color: var(--c-online, #22C55E); }

/* ======================== Password Rules Checklist ======================== */
.rules-checklist {
  list-style: none;
  margin-top: 4px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.rule-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  font-weight: 500;
  line-height: 16px;
  color: var(--c-text-secondary, #4E5969);
  opacity: 0.6;
  transition: all 0.3s var(--ease-out, ease);
}
.rule-item.met {
  opacity: 1;
  color: var(--c-online, #22C55E);
}
.rule-icon {
  font-size: 14px;
}

/* ======================== Mismatch Error ======================== */
.mismatch-error {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 8px;
  font-size: 12px;
  font-weight: 500;
  line-height: 16px;
  color: var(--c-danger, #FF3B30);
}
.error-icon {
  font-size: 14px;
}

/* ======================== Social Proof (Step 0) ======================== */
.social-proof {
  margin-top: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  opacity: 0.5;
}
.social-avatars {
  display: flex;
  align-items: center;
  margin-left: -12px;
}
.social-avatars > * {
  margin-left: -12px;
}
.social-avatars > *:first-child {
  margin-left: 0;
}
.social-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  border: 4px solid var(--c-bg, #F5F6F7);
  background: var(--c-surface-dim, #E7E8E9);
}
.social-avatar-count {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  border: 4px solid var(--c-bg, #F5F6F7);
  background: var(--c-surface-dim, #EDEEEF);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 10px;
  font-weight: 700;
  color: var(--c-text-secondary, #4E5969);
}
.social-text {
  display: flex;
  flex-direction: column;
  justify-content: center;
}
.social-text-primary {
  font-size: 12px;
  font-weight: 500;
  line-height: 16px;
  letter-spacing: 0.01em;
  color: var(--c-text-secondary, #4E5969);
}
.social-text-secondary {
  font-size: 10px;
  color: var(--c-text-muted, #86909C);
}

/* ======================== Success State (Step 2) ======================== */
.success-card {
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  animation: fade-up 0.4s var(--ease-out, ease);
}
.success-icon-wrap {
  margin-bottom: 16px;
}
.success-icon {
  font-size: 56px;
  color: var(--c-online, #22C55E);
  font-variation-settings: 'FILL' 1;
}
.success-title {
  font-size: 20px;
  font-weight: 600;
  line-height: 28px;
  color: var(--c-text, #1D2129);
  margin-bottom: 8px;
}
.success-desc {
  font-size: 14px;
  line-height: 22px;
  color: var(--c-text-secondary, #4E5969);
  margin-bottom: 24px;
}

/* ======================== Footer ======================== */
.page-footer {
  position: relative;
  z-index: 1;
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 var(--gutter, 1rem);
  font-size: 12px;
  font-weight: 500;
  line-height: 16px;
  color: var(--c-text-muted, #86909C);
  opacity: 0.6;
}
</style>
