<template>
  <div class="org-page">
    <div class="top-bar">
      <div class="breadcrumb">
        <span>组织管理</span>
        <span class="material-symbols-outlined">chevron_right</span>
        <span class="current">部门架构配置</span>
      </div>
      <div class="top-actions">
        <button class="btn-outline-sm">
          <span class="material-symbols-outlined">history</span>
          版本记录
        </button>
        <button class="btn-primary-sm" @click="showDeptDialog = true">
          发布更改
        </button>
      </div>
    </div>

    <div class="org-body">
      <!-- Left: Department Tree -->
      <aside class="tree-panel">
        <div class="tree-toolbar">
          <div class="search-box">
            <span class="material-symbols-outlined search-icon">search</span>
            <input class="search-input" type="text" placeholder="搜索部门..." />
          </div>
          <div class="tree-title-row">
            <h3 class="tree-panel-title">层级结构</h3>
            <button class="btn-add-dept" @click="addDept(null)">
              <span class="material-symbols-outlined">add_circle</span>
              新增
            </button>
          </div>
        </div>

        <div class="tree-scroll">
          <div class="tree-root">
            <!-- Root Node -->
            <div class="tree-item active">
              <span class="material-symbols-outlined tree-arrow">expand_more</span>
              <span class="material-symbols-outlined tree-node-icon" style="font-variation-settings:'FILL'1">corporate_fare</span>
              <span class="tree-node-name">未来科技集团</span>
              <div class="tree-node-actions">
                <span class="material-symbols-outlined action-icon" @click="addDept(null)">add</span>
                <span class="material-symbols-outlined action-icon">edit</span>
              </div>
            </div>

            <!-- Level 1 Children -->
            <div class="tree-children">
              <div class="tree-item" v-for="dept in deptTree" :key="dept.id">
                <span class="material-symbols-outlined tree-arrow" @click="dept.expanded = !dept.expanded">
                  {{ dept.children?.length ? (dept.expanded ? 'expand_more' : 'chevron_right') : 'drag_handle' }}
                </span>
                <span class="material-symbols-outlined tree-node-icon">{{ dept.children?.length ? 'domain' : 'groups' }}</span>
                <span class="tree-node-name">{{ dept.name }}</span>
                <span class="tree-node-count">{{ dept.memberCount || 0 }} 人</span>
                <div class="tree-node-actions">
                  <span class="material-symbols-outlined action-icon">drag_indicator</span>
                  <span class="material-symbols-outlined action-icon" @click="addDept(dept.id)">add</span>
                  <span class="material-symbols-outlined action-icon" @click="editDept(dept)">edit</span>
                  <span class="material-symbols-outlined action-icon danger" @click="deleteDept(dept)">delete</span>
                </div>
              </div>

              <!-- Level 2 Children (for expanded nodes) -->
              <template v-for="dept in deptTree" :key="'children-' + dept.id">
                <div v-if="dept.children?.length && dept.expanded" class="tree-children">
                  <div class="tree-item" v-for="child in dept.children" :key="child.id">
                    <span class="material-symbols-outlined tree-arrow">drag_handle</span>
                    <span class="material-symbols-outlined tree-node-icon">group_work</span>
                    <span class="tree-node-name">{{ child.name }}</span>
                    <span class="tree-node-count">{{ child.memberCount || 0 }} 人</span>
                    <div class="tree-node-actions">
                      <span class="material-symbols-outlined action-icon" @click="editDept(child)">edit</span>
                      <span class="material-symbols-outlined action-icon danger" @click="deleteDept(child)">delete</span>
                    </div>
                  </div>
                </div>
              </template>
            </div>
          </div>
        </div>
      </aside>

      <!-- Right: Detail Panel -->
      <section class="detail-panel">
        <div class="detail-header">
          <div class="dept-hero">
            <div class="dept-hero-icon">
              <span class="material-symbols-outlined" style="font-variation-settings:'FILL'1">corporate_fare</span>
            </div>
            <div>
              <h2 class="dept-hero-name">未来科技集团</h2>
              <div class="dept-hero-meta">
                <span class="meta-item">
                  <span class="material-symbols-outlined">person</span>
                  负责人：<span class="meta-highlight">陈明远 (CEO)</span>
                </span>
                <span class="meta-item">
                  <span class="material-symbols-outlined">group</span>
                  成员总数：<span class="meta-mono">{{ totalMembers }}</span>
                </span>
              </div>
            </div>
          </div>
          <div class="dept-actions">
            <button class="btn-icon">
              <span class="material-symbols-outlined">edit</span>
            </button>
            <button class="btn-icon">
              <span class="material-symbols-outlined">manage_accounts</span>
            </button>
          </div>
        </div>

        <!-- Summary Cards -->
        <div class="summary-grid">
          <div class="summary-card">
            <div class="summary-label">子部门数量</div>
            <div class="summary-value">{{ deptTree.length }}</div>
            <div class="summary-trend up">
              <span class="material-symbols-outlined">trending_up</span>
              较上季增长 20%
            </div>
          </div>
          <div class="summary-card">
            <div class="summary-label">关键岗位饱和度</div>
            <div class="summary-value">94%</div>
            <div class="summary-bar">
              <div class="summary-bar-fill" style="width:94%" />
            </div>
          </div>
          <div class="summary-card">
            <div class="summary-label">部门活跃度指数</div>
            <div class="summary-value">A+</div>
            <div class="summary-meta">基于近30日协作数据</div>
          </div>
        </div>

        <!-- Member Table -->
        <div class="member-section">
          <div class="member-section-header">
            <h3 class="section-title">核心成员</h3>
            <div class="member-actions">
              <button class="btn-outline-sm">
                <span class="material-symbols-outlined">filter_list</span>
                筛选
              </button>
              <button class="btn-primary-sm">
                <span class="material-symbols-outlined">add</span>
                添加成员
              </button>
            </div>
          </div>
          <div class="member-table-wrap">
            <table class="member-table">
              <thead>
                <tr>
                  <th>姓名</th>
                  <th>职位</th>
                  <th>状态</th>
                  <th>最近协作</th>
                  <th class="text-right">操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(m, i) in sampleMembers" :key="i">
                  <td>
                    <div class="member-cell">
                      <div class="member-avatar-xs" :style="{ background: m.color }">
                        {{ m.name[0] }}
                        <span class="presence-dot" :class="m.status" />
                      </div>
                      <div>
                        <div class="member-name">{{ m.name }}</div>
                        <div class="member-email">{{ m.email }}</div>
                      </div>
                    </div>
                  </td>
                  <td class="text-muted">{{ m.role }}</td>
                  <td>
                    <span class="status-badge-sm" :class="m.status">
                      {{ m.status === 'online' ? 'Online' : m.status === 'busy' ? 'Busy' : 'Offline' }}
                    </span>
                  </td>
                  <td class="text-mono">{{ m.lastSeen }}</td>
                  <td class="text-right">
                    <button class="btn-icon-sm">
                      <span class="material-symbols-outlined">more_horiz</span>
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </section>
    </div>

    <!-- FAB -->
    <div class="fab-group">
      <button class="fab-secondary">
        <span class="material-symbols-outlined">history</span>
      </button>
      <button class="fab-primary">
        <span class="material-symbols-outlined">schema</span>
      </button>
    </div>

    <el-dialog v-model="showDeptDialog" title="编辑部门" width="400px">
      <el-form>
        <el-form-item label="名称"><el-input v-model="deptForm.name" /></el-form-item>
        <el-form-item label="负责人"><el-input v-model="deptForm.leader" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDeptDialog = false">取消</el-button>
        <el-button type="primary" @click="saveDept">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

