<template>
  <el-dialog :model-value="visible" title="版本对比" width="min(900px,94vw)" @update:model-value="$emit('update:visible',$event)">
    <div class="office-toolbar"><span>v{{ versionFrom }} → v{{ versionTo }}</span><el-button v-if="canEdit" @click="$emit('rollback',versionFrom)">恢复 v{{ versionFrom }}</el-button></div>
    <div v-loading="loading" class="version-diff"><p v-if="!loading&&!segments.length" class="office-muted">两个版本的正文相同</p><div v-for="(s,i) in segments" :key="i" :class="s.type"><span aria-hidden="true">{{ s.type==='insert'?'+':s.type==='delete'?'−':' ' }}</span><pre>{{ s.text }}</pre></div></div>
  </el-dialog>
</template>
<script setup lang="ts">
import {ref,watch} from 'vue'
import request from '@/utils/request'
const props=defineProps<{visible:boolean;docId:string;versionFrom:number;versionTo:number;canEdit:boolean}>()
defineEmits<{(e:'update:visible',v:boolean):void;(e:'rollback',version:number):void}>()
const segments=ref<{type:string;text:string}[]>([]),loading=ref(false)
watch(()=>[props.visible,props.versionFrom,props.versionTo],async()=>{if(!props.visible)return;loading.value=true;segments.value=[];try{const r:any=await request.get(`/document/${props.docId}/versions/compare`,{params:{v1:props.versionFrom,v2:props.versionTo}});segments.value=r.data.diffSegments||[]}catch{}finally{loading.value=false}},{immediate:true})
</script>
<style scoped>
.version-diff{max-height:65vh;overflow:auto;margin-top:var(--space-4)}.version-diff>div{display:flex;gap:var(--space-2);padding:var(--space-2);border-bottom:1px solid var(--c-border)}pre{white-space:pre-wrap;overflow-wrap:anywhere;margin:0;min-width:0;font:inherit}.insert{background:var(--c-success-light);color:var(--c-success)}.delete{background:var(--c-danger-light);color:var(--c-danger);text-decoration:line-through}
</style>
