<template>
  <div class="editor-page">
    <header class="document-toolbar">
      <el-input v-model="title" :readonly="!canEdit" maxlength="100" aria-label="文档标题" @change="rename" class="document-title" />
      <span class="office-muted">{{ canEdit?saveStatus:'只读' }} · {{ wordCount }} 字</span>
      <span v-if="!shareToken" class="office-muted" :title="onlineUsers.map(u=>u.name).join('、')">{{ collabStatusText }} · {{ onlineUsers.length }} 人</span>
      <div class="office-actions" v-if="!shareToken">
        <el-button circle title="文档大纲" @click="showOutline=!showOutline"><span class="material-symbols-outlined">toc</span></el-button>
        <el-button circle title="评论" @click="showComments=!showComments"><span class="material-symbols-outlined">comment</span></el-button>
        <el-button circle title="版本记录" @click="showVersions=true"><span class="material-symbols-outlined">history</span></el-button>
        <el-button v-if="canEdit" circle title="分享" @click="showShare=true"><span class="material-symbols-outlined">share</span></el-button>
        <el-button circle title="导出下载" @click="showExport=true"><span class="material-symbols-outlined">download</span></el-button>
      </div>
    </header>
    <!-- Main Workspace -->
    <main class="workspace">
      <aside v-if="showOutline" class="document-outline"><div class="office-toolbar"><strong>大纲</strong><el-button text size="small" @click="downloadOutline">导出</el-button></div><el-checkbox v-model="outlineCollapsed">仅显示一级标题</el-checkbox><button v-for="h in headings.filter(h=>!outlineCollapsed||h.level===1)" :key="h.index" class="outline-item office-link" :style="{paddingLeft:(h.level-1)*12+'px'}" :draggable="canEdit" @dragstart="dragHeading=h.index" @dragover.prevent @drop.prevent="moveHeading(h.index)" @click="quillEditorRef?.locateHeading(h.index)">{{ h.text }}</button><p v-if="!headings.length" class="office-muted">暂无标题</p></aside>
      <!-- Editor Canvas -->
      <div class="editor-canvas-area">
        <!-- Editor Instance -->
        <div class="editor-wrapper">
          <QuillEditor
            v-if="editorReady"
            ref="quillEditorRef"
            :key="activeDocId + editorGeneration"
            :doc-id="activeDocId"
            :initial-content="initialContent"
            :read-only="!canEdit"
            :standalone="!!shareToken"
            @save="saveEditorContent"
            @word-count="wordCount = $event"
            @status-change="handleCollabStatusChange"
            @collaborators-change="onlineUsers = $event"
            @headings-change="headings=$event"
            @dirty="dirtyRevision++; saveStatus='未保存'"
          />
          <div v-else class="editor-loading">
            <p>正在加载文档...</p>
          </div>
        </div>
      </div>

      <!-- Comments Panel -->
      <aside class="comments-panel" v-if="showComments">
        <div class="comments-header">
          <h3 class="comments-title">评论</h3>
          <span class="material-symbols-outlined comments-close" @click="showComments = false">close</span>
        </div>
        <div class="comment-filter"><el-select v-model="commentFilter" aria-label="评论筛选" @change="loadComments"><el-option label="全部评论" value="all"/><el-option label="未解决" value="unresolved"/><el-option label="已解决" value="resolved"/></el-select><el-button text @click="exportComments">导出批注</el-button></div>
        <div class="comments-list">
          <CommentThread v-for="c in comments" :key="c.commentId" :comment="c" :can-comment="canComment" :can-edit="canEdit" @reply="replying=$event" @resolve="resolveComment" @locate="locateComment" />
          <div v-if="comments.length === 0" class="comments-empty">暂无评论</div>
        </div>
        <div class="comments-input-area" v-if="canComment">
          <div v-if="replying" class="office-toolbar"><span>回复 {{ replying.userName }}</span><el-button text size="small" @click="replying=null">取消</el-button></div>
          <div v-if="commentAnchor" class="office-toolbar"><span class="comment-quote">“{{ commentAnchor.text }}”</span><el-button text size="small" @click="commentAnchor=null">清除</el-button></div>
          <el-button v-else-if="!replying" text size="small" @click="captureSelection">引用选中文本</el-button>
          <el-select v-model="mentionIds" multiple filterable placeholder="@ 提及成员" aria-label="提及成员"><el-option v-for="m in commentMembers" :key="m.userId" :value="m.userId" :label="m.userName" /></el-select>
          <div class="comment-input-row">
            <input
              v-model="newCommentText"
              class="comment-input"
              placeholder="添加评论..."
              @keyup.enter="submitComment" maxlength="4000"
            />
            <button class="office-link" title="发送评论" :disabled="commentSending" @click="submitComment"><span class="material-symbols-outlined">send</span></button>
          </div>
        </div>
      </aside>
    </main>

    <!-- Version History Dialog -->
    <div class="modal-overlay" v-if="showVersions" @click.self="showVersions = false">
      <div class="modal-dialog">
        <div class="modal-header">
          <h3>版本历史</h3>
          <span class="material-symbols-outlined modal-close" @click="showVersions = false">close</span>
        </div>
        <div class="modal-body">
          <div v-if="canEdit" class="office-toolbar"><el-button type="primary" @click="snapshot">创建快照</el-button><label>保留版本 <el-input-number v-model="retention" :min="10" :max="1000" :step="10" aria-label="保留版本数" /></label><el-button @click="savePolicy">保存策略</el-button></div>
          <div class="version-comparison"><el-select v-model="compareFrom" placeholder="旧版本" aria-label="对比旧版本"><el-option v-for="v in versions" :key="v.version" :value="v.version" :label="`v${v.version} ${v.versionName}`"/></el-select><el-select v-model="compareTo" placeholder="新版本" aria-label="对比新版本"><el-option v-for="v in versions" :key="v.version" :value="v.version" :label="`v${v.version} ${v.versionName}`"/></el-select><el-button :disabled="!compareFrom||!compareTo" @click="showDiff=true">对比</el-button></div>
          <div v-for="v in versions" :key="v.version" class="version-item">
            <div class="version-info">
              <strong>v{{ v.version }} · {{ v.versionName }} <span v-if="v.isSnapshot">· 快照</span></strong>
              <span>{{ v.editorName }} · {{ v.createdTime }}</span>
              <span class="version-summary">{{ v.changeSummary }}</span>
            </div>
            <button v-if="canEdit" class="btn-sm" @click="rollback(v.version)">恢复此版本</button>
            <el-button v-if="canEdit" text size="small" @click="nameVersion(v)">命名与备注</el-button>
          </div>
        </div>
      </div>
    </div>

    <VersionDiffPanel v-model:visible="showDiff" :doc-id="activeDocId" :version-from="compareFrom" :version-to="compareTo" :can-edit="canEdit" @rollback="rollback" />

    <!-- Share Dialog -->
    <DocumentPermissionDialog v-model="permissionOpen" :doc-id="activeDocId" />
    <div class="modal-overlay" v-if="showShare" @click.self="showShare = false">
      <div class="modal-dialog modal-sm">
        <div class="modal-header">
          <h3>分享文档</h3>
          <span class="material-symbols-outlined modal-close" @click="showShare = false">close</span>
        </div>
        <div class="modal-body">
          <el-button @click="showShare=false;permissionOpen=true">文档成员权限</el-button>
          <div class="form-group">
            <label>权限</label>
            <select v-model="sharePerm" class="native-select">
              <option :value="1">查看</option>
            </select>
          </div>
          <div class="form-group">
            <label>密码（可选）</label>
            <input v-model="sharePwd" class="native-input" type="password" maxlength="20" placeholder="可选，最多20字" />
          </div>
          <button class="btn-primary btn-block" @click="createShare">生成链接</button>
          <div class="form-group"><label>有效期</label><select v-model="shareDays" class="native-select"><option :value="1">1天</option><option :value="7">7天</option><option :value="30">30天</option></select></div>
          <div v-if="shareLink" class="share-result">
            <input :value="shareLink" class="native-input" readonly />
            <button class="btn-sm btn-primary" @click="copyLink" style="margin-top:6px">复制链接</button>
          </div>
          <h4>分享记录</h4><div v-for="s in shareRecords" :key="s.id" class="share-record"><span>{{ s.status===1?'有效':'已撤销' }} · {{ s.viewCount }} 次访问 · {{ s.expireTime?String(s.expireTime).replace('T',' '):'永久' }}</span><el-button v-if="s.status===1" type="danger" text @click="revokeShare(s.id)">撤销</el-button></div><p v-if="!shareRecords.length" class="office-muted">暂无分享记录</p>
        </div>
      </div>
    </div>

    <!-- Export Dialog -->
    <div class="modal-overlay" v-if="showExport" @click.self="showExport = false">
      <div class="modal-dialog modal-sm">
        <div class="modal-header">
          <h3>导出</h3>
          <span class="material-symbols-outlined modal-close" @click="showExport = false">close</span>
        </div>
        <div class="modal-body">
          <div class="form-group">
            <label>格式</label>
            <select v-model="exportFormat" class="native-select">
              <option value="html">HTML</option>
              <option value="md">Markdown</option>
              <option value="txt">TXT</option>
              <option value="docx">Word (.docx)</option>
              <option value="pdf">PDF</option>
            </select>
          </div>
          <div class="office-form"><el-checkbox v-model="exportCommentsIncluded">包含评论</el-checkbox><el-checkbox v-model="exportVersionsIncluded">包含最近100个历史版本</el-checkbox></div>
          <button class="btn-primary btn-block" @click="doExport">导出</button>
        </div>
      </div>
    </div>

    <!-- Collaborative Cursor Layer -->
    <div class="collab-layer"></div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import QuillEditor from '@/components/editor/QuillEditor.vue'
