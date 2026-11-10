<template>
  <div class="office-shell" :class="{'role-member':!identity.isAdmin&&!identity.isManager,'role-project':identity.isManager,'role-admin':identity.isAdmin,'is-dashboard':route.path==='/dashboard','is-messaging':route.path==='/im'}">
    <aside class="office-sidebar">
      <div class="office-brand"><span class="brand-symbol">N</span><strong>Nexus Office</strong></div>
      <div class="identity-label" :title="identity.roleName">{{ identity.roleName }}</div>
      <el-select :model-value="workspace.spaceId" @change="workspace.select" aria-label="当前团队" class="team-picker">
        <el-option v-for="s in workspace.spaces" :key="s.spaceId" :label="s.spaceName" :value="String(s.spaceId)" />
      </el-select>
      <nav>
        <template v-for="n in navigation" :key="n.path">
          <span v-if="n.path==='/admin' || n.path==='/documents'" class="home-nav-group">{{ n.path==='/admin'?'平台管理':'协作应用' }}</span>
          <router-link :to="n.path" :class="{selected: route.path === n.path || n.path !== '/dashboard' && route.path.startsWith(n.path + '/')}" :title="n.label" :aria-label="n.label">
            <span class="material-symbols-outlined" aria-hidden="true">{{ n.icon }}</span><span class="nav-label">{{ n.label }}</span>
          </router-link>
        </template>
      </nav>
      <div class="sidebar-bottom"><router-link to="/space/manage"><span class="material-symbols-outlined">group</span>{{ identity.isAdmin?'团队与权限':'团队成员' }}</router-link><router-link to="/settings"><span class="material-symbols-outlined">settings</span>个人设置</router-link></div>
    </aside>
    <div class="office-main">
      <header class="office-header">
        <el-select :model-value="workspace.spaceId" @change="workspace.select" aria-label="切换团队" class="mobile-team-picker">
          <el-option v-for="s in workspace.spaces" :key="s.spaceId" :label="s.spaceName" :value="String(s.spaceId)" />
        </el-select>
        <span class="home-header-label">{{ identity.isAdmin?'平台控制台':identity.isManager?'项目交付中心':'个人空间' }}<span class="current-page-label">/ {{ pageTitle }}</span></span>
        <span class="header-team">{{ workspace.spaces.find(s => String(s.spaceId) === workspace.spaceId)?.spaceName || '我的团队' }}</span>
        <el-input v-model="query" placeholder="搜索文档、任务与成员" clearable @keyup.enter="search" class="global-search"><template #prefix><span class="material-symbols-outlined">search</span></template></el-input>
        <el-button circle title="快捷操作" @click="openCommands"><span class="material-symbols-outlined">keyboard_command_key</span></el-button>
        <el-button circle title="站内通知" @click="router.push('/settings/notifications')"><span class="material-symbols-outlined">notifications</span></el-button>
        <el-dropdown><button class="profile-button" :title="username">{{ username.slice(0,1) }}</button><template #dropdown><el-dropdown-menu><el-dropdown-item @click="router.push('/settings')">{{ username }}</el-dropdown-item><el-dropdown-item divided @click="logout">退出登录</el-dropdown-item></el-dropdown-menu></template></el-dropdown>
      </header>
      <main class="office-content">
        <div v-if="!workspace.ready" class="office-empty">正在加载团队…</div>
        <template v-else-if="workspace.spaceId || route.path === '/space/manage' || route.path === '/settings'">
          <router-view :key="workspace.spaceId + route.fullPath" />
        </template>
        <el-empty v-else description="尚未加入团队"><el-button v-if="identity.isAdmin" type="primary" @click="router.push('/space/manage')">创建团队</el-button></el-empty>
      </main>
    </div>
    <el-dialog v-model="commandsOpen" title="快捷操作" width="540px" class="command-dialog"><form class="command-search" @submit.prevent="commandSearch"><el-input v-model="commandQuery" aria-label="快捷搜索" placeholder="搜索团队内容" autofocus/><el-button native-type="submit" circle title="搜索"><span class="material-symbols-outlined">search</span></el-button></form><div class="command-list"><button v-for="item in commands" :key="item.path" @click="navigate(item.path)"><span class="material-symbols-outlined">{{ item.icon }}</span>{{ item.label }}</button></div><h4 v-if="recent.length">最近访问</h4><div class="command-list"><button v-for="doc in recent" :key="doc.docId" @click="navigate(doc.type===2?'/sheets/'+doc.docId:doc.type===3?'/mindmap/'+doc.docId:doc.type===4?'/whiteboard/'+doc.docId:'/documents/'+doc.docId)"><span class="material-symbols-outlined">description</span>{{ doc.title }}</button></div></el-dialog>
  </div>
