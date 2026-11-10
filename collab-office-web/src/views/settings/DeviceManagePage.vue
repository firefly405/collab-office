<template>
  <div class="device-page">
    <div class="page-hero">
      <h2 class="page-title">多设备管理</h2>
      <p class="page-subtitle">监控并管理所有已登录 Nexus Office 的实时在线设备。Nexus Office 实时同步已开启。</p>
    </div>

    <div class="stats-grid">
      <div class="stat-card">
        <div class="stat-row">
          <div>
            <p class="stat-label">当前活动会话</p>
            <p class="stat-value">{{ devices.length }}</p>
          </div>
          <div class="stat-icon-circle">
            <span class="material-symbols-outlined">devices</span>
          </div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-row">
          <div>
            <p class="stat-label">实时流量 (Sync)</p>
            <p class="stat-value success">{{ syncRate }}</p>
          </div>
          <div class="stat-icon-circle success">
            <span class="material-symbols-outlined">sync</span>
          </div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-row">
          <div>
            <p class="stat-label">高风险会话</p>
            <p class="stat-value danger">0</p>
          </div>
          <div class="stat-icon-circle danger">
            <span class="material-symbols-outlined">security</span>
          </div>
        </div>
      </div>
    </div>

    <div class="device-section">
      <div class="section-header">
        <h3 class="section-title">活动设备列表</h3>
        <span class="sync-badge">
          <span class="sync-pulse" />
          Nexus Office 已就绪
        </span>
      </div>

      <div class="device-list">
        <div
          v-for="device in devices"
          :key="device.id"
          class="device-card"
          :class="{ current: device.isCurrent }"
        >
          <div class="device-main">
            <div class="device-icon-box" :class="{ primary: device.isCurrent }">
              <span class="material-symbols-outlined">{{ deviceIcon(device) }}</span>
            </div>
            <div class="device-info">
              <div class="device-name-row">
                <h4 class="device-name">{{ device.name }}</h4>
                <span v-if="device.isCurrent" class="current-tag">本机</span>
              </div>
              <div class="device-meta">
                <span class="meta-item">
                  <span class="material-symbols-outlined">location_on</span>
                  {{ device.location || '未知' }}
                </span>
                <span class="meta-item mono">
                  <span class="material-symbols-outlined">dns</span>
                  {{ device.ip }}
                </span>
                <span class="meta-item">
                  <span class="material-symbols-outlined">schedule</span>
                  最后活跃: {{ device.lastActive || '刚才' }}
                </span>
              </div>
            </div>
          </div>
          <button
            v-if="!device.isCurrent"
            class="btn-danger"
            @click="revokeDevice(device)"
          >
            强制下线
          </button>
          <button v-else class="btn-disabled" disabled>
            当前会话
          </button>
        </div>
      </div>
    </div>

    <div class="security-tips">
      <h3 class="tips-title">安全建议</h3>
      <ul class="tips-list">
        <li>及时移除不认识的设备</li>
        <li>在公共设备登录后务必退出</li>
        <li>开启登录通知，获取异常登录提醒</li>
        <li>定期检查活跃会话列表</li>
      </ul>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

const syncRate = ref('1.2 MB/s')
const loading = ref(false)
const devices = ref<any[]>([])

function deviceIcon(device: any): string {
  const name = (device.name || device.deviceName || '').toLowerCase()
  if (name.includes('iphone') || name.includes('phone')) return 'smartphone'
  if (name.includes('ipad') || name.includes('tablet')) return 'tablet_mac'
  if (name.includes('macbook') || name.includes('mac')) return 'laptop_mac'
  if (name.includes('chrome') || name.includes('browser') || name.includes('windows')) return 'language'
  return 'devices'
}

onMounted(() => fetchDevices())

function fetchDevices() {
  loading.value = true
  request.get('/user/devices').then((res: any) => {
    if (res.code === 200) devices.value = res.data || []
  }).catch(() => {}).finally(() => loading.value = false)
}

async function revokeDevice(device: any) {
  await ElMessageBox.confirm(`确定要强制下线 ${device.name} 吗？`, '强制下线', { type: 'warning' })
  request.post(`/user/device/${device.id || device.deviceId}/revoke`).then((res: any) => {
    if (res.code === 200) {
      devices.value = devices.value.filter(d => d.id !== device.id && d.deviceId !== device.deviceId)
      ElMessage.success(`${device.name} 已强制下线`)
    }
  }).catch(() => ElMessage.error('操作失败'))
}
</script>

<style lang="scss" scoped>
.device-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: var(--spacing-desktop, 2rem);
}

.page-hero {
  margin-bottom: 2rem;
}

.page-title {
  font-size: 32px;
  line-height: 40px;
  letter-spacing: -0.02em;
  font-weight: 700;
  color: var(--c-text);
  margin: 0 0 0.5rem;
}

