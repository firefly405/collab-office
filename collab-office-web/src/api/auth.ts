import request from '@/utils/request'

export function login(account: string, password: string) {
  return request.post('/auth/login', { account, password })
}

export function register(data: { username: string; phone: string; password: string; smsCode: string }) {
  return request.post('/auth/register', data)
}

export function sendSmsCode(phone: string) {
  return request.post('/auth/send-sms-code', { phone })
}

export function refreshToken(refreshToken: string) {
  return request.post('/auth/refresh-token', { refreshToken })
}

export function getUserInfo() {
  return request.get('/user/info')
}

export function getSpaces() {
  return request.get('/space/my')
}

export function createSpace(name: string, description?: string) {
  return request.post('/space/create', { spaceName: name, description })
}

export function logout() {
  return request.post('/auth/logout')
}
