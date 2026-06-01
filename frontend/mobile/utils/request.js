/**
 * HTTP 请求封装 — 基于 uni.request
 *
 * 鉴权方式:
 *   用户端  → header: authentication = <token>
 *   管理端  → header: token = <token>
 *
 * 响应格式:
 *   { code: 1, msg: "操作成功", data: {} }
 *   分页:    { code: 1, data: { total: 100, records: [...] } }
 */

import { BASE_URL } from '@/utils/config'
import { classifyError } from '@/utils/error'
import { showToast } from '@/utils/toast'

// ---------- token 持久化 ----------

const TOKEN_KEY = 'd2d_user_token'
const ADMIN_TOKEN_KEY = 'd2d_admin_token'

export function getToken() {
  return uni.getStorageSync(TOKEN_KEY) || ''
}

export function setToken(token) {
  uni.setStorageSync(TOKEN_KEY, token)
}

export function removeToken() {
  uni.removeStorageSync(TOKEN_KEY)
}

export function getAdminToken() {
  return uni.getStorageSync(ADMIN_TOKEN_KEY) || ''
}

export function setAdminToken(token) {
  uni.setStorageSync(ADMIN_TOKEN_KEY, token)
}

export function removeAdminToken() {
  uni.removeStorageSync(ADMIN_TOKEN_KEY)
}

// ---------- refresh token 持久化 ----------

const REFRESH_TOKEN_KEY = 'd2d_user_refresh_token'
const ADMIN_REFRESH_TOKEN_KEY = 'd2d_admin_refresh_token'

export function getRefreshToken() {
  return uni.getStorageSync(REFRESH_TOKEN_KEY) || ''
}

export function setRefreshToken(token) {
  uni.setStorageSync(REFRESH_TOKEN_KEY, token)
}

export function removeRefreshToken() {
  uni.removeStorageSync(REFRESH_TOKEN_KEY)
}

export function getAdminRefreshToken() {
  return uni.getStorageSync(ADMIN_REFRESH_TOKEN_KEY) || ''
}

export function setAdminRefreshToken(token) {
  uni.setStorageSync(ADMIN_REFRESH_TOKEN_KEY, token)
}

export function removeAdminRefreshToken() {
  uni.removeStorageSync(ADMIN_REFRESH_TOKEN_KEY)
}

// ---------- refresh 队列 ----------

let isRefreshing = false
let refreshQueue = []

function performRefresh(auth) {
  const refreshToken = auth === 'admin' ? getAdminRefreshToken() : getRefreshToken()
  if (!refreshToken) {
    return Promise.reject(new Error('NO_REFRESH_TOKEN'))
  }

  const endpoint = BASE_URL + (auth === 'admin' ? '/admin/refresh' : '/user/refresh')

  return new Promise((resolve, reject) => {
    uni.request({
      url: endpoint,
      method: 'POST',
      data: {},
      header: {
        'Content-Type': 'application/json',
        'X-Refresh-Token': refreshToken
      },
      success(res) {
        const { statusCode, data: body } = res
        if (statusCode === 200 && body.code === 1) {
          const { token, refreshToken: newRefreshToken } = body.data
          if (auth === 'admin') {
            setAdminToken(token)
            setAdminRefreshToken(newRefreshToken)
          } else {
            setToken(token)
            setRefreshToken(newRefreshToken)
          }
          resolve(token)
        } else {
          reject(new Error('REFRESH_FAILED'))
        }
      },
      fail(err) {
        reject(err)
      }
    })
  })
}

function handleRefresh(auth) {
  if (isRefreshing) {
    return new Promise((resolve, reject) => {
      refreshQueue.push({ resolve, reject })
    })
  }

  isRefreshing = true
  return performRefresh(auth)
    .then((newToken) => {
      isRefreshing = false
      refreshQueue.forEach(({ resolve }) => {
        resolve(auth === 'admin' ? getAdminToken() : getToken())
      })
      refreshQueue = []
      return newToken
    })
    .catch((err) => {
      isRefreshing = false
      refreshQueue.forEach(({ reject }) => reject(err))
      refreshQueue = []
      return Promise.reject(err)
    })
}

// ---------- 请求核心 ----------

