import axios from 'axios'
import type { AxiosResponse, InternalAxiosRequestConfig } from 'axios'
import { getToken, getRefreshToken, setToken, removeToken, readRefreshResponse } from './token'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: '/api/v1',
  timeout: 15000,
})

request.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = getToken()
    const spaceId = localStorage.getItem('active_space_id')
    if (spaceId && config.headers) config.headers['X-Space-Id'] = spaceId
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

let isRefreshing = false
let refreshQueue: Array<{ resolve: (token: string) => void; reject: (err: unknown) => void }> = []
let lastErrorMessage = ''
let lastErrorTime = 0

function showErrorMessage(message: string) {
  const now = Date.now()
  if (message === lastErrorMessage && now - lastErrorTime < 1500) return
  lastErrorMessage = message
  lastErrorTime = now
  ElMessage.error(message)
}

function getRequestErrorMessage(error: any): string {
  const url = error.config?.url
  const status = error.response?.status
  const data = error.response?.data
  const responseMessage = typeof data === 'string' ? data : data?.message
  if (responseMessage) return url ? `${responseMessage}: ${url}` : responseMessage
  if (status) return url ? `请求失败(${status}): ${url}` : `请求失败(${status})`
  if (error.code === 'ECONNABORTED') return url ? `请求超时: ${url}` : '请求超时'
  return url ? `网络错误: ${url}` : '网络错误'
}

request.interceptors.response.use(
  (response: AxiosResponse) => {
    const { data } = response
    if (data.code !== 200) {
      if (!(response.config as any)?._silent) {
        const url = response.config?.url || ''
        const msg = data.message || '请求失败'
        showErrorMessage(url ? `${msg} [${url}]` : msg)
      }
      const error = new Error(data.message || '请求失败') as Error & { displayed: boolean }
      error.displayed = true
      return Promise.reject(error)
    }
    return data
  },
  async (error) => {
    if (error.response?.status === 401) {
      const refreshToken = getRefreshToken()
      if (!refreshToken || error.config?._retried) {
        removeToken()
        showErrorMessage('登录已过期，请重新登录')
        window.location.hash = '#/login'
        return Promise.reject(error)
      }
      error.config._retried = true

      if (!isRefreshing) {
        isRefreshing = true
        try {
          const res = await axios.post('/api/v1/auth/refresh-token', { refreshToken })
          const { accessToken: newToken, refreshToken: newRefreshToken } = readRefreshResponse(res.data, refreshToken)
          setToken(newToken, newRefreshToken)
          refreshQueue.forEach((q) => q.resolve(newToken))
          refreshQueue = []
          error.config.headers.Authorization = `Bearer ${newToken}`
          return request(error.config)
        } catch (err) {
          refreshQueue.forEach((q) => q.reject(err))
          refreshQueue = []
          removeToken()
          showErrorMessage('登录已过期，请重新登录')
          window.location.hash = '#/login'
          return Promise.reject(err)
        } finally {
          isRefreshing = false
        }
      } else {
        return new Promise((resolve, reject) => {
          refreshQueue.push({ resolve, reject })
        }).then((token) => {
          error.config.headers.Authorization = `Bearer ${token}`
          return request(error.config)
        })
      }
    }
    if (!(error.config as any)?._silent) {
      showErrorMessage(getRequestErrorMessage(error))
    }
    return Promise.reject(error)
  }
)

export default request
