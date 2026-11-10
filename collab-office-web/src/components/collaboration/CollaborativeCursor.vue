<!-- Kinetic Sync §1: Collaborative Cursors -->
<template>
  <div class="collab-cursors" aria-hidden="true">
    <div
      v-for="user in users"
      :key="user.userId"
      class="cursor"
      :style="{
        top: user.top + 'px',
        left: user.left + 'px',
        height: user.height + 'px',
        '--cursor-color': user.color
      }"
    >
      <div class="cursor-line" />
      <span class="cursor-label" :class="{ fading: user.isIdle }">
        {{ user.name }}
      </span>
    </div>
  </div>
</template>

<script setup lang="ts">
interface CursorUser {
  userId: string
  name: string
  color: string
  top: number
  left: number
  height: number
  isIdle: boolean
}

defineProps<{ users: CursorUser[] }>()
</script>

<style lang="scss" scoped>
.collab-cursors {
  position: absolute;
  inset: 0;
  pointer-events: none;
  z-index: 10;
}

.cursor {
  position: absolute;
  display: flex;
  flex-direction: column;
  transition: top .05s linear, left .05s linear;
}

.cursor-line {
  width: 2px;
  height: 100%;
  background: var(--cursor-color, #3370FF);
  border-radius: 1px;
}

.cursor-label {
  position: absolute;
  top: -2px;
  left: 4px;
  padding: 2px 6px;
  font-size: 12px;
  font-weight: 500;
  line-height: 16px;
  color: #fff;
  background: var(--cursor-color, #3370FF);
  border-radius: 3px;
  white-space: nowrap;
  opacity: 1;
  transition: opacity 0.3s ease;

  &.fading { opacity: 0.35; }
}
</style>
