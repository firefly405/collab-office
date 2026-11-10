<template><div class="office-page" v-loading="loading"><div class="office-toolbar"><h2>云盘</h2><div class="office-actions"><el-button @click="newFolder"><span class="material-symbols-outlined">create_new_folder</span>新建文件夹</el-button><el-button type="primary" :loading="uploading" @click="input?.click()"><span class="material-symbols-outlined">upload</span>上传文件</el-button><input ref="input" type="file" multiple hidden @change="upload" /></div></div>
  <div class="office-toolbar"><el-breadcrumb separator="/"><el-breadcrumb-item><button class="office-link" @click="go(-1)">全部文件</button></el-breadcrumb-item><el-breadcrumb-item v-for="(c,i) in crumbs" :key="c.id"><button class="office-link" @click="go(i)">{{ c.name }}</button></el-breadcrumb-item></el-breadcrumb><el-input v-model="keyword" placeholder="搜索团队文件" clearable style="width:220px" @keyup.enter="load" @clear="load" /></div>
  <el-table :data="files" empty-text="暂无文件"><el-table-column label="名称" min-width="240"><template #default="{row}"><button class="office-link file-name" @click="open(row)"><span class="material-symbols-outlined" :style="{color:row.type==='folder'?'#d6a028':'#3370ff'}">{{ row.type==='folder'?'folder':'draft' }}</span>{{ row.fileName||row.name }}</button></template></el-table-column><el-table-column label="大小" width="120"><template #default="{row}">{{ row.type==='folder'?'—':size(row.fileSize) }}</template></el-table-column><el-table-column label="上传时间" min-width="160"><template #default="{row}">{{ String(row.createTime||'').replace('T',' ').slice(0,16) }}</template></el-table-column><el-table-column label="操作" width="150"><template #default="{row}"><div v-if="row.type!=='folder'" class="office-actions"><el-button circle title="下载" @click="download(row)"><span class="material-symbols-outlined">download</span></el-button><el-button circle title="移动" @click="beginMove(row)"><span class="material-symbols-outlined">drive_file_move</span></el-button><el-button circle title="删除" @click="remove(row)"><span class="material-symbols-outlined">delete</span></el-button></div></template></el-table-column></el-table>
  <el-dialog v-model="moveVisible" title="移动文件" width="420px"><el-breadcrumb><el-breadcrumb-item><button class="office-link" @click="moveRoot">根目录</button></el-breadcrumb-item><el-breadcrumb-item>{{ moveName }}</el-breadcrumb-item></el-breadcrumb><div class="folder-choices"><button v-for="f in moveFolders" :key="f.folderId" class="office-link file-name" @click="enterMove(f)"><span class="material-symbols-outlined">folder</span>{{ f.name }}</button><el-empty v-if="!moveFolders.length" description="没有子文件夹" :image-size="40" /></div><template #footer><el-button @click="moveVisible=false">取消</el-button><el-button type="primary" @click="confirmMove">移动到此处</el-button></template></el-dialog>
<el-button v-if="currentFolder()" @click="permissionOpen=true"><span class="material-symbols-outlined">admin_panel_settings</span>当前文件夹权限</el-button><FolderPermissionDialog v-model="permissionOpen" :folder-id="currentFolder()" />
</div></template>
<script setup lang="ts">
import {onMounted,ref} from 'vue'
import {useRouter} from 'vue-router'
import {ElMessage,ElMessageBox} from 'element-plus'
import {useWorkspaceStore} from '@/stores/workspace'
import request from '@/utils/request'
import {downloadFile} from '@/utils/download'
import FolderPermissionDialog from '@/views/org/FolderPermissionDialog.vue'
const permissionOpen=ref(false)
const workspace=useWorkspaceStore(),router=useRouter(),files=ref<any[]>([]),crumbs=ref<{id:string,name:string}[]>([]),keyword=ref(''),loading=ref(false),uploading=ref(false),input=ref<HTMLInputElement>()
const moveVisible=ref(false),moveFile=ref<any>(),moveId=ref(''),moveName=ref(''),moveFolders=ref<any[]>([])
function currentFolder(){return crumbs.value[crumbs.value.length-1]?.id}
async function load(){loading.value=true;try{const r:any=keyword.value.trim()?await request.get('/file/search',{params:{keyword:keyword.value.trim()}}):await request.get('/file/list',{params:{spaceId:workspace.spaceId,folderId:currentFolder()}});files.value=r.data}catch{}finally{loading.value=false}}
onMounted(load)
function go(index:number){crumbs.value=crumbs.value.slice(0,index+1);keyword.value='';load()}
function open(row:any){if(row.type==='folder'){crumbs.value.push({id:String(row.folderId),name:row.name});keyword.value='';load()}else router.push('/files/preview/'+row.fileId)}
function size(n:number){return !n?'0 B':n<1024?n+' B':n<1048576?(n/1024).toFixed(1)+' KB':(n/1048576).toFixed(1)+' MB'}
async function newFolder(){try{const {value}=await ElMessageBox.prompt('文件夹名称','新建文件夹',{inputValidator:v=>!!v?.trim()&&v.length<=100||'请输入1到100字'});await request.post('/file/folder',null,{params:{folderName:value.trim(),parentId:currentFolder()}});await load()}catch{}}
async function upload(event:Event){const selected=Array.from((event.target as HTMLInputElement).files||[]);uploading.value=true;try{for(const f of selected){const data=new FormData();data.append('file',f);if(currentFolder())data.append('folderId',currentFolder()!);await request.post('/file/upload',data,{timeout:120000})}if(selected.length)ElMessage.success('上传完成');await load()}catch{}finally{uploading.value=false;if(input.value)input.value.value=''}}
async function download(f:any){try{await downloadFile(f.fileId,f.fileName)}catch(e:any){if(!e.displayed)ElMessage.error(e.message)}}
async function remove(f:any){try{await ElMessageBox.confirm('删除“'+f.fileName+'”？','删除文件',{type:'warning'});await request.delete('/file/'+f.fileId);await load()}catch{}}
async function moveRoot(){moveId.value='';moveName.value='';await loadMove()}
async function loadMove(){const r:any=await request.get('/file/list',{params:{spaceId:workspace.spaceId,folderId:moveId.value||undefined}});moveFolders.value=r.data.filter((f:any)=>f.type==='folder')}
async function beginMove(f:any){moveFile.value=f;await moveRoot();moveVisible.value=true}
async function enterMove(f:any){moveId.value=String(f.folderId);moveName.value=f.name;await loadMove()}
async function confirmMove(){try{await request.put('/file/'+moveFile.value.fileId+'/move',null,{params:{targetFolderId:moveId.value||undefined}});moveVisible.value=false;await load();ElMessage.success('已移动')}catch{}}
</script>
<style scoped>.file-name{display:flex;align-items:center;gap:10px;overflow-wrap:anywhere}.material-symbols-outlined{font-size:20px}.folder-choices{display:grid;gap:14px;padding:24px 0;max-height:300px;overflow:auto}</style>
