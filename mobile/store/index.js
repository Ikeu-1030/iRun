/**
 * Pinia 全局 Store — 登录态 + 用户信息缓存
 *
 * 页面数据（账单/地址/订单列表等）由各页面直接调用 API，
 * Store 只负责跨页面共享的登录态和用户基本信息。
 */
import { defineStore } from 'pinia'
import { getToken, setToken, removeToken, setRefreshToken, removeRefreshToken } from '@/utils/request'
import { classifyError, isErrorType, ErrorType } from '@/utils/error'
import { SERVER_ORIGIN } from '@/utils/config'
import * as userApi from '@/api/user'
import { useChatStore } from '@/store/chat.js'

const PAY_PW_KEY = 'd2d_has_pay_password'

function getPersistedPayPassword() {
  try {
    return uni.getStorageSync(PAY_PW_KEY) === true || uni.getStorageSync(PAY_PW_KEY) === '1'
  } catch (e) {
    return false
  }
}

function persistPayPassword(val) {
  try {
    uni.setStorageSync(PAY_PW_KEY, val ? '1' : '0')
  } catch (e) { /* ignore */ }
}

export const useStore = defineStore('main', {
  state: () => ({
    // 登录态
    token: getToken() || '',
    userId: null,

    // 用户信息（从 GET /user/info 加载）
    userInfo: {
      id: null,
      username: '',
      nickname: '',
      avatarUrl: '',
      phone: '',
      realName: '',
      studentId: '',
      balance: 0,
      isCertify: 0,
      verifyStatus: 0,
      creditScore: 100,
      banUntil: null,
      registerType: 1,
      campus: '',
      signature: '',
      sex: '',
      certifyImg: '',
      certifyRemark: ''
    },

    // 支付密码是否已设置（本地持久化以跨会话记忆）
    hasPayPassword: getPersistedPayPassword(),

    // 全局就绪标识
    ready: false
  }),

  getters: {
    isLoggedIn: (state) => !!state.token,
    displayName: (state) => state.userInfo.nickname || state.userInfo.realName || '用户',
    avatarText: (state) => (state.userInfo.nickname || state.userInfo.realName || '用').charAt(0),
    avatarUrl: (state) => {
      const url = state.userInfo.avatarUrl
      if (!url || url.includes('default_avatar')) return '/static/default_avatar.jpg'
      return url
    },
    isCertified: (state) => state.userInfo.isCertify === 2,
    isCertifiedRunner: (state) => state.userInfo.verifyStatus === 2,
    isCreditFrozen: (state) => {
      if (!state.userInfo.banUntil) return false
      return new Date(state.userInfo.banUntil) > new Date()
    },
    balanceText: (state) => `¥ ${(state.userInfo.balance || 0).toFixed(2)}`,
    certifyStatusLabel: (state) => {
      const map = { 0: '未认证', 1: '审核中', 2: '已认证', 3: '已驳回' }
      return map[state.userInfo.isCertify] || '未认证'
    },
    runnerCertStatusLabel: (state) => {
      const map = { 0: '未认证', 1: '审核中', 2: '已认证', 3: '已驳回' }
      return map[state.userInfo.verifyStatus] || '未认证'
    }
  },

  actions: {
    // ---------- 登录 ----------
    saveLogin(data) {
      this.token = data.token
      this.userId = data.userId
      if (data.nickname) this.userInfo.nickname = data.nickname
      if (data.avatarUrl) this.userInfo.avatarUrl = data.avatarUrl
      setToken(data.token)
      if (data.refreshToken) setRefreshToken(data.refreshToken)
      // 登录后连接全局聊天 WebSocket
      try {
        const chatStore = useChatStore()
        if (!chatStore.wsConnected) {
          chatStore.connectStomp(data.token, data.userId)
        }
      } catch (e) { /* ignore */ }
    },

    async login(payload) {
      const data = await userApi.login(payload)
      this.saveLogin(data)
      return data
    },

    async wechatLogin(code, encryptedData, iv) {
      const data = await userApi.wechatLogin(code, encryptedData, iv)
      this.saveLogin(data)
      return data
    },

    // ---------- 用户信息 ----------
    async fetchUserInfo() {
      if (!this.token) return null
      try {
        const data = await userApi.getUserInfo()
        // 后端返回的相对路径补全为完整URL（小程序不支持相对路径）
        if (data.avatarUrl && data.avatarUrl.startsWith('/') && !data.avatarUrl.includes('default_avatar')) {
          data.avatarUrl = SERVER_ORIGIN + data.avatarUrl
        }
        this.userInfo = { ...this.userInfo, ...data }
        this.userId = data.id
        return data
      } catch (e) {
        const classified = classifyError(e)
        if (isErrorType(classified, ErrorType.AUTH_REQUIRED)) {
          return null
        }
        if (isErrorType(classified, ErrorType.AUTH_EXPIRED)) {
          this.clearLogin()
          throw classified
        }
        throw e
      }
    },

    async updateProfile(payload) {
      await userApi.updateProfile(payload)
      this.userInfo = { ...this.userInfo, ...payload }
    },

    // ---------- 支付密码 ----------
    markPayPasswordSet() {
      this.hasPayPassword = true
      persistPayPassword(true)
    },

    // ---------- 退出 ----------
    async logout() {
      try { await userApi.logout() } catch (e) { /* ignore */ }
      this.clearLogin()
    },

    clearLogin() {
      // 断开全局聊天 WebSocket
      try {
        const chatStore = useChatStore()
        chatStore.disconnectStomp()
        chatStore.clearCache()
      } catch (e) { /* ignore */ }
      this.token = ''
      this.userId = null
      this.userInfo = {
        id: null, username: '', nickname: '', avatarUrl: '', phone: '',
        realName: '', studentId: '', balance: 0, isCertify: 0, verifyStatus: 0, creditScore: 100, banUntil: null, registerType: 1,
        campus: '', signature: '', sex: '', certifyImg: '', certifyRemark: ''
      }
      this.hasPayPassword = false
      persistPayPassword(false)
      removeToken()
      removeRefreshToken()
    }
  }
})
