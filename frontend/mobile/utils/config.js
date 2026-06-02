/**
 * 多环境配置
 *
 * 通过微信小程序环境版本自动切换：
 *   develop → 开发环境 (localhost)
 *   trial   → 体验版 (测试服务器)
 *   release → 正式版 (生产服务器)
 */

// 尝试获取小程序环境版本，非微信环境默认 develop
let envVersion = 'develop'
try {
  const accountInfo = uni.getAccountInfoSync?.()
  envVersion = accountInfo?.miniProgram?.envVersion || 'develop'
} catch (e) {
  // H5 / 非微信环境
  envVersion = 'develop'
}

const ENV = {
  develop: {
    SERVER_ORIGIN: 'http://127.0.0.1:8080',
    BASE_URL: 'http://127.0.0.1:8080/api',
    WS_URL: 'ws://127.0.0.1:8080/api/ws'
  },
  trial: {
    SERVER_ORIGIN: 'https://test-api.runningerrands.com',
    BASE_URL: 'https://test-api.runningerrands.com/api',
    WS_URL: 'wss://test-api.runningerrands.com/api/ws'
  },
  release: {
    SERVER_ORIGIN: 'https://api.runningerrands.com',
    BASE_URL: 'https://api.runningerrands.com/api',
    WS_URL: 'wss://api.runningerrands.com/api/ws'
  }
}

const current = ENV[envVersion] || ENV.develop

export const SERVER_ORIGIN = current.SERVER_ORIGIN
export const BASE_URL = current.BASE_URL
export const WS_URL = current.WS_URL
export const ENV_VERSION = envVersion

/** 将相对路径的头像 URL 补全为完整 URL，小程序不支持相对路径 */
export function normalizeAvatarUrl(url) {
  if (!url) return ''
  if (url.startsWith('http://') || url.startsWith('https://')) return url
  if (url.startsWith('/')) return SERVER_ORIGIN + url
  return url
}

export default { SERVER_ORIGIN, BASE_URL, WS_URL, ENV_VERSION }
