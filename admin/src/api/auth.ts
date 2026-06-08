import request from '@/utils/request'

export function login(data: { username: string; password: string }) {
  return request({ url: '/admin/login', method: 'post', data })
}

export function getAdminInfo() {
  return request({ url: '/admin/info', method: 'get' })
}

export function logout() {
  return request({ url: '/admin/logout', method: 'post' })
}

export function refreshToken(refreshToken: string) {
  return request({
    url: '/admin/refresh',
    method: 'post',
    headers: { 'X-Refresh-Token': refreshToken }
  })
}
