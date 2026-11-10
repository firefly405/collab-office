<template>
  <div class="office-page role-home" :class="identity.isManager?'manager-home':'member-home'" v-loading="loading">
    <header v-if="identity.isManager" class="manager-heading">
      <div class="manager-title"><span class="manager-mark material-symbols-outlined" aria-hidden="true">folder_managed</span><div><span class="page-kicker">项目交付 / {{ teamName }}</span><h2>项目指挥台</h2></div></div>
      <div class="office-actions"><el-button @click="router.push('/reports')"><span class="material-symbols-outlined" aria-hidden="true">forum</span>处理汇报</el-button><el-button type="primary" @click="router.push('/tasks')"><span class="material-symbols-outlined" aria-hidden="true">assignment</span>管理项目</el-button></div>
    </header>
    <header v-else class="personal-heading">
      <div class="personal-welcome"><span class="page-kicker">{{ today.toLocaleDateString('zh-CN',{month:'long',day:'numeric',weekday:'long'}) }} <span class="kicker-divider">/</span> {{ teamName }}</span><h2>我的一天<span class="welcome-dot">.</span></h2><p class="welcome-name">{{ greeting }}，{{ displayName }}</p><p>{{ mine.some(t=>t.status<2)?`还有 ${mine.filter(t=>t.status<2).length} 项待办，按自己的节奏向前。`:'待办已处理完，留一点时间给新的想法。' }}</p></div>
      <div class="personal-focus"><div class="completion-ring" role="img" :aria-label="`本人任务完成率 ${personalProgress}%`" :style="{'--completion-angle':personalProgress*3.6+'deg'}"><div><strong>{{ personalProgress }}<small>%</small></strong><span>任务完成率</span></div></div><span class="focus-caption">{{ mine.filter(t=>t.status===2).length }} / {{ mine.length }} 项已完成</span></div>
    </header>

    <div class="role-metrics" :aria-label="identity.isManager?'项目管理统计':'个人协作统计'">
      <article v-for="metric in metrics" :key="metric.label" :class="metric.tone"><span class="metric-icon material-symbols-outlined" aria-hidden="true">{{ metric.icon }}</span><div><span class="metric-label">{{ metric.label }}</span><strong>{{ metric.value }}</strong></div><span class="metric-note">{{ metric.note }}</span></article>
    </div>
    <nav v-if="!identity.isManager" class="personal-shortcuts" aria-label="常用协作入口">
      <router-link v-for="item in shortcuts" :key="item.path" :to="item.path"><span class="shortcut-icon material-symbols-outlined" aria-hidden="true">{{ item.icon }}</span><span>{{ item.label }}</span><span class="material-symbols-outlined shortcut-arrow" aria-hidden="true">arrow_outward</span></router-link>
    </nav>

    <div class="role-columns">
      <section :aria-labelledby="identity.isManager?'projects-heading':'personal-tasks-heading'">
        <div class="section-heading"><div><span v-if="!identity.isManager" class="section-kicker">专注当下</span><h3 :id="identity.isManager?'projects-heading':'personal-tasks-heading'">{{ identity.isManager?'负责的项目':'我的任务' }}<span class="section-count">{{ identity.isManager?managedProjects.length:mine.filter(t=>t.status<2).length }}</span></h3></div><router-link to="/tasks" class="office-link">{{ identity.isManager?'进入看板':'查看全部' }}<span class="material-symbols-outlined" aria-hidden="true">arrow_forward</span></router-link></div>
        <template v-if="identity.isManager">
          <div class="project-table-heading" v-if="managedProjects.length"><span>项目 / 里程碑</span><span>完成进度</span><span>任务</span><span></span></div>
          <article v-for="p in managedProjects" :key="p.projectId" class="managed-project">
            <div class="project-identity"><span class="project-icon material-symbols-outlined" aria-hidden="true">folder_managed</span><div><router-link to="/tasks" class="project-title">{{ p.name }}</router-link><small>{{ p.milestones?.filter((m:any)=>m.done).length||0 }}/{{ p.milestones?.length||0 }} 个里程碑完成</small><el-tag v-if="p.status===2" size="small" type="info">已归档</el-tag></div></div>
            <div class="project-progress"><strong>{{ progress(p.projectId) }}<small>%</small></strong><el-progress :percentage="progress(p.projectId)" :show-text="false" :stroke-width="5"/></div>
            <div class="project-task-count"><strong>{{ tasks.filter(t=>t.projectId===p.projectId).length }}</strong><small>{{ tasks.filter(t=>t.projectId===p.projectId&&t.status<2).length }} 项未完成</small></div>
            <el-button circle title="打开任务看板" @click="router.push('/tasks')"><span class="material-symbols-outlined" aria-hidden="true">arrow_forward</span></el-button>
          </article>
          <el-empty v-if="!managedProjects.length" description="暂无负责的项目"><el-button @click="router.push('/tasks')">新建项目</el-button></el-empty>
          <div class="milestone-section"><div class="section-heading"><h3>待完成里程碑</h3><span class="section-count">{{ pendingMilestones.length }}</span></div><div v-for="(m,i) in pendingMilestones.slice(0,5)" :key="m.projectId+':'+i" class="milestone-row"><span class="material-symbols-outlined" aria-hidden="true">flag</span><div><strong>{{ m.title }}</strong><small>{{ m.projectName }}</small></div><time>{{ m.dueDate||'未设期限' }}</time></div><p v-if="!pendingMilestones.length" class="inline-empty">暂无待完成里程碑</p></div>
        </template>
        <template v-else>
          <div v-if="mine.length" class="personal-progress"><span>已完成 {{ mine.filter(t=>t.status===2).length }} / {{ mine.length }}</span><el-progress :percentage="personalProgress" color="var(--c-success)" :show-text="false" :stroke-width="4"/></div>
          <button v-for="t in mine.filter(t=>t.status<2).slice(0,8)" :key="t.taskId" class="personal-task" @click="router.push('/tasks?task='+t.taskId)">
            <span class="task-symbol material-symbols-outlined" :class="{active:t.status===1}" aria-hidden="true">{{ t.status===1?'timelapse':'radio_button_unchecked' }}</span><span class="task-copy"><strong>{{ t.title }}</strong><small>{{ projects.find(p=>p.projectId===t.projectId)?.name||'我的任务' }}<span>·</span>{{ t.dueDate?shortDate(t.dueDate)+' 截止':'未设期限' }}</small></span><span class="task-status" :class="{active:t.status===1}">{{ t.status===1?'进行中':'待处理' }}</span><span class="task-arrow material-symbols-outlined" aria-hidden="true">chevron_right</span>
          </button>
          <el-empty v-if="!mine.some(t=>t.status<2)" description="当前任务已处理完"><el-button @click="router.push('/tasks')">查看任务</el-button></el-empty>
          <router-link to="/reports" class="personal-report-link"><span class="material-symbols-outlined" aria-hidden="true">edit_note</span><span>记录进展，分享下一步</span><span class="material-symbols-outlined" aria-hidden="true">arrow_forward</span></router-link>
          <section class="joined-projects" aria-labelledby="joined-projects-heading"><div class="section-heading"><h3 id="joined-projects-heading">一起推进的项目</h3><router-link to="/im" class="office-link">团队交流<span class="material-symbols-outlined" aria-hidden="true">arrow_forward</span></router-link></div><div class="joined-project-list"><router-link v-for="p in projects" :key="p.projectId" to="/tasks" class="joined-project"><span class="joined-icon material-symbols-outlined" aria-hidden="true">workspaces</span><div><strong>{{ p.name }}</strong><small>{{ p.status===2?'已归档':'进行中' }} · {{ tasks.filter(t=>t.projectId===p.projectId).length }} 个任务</small></div><span class="material-symbols-outlined" aria-hidden="true">arrow_outward</span></router-link></div><p v-if="!projects.length" class="inline-empty">暂无参与项目</p></section>
        </template>
      </section>
      <section class="report-section" aria-labelledby="reports-heading">
        <div class="section-heading"><div><span v-if="!identity.isManager" class="section-kicker">协作回声</span><h3 id="reports-heading">{{ identity.isManager?'待阅汇报':'最近汇报' }}<span class="section-count">{{ shownReports.length }}</span></h3></div><router-link to="/reports" class="office-link">查看全部<span class="material-symbols-outlined" aria-hidden="true">arrow_forward</span></router-link></div>
        <article v-for="r in shownReports.slice(0,6)" :key="r.id" class="report-row">
          <div class="report-meta"><span class="report-author">{{ r.authorName }}</span><time>{{ shortDate(r.createdAt) }}</time><el-tag size="small" :type="r.status==='reviewed'?'success':'warning'">{{ r.status==='reviewed'?'已阅':'待阅' }}</el-tag></div><router-link to="/reports" class="report-title">{{ r.title }}</router-link><p class="report-excerpt">{{ r.content }}</p><div class="report-footer"><span>{{ r.projectName }}</span><router-link to="/reports"><span class="material-symbols-outlined" aria-hidden="true">chat_bubble_outline</span>{{ r.replies.length }} 条回复</router-link></div>
        </article>
        <el-empty v-if="!shownReports.length" :description="identity.isManager?'汇报已处理完':'还没有提交汇报'"><el-button @click="router.push('/reports')">{{ identity.isManager?'查看全部汇报':'提交汇报' }}</el-button></el-empty>
      </section>
    </div>
    <nav v-if="identity.isManager" class="manager-shortcuts" aria-label="项目协作入口"><router-link v-for="item in shortcuts" :key="item.path" :to="item.path"><span class="material-symbols-outlined" aria-hidden="true">{{ item.icon }}</span>{{ item.label }}<span class="material-symbols-outlined" aria-hidden="true">arrow_forward</span></router-link></nav>
  </div>
