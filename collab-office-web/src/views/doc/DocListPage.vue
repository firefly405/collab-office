<template>
  <div class="office-page document-page" :class="{'is-dashboard':dashboard}" v-loading="loading">
    <div class="office-toolbar page-heading"><h2>{{ pageTitle }}</h2><div class="office-actions"><el-button @click="router.push(trash?'/documents':'/recycle')">{{ trash?'返回文档':'回收站' }}</el-button><el-button v-if="!trash" type="primary" @click="create"><span class="material-symbols-outlined">add</span>新建{{ typeName }}</el-button></div></div>
    <template v-if="dashboard">
      <div class="dashboard-stats" aria-label="工作台统计">
        <article class="stat-card stat-documents" data-stat="documents">
          <div class="stat-top"><span class="stat-label">团队文档</span><span class="stat-icon material-symbols-outlined" aria-hidden="true">description</span></div>
          <strong class="stat-value">{{ documentTotal ?? '—' }}</strong><span class="stat-note">当前团队 · 全部类型</span>
        </article>
        <article class="stat-card stat-tasks" data-stat="tasks">
          <div class="stat-top"><span class="stat-label">待办任务</span><span class="stat-icon material-symbols-outlined" aria-hidden="true">task_alt</span></div>
          <strong class="stat-value">{{ pendingTasks.length }}</strong><span class="stat-note">{{ tasks.filter(t=>t.status===1).length }} 项进行中 · {{ tasks.filter(t=>t.status===2).length }} 项已完成</span>
        </article>
        <article class="stat-card stat-meetings" data-stat="meetings">
          <div class="stat-top"><span class="stat-label">待开会议</span><span class="stat-icon material-symbols-outlined" aria-hidden="true">event</span></div>
          <strong class="stat-value">{{ upcoming.length }}</strong><span class="stat-note">我的日程 · 含进行中的会议</span>
        </article>
      </div>
      <div class="dashboard-agenda">
        <section class="agenda-section" aria-labelledby="pending-heading">
          <div class="agenda-heading"><h3 id="pending-heading">待办任务 <span class="section-count">{{ pendingTasks.length }}</span></h3><button class="office-link section-link" @click="router.push('/tasks')">全部任务 <span aria-hidden="true">→</span></button></div>
          <div v-if="pendingTasks.length" class="agenda-list" aria-label="未完成任务列表" tabindex="0">
            <div v-for="t in pendingTasks" :key="t.taskId" class="task-row">
              <span class="task-marker material-symbols-outlined" :class="{'is-active':t.status===1}" aria-hidden="true">{{ t.status===1?'timelapse':'radio_button_unchecked' }}</span>
              <div class="agenda-copy"><button class="office-link agenda-title" @click="router.push('/tasks?task='+t.taskId)">{{ t.title }}</button><span class="agenda-meta">{{ t.dueDate ? relativeTime(t.dueDate, false)+' 截止' : '未设期限' }}</span></div>
              <span class="agenda-status" :class="t.status===1?'status-active':'status-pending'">{{ t.status===1?'进行中':'待处理' }}</span>
            </div>
          </div>
          <div v-else class="dashboard-empty" data-testid="tasks-empty">
            <span class="empty-symbol material-symbols-outlined" aria-hidden="true">task_alt</span><strong>暂时没有待办</strong><p>安排好下一步，协作从这里开始。</p>
            <el-button type="primary" plain @click="router.push('/tasks')"><span class="material-symbols-outlined" aria-hidden="true">add</span>创建任务</el-button>
          </div>
        </section>
        <section class="agenda-section" aria-labelledby="meetings-heading">
          <div class="agenda-heading"><h3 id="meetings-heading">会议安排 <span class="section-count">{{ upcoming.length }}</span></h3><button class="office-link section-link" @click="router.push('/meetings')">全部会议 <span aria-hidden="true">→</span></button></div>
          <div v-if="upcoming.length" class="agenda-list meeting-list" aria-label="待开会议列表" tabindex="0">
            <div v-for="m in upcoming" :key="m.meetingId" class="meeting-row" :class="{'is-live':meetingLive(m)}">
              <div class="agenda-copy"><button class="office-link agenda-title" @click="router.push('/meetings?meeting='+m.meetingId)">{{ m.title }}</button><time class="agenda-meta" :datetime="m.startTime" :title="formatTime(m.startTime)">{{ relativeTime(m.startTime) }}<span v-if="m.endTime"> – {{ formatTime(m.endTime).slice(11) }}</span></time></div>
              <span class="agenda-status" :class="meetingLive(m)?'status-active':'status-soon'">{{ meetingLive(m)?'进行中':'即将开始' }}</span>
            </div>
          </div>
          <div v-else class="dashboard-empty"><span class="empty-symbol material-symbols-outlined" aria-hidden="true">event_available</span><strong>日程暂时空闲</strong><p>下一场讨论，留给新的想法。</p><el-button plain @click="router.push('/meetings/schedule')">预约会议</el-button></div>
        </section>
      </div>
      <div class="recent-heading"><h3>最近编辑</h3><span class="office-muted">共 {{ total }} 条{{ filter!=='all'||keyword?'筛选结果':'' }}</span></div>
    </template>
    <div class="office-toolbar document-filters"><el-radio-group v-model="filter" @change="page=1; load()"><el-radio-button value="all">全部</el-radio-button><el-radio-button value="mine">我创建的</el-radio-button><el-radio-button value="shared">团队共享</el-radio-button><el-radio-button value="starred">收藏</el-radio-button></el-radio-group><el-input v-model="keyword" placeholder="搜索标题" aria-label="搜索标题" clearable style="width:220px" @keyup.enter="page=1; load()" @clear="page=1; load()" /><el-select v-if="!dashboard&&!trash" v-model="folderFilter" placeholder="全部文件夹" clearable style="width:180px" @change="page=1;load()"><el-option v-for="f in folders" :key="f.folderId" :label="f.name" :value="String(f.folderId)"/></el-select><el-button v-if="!dashboard&&!trash" circle title="新建文件夹" @click="createFolder"><span class="material-symbols-outlined">create_new_folder</span></el-button></div>
    <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" />
    <el-table :data="documents" empty-text="暂无文档" class="document-table">
      <el-table-column label="名称" min-width="230"><template #default="{row}"><button class="doc-name office-link" @click="open(row)"><span class="doc-symbol material-symbols-outlined" :style="{color: colors[row.type-1]}" aria-hidden="true">{{ icons[row.type-1] || 'description' }}</span><span>{{ row.isPinned?'↑ ':'' }}{{ row.title }}</span></button><div v-if="row.tags" class="office-muted">{{ row.tags }}</div></template></el-table-column>
      <el-table-column prop="creatorName" label="创建者" width="140" /><el-table-column label="最后编辑" min-width="160"><template #default="{row}">{{ formatTime(row.lastEditTime) }}</template></el-table-column>
      <el-table-column label="操作" width="160"><template #default="{row}"><el-button v-if="trash" @click="recover(row)">恢复</el-button><div v-else class="document-row-actions"><el-button circle :title="row.isFavorite?'取消收藏':'收藏'" :aria-label="row.isFavorite?'取消收藏':'收藏'" @click="favorite(row)"><span class="material-symbols-outlined" :style="{color:row.isFavorite?'#d69512':''}">star</span></el-button><el-dropdown><el-button circle title="更多文档操作"><span class="material-symbols-outlined">more_horiz</span></el-button><template #dropdown><el-dropdown-menu><el-dropdown-item @click="pin(row)">{{ row.isPinned?'取消置顶':'置顶' }}</el-dropdown-item><el-dropdown-item @click="duplicate(row)">创建副本</el-dropdown-item><el-dropdown-item @click="moving=row;targetFolder=row.folderId?String(row.folderId):''">移动</el-dropdown-item><el-dropdown-item @click="tag(row)">编辑标签</el-dropdown-item></el-dropdown-menu></template></el-dropdown><el-button circle title="删除" aria-label="删除" @click="remove(row)"><span class="material-symbols-outlined">delete</span></el-button></div></template></el-table-column>
    </el-table><el-pagination style="margin-top:20px" v-model:current-page="page" :total="total" :page-size="15" layout="prev,pager,next,total" @current-change="load" />
    <el-dialog :model-value="!!moving" title="移动文档" width="min(440px,94vw)" @close="moving=undefined"><label>目标文件夹<el-select v-model="targetFolder"><el-option label="根目录" value=""/><el-option v-for="f in folders" :key="f.folderId" :label="f.name" :value="String(f.folderId)"/></el-select></label><template #footer><el-button type="primary" @click="move">移动</el-button></template></el-dialog>
  </div>
