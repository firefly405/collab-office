<template>
  <div class="group-manage-page">
    <!-- Three-column grid layout -->
    <div class="columns-grid">
      <!-- LEFT: Group Config + Quick Stats -->
      <aside class="left-col">
        <!-- Group Config Card -->
        <div class="card config-card">
          <h3 class="card-title">群组配置</h3>

          <!-- Group Avatar -->
          <div class="avatar-section">
            <div class="avatar-wrapper" @click="isOwner && (($refs.avatarUpload as any)?.$el)?.click()">
              <div class="avatar-circle">
                <span class="material-symbols-outlined avatar-icon">groups</span>
              </div>
              <div class="avatar-overlay">
                <span class="material-symbols-outlined">photo_camera</span>
              </div>
            </div>
            <span class="avatar-hint">点击更换头像</span>
            <el-upload
              v-if="isOwner"
              ref="avatarUpload"
              action="#"
              :show-file-list="false"
              accept="image/*"
              class="avatar-upload-hidden"
            >
              <el-avatar :size="64" shape="square">{{ group.name?.[0] }}</el-avatar>
            </el-upload>
          </div>

          <!-- Config Form -->
          <div class="config-form">
            <div class="form-group">
              <label class="form-label">群名称</label>
              <el-input
                v-model="group.name"
                :disabled="!isOwner"
                class="form-input"
                placeholder="输入群名称"
              />
            </div>

            <div class="form-group">
              <label class="form-label">群公告</label>
              <el-input
                v-model="newNotice"
                type="textarea"
                :rows="4"
                class="form-textarea"
                placeholder="输入群公告内容..."
                :disabled="!isOwner && !isAdmin"
              />
            </div>

            <div class="form-group">
              <label class="form-label">入群权限</label>
              <el-select
                v-model="joinPermission"
                class="form-select"
                :disabled="!isOwner"
              >
                <el-option label="无需审批" value="open" />
                <el-option label="管理员审批" value="approval" />
                <el-option label="禁止入群" value="forbidden" />
              </el-select>
            </div>

            <el-button
              v-if="isOwner"
              type="primary"
              class="save-btn"
              @click="saveInfo"
            >
              保存基本信息
            </el-button>

            <div v-if="isOwner || isAdmin" class="notice-actions" style="margin-top: 8px;">
              <el-button
                v-if="newNotice.trim()"
                type="primary"
                size="small"
                class="publish-btn"
                @click="publishNotice"
              >
                发布公告
              </el-button>
            </div>
          </div>
        </div>

        <!-- Quick Statistics Card -->
        <div class="card stats-card">
          <h4 class="stats-title">活跃度概览</h4>
          <div class="stats-grid">
            <div class="stat-item">
              <div class="stat-value">128</div>
              <div class="stat-label">今日消息</div>
            </div>
            <div class="stat-item">
              <div class="stat-value stat-online">12</div>
              <div class="stat-label">当前在线</div>
            </div>
          </div>
        </div>

        <!-- Danger Zone -->
        <div class="danger-zone">
          <el-button
            v-if="isOwner"
            type="danger"
            text
            class="danger-btn"
            @click="handleDismiss"
          >
            <span class="material-symbols-outlined">delete_forever</span>
            解散群组
          </el-button>
          <el-button
            v-else
            type="warning"
            text
            class="danger-btn"
            @click="handleLeave"
          >
            <span class="material-symbols-outlined">logout</span>
            退出群组
          </el-button>
        </div>
      </aside>

      <!-- CENTER: Member Management -->
      <section class="center-col">
        <div class="card member-card">
          <!-- Header with Search and Actions -->
          <div class="member-header">
            <div class="member-header-top">
              <h3 class="card-title">
                成员管理
                <span class="member-count">({{ group.members?.length || 0 }}人)</span>
              </h3>
              <div class="header-actions">
                <el-button
                  v-if="isOwner"
                  class="batch-remove-btn"
                  @click="batchRemove"
                >
                  <span class="material-symbols-outlined">person_remove</span>
                  批量移除
                </el-button>
              </div>
            </div>
            <div class="search-box">
              <span class="material-symbols-outlined search-icon">search</span>
              <input
                v-model="memberSearch"
                class="search-input"
                type="text"
                placeholder="搜索成员、职位或角色..."
              />
            </div>
          </div>

          <!-- Member Table -->
          <div class="member-table-wrapper">
            <el-table
              :data="filteredMembers"
              row-key="userId"
              class="member-table"
              @selection-change="onSelectionChange"
            >
              <el-table-column type="selection" width="44" />
              <el-table-column label="成员姓名" min-width="200">
                <template #default="{ row }">
                  <div class="member-cell">
                    <div class="member-avatar-wrapper">
                      <el-avatar
                        :size="40"
                        :style="{ backgroundColor: row.color || '#3370FF' }"
                        class="member-avatar"
                      >
                        {{ row.userName?.[0] }}
                      </el-avatar>
                      <span
                        class="presence-dot"
                        :class="row.online ? 'online' : 'offline'"
                      ></span>
                    </div>
                    <div class="member-info">
                      <div class="member-name">
                        {{ row.userName }}
                        <span v-if="row.userId === currentUserId" class="me-tag">(我)</span>
                      </div>
                      <div class="member-title">{{ row.title || row.role || '--' }}</div>
                    </div>
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="所属部门" width="140">
                <template #default="{ row }">
                  <span class="dept-text">{{ row.department || '--' }}</span>
                </template>
              </el-table-column>
              <el-table-column label="群角色" width="120">
                <template #default="{ row }">
                  <el-select
                    v-if="isOwner && row.userId !== currentUserId"
                    :model-value="row.role"
                    size="small"
                    @change="(v: string) => changeRole(row, v)"
                    class="role-select"
                  >
                    <el-option label="管理员" value="ADMIN" />
                    <el-option label="成员" value="MEMBER" />
                  </el-select>
                  <span
                    v-else
                    class="role-badge"
                    :class="'role-' + (row.role || 'MEMBER').toLowerCase()"
                  >
                    {{ row.role === 'OWNER' ? '群主' : row.role === 'ADMIN' ? '管理员' : '成员' }}
                  </span>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="80" align="right">
                <template #default="{ row }">
                  <div class="action-cell">
                    <el-button
                      v-if="isOwner && row.userId !== currentUserId"
                      text
                      type="danger"
                      size="small"
                      @click="removeMember(row)"
                      class="action-btn"
                    >
                      <span class="material-symbols-outlined">person_remove</span>
                    </el-button>
                    <button class="more-btn" @click.stop>
                      <span class="material-symbols-outlined">more_vert</span>
                    </button>
                  </div>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </div>
      </section>

      <!-- RIGHT: Invite Members -->
      <aside class="right-col">
        <div class="card invite-card">
          <div class="invite-header">
            <h3 class="card-title">邀请成员</h3>
            <div class="search-box">
              <span class="material-symbols-outlined search-icon">search</span>
              <input
                v-model="inviteSearch"
                class="search-input"
                type="text"
                placeholder="搜索同事或部门..."
                @input="onInviteSearch"
              />
            </div>
          </div>

          <!-- Org Tree View -->
          <div class="tree-view">
            <div class="tree-node-group">
              <!-- Node 1: Nexus Office HQ -->
              <div class="tree-node parent expanded">
                <span class="material-symbols-outlined tree-arrow">chevron_right</span>
                <span class="material-symbols-outlined tree-icon corp">corporate_fare</span>
                <span class="tree-label bold">Nexus Office 总部</span>
              </div>
              <div class="tree-children">
                <div class="tree-node parent expanded">
                  <span class="material-symbols-outlined tree-arrow">chevron_right</span>
                  <span class="material-symbols-outlined tree-icon folder-open">folder_open</span>
                  <span class="tree-label">产品研发部</span>
                  <span class="tree-count">(24)</span>
                </div>
                <div class="tree-children">
                  <label class="tree-node leaf">
                    <input type="checkbox" class="tree-checkbox" />
                    <span class="tree-label">产品一组</span>
                  </label>
                  <label class="tree-node leaf">
                    <input type="checkbox" class="tree-checkbox" />
                    <span class="tree-label">技术二组</span>
                  </label>
                </div>
                <div class="tree-node parent">
                  <span class="material-symbols-outlined tree-arrow">chevron_right</span>
                  <span class="material-symbols-outlined tree-icon folder">folder</span>
                  <span class="tree-label">体验设计中心</span>
                  <span class="tree-count">(12)</span>
                </div>
                <div class="tree-node parent">
                  <span class="material-symbols-outlined tree-arrow">chevron_right</span>
                  <span class="material-symbols-outlined tree-icon folder">folder</span>
                  <span class="tree-label">市场运营部</span>
                  <span class="tree-count">(45)</span>
                </div>
              </div>
            </div>
            <!-- Node 2: Shanghai Office -->
            <div class="tree-node parent">
              <span class="material-symbols-outlined tree-arrow">chevron_right</span>
              <span class="material-symbols-outlined tree-icon location">location_on</span>
              <span class="tree-label bold">上海办事处</span>
            </div>
          </div>

          <!-- Search Results (from existing invite logic) -->
          <div v-if="inviteList.length" class="invite-results">
            <div
              v-for="u in inviteList"
              :key="u.userId"
              class="invite-result-item"
            >
              <span class="t-body-md">{{ u.userName || u.realName }}</span>
              <el-button size="small" type="primary" @click="addMember(u)">邀请</el-button>
            </div>
          </div>

          <!-- Invite Footer -->
          <div class="invite-footer">
            <div class="invite-footer-row">
              <span class="selected-info">
                已选: <span class="selected-count">{{ selectedDepts }}个部门</span>
              </span>
              <button class="clear-btn" @click="clearSelection">清除全部</button>
            </div>
            <el-button
              type="primary"
              class="send-invite-btn"
              @click="onSendInvite"
            >
              发送邀请通知
            </el-button>
          </div>
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

