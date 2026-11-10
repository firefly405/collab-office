import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getToken, setToken, removeToken } from '@/utils/token'
import { login as apiLogin, getUserInfo, getSpaces } from '@/api/auth'

export const useAuthStore = defineStore('auth', () => {
  const user = ref<any>(null)
  const spaces = ref<any[]>([])
  const token = ref<string | null>(getToken())

  const isLoggedIn = computed(() => !!token.value)
  const username = computed(() => user.value?.realName || user.value?.username || '用户')

  async function login(account: string, password: string) {
    const res: any = await apiLogin(account, password)
    if (res.code === 200) {
      setToken(res.data.accessToken, res.data.refreshToken)
      token.value = res.data.accessToken
      user.value = res.data.userInfo
      return true
    }
    return false
  }

  async function fetchUserInfo() {
    if (!token.value) return
    try {
      const res: any = await getUserInfo()
      if (res.code === 200) user.value = res.data
    } catch {}
  }

  async function loadSpaces() {
    try {
      const res: any = await getSpaces()
      if (res.code === 200) spaces.value = res.data || []
    } catch {}
  }

  function logout() {
    removeToken()
    token.value = null
    user.value = null
    spaces.value = []
  }

  return { user, spaces, token, isLoggedIn, username, login, fetchUserInfo, loadSpaces, logout }
})
