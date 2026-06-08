/**
 * 常量映射 — 任务/订单/交易状态码 → 中文标签
 */

// ---------- 任务状态 (§14.1) ----------
export const TASK_STATUS = {
  1: '待接单',
  2: '已接单',
  3: '配送中',
  4: '待确认',
  5: '已完成',
  6: '已取消'
}

// ---------- 订单状态 (§14.2) ----------
export const ORDER_STATUS = {
  1: '待取货',
  2: '待送达',
  3: '待确认',
  4: '已完成',
  5: '已取消'
}

// ---------- 任务大类 (§14.3) ----------
export const TASK_TYPES = {
  1: '代取快递',
  2: '代拿餐食',
  3: '校内代办',
  4: '代购物品',
  5: '通用代办'
}

// ---------- 任务小类 (§14.4) ----------
export const TASK_SUB_TYPES = {
  11: '小件快递',
  12: '中件快递',
  13: '大件快递',
  21: '校内餐饮',
  22: '校外餐饮',
  23: '奶茶咖啡代取',
  31: '资料打印',
  32: '图书馆还书',
  33: '物品急送',
  34: '帮扔杂物',
  35: '办事代排',
  41: '纸品速达',
  42: '文具急送',
  43: '校内代购',
  44: '校外代购'
}

// ---------- 交易类型 (§14.5) ----------
export const TRANSACTION_TYPES = {
  1: '任务支出',
  2: '跑腿收入',
  3: '充值',
  4: '提现',
  5: '退款'
}

// ---------- 通知类型 (§14.6) ----------
export const NOTIFICATION_TYPES = {
  1: '系统通知',
  2: '订单状态',
  3: '活动提醒'
}

// ---------- 跑腿员认证状态 (§14.7) ----------
export const CERTIFY_STATUS = {
  0: '未认证',
  1: '审核中',
  2: '已认证',
  3: '认证驳回'
}

// ---------- 管理员角色 (§14.8) ----------
export const ADMIN_ROLES = {
  1: '超级管理员',
  2: '运营',
  3: '财务'
}

// ---------- 订单状态对应的步骤索引 (用于 uni-steps) ----------
export const ORDER_STEP_INDEX = {
  1: 0, // 待取货 → step 0
  2: 1, // 配送中 → step 1
  3: 2, // 待确认 → step 2
  4: 3  // 已完成 → step 3
}

// ---------- 订单状态对应的徽标样式 ----------
export const ORDER_STATUS_BADGE = {
  1: { text: '待取货', type: 'primary', bg: '#FFF0ED', color: '#FF6B4A' },
  2: { text: '待送达', type: 'warning', bg: '#fff7ed', color: '#ad6200' },
  3: { text: '待确认', type: 'primary', bg: '#FFF0ED', color: '#FF6B4A' },
  4: { text: '已完成', type: 'default', bg: '#ECFDF5', color: '#065F46' },
  5: { text: '已取消', type: 'info', bg: '#F5F5F0', color: '#5E5D58' }
}

// ---------- 任务状态对应的徽标样式 ----------
export const TASK_STATUS_BADGE = {
  1: { text: '待接单', type: 'primary', bg: '#FFF0ED', color: '#FF6B4A' },
  2: { text: '已接单', type: 'warning', bg: '#fff7ed', color: '#ad6200' },
  3: { text: '配送中', type: 'warning', bg: '#fff7ed', color: '#ad6200' },
  4: { text: '待确认', type: 'success', bg: '#f0fdf4', color: '#16a34a' },
  5: { text: '已完成', type: 'default', bg: '#ECFDF5', color: '#065F46' },
  6: { text: '已取消', type: 'info', bg: '#F5F5F0', color: '#5E5D58' }
}

// ---------- 任务类型 → 图标/颜色映射 ----------
export const TASK_TYPE_META = {
  1: { icon: 'express', color: '#FF6B4A', bg: '#FFF0ED' },
  2: { icon: 'fire', color: '#e67e22', bg: '#fff7ed' },
  3: { icon: 'campusErrand', color: '#4e5f82', bg: '#f0fdf4' },
  4: { icon: 'shop', color: '#0891B2', bg: '#ecf2ff' },
  5: { icon: 'compose', color: '#7C3AED', bg: '#F5F3FF' }
}

/** 获取任务状态标签 */
export function getTaskStatusLabel(status) {
  return TASK_STATUS[status] || '未知'
}

/** 获取订单状态标签 */
export function getOrderStatusLabel(status) {
  return ORDER_STATUS[status] || '未知'
}

/** 获取任务类型标签 */
export function getTaskTypeLabel(type) {
  return TASK_TYPES[type] || '未知'
}

// ---------- API 适配：整数 ↔ 后端中文格式 ----------

/** Integer → 中文 type（API 请求用） */
export const TYPE_TO_API = { 1: '代取快递', 2: '代拿餐食', 3: '校内代办', 4: '代购物品', 5: '通用代办' }

/** 中文 type → Integer（API 响应解析用） */
export const TYPE_FROM_API = { '代取快递': 1, '代拿餐食': 2, '校内代办': 3, '代购物品': 4, '通用代办': 5 }

/** Integer → String subType（API 请求用） */
export const SUBTYPE_TO_VALUE = {
  11: '小件快递', 12: '中件快递', 13: '大件快递',
  21: '校内餐饮', 22: '校外餐饮', 23: '奶茶咖啡代取',
  31: '资料打印',
  32: '图书馆还书', 33: '物品急送', 34: '帮扔杂物', 35: '办事代排',
  41: '纸品速达', 42: '文具急送', 43: '校内代购', 44: '校外代购'
}

/** 办事代排 subType code */
export const SUBTYPE_QUEUE_WAIT = 35

/** 判断是否为办事代排子类型（兼容字符串和整数） */
export function isQueueWaitType(subType) {
  if (!subType) return false
  if (typeof subType === 'number') return subType === SUBTYPE_QUEUE_WAIT
  return subType === '办事代排'
}

/** String subType → Integer code（API 响应解析用） */
export function subTypeToCode(val) {
  if (!val) return null
  for (const [code, label] of Object.entries(SUBTYPE_TO_VALUE)) {
    if (label === val) return Number(code)
  }
  return null
}