const route = useRoute()
const router = useRouter()
const groupId = ref(route.params.groupId as string || '')
const showInvite = ref(false)
const inviteSearch = ref('')
const inviteList = ref<any[]>([])
const newNotice = ref('')
const currentUserId = ref('')
const memberSearch = ref('')
const joinPermission = ref('approval')
const selectedMembers = ref<any[]>([])
const selectedDepts = ref(0)

const group = ref<any>({
  name: '加载中...',
  description: '',
  notice: '',
  noticeTime: '',
  members: [],
})

const isOwner = ref(false)
const isAdmin = ref(false)

const filteredMembers = computed(() => {
  if (!memberSearch.value.trim()) return group.value.members || []
  const kw = memberSearch.value.toLowerCase()
  return (group.value.members || []).filter((m: any) => {
    const text = (m.userName + ' ' + (m.title || '') + ' ' + (m.department || '') + ' ' + (m.role || '')).toLowerCase()
    return text.includes(kw)
  })
})

onMounted(() => {
  loadGroup()
  try {
    const token = localStorage.getItem('access_token')
    if (token) {
      const payload = JSON.parse(atob(token.split('.')[1]))
      currentUserId.value = payload.userId || payload.sub || ''
    }
  } catch { /* ignore */ }
})

function loadGroup() {
  if (!groupId.value) return
  request.get(`/chat/session/${groupId.value}`).then((res: any) => {
    if (res.code === 200 && res.data) {
      group.value = {
        ...group.value,
        name: res.data.sessionName || res.data.name || '群组',
        description: res.data.description || '',
        notice: res.data.notice || '',
        noticeTime: res.data.noticeTime || '',
        members: (res.data.members || []).map((m: any) => ({
          userId: m.userId,
          userName: m.userName || m.nickname || '--',
          role: m.role || 'MEMBER',
          joinedTime: m.joinedTime || '--',
          title: m.title || '',
          department: m.department || '',
          online: m.online || false,
          color: ['#3370FF', '#10B981', '#8B5CF6', '#FF8F1F'][Math.floor(Math.random() * 4)],
        })),
      }
      const me = group.value.members.find((m: any) => m.userId === currentUserId.value)
      if (me) {
        isOwner.value = me.role === 'OWNER'
        isAdmin.value = me.role === 'ADMIN' || me.role === 'OWNER'
      }
    }
  }).catch(() => {})
}

