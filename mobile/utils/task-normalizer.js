/**
 * 统一任务范式化层 —— 消除各页面的重复 if-else 解析链
 *
 * 使用方式:
 *   import { normalizeTaskCard } from '@/utils/task-normalizer.js'
 *   const card = normalizeTaskCard(apiRecord, { role: 'viewer', isOwner: false })
 */
import {
  parseTaskSpecs,
  parseExpressPackagesFromSpecs,
  parseShoppingItemsFromSpecs,
  parseServiceDurationFromSpecs,
  parseItemExpressFromSpecs,
  parseBookCountFromSpecs,
  parsePrintSpecsFromSpecs,
  parseMerchantInfoFromSpecs,
  parseExtraFeeFromSpecs
} from './campus-data.js'
import { TASK_TYPE_META, getTaskTypeLabel, TYPE_FROM_API } from './constants.js'

const iconStyles = { 1: 'blue', 2: 'orange', 3: 'green', 4: 'teal' }

/**
 * 范式化任务卡片数据（task-hall / orders 列表共用）
 * @param {object} raw  API 原始记录
 * @param {object} opts
 * @param {'viewer'|'publisher'|'runner'} opts.role  当前用户角色
 * @param {boolean} opts.isOwner  当前用户是否为发布者
 */
export function normalizeTaskCard(raw, opts = {}) {
  const taskType = typeof raw.type === 'number' ? raw.type : (TYPE_FROM_API[raw.type] || 1)
  const typeMeta = TASK_TYPE_META[taskType] || TASK_TYPE_META[1]
  const specs = parseTaskSpecs(raw.taskSpecs || raw.task_specs)

  // 描述：直接使用 publicDesc（后端已按权限返回）
  const displayDesc = raw.publicDesc || ''

  const card = {
    taskId: raw.taskId || raw.id,
    orderId: raw.orderId || raw.order_id,
    type: taskType,
    taskNo: raw.taskNo || '',
    title: getTaskTypeLabel(taskType),
    description: displayDesc,
    rewardText: Number(raw.reward || 0).toFixed(2),
    pickupAddress: raw.pickupAddress || '',
    deliveryAddr: raw.deliveryAddress || '',
    iconType: typeMeta.icon,
    iconColor: typeMeta.color,
    iconStyle: iconStyles[taskType] || 'blue',

    // 类型化解析（默认值）
    packageInfo: null,
    productTags: [],
    productFee: null,
    serviceDuration: null,
    itemExpress: null,
    bookCount: null,
    printSpecs: null,
    merchantTag: null,
    extraFee: null,
    contactName: '',
    contactPhone: ''
  }

  // type=1：快递包裹
  if (taskType === 1) {
    const pkg = parseExpressPackagesFromSpecs(specs)
    if (pkg) card.packageInfo = pkg
  }

  // type=2：代拿餐食（商家信息 / 奶茶咖啡代取）
  if (taskType === 2) {
    card.merchantTag = parseMerchantInfoFromSpecs(specs)
  }

  // type=3：校内代办（服务时长 / 物品急送 / 书本数量 / 资料打印）
  if (taskType === 3) {
    card.serviceDuration = parseServiceDurationFromSpecs(specs)
    card.itemExpress = parseItemExpressFromSpecs(specs)
    card.bookCount = parseBookCountFromSpecs(specs)
    card.printSpecs = parsePrintSpecsFromSpecs(specs)
  }

  // type=4：代购物品
  if (taskType === 4) {
    const shop = parseShoppingItemsFromSpecs(specs)
    if (shop) {
      card.productFee = shop.estimatedProductFee
      card.productTags = shop.items.map(item =>
        item.规格 ? `${item.名称}x${item.数量}（${item.规格}）` : `${item.名称}x${item.数量}`
      )
    }
  }

  // type=5：通用代办（额外费用）
  if (taskType === 5) {
    card.extraFee = parseExtraFeeFromSpecs(specs)
  }

  // 骑手视图需要联系人信息
  if (opts.role === 'runner') {
    card.contactName = (raw.contactName && raw.contactName !== 'null') ? raw.contactName : ''
    card.contactPhone = (raw.contactPhone && raw.contactPhone !== 'null') ? raw.contactPhone : ''
  }

  return card
}
