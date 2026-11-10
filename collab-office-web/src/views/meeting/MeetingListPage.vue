<template>
<div class="office-page" v-loading="loading">
<div class="office-toolbar">
<h2>会议安排</h2>
<div class="office-actions">
<el-button @click="schedule()">预约会议</el-button>
<el-button type="primary" @click="schedule(true)">
<span class="material-symbols-outlined">add</span>即时会议</el-button>
</div>
</div>
<div class="office-toolbar">
<el-radio-group v-model="filter">
<el-radio-button value="upcoming">待开会议</el-radio-button>
<el-radio-button value="past">历史会议</el-radio-button>
<el-radio-button value="all">全部</el-radio-button>
</el-radio-group>
<el-input v-model="keyword" clearable placeholder="搜索会议" style="width:220px" />
</div>
  <el-table :data="filtered" @row-click="open" style="cursor:pointer" empty-text="暂无会议">
<el-table-column prop="title" label="会议" min-width="220" />
<el-table-column label="时间" min-width="200">
<template #default="{row}">{{ time(row.startTime) }}<br/>
<span class="office-muted">至 {{ time(row.endTime) }}</span>
</template>
</el-table-column>
<el-table-column label="状态" width="100">
<template #default="{row}">
<el-tag :type="isCancelledMeeting(row)?'info':past(row)?'success':'primary'">{{ isCancelledMeeting(row)?'已取消':past(row)?'已结束':new Date(row.startTime).getTime()<=Date.now()?'进行中':'待开' }}</el-tag>
</template>
</el-table-column>
<el-table-column label="主持人" width="130">
<template #default="{row}">{{ person(row.hostId) }}</template>
</el-table-column>
</el-table>
  <el-dialog v-model="visible" :title="editingMeeting?'调整会议安排':'预约会议'" width="min(540px, 94vw)">
<form class="office-form" @submit.prevent="create">
<label>主题<el-input v-model="form.title" maxlength="200" />
</label>
<label>开始时间<el-date-picker v-model="form.startTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width:100%" />
</label>
<label>结束时间<el-date-picker v-model="form.endTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width:100%" />
</label>
<label>{{ editingMeeting?'新增受邀成员':'参与人' }}<el-select v-model="form.participantIds" multiple filterable placeholder="选择团队成员">
<el-option v-for="m in members.filter(m=>String(m.userId)!==userId)" :key="m.userId" :label="m.userName" :value="String(m.userId)" />
</el-select>
</label>
<label>议程<el-input v-model="form.agenda" type="textarea" :rows="4" maxlength="5000" />
</label>
<el-button type="primary" native-type="submit" :loading="saving">{{ editingMeeting?'保存安排':'创建会议' }}</el-button>
</form>
</el-dialog>
  <el-drawer v-model="detailVisible" title="会议详情" size="min(560px, 100vw)">
<template v-if="detail">
<h3>{{ detail.title }}</h3>
<p class="office-muted">{{ time(detail.startTime) }} 至 {{ time(detail.endTime) }}</p>
<p>主持人：{{ person(detail.hostId) }}</p>
<p>参与人：{{ (detail.participants||[]).map((p:any)=>p.userName||person(p.userId)).join('、') }}</p>
<p class="office-muted">计划时长 {{ duration }} 分钟 · {{ (detail.participants||[]).length }} 位受邀成员</p>
<section class="office-section">
<h3>议程</h3>
<p class="meeting-text">{{ detail.agenda||'暂无议程' }}</p>
</section>
<section class="office-section">
<div class="office-toolbar">
<h3>会议纪要</h3>
<el-button @click="exportMinutes">
<span class="material-symbols-outlined">download</span>导出记录</el-button>
</div>
<el-input v-model="minuteContent" type="textarea" :rows="8" :readonly="!host" maxlength="20000" placeholder="暂无纪要" />
<div v-if="host" class="office-toolbar minutes-actions">
<el-button :loading="saving" @click="extractTodos">提取待办</el-button>
<el-button :disabled="todos.length>=100" @click="addTodo">添加事项</el-button>
<el-button type="primary" :loading="saving" @click="saveMinutes">保存纪要</el-button>
</div>
</section>
  <section class="office-section">