const showDeptDialog = ref(false)
const deptForm = reactive({ id: 0, name: '', leader: '' })
const loading = ref(false)
const deptTree = ref<any[]>([])

const sampleMembers = ref([
  { name: '李博文', email: 'bowen.li@nexus.com', role: '首席技术官 (CTO)', status: 'online', lastSeen: '2 mins ago', color: '#3370FF' },
  { name: '张晓雅', email: 'xiaoya.z@nexus.com', role: '财务总监', status: 'busy', lastSeen: '15 mins ago', color: '#10B981' },
  { name: '王海峰', email: 'haifeng.w@nexus.com', role: '产品运营总监', status: 'offline', lastSeen: '2 hours ago', color: '#8B5CF6' },
  { name: '刘思纯', email: 'sichun.l@nexus.com', role: '人力资源总监', status: 'online', lastSeen: 'Just now', color: '#FF8F1F' },
])

const totalMembers = computed(() => {
  let count = 0
  const walk = (nodes: any[]) => {
    nodes.forEach(n => {
      count += n.memberCount || 0
      if (n.children) walk(n.children)
    })
  }
  walk(deptTree.value)
  return count || 1284
})

onMounted(() => fetchDeptTree())

function fetchDeptTree() {
  loading.value = true
  request.get('/org/dept/tree', { params: { spaceId: 1 } }).then((res: any) => {
    if (res.code === 200) {
      deptTree.value = (res.data || []).map((d: any) => ({ ...d, expanded: true }))
    }
  }).catch(() => {}).finally(() => loading.value = false)
}

