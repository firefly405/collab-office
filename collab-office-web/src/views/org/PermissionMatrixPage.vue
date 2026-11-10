<template>
  <div class="perm-page">
    <div class="page-header">
      <div>
        <h2 class="page-title">权限矩阵配置</h2>
        <p class="page-subtitle">定义不同角色在各个功能模块中的细粒度操作权限。修改将实时同步至所有协作成员。</p>
      </div>
      <div class="header-actions">
        <button class="btn-outline" @click="showTemplateDialog = true">重置更改</button>
        <button class="btn-primary" @click="showTemplateDialog = true">保存配置</button>
      </div>
    </div>

    <!-- Preset Roles -->
    <div class="preset-bar">
      <span class="preset-label">应用预设角色:</span>
      <button class="preset-chip" @click="applyTemplate({ key: 'admin', name: '管理员', desc: '' })">
        <span class="material-symbols-outlined">verified_user</span>
        管理员
      </button>
      <button class="preset-chip" @click="applyTemplate({ key: 'editor', name: '普通成员', desc: '' })">
        <span class="material-symbols-outlined">person</span>
        普通成员
      </button>
      <button class="preset-chip" @click="applyTemplate({ key: 'viewer', name: '访客', desc: '' })">
        <span class="material-symbols-outlined">visibility</span>
        访客
      </button>
      <div class="preset-divider" />
      <div class="search-box">
        <span class="material-symbols-outlined search-icon">search</span>
        <input class="search-input" type="text" placeholder="搜索功能或权限项..." />
      </div>
    </div>

    <!-- Matrix Table -->
    <div class="matrix-card">
      <div class="matrix-scroll">
        <table class="matrix-table">
          <thead>
            <tr>
              <th class="row-label">权限操作 / 功能模块</th>
              <th v-for="role in roles" :key="role.key" class="col-header">
                <div class="col-icon">
                  <span class="material-symbols-outlined">{{ moduleIcons[role.key] || 'folder' }}</span>
                </div>
                <span class="col-name">{{ role.name }}</span>
              </th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="level in permLevels" :key="level.key" class="perm-row">
              <td class="row-label">
                <div class="perm-info">
                  <span class="perm-icon" :class="level.colorClass">
                    <span class="material-symbols-outlined">{{ level.icon }}</span>
                  </span>
                  <div>
                    <div class="perm-name">{{ level.label }}</div>
                    <div class="perm-desc">{{ level.desc }}</div>
                  </div>
                </div>
              </td>
              <td v-for="role in roles" :key="role.key + level.key" class="cell">
                <label class="checkbox-cell">
                  <input
                    type="checkbox"
                    :checked="getPermission(role.key, level.key) !== 'none'"
                    @change="togglePermission(role.key, level.key, $event)"
                  />
                  <span class="checkbox-visual" />
                </label>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Bottom Cards -->
    <div class="insight-grid">
      <div class="insight-card">
        <div class="insight-header">
          <span class="insight-label">权限风险提示</span>
          <span class="material-symbols-outlined insight-icon danger">warning</span>
        </div>
        <p class="insight-text">当前配置允许"访客"导出核心文档。建议关闭此权限以防数据泄露。</p>
        <button class="link-btn">立即修复</button>
      </div>
      <div class="insight-card">
        <div class="insight-header">
          <span class="insight-label">覆盖范围</span>
          <span class="material-symbols-outlined insight-icon success">groups</span>
        </div>
        <p class="insight-text">此配置将立即影响到当前工作区中的 124 名活跃成员。</p>
        <div class="insight-avatars">
          <div class="insight-avatar" v-for="i in 3" :key="i" :style="{ background: ['#3370FF','#10B981','#8B5CF6'][i-1] }">
            {{ ['A','B','C'][i-1] }}
          </div>
          <div class="insight-avatar overflow">+121</div>
        </div>
      </div>
      <div class="insight-card primary">
        <div class="insight-header">
          <span class="insight-label-inverse">智能权限推荐</span>
          <span class="material-symbols-outlined insight-icon-inverse">auto_awesome</span>
        </div>
        <p class="insight-text-inverse">基于您的团队规模，我们推荐为"项目经理"开启"会议管理"权限。</p>
        <button class="btn-white-sm">应用建议</button>
      </div>
    </div>

    <el-dialog v-model="showTemplateDialog" title="预设角色模板" width="400px">
      <div v-for="tpl in templates" :key="tpl.key" class="template-item" @click="applyTemplate(tpl)">
        <div class="t-body-md" style="font-weight: 600">{{ tpl.name }}</div>
        <div class="t-label-md" style="color: var(--c-text-muted)">{{ tpl.desc }}</div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const showTemplateDialog = ref(false)

const moduleIcons: Record<string, string> = {
  admin: 'shield_person',
  editor: 'edit',
  viewer: 'visibility',
  guest: 'person_outline',
}

const roles = [
  { key: 'admin', name: '管理员', desc: '空间管理员' },
  { key: 'editor', name: '编辑者', desc: '普通成员' },
  { key: 'viewer', name: '查看者', desc: '只读访问' },
  { key: 'guest', name: '访客', desc: '外部协作者' },
]