.page-subtitle {
  font-size: 16px;
  color: var(--c-text-muted);
  margin: 0;
  max-width: 700px;
}

/* Stats Grid */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 1rem;
  margin-bottom: 2rem;
}

.stat-card {
  background: var(--c-bg);
  border: 1px solid var(--c-border);
  border-radius: var(--radius-xl, 0.75rem);
  padding: 1.5rem;
}

.stat-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.stat-label {
  font-size: 12px;
  font-weight: 500;
  color: var(--c-text-muted);
  margin: 0 0 0.25rem;
}

.stat-value {
  font-size: 24px;
  line-height: 32px;
  font-weight: 700;
  color: var(--c-text);
  margin: 0;

  &.success { color: var(--c-success); }
  &.danger { color: var(--c-danger); }
}

.stat-icon-circle {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: rgba(51,112,255,.08);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--c-brand);

  &.success { background: rgba(34,197,94,.08); color: var(--c-success); }
  &.danger { background: rgba(255,59,48,.08); color: var(--c-danger); }

  .material-symbols-outlined { font-size: 24px; }
}

/* Device Section */
.device-section {
  margin-bottom: 2rem;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 1rem;
}

.section-title {
  font-size: 20px;
  line-height: 28px;
  font-weight: 600;
  color: var(--c-text);
  margin: 0;
}

.sync-badge {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 12px;
  background: var(--c-surface-dim);
  border-radius: 20px;
  font-size: 12px;
  color: var(--c-text-secondary);
}

.sync-pulse {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--c-success);
  position: relative;

  &::after {
    content: '';
    position: absolute;
    inset: -4px;
    border-radius: 50%;
    background: var(--c-success);
    opacity: 0.3;
    animation: pulse 2s infinite;
  }
}

@keyframes pulse {
  0% { transform: scale(1); opacity: 0.6; }
  100% { transform: scale(2.5); opacity: 0; }
}

/* Device List */
.device-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.device-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 1.5rem;
  background: rgba(255,255,255,.7);
  backdrop-filter: blur(12px);
  border: 1px solid var(--c-border);
  border-radius: var(--radius-xl, 0.75rem);
  transition: border-color 0.15s, background 0.15s;

  &:hover { border-color: rgba(51,112,255,.3); background: rgba(255,255,255,.9); }

  &.current {
    border-color: rgba(51,112,255,.3);
    border-width: 2px;
  }
}

.device-main {
  display: flex;
  align-items: flex-start;
  gap: 1rem;
  flex: 1;
  min-width: 0;
}

.device-icon-box {
  width: 56px;
  height: 56px;
  border-radius: var(--radius-xl, 0.75rem);
  background: var(--c-border);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--c-text-secondary);
  flex-shrink: 0;
  transition: background 0.15s, color 0.15s;

  .material-symbols-outlined { font-size: 32px; }

  &.primary {
    background: var(--c-brand);
    color: #fff;
    box-shadow: 0 4px 12px rgba(51,112,255,.25);
  }
}

.device-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.device-name-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.device-name {
  font-size: 16px;
  font-weight: 600;
  color: var(--c-text);
  margin: 0;
}

.current-tag {
  font-size: 10px;
  font-weight: 700;
  padding: 2px 8px;
  background: rgba(51,112,255,.1);
  color: var(--c-brand);
  border-radius: 4px;
}

.device-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 1rem;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--c-text-muted);

  .material-symbols-outlined { font-size: 16px; }

  &.mono { font-family: 'JetBrains Mono Variable', monospace; }
}

/* Buttons */
.btn-danger {
  padding: 0.5rem 1.5rem;
  background: var(--c-danger);
  color: #fff;
  border: none;
  border-radius: var(--radius-lg, 0.5rem);
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  flex-shrink: 0;
  transition: filter 0.15s, transform 0.1s;
  box-shadow: 0 1px 3px rgba(0,0,0,.08);

  &:hover { filter: brightness(1.1); }
  &:active { transform: scale(0.95); }
}

.btn-disabled {
  padding: 0.5rem 1.5rem;
  background: var(--c-border);
  color: var(--c-text-muted);
  border: none;
  border-radius: var(--radius-lg, 0.5rem);
  font-size: 12px;
  font-weight: 500;
  cursor: not-allowed;
  opacity: 0.5;
  flex-shrink: 0;
}

/* Security Tips */
.security-tips {
  background: var(--c-surface);
  border: 1px solid var(--c-border);
  border-radius: var(--radius-xl, 0.75rem);
  padding: 1.5rem;
}

.tips-title {
  font-size: 20px;
  font-weight: 600;
  color: var(--c-text);
  margin: 0 0 1rem;
}

.tips-list {
  padding-left: 1.25rem;
  margin: 0;
  color: var(--c-text-secondary);
  font-size: 14px;
  line-height: 2.2;

  li { padding-left: 4px; }
}
</style>