import CommentThread from './components/CommentThread.vue'
import VersionDiffPanel from '@/components/doc/VersionDiffPanel.vue'
import { getDocDetail, getDocContent, saveDocContent, getVersions, getComments } from '@/api/doc'
import request from '@/utils/request'
import DocumentPermissionDialog from '@/views/org/DocumentPermissionDialog.vue'
const permissionOpen=ref(false)

type CollabStatus = 'offline' | 'connecting' | 'online'

type OnlineUser = {
  id: string
  name: string
  color: string
  lastSeen: number
}

type StatusPayload = {
  state: CollabStatus
  text: string
}

const route = useRoute()
const routeDocId = route.params.docId as string | undefined
const shareToken = route.params.token as string | undefined
const activeDocId = ref(routeDocId || '')
const quillEditorRef = ref<InstanceType<typeof QuillEditor>>()
const title = ref('')
const initialContent = ref('')
const editorReady = ref(false)
const canEdit = ref(false), editorGeneration = ref(0), sharePassword = ref('')
const canComment=ref(false),commentSending=ref(false),commentFilter=ref('all'),replying=ref<any>(null),commentAnchor=ref<any>(null),mentionIds=ref<any[]>([]),commentMembers=ref<any[]>([])
const showDiff=ref(false),compareFrom=ref(0),compareTo=ref(0),retention=ref(100)
const showOutline=ref(false),outlineCollapsed=ref(false),headings=ref<{index:number;level:number;text:string}[]>([]),dragHeading=ref<number|null>(null)
const saveStatus = ref('已保存')
const dirtyRevision=ref(0)
let saveQueue:Promise<boolean>=Promise.resolve(true),saveSequence=0
const version = ref(0)
const wordCount = ref(0)
const onlineUsers = ref<OnlineUser[]>([])
const collabStatus = ref<CollabStatus>('offline')
const collabStatusText = ref('协同离线')

