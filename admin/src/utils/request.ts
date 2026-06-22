import axios from 'axios'
import type { AxiosResponse, InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
import { useAuthStore } from '@/stores/auth'
import {
  getAdminToken,
  setAdminToken,
  removeAdminToken,
  getAdminRefreshToken,
  setAdminRefreshToken,
  removeAdminRefreshToken,
} from './tokenStore'

export {
  getAdminToken,
  setAdminToken,
  removeAdminToken,
  getAdminRefreshToken,
  setAdminRefreshToken,
  removeAdminRefreshToken,
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
      const refreshToken = getAdminRefreshToken()
      if (!refreshToken) {
        removeAdminToken()
        removeAdminRefreshToken()
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
          setAdminRefreshToken(newRefresh)
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
          return Promise.reject(error)
        } catch {
          refreshQueue = [] // 刷新失败，导航已触发，直接清空队列
          removeAdminToken()
          removeAdminRefreshToken()
          ElMessage.error('登录已过期，请重新登录')
          router.replace('/login')
          return Promise.reject(error)
        } finally {
          isRefreshing = false
        }
      } else {
        return new Promise((resolve, reject) => {
          refreshQueue.push((token: string) => {
            if (error.config) {
              error.config.headers['token'] = token
              resolve(service(error.config))
            } else {
              reject(error)
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
