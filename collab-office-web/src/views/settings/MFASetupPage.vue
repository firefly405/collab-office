<template>
  <div class="mfa-page">
    <div class="page-header">
      <div class="header-left">
        <span class="material-symbols-outlined header-icon">security</span>
        <div>
          <h2 class="t-h2" style="color: var(--c-text)">多因素认证 (MFA)</h2>
          <p class="t-body-md" style="color: var(--c-text-muted)">启用多因素认证以增强账户安全</p>
        </div>
      </div>
    </div>

    <!-- Status banner -->
    <div class="status-banner" :class="{ active: mfaEnabled }">
      <span class="material-symbols-outlined status-icon">
        {{ mfaEnabled ? 'verified_user' : 'gpp_maybe' }}
      </span>
      <div>
        <div class="status-title">{{ mfaEnabled ? '已启用' : '未启用' }}</div>
        <div class="t-body-md status-desc">{{ mfaEnabled ? '账户受到多因素认证保护' : '建议启用以保护账户安全' }}</div>
      </div>
    </div>

    <div class="mfa-card">
      <!-- MFA methods -->
      <div class="mfa-section">
        <h3 class="t-h3 section-title">认证方式</h3>
        <div class="methods-list">
          <div
            v-for="method in methods"
            :key="method.key"
            class="method-item"
            :class="{ active: method.enabled }"
            @click="toggleMethod(method)"
          >
            <div class="method-left">
              <div class="method-icon-wrap" :class="{ on: method.enabled }">
                <span class="material-symbols-outlined method-icon">
                  {{ method.key === 'totp' ? 'phonelink_lock' : method.key === 'sms' ? 'sms' : 'mail' }}
                </span>
              </div>
              <div class="method-info">
                <div class="method-name">{{ method.name }}</div>
                <div class="t-label-md method-desc">{{ method.desc }}</div>
              </div>
            </div>
            <el-switch :model-value="method.enabled" @click.stop />
          </div>
        </div>
      </div>

      <div class="section-divider" />

      <!-- Backup codes -->
      <div class="mfa-section" v-if="mfaEnabled">
        <h3 class="t-h3 section-title">备用恢复码</h3>
        <p class="t-body-md" style="color: var(--c-text-muted); margin-bottom: 14px">
          请妥善保存，每个码仅可使用一次。如遗失认证器，可使用恢复码登录。
        </p>
        <div class="codes-grid">
          <div v-for="code in backupCodes" :key="code" class="code-item">
            <span class="material-symbols-outlined code-icon">key</span>
            {{ code }}
          </div>
        </div>
        <button class="regenerate-btn" @click="regenerateCodes">
          <span class="material-symbols-outlined">refresh</span>
          重新生成
        </button>
      </div>

      <div class="section-divider" />

      <!-- Actions -->
      <div class="mfa-actions">
        <button v-if="!mfaEnabled" type="button" class="enable-btn" @click="enableMFA">
          <span class="material-symbols-outlined">shield_plus</span>
          启用 MFA
        </button>
        <button v-else type="button" class="disable-btn" @click="showDisableDialog = true">
          <span class="material-symbols-outlined">shield_minus</span>
          关闭 MFA
        </button>
      </div>
    </div>

    <!-- QR Code dialog -->
    <el-dialog v-model="showQRCode" title="扫描二维码绑定" width="400px" center :close-on-click-modal="false">
      <div class="qr-section">
        <div class="qr-placeholder">
          <span class="material-symbols-outlined qr-icon">qr_code_2</span>
        </div>
        <p class="t-body-md" style="text-align:center; color:var(--c-text-muted)">
          使用认证器 APP（如 Google Authenticator）扫描二维码
        </p>
        <el-input
          v-model="verifyCode"
          placeholder="输入6位动态验证码"
          class="t-mono"
          size="large"
          style="margin-top: 16px; text-align: center;"
          maxlength="6"
        >
          <template #prefix>
            <span class="material-symbols-outlined" style="font-size:20px;color:var(--c-text-muted)">pin</span>
          </template>
        </el-input>
        <button type="button" class="verify-btn" @click="verifyMFA">
          <span class="material-symbols-outlined">check</span>
          验证并启用
        </button>
      </div>
    </el-dialog>

    <!-- Disable confirmation dialog -->
    <el-dialog v-model="showDisableDialog" title="关闭 MFA" width="380px" center>
      <div style="text-align: center; padding: 8px 0 16px;">
        <span class="material-symbols-outlined" style="font-size:48px;color:var(--c-warning);margin-bottom:12px;display:block">warning</span>
        <p class="t-body-md" style="color: var(--c-text-secondary)">
          关闭后，您的账户将仅通过密码保护。确定要关闭吗？
        </p>
      </div>
      <template #footer>
        <div style="display:flex;gap:12px;justify-content:center">
          <el-button @click="showDisableDialog = false">取消</el-button>
          <el-button type="danger" @click="disableMFA">
            <span class="material-symbols-outlined" style="font-size:16px;margin-right:4px">shield_off</span>
            确认关闭
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const mfaEnabled = ref(false)
const showQRCode = ref(false)
const showDisableDialog = ref(false)
const verifyCode = ref('')
const backupCodes = ref<string[]>([])
const loading = ref(false)