<h3>会议待办 · {{ todos.length }}</h3>
<el-empty v-if="!todos.length" description="暂无会议待办" :image-size="55"/>
<div v-for="todo in todos" :key="todo.id" class="minute-todo">
<div class="todo-title">
<el-checkbox v-if="host&&!todo.taskId" v-model="todo.selected" :aria-label="'选择事项 '+todo.title"/>
<el-input v-model="todo.title" :readonly="!host||!!todo.taskId" maxlength="200" placeholder="待办事项"/>
<el-button v-if="host&&!todo.taskId" text circle title="移除事项" @click="todos=todos.filter(t=>t.id!==todo.id)">
<span class="material-symbols-outlined">close</span>
</el-button>
</div>
<div class="todo-fields">
<el-select v-model="todo.assigneeId" :disabled="!host||!!todo.taskId" clearable placeholder="负责人">
<el-option v-for="m in members" :key="m.userId" :value="String(m.userId)" :label="m.userName"/>
</el-select>
<el-date-picker v-model="todo.dueDate" type="date" value-format="YYYY-MM-DD" placeholder="截止日期" :disabled="!host||!!todo.taskId"/>
</div>
<router-link v-if="todo.taskId" :to="'/tasks?task='+todo.taskId">查看关联任务</router-link>
</div>
<template v-if="host&&todos.some(t=>!t.taskId)">
<el-select v-model="projectId" clearable placeholder="任务项目（默认团队项目）" style="width:100%;margin:12px 0">
<el-option v-for="p in projects.filter(p=>p.status!==2)" :key="p.projectId" :value="p.projectId" :label="p.name"/>
</el-select>
<el-button type="primary" :loading="saving" :disabled="!todos.some(t=>t.selected&&!t.taskId)" @click="convertTodos">勾选事项转任务</el-button>
</template>
</section>
<el-button v-if="host && isUpcomingMeeting(detail)" type="danger" plain @click="cancel">取消会议</el-button>
</template>
<template #footer><div v-if="detail" class="office-actions minutes-actions"><el-button v-if="host&&isUpcomingMeeting(detail)" @click="editSchedule">调整安排</el-button><el-button @click="copyInvitation">复制邀请链接</el-button><el-button @click="calendarInvite"><span class="material-symbols-outlined">calendar_add_on</span>日历邀请</el-button></div></template>
</el-drawer>
</div>
</template>
<script setup lang="ts">
import {computed,onMounted,reactive,ref} from 'vue'
import {useRoute} from 'vue-router'
import {ElMessage,ElMessageBox} from 'element-plus'
import request from '@/utils/request'
import {useWorkspaceStore} from '@/stores/workspace'
import {isCancelledMeeting,isUpcomingMeeting} from '@/utils/meeting'
const route=useRoute(),workspace=useWorkspaceStore(),meetings=ref<any[]>([]),members=ref<any[]>([]),userId=ref(''),filter=ref(route.path.endsWith('/history')?'past':'upcoming'),keyword=ref(''),loading=ref(false),saving=ref(false),visible=ref(route.path.endsWith('/schedule')),detailVisible=ref(false),detail=ref<any>(),minuteContent=ref('')
const blank=()=>({title:'',startTime:'',endTime:'',agenda:'',participantIds:[] as string[]}),form=reactive(blank())
const editingMeeting=ref('')
function localDate(date:Date){return new Date(date.getTime()-date.getTimezoneOffset()*60000).toISOString().slice(0,19)}
function schedule(immediate=false){editingMeeting.value='';Object.assign(form,blank());if(immediate){form.startTime=localDate(new Date());form.endTime=localDate(new Date(Date.now()+30*60000))}visible.value=true}
function editSchedule(){editingMeeting.value=detail.value.meetingId;Object.assign(form,{title:detail.value.title,agenda:detail.value.agenda||'',startTime:String(detail.value.startTime).replace(' ','T'),endTime:String(detail.value.endTime).replace(' ','T'),participantIds:[]});visible.value=true}
function downloadFile(content:string,name:string,type:string){const url=URL.createObjectURL(new Blob([content],{type}));const a=document.createElement('a');a.href=url;a.download=name;a.click();setTimeout(()=>URL.revokeObjectURL(url),10000)}
function calendarInvite(){const m=detail.value,escape=(s:string)=>s.replace(/\\/g,'\\\\').replace(/\r?\n/g,'\\n').replace(/;/g,'\\;').replace(/,/g,'\\,'),date=(s:string)=>new Date(s).toISOString().replace(/[-:]/g,'').replace(/\.\d{3}/,'');downloadFile(['BEGIN:VCALENDAR','VERSION:2.0','PRODID:-//Nexus Office//Meetings//ZH','BEGIN:VEVENT','UID:'+m.meetingId+'@nexus.local','DTSTAMP:'+date(new Date().toISOString()),'DTSTART:'+date(m.startTime),'DTEND:'+date(m.endTime),'SUMMARY:'+escape(m.title),'DESCRIPTION:'+escape(m.agenda||''),'URL:'+location.origin+'/#/meetings?meeting='+m.meetingId,'BEGIN:VALARM','TRIGGER:-PT15M','ACTION:DISPLAY','DESCRIPTION:会议即将开始','END:VALARM','END:VEVENT','END:VCALENDAR',''].join('\r\n'),m.title.replace(/[\\/:*?"<>|]/g,'_')+'.ics','text/calendar;charset=utf-8')}
async function copyInvitation(){try{await navigator.clipboard.writeText(location.origin+'/#/meetings?meeting='+detail.value.meetingId);ElMessage.success('邀请链接已复制，仅受邀成员可访问')}catch{ElMessage.error('浏览器未允许复制，请使用日历邀请')}}
const host=computed(()=>String(detail.value?.hostId)===userId.value)
type MinuteTodo={id:string;title:string;assigneeId:string;dueDate:string;taskId?:string;selected?:boolean;spaceId?:number}
const todos=ref<MinuteTodo[]>([]),revision=ref(''),projects=ref<any[]>([]),projectId=ref('')
const duration=computed(()=>Math.max(0,Math.round((new Date(detail.value?.endTime).getTime()-new Date(detail.value?.startTime).getTime())/60000))||0)
const past=(m:any)=>new Date(m.endTime||m.startTime).getTime()<Date.now()
const filtered=computed(()=>meetings.value.filter(m=>m.title.includes(keyword.value)&&(filter.value==='all'||filter.value==='past'&&(past(m)||isCancelledMeeting(m))||filter.value==='upcoming'&&isUpcomingMeeting(m))).sort((a,b)=>String(a.startTime).localeCompare(b.startTime)))
const time=(v:any)=>v?String(v).replace('T',' ').slice(0,16):'—'
const person=(id:any)=>members.value.find(m=>String(m.userId)===String(id))?.userName||'团队成员'
async function load(){loading.value=true;try{const r:any[]=await Promise.all([request.get('/meeting/list'),request.get(`/space/${workspace.spaceId}/members`),request.get('/user/info')]);meetings.value=r[0].data;members.value=r[1].data;userId.value=String(r[2].data.userId)}catch{}finally{loading.value=false}}
onMounted(async()=>{await load();if(route.query.meeting)open({meetingId:route.query.meeting})})
async function create(){if(!form.title.trim()||!form.startTime||!form.endTime||form.endTime<=form.startTime){ElMessage.warning('请填写主题，并确保结束时间晚于开始时间');return}saving.value=true;try{if(editingMeeting.value)await request.put('/meeting/'+editingMeeting.value+'/schedule',{...form,title:form.title.trim()});else await request.post('/meeting/create',{...form,title:form.title.trim()});visible.value=false;if(editingMeeting.value)await open({meetingId:editingMeeting.value});Object.assign(form,blank());await load();ElMessage.success('会议安排已保存')}catch{}finally{saving.value=false}}
async function readMinutes(){const r:any=await request.get('/meeting/'+detail.value.meetingId+'/minutes');minuteContent.value=r.data.minuteContent||'';todos.value=(r.data.todoListJson||[]).map((t:MinuteTodo)=>({...t,selected:false}));revision.value=r.data.revision}
async function open(m:any){try{const r:any=await request.get('/meeting/'+m.meetingId);detail.value=r.data;await readMinutes();const p:any=await request.get('/task/project/list',{params:{spaceId:workspace.spaceId}});projects.value=p.data;projectId.value='';detailVisible.value=true}catch{}}
function addTodo(){todos.value.push({id:crypto.randomUUID(),title:'',assigneeId:'',dueDate:'',selected:true})}
function extractTodos(){const lines=minuteContent.value.split('\n').map(l=>l.match(/^\s*(?:[-*]\s*\[ \]\s*|待办[:：]\s*)(.+)$/)?.[1]?.trim()).filter(Boolean) as string[];let added=0;for(const title of lines){if(todos.value.length>=100)break;if(!todos.value.some(t=>t.title===title)){todos.value.push({id:crypto.randomUUID(),title:title.slice(0,200),assigneeId:'',dueDate:'',selected:true});added++}}ElMessage.info(added?`已提取 ${added} 项`:'未发现新的待办事项')}
async function persistMinutes(){if(todos.value.some(t=>!t.title.trim())){ElMessage.warning('请填写待办标题');return false}await request.put('/meeting/'+detail.value.meetingId+'/minutes',{minuteContent:minuteContent.value,todoListJson:todos.value.map(({selected,...t})=>({...t,dueDate:t.dueDate||'',assigneeId:t.assigneeId||''})),revision:revision.value});return true}
async function saveMinutes(){saving.value=true;try{if(await persistMinutes()){await readMinutes();ElMessage.success('纪要已保存')}}catch{}finally{saving.value=false}}
async function convertTodos(){const ids=todos.value.filter(t=>t.selected&&!t.taskId).map(t=>t.id);if(!ids.length)return;saving.value=true;try{if(!await persistMinutes())return;await request.post('/task/from-meeting',{meetingId:detail.value.meetingId,itemIds:ids,projectId:projectId.value||null});await readMinutes();ElMessage.success('待办已转为任务')}catch{}finally{saving.value=false}}
function exportMinutes(){const data={meeting:detail.value,plannedDurationMinutes:duration.value,invitedCount:(detail.value.participants||[]).length,minuteContent:minuteContent.value,todos:todos.value.map(({selected,...t})=>t)};const url=URL.createObjectURL(new Blob([JSON.stringify(data,null,2)],{type:'application/json;charset=utf-8'}));const a=document.createElement('a');a.href=url;a.download=detail.value.title.replace(/[\\/:*?"<>|]/g,'_')+'-会议记录.json';a.click();URL.revokeObjectURL(url)}
async function cancel(){try{await ElMessageBox.confirm('取消此会议？','取消会议',{type:'warning'});await request.delete('/meeting/'+detail.value.meetingId);detailVisible.value=false;await load()}catch{}}
</script>
<style scoped>.meeting-text{white-space:pre-wrap;overflow-wrap:anywhere;line-height:1.8}h3{overflow-wrap:anywhere}.minutes-actions{margin-top:12px;flex-wrap:wrap}.minute-todo{padding:12px 0;border-bottom:1px solid var(--office-border)}.todo-title{display:flex;align-items:center;gap:8px}.todo-title .el-input{flex:1;min-width:0}.todo-fields{display:grid;grid-template-columns:minmax(0,1fr) minmax(0,1fr);gap:8px;margin:8px 0}.todo-fields :deep(.el-date-editor){width:100%;min-width:0}@media(max-width:500px){.todo-fields{grid-template-columns:minmax(0,1fr)}}</style>