function addDept(_parentId: number | null) {
  deptForm.id = 0; deptForm.name = ''; deptForm.leader = ''
  showDeptDialog.value = true
}

function editDept(dept: { id: number; name: string }) {
  deptForm.id = dept.id; deptForm.name = dept.name; deptForm.leader = ''
  showDeptDialog.value = true
}

function saveDept() {
  if (deptForm.id) {
    request.put(`/org/dept/${deptForm.id}`, { deptName: deptForm.name }).then((res: any) => {
      if (res.code === 200) { showDeptDialog.value = false; fetchDeptTree(); ElMessage.success('已保存') }
    }).catch(() => ElMessage.error('保存失败'))
  } else {
    request.post('/org/dept', { name: deptForm.name }).then((res: any) => {
      if (res.code === 200) { showDeptDialog.value = false; fetchDeptTree(); ElMessage.success('已创建') }
    }).catch(() => ElMessage.error('创建失败'))
  }
}

async function deleteDept(dept: any) {
  await ElMessageBox.confirm(`确定删除 ${dept.deptName || dept.name}？`, '删除', { type: 'warning' })
  request.delete(`/org/dept/${dept.deptId || dept.id}`).then((res: any) => {
    if (res.code === 200) { fetchDeptTree(); ElMessage.success('已删除') }
  }).catch(() => ElMessage.error('删除失败'))
}
</script>

<style lang="scss" scoped>
.org-page {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: var(--c-bg);
}

/* Top Bar */
.top-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 2rem;
  height: 56px;
  border-bottom: 1px solid var(--c-border);
  background: var(--c-surface);
  position: sticky;
  top: 0;
  z-index: 10;
}

.breadcrumb {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  font-weight: 500;
  color: var(--c-text-secondary);

  .material-symbols-outlined { font-size: 16px; }
}

.breadcrumb .current {
  color: var(--c-brand);
  font-weight: 600;
}

.top-actions {
  display: flex;
  gap: 0.75rem;
}

/* Org Body */
.org-body {
  display: flex;
  flex: 1;
  overflow: hidden;
}

/* Tree Panel */
.tree-panel {
  width: 400px;
  border-right: 1px solid var(--c-border);
  display: flex;
  flex-direction: column;
  background: var(--c-surface-dim);
  flex-shrink: 0;
}

.tree-toolbar {
  padding: 1rem;
  border-bottom: 1px solid var(--c-border);
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.search-box {
  position: relative;
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
  padding: 0.5rem 0.75rem 0.5rem 36px;
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

.tree-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.tree-panel-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--c-text);
  margin: 0;
}

.btn-add-dept {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 8px;
  border: none;
  background: transparent;
  color: var(--c-brand);
  font-size: 12px;
  cursor: pointer;
  border-radius: 4px;

  &:hover { background: rgba(51,112,255,.06); }

  .material-symbols-outlined { font-size: 20px; }
}

/* Tree Scroll */
.tree-scroll {
  flex: 1;
  overflow-y: auto;
  padding: 1rem;

  &::-webkit-scrollbar { width: 4px; }
  &::-webkit-scrollbar-thumb { background: var(--c-border); border-radius: 4px; }
}