const showComments = ref(false)
const showVersions = ref(false)
const showShare = ref(false)
const showExport = ref(false)
const newCommentText = ref('')
const comments = ref<any[]>([])
const versions = ref<any[]>([])
const sharePerm = ref(1)
const sharePwd = ref('')
const shareDays=ref(7),shareRecords=ref<any[]>([])
watch(showShare,open=>{if(open)loadShares()})
async function loadShares(){try{const r:any=await request.get(`/document/${activeDocId.value}/shares`);shareRecords.value=r.data}catch{}}
async function revokeShare(id:string){try{await request.delete(`/document/${activeDocId.value}/shares/${id}`);await loadShares();ElMessage.success('分享已撤销')}catch{}}
const shareLink = ref('')
const exportFormat = ref('html')
const exportCommentsIncluded=ref(false),exportVersionsIncluded=ref(false)

watch(showVersions, (val) => { if (val) loadVersions() })
watch(showComments, (val) => { if (val) loadComments() })

onMounted(() => {
  loadDoc()
})

async function submitComment() {
  if (!newCommentText.value.trim()||commentSending.value) return
  commentSending.value=true
  try {
    const mentions=commentMembers.value.filter(m=>mentionIds.value.includes(m.userId)).map(m=>'@'+m.userName).join(' ')
    await request.post(`/document/${activeDocId.value}/comments`,{docId:activeDocId.value,content:(mentions?mentions+' ':'')+newCommentText.value.trim(),selectionRange:!replying.value&&commentAnchor.value?JSON.stringify(commentAnchor.value):undefined,parentId:replying.value?.commentId,mentionUsers:mentionIds.value})
    newCommentText.value='';replying.value=null;commentAnchor.value=null;mentionIds.value=[];await loadComments();ElMessage.success('评论已发表')
  }catch{}finally{commentSending.value=false}
}
function captureSelection(){commentAnchor.value=quillEditorRef.value?.getCommentAnchor()||null;if(!commentAnchor.value)ElMessage.info('请先选中文档中的文字')}
function locateComment(anchor:string){try{if(!quillEditorRef.value?.locateAnchor(JSON.parse(anchor)))ElMessage.warning('原文已修改，无法唯一定位这段批注')}catch{ElMessage.warning('批注选区无效')}}
function download(name:string,body:string,type='text/plain'){const url=URL.createObjectURL(new Blob([body],{type})),a=document.createElement('a');a.href=url;a.download=name;a.click();setTimeout(()=>URL.revokeObjectURL(url),10000)}
async function exportComments(){try{const r:any=await getComments(activeDocId.value,'all');download(title.value+'-批注.json',JSON.stringify(r.data,null,2),'application/json')}catch{}}
function downloadOutline(){download(title.value+'-目录.md',headings.value.map(h=>'  '.repeat(h.level-1)+'- '+h.text).join('\n'))}
function moveHeading(index:number){if(dragHeading.value!==null&&canEdit.value)quillEditorRef.value?.moveSection(dragHeading.value,index);dragHeading.value=null}
async function snapshot(){try{const {value}=await ElMessageBox.prompt('版本名称','创建快照',{inputPattern:/^.{1,100}$/,inputErrorMessage:'请输入1至100字'});const content=quillEditorRef.value?.flushContent();if(content!==undefined&&!await saveEditorContent(content)){ElMessage.error('正文保存失败，未创建快照');return}await request.post(`/document/${activeDocId.value}/versions/snapshot`,null,{params:{name:value}});await loadVersions();ElMessage.success('快照已创建')}catch{}}
async function nameVersion(v:any){try{const name=await ElMessageBox.prompt('版本名称','命名版本',{inputValue:v.versionName,inputPattern:/^.{1,100}$/,inputErrorMessage:'请输入1至100字'});const remark=await ElMessageBox.prompt('版本备注（可留空）','版本备注',{inputValue:v.changeSummary||'',inputType:'textarea'});await request.put(`/document/${activeDocId.value}/versions/${v.version}/metadata`,{name:name.value,remark:remark.value||''});await loadVersions()}catch{}}
async function savePolicy(){try{await request.put(`/document/${activeDocId.value}/versions/policy`,{limit:retention.value});await loadVersions();ElMessage.success('保留策略已更新')}catch{}}