const permLevels = [
  { key: 'manage', label: '管理 (Admin)', icon: 'security', colorClass: 'icon-red', desc: '允许删除、归档及配置成员' },
  { key: 'edit', label: '编辑 (Edit)', icon: 'edit', colorClass: 'icon-green', desc: '允许修改、添加和删除内容' },
  { key: 'comment', label: '评论 (Comment)', icon: 'add_comment', colorClass: 'icon-purple', desc: '仅允许在内容中添加批注' },
  { key: 'view', label: '查看 (View)', icon: 'visibility', colorClass: 'icon-blue', desc: '允许查看内容和参与者' },
  { key: 'none', label: '无权限', icon: 'block', colorClass: 'icon-gray', desc: '不可访问此模块' },
]

const defaultMatrix: Record<string, Record<string, string>> = {
  admin: { document: 'manage', spreadsheet: 'manage', mindmap: 'manage', whiteboard: 'manage', im: 'manage', meeting: 'manage', task: 'manage' },
  editor: { document: 'edit', spreadsheet: 'edit', mindmap: 'edit', whiteboard: 'edit', im: 'edit', meeting: 'edit', task: 'edit' },
  viewer: { document: 'view', spreadsheet: 'view', mindmap: 'view', whiteboard: 'view', im: 'view', meeting: 'view', task: 'view' },
  guest: { document: 'comment', spreadsheet: 'none', mindmap: 'none', whiteboard: 'none', im: 'none', meeting: 'none', task: 'none' },
}

const matrix = reactive(structuredClone(defaultMatrix))

const templates = [
  { key: 'default', name: '默认权限', desc: '管理员全部管理，成员可编辑，访客仅评论' },
  { key: 'open', name: '开放协作', desc: '所有成员可编辑，访客可查看' },
  { key: 'strict', name: '严格管控', desc: '只有管理员可编辑，成员仅查看' },
]

function getPermission(role: string, module: string) {
  return matrix[role]?.[module] || 'none'
}

function togglePermission(role: string, module: string, event: Event) {
  const checked = (event.target as HTMLInputElement).checked
  const newVal = checked ? 'edit' : 'none'
  matrix[role][module] = newVal
  request.put('/org/permission/matrix', { role, module, level: newVal }).then(() => {
    ElMessage.success('权限已更新')
  }).catch(() => ElMessage.error('更新失败'))
}

function applyTemplate(tpl: typeof templates[0]) {
  if (tpl.key === 'default') Object.assign(matrix, structuredClone(defaultMatrix))
  showTemplateDialog.value = false
  ElMessage.success(`已应用"${tpl.name}"模板`)
}
</script>

<style lang="scss" scoped>
.perm-page {
  padding: var(--spacing-desktop, 2rem);
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.page-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
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
  font-size: 14px;
  color: var(--c-text-muted);
  margin: 0;
  max-width: 600px;
}

.header-actions {
  display: flex;
  gap: 0.75rem;
}

/* Preset Bar */
.preset-bar {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 1rem;
  background: var(--c-bg);
  border: 1px solid var(--c-border);
  border-radius: var(--radius-xl, 0.75rem);
  flex-wrap: wrap;
}

.preset-label {
  font-size: 12px;
  font-weight: 600;
  color: var(--c-text-ghost);
}

.preset-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  background: var(--c-surface);
  border: 1px solid var(--c-border);
  border-radius: 20px;
  font-size: 14px;
  color: var(--c-text-secondary);
  cursor: pointer;
  transition: border-color 0.15s, color 0.15s;

  &:hover { border-color: var(--c-brand); color: var(--c-brand); }

  .material-symbols-outlined { font-size: 18px; }
}

.preset-divider {
  width: 1px;
  height: 24px;
  background: var(--c-border);
  margin: 0 0.5rem;
}

.search-box {
  position: relative;
  flex: 1;
  min-width: 200px;
}

.search-icon {
  position: absolute;
  left: 10px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 20px;
  color: var(--c-text-ghost);
  pointer-events: none;
}

.search-input {
  width: 100%;
  padding: 6px 12px 6px 36px;
  background: var(--c-surface);
  border: 1px solid var(--c-border);
  border-radius: var(--radius-lg, 0.5rem);
  font-size: 14px;
  color: var(--c-text);
  outline: none;
  box-sizing: border-box;
  transition: border-color 0.15s;

  &:focus { border-color: var(--c-brand); }
}

/* Matrix Table */
.matrix-card {
  background: var(--c-surface);
  border: 1px solid var(--c-border);
  border-radius: var(--radius-xl, 0.75rem);
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0,0,0,.04);
}

.matrix-scroll {
  overflow-x: auto;
}

