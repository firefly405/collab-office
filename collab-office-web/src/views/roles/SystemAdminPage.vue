<template>
  <div class="office-page admin-home" v-loading="loading">
    <header class="admin-heading"><div class="admin-title"><span class="admin-emblem material-symbols-outlined" aria-hidden="true">admin_panel_settings</span><div><span class="admin-kicker">系统管理员 / 全局管理</span><h2>管理控制台</h2></div></div><div class="office-actions"><time v-if="updatedAt" class="updated-time">更新于 {{ updatedAt }}</time><el-button circle title="刷新管理数据" @click="load"><span class="material-symbols-outlined" aria-hidden="true">refresh</span></el-button><el-button @click="router.push('/space/manage')"><span class="material-symbols-outlined" aria-hidden="true">settings</span>团队与权限</el-button></div></header>
    <div class="admin-summary" aria-label="全局管理统计"><article v-for="m in metrics" :key="m.label"><span class="summary-label"><span class="material-symbols-outlined" aria-hidden="true">{{ m.icon }}</span>{{ m.label }}</span><strong>{{ m.value }}</strong></article></div>
    <div class="admin-columns">
      <section class="accounts-section" aria-labelledby="accounts-heading">
        <div class="accounts-heading"><h3 id="accounts-heading">账号与身份 <span>{{ users.length }}</span></h3><form class="account-search" @submit.prevent="load"><el-input v-model="keyword" placeholder="查找账号或姓名" aria-label="查找账号或姓名" clearable/><el-button native-type="submit" circle title="搜索账号"><span class="material-symbols-outlined" aria-hidden="true">search</span></el-button></form></div>
        <table class="account-table"><thead><tr><th scope="col">账号</th><th scope="col">姓名</th><th scope="col">系统身份</th><th scope="col">状态</th></tr></thead><tbody><tr v-for="user in users" :key="user.userId">
          <td class="account-name"><div class="account-identity"><span class="account-avatar" :class="user.platformRole.toLowerCase()">{{ user.username.slice(0,1).toUpperCase() }}</span><div><strong>{{ user.username }}</strong><small>ID {{ user.userId }}<span v-if="String(user.userId)===String(identity.user?.userId)"> · 当前账号</span></small></div></div></td>
          <td class="real-name">{{ user.realName||'—' }}</td>
          <td class="identity-cell"><span class="mobile-field-label">系统身份</span><el-select :model-value="user.platformRole" :disabled="String(user.userId)===String(identity.user?.userId)||saving===user.userId" :aria-label="`修改 ${user.username} 身份`" @change="change(user,$event)"><el-option v-for="r in roles" :key="r.value" :label="r.label" :value="r.value"/></el-select></td>
          <td class="account-status"><span :class="user.status===1?'enabled':'disabled'"><span class="status-mark" aria-hidden="true"></span>{{ user.status===1?'启用':'停用' }}</span></td>
        </tr></tbody></table><el-empty v-if="!users.length" description="没有找到匹配的账号"/>
      </section>
      <aside class="admin-aside">
        <section aria-labelledby="management-heading"><h3 id="management-heading">管理入口</h3><nav class="management-links"><router-link v-for="item in managementLinks" :key="item.path" :to="item.path"><span class="material-symbols-outlined" aria-hidden="true">{{ item.icon }}</span><span>{{ item.label }}</span><span class="material-symbols-outlined" aria-hidden="true">chevron_right</span></router-link></nav></section>
        <section class="operation-trend" aria-labelledby="trend-heading"><div class="trend-heading"><h3 id="trend-heading">近 7 日管理变更</h3><strong>{{ trendTotal }}</strong></div><div class="trend-chart" role="img" :aria-label="`近7日管理变更：${(stats.trend||[]).map((d:any)=>d.date+'，'+d.count+'次').join('；')}`"><div v-for="d in stats.trend||[]" :key="d.date" class="trend-day" :title="`${d.date}：${d.count} 次管理变更`"><span class="trend-value">{{ d.count }}</span><div class="trend-track"><span :style="{height:Math.max(2,Number(d.count)/trendMax*72)+'px'}"></span></div><time>{{ d.date.slice(5).replace('-','/') }}</time></div></div></section>
        <section class="account-distribution" aria-labelledby="distribution-heading"><h3 id="distribution-heading">当前名单身份</h3><div v-for="role in roles" :key="role.value"><span class="distribution-mark" :class="role.value.toLowerCase()"></span><span>{{ role.label }}</span><strong>{{ users.filter(u=>u.platformRole===role.value).length }}</strong></div></section>
      </aside>
    </div>
  </div>
