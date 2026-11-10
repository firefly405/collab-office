<template><div class="office-page" v-loading="loading"><div class="office-toolbar"><h2>{{ identity.isAdmin?'团队与权限':'团队成员' }}</h2><div class="office-actions"><el-button v-if="identity.isAdmin" @click="create">创建团队</el-button><el-button v-if="admin" type="primary" @click="invite">添加成员</el-button></div></div><el-table :data="members"><el-table-column prop="userName" label="成员" min-width="200" /><el-table-column label="团队角色" width="120"><template #default="{row}">{{ roles[row.roleType-1]||'成员' }}</template></el-table-column><el-table-column label="加入时间" min-width="160"><template #default="{row}">{{ String(row.joinTime||'').replace('T',' ').slice(0,16) }}</template></el-table-column><el-table-column v-if="admin" label="操作" width="230"><template #default="{row}"><div v-if="String(row.userId)!==ownerId" class="office-actions"><el-select :model-value="row.roleType" style="width:120px" @change="role(row,$event)"><el-option v-for="(r,i) in roles" :key="r" :label="r" :value="i+1" /></el-select><el-button text type="danger" @click="remove(row)">移除</el-button></div></template></el-table-column></el-table>
  <el-dialog v-model="visible" title="添加成员" width="min(460px, 94vw)"><el-select v-model="selected" multiple filterable remote :remote-method="directory" :loading="searching" placeholder="输入已注册的用户名" style="width:100%"><el-option v-for="u in users" :key="u.userId" :label="(u.realName||u.username)+' ('+u.username+')'" :value="String(u.userId)" /></el-select><template #footer><el-button @click="visible=false">取消</el-button><el-button type="primary" :disabled="!selected.length" @click="add">添加</el-button></template></el-dialog>
<OrganizationSettingsPanel :admin="admin" :owner-id="ownerId" :user-id="userId" />
</div></template>
<script setup lang="ts">
import {computed,onMounted,ref} from 'vue'
import {ElMessage,ElMessageBox} from 'element-plus'
import request from '@/utils/request'
import {useWorkspaceStore} from '@/stores/workspace'
import OrganizationSettingsPanel from './OrganizationSettingsPanel.vue'
import {useIdentityStore} from '@/stores/identity'
const identity=useIdentityStore()
const workspace=useWorkspaceStore(),members=ref<any[]>([]),users=ref<any[]>([]),selected=ref<string[]>([]),userId=ref(''),ownerId=ref(''),visible=ref(false),loading=ref(false),searching=ref(false),roles=['管理员','成员','访客']
const admin=computed(()=>identity.isAdmin&&members.value.some(m=>String(m.userId)===userId.value&&m.roleType===1))
async function load(){if(!workspace.spaceId)return;loading.value=true;try{const r:any[]=await Promise.all([request.get(`/space/${workspace.spaceId}/members`),request.get('/user/info'),request.get('/space/'+workspace.spaceId)]);members.value=r[0].data;userId.value=String(r[1].data.userId);ownerId.value=String(r[2].data.ownerId)}catch{}finally{loading.value=false}}
onMounted(load)
async function create(){try{const {value}=await ElMessageBox.prompt('团队名称','创建团队',{inputValidator:v=>!!v?.trim()&&v.length<=100||'请输入1到100字'});const r:any=await request.post('/space/create',{spaceName:value.trim()});await workspace.load();workspace.select(String(r.data.spaceId));await load()}catch{}}
async function directory(keyword:string){searching.value=true;try{const r:any=await request.get('/user/directory',{params:{keyword}});users.value=r.data.filter((u:any)=>!members.value.some(m=>String(m.userId)===String(u.userId)))}catch{}finally{searching.value=false}}
async function invite(){selected.value=[];await directory('');visible.value=true}
async function add(){try{await request.post(`/space/${workspace.spaceId}/members`,selected.value);visible.value=false;await load();ElMessage.success('成员已添加')}catch{}}
async function role(m:any,value:number){try{await request.put(`/space/${workspace.spaceId}/members/role`,{spaceId:workspace.spaceId,userId:m.userId,roleType:value});await load()}catch{}}
async function remove(m:any){try{await ElMessageBox.confirm('移除“'+m.userName+'”的团队权限？','移除成员',{type:'warning'});await request.delete(`/space/${workspace.spaceId}/members/${m.userId}`);await load()}catch{}}
</script>
