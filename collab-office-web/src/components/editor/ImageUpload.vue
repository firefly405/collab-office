<template>
  <div
    class="image-upload-zone"
    :class="{ dragging, uploading }"
    @dragover.prevent="handleDragOver"
    @dragleave="handleDragLeave"
    @drop.prevent="handleDrop"
    @paste.capture="handlePaste"
  >
    <slot />
    <input
      ref="fileInputRef"
      class="file-input"
      type="file"
      accept="image/*"
      @change="handleFileChange"
    />
    <div v-if="dragging" class="upload-overlay">
      释放图片插入文档
    </div>
    <div v-if="uploading" class="upload-progress">
      图片处理中...
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'

type UploadedImage = {
  url: string
  file: File
  fileId?: string
  fileName: string
  fileSize: number
}

const props = withDefaults(defineProps<{
  maxSizeMb?: number
}>(), {
  maxSizeMb: 5,
})

const emit = defineEmits<{
  (e: 'uploaded', image: UploadedImage): void
  (e: 'uploading-change', uploading: boolean): void
  (e: 'error', message: string): void
}>()

const fileInputRef = ref<HTMLInputElement>()
const dragging = ref(false)
const uploading = ref(false)

function selectImage() {
  fileInputRef.value?.click()
}

async function uploadImage(file: File) {
  if (!validateImage(file)) return
  setUploading(true)
  try {
    const url = await readAsDataUrl(file)
    emit('uploaded', {
      url,
      file,
      fileName: file.name,
      fileSize: file.size,
    })
  } catch {
    reportError('图片读取失败')
  } finally {
    setUploading(false)
  }
}

function handleFileChange(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (file) uploadImage(file)
  input.value = ''
}

function handleDragOver(event: DragEvent) {
  if (hasImageFile(event.dataTransfer?.items)) {
    dragging.value = true
  }
}

function handleDragLeave(event: DragEvent) {
  const current = event.currentTarget as HTMLElement
  const related = event.relatedTarget as Node | null
  if (!related || !current.contains(related)) {
    dragging.value = false
  }
}

function handleDrop(event: DragEvent) {
  dragging.value = false
  const file = getImageFile(event.dataTransfer?.files)
  if (file) uploadImage(file)
}

function handlePaste(event: ClipboardEvent) {
  const file = getImageFile(event.clipboardData?.files)
  if (!file) return
  event.preventDefault()
  uploadImage(file)
}

function validateImage(file: File) {
  if (!file.type.startsWith('image/')) {
    reportError('请选择图片文件')
    return false
  }
  if (file.size > props.maxSizeMb * 1024 * 1024) {
    reportError(`图片不能超过 ${props.maxSizeMb}MB`)
    return false
  }
  return true
}

function getImageFile(files?: FileList | null) {
  if (!files) return null
  return Array.from(files).find((file) => file.type.startsWith('image/')) || null
}

function hasImageFile(items?: DataTransferItemList | null) {
  if (!items) return false
  return Array.from(items).some((item) => item.kind === 'file' && item.type.startsWith('image/'))
}

function readAsDataUrl(file: File) {
  return new Promise<string>((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(String(reader.result || ''))
    reader.onerror = () => reject(reader.error)
    reader.readAsDataURL(file)
  })
}

function setUploading(value: boolean) {
  uploading.value = value
  emit('uploading-change', value)
}

function reportError(message: string) {
  ElMessage.error(message)
  emit('error', message)
}

defineExpose({
  selectImage,
  uploadImage,
})
</script>

<style scoped lang="scss">
.image-upload-zone {
  position: relative;
  display: flex;
  flex: 1;
  min-height: 0;
  flex-direction: column;
}

.file-input {
  display: none;
}

.upload-overlay {
  position: absolute;
  inset: 12px;
  z-index: 5;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px dashed #409eff;
  border-radius: 8px;
  color: #1677d2;
  background: rgba(236, 245, 255, 0.86);
  font-size: 15px;
  font-weight: 600;
  pointer-events: none;
}

.upload-progress {
  position: absolute;
  right: 16px;
  bottom: 16px;
  z-index: 6;
  padding: 6px 12px;
  border-radius: 6px;
  color: #606266;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.12);
  font-size: 13px;
}
</style>