</template>
<script setup lang="ts">
import { onMounted,onBeforeUnmount,watch, ref } from 'vue'
import {computed} from 'vue'
import {useIdentityStore} from '@/stores/identity'
import { useRoute, useRouter } from 'vue-router'
import { useWorkspaceStore } from '@/stores/workspace'
import { removeToken, hasToken } from '@/utils/token'
import { wsClient } from '@/utils/wsClient'
import request from '@/utils/request'
import {preferences,loadPreferences} from '@/utils/preferences'
import '@/styles/office.css'
const route = useRoute(), router = useRouter(), workspace = useWorkspaceStore()
const identity=useIdentityStore()
const query = ref(''), username = ref('用户')
const commandsOpen=ref(false),commandQuery=ref(''),recent=ref<any[]>([]),commands=[{path:'/documents?create=1',label:'新建文档',icon:'note_add'},{path:'/tasks',label:'待办任务',icon:'task_alt'},{path:'/documents?view=favorites',label:'收藏夹',icon:'star'},{path:'/search',label:'全局搜索',icon:'search'}]
const recentKey=ref('')
async function openCommands(){commandsOpen.value=true;try{const u:any=await request.get('/user/info');recentKey.value=`recent-documents:${u.data.userId}:${workspace.spaceId}`;recent.value=JSON.parse(localStorage.getItem(recentKey.value)||'[]').slice(0,6)}catch{recent.value=[]}}
async function rememberDocument(){if(!/^\/(documents|sheets|mindmap|whiteboard)\/\d+$/.test(route.path))return;const docId=String(route.params.docId),space=workspace.spaceId;try{const [u,d]:any[]=await Promise.all([request.get('/user/info'),request.get('/document/'+docId)]);const key=`recent-documents:${u.data.userId}:${space}`,list=JSON.parse(localStorage.getItem(key)||'[]');localStorage.setItem(key,JSON.stringify([d.data,...list.filter((x:any)=>x.docId!==docId)].slice(0,12)))}catch{}}
watch(()=>route.path,rememberDocument)
function navigate(path:string){commandsOpen.value=false;router.push(path)}
function commandSearch(){if(commandQuery.value.trim())navigate('/search?q='+encodeURIComponent(commandQuery.value.trim()))}
function shortcut(event:KeyboardEvent){if((event.ctrlKey||event.metaKey)&&event.key.toLowerCase()===preferences.searchShortcut){event.preventDefault();openCommands()}}
onBeforeUnmount(()=>window.removeEventListener('keydown',shortcut))
const navigation = computed(()=>[
  ...(identity.isAdmin?[{path:'/admin',label:'系统管理',icon:'admin_panel_settings'}]:[]),
  {path:'/dashboard',label:identity.isAdmin?'管理总览':identity.isManager?'项目工作台':'工作台',icon:'dashboard'}, {path:'/im',label:'消息',icon:'chat'},
  {path:'/reports',label:identity.isManager?'汇报处理':'工作汇报',icon:'forum'},
  {path:'/documents',label:'云文档',icon:'description'}, {path:'/tasks',label:'任务',icon:'task_alt'},
  {path:'/files',label:'云盘',icon:'folder'}, {path:'/meetings',label:'会议安排',icon:'event'},
  {path:'/sheets',label:'表格',icon:'table_chart'}, {path:'/mindmap',label:'思维导图',icon:'account_tree'},
  {path:'/templates',label:'模板中心',icon:'dashboard_customize'}, {path:'/knowledge',label:'知识库',icon:'menu_book'},
  {path:'/statistics',label:'统计与导出',icon:'monitoring'},
  {path:'/whiteboard',label:'白板',icon:'draw'},
])
const pageTitle=computed(()=>navigation.value.find(n=>route.path===n.path||route.path.startsWith(n.path+'/'))?.label||(route.path.startsWith('/settings')?'个人设置':route.path==='/space/manage'?(identity.isAdmin?'团队与权限':'团队成员'):route.path==='/recycle'?'回收站':'工作空间'))
onMounted(async () => {
  window.addEventListener('keydown',shortcut)
  await workspace.load().catch(error => { if (hasToken()) throw error })
  if (!hasToken()) return
  await loadPreferences()
  const result:any = await request.get('/user/info')
  username.value = result.data.realName || result.data.username
  rememberDocument()
})
function search() { if(query.value.trim()) router.push('/search?q='+encodeURIComponent(query.value.trim())) }
async function logout() { try { await request.post('/auth/logout') } finally { wsClient.disconnect(); removeToken(); identity.reset(); workspace.select(''); router.push('/login') } }
</script>
<style scoped>
.mobile-team-picker { display:none; }
.identity-label{color:var(--c-brand-hover);background:var(--c-brand-light);font-size:var(--text-small);padding:6px 10px;margin:0 10px 12px;border-radius:var(--radius-md);text-align:center}
.home-nav-group,.home-header-label{display:none}
@media(max-width:520px){.identity-label{font-size:10px;padding:4px 0;margin:0 0 8px;overflow-wrap:anywhere}}
:global(.command-dialog.el-dialog){max-width:calc(100vw - 24px);max-height:85dvh;overflow:auto}.command-search{display:flex;gap:8px}.command-list{display:grid;gap:4px;margin-top:12px}.command-list button{display:flex;align-items:center;gap:10px;border:0;padding:10px;background:transparent;color:var(--c-text);text-align:left;cursor:pointer;overflow-wrap:anywhere}.command-list button:hover{background:var(--c-bg)}
@media(max-width:520px) { .mobile-team-picker { display:block; width:90px; flex-shrink:0; }.global-search { min-width:0; } }
.office-shell { display:flex; height:100vh; background:var(--c-surface); letter-spacing:0; }
.office-sidebar { width:208px; flex-shrink:0; background:var(--c-bg); border-right:1px solid var(--c-border); display:flex; flex-direction:column; padding:var(--space-6) var(--space-3); }
.office-brand { display:flex; align-items:center; gap:10px; padding:0 10px var(--space-6); font-size:17px; }
.brand-symbol { width:30px; height:30px; background:var(--c-brand); color:#fff; display:grid; place-items:center; border-radius:var(--radius-md); font-weight:700; }
.team-picker { width:100%; margin-bottom:var(--space-6); }
nav { display:grid; gap:5px; overflow-y:auto; min-height:0; }
.office-sidebar a { display:flex; align-items:center; gap:var(--space-3); color:var(--c-text-secondary); text-decoration:none; padding:10px 12px; border-radius:var(--radius-md); font-size:var(--text-body); transition:background .15s,color .15s; }
.office-sidebar a:hover { background:var(--c-surface-dim); }.office-sidebar a.selected,.office-sidebar a.router-link-exact-active { background:var(--c-brand-light); color:var(--c-brand-hover); font-weight:500; }
.office-sidebar a:focus-visible,.profile-button:focus-visible { outline:2px solid var(--c-brand); outline-offset:2px; }
.material-symbols-outlined { font-size:21px; }.sidebar-bottom { margin-top:auto; border-top:1px solid var(--c-border); padding-top:var(--space-3); }
.office-main { flex:1; min-width:0; min-height:0; display:flex; flex-direction:column; }.office-header { height:64px; border-bottom:1px solid var(--c-border-light); display:flex; align-items:center; gap:var(--space-4); padding:0 var(--space-8); flex-shrink:0; }
.header-team { font-size:var(--text-body); color:var(--c-text-secondary); overflow:hidden; text-overflow:ellipsis; white-space:nowrap; max-width:180px; }.global-search { width:300px; margin-left:auto; }
.profile-button { width:32px; height:32px; border:0; border-radius:50%; background:var(--c-success-light); color:#167448; cursor:pointer; }.office-content { flex:1; overflow:auto; padding:var(--space-6) var(--space-8); min-height:0; }
@media(max-width:800px) { .office-sidebar { width:160px; padding:16px 8px; }.office-brand { font-size:14px; padding-inline:0; }.office-header { padding:0 12px; gap:8px; }.header-team { display:none; }.global-search { width:180px; }.office-content { padding:16px; } }
@media(max-width:520px) { .office-sidebar { width:64px; padding:14px 6px; }.office-brand strong,.team-picker { display:none; }.office-brand { justify-content:center; padding-bottom:12px; }.office-sidebar a { font-size:0; padding:12px; justify-content:center; gap:0; }.office-sidebar a .material-symbols-outlined { font-size:22px; }.office-header { gap:8px; }.global-search { width:100%; }.office-content { padding:12px; } }

.current-page-label{font-size:12px;color:var(--c-text-muted);font-weight:400;margin-left:12px}
.office-shell{--role-accent:var(--c-brand);--role-accent-light:var(--c-brand-light);--el-color-primary:var(--role-accent);--el-color-primary-light-3:color-mix(in srgb,var(--role-accent),white 30%);--el-color-primary-light-5:color-mix(in srgb,var(--role-accent),white 50%);--el-color-primary-light-9:var(--role-accent-light);--el-color-primary-dark-2:color-mix(in srgb,var(--role-accent),black 20%)}
.role-member{--role-accent:color-mix(in srgb,var(--c-success),black 25%);--role-accent-light:var(--c-success-light);background:var(--c-bg)}
.role-member .office-sidebar{width:80px;background:var(--c-surface);padding:16px 6px;margin:12px 0 12px 12px;border:1px solid var(--c-border-light);border-radius:var(--radius-lg);box-shadow:var(--shadow-sm)}
.role-member .office-brand{justify-content:center;padding:0 0 12px}.role-member .office-brand strong,.role-member .team-picker{display:none}
.role-member .brand-symbol{background:var(--role-accent);width:34px;height:34px;border-radius:var(--radius-lg)}
.role-member .identity-label{font-size:10px;padding:5px 0;margin:0 0 12px;background:transparent;color:var(--c-text-muted)}
.role-member .office-sidebar a{flex-direction:column;gap:5px;padding:10px 2px;font-size:10px;border-radius:var(--radius-lg)}
.role-member .office-sidebar a.selected,.role-member .office-sidebar a.router-link-exact-active{background:var(--role-accent-light);color:var(--c-text)}
.role-member .office-sidebar a .material-symbols-outlined{font-size:22px}
.role-member .office-header{border-bottom:0;height:72px;background:var(--c-bg)}
.role-member .home-header-label{display:block;font-size:14px;font-weight:600;color:var(--c-text);white-space:nowrap}
.role-member .mobile-team-picker{display:block;width:160px;flex-shrink:0}.role-member .header-team{display:none}
.role-member .office-content{background:var(--c-bg);padding:24px 32px}.role-member.is-dashboard .office-content{padding:24px 44px 40px}
.role-member .profile-button{background:var(--role-accent);color:#fff}

.role-project{flex-direction:column}
.role-project .office-sidebar{width:100%;display:grid;grid-template-columns:auto auto minmax(120px,180px) minmax(0,1fr);grid-template-rows:44px 46px;gap:0 20px;padding:8px 28px 0;border-right:0;border-bottom:1px solid var(--c-border);border-top:3px solid var(--c-brand);background:var(--c-surface)}
.role-project .office-brand{grid-column:1;grid-row:1;padding:0;font-size:16px}.role-project .identity-label{grid-column:2;grid-row:1;align-self:center;margin:0;padding:5px 8px;font-size:11px;border-radius:var(--radius-sm)}
.role-project .team-picker{grid-column:3;grid-row:1;align-self:center;margin:0}
.role-project .office-sidebar nav{grid-row:2;grid-column:1 / -1;display:flex;gap:6px;overflow-x:auto;overflow-y:hidden;align-items:stretch;scrollbar-width:thin}
.role-project .home-nav-group{display:none}
.role-project .office-sidebar a{flex-shrink:0;border-radius:0;padding:10px 12px;font-size:12px;gap:6px;white-space:nowrap}
.role-project .office-sidebar a.selected{background:var(--c-brand-light);color:var(--c-brand-hover);box-shadow:inset 0 -3px 0 var(--c-brand)}
.role-project .office-sidebar .material-symbols-outlined{font-size:18px}
.role-project .sidebar-bottom{grid-column:4;grid-row:1;justify-self:end;display:flex;border:0;margin:0;padding:0;min-width:0}
.role-project .sidebar-bottom a{font-size:11px;padding:8px}
.role-project .office-header{height:52px;background:var(--c-bg);border-bottom:0}
.role-project .home-header-label{display:block;font-size:12px;font-weight:600;color:var(--c-brand-hover);white-space:nowrap}.role-project .header-team{display:none}
.role-project .office-content{padding:24px 32px}

.role-admin{--role-accent:#353c46;--role-accent-light:var(--c-surface-dim)}
.role-admin .office-sidebar{--admin-ink:#191d23;--admin-muted:#aeb6c2;width:216px;background:var(--admin-ink);border-right:0;padding:20px 12px}
.role-admin .office-brand{color:#fff;font-size:15px;padding-bottom:20px}.role-admin .brand-symbol{background:#fff;color:var(--admin-ink);border-radius:var(--radius-sm);width:26px;height:26px}
.role-admin .identity-label{background:#292f38;color:#e4e9ef;border-radius:var(--radius-sm);font-size:11px;margin:0 8px 16px;text-align:left}
.role-admin .home-nav-group{display:block;color:var(--admin-muted);font-size:10px;padding:12px 12px 6px}
.role-admin .office-sidebar nav{gap:2px}.role-admin .office-sidebar a{color:var(--admin-muted);padding:8px 12px;font-size:12px;border-radius:var(--radius-sm)}
.role-admin .office-sidebar a:hover{background:#292f38;color:#fff}.role-admin .office-sidebar a.selected,.role-admin .office-sidebar a.router-link-exact-active{background:#3a424e;color:#fff;box-shadow:inset 3px 0 0 var(--c-success)}
.role-admin .office-sidebar .material-symbols-outlined{font-size:18px}.role-admin .sidebar-bottom{border-color:#343a44}
.role-admin .team-picker :deep(.el-select__wrapper){background:#292f38;box-shadow:0 0 0 1px #424a57 inset}.role-admin .team-picker :deep(.el-select__selected-item){color:#e4e9ef}
.role-admin .office-header{height:48px;padding-inline:24px;background:var(--c-surface);gap:12px}.role-admin .home-header-label{display:block;color:var(--c-text);font-size:12px;font-weight:600;white-space:nowrap}.role-admin .header-team{font-size:11px;color:var(--c-text-muted)}
.role-admin .office-content{background:var(--c-bg);padding:20px 24px}.role-admin .office-header .el-button,.role-admin .profile-button{width:28px;height:28px}.role-admin .global-search{width:280px}
.is-messaging .office-content{display:flex;overflow:hidden}
.is-messaging :deep(.im-workspace){flex:1;min-width:0;min-height:0;height:auto;background:var(--c-surface)}
.role-member.is-messaging :deep(.im-workspace){border-radius:var(--radius-lg);box-shadow:var(--shadow-sm)}
.role-member.is-messaging :deep(.conversation-avatar){border-radius:50%;background:var(--c-success-light);color:var(--c-success)}
.role-member.is-messaging :deep(.conversation.active),.role-member.is-messaging :deep(.own .message-content){background:var(--c-success-light)}
.role-member.is-messaging :deep(.message-content){border-radius:var(--radius-lg)}
.role-admin.is-messaging :deep(.im-workspace){border-radius:0}.role-admin.is-messaging :deep(.conversations){background:var(--c-bg)}
.role-admin.is-messaging :deep(.conversation.active),.role-admin.is-messaging :deep(.own .message-content){background:var(--c-surface-dim)}
.role-admin.is-messaging :deep(.message-content){border-radius:var(--radius-sm)}
@media(max-width:1000px){.role-project .office-sidebar{gap:0 12px;padding-inline:20px}.role-project .sidebar-bottom a{font-size:0;gap:0}.role-member .current-page-label,.role-admin .current-page-label{display:none}}
@media(max-width:800px){.role-admin .office-sidebar{width:160px;padding-inline:8px}.role-member .office-content,.role-member.is-dashboard .office-content,.role-project .office-content,.role-admin .office-content{padding:20px}.role-member .home-header-label,.role-admin .home-header-label{display:none}.role-project .office-brand{font-size:14px}.role-project .office-sidebar{grid-template-columns:auto auto minmax(100px,160px) minmax(0,1fr)}}
@media(max-width:520px){
  .role-member .office-sidebar{width:58px;padding:12px 3px;margin:8px 0 8px 6px}.role-member .office-sidebar a{font-size:9px;padding:10px 0}.role-member .office-sidebar a .material-symbols-outlined{font-size:20px}.role-member .identity-label{font-size:9px;margin-bottom:10px}
  .role-member .office-header{height:60px;padding-inline:10px}.role-member .mobile-team-picker{width:90px}.role-member .office-content,.role-member.is-dashboard .office-content{padding:12px 10px}
  .role-admin .office-sidebar{width:56px;padding:14px 4px}.role-admin .identity-label{font-size:9px;padding:5px 0;margin:0 0 10px;text-align:center;border:0}.role-admin .office-brand{padding:0 0 12px}.role-admin .office-sidebar a{font-size:0;padding:10px;gap:0}.role-admin .home-nav-group{display:none}.role-admin .office-content{padding:12px}.role-admin .office-header{padding:0 10px;gap:8px}.role-admin .global-search{width:100%}
  .role-project .office-sidebar{grid-template-columns:30px auto minmax(100px,1fr) auto;grid-template-rows:38px 44px;padding:6px 10px 0;gap:0 8px}.role-project .office-brand strong{display:none}.role-project .identity-label{font-size:10px;padding:4px}.role-project .team-picker{display:block}.role-project .office-sidebar a{font-size:11px;padding:10px 8px;gap:5px}.role-project .sidebar-bottom{gap:4px}.role-project .sidebar-bottom a{font-size:0;padding:6px;gap:0}.role-project .office-header{height:44px}.role-project .home-header-label,.role-project .mobile-team-picker{display:none}.role-project .office-content{padding:12px}
}
</style>
