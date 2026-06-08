import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getAdminToken, setAdminToken, removeAdminToken } from '@/utils/request'
import { getAdminInfo } from '@/api/auth'
import router from '@/router'

interface AdminInfo {
  adminId: number
  username: string
  name: string
  role: number
}

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(getAdminToken())
  const adminInfo = ref<AdminInfo | null>(null)

  const isLoggedIn = () => !!token.value

  const setAuth = (t: string, refreshToken: string, info: AdminInfo) => {
    token.value = t
    setAdminToken(t)
    localStorage.setItem('admin_refresh_token', refreshToken)
    adminInfo.value = info
  }

  const fetchAdminInfo = async () => {
    if (!token.value || adminInfo.value) return
    try {
      const info = await getAdminInfo()
      adminInfo.value = {
        adminId: info.adminId,
        username: info.username,
        name: info.name,
        role: info.role,
      }
    } catch {
      clearAuth()
    }
  }

  const clearAuth = () => {
    token.value = null
    adminInfo.value = null
    removeAdminToken()
    localStorage.removeItem('admin_refresh_token')
  }

  const logout = () => {
    clearAuth()
    router.replace('/login')
  }

  return { token, adminInfo, isLoggedIn, setAuth, fetchAdminInfo, clearAuth, logout }
})