</template>
<script setup lang="ts">
import {computed,onMounted,ref} from 'vue'
import {useRouter} from 'vue-router'
import {useIdentityStore} from '@/stores/identity'
import {useWorkspaceStore} from '@/stores/workspace'
import request from '@/utils/request'
const identity=useIdentityStore(),workspace=useWorkspaceStore(),router=useRouter(),loading=ref(false),projects=ref<any[]>([]),tasks=ref<any[]>([]),reports=ref<any[]>([])
const today=new Date(),greeting=today.getHours()<12?'上午好':today.getHours()<18?'下午好':'晚上好'
const displayName=computed(()=>identity.user?.realName||identity.user?.username||'伙伴')
const teamName=computed(()=>workspace.spaces.find(s=>String(s.spaceId)===workspace.spaceId)?.spaceName||'我的团队')
const mine=computed(()=>tasks.value.filter(t=>String(t.assigneeId)===String(identity.user?.userId)))
const managedProjects=computed(()=>projects.value.filter(p=>p.canManage))
const personalProgress=computed(()=>mine.value.length?Math.round(mine.value.filter(t=>t.status===2).length*100/mine.value.length):0)
const pendingMilestones=computed(()=>managedProjects.value.filter(p=>p.status===1).flatMap(p=>(p.milestones||[]).filter((m:any)=>!m.done).map((m:any)=>({...m,projectId:p.projectId,projectName:p.name}))).sort((a:any,b:any)=>(a.dueDate||'9999').localeCompare(b.dueDate||'9999')))
const shownReports=computed(()=>identity.isManager?reports.value.filter(r=>r.canReview&&r.status==='submitted'):reports.value.filter(r=>String(r.authorId)===String(identity.user?.userId)))
const metrics=computed(()=>identity.isManager?[
  {label:'负责项目',value:projects.value.filter(p=>p.canManage&&p.status===1).length,icon:'folder_managed',tone:'blue',note:'进行中的项目'},
  {label:'未完成任务',value:tasks.value.filter(t=>t.status<2&&projects.value.some(p=>p.projectId===t.projectId&&p.canManage)).length,icon:'assignment',tone:'green',note:'负责项目内的待办'},
  {label:'待阅汇报',value:shownReports.value.length,icon:'mark_email_unread',tone:'amber',note:'等待你的反馈'}
]:[
  {label:'参与项目',value:projects.value.filter(p=>p.status===1).length,icon:'workspaces',tone:'blue',note:'和团队一起推进'},
  {label:'我的待办',value:mine.value.filter(t=>t.status<2).length,icon:'task_alt',tone:'green',note:'一步一步，稳步完成'},
  {label:'我的汇报',value:shownReports.value.length,icon:'edit_note',tone:'amber',note:'每一份进展都有记录'}
])
const shortcuts=computed(()=>[{path:'/documents',label:'团队文档',icon:'description'},{path:'/tasks',label:identity.isManager?'项目与分工':'我的任务',icon:'assignment'},{path:'/reports',label:identity.isManager?'汇报处理':'写份汇报',icon:'edit_note'},{path:'/im',label:'团队交流',icon:'chat'}])
function shortDate(value:string){return String(value||'').slice(5,10).replace('-','月')+'日'}
function progress(id:string){const all=tasks.value.filter(t=>t.projectId===id);return all.length?Math.round(all.filter(t=>t.status===2).length*100/all.length):0}
onMounted(async()=>{loading.value=true;try{const [p,t,r]:any[]=await Promise.all([request.get('/task/project/list',{params:{spaceId:workspace.spaceId}}),request.get('/task/list'),request.get('/task/reports')]);projects.value=p.data;tasks.value=t.data;reports.value=r.data}finally{loading.value=false}})
</script>
<style scoped>
.role-home{max-width:1280px;margin:0 auto}
.page-kicker{display:block;font-size:var(--text-small);color:var(--c-text-muted);margin-bottom:10px;overflow-wrap:anywhere}
.kicker-divider{color:var(--c-text-ghost);margin:0 8px}
.role-home .material-symbols-outlined{font-size:20px}
.role-home h2{overflow-wrap:anywhere}
.section-heading{display:flex;align-items:center;justify-content:space-between;gap:12px;margin-bottom:var(--space-5)}
.section-heading h3{display:flex;align-items:center;gap:10px;margin:0}
.section-heading .office-link{display:inline-flex;align-items:center;gap:6px;font-size:var(--text-small);white-space:nowrap;text-decoration:none}
.section-heading .office-link .material-symbols-outlined{font-size:16px}
.section-count{font-size:12px;font-weight:500;color:var(--c-text-muted);font-variant-numeric:tabular-nums}
.section-kicker{font-size:11px;color:var(--c-text-muted);display:block;margin-bottom:5px}
.role-metrics{display:grid;grid-template-columns:repeat(3,minmax(0,1fr))}
.role-metrics article{min-width:0}
.metric-label{font-size:var(--text-small);color:var(--c-text-secondary)}
.role-metrics strong{font-variant-numeric:tabular-nums}
.metric-note{font-size:var(--text-small);color:var(--c-text-muted)}
.metric-icon{color:var(--c-brand);background:var(--c-brand-light)}
.green .metric-icon{color:var(--c-success);background:var(--c-success-light)}
.amber .metric-icon{color:var(--c-warning);background:var(--c-warning-light)}
.role-columns{display:grid;gap:var(--space-8)}
.role-columns>section{min-width:0}
.inline-empty{padding:24px 0;font-size:13px;color:var(--c-text-muted)}
.report-meta{display:flex;align-items:center;gap:8px;font-size:11px;color:var(--c-text-muted);min-width:0}
.report-author{color:var(--c-text-secondary);overflow-wrap:anywhere;min-width:0}
.report-meta .el-tag{margin-left:auto;flex-shrink:0}
.report-title{display:block;margin:12px 0 8px;color:var(--c-text);font-weight:600;text-decoration:none;overflow-wrap:anywhere}
.report-title:hover{color:var(--c-brand-hover)}
.report-excerpt{color:var(--c-text-secondary);font-size:13px;line-height:1.7;display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical;overflow:hidden;overflow-wrap:anywhere}
.report-footer{display:flex;gap:8px;align-items:center;justify-content:space-between;margin-top:16px;font-size:11px;color:var(--c-text-muted)}
.report-footer>span{overflow:hidden;white-space:nowrap;text-overflow:ellipsis}
.report-footer a{display:inline-flex;align-items:center;gap:5px;flex-shrink:0;color:var(--c-text-secondary);text-decoration:none}
.report-footer a .material-symbols-outlined{font-size:15px}
.report-footer a:hover{color:var(--c-brand-hover)}

