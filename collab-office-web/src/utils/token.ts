const TOKEN_KEY = 'access_token'
const REFRESH_TOKEN_KEY = 'refresh_token'

export function readRefreshResponse(response: any, previousRefreshToken: string) {
  const data = response?.data
  const accessToken = typeof data === 'string' ? data : data?.accessToken
  if (response?.code !== 200 || typeof accessToken !== 'string' || !accessToken.trim()) {
    throw new Error('登录已过期，请重新登录')
  }
  const refreshToken = typeof data?.refreshToken === 'string' && data.refreshToken.trim()
    ? data.refreshToken : previousRefreshToken
  return { accessToken, refreshToken }
}

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(accessToken: string, refreshToken?: string): void {
  localStorage.setItem(TOKEN_KEY, accessToken)
  if (refreshToken) {
    localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken)
  }
}

export function removeToken(): void {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(REFRESH_TOKEN_KEY)
}

export function getRefreshToken(): string | null {
  return localStorage.getItem(REFRESH_TOKEN_KEY)
}

export function hasToken(): boolean {
  return !!localStorage.getItem(TOKEN_KEY)
}