const methods = reactive([
  { key: 'totp', name: '认证器 APP', desc: '使用 Google Authenticator 等应用生成动态码', enabled: false },
  { key: 'sms', name: '短信验证码', desc: '通过绑定的手机号接收验证码', enabled: false },
  { key: 'email', name: '邮箱验证码', desc: '通过绑定的邮箱接收验证码', enabled: false },
])

async function toggleMethod(method: typeof methods[0]) {
  method.enabled = !method.enabled
  ElMessage.success(`${method.name} ${method.enabled ? '已启用' : '已关闭'}`)
}

async function enableMFA() {
  loading.value = true
  request.post('/user/mfa/enable').then((res: any) => {
    if (res.code === 200) {
      if (res.data?.backupCodes) backupCodes.value = res.data.backupCodes.split(',')
      showQRCode.value = true
    }
  }).catch(() => ElMessage.error('操作失败')).finally(() => loading.value = false)
}

async function verifyMFA() {
  if (!verifyCode.value.trim()) return
  request.post('/user/mfa/verify', { code: verifyCode.value }).then((res: any) => {
    if (res.code === 200) {
      mfaEnabled.value = true
      showQRCode.value = false
      ElMessage.success('MFA 已成功启用')
    }
  }).catch(() => ElMessage.error('验证失败'))
}

async function disableMFA() {
  request.post('/user/mfa/disable').then((res: any) => {
    if (res.code === 200) {
      mfaEnabled.value = false
      showDisableDialog.value = false
      ElMessage.success('MFA 已关闭')
    }
  }).catch(() => ElMessage.error('操作失败'))
}

function regenerateCodes() {
  request.post('/user/mfa/enable').then((res: any) => {
    if (res.code === 200 && res.data?.backupCodes) {
      backupCodes.value = res.data.backupCodes.split(',')
      ElMessage.success('备用码已更新')
    }
  }).catch(() => {})
}
</script>

<style lang="scss" scoped>
.mfa-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
  max-width: 680px;
}

.page-header { margin-bottom: 4px; }
.header-left { display: flex; align-items: flex-start; gap: 14px; }
.header-icon {
  font-size: 36px; color: var(--c-brand);
  background: var(--c-brand-light); padding: 10px; border-radius: var(--radius-lg);
}

// -- Status banner --
.status-banner {
  display: flex; gap: 14px; align-items: center;
  padding: 16px 20px; border-radius: var(--radius-xl);
  background: var(--c-danger-light); color: var(--c-danger);
  border: 1px solid transparent;

  &.active {
    background: var(--c-success-light); color: var(--c-success);
    border-color: rgba(0,181,120,.2);
  }
}
.status-icon { font-size: 28px; }
.status-title { font-size: 16px; font-weight: 600; line-height: 24px; }
.status-desc { color: inherit; opacity: .8; }

// -- Card --
.mfa-card {
  background: var(--c-surface);
  border: 1px solid var(--c-border);
  border-radius: var(--radius-xl);
  padding: 28px 32px;
  box-shadow: var(--shadow-xs);
}