function saveInfo() {
  request.put(`/chat/session/${groupId.value}`, {
    sessionName: group.value.name,
    description: group.value.description,
  }).then((res: any) => {
    if (res.code === 200) ElMessage.success('已保存')
  }).catch(() => ElMessage.error('保存失败'))
}

function changeRole(member: any, newRole: string) {
  request.put(`/chat/session/${groupId.value}/members/${member.userId}/role`, { role: newRole }).then((res: any) => {
    if (res.code === 200) { member.role = newRole; ElMessage.success('角色已更新') }
  }).catch(() => ElMessage.error('更新失败'))
}

function removeMember(member: any) {
  ElMessageBox.confirm(`确认移除 ${member.userName}？`, '移除成员', { type: 'warning' }).then(() => {
    request.delete(`/chat/session/${groupId.value}/members/${member.userId}`).then((res: any) => {
      if (res.code === 200) { group.value.members = group.value.members.filter((m: any) => m.userId !== member.userId) }
    }).catch(() => ElMessage.error('移除失败'))
  }).catch(() => {})
}

function handleLeave() {
  ElMessageBox.confirm('确认退出该群组？', '退出', { type: 'warning' }).then(() => {
    request.post(`/chat/session/${groupId.value}/leave`).then(() => {
      ElMessage.success('已退出'); router.push('/im')
    }).catch(() => ElMessage.error('退出失败'))
  }).catch(() => {})
}

