/**
 * 错误处理中枢 — 分类、辅助函数、全局兜底
 *
 * 设计目标:
 *   - 后端 5 种异常 + 150 条消息 → 前端 9 种类型，页面 switch type 而非 match string
 *   - ClassifiedError 携带 handled 标记，防止 request.js 和页面双重 toast
 *   - 共享辅助函数消除跨页面的重复弹窗/提示代码
 */

import { showToast } from '@/utils/toast'

export const ErrorType = Object.freeze({
  AUTH_EXPIRED: 'AUTH_EXPIRED',       // 401 / token 过期 / refresh 失败
  AUTH_REQUIRED: 'AUTH_REQUIRED',     // 未登录（无 token — NOT_AUTH）
  FORBIDDEN: 'FORBIDDEN',             // 403 / 权限不足 / 非法状态转移
  NOT_FOUND: 'NOT_FOUND',             // 404 / 资源不存在
  NETWORK: 'NETWORK',                 // 网络异常 / uni.request fail
  SERVER_ERROR: 'SERVER_ERROR',       // 500 / 系统错误
  BUSINESS: 'BUSINESS',               // 400 / 业务规则失败 (code=0)
  PARAM_ERROR: 'PARAM_ERROR',         // 参数错误
  UNKNOWN: 'UNKNOWN'                  // 无法归类的异常
})

// ═══════════════════════════════════════════
// ClassifiedError
// ═══════════════════════════════════════════

const BRAND = Symbol('ClassifiedError')

export class ClassifiedError extends Error {
  /**
   * @param {string}   type          - ErrorType 值
   * @param {string}   message       - 用户可读的错误消息
   * @param {object}   [meta]
   * @param {number}   [meta.code]         - 后端业务 code（0/401/...）
   * @param {number}   [meta.httpStatus]   - HTTP 状态码
   * @param {boolean}  [meta.showError]    - request.js 是否显示了 toast
   * @param {boolean}  [meta.handled]      - 页面是否已处理
   */
  constructor(type, message, { code, httpStatus, showError, handled } = {}) {
    super(message)
    this.name = 'ClassifiedError'
    this.type = type
    this.code = code ?? null
    this.httpStatus = httpStatus ?? null
    this.showError = showError !== false
    this.handled = handled || false
    this[BRAND] = true
  }

  /** 标记已处理 → 防止后续环节再弹 toast */
  markHandled() {
    this.handled = true
    return this
  }
}

// ═══════════════════════════════════════════
// 分类函数
// ═══════════════════════════════════════════

/**
 * 将原始错误分类为 ClassifiedError
 *
 * @param {Error|string} rawError
 * @param {object} [ctx]
 * @param {number} [ctx.httpStatus]    - HTTP 状态码
 * @param {number} [ctx.businessCode]  - 后端返回的 body.code
 * @param {string} [ctx.businessMsg]   - 后端返回的 body.msg
 * @param {boolean} [ctx.showError]    - request.js showError 标志
 * @returns {ClassifiedError}
 */
