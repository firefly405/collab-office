<template>
  <article class="comment-thread">
    <header><strong>{{ comment.userName }}</strong><span>{{ String(comment.createdTime||'').replace('T',' ').slice(0,16) }}</span><el-tag v-if="comment.isResolved" size="small" type="success">已解决</el-tag></header>
    <button v-if="anchor" class="anchor office-link" @click="$emit('locate',comment.selectionRange)">“{{ anchor.text }}”</button>
    <p>{{ comment.content }}</p>
    <div class="office-actions"><el-button v-if="canComment" text size="small" @click="$emit('reply',comment)">回复</el-button><el-button v-if="canEdit&&!comment.isResolved&&!nested" text size="small" @click="$emit('resolve',comment)">解决</el-button></div>
    <CommentThread v-for="child in comment.replies||[]" :key="child.commentId" :comment="child" :can-comment="canComment" :can-edit="canEdit" nested @reply="$emit('reply',$event)" @resolve="$emit('resolve',$event)" @locate="$emit('locate',$event)" />
  </article>
</template>
<script setup lang="ts">
import {computed} from 'vue'
const props=defineProps<{comment:any;canComment:boolean;canEdit:boolean;nested?:boolean}>()
defineEmits<{(e:'reply',comment:any):void;(e:'resolve',comment:any):void;(e:'locate',anchor:string):void}>()
const anchor=computed(()=>{try{return JSON.parse(props.comment.selectionRange)}catch{return null}})
</script>
<style scoped>
.comment-thread{padding:var(--space-3) 0;border-bottom:1px solid var(--c-border)}header{display:flex;align-items:center;flex-wrap:wrap;gap:var(--space-2);font-size:var(--text-small)}header span{color:var(--c-text-muted);font-size:11px}.comment-thread p{white-space:pre-wrap;overflow-wrap:anywhere;margin:var(--space-3) 0}.anchor{display:block;text-align:left;white-space:normal;overflow-wrap:anywhere;border-left:2px solid var(--c-brand);padding-left:var(--space-2);margin-top:var(--space-2)}.comment-thread .comment-thread{margin-left:var(--space-3);padding-left:var(--space-3);border-left:1px solid var(--c-border)}
</style>