.matrix-table {
  width: 100%;
  border-collapse: collapse;
  min-width: 800px;

  th, td {
    padding: 1rem;
    border-bottom: 1px solid var(--c-border-light, #E5E6EB);
  }

  thead th {
    background: var(--c-bg);
    position: sticky;
    top: 0;
    z-index: 2;
    backdrop-filter: blur(8px);
  }
}

.col-header {
  text-align: center;
  min-width: 100px;
}

.col-icon {
  margin-bottom: 4px;

  .material-symbols-outlined { font-size: 24px; color: var(--c-brand); }
}

.col-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--c-text);
}

.row-label {
  font-weight: 600;
  color: var(--c-text);
  background: var(--c-surface-dim);
  min-width: 200px;
  text-align: left !important;
  border-right: 1px solid var(--c-border);
}

.perm-row {
  transition: background 0.15s;

  &:hover { background: var(--c-surface-dim); }
}

.perm-info {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.perm-icon {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;

  .material-symbols-outlined { font-size: 20px; }

  &.icon-blue { background: #EFF6FF; color: #3B82F6; }
  &.icon-green { background: #ECFDF5; color: #10B981; }
  &.icon-orange { background: #FFF7ED; color: #F97316; }
  &.icon-red { background: #FEF2F2; color: #EF4444; }
  &.icon-purple { background: #F5F3FF; color: #8B5CF6; }
  &.icon-gray { background: #F3F4F6; color: #6B7280; }
}

.perm-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--c-text);
}

.perm-desc {
  font-size: 11px;
  color: var(--c-text-muted);
}

.cell {
  text-align: center;
}

/* Custom checkbox */
.checkbox-cell {
  display: inline-flex;
  cursor: pointer;

  input {
    position: absolute;
    opacity: 0;
    width: 0;
    height: 0;

    &:checked + .checkbox-visual {
      background: var(--c-brand);
      border-color: var(--c-brand);

      &::after {
        content: '';
        position: absolute;
        left: 5px;
        top: 2px;
        width: 6px;
        height: 10px;
        border: solid #fff;
        border-width: 0 2px 2px 0;
        transform: rotate(45deg);
      }
    }
  }
}

.checkbox-visual {
  width: 20px;
  height: 20px;
  border: 2px solid var(--c-border);
  border-radius: 4px;
  background: transparent;
  position: relative;
  transition: background 0.15s, border-color 0.15s;
}

/* Insight Cards */
.insight-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 1.5rem;
}

.insight-card {
  padding: 1.25rem;
  background: var(--c-surface);
  border: 1px solid var(--c-border);
  border-radius: var(--radius-xl, 0.75rem);
  display: flex;
  flex-direction: column;
  gap: 0.75rem;

  &.primary {
    background: var(--c-brand);
    border-color: var(--c-brand);
  }
}

.insight-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.insight-label {
  font-size: 12px;
  font-weight: 600;
  color: var(--c-text-ghost);
}

.insight-label-inverse {
  font-size: 12px;
  font-weight: 600;
  color: rgba(255,255,255,.7);
}

.insight-icon {
  font-size: 20px;

  &.danger { color: var(--c-danger); }
  &.success { color: var(--c-success); }
}

.insight-icon-inverse {
  font-size: 20px;
  color: rgba(255,255,255,.8);
}

.insight-text {
  font-size: 14px;
  color: var(--c-text-secondary);
  margin: 0;
  line-height: 1.5;
}

.insight-text-inverse {
  font-size: 14px;
  color: rgba(255,255,255,.9);
  margin: 0;
  line-height: 1.5;
}

.insight-avatars {
  display: flex;
  margin-top: 0.25rem;
}

.insight-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: 2px solid var(--c-surface);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 11px;
  font-weight: 600;
  margin-right: -8px;

  &.overflow {
    background: var(--c-border);
    color: var(--c-text-secondary);
    font-size: 10px;
  }
}

.link-btn {
  background: none;
  border: none;
  color: var(--c-brand);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  text-align: left;
  padding: 0;

  &:hover { text-decoration: underline; }
}

.btn-white-sm {
  padding: 6px 12px;
  background: #fff;
  color: var(--c-brand);
  border: none;
  border-radius: var(--radius-lg, 0.5rem);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  width: fit-content;

  &:hover { opacity: 0.9; }
}

/* Buttons */
.btn-primary {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0.5rem 1rem;
  background: var(--c-brand);
  color: #fff;
  border: none;
  border-radius: var(--radius-lg, 0.5rem);
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  transition: opacity 0.15s, transform 0.1s;
  box-shadow: 0 1px 3px rgba(0,0,0,.08);

  &:hover { opacity: 0.9; }
  &:active { transform: scale(0.98); }
}

.btn-outline {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0.5rem 1rem;
  background: transparent;
  color: var(--c-text-secondary);
  border: 1px solid var(--c-border);
  border-radius: var(--radius-lg, 0.5rem);
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.15s;

  &:hover { background: var(--c-surface-dim); }
}

.template-item {
  padding: 12px;
  border: 1px solid var(--c-border);
  border-radius: var(--radius);
  cursor: pointer;
  margin-bottom: 8px;
  transition: border-color .15s;

  &:hover { border-color: var(--c-brand); }
}
</style>
