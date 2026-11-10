<template>
  <div class="office-page">
    <div class="office-toolbar">
      <div class="office-actions"><el-button circle title="返回云盘" @click="router.push('/files')"><span class="material-symbols-outlined">arrow_back</span></el-button><h2>{{ file.fileName||'文件详情' }}</h2></div>
      <el-button :disabled="loading||!!error" :loading="downloading" @click="download"><span class="material-symbols-outlined">download</span>下载</el-button>
    </div>
    <div v-if="loading" class="office-empty">正在读取文件…</div>
    <el-result v-else-if="error" icon="info" title="无法读取文件" :sub-title="error"><template #extra><el-button @click="load">重试</el-button></template></el-result>
    <img v-else-if="imageUrl" :src="imageUrl" class="preview-image" :alt="file.fileName" @error="imageUrl=''" />
    <div v-else class="file-download">
      <span class="material-symbols-outlined file-symbol" aria-hidden="true">description</span>
      <h3>{{ file.fileName }}</h3><p>{{ fileType }} · {{ fileSize }}</p>
      <el-button type="primary" :loading="downloading" @click="download"><span class="material-symbols-outlined">download</span>下载文件</el-button>
    </div>
  </div>
</template>
<script setup lang="ts">
import {computed,onMounted,ref} from 'vue'
import {useRoute,useRouter} from 'vue-router'
import {ElMessage} from 'element-plus'
import request from '@/utils/request'
import {downloadFile} from '@/utils/download'
const route=useRoute(),router=useRouter(),id=String(route.params.fileId),file=ref<any>({}),imageUrl=ref(''),loading=ref(true),error=ref(''),downloading=ref(false)
const fileType=computed(()=>String(file.value.fileName||'').split('.').pop()?.toUpperCase()||'文件')
const fileSize=computed(()=>{const size=Number(file.value.fileSize||0);return size<1024?size+' B':size<1048576?(size/1024).toFixed(1)+' KB':(size/1048576).toFixed(1)+' MB'})
async function load(){loading.value=true;error.value='';imageUrl.value='';try{const r:any=await request.get('/file/'+id);file.value=r.data;if(/\.(png|jpe?g|gif|webp)$/i.test(file.value.fileName||'')){try{const original:any=await request.get('/file/download/'+id);imageUrl.value=original.data.downloadUrl}catch{}}}catch(e:any){error.value=e.message}finally{loading.value=false}}
async function download(){downloading.value=true;try{await downloadFile(id,file.value.fileName||'download')}catch(e:any){if(!e.displayed)ElMessage.error(e.message)}finally{downloading.value=false}}
onMounted(load)
</script>
<style scoped>
.file-download{display:flex;flex-direction:column;align-items:center;text-align:center;gap:16px;padding:64px 16px;border-top:1px solid var(--c-border)}
.file-symbol{font-size:52px;color:var(--c-brand)}
.file-download h3{max-width:100%;font-size:18px;overflow-wrap:anywhere;margin:0}
.file-download p{font-size:13px;color:var(--c-text-muted);margin:0}
.preview-image{display:block;max-width:100%;max-height:calc(100vh - 190px);object-fit:contain;margin:auto}
h2{overflow-wrap:anywhere;font-size:20px;min-width:0}.office-toolbar>.office-actions{min-width:0}
</style>