function handleDismiss() {
  ElMessageBox.confirm('解散后所有聊天记录将被清除，不可恢复。确认解散？', '解散群组', { type: 'error' }).then(() => {
    request.delete(`/chat/session/${groupId.value}`).then(() => {
      ElMessage.success('已解散'); router.push('/im')
    }).catch(() => ElMessage.error('解散失败'))
  }).catch(() => {})
}

function publishNotice() {
  if (!newNotice.value.trim()) return
  request.put(`/chat/session/${groupId.value}/notice`, { content: newNotice.value }).then((res: any) => {
    if (res.code === 200) { group.value.notice = newNotice.value; newNotice.value = ''; ElMessage.success('已发布') }
  }).catch(() => ElMessage.error('发布失败'))
}

function addMember(user: any) {
  request.post(`/chat/session/${groupId.value}/members`, { userId: user.userId }).then((res: any) => {
    if (res.code === 200) { ElMessage.success('已邀请'); showInvite.value = false; loadGroup() }
  }).catch(() => ElMessage.error('邀请失败'))
}

function onSelectionChange(selection: any[]) {
  selectedMembers.value = selection
}

function batchRemove() {
  if (!selectedMembers.value.length) {
    ElMessage.warning('请先选择成员')
    return
  }
  const names = selectedMembers.value.map((m: any) => m.userName).join('、')
  ElMessageBox.confirm(`确认移除 ${names}？`, '批量移除', { type: 'warning' }).then(() => {
    const ids = selectedMembers.value.map((m: any) => m.userId)
    Promise.all(ids.map((id: string) =>
      request.delete(`/chat/session/${groupId.value}/members/${id}`)
    )).then(() => {
      group.value.members = group.value.members.filter((m: any) => !ids.includes(m.userId))
      ElMessage.success('已移除')
    }).catch(() => ElMessage.error('移除失败'))
  }).catch(() => {})
}

