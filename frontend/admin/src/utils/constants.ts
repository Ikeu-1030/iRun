export const ADMIN_ROLES: Record<number, string> = {
  1: '超级管理员',
  2: '普通管理员'
}

export const USER_STATUS: Record<number, string> = {
  0: '禁用',
  1: '正常'
}

export const CERTIFY_STATUS: Record<number, string> = {
  0: '未认证',
  1: '审核中',
  2: '已认证',
  3: '认证驳回'
}

export const TASK_TYPES: Record<string, string> = {
  '代取快递': '代取快递',
  '代拿餐食': '代拿餐食',
  '校内代办': '校内代办',
  '代购物品': '代购物品',
  '通用代办': '通用代办'
}

export const TASK_SUB_TYPES: Record<string, string> = {
  '小件快递': '小件快递',
  '中件快递': '中件快递',
  '大件快递': '大件快递',
  '校内餐饮': '校内餐饮',
  '校外餐饮': '校外餐饮',
  '奶茶咖啡代取': '奶茶咖啡代取',
  '资料打印': '资料打印',
  '图书馆还书': '图书馆还书',
  '物品急送': '物品急送',
  '帮扔杂物': '帮扔杂物',
  '办事代排': '办事代排',
  '纸品速达': '纸品速达',
  '文具急送': '文具急送',
  '校内代购': '校内代购',
  '校外代购': '校外代购'
}

export const TASK_STATUS: Record<number, string> = {
  1: '待接单',
  2: '已接单',
  3: '配送中',
  4: '待确认',
  5: '已完成',
  6: '已取消'
}

export const ORDER_STATUS: Record<number, string> = {
  1: '待取货',
  2: '配送中',
  3: '待确认',
  4: '已完成',
  5: '已取消'
}

export const TRANSACTION_TYPES: Record<number, string> = {
  1: '任务悬赏支出',
  2: '跑腿收入',
  3: '充值',
  4: '提现',
  5: '退款'
}

export const NOTIFICATION_TYPES: Record<number, string> = {
  1: '系统通知',
  2: '订单状态',
  3: '活动提醒'
}