async function loadDoc() {
  editorReady.value = false
  saveStatus.value = '加载中'
  const currentDocId = activeDocId.value

  try {
    const detailRes: any = shareToken
      ? await request.get(`/document/share/${shareToken}`, {params:{password:sharePassword.value},...({_silent:true} as any)})
      : await getDocDetail(currentDocId)
    if (detailRes.code === 200) {
      const detail = detailRes.data
      activeDocId.value = detail.docId || currentDocId
      canEdit.value = !shareToken && detail.canEdit === true
      canComment.value = !shareToken && detail.canComment === true
      title.value = detail.title
      wordCount.value = detail.wordCount || 0
    }
  } catch (e:any) {
    title.value = title.value || '文档加载失败'
    if(shareToken && e.message?.includes('密码')) {
      try { const {value}=await ElMessageBox.prompt('请输入分享密码','查看文档',{inputType:'password'}); sharePassword.value=value; await loadDoc() } catch {}
    } else ElMessage.error(e.message||'文档加载失败')
    return
  }

  try {
    const contentRes: any = shareToken
      ? await request.get(`/document/share/${shareToken}/content`, { params:{password:sharePassword.value} })
      : await getDocContent(activeDocId.value)
    if (contentRes.code === 200) {
      initialContent.value = normalizeInitialContent(contentRes.data.content)
      version.value = contentRes.data.version || 0
      saveStatus.value = '已保存'
    } else {
      saveStatus.value = '加载失败'
    }
  } catch {
    initialContent.value = ''
    saveStatus.value = '加载失败'
  }
  if(saveStatus.value==='已保存')editorReady.value = true
}