.personal-heading{display:flex;justify-content:space-between;align-items:center;gap:32px;padding:8px 0 32px}
.personal-heading h2{font-size:36px;line-height:1.4;font-weight:600;margin:16px 0 12px}.welcome-dot{color:var(--c-success)}
.personal-heading p{color:var(--c-text-secondary);font-size:13px;margin-top:8px}.personal-heading .welcome-name{font-size:16px;color:var(--c-text);font-weight:500}
.personal-focus{display:grid;justify-items:center;gap:12px;flex-shrink:0;padding-right:24px}
.completion-ring{width:126px;height:126px;border-radius:50%;padding:7px;background:conic-gradient(var(--c-success) var(--completion-angle),var(--c-border) 0)}
.completion-ring>div{width:100%;height:100%;border-radius:50%;background:var(--c-bg);display:flex;align-items:center;justify-content:center;flex-direction:column}
.completion-ring strong{font-size:30px;font-weight:600;font-variant-numeric:tabular-nums}.completion-ring small{font-size:13px;margin-left:3px;color:var(--c-text-secondary)}
.completion-ring div>span,.focus-caption{font-size:11px;color:var(--c-text-muted)}
.member-home .role-metrics{gap:16px}
.member-home .role-metrics article{display:grid;grid-template-columns:44px minmax(0,1fr);column-gap:14px;align-items:center;padding:20px;background:var(--c-surface);border:1px solid var(--c-border-light);border-radius:var(--radius-lg);box-shadow:var(--shadow-xs)}
.member-home .metric-icon{width:44px;height:44px;border-radius:var(--radius-md);display:grid;place-items:center;grid-row:1 / span 2}
.member-home .role-metrics article>div{display:flex;align-items:baseline;gap:12px;justify-content:space-between}
.member-home .role-metrics strong{font-size:28px;font-weight:600}
.member-home .metric-note{font-size:11px}
.personal-shortcuts{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:24px;padding:20px 0 28px;margin-bottom:8px;border-bottom:1px solid var(--c-border)}
.personal-shortcuts a{display:flex;align-items:center;gap:10px;text-decoration:none;color:var(--c-text-secondary);font-size:13px;padding:12px 0;min-width:0;transition:color .15s}
.personal-shortcuts a:hover{color:var(--c-text)}
.shortcut-icon{width:32px;height:32px;display:grid;place-items:center;border:1px solid var(--c-border);border-radius:var(--radius-md);color:var(--c-text-secondary);background:var(--c-surface);flex-shrink:0}
.shortcut-arrow{font-size:16px!important;color:var(--c-text-muted);margin-left:auto}
.member-home .role-columns{grid-template-columns:minmax(0,1.35fr) minmax(0,1fr);margin-top:28px}
.member-home .section-heading .office-link{color:var(--c-text-secondary)}
.personal-progress{display:flex;gap:16px;align-items:center;margin-bottom:16px;color:var(--c-text-muted);font-size:11px}
.personal-progress .el-progress{width:100px;flex-shrink:0}
.personal-task{display:flex;align-items:center;gap:12px;padding:24px 20px;margin-bottom:12px;width:100%;border:1px solid var(--c-border-light);border-radius:var(--radius-lg);background:var(--c-surface);text-align:left;color:var(--c-text);cursor:pointer;transition:border-color .15s,box-shadow .15s}
.personal-task:hover{border-color:var(--c-success);box-shadow:var(--shadow-sm)}
.task-symbol{color:var(--c-text-ghost);font-size:22px!important;flex-shrink:0}
.task-symbol.active{color:var(--c-success)}
.task-copy{flex:1;min-width:0}
.task-copy strong{display:block;font-size:14px;font-weight:500;overflow-wrap:anywhere}
.task-copy small{display:block;margin-top:8px;color:var(--c-text-muted);font-size:11px;line-height:1.6}
.task-copy small span{margin:0 6px}
.task-status{font-size:11px;white-space:nowrap;color:var(--c-text-secondary);background:var(--c-bg);padding:3px 7px;border-radius:var(--radius-sm)}
.task-status.active{color:var(--c-text-secondary);background:var(--c-success-light)}
.task-arrow{color:var(--c-text-ghost);font-size:17px!important}
.personal-report-link{display:flex;align-items:center;gap:12px;margin-top:24px;padding:16px 0;font-size:12px;text-decoration:none;color:var(--c-text-secondary)}
.personal-report-link>span:first-child{color:var(--c-success)}
.personal-report-link>span:last-child{margin-left:auto;font-size:16px}
.personal-report-link:hover{color:var(--c-text)}
.member-home .report-row{background:var(--c-surface);border:1px solid var(--c-border-light);border-radius:var(--radius-lg);padding:18px;margin-bottom:12px}
.joined-projects{border-top:1px solid var(--c-border);padding-top:24px;margin-top:32px}
.joined-project-list{display:grid;grid-template-columns:1fr;gap:12px}
.joined-project{display:flex;align-items:center;gap:14px;min-width:0;color:var(--c-text);text-decoration:none;padding:16px 0}
.joined-icon{width:44px;height:44px;background:var(--c-success-light);color:var(--c-success);display:grid;place-items:center;border-radius:var(--radius-md);flex-shrink:0}
.joined-project>div{flex:1;min-width:0}
.joined-project strong{display:block;font-size:13px;font-weight:500;overflow-wrap:anywhere}
.joined-project small{display:block;font-size:11px;color:var(--c-text-muted);margin-top:6px}
.joined-project>span:last-child{color:var(--c-text-muted);font-size:16px}

