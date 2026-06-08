import axios from 'axios'
import type { AxiosResponse, InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
import { useAuthStore } from '@/stores/auth'

const ADMIN_TOKEN_KEY = 'admin_token'
const ADMIN_REFRESH_KEY = 'admin_refresh_token'

export function getAdminToken(): string | null {
  return localStorage.getItem(ADMIN_TOKEN_KEY)
}

export function setAdminToken(token: string) {
  localStorage.setItem(ADMIN_TOKEN_KEY, token)
}

export function removeAdminToken() {
  localStorage.removeItem(ADMIN_TOKEN_KEY)
}

function getRefreshToken(): string | null {
  return localStorage.getItem(ADMIN_REFRESH_KEY)
}

function setRefreshToken(token: string) {
  localStorage.setItem(ADMIN_REFRESH_KEY, token)
}

function removeRefreshToken() {
  localStorage.removeItem(ADMIN_REFRESH_KEY)
}

let isRefreshing = false
let refreshQueue: Array<(token: string) => void> = []

// 开发环境 (Vite proxy 转发到 Ngrok 后端)
const API_BASE = '/api'

const service = axios.create({ baseURL: API_BASE,
  timeout: 15000
})

service.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = getAdminToken()
  if (token && config.headers) {
    config.headers['token'] = token
  }
  return config
})

function formatDates(obj: unknown): unknown {
  if (typeof obj === 'string') {
    return obj.replace(/(\d)T(\d{2}:\d{2})/g, '$1 $2')
  }
  if (Array.isArray(obj)) {
    return obj.map(formatDates)
  }
  if (obj && typeof obj === 'object') {
    const result: Record<string, unknown> = {}
    for (const key of Object.keys(obj as Record<string, unknown>)) {
      result[key] = formatDates((obj as Record<string, unknown>)[key])
    }
    return result
  }
  return obj
}

service.interceptors.response.use(
  (response: AxiosResponse) => {
    const body = response.data
    if (body.code === 1) {
      return formatDates(body.data)
    }
    ElMessage.error(body.msg || '请求失败')
    return Promise.reject(new Error(body.msg))
  },
  async (error) => {
    if (error.response?.status === 401) {
      const refreshToken = getRefreshToken()
      if (!refreshToken) {
        removeAdminToken()
        removeRefreshToken()
        ElMessage.error('登录已过期，请重新登录')
        router.replace('/login')
        return Promise.reject(error)
      }

      if (!isRefreshing) {
        isRefreshing = true
        try {
          const res = await axios.post('/api/admin/refresh', null, {
            headers: { 'X-Refresh-Token': refreshToken }
          })
          const { token, refreshToken: newRefresh, adminId, username, name, role } = res.data.data
          setAdminToken(token)
          setRefreshToken(newRefresh)
          const authStore = useAuthStore()
          if (adminId) {
            authStore.adminInfo = { adminId, username, name, role }
          }
          refreshQueue.forEach(cb => cb(token))
          refreshQueue = []

          if (error.config) {
            error.config.headers['token'] = token
            return service(error.config)
          }
        } catch {
          removeAdminToken()
          removeRefreshToken()
          ElMessage.error('登录已过期，请重新登录')
          router.replace('/login')
          return Promise.reject(error)
        } finally {
          isRefreshing = false
        }
      } else {
        return new Promise(resolve => {
          refreshQueue.push((token: string) => {
            if (error.config) {
              error.config.headers['token'] = token
              resolve(service(error.config))
            }
          })
        })
      }
    }
    ElMessage.error(error.response?.data?.msg || '网络错误')
    return Promise.reject(error)
  }
)

export default service