function saveEditorContent(content: string) {
  if (!canEdit.value) return Promise.resolve(false)
  const sequence=++saveSequence,revision=dirtyRevision.value,docId=activeDocId.value
  saveStatus.value = '正在保存'
  saveQueue=saveQueue.catch(()=>false).then(async()=>{try{await saveDocContent(docId,content);if(sequence===saveSequence&&revision===dirtyRevision.value)saveStatus.value='已保存';return true}catch{if(sequence===saveSequence)saveStatus.value='保存失败';return false}})
  return saveQueue
}

function handleCollabStatusChange(payload: StatusPayload) {
  collabStatus.value = payload.state
  collabStatusText.value = payload.text
}


function resolveComment(c: any) {
  request.put(`/document/${activeDocId.value}/comments/${c.commentId || c.id}/resolve`)
    .then(() => {
      ElMessage.success('已标记为已解决')
      loadComments()
    }).catch(() => ElMessage.error('操作失败'))
}

async function rollback(v: number) {
  try {
    await ElMessageBox.confirm('将当前内容恢复为该版本？现有内容仍保留在版本记录中。','恢复版本')
    const r:any=await request.get(`/document/${activeDocId.value}/versions/${v}/content`)
    quillEditorRef.value?.restoreContent(r.data)
    showVersions.value=false
    showDiff.value=false
    ElMessage.success('已恢复，正在保存')
  } catch {}
}

function createShare() {
  request.post(`/document/${activeDocId.value}/share`, { permission: sharePerm.value, password: sharePwd.value || undefined, expireDays: shareDays.value })
    .then((res: any) => {
      if (res.code === 200 && res.data) {
        shareLink.value = window.location.origin + '/#/share/' + res.data.shareToken
        ElMessage.success('分享链接已生成')
        loadShares()
      }
    }).catch(() => ElMessage.error('生成分享链接失败'))
}

function copyLink() {
  navigator.clipboard.writeText(shareLink.value)
  ElMessage.success('已复制')
}