/**
 * @param {object} options
 * @param {string}  options.url           - 接口路径 (不含 base)
 * @param {string}  [options.method='GET']
 * @param {object}  [options.data]        - 请求体 / query
 * @param {string}  [options.auth='user'] - 'user' | 'admin' | 'none'
 * @param {boolean} [options.showLoading=true]
 * @param {boolean} [options.showError=true]
 * @returns {Promise<any>}  直接返回解析后的 data 字段
 */
export function request({
  url,
  method = 'GET',
  data = {},
  auth = 'user',
  showLoading = true,
  showError = true
}) {
  // 需要鉴权但无 token → 静默跳过，不弹 toast
  if (auth === 'user' && !getToken()) {
    return Promise.reject(classifyError('NOT_AUTH'))
  }
  if (auth === 'admin' && !getAdminToken()) {
    return Promise.reject(classifyError('NOT_AUTH'))
  }

  if (showLoading) {
    uni.showLoading({ title: '加载中…', mask: true })
  }

  const header = { 'Content-Type': 'application/json' }

  if (auth === 'user') {
    header['authentication'] = getToken()
  } else if (auth === 'admin') {
    header['token'] = getAdminToken()
  }

  return new Promise((resolve, reject) => {
    uni.request({
      url: BASE_URL + url,
      method,
      data,
      header,
      success(res) {
        if (showLoading) uni.hideLoading()

        const { statusCode, data: body } = res

        // HTTP 401 → 尝试 refresh token
        if (statusCode === 401 && auth !== 'none') {
          handleRefresh(auth)
            .then((newToken) => {
              const retryHeader = { 'Content-Type': 'application/json' }
              if (auth === 'user') retryHeader['authentication'] = newToken
              else if (auth === 'admin') retryHeader['token'] = newToken

              uni.request({
                url: BASE_URL + url,
                method,
                data,
                header: retryHeader,
                success(retryRes) {
                  if (showLoading) uni.hideLoading()
                  const { statusCode: sc, data: retryBody } = retryRes
                  if (sc !== 200) {
                    const err = classifyError(`HTTP ${sc}`, { httpStatus: sc, showError })
                    if (showError) showToast(err.message)
                    return reject(err)
                  }
                  if (retryBody.code === 1) {
                    resolve(retryBody.data)
                  } else {
                    const err = classifyError(retryBody.msg, { businessCode: retryBody.code, businessMsg: retryBody.msg, showError })
                    if (showError) showToast(err.message || '操作失败')
                    reject(err)
                  }
                },
                fail(retryErr) {
                  if (showLoading) uni.hideLoading()
                  const err = classifyError(retryErr, { showError })
                  if (showError) showToast(err.message)
                  reject(err)
                }
              })
            })
            .catch(() => {
              if (showLoading) uni.hideLoading()
              removeToken()
              removeRefreshToken()
              showToast('登录已过期，请重新登录', { duration: 2000 })
              setTimeout(() => {
                uni.reLaunch({ url: '/pages/login/login' })
              }, 1500)
              reject(classifyError('AUTH_EXPIRED'))
            })
          return
        }

        // 其他 HTTP 状态码异常——优先使用后端返回的 msg
        if (statusCode !== 200) {
          const msg = (body && body.msg) ? body.msg : `HTTP ${statusCode}`
          const err = classifyError(msg, { httpStatus: statusCode, businessCode: body?.code, businessMsg: msg, showError })
          if (showError) {
            showToast(err.message)
          }
          return reject(err)
        }

        // 业务 code 判断
        if (body.code === 1) {
          resolve(body.data)
        } else {
          const err = classifyError(body.msg, { businessCode: body.code, businessMsg: body.msg, showError })
          if (showError) {
            showToast(err.message || '操作失败')
          }
          reject(err)
        }
      },
      fail(err) {
        if (showLoading) uni.hideLoading()
        const classified = classifyError(err, { showError })
        if (showError) {
          showToast(classified.message)
        }
        reject(classified)
      }
    })
  })
}

// ---------- 便捷方法 ----------

export const get = (url, params, opts) =>
  request({ url, method: 'GET', data: params, ...opts })

export const post = (url, data, opts) =>
  request({ url, method: 'POST', data, ...opts })

export const put = (url, data, opts) =>
  request({ url, method: 'PUT', data, ...opts })

export const del = (url, opts) =>
  request({ url, method: 'DELETE', ...opts })