function clearSelection() {
  selectedMembers.value = []
  selectedDepts.value = 0
}

function onSendInvite() {
  showInvite.value = true
}

function onInviteSearch() {
  if (!inviteSearch.value.trim()) {
    inviteList.value = []
    return
  }
  request.get('/chat/users/search', { params: { keyword: inviteSearch.value.trim() } }).then((res: any) => {
    if (res.code === 200) inviteList.value = res.data?.records || res.data || []
  }).catch(() => {})
}
</script>

<style lang="scss" scoped>
/* ===== CSS Variables (aligned with design system) ===== */
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
$c-border-light: #e5e6eb;
$c-error: #ba1a1a;
$c-error-light: #ffdad6;
$c-success: #22c55e;
$c-warning: #ef4444;
$radius-sm: 4px;
$radius: 8px;
$radius-lg: 12px;
$radius-xl: 16px;
$radius-full: 12px;

/* ===== Material Symbols base ===== */
.material-symbols-outlined {
  font-variation-settings: 'FILL' 0, 'wght' 400, 'GRAD' 0, 'opsz' 24;
  vertical-align: middle;
  font-size: 20px;
}

/* ===== Page Layout ===== */
.group-manage-page {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: $c-bg;
  overflow: hidden;
}

.columns-grid {
  display: grid;
  grid-template-columns: 280px 1fr 280px;
  gap: 16px;
  flex: 1;
  overflow: hidden;
  padding: 16px;
  max-width: 1400px;
  margin: 0 auto;
  width: 100%;
}

/* ===== Columns ===== */
.left-col {
  display: flex;
  flex-direction: column;
  gap: 12px;
  overflow-y: auto;
  padding-right: 4px;
}