async function doExport() {
  try {
    if(canEdit.value&&saveStatus.value!=='已保存'){const content=quillEditorRef.value?.flushContent();if(content!==undefined&&!await saveEditorContent(content)){ElMessage.error('正文保存失败，未导出');return}}
    const r:any=await request.post(`/document/${activeDocId.value}/export`,{format:exportFormat.value,includeComments:exportCommentsIncluded.value,includeVersions:exportVersionsIncluded.value},{timeout:60000})
    let task=r.data
    for(let i=0;i<30 && task.status==='PROCESSING';i++) {
      await new Promise(resolve=>setTimeout(resolve,1000))
      const status:any=await request.get(`/document/export/tasks/${task.taskId}`); task=status.data
    }
    if(task.status!=='COMPLETED') {ElMessage.error(task.errorMsg||'导出尚未完成，请重试');return}
    const response=await fetch(task.fileUrl)
    if(!response.ok) throw new Error('下载失败')
    const url=URL.createObjectURL(await response.blob()), a=document.createElement('a');a.href=url;a.download=title.value+'.'+exportFormat.value;a.click();setTimeout(()=>URL.revokeObjectURL(url),10000)
    showExport.value=false
  }catch(e:any){if(!e.displayed)ElMessage.error(e.message||'导出失败')}
}

async function rename(){if(!canEdit.value)return;try{await request.put(`/document/${activeDocId.value}`,{title:title.value})}catch{}}

async function loadVersions() {
  if (shareToken) return
  try{const results:any[]=await Promise.all([getVersions(activeDocId.value,1,100),request.get(`/document/${activeDocId.value}/versions/policy`)]);versions.value=results[0].data||[];retention.value=results[1].data.limit;if(!versions.value.some(v=>v.version===compareTo.value))compareTo.value=versions.value[0]?.version||0;if(!versions.value.some(v=>v.version===compareFrom.value))compareFrom.value=versions.value[1]?.version||compareTo.value}catch{}
}

async function loadComments() {
  if (shareToken) return
  try{const results:any[]=await Promise.all([getComments(activeDocId.value,commentFilter.value),request.get(`/document/${activeDocId.value}/comment-members`)]);comments.value=results[0].data||[];commentMembers.value=results[1].data||[]}catch{}
}

function normalizeInitialContent(content?: string) {
  if (!content || content === '{}') return ''
  return content
}
</script>