export function classifyError(rawError, ctx = {}) {
  // 已是 ClassifiedError → 直接返回（防止二次分类丢失 type）
  if (rawError != null && rawError[BRAND] === true) {
    return rawError
  }

  const msg = rawError?.message || rawError || ctx.businessMsg || '未知错误'
  const { httpStatus, businessCode, showError } = ctx

  // 1. 前端内部已知消息 — 精确匹配
  if (msg === 'NOT_AUTH') {
    return new ClassifiedError(ErrorType.AUTH_REQUIRED, '请先登录', { showError: false, handled: true })
  }
  if (msg === 'NO_REFRESH_TOKEN') {
    return new ClassifiedError(ErrorType.AUTH_EXPIRED, '登录已过期，请重新登录', { code: 401, showError })
  }
  if (msg === 'REFRESH_FAILED') {
    return new ClassifiedError(ErrorType.AUTH_EXPIRED, '登录已过期，请重新登录', { code: 401, showError })
  }
  if (msg === 'AUTH_EXPIRED') {
    return new ClassifiedError(ErrorType.AUTH_EXPIRED, '登录已过期，请重新登录', { code: 401, showError })
  }

  // 2. HTTP 状态码分类
  if (httpStatus) {
    if (httpStatus === 401) return new ClassifiedError(ErrorType.AUTH_EXPIRED, msg, { httpStatus, showError })
    if (httpStatus === 403) return new ClassifiedError(ErrorType.FORBIDDEN, msg, { httpStatus, showError })
    if (httpStatus === 404) return new ClassifiedError(ErrorType.NOT_FOUND, msg, { httpStatus, showError })
    if (httpStatus >= 500) return new ClassifiedError(ErrorType.SERVER_ERROR, '系统繁忙，请稍后重试', { httpStatus, showError })
    return new ClassifiedError(ErrorType.UNKNOWN, msg, { httpStatus, showError })
  }

  // 3. 后端 body.code 分类（JWT 拦截器直接写入 code=401）
  if (businessCode === 401) {
    return new ClassifiedError(ErrorType.AUTH_EXPIRED, msg, { code: businessCode, showError })
  }

  // 4. 网络层异常（uni.request fail 回调）
  if (rawError?.errMsg || rawError?.errno !== undefined) {
    return new ClassifiedError(ErrorType.NETWORK, '网络异常，请稍后重试', { showError })
  }

  // 5. 兜底 → BUSINESS（后端 code=0 或未识别的业务错误）
  return new ClassifiedError(ErrorType.BUSINESS, msg, { code: businessCode, showError })
}

// ═══════════════════════════════════════════
// 类型检查
// ═══════════════════════════════════════════

/**
 * 安全类型检查（用 branded property，避免 instanceof 跨模块边界问题）
 * @param {*} error
 * @param {string} type - ErrorType 值
 * @returns {boolean}
 */
export function isErrorType(error, type) {
  return error != null && error[BRAND] === true && error.type === type
}

// ═══════════════════════════════════════════
// 共享 UI 辅助函数
// ═══════════════════════════════════════════

/**
 * 检查用户是否已实名认证，未认证则 toast + 跳转认证页
 * @param {import('@/store').useStore} store
 * @returns {boolean} true = 已认证
 */
export function requireCertified(store) {
  if (!store.isCertified) {
    showToast('请先完成实名认证')
    uni.navigateTo({ url: '/pages/certify/certify' })
    return false
  }
  return true
}

/**
 * 引导用户设置支付密码（弹窗确认后跳转设置页）
 */
export function guideToSetPayPassword() {
  uni.showModal({
    title: '未设置支付密码',
    content: '您还未设置支付密码，请先前往设置。',
    confirmText: '去设置',
    success: (res) => {
      if (res.confirm) {
        uni.navigateTo({ url: '/pages/pay-password-edit/pay-password-edit?mode=set' })
      }
    }
  })
}

// ═══════════════════════════════════════════
// 页面 catch 块标准处理
// ═══════════════════════════════════════════

/**
 * 页面级 catch 块标准处理函数。
 *
 * 用法示例:
 *   try { await someApi() }
 *   catch (e) {
 *     handlePageError(e, {
 *       toastAlreadyShown: true,
 *       customHandlers: {
 *         [ErrorType.BUSINESS]: (err) => {
 *           if (err.message.includes('请先设置支付密码')) guideToSetPayPassword()
 *         }
 *       }
 *     })
 *   }
 *
 * @param {Error} error
 * @param {object} [options]
 * @param {boolean} [options.toastAlreadyShown]  - request.js 已弹过 toast（showError=true 的默认行为）
 * @param {Object.<string, Function>} [options.customHandlers] - ErrorType → handler 映射
 * @returns {ClassifiedError}
 */
export function handlePageError(error, options = {}) {
  const classified = error instanceof ClassifiedError ? error : classifyError(error)

  const handler = options.customHandlers?.[classified.type]
  if (handler) {
    handler(classified)
    return classified.markHandled()
  }

  // request.js 已显示 toast 的错误，页面无需再弹
  if (options.toastAlreadyShown) {
    return classified
  }

  // 对未提示过的网络/服务器错误补一刀 toast
  if (classified.type === ErrorType.NETWORK || classified.type === ErrorType.SERVER_ERROR) {
    if (!classified.handled) {
      showToast(classified.message)
    }
  }

  return classified
}