.manager-home{display:grid;grid-template-columns:160px minmax(0,1fr);column-gap:28px;row-gap:28px}
.manager-heading{grid-column:1 / -1;display:flex;align-items:center;justify-content:space-between;gap:16px;padding-bottom:24px;border-bottom:1px solid var(--c-border)}
.manager-title{display:flex;align-items:center;gap:14px;min-width:0}.manager-title>div{min-width:0}.manager-title .page-kicker{font-size:11px;margin-bottom:5px}.manager-mark{width:44px;height:44px;background:var(--c-brand);color:#fff;display:grid;place-items:center;border-radius:var(--radius-md);flex-shrink:0;font-size:24px!important}
.manager-home .role-metrics{grid-column:1;grid-row:2;display:flex;flex-direction:column;margin:0;padding:0 24px 0 0;border-right:1px solid var(--c-border);gap:0}
.manager-home .role-metrics article{padding:18px 0 24px;border-bottom:1px solid var(--c-border-light)}.manager-home .role-metrics article:first-child{padding-top:0}.manager-home .role-metrics article:last-child{border-bottom:0}
.manager-home .role-metrics article{display:grid;grid-template-columns:minmax(0,1fr) 40px;align-items:start;column-gap:12px}
.manager-home .metric-icon{grid-column:2;grid-row:1;width:36px;height:36px;display:grid;place-items:center;border-radius:var(--radius-md)}
.manager-home .role-metrics article>div{grid-column:1;grid-row:1}
.manager-home .role-metrics strong{font-size:36px;display:block;margin-top:8px;line-height:1.3}
.manager-home .metric-note{grid-column:1 / -1;margin-top:8px;font-size:11px}
.manager-home .role-columns{grid-column:2;grid-row:2;grid-template-columns:minmax(0,1.6fr) minmax(220px,1fr);gap:24px}
.project-table-heading{display:grid;grid-template-columns:minmax(0,1.6fr) minmax(80px,.7fr) minmax(70px,.5fr) 32px;gap:16px;background:var(--c-bg);padding:10px 12px;font-size:11px;color:var(--c-text-muted)}
.managed-project{display:grid;grid-template-columns:minmax(0,1.6fr) minmax(80px,.7fr) minmax(70px,.5fr) 32px;align-items:center;gap:16px;padding:24px 12px;border-bottom:1px solid var(--c-border)}
.project-identity{display:flex;align-items:flex-start;gap:10px;min-width:0}
.project-icon{color:var(--c-brand);font-size:21px}
.project-identity>div{min-width:0}
.project-title{display:block;color:var(--c-text);font-size:13px;font-weight:600;overflow-wrap:anywhere;text-decoration:none}
.project-title:hover{color:var(--c-brand-hover)}
.project-identity small{display:block;color:var(--c-text-muted);font-size:11px;margin-top:8px}
.project-progress strong{font-size:20px;font-weight:500;display:block;margin-bottom:10px}
.project-progress small{font-size:11px;margin-left:2px;color:var(--c-text-muted)}
.project-task-count strong{font-size:16px;display:block;font-weight:500}
.project-task-count small{display:block;font-size:10px;margin-top:7px;color:var(--c-text-muted)}
.managed-project .el-button{width:30px;height:30px}
.milestone-section{margin-top:32px}
.milestone-row{display:flex;align-items:center;gap:12px;padding:14px 0;border-bottom:1px solid var(--c-border-light)}
.milestone-row>.material-symbols-outlined{color:var(--c-brand);font-size:18px}
.milestone-row>div{min-width:0;flex:1}
.milestone-row strong{font-size:13px;font-weight:500;overflow-wrap:anywhere}
.milestone-row small{display:block;color:var(--c-text-muted);font-size:11px;margin-top:4px}
.milestone-row time{font-size:11px;color:var(--c-text-secondary);flex-shrink:0}
.manager-home .report-section{border-left:1px solid var(--c-border);padding-left:24px}
.manager-home .report-row{padding:18px 0 18px 14px;border-left:2px solid var(--c-warning);border-bottom:1px solid var(--c-border-light);margin-bottom:16px}
.manager-home .report-title{font-size:14px}
.manager-shortcuts{grid-column:1 / -1;display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:24px;border-top:1px solid var(--c-border);padding-top:24px}
.manager-shortcuts a{display:flex;align-items:center;gap:8px;text-decoration:none;font-size:12px;color:var(--c-text-secondary);min-width:0}
.manager-shortcuts a>.material-symbols-outlined:first-child{color:var(--c-brand)}
.manager-shortcuts a>.material-symbols-outlined:last-child{font-size:15px;margin-left:auto}
.manager-shortcuts a:hover{color:var(--c-brand-hover)}
.role-home a:focus-visible,.personal-task:focus-visible{outline:2px solid var(--c-brand);outline-offset:4px;border-radius:var(--radius-sm)}
@media(max-width:1100px){.manager-home .role-columns{grid-template-columns:1fr}.manager-home .report-section{border-left:0;padding-left:0}}
@media(max-width:850px){.member-home .role-columns{grid-template-columns:1fr}.personal-shortcuts{gap:12px}.member-home .role-metrics{gap:12px}.member-home .metric-note{display:none}.member-home .role-metrics article{grid-template-columns:1fr;gap:10px;padding:16px}.member-home .metric-icon{grid-row:1;width:32px;height:32px}.member-home .role-metrics article>div{display:block}.member-home .role-metrics strong{display:block;margin-top:4px}.manager-home{grid-template-columns:124px minmax(0,1fr);column-gap:20px}.manager-home .role-metrics{padding-right:16px}.manager-home .role-metrics article{display:block}.manager-home .metric-icon{margin-bottom:12px}.personal-focus{padding-right:0}}
@media(max-width:600px){
  .personal-heading{align-items:center;gap:12px;padding:8px 0 24px}.personal-welcome{min-width:0}.personal-heading h2{font-size:28px;margin:10px 0}.personal-heading p{font-size:11px}.personal-heading .welcome-name{font-size:13px;overflow-wrap:anywhere}.page-kicker{font-size:10px;line-height:1.6}.completion-ring{width:80px;height:80px;padding:5px}.completion-ring strong{font-size:22px}.completion-ring div>span{font-size:9px}.focus-caption{font-size:9px}.personal-focus{gap:8px}
  .member-home .role-metrics{gap:8px}.member-home .role-metrics article{padding:12px 10px}.metric-label{font-size:11px}.member-home .role-metrics strong{font-size:24px}
  .personal-shortcuts{grid-template-columns:repeat(2,minmax(0,1fr));gap:4px 16px;padding:16px 0 24px}.personal-shortcuts a{font-size:12px;padding:8px 0}
  .personal-task{padding:16px 12px;gap:8px;flex-wrap:wrap}.task-copy{flex-basis:calc(100% - 40px)}.task-status{margin-left:30px}.task-arrow{margin-left:auto}
  .member-home .report-row{padding:14px}.section-heading{gap:8px}.section-heading h3{font-size:15px}.section-heading .office-link{font-size:11px}.joined-project-list{grid-template-columns:1fr}
  .manager-home{display:block}.manager-heading{flex-wrap:wrap;padding-bottom:20px}.manager-title h2{font-size:20px}.manager-heading .el-button{font-size:12px}.manager-home .role-metrics{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:10px;padding:20px 0;margin-bottom:24px;border-right:0;border-bottom:1px solid var(--c-border)}.manager-home .role-metrics article,.manager-home .role-metrics article:first-child{display:block;border:0;padding:0}.manager-home .metric-icon{margin-bottom:10px;width:28px;height:28px;font-size:17px}.manager-home .role-metrics strong{font-size:26px}.manager-home .metric-note{display:block;font-size:10px;line-height:1.6}.manager-shortcuts{margin-top:24px}
  .project-table-heading{display:none}.managed-project{grid-template-columns:minmax(0,1fr) 32px;padding:18px 0;gap:16px}.project-identity{grid-column:1;grid-row:1}.managed-project>.el-button{grid-column:2;grid-row:1}.project-progress{grid-column:1;grid-row:2}.project-task-count{grid-column:2;grid-row:2;text-align:center}.project-task-count small{display:none}.project-progress strong{font-size:18px}
  .milestone-row{gap:8px}.milestone-row time{font-size:10px}.manager-shortcuts{grid-template-columns:repeat(2,minmax(0,1fr));gap:20px 16px}.report-meta{flex-wrap:wrap}.report-meta time{font-size:10px}
}
</style>