<style lang="scss" scoped>
.comment-filter{display:flex;gap:var(--space-2);padding:0 var(--space-4) var(--space-3)}.comment-filter .el-select{min-width:0}.comment-quote{overflow-wrap:anywhere;min-width:0}.comments-input-area .el-select{width:100%;margin-block:var(--space-2)}.version-comparison{display:flex;gap:var(--space-2);margin-block:var(--space-4)}.version-comparison .el-select{min-width:0;flex:1}.document-outline{width:220px;flex-shrink:0;padding:var(--space-4);overflow:auto;border-right:1px solid var(--c-border);background:var(--c-surface)}.outline-item{display:block;width:100%;white-space:normal;text-align:left;overflow-wrap:anywhere;margin-block:var(--space-3)}
@media(max-width:700px){.comments-panel,.document-outline{position:absolute;right:0;top:0;bottom:0;width:min(350px,100%);z-index:20;box-shadow:var(--shadow-md)}.workspace{position:relative}.version-item{flex-wrap:wrap;gap:var(--space-2)}.version-comparison{flex-wrap:wrap}.version-comparison .el-select{flex-basis:120px}.version-info{min-width:0;overflow-wrap:anywhere}}
.document-toolbar {display:flex;align-items:center;gap:12px;flex-wrap:wrap;padding:12px;border-bottom:1px solid #e3e6eb;background:white;flex-shrink:0;}
.document-title {width:260px;max-width:100%;}.document-toolbar .office-actions{margin-left:auto;}
.modal-dialog{max-width:calc(100vw - 32px);border-radius:8px!important;}.comment-card{border-radius:6px!important;}
// ---------- CSS Variables ----------
$c-surface: #f8f9fa;
$c-surface-container-lowest: #ffffff;
$c-surface-container-low: #f3f4f5;
$c-surface-container: #edeeef;
$c-surface-container-high: #e7e8e9;
$c-surface-container-highest: #e1e3e4;
$c-surface-dim: #d9dadb;
$c-border-subtle: #E5E6EB;
$c-outline-variant: #c3c6d8;
$c-outline: #737687;
$c-primary: #0050d6;
$c-primary-container: #2a6af9;
$c-primary-fixed: #dbe1ff;
$c-selection-blue: rgba(51, 112, 255, 0.1);
$c-on-surface: #191c1d;
$c-on-surface-variant: #424655;
$c-secondary: #5b5e65;
$c-on-primary: #ffffff;
$c-success: #22C55E;
$c-error: #ba1a1a;
$c-text: #191c1d;
$c-text-secondary: #424655;
$c-text-muted: #737687;
$c-bg: #f8f9fa;
$c-border: #E5E6EB;
$c-border-light: #e8e9ec;
$c-brand: #0050d6;
$c-brand-light: rgba(51, 112, 255, 0.06);
$c-success-light: rgba(34, 197, 94, 0.1);
$c-warning: #F59E0B;
$c-warning-light: rgba(245, 158, 11, 0.1);
$c-danger: #EF4444;
$c-danger-light: rgba(239, 68, 68, 0.1);
$c-online: #22C55E;
$radius: 4px;
$radius-lg: 8px;
$radius-xl: 12px;
$radius-full: 12px;
$gutter: 16px;
$ease-out: cubic-bezier(0.16, 1, 0.3, 1);

// ---------- Layout ----------
.editor-page {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: var(--c-bg);
  overflow: hidden;
  font-family: 'Inter Variable', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
  color: var(--c-text);
}

// ---------- Workspace ----------
.workspace {
  display: flex;
  flex: 1;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
}

// ---------- Editor Canvas Area ----------
.editor-canvas-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: $c-surface-container-low;
  overflow-y: auto;
  align-items: center;
  padding: 0;
  min-width: 0;
}

// ---------- Editor Wrapper ----------
.editor-wrapper {
  width: 100%;
  background: $c-surface-container-lowest;
  min-height: calc(100vh - 60px);
}

.editor-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 300px;
  color: $c-text-muted;
  font-size: 15px;
}

// ---------- Comments Panel ----------
.comments-panel {
  width: 320px;
  display: flex;
  flex-direction: column;
  background: $c-surface-container-low;
  border-left: 1px solid $c-border-subtle;
  overflow-y: auto;
  flex-shrink: 0;
}

.comments-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
}

.comments-title {
  font-size: 12px;
  font-weight: 600;
  color: $c-on-surface;
}

.comments-close {
  font-size: 20px;
  color: $c-secondary;
  cursor: pointer;
  &:hover { color: $c-on-surface; }
}

.comments-list {
  flex: 1;
  padding: 0 16px 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  overflow-y: auto;
}

.comment-card {
  background: $c-surface-container-lowest;
  padding: 16px;
  border-radius: $radius-xl;
  border: 1px solid $c-border-subtle;
  box-shadow: 0 1px 2px rgba(0,0,0,0.03);
}

.comment-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.comment-user {
  display: flex;
  align-items: center;
  gap: 8px;
}

.comment-avatar {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: $c-primary-fixed;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 10px;
  font-weight: 700;
  color: $c-on-surface-variant;
}

.comment-name {
  font-size: 12px;
  font-weight: 700;
  color: $c-on-surface;
}

.comment-time {
  font-size: 10px;
  color: $c-secondary;
}

.comment-body {
  font-size: 14px;
  color: $c-on-surface-variant;
  line-height: 1.6;
}

.comment-actions {
  display: flex;
  gap: 12px;
  margin-top: 8px;
}

