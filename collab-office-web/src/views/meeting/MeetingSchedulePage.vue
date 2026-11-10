<template>
  <div class="schedule-page">
    <div class="page-header">
      <div class="header-left">
        <span class="material-symbols-outlined header-icon">video_call</span>
        <div>
          <h2 class="t-h2" style="color: var(--c-text)">预约会议</h2>
          <p class="t-body-md" style="color: var(--c-text-muted)">创建一个新会议，邀请团队成员参与</p>
        </div>
      </div>
    </div>

    <div class="form-card">
      <el-form label-position="top" size="large" class="meeting-form">
        <el-form-item>
          <template #label>
            <div class="form-label">
              <span class="material-symbols-outlined label-icon">title</span>
              <span>会议主题</span>
            </div>
          </template>
          <el-input v-model="form.title" placeholder="输入会议主题，如：Q3 产品规划评审" />
        </el-form-item>

        <el-form-item>
          <template #label>
            <div class="form-label">
              <span class="material-symbols-outlined label-icon">list_alt</span>
              <span>会议议程</span>
            </div>
          </template>
          <el-input v-model="form.agenda" type="textarea" :rows="3" placeholder="描述会议议程..." />
        </el-form-item>

        <el-form-item>
          <template #label>
            <div class="form-label">
              <span class="material-symbols-outlined label-icon">schedule</span>
              <span>时间</span>
            </div>
          </template>
          <el-date-picker
            v-model="form.time"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            format="MM/DD HH:mm"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item>
          <template #label>
            <div class="form-label">
              <span class="material-symbols-outlined label-icon">key</span>
              <span>入会密码（可选）</span>
            </div>
          </template>
          <el-input v-model="form.password" placeholder="不填则无需密码" show-password>
            <template #prefix>
              <span class="material-symbols-outlined input-prefix-icon">lock</span>
            </template>
          </el-input>
        </el-form-item>

        <el-form-item class="form-actions">
          <el-button size="large" @click="$router.back()">
            <span class="material-symbols-outlined">close</span>
            取消
          </el-button>
          <button type="button" class="submit-btn" @click="submit">
            <span class="material-symbols-outlined">add</span>
            创建会议
          </button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { createMeeting } from '@/api/meeting'

const router = useRouter()

const form = reactive({
  title: '',
  agenda: '',
  time: null as [Date, Date] | null,
  password: '',
})

function submit() {
  if (!form.title.trim()) return
  const data: any = { title: form.title, agenda: form.agenda, password: form.password || undefined }
  if (form.time) {
    data.startTime = form.time[0].toISOString()
    data.endTime = form.time[1].toISOString()
  }
  createMeeting(data).then((res: any) => {
    if (res.code === 200) {
      ElMessage.success('会议已创建')
      router.push('/meetings')
    }
  }).catch(() => ElMessage.error('创建失败'))
}
</script>

<style lang="scss" scoped>
.schedule-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
  max-width: 640px;
}

.page-header { margin-bottom: 4px; }
.header-left {
  display: flex; align-items: flex-start; gap: 14px;
}
.header-icon {
  font-size: 36px; color: var(--c-brand);
  background: var(--c-brand-light); padding: 10px; border-radius: var(--radius-lg);
}

.form-card {
  background: var(--c-surface);
  border: 1px solid var(--c-border);
  border-radius: var(--radius-xl);
  padding: 28px 32px;
  box-shadow: var(--shadow-xs);
}

.meeting-form {
  :deep(.el-form-item__label) { margin-bottom: 0; }
}

.form-label {
  display: flex; align-items: center; gap: 6px;
  font-size: 13px; font-weight: 500; color: var(--c-text-secondary);
}
.label-icon { font-size: 18px; color: var(--c-text-muted); }

.input-prefix-icon { font-size: 18px; color: var(--c-text-muted); }

.form-actions {
  display: flex; gap: 12px; justify-content: flex-end;
  padding-top: 8px; border-top: 1px solid var(--c-border-light); margin-top: 8px;

  :deep(.el-form-item__content) { display: flex; gap: 12px; justify-content: flex-end; }
}

.submit-btn {
  display: flex; align-items: center; gap: 6px;
  padding: 12px 28px;
  background: var(--c-brand); color: #fff; border: none;
  border-radius: var(--radius-lg); font-size: 15px; font-weight: 500; cursor: pointer;
  transition: all .2s var(--ease-out);
  &:hover { background: var(--c-brand-hover); transform: translateY(-1px); box-shadow: var(--shadow-sm); }
  &:active { transform: scale(.98); }
  .material-symbols-outlined { font-size: 20px; }
}

:deep(.el-input__wrapper) {
  border-radius: var(--radius-lg) !important; box-shadow: none !important; border: 1px solid var(--c-border);
  transition: all .15s var(--ease-out);
  &:hover { border-color: var(--c-brand); }
  &:focus-within, &.is-focus { border-color: var(--c-brand); box-shadow: 0 0 0 3px rgba(51,112,255,.1) !important; }
}
</style>