.mfa-section { padding: 4px 0; }
.section-title { color: var(--c-text); margin-bottom: 12px; }
.section-divider { border-top: 1px solid var(--c-border-light); margin: 24px 0; }

// -- Methods --
.methods-list { display: flex; flex-direction: column; gap: 8px; }

.method-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  border: 1px solid var(--c-border);
  border-radius: var(--radius-lg);
  cursor: pointer;
  transition: all .2s var(--ease-out);

  &:hover { border-color: var(--c-brand); }
  &.active { border-color: var(--c-brand); background: var(--c-brand-light); }
}

.method-left { display: flex; align-items: center; gap: 14px; }
.method-icon-wrap {
  width: 40px; height: 40px;
  display: flex; align-items: center; justify-content: center;
  border-radius: var(--radius-lg); background: var(--c-surface-dim);

  &.on { background: var(--c-brand-light); }
}
.method-icon { font-size: 22px; color: var(--c-text-muted); }
.method-icon-wrap.on .method-icon { color: var(--c-brand); }

.method-name { font-size: 14px; font-weight: 600; color: var(--c-text); }
.method-desc { color: var(--c-text-muted); margin-top: 2px; }

// -- Backup codes --
.codes-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  margin-bottom: 10px;
}

.code-item {
  display: flex; align-items: center; gap: 8px;
  padding: 10px 14px;
  background: var(--c-surface-dim);
  border: 1px solid var(--c-border);
  border-radius: var(--radius-lg);
  font-family: var(--font-mono);
  font-size: 14px; letter-spacing: 2px;
  color: var(--c-text);
}
.code-icon { font-size: 16px; color: var(--c-text-muted); }

.regenerate-btn {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 6px 14px; border: 1px solid var(--c-border); border-radius: var(--radius-lg);
  background: var(--c-surface); color: var(--c-brand); font-size: 13px; font-weight: 500; cursor: pointer;
  transition: all .15s var(--ease-out);
  &:hover { border-color: var(--c-brand); background: var(--c-brand-light); }
  .material-symbols-outlined { font-size: 18px; }
}

// -- Actions --
.mfa-actions {
  display: flex; gap: 10px;
}

.enable-btn {
  display: flex; align-items: center; gap: 8px;
  padding: 12px 28px; background: var(--c-brand); color: #fff; border: none;
  border-radius: var(--radius-lg); font-size: 15px; font-weight: 500; cursor: pointer;
  transition: all .2s var(--ease-out);
  &:hover { background: var(--c-brand-hover); transform: translateY(-1px); box-shadow: var(--shadow-sm); }
  &:active { transform: scale(.98); }
  .material-symbols-outlined { font-size: 20px; }
}

.disable-btn {
  display: flex; align-items: center; gap: 8px;
  padding: 12px 28px; background: transparent; color: var(--c-danger); border: 1px solid var(--c-danger);
  border-radius: var(--radius-lg); font-size: 15px; font-weight: 500; cursor: pointer;
  transition: all .2s var(--ease-out);
  &:hover { background: var(--c-danger-light); }
  &:active { transform: scale(.98); }
  .material-symbols-outlined { font-size: 20px; }
}

// -- QR dialog --
.qr-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 8px 0;
}

.qr-placeholder {
  width: 200px; height: 200px;
  display: flex; align-items: center; justify-content: center;
  background: var(--c-surface-dim); border: 2px dashed var(--c-border);
  border-radius: var(--radius-xl); margin-bottom: 16px;
}
.qr-icon { font-size: 120px; color: var(--c-text-ghost); opacity: .3; }

.verify-btn {
  width: 100%; display: flex; align-items: center; justify-content: center; gap: 8px;
  margin-top: 16px; padding: 12px;
  background: var(--c-brand); color: #fff; border: none;
  border-radius: var(--radius-lg); font-size: 15px; font-weight: 500; cursor: pointer;
  transition: all .2s var(--ease-out);
  &:hover { background: var(--c-brand-hover); }
  &:active { transform: scale(.98); }
  .material-symbols-outlined { font-size: 20px; }
}
</style>