.comment-act-icon {
  font-size: 16px;
  color: $c-secondary;
  cursor: pointer;
  &:hover { color: $c-primary; }
}

.comments-empty {
  text-align: center;
  color: $c-text-muted;
  font-size: 13px;
  padding: 24px;
}

.comments-input-area {
  padding: 16px;
  border-top: 1px solid $c-border-subtle;
  margin-top: auto;
}

.comment-input-row {
  position: relative;
}

.comment-input {
  width: 100%;
  background: $c-surface-container-lowest;
  border: 1px solid $c-border-subtle;
  border-radius: 24px;
  padding: 8px 40px 8px 16px;
  font-size: 14px;
  outline: none;
  transition: border-color .15s;
  box-sizing: border-box;
  &:focus { border-color: $c-primary; box-shadow: 0 0 0 2px rgba(0, 80, 214, 0.1); }
}

.comment-send {
  position: absolute;
  right: 8px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 20px;
  color: $c-primary;
  cursor: pointer;
}

// ---------- Modal Dialogs ----------
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
}

.modal-dialog {
  background: $c-surface-container-lowest;
  border-radius: $radius-xl;
  width: 600px;
  max-height: 80vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 8px 32px rgba(0,0,0,0.12);
  &.modal-sm { width: 380px; }
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 24px;
  border-bottom: 1px solid $c-border-subtle;
  h3 { font-size: 16px; font-weight: 600; color: $c-on-surface; margin: 0; }
}

.modal-close {
  font-size: 22px;
  color: $c-outline;
  cursor: pointer;
  &:hover { color: $c-on-surface; }
}

.modal-body {
  padding: 24px;
  overflow-y: auto;
}

// ---------- Version Items ----------
.version-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid $c-border-light;
  font-size: 13px;
  color: $c-text-secondary;
}

.version-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.version-summary {
  color: $c-text-muted;
  font-size: 12px;
}

// ---------- Form Elements ----------
.form-group {
  margin-bottom: 16px;
  label {
    display: block;
    font-size: 13px;
    font-weight: 500;
    color: $c-on-surface-variant;
    margin-bottom: 6px;
  }
}

.native-input {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid $c-border-subtle;
  border-radius: $radius;
  font-size: 14px;
  background: $c-surface;
  color: $c-on-surface;
  outline: none;
  transition: border-color .15s;
  box-sizing: border-box;
  &:focus { border-color: $c-primary; box-shadow: 0 0 0 2px rgba(0, 80, 214, 0.1); }
}

.native-select {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid $c-border-subtle;
  border-radius: $radius;
  font-size: 14px;
  background: $c-surface;
  color: $c-on-surface;
  outline: none;
  box-sizing: border-box;
  &:focus { border-color: $c-primary; }
}

.btn-primary {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 8px 16px;
  background: $c-primary;
  color: $c-on-primary;
  border: none;
  border-radius: $radius;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: opacity .15s;
  &:hover { opacity: 0.9; }
  &.btn-block { width: 100%; }
}

.btn-sm {
  padding: 4px 12px;
  font-size: 12px;
  background: transparent;
  border: 1px solid $c-border-subtle;
  border-radius: $radius;
  color: $c-on-surface-variant;
  cursor: pointer;
  transition: all .15s;
  &:hover { background: $c-surface-container-low; }
  &.btn-primary {
    background: $c-primary;
    color: $c-on-primary;
    border-color: $c-primary;
  }
}

.share-result {
  margin-top: 12px;
}

// ---------- Collaborative Layer ----------
.collab-layer {
  pointer-events: none;
  position: fixed;
  inset: 0;
  z-index: 40;
  overflow: hidden;
}

// ---------- Scrollbar ----------
::-webkit-scrollbar { width: 6px; }
::-webkit-scrollbar-track { background: transparent; }
::-webkit-scrollbar-thumb { background: $c-border-subtle; border-radius: 10px; }
</style>