</template>
<script setup lang="ts">
import {computed,onMounted,ref} from 'vue'
import {useRouter} from 'vue-router'
import {ElMessage,ElMessageBox} from 'element-plus'
import request from '@/utils/request'
import {useIdentityStore} from '@/stores/identity'
const identity=useIdentityStore(),router=useRouter(),users=ref<any[]>([]),stats=ref<any>({}),keyword=ref(''),loading=ref(false),saving=ref<number|null>(null),updatedAt=ref('')
const roles=[{value:'SYSTEM_ADMIN',label:'系统管理员'},{value:'PROJECT_ADMIN',label:'项目管理员'},{value:'MEMBER',label:'普通用户'}]
const metrics=computed(()=>[{label:'启用账号',value:stats.value.activeUsers||0,icon:'person_check'},{label:'有效团队',value:stats.value.spaces||0,icon:'groups'},{label:'今日管理变更',value:stats.value.todayOperations||0,icon:'touch_app'},{label:'管理审计记录',value:stats.value.totalLogs||0,icon:'receipt_long'}])
const managementLinks=[{path:'/space/manage',label:'团队与权限',icon:'groups'},{path:'/tasks',label:'项目与任务',icon:'assignment'},{path:'/statistics',label:'统计与数据导出',icon:'monitoring'},{path:'/reports',label:'项目汇报',icon:'forum'},{path:'/documents',label:'团队文档',icon:'description'}]
const trendTotal=computed(()=>(stats.value.trend||[]).reduce((sum:number,d:any)=>sum+Number(d.count),0))
const trendMax=computed(()=>Math.max(1,...(stats.value.trend||[]).map((d:any)=>Number(d.count))))
async function load(){loading.value=true;try{const [u,s]:any[]=await Promise.all([request.get('/admin/roles/users',{params:{keyword:keyword.value}}),request.get('/admin/dashboard/stats')]);users.value=u.data;stats.value=s.data;updatedAt.value=new Date().toLocaleTimeString('zh-CN',{hour:'2-digit',minute:'2-digit'})}finally{loading.value=false}}
async function change(user:any,role:string){try{await ElMessageBox.confirm(`将 ${user.username} 设为${roles.find(r=>r.value===role)?.label}？`,'修改系统身份',{type:'warning'})}catch{return}saving.value=user.userId;try{await request.put('/admin/roles/users/'+user.userId,{platformRole:role});await load();ElMessage.success('身份已更新')}finally{saving.value=null}}
onMounted(load)
</script>
<style scoped>
.admin-home{max-width:1440px;margin:auto}
.admin-heading{display:flex;align-items:center;justify-content:space-between;gap:16px;padding:0 0 16px;border-bottom:1px solid var(--c-border)}
.admin-title h2{font-size:20px}.admin-home .office-actions .el-button{font-size:12px}
.admin-title{display:flex;align-items:center;gap:12px;min-width:0}
.admin-emblem{display:grid;place-items:center;width:34px;height:34px;border:1px solid var(--c-border);background:var(--c-text);color:var(--c-surface);border-radius:var(--radius-sm);flex-shrink:0;font-size:21px}
.admin-kicker{font-size:11px;color:var(--c-text-muted);display:block;margin-bottom:4px}
.updated-time{font-size:11px;color:var(--c-text-muted);margin-right:4px}
.admin-home .material-symbols-outlined{font-size:19px}
.admin-summary{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:12px;margin:16px 0 24px}
.admin-summary article{display:flex;justify-content:space-between;align-items:center;gap:12px;padding:16px;background:var(--c-surface);border:1px solid var(--c-border);border-radius:var(--radius-sm);min-width:0}
.summary-label{display:flex;align-items:center;gap:8px;font-size:12px;color:var(--c-text-secondary)}
.summary-label .material-symbols-outlined{font-size:17px;color:var(--c-text-muted)}
.admin-summary strong{font-size:24px;font-weight:600;display:block;font-variant-numeric:tabular-nums}
.admin-columns{display:grid;grid-template-columns:minmax(0,1fr) 232px;gap:24px}
.accounts-section{min-width:0}
.accounts-heading{display:flex;align-items:center;justify-content:space-between;gap:16px;margin-bottom:12px}
.accounts-heading h3{margin:0;white-space:nowrap}.accounts-heading h3>span{font-size:11px;font-weight:400;color:var(--c-text-muted);margin-left:6px}
.account-search{display:flex;align-items:center;gap:8px;width:240px;max-width:100%}
.account-table{width:100%;border-collapse:collapse;text-align:left;font-size:12px;background:var(--c-surface);border:1px solid var(--c-border)}
.account-table th{background:var(--c-surface-dim);font-size:11px;font-weight:500;color:var(--c-text-secondary);padding:10px 12px;white-space:nowrap}
.account-table td{padding:9px 12px;border-bottom:1px solid var(--c-border-light)}
.account-table tbody tr:hover{background:var(--c-surface-hover)}
.account-identity{display:flex;gap:10px;align-items:center;min-width:0}.account-identity>div{min-width:0}
.account-avatar{width:26px;height:26px;display:grid;place-items:center;flex-shrink:0;font-size:11px;font-weight:500;border-radius:var(--radius-sm);background:var(--c-success-light);color:var(--c-success)}
.account-avatar.system_admin{background:var(--c-surface-dim);color:var(--c-text)}
.account-avatar.project_admin{background:var(--c-brand-light);color:var(--c-brand-hover)}
.account-identity strong{display:block;font-size:12px;font-weight:500;overflow-wrap:anywhere}
.account-identity small{display:block;font-size:10px;color:var(--c-text-muted);margin-top:4px}
.real-name{color:var(--c-text-secondary)}
.identity-cell{width:190px}.identity-cell .el-select{width:166px;max-width:100%}
.mobile-field-label{display:none}
.account-status{width:74px;white-space:nowrap;font-size:11px}
.account-status>span{display:inline-flex;align-items:center;gap:5px;color:var(--c-text-secondary)}
.status-mark{width:5px;height:5px;display:inline-block;border-radius:50%;background:var(--c-text-ghost)}
.enabled .status-mark{background:var(--c-success)}
.admin-aside{padding-left:20px;border-left:1px solid var(--c-border);min-width:0}
.admin-aside h3{font-size:13px;margin-bottom:12px}
.management-links{display:grid;gap:4px}
.management-links a{display:flex;align-items:center;gap:10px;color:var(--c-text-secondary);text-decoration:none;padding:10px 0;font-size:12px;min-width:0}
.management-links a>span:nth-child(2){min-width:0;overflow-wrap:anywhere}
.management-links a>span:last-child{margin-left:auto;color:var(--c-text-muted);font-size:16px}
.management-links a:hover{color:var(--c-brand-hover)}
.operation-trend{padding-top:24px;margin-top:24px;border-top:1px solid var(--c-border)}
.trend-heading{display:flex;align-items:center;justify-content:space-between;gap:12px}.trend-heading h3{margin:0}.trend-heading strong{font-size:16px;font-weight:500}
.trend-chart{height:128px;display:grid;grid-template-columns:repeat(7,minmax(0,1fr));gap:6px;padding-top:16px}
.trend-day{display:flex;flex-direction:column;align-items:center;min-width:0;font-variant-numeric:tabular-nums}
.trend-value{font-size:10px;color:var(--c-text-muted)}
.trend-track{height:72px;width:100%;display:flex;align-items:flex-end;justify-content:center;margin:6px 0}
.trend-track>span{display:block;width:12px;max-width:100%;background:var(--c-text-secondary);border-radius:2px 2px 0 0}
.trend-day time{font-size:9px;color:var(--c-text-muted)}
.account-distribution{margin-top:28px;padding-top:20px;border-top:1px solid var(--c-border)}
.account-distribution>div{display:flex;align-items:center;gap:8px;padding:8px 0;font-size:11px;color:var(--c-text-secondary)}
.account-distribution strong{margin-left:auto;font-weight:500;color:var(--c-text)}
.distribution-mark{width:7px;height:7px;border-radius:2px;background:var(--c-success)}
.distribution-mark.system_admin{background:var(--c-text-secondary)}.distribution-mark.project_admin{background:var(--c-brand)}
.management-links a:focus-visible{outline:2px solid var(--c-brand);outline-offset:3px}
@media(max-width:1100px){.admin-columns{grid-template-columns:1fr}.admin-aside{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:24px;padding:24px 0 0;border-left:0;border-top:1px solid var(--c-border)}.operation-trend,.account-distribution{margin:0;padding:0;border:0}}
@media(max-width:600px){
  .admin-heading{flex-wrap:wrap;gap:16px}.admin-heading .office-actions{width:100%;justify-content:flex-end}.updated-time{margin-right:auto}.admin-title h2{font-size:20px}
  .admin-summary{grid-template-columns:repeat(2,minmax(0,1fr));gap:8px;margin:16px 0 24px}.admin-summary article{display:block;padding:12px}.admin-summary strong{font-size:24px;margin-top:7px}.summary-label{font-size:11px;gap:5px}
  .accounts-heading{align-items:flex-start;flex-wrap:wrap;gap:12px}.account-search{width:100%}
  .account-table thead{display:none}.account-table tbody tr{display:grid;grid-template-columns:minmax(0,1fr) auto;padding:12px;border-bottom:1px solid var(--c-border)}
  .account-table td{border:0;padding:0}.account-name{grid-row:1;grid-column:1}.account-status{grid-row:1;grid-column:2;align-self:center;width:auto}.account-table .real-name{grid-column:1 / -1;grid-row:2;padding-left:40px;font-size:11px;margin-top:4px}
  .identity-cell{grid-column:1 / -1;grid-row:3;display:flex;align-items:center;justify-content:space-between;gap:12px;width:100%;margin-top:12px}.identity-cell .el-select{width:166px}.mobile-field-label{display:block;font-size:11px;color:var(--c-text-muted)}
  .admin-aside{grid-template-columns:1fr}.management-links{grid-template-columns:repeat(2,minmax(0,1fr));gap:6px 16px}.management-links a{font-size:11px;gap:6px}.management-links .material-symbols-outlined{font-size:17px}.trend-chart{max-width:340px}.account-distribution{padding-top:20px;border-top:1px solid var(--c-border)}
}
</style>