.center-col {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.right-col {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* ===== Cards ===== */
.card {
  background: $c-surface;
  border: 1px solid $c-border;
  border-radius: $radius-xl;
  padding: 20px;
}

.card-title {
  font-size: 20px;
  font-weight: 600;
  line-height: 28px;
  color: $c-text;
  margin: 0 0 16px 0;
}

/* ===== Left Column: Config Card ===== */
.config-card {
  .avatar-section {
    display: flex;
    flex-direction: column;
    align-items: center;
    margin-bottom: 24px;
  }

  .avatar-wrapper {
    position: relative;
    cursor: pointer;
    margin-bottom: 8px;

    &:hover .avatar-overlay {
      opacity: 1;
    }
  }

  .avatar-circle {
    width: 96px;
    height: 96px;
    border-radius: 50%;
    background: #dbe1ff;
    display: flex;
    align-items: center;
    justify-content: center;
    border: 4px solid $c-surface;
    overflow: hidden;
  }

  .avatar-icon {
    font-size: 40px;
    color: $c-brand;
  }

  .avatar-overlay {
    position: absolute;
    inset: 0;
    border-radius: 50%;
    background: rgba(0, 0, 0, 0.2);
    display: flex;
    align-items: center;
    justify-content: center;
    opacity: 0;
    transition: opacity 0.2s;

    .material-symbols-outlined {
      color: #fff;
      font-size: 28px;
    }
  }

  .avatar-hint {
    font-size: 12px;
    color: $c-text-ghost;
  }

  .avatar-upload-hidden {
    display: none;
  }

  .config-form {
    display: flex;
    flex-direction: column;
    gap: 16px;
  }

  .form-group {
    display: flex;
    flex-direction: column;
    gap: 6px;
  }

  .form-label {
    font-size: 12px;
    font-weight: 600;
    color: $c-text-muted;
    letter-spacing: 0.01em;
  }

  .form-input,
  .form-textarea,
  .form-select {
    :deep(.el-input__wrapper),
    :deep(.el-textarea__inner) {
      background: $c-surface-low;
      border: none;
      border-radius: $radius;
      box-shadow: none;
    }

    :deep(.el-input__wrapper:focus),
    :deep(.el-textarea__inner:focus) {
      box-shadow: 0 0 0 2px rgba(0, 80, 214, 0.2);
    }
  }

  .save-btn {
    width: 100%;
    padding: 10px 0;
    background: $c-brand-container;
    color: #fff;
    border: none;
    border-radius: $radius;
    font-weight: 600;
    margin-top: 8px;

    &:hover {
      opacity: 0.9;
    }
  }
}

/* ===== Left Column: Stats Card ===== */
.stats-card {
  .stats-title {
    font-size: 12px;
    font-weight: 600;
    color: $c-text-ghost;
    margin: 0 0 12px 0;
    letter-spacing: 0.01em;
  }
}

.stats-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.stat-item {
  padding: 12px;
  background: $c-surface-low;
  border-radius: $radius;

  .stat-value {
    font-size: 20px;
    font-weight: 600;
    color: $c-brand;
    line-height: 28px;
  }

  .stat-value.stat-online {
    color: $c-success;
  }

  .stat-label {
    font-size: 12px;
    color: $c-text-secondary;
    margin-top: 2px;
  }
}

/* ===== Danger Zone ===== */
.danger-zone {
  margin-top: 4px;
  padding: 12px;
  background: $c-surface;
  border: 1px solid $c-border;
  border-radius: $radius-xl;
  display: flex;
  justify-content: center;
}

.danger-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;

  .material-symbols-outlined {
    font-size: 18px;
  }
}

/* ===== Center Column: Member Card ===== */
.member-card {
  display: flex;
  flex-direction: column;
  flex: 1;
  overflow: hidden;
  padding: 0;
}

.member-header {
  padding: 20px 20px 0 20px;
  border-bottom: 1px solid $c-border;
}

.member-header-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.member-count {
  font-size: 14px;
  font-weight: 400;
  color: $c-text-secondary;
  margin-left: 8px;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.batch-remove-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  color: $c-error;
  font-size: 12px;
  font-weight: 600;
  border: none;
  background: transparent;
  padding: 6px 12px;
  border-radius: $radius;
  cursor: pointer;
  transition: background 0.15s;

  &:hover {
    background: rgba(186, 26, 26, 0.08);
  }

  .material-symbols-outlined {
    font-size: 16px;
  }
}

.search-box {
  position: relative;
  margin-bottom: 16px;

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
    padding: 10px 12px 10px 40px;
    background: $c-surface-low;
    border: none;
    border-radius: $radius-lg;
    font-size: 14px;
    color: $c-text;
    outline: none;
    box-sizing: border-box;

    &:focus {
      box-shadow: 0 0 0 2px rgba(0, 80, 214, 0.2);
    }

    &::placeholder {
      color: $c-text-ghost;
    }
  }
}

/* ===== Member Table ===== */
.member-table-wrapper {
  flex: 1;
  overflow-y: auto;
  padding: 0 8px;

  .member-table {
    width: 100%;
    border: none;

    :deep(.el-table__header-wrapper) {
      position: sticky;
      top: 0;
      z-index: 10;
      background: $c-surface;
    }

    :deep(.el-table__header) th {
      background: $c-surface;
      color: $c-text-ghost;
      font-size: 12px;
      font-weight: 600;
      letter-spacing: 0.01em;
      border-bottom: 1px solid $c-border;
      padding: 14px 0;
    }

    :deep(.el-table__body) td {
      border-bottom: 1px solid rgba(229, 230, 235, 0.5);
      padding: 12px 0;
    }

    :deep(.el-table__row) {
      &:hover td {
        background: $c-surface-low;
      }
    }

    :deep(.el-checkbox__inner) {
      border-color: #c3c6d8;
      border-radius: 4px;
    }
  }
}

.member-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.member-avatar-wrapper {
  position: relative;
  flex-shrink: 0;
}

.member-avatar {
  border: 2px solid $c-surface;
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

  &.offline {
    background: #94a3b8;
  }
}

.member-info {
  min-width: 0;
}

