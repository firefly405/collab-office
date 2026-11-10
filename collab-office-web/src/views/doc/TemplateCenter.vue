<template>
  <section class="template-workspace">
    <div class="office-toolbar"><h1>模板中心</h1><el-button type="primary" @click="createVisible=true">保存文档为模板</el-button></div>
    <div class="office-toolbar"><el-input v-model="keyword" placeholder="搜索模板名称或分类" clearable/><el-select v-model="type" aria-label="模板类型" clearable placeholder="全部类型"><el-option v-for="(name,index) in typeNames.slice(1)" :key="index" :label="name" :value="index+1"/></el-select></div>
    <div class="template-grid" v-loading="loading"><article v-for="tpl in filtered" :key="tpl.templateId"><div class="template-icon"><span class="material-symbols-outlined">{{ icons[tpl.type] }}</span><el-tag v-if="tpl.isOfficial===1" size="small">官方</el-tag></div><h3>{{ tpl.name }}</h3><p class="office-muted">{{ tpl.category || '通用' }} · {{ typeNames[tpl.type] }} · 使用 {{ tpl.usageCount }} 次</p><div class="office-toolbar"><el-button type="primary" :loading="using===tpl.templateId" @click="use(tpl)">使用模板</el-button><el-button v-if="String(tpl.creatorId)===userId&&tpl.isOfficial!==1" circle title="编辑模板" @click="rename(tpl)"><span class="material-symbols-outlined">edit</span></el-button><el-button v-if="String(tpl.creatorId)===userId&&tpl.isOfficial!==1" circle title="删除模板" @click="remove(tpl)"><span class="material-symbols-outlined">delete</span></el-button></div></article></div>
    <el-empty v-if="!loading&&!filtered.length" description="暂无模板"><el-button @click="createVisible=true">保存文档为模板</el-button></el-empty>
    <el-dialog v-model="createVisible" title="保存文档为模板" width="min(480px,94vw)"><form class="office-form" @submit.prevent="create"><label>来源文档<el-select v-model="source" filterable required><el-option v-for="doc in docs" :key="doc.docId" :label="doc.title" :value="doc.docId"/></el-select></label><label>模板名称<el-input v-model="name" maxlength="100" required/></label><label>分类<el-input v-model="category" maxlength="50"/></label><el-button type="primary" native-type="submit" :loading="loading">保存模板</el-button></form></el-dialog>
  </section>
</template>
<script setup lang="ts">
import {computed,onMounted,ref} from 'vue'
import {useRouter} from 'vue-router'
import {ElMessage,ElMessageBox} from 'element-plus'
import request from '@/utils/request'
import {useWorkspaceStore} from '@/stores/workspace'
const router=useRouter(),workspace=useWorkspaceStore(),templates=ref<any[]>([]),docs=ref<any[]>([]),keyword=ref(''),type=ref<number>(),loading=ref(false),using=ref(''),createVisible=ref(false),source=ref(''),name=ref(''),category=ref('通用'),userId=ref('')
const typeNames=['','文档','表格','思维导图','白板'],icons=['','description','table_chart','account_tree','draw']
const filtered=computed(()=>templates.value.filter(t=>(!type.value||t.type===type.value)&&[t.name,t.category].join(' ').toLowerCase().includes(keyword.value.toLowerCase())))
async function load(){loading.value=true;try{const r:any=await request.get('/template/list',{params:{pageSize:100}});templates.value=r.data.records}catch{}finally{loading.value=false}}
async function create(){if(!source.value||!name.value.trim())return;loading.value=true;try{await request.post('/template/create',{sourceDocId:source.value,name:name.value.trim(),category:category.value});createVisible.value=false;await load();ElMessage.success('模板已保存，团队成员可使用')}catch{}finally{loading.value=false}}
async function use(tpl:any){if(using.value)return;using.value=tpl.templateId;try{const r:any=await request.post(`/template/${tpl.templateId}/use`);ElMessage.success('已创建独立文档');await router.push(`/${['','documents','sheets','mindmap','whiteboard'][tpl.type]}/${r.data}`)}catch{}finally{using.value=''}}
async function rename(tpl:any){try{const r=await ElMessageBox.prompt('模板名称','编辑模板',{inputValue:tpl.name,inputValidator:v=>!!v?.trim()&&v.length<=100});await request.put('/template/'+tpl.templateId,{name:r.value,category:tpl.category});await load()}catch{}}
async function remove(tpl:any){try{await ElMessageBox.confirm('确认删除模板？已创建的文档不受影响。','删除模板');await request.delete('/template/'+tpl.templateId);await load()}catch{}}
onMounted(async()=>{await load();try{const r:any=await request.get('/document/list',{params:{spaceId:workspace.spaceId,pageSize:100}});docs.value=r.data.records;const u:any=await request.get('/user/info');userId.value=String(u.data.userId)}catch{}})
</script>
<style scoped>
.template-workspace h1{margin:0;font-size:var(--text-heading)}.template-workspace .office-toolbar{flex-wrap:wrap}.template-workspace .el-input{max-width:360px}.template-workspace .el-select{max-width:220px}.template-grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(min(260px,100%),1fr));gap:var(--space-4)}.template-grid article{padding:var(--space-5);border:1px solid var(--c-border);border-radius:var(--radius-md);background:var(--c-bg)}.template-grid h3{font-size:17px;overflow-wrap:anywhere}.template-icon{display:flex;align-items:center;justify-content:space-between;color:var(--c-brand)}.template-icon>.material-symbols-outlined{font-size:32px}.template-grid article:hover{border-color:var(--c-brand);box-shadow:var(--shadow-sm)}.template-grid .office-toolbar{margin-bottom:0}.template-grid .material-symbols-outlined{font-size:20px}
</style>