</template>
<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox, ElMessage } from 'element-plus'
import request from '@/utils/request'
import { useWorkspaceStore } from '@/stores/workspace'
import { isUpcomingMeeting } from '@/utils/meeting'
const route=useRoute(), router=useRouter(), workspace=useWorkspaceStore()
const dashboard=route.path==='/dashboard'
const trash=route.path==='/recycle'
const type=route.path==='/sheets'?2:route.path==='/mindmap'?3:route.path==='/whiteboard'?4:1
const typeName=['文档','表格','思维导图','白板'][type-1]
const pageTitle=trash?'回收站':dashboard?'工作台':typeName
const icons=['description','table_chart','account_tree','draw'], colors=['#3370ff','#169568','#b05bce','#dc8a24']
const documents=ref<any[]>([]), tasks=ref<any[]>([]), meetings=ref<any[]>([]), total=ref(0), page=ref(1), filter=ref('all'), keyword=ref(''), loading=ref(false), error=ref('')
if(route.query.view==='favorites')filter.value='starred'
const documentTotal=ref<number>()
const folders=ref<any[]>([]),folderFilter=ref(''),moving=ref<any>(),targetFolder=ref('')
const pendingTasks=computed(()=>tasks.value.filter(t=>t.status<2))
const upcoming=computed(()=>meetings.value.filter(isUpcomingMeeting).sort((a,b)=>String(a.startTime).localeCompare(b.startTime)))
function formatTime(value:any) { return value?String(value).replace('T',' ').slice(0,16):'—' }
function relativeTime(value:any, withClock=true) {
  if(!value)return '未设时间'
  const date=new Date(String(value).replace(' ','T'))
  if(Number.isNaN(date.getTime()))return formatTime(value)
  const today=new Date(), day=(d:Date)=>Date.UTC(d.getFullYear(),d.getMonth(),d.getDate())
  const offset=(day(date)-day(today))/86400000
  const label=offset===0?'今天':offset===1?'明天':offset===-1?'昨天':`${date.getMonth()+1}月${date.getDate()}日`
  const clock=withClock?` ${String(date.getHours()).padStart(2,'0')}:${String(date.getMinutes()).padStart(2,'0')}`:''
  return `${date.getFullYear()!==today.getFullYear()?date.getFullYear()+'年':''}${label}${clock}`
}
function meetingLive(m:any) { return new Date(String(m.startTime).replace(' ','T')).getTime()<=Date.now() }
async function load() { loading.value=true; error.value=''; try {const r:any=await request.get('/document/list',{params:{spaceId:workspace.spaceId,type:dashboard||trash?undefined:type,status:trash?0:1,filter:filter.value,keyword:keyword.value,folderId:folderFilter.value||undefined,pageNum:page.value,pageSize:15}}); documents.value=r.data.records; total.value=Number(r.data.total);if(dashboard){if(filter.value==='all'&&!keyword.value)documentTotal.value=total.value;else{const count:any=await request.get('/document/list',{params:{spaceId:workspace.spaceId,status:1,filter:'all',pageNum:1,pageSize:1}});documentTotal.value=Number(count.data.total)}}} catch(e:any){error.value=e.message||'加载失败'} finally{loading.value=false} }
async function loadFolders(){try{const r:any=await request.get('/document/folders');folders.value=r.data}catch{}}
onMounted(loadFolders)
async function createFolder(){try{const r=await ElMessageBox.prompt('文件夹名称','新建文件夹',{inputValidator:v=>!!v?.trim()&&v.length<=100});await request.post('/file/folder',null,{params:{folderName:r.value.trim()}});await loadFolders()}catch{}}
async function duplicate(d:any){try{const r:any=await request.post(`/document/${d.docId}/duplicate`);open({docId:r.data,type:d.type})}catch{}}
async function pin(d:any){try{await request.put(`/document/${d.docId}/pin`,null,{params:{value:!d.isPinned}});await load()}catch{}}
async function tag(d:any){try{const r=await ElMessageBox.prompt('标签（用逗号分隔）','编辑标签',{inputValue:d.tags||'',inputValidator:v=>(v||'').length<=500});await request.put(`/document/${d.docId}`,{tags:r.value||''});await load()}catch{}}
async function move(){try{await request.put(`/document/${moving.value.docId}/move`,null,{params:{folderId:targetFolder.value||undefined}});moving.value=undefined;await load();ElMessage.success('已移动')}catch{}}
onMounted(async()=>{await load(); if(dashboard){const results=await Promise.allSettled([request.get('/task/list'),request.get('/meeting/list')]); if(results[0].status==='fulfilled')tasks.value=(results[0].value as any).data; if(results[1].status==='fulfilled')meetings.value=(results[1].value as any).data} })
function open(d:any){router.push((['/documents/','/sheets/','/mindmap/','/whiteboard/'][d.type-1]||'/documents/')+d.docId)}
async function create(){try{const {value}=await ElMessageBox.prompt('名称','新建'+typeName,{inputValidator:v=>!!v?.trim()&&v.length<=100 || '请输入1到100字'}); const r:any=await request.post('/document/create',{title:value.trim(),type,spaceId:workspace.spaceId}); open({docId:r.data,type})}catch{}}
onMounted(()=>{if(route.query.create==='1')create()})
async function favorite(d:any){try{await request.post(`/document/${d.docId}/favorite`,null,{params:{fav:!d.isFavorite}}); await load()}catch{}}
async function recover(d:any){try{await request.post(`/document/${d.docId}/recover`);await load();ElMessage.success('已恢复')}catch{}}
async function remove(d:any){try{await ElMessageBox.confirm('将“'+d.title+'”移入回收站？','删除文档',{type:'warning'});await request.delete('/document/'+d.docId); ElMessage.success('已移入回收站');await load()}catch{}}
</script>
<style scoped>
.doc-name { display:flex; align-items:center; gap:12px; overflow-wrap:anywhere; color:var(--c-text); font-weight:500; }
.doc-symbol { display:grid; place-items:center; flex:0 0 34px; width:34px; height:36px; background:var(--c-surface-hover); border:1px solid var(--c-border-light); border-radius:var(--radius-md); }
.material-symbols-outlined { font-size:20px; }
.document-row-actions { display:flex; gap:6px; }
.document-row-actions .el-button + .el-button { margin-left:0; }
.document-table :deep(.el-table__cell) { padding-block:12px; }
.document-table :deep(.el-table__row) { --el-table-row-hover-bg-color:var(--c-surface-hover); }
@media(hover:hover) and (pointer:fine) {
  .document-row-actions { opacity:0; transition:opacity .16s; }
  .document-table :deep(.el-table__row:hover) .document-row-actions,.document-table :deep(.el-table__row:focus-within) .document-row-actions { opacity:1; }
}
@media(max-width:800px) { .document-row-actions { opacity:1; } }
.is-dashboard .page-heading { margin-bottom:var(--space-6); }
.is-dashboard .dashboard-stats { display:grid; grid-template-columns:repeat(3,minmax(0,1fr)); gap:var(--space-4); margin-bottom:var(--space-8); }
.is-dashboard .stat-card { min-width:0; padding:20px 24px; border:1px solid var(--c-border); border-radius:var(--radius-lg); background:var(--c-surface); box-shadow:var(--shadow-sm); transition:box-shadow .18s,border-color .18s,transform .18s; }
.is-dashboard .stat-card:hover { box-shadow:var(--shadow-md); border-color:var(--c-text-ghost); transform:translateY(-2px); }
.is-dashboard .stat-top { display:flex; align-items:center; justify-content:space-between; gap:8px; }
.is-dashboard .stat-label { font-size:var(--text-body); font-weight:500; color:var(--c-text-secondary); }
.is-dashboard .stat-icon { display:grid; place-items:center; flex:0 0 36px; width:36px; height:36px; border-radius:var(--radius-lg); font-size:21px; }
.is-dashboard .stat-documents .stat-icon { color:var(--c-brand); background:var(--c-brand-light); }
.is-dashboard .stat-tasks .stat-icon { color:var(--c-success); background:var(--c-success-light); }
.is-dashboard .stat-meetings .stat-icon { color:var(--c-warning); background:var(--c-warning-light); }
.is-dashboard .stat-value { display:block; font-size:var(--text-stat); line-height:1.25; font-weight:650; font-variant-numeric:tabular-nums; margin:4px 0 8px; }
.is-dashboard .stat-note { display:block; color:var(--c-text-muted); font-size:var(--text-small); line-height:20px; }
.is-dashboard .dashboard-agenda { display:grid; grid-template-columns:minmax(0,1fr) minmax(0,1fr); gap:var(--space-8); margin-bottom:var(--space-8); padding-bottom:var(--space-6); border-bottom:1px solid var(--c-border); }
.is-dashboard .agenda-section { min-width:0; }
.is-dashboard .agenda-heading,.is-dashboard .recent-heading { display:flex; align-items:center; justify-content:space-between; gap:12px; margin-bottom:16px; }
.is-dashboard .agenda-heading h3,.is-dashboard .recent-heading h3 { margin:0; }
.is-dashboard .section-count { display:inline-grid; place-items:center; margin-left:6px; min-width:22px; height:22px; padding-inline:5px; background:var(--c-surface-dim); color:var(--c-text-secondary); border-radius:var(--radius-sm); font-size:12px; font-weight:500; vertical-align:middle; }
.is-dashboard .section-link { font-size:12px; white-space:nowrap; color:var(--c-text-muted); }
.is-dashboard .section-link:hover { color:var(--c-brand); }
.is-dashboard .agenda-list { min-height:216px; max-height:288px; overflow:auto; scrollbar-width:thin; scrollbar-color:var(--c-border) transparent; }
.is-dashboard .task-row,.is-dashboard .meeting-row { display:flex; align-items:center; gap:12px; min-height:72px; padding:12px 4px; border-bottom:1px solid var(--c-border-light); }
.is-dashboard .task-row:last-child,.is-dashboard .meeting-row:last-child { border-bottom:0; }
.is-dashboard .task-marker { color:var(--c-text-ghost); font-size:20px; flex-shrink:0; }
.is-dashboard .task-marker.is-active { color:var(--c-brand); }
.is-dashboard .agenda-copy { display:flex; flex-direction:column; gap:7px; min-width:0; flex:1; }
.is-dashboard .agenda-title { display:block; color:var(--c-text); font-size:14px; font-weight:500; line-height:20px; overflow-wrap:anywhere; }
.is-dashboard .agenda-title:hover { color:var(--c-brand); }
.is-dashboard .agenda-meta { color:var(--c-text-muted); font-size:12px; line-height:18px; }
.is-dashboard .agenda-status { flex-shrink:0; padding:3px 7px; border-radius:var(--radius-sm); font-size:11px; line-height:18px; white-space:nowrap; }
.is-dashboard .status-active { color:#168363; background:var(--c-success-light); }
.is-dashboard .status-pending { color:var(--c-text-secondary); background:var(--c-surface-dim); }
.is-dashboard .status-soon { color:#946014; background:var(--c-warning-light); }
.is-dashboard .meeting-row { position:relative; padding-left:15px; }
.is-dashboard .meeting-row::before { content:''; position:absolute; left:0; top:18px; bottom:18px; width:3px; background:var(--c-brand); border-radius:2px; }
.is-dashboard .meeting-row.is-live::before { background:var(--c-success); }
.is-dashboard .dashboard-empty { height:232px; display:flex; flex-direction:column; align-items:center; justify-content:center; text-align:center; gap:10px; padding:16px; background:var(--c-surface-hover); border-radius:var(--radius-lg); }
.is-dashboard .empty-symbol { display:grid; place-items:center; width:48px; height:48px; color:var(--c-brand); background:var(--c-brand-light); border-radius:50%; font-size:26px; margin-bottom:2px; }
.is-dashboard .dashboard-empty strong { font-size:14px; font-weight:500; }
.is-dashboard .dashboard-empty p { color:var(--c-text-muted); font-size:12px; line-height:18px; }
.is-dashboard .dashboard-empty .el-button { margin-top:2px; }
.is-dashboard .document-filters { margin-bottom:16px; }
@media(max-width:1000px) { .is-dashboard .stat-card { padding:16px; }.is-dashboard .dashboard-agenda { gap:20px; } }
@media(max-width:800px) { .is-dashboard .dashboard-agenda { grid-template-columns:1fr; gap:24px; }.is-dashboard .dashboard-stats { gap:12px; }.is-dashboard .stat-top { align-items:flex-start; }.is-dashboard .stat-icon { width:28px; height:28px; flex-basis:28px; font-size:18px; } }
@media(max-width:600px) { .is-dashboard .dashboard-stats { grid-template-columns:1fr; gap:10px; margin-bottom:24px; }.is-dashboard .stat-card { display:grid; grid-template-columns:1fr auto; grid-template-rows:auto auto; gap:4px 12px; padding:16px; }.is-dashboard .stat-top { justify-content:flex-start; grid-column:1; align-items:center; }.is-dashboard .stat-icon { order:-1; }.is-dashboard .stat-value { grid-column:2; grid-row:1 / 3; align-self:center; margin:0; }.is-dashboard .stat-note { grid-column:1; padding-left:36px; }.is-dashboard .document-filters { align-items:flex-start; }.is-dashboard .document-filters :deep(.el-radio-button__inner) { padding:8px 10px; }.is-dashboard .recent-heading { align-items:center; } }
@media(prefers-reduced-motion:reduce) { .is-dashboard .stat-card,.document-row-actions { transition:none; }.is-dashboard .stat-card:hover { transform:none; } }
</style>
