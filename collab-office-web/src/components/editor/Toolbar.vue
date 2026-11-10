<template>
  <div :id="toolbarId" class="editor-toolbar" role="toolbar" aria-label="文档格式工具栏">
    <span class="ql-formats toolbar-group">
      <select class="ql-header" title="标题">
        <option value="1">标题1</option>
        <option value="2">标题2</option>
        <option value="3">标题3</option>
        <option value="4">标题4</option>
        <option value="5">标题5</option>
        <option value="6">标题6</option>
        <option selected>正文</option>
      </select>
      <select class="ql-font" title="字体">
        <option selected></option>
        <option value="serif">衬线</option>
        <option value="monospace">等宽</option>
      </select>
      <select class="ql-size" title="字号">
        <option value="small">小</option>
        <option selected>中</option>
        <option value="large">大</option>
        <option value="huge">特大</option>
      </select>
    </span>

    <span class="ql-formats toolbar-group">
      <button type="button" class="ql-bold" title="加粗"></button>
      <button type="button" class="ql-italic" title="斜体"></button>
      <button type="button" class="ql-underline" title="下划线"></button>
      <button type="button" class="ql-strike" title="删除线"></button>
    </span>

    <span class="ql-formats toolbar-group">
      <select class="ql-color" title="文字颜色"></select>
      <select class="ql-background" title="背景颜色"></select>
    </span>

    <span class="ql-formats toolbar-group">
      <button type="button" class="ql-list" value="ordered" title="有序列表"></button>
      <button type="button" class="ql-list" value="bullet" title="无序列表"></button>
      <button type="button" class="ql-list" value="check" title="任务列表"></button>
      <button type="button" class="ql-indent" value="-1" title="减少缩进"></button>
      <button type="button" class="ql-indent" value="+1" title="增加缩进"></button>
      <select class="ql-align" title="对齐"></select>
    </span>

    <span class="ql-formats toolbar-group">
      <button type="button" class="ql-blockquote" title="引用块"></button>
      <button type="button" class="ql-code-block" title="代码块"></button>
      <button type="button" class="ql-divider custom-button" title="分割线">
        <span aria-hidden="true">—</span>
      </button>
    </span>

    <span class="ql-formats toolbar-group">
      <button type="button" class="ql-link" title="链接"></button>
      <button type="button" class="ql-image" title="图片"></button>
      <button type="button" class="ql-clean" title="清除格式"></button>
    </span>

    <span class="ql-formats toolbar-group">
      <button type="button" class="ql-undo custom-button" title="撤销">
        <span aria-hidden="true">↶</span>
      </button>
      <button type="button" class="ql-redo custom-button" title="重做">
        <span aria-hidden="true">↷</span>
      </button>
    </span>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  toolbarId: string
}>()
</script>

<style scoped lang="scss">
.editor-toolbar {
  display: flex;
  min-height: 42px;
  align-items: center;
  gap: 0;
  padding: 4px 10px;
  overflow-x: auto;
  border: 0;
  border-bottom: 1px solid var(--c-border);
  background: var(--c-surface-dim);
  white-space: nowrap;
  flex-shrink: 0;
}

.toolbar-group {
  display: inline-flex;
  align-items: center;
  margin-right: 6px;
  padding-right: 6px;
  border-right: 1px solid var(--c-border);

  &:last-child {
    margin-right: 0;
    padding-right: 0;
    border-right: 0;
  }
}

.custom-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: var(--c-text-secondary);
  font-size: 13px;
  font-weight: 600;
}

.custom-button:hover,
.custom-button.ql-active {
  color: var(--c-brand);
}

// Quill toolbar overrides
:deep(.ql-picker) {
  color: var(--c-text-secondary);
}

:deep(.ql-picker.ql-expanded) {
  .ql-picker-label {
    color: var(--c-brand);
    border-color: var(--c-brand);
  }
}

:deep(.ql-picker-label:hover),
:deep(.ql-picker-item:hover) {
  color: var(--c-brand);
}

:deep(.ql-picker-label) {
  color: var(--c-text-secondary);
}

:deep(button) {
  color: var(--c-text-secondary);
}

:deep(button:hover),
:deep(button.ql-active) {
  color: var(--c-brand);
}
</style>