.member-name {
  font-size: 14px;
  font-weight: 600;
  color: $c-text;
}

.me-tag {
  font-weight: 400;
  color: $c-text-secondary;
}

.member-title {
  font-size: 12px;
  color: $c-text-secondary;
}

.dept-text {
  font-size: 12px;
  color: $c-text-muted;
}

/* ===== Role Badges ===== */
.role-badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.05em;

  &.role-owner {
    background: rgba(0, 80, 214, 0.1);
    color: $c-brand;
  }

  &.role-admin {
    background: #dfe2eb;
    color: #61646c;
  }

  &.role-member {
    background: transparent;
    color: $c-text-secondary;
    border: 1px solid $c-border;
  }
}

.role-select {
  width: 100%;
}

/* ===== Action Cell ===== */
.action-cell {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 4px;
}

.more-btn {
  background: none;
  border: none;
  cursor: pointer;
  color: $c-text-ghost;
  padding: 4px;
  border-radius: 4px;

  &:hover {
    color: $c-brand;
  }

  .material-symbols-outlined {
    font-size: 20px;
  }
}

/* ===== Right Column: Invite Card ===== */
.invite-card {
  display: flex;
  flex-direction: column;
  flex: 1;
  overflow: hidden;
  padding: 0;
}

.invite-header {
  padding: 20px 20px 0 20px;
  border-bottom: 1px solid $c-border;

  .card-title {
    margin-bottom: 12px;
  }
}

/* ===== Tree View ===== */
.tree-view {
  flex: 1;
  overflow-y: auto;
  padding: 12px 16px;
}

.tree-node-group {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.tree-children {
  margin-left: 24px;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.tree-node {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 4px;
  border-radius: $radius;
  cursor: pointer;
  transition: background 0.15s;
  font-size: 14px;

  &:hover {
    background: $c-surface-low;
  }

  &.parent {
    .tree-label.bold {
      font-weight: 600;
    }
  }

  &.expanded .tree-arrow {
    transform: rotate(90deg);
  }

  &.leaf {
    padding: 4px 8px;
  }
}

.tree-arrow {
  font-size: 18px;
  color: $c-text-ghost;
  transition: transform 0.2s;
  flex-shrink: 0;
}

.tree-icon {
  font-size: 18px;
  flex-shrink: 0;

  &.corp {
    color: $c-brand;
  }

  &.folder-open {
    color: #565c65;
  }

  &.folder {
    color: #565c65;
  }

  &.location {
    color: $c-brand;
  }
}

.tree-label {
  flex: 1;
  color: $c-text;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;

  &.bold {
    font-weight: 600;
  }
}

.tree-count {
  font-size: 12px;
  color: $c-text-secondary;
  flex-shrink: 0;
}

.tree-checkbox {
  border-radius: 4px;
  border-color: #c3c6d8;
  accent-color: $c-brand;
  margin-right: 8px;
}

/* ===== Invite Search Results ===== */
.invite-results {
  padding: 0 20px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 160px;
  overflow-y: auto;
}

.invite-result-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 0;
  border-bottom: 1px solid $c-border;
}

/* ===== Invite Footer ===== */
.invite-footer {
  padding: 16px 20px;
  border-top: 1px solid $c-border;
  background: rgba(243, 244, 245, 0.5);
}

.invite-footer-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.selected-info {
  font-size: 12px;
  color: $c-text-secondary;
}

.selected-count {
  color: $c-brand;
  font-weight: 600;
}

.clear-btn {
  background: none;
  border: none;
  font-size: 12px;
  color: $c-brand;
  font-weight: 600;
  cursor: pointer;
  padding: 0;

  &:hover {
    text-decoration: underline;
  }
}

.send-invite-btn {
  width: 100%;
  padding: 10px 0;
  font-weight: 600;
  border-radius: $radius;
}

/* ===== Scrollbar ===== */
.left-col,
.member-table-wrapper,
.tree-view {
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