.tree-root {
  display: flex;
  flex-direction: column;
}

.tree-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0.5rem 0.75rem;
  border-radius: var(--radius-lg, 0.5rem);
  cursor: pointer;
  transition: background 0.15s;

  &:hover { background: var(--c-surface-dim); }

  &.active {
    background: rgba(51,112,255,.08);
    border-left: 4px solid var(--c-brand);
    padding-left: calc(0.75rem - 4px);
  }
}

.tree-arrow {
  font-size: 20px;
  color: var(--c-text-ghost);
  flex-shrink: 0;
  cursor: pointer;
}

.tree-node-icon {
  font-size: 20px;
  color: var(--c-text-muted);
  flex-shrink: 0;
}

.tree-node-name {
  flex: 1;
  font-size: 14px;
  color: var(--c-text);
}

.tree-node-count {
  font-size: 12px;
  color: var(--c-text-muted);
  white-space: nowrap;
}

.tree-node-actions {
  display: flex;
  gap: 2px;
  opacity: 0;
  transition: opacity 0.15s;
}

.tree-item:hover .tree-node-actions { opacity: 1; }

.action-icon {
  font-size: 18px;
  color: var(--c-text-ghost);
  padding: 2px;
  border-radius: 4px;
  cursor: pointer;

  &:hover { color: var(--c-brand); background: rgba(51,112,255,.06); }

  &.danger:hover { color: var(--c-danger); background: rgba(255,59,48,.06); }
}

.tree-children {
  margin-left: 1.5rem;
  border-left: 1px solid var(--c-border);
  padding-left: 1rem;
}

/* Detail Panel */
.detail-panel {
  flex: 1;
  overflow-y: auto;
  background: var(--c-surface);
}

.detail-header {
  padding: 2rem;
  border-bottom: 1px solid var(--c-border);
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.dept-hero {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.dept-hero-icon {
  width: 64px;
  height: 64px;
  border-radius: 1rem;
  background: rgba(51,112,255,.06);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--c-brand);
  flex-shrink: 0;

  .material-symbols-outlined { font-size: 40px; }
}

.dept-hero-name {
  font-size: 32px;
  font-weight: 700;
  color: var(--c-text);
  margin: 0 0 0.5rem;
}

.dept-hero-meta {
  display: flex;
  align-items: center;
  gap: 1.5rem;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--c-text-secondary);

  .material-symbols-outlined { font-size: 18px; }
}

.meta-highlight {
  font-weight: 600;
  color: var(--c-brand);
  cursor: pointer;

  &:hover { text-decoration: underline; }
}

.meta-mono {
  font-family: 'JetBrains Mono Variable', monospace;
  font-size: 13px;
}

.dept-actions {
  display: flex;
  gap: 0.5rem;
}

.btn-icon {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--c-border);
  border-radius: var(--radius-lg, 0.5rem);
  background: transparent;
  color: var(--c-text-ghost);
  cursor: pointer;
  transition: color 0.15s, background 0.15s;

  &:hover { color: var(--c-brand); background: rgba(51,112,255,.04); }

  .material-symbols-outlined { font-size: 20px; }
}

/* Summary Grid */
.summary-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 1.5rem;
  padding: 1.5rem 2rem;
  border-bottom: 1px solid var(--c-border);
}

.summary-card {
  padding: 1rem;
  background: var(--c-bg);
  border: 1px solid var(--c-border);
  border-radius: var(--radius-xl, 0.75rem);

  &:hover { border-color: rgba(51,112,255,.3); }
}

.summary-label {
  font-size: 12px;
  color: var(--c-text-muted);
  margin-bottom: 0.25rem;
}

.summary-value {
  font-size: 24px;
  line-height: 32px;
  font-weight: 700;
  color: var(--c-text);
}

.summary-trend {
  display: flex;
  align-items: center;
  gap: 2px;
  font-size: 10px;
  margin-top: 0.5rem;

  &.up { color: var(--c-success); }

  .material-symbols-outlined { font-size: 12px; }
}

