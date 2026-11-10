import { defineStore } from 'pinia'
import { ref } from 'vue'
import request from '@/utils/request'

export const useWorkspaceStore = defineStore('workspace', () => {
  const spaces = ref<any[]>([])
  const spaceId = ref(localStorage.getItem('active_space_id') || '')
  const ready = ref(false)
  async function load() {
    ready.value = false
    try {
      const result: any = await request.get('/space/my')
      spaces.value = result.data || []
      select(spaces.value.some(s => String(s.spaceId) === spaceId.value) ? spaceId.value : String(spaces.value[0]?.spaceId || ''))
    } finally { ready.value = true }
  }
  function select(id: string) {
    spaceId.value = id
    if (id) localStorage.setItem('active_space_id', id)
    else localStorage.removeItem('active_space_id')
  }
  return { spaces, spaceId, ready, load, select }
})
