<template>
  <div class="notif-page">
    <div class="page-header">
      <div class="header-left">
        <h2 class="t-h2" style="color: var(--c-text)">通知中心</h2>
        <span v-if="unreadCount > 0" class="unread-count-badge">{{ unreadCount }} 条未读</span>
      </div>
      <button class="mark-all-btn" :disabled="unreadCount === 0" @click="handleMarkAllRead">
        <span class="material-symbols-outlined">done_all</span>
        全部已读
      </button>
    </div>

    <!-- Filter tabs -->
    <div class="filter-row">
      <el-radio-group v-model="filter" size="small">
        <el-radio-button value="all">全部</el-radio-button>
        <el-radio-button value="collab">协作</el-radio-button>
        <el-radio-button value="system">系统</el-radio-button>
        <el-radio-button value="task">任务</el-radio-button>
        <el-radio-button value="meeting">会议</el-radio-button>
      </el-radio-group>
    </div>

    <!-- Notification list -->
    <div class="notif-list" v-loading="loading">
      <div
        v-for="n in filteredNotifs"
        :key="n.id"
        class="notif-item"
        :class="{ unread: !n.isRead }"
        @click="openNotif(n)"
      >
        <div class="notif-icon" :style="{ background: typeBg(n.type) }">
          <span class="material-symbols-outlined" :style="{ color: typeColor(n.type), fontSize: '20px' }">
            {{ typeIcon(n.type) }}
          </span>
        </div>
        <div class="notif-body">
          <div class="notif-title">{{ n.title }}</div>
          <div class="notif-content t-body-md">{{ n.content }}</div>
          <div class="t-label-md notif-time">{{ String(n.sendTime||'').replace('T',' ').slice(0,16) }}</div>
        </div>
        <div class="notif-status">
          <span v-if="!n.isRead" class="unread-dot" title="未读" />
          <el-button circle title="删除通知" @click.stop="deleteNotif(n.id)"><span class="material-symbols-outlined">delete</span></el-button>
        </div>
      </div>

      <div v-if="filteredNotifs.length === 0" class="empty-state">
        <span class="material-symbols-outlined empty-icon">notifications_off</span>
        <p class="t-body-md" style="color: var(--c-text-secondary); font-weight: 500">暂无通知</p>
        <p class="t-label-md" style="color: var(--c-text-muted)">当有新的协作或系统通知时，会显示在这里</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {useRouter} from 'vue-router'
import { getNotificationList, markAsRead, markAllRead as apiMarkAllRead } from '@/api/notification'
import request from '@/utils/request'

const filter = ref('all')
const router=useRouter()
const loading = ref(false)
const notifs = ref<any[]>([])

onMounted(() => fetchNotifs())

function fetchNotifs() {
  loading.value = true
  getNotificationList().then((res: any) => {
    if (res.code === 200) notifs.value = res.data?.records || res.data || []
  }).catch(() => {}).finally(() => loading.value = false)
}

const unreadCount = computed(() => notifs.value.filter(n => !n.isRead).length)

const filteredNotifs = computed(() => {
  if (filter.value === 'all') return notifs.value
  return notifs.value.filter(n => typeName(n.type) === filter.value)
})
function typeName(type:number|string){return ({1:'system',2:'collab',3:'task',4:'meeting'} as Record<string,string>)[String(type)]||String(type)}

function typeIcon(type: string) {
  return { collab: 'description', system: 'settings', task: 'check_circle', meeting: 'event' }[typeName(type)] || 'notifications'
}

function typeColor(type: string) {
  return { collab: 'var(--c-brand)', system: 'var(--c-text-muted)', task: 'var(--c-warning)', meeting: 'var(--c-success)' }[typeName(type)] || 'var(--c-text-muted)'
}

function typeBg(type: string) {
  return { collab: 'var(--c-brand-light)', system: 'var(--c-surface-dim)', task: 'var(--c-warning-light)', meeting: 'var(--c-success-light)' }[typeName(type)] || 'var(--c-bg)'
}

function openNotif(n: any) {
  if (!n.isRead) {
    markAsRead(n.id).then(() => {
      n.isRead = true
    }).catch(() => {})
  }
  if(n.relatedId && n.type===3)router.push('/tasks?task='+n.relatedId)
  else if(n.relatedId && n.type===4)router.push('/meetings?meeting='+n.relatedId)
  else if(n.relatedId && n.type===2)request.get('/document/'+n.relatedId).then((r:any)=>router.push((({2:'/sheets/',3:'/mindmap/',4:'/whiteboard/'} as Record<number,string>)[r.data.type]||'/documents/')+n.relatedId)).catch(()=>{})
}
async function deleteNotif(id:string){try{await request.delete('/notification/'+id);notifs.value=notifs.value.filter(n=>n.id!==id)}catch{}}

function handleMarkAllRead() {
  apiMarkAllRead().then(() => {
    notifs.value.forEach(n => n.isRead = true)
    ElMessage.success('全部已读')
  }).catch(() => ElMessage.error('操作失败'))
}
</script>

<style lang="scss" scoped>
.notif-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.header-left { display: flex; align-items: center; gap: 12px; }

.unread-count-badge {
  font-size: 12px; font-weight: 500; color: var(--c-brand);
  background: var(--c-brand-light); padding: 2px 10px; border-radius: var(--radius-full);
}

.mark-all-btn {
  display: flex; align-items: center; gap: 4px;
  padding: 6px 14px;
  border: 1px solid var(--c-border); border-radius: var(--radius-lg);
  background: var(--c-surface); color: var(--c-text-secondary);
  font-size: 13px; font-weight: 500; cursor: pointer;
  transition: all .15s var(--ease-out);
  &:hover:not(:disabled) { border-color: var(--c-brand); color: var(--c-brand); background: var(--c-brand-light); }
  &:disabled { opacity: .4; cursor: not-allowed; }
  .material-symbols-outlined { font-size: 18px; }
}

// -- Notification list card --
.notif-list {
  background: var(--c-surface);
  border: 1px solid var(--c-border);
  border-radius: var(--radius-xl);
  overflow: hidden;
}

.notif-item {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 16px 20px;
  border-bottom: 1px solid var(--c-border-light);
  cursor: pointer;
  transition: background .15s var(--ease-out);

  &:hover { background: var(--c-surface-hover); }
  &.unread {
    background: linear-gradient(135deg, var(--c-brand-light), transparent);
    .notif-title { font-weight: 600; }
  }
  &:last-child { border-bottom: 0; }
}

.notif-icon {
  width: 40px; height: 40px;
  border-radius: var(--radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.notif-body { flex: 1; min-width: 0; }
.notif-title { font-size: 14px; font-weight: 500; color: var(--c-text); margin-bottom: 2px; }
.notif-content { color: var(--c-text-secondary); }
.notif-time { color: var(--c-text-muted); margin-top: 4px; }

.notif-status { flex-shrink: 0; padding-top: 4px; }
.unread-dot {
  width: 8px; height: 8px;
  border-radius: 50%;
  background: var(--c-brand);
  display: block;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  gap: 8px;
}
.empty-icon { font-size: 56px; color: var(--c-text-ghost); margin-bottom: 4px; }
</style>