.summary-bar {
  width: 100%;
  height: 4px;
  background: var(--c-border);
  border-radius: 2px;
  overflow: hidden;
  margin-top: 0.5rem;
}

.summary-bar-fill {
  height: 100%;
  background: var(--c-brand);
  border-radius: 2px;
}

.summary-meta {
  font-size: 10px;
  color: var(--c-text-secondary);
  margin-top: 0.5rem;
}

/* Member Section */
.member-section {
  padding: 0 2rem 2rem;
}

.member-section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1rem 0;
}

.section-title {
  font-size: 20px;
  font-weight: 600;
  color: var(--c-text);
  margin: 0;
}

.member-actions {
  display: flex;
  gap: 0.5rem;
}

/* Member Table */
.member-table-wrap {
  overflow-x: auto;
}

.member-table {
  width: 100%;
  border-collapse: collapse;

  th {
    padding: 1rem 1rem;
    font-size: 12px;
    font-weight: 500;
    color: var(--c-text-muted);
    text-align: left;
    border-bottom: 1px solid var(--c-border);
    text-transform: uppercase;
    letter-spacing: 0.02em;
  }

  td {
    padding: 1rem;
    border-bottom: 1px solid var(--c-border-light, #E5E6EB);
  }

  tbody tr {
    transition: background 0.15s;

    &:hover { background: var(--c-surface-dim); }
  }
}

.member-cell {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.member-avatar-xs {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  position: relative;
  flex-shrink: 0;
  border: 1px solid var(--c-border);
}

.presence-dot {
  position: absolute;
  bottom: -1px;
  right: -1px;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  border: 2px solid #fff;

  &.online { background: var(--c-success); }
  &.busy { background: var(--c-danger); }
  &.offline { background: var(--c-text-ghost); }
}

.member-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--c-text);
}

.member-email {
  font-size: 10px;
  color: var(--c-text-muted);
}

.text-muted { font-size: 14px; color: var(--c-text-secondary); }
.text-mono { font-size: 13px; font-family: 'JetBrains Mono Variable', monospace; color: var(--c-text-secondary); }
.text-right { text-align: right; }

.status-badge-sm {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 10px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.04em;

  &.online { background: rgba(34,197,94,.1); color: var(--c-success); }
  &.busy { background: rgba(255,59,48,.1); color: var(--c-danger); }
  &.offline { background: rgba(148,163,184,.1); color: var(--c-text-ghost); }
}

.btn-icon-sm {
  background: none;
  border: none;
  color: var(--c-text-ghost);
  cursor: pointer;
  padding: 2px;

  &:hover { color: var(--c-brand); }

  .material-symbols-outlined { font-size: 20px; }
}

/* Buttons (shared) */
.btn-outline-sm {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 12px;
  border: 1px solid var(--c-border);
  border-radius: var(--radius-lg, 0.5rem);
  background: transparent;
  font-size: 12px;
  font-weight: 500;
  color: var(--c-text-secondary);
  cursor: pointer;

  &:hover { background: var(--c-surface-dim); }

  .material-symbols-outlined { font-size: 18px; }
}

.btn-primary-sm {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 12px;
  background: var(--c-brand);
  color: #fff;
  border: none;
  border-radius: var(--radius-lg, 0.5rem);
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;

  &:hover { opacity: 0.9; }
  &:active { transform: scale(0.98); }

  .material-symbols-outlined { font-size: 18px; }
}

/* FAB Group */
.fab-group {
  position: fixed;
  bottom: 2rem;
  right: 2rem;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  z-index: 50;
}

.fab-secondary {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: var(--c-surface);
  border: 1px solid var(--c-border);
  color: var(--c-brand);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(0,0,0,.08);
  transition: transform 0.2s;

  &:hover { transform: scale(1.1); }
}

.fab-primary {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: var(--c-brand);
  border: none;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: 0 6px 20px rgba(0,0,0,.18);
  transition: transform 0.2s;

  &:hover { transform: scale(1.1); }
  &:active { transform: scale(0.95); }

  .material-symbols-outlined { font-size: 28px; }
}
</style>
