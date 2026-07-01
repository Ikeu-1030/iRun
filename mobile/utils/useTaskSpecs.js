import { computed } from 'vue'
import { TASK_TYPES, TASK_TYPE_META, TYPE_FROM_API, isQueueWaitType } from '@/utils/constants.js'
import {
  parseTaskSpecs, parseExpressPackagesFromSpecs, parseBookCountFromSpecs,
  parsePrintSpecsFromSpecs, parseMerchantInfoFromSpecs, parseFoodItemsFromSpecs,
  parseItemExpressFromSpecs, parseServiceDurationFromSpecs, parseExtraFeeFromSpecs
} from '@/utils/campus-data.js'

/**
 * 订单/任务详情页共享 composable — 从任务数据中提取规格信息。
 *
 * @param {import('vue').Ref<Object>} source — 包含 task/order 数据的 ref
 *   支持两种数据源：
 *     - order-waiting: taskApi.getTaskDetail() 返回的 TaskDetailVO
 *     - order-delivering/completed: orderApi.getOrderDetail() 返回的 OrderDetailVO
 */
export function useTaskSpecs(source) {
  const taskSpecs = computed(() => parseTaskSpecs(source.value.taskSpecs))

  const taskTypeCode = computed(() =>
    typeof source.value.type === 'number' ? source.value.type : (TYPE_FROM_API[source.value.type] != null ? TYPE_FROM_API[source.value.type] : 1))

  const typeLabel = computed(() => TASK_TYPES[taskTypeCode.value] || '任务')
  const rewardText = computed(() => Number(source.value.reward || 0).toFixed(2))

  const typeMeta = computed(() => TASK_TYPE_META[taskTypeCode.value] || TASK_TYPE_META[1])
  const typeIcon = computed(() => typeMeta.value.icon)
  const typeIconColor = computed(() => typeMeta.value.color)
  const typeColor = computed(() => ({ 1: 'blue', 2: 'orange', 3: 'green', 4: 'teal' }[taskTypeCode.value] || 'blue'))

  const isQueueWait = computed(() => isQueueWaitType(source.value.subType))
  const isPaperExpress = computed(() => taskTypeCode.value === 4 && source.value.subType === '纸品速达')

  // --- 任务类型专属规格 ---

  const productFeeText = computed(() => {
    if (taskTypeCode.value !== 4) return null
    const specs = taskSpecs.value
    if (specs && specs.预估商品费 != null) return Number(specs.预估商品费)
    return null
  })

  const productTags = computed(() => {
    if (taskTypeCode.value !== 4) return []
    if (isPaperExpress.value) return []
    const specs = taskSpecs.value
    if (specs && specs.商品列表) {
      return specs.商品列表.map(item =>
        item.规格 ? `${item.名称}x${item.数量}（${item.规格}）` : `${item.名称}x${item.数量}`)
    }
    const val = source.value.subType || ''
    if (!val) return []
    if (val.startsWith('[')) {
      try { return JSON.parse(val) } catch (e) { return [] }
    }
    return val.split('、')
  })

  const bookCount = computed(() => {
    if (taskTypeCode.value !== 3) return null
    return parseBookCountFromSpecs(taskSpecs.value)
  })

  const printSpecs = computed(() => {
    if (taskTypeCode.value !== 3) return null
    return parsePrintSpecsFromSpecs(taskSpecs.value)
  })

  const merchantTag = computed(() => {
    if (taskTypeCode.value !== 2) return null
    return parseMerchantInfoFromSpecs(taskSpecs.value)
  })

  const foodItems = computed(() => {
    if (taskTypeCode.value !== 2) return null
    return parseFoodItemsFromSpecs(taskSpecs.value)
  })

  const serviceDuration = computed(() => {
    if (!isQueueWait.value) return null
    return parseServiceDurationFromSpecs(taskSpecs.value)
  })

  const itemExpress = computed(() => {
    if (taskTypeCode.value !== 3) return null
    return parseItemExpressFromSpecs(taskSpecs.value)
  })

  const extraFee = computed(() => {
    if (taskTypeCode.value !== 5) return null
    return parseExtraFeeFromSpecs(taskSpecs.value)
  })

  // --- 动态标题/标签 ---

  const pickupSectionTitle = computed(() => {
    if (isQueueWait.value) return '代办信息'
    if (taskTypeCode.value === 2) return '取餐信息'
    if (taskTypeCode.value === 4 && !isPaperExpress.value) return '代购信息'
    return '取件信息'
  })

  const pickupAddressLabel = computed(() => {
    if (isQueueWait.value) return '代办地址'
    if (taskTypeCode.value === 2) return '取餐地址'
    if (taskTypeCode.value === 4 && !isPaperExpress.value) return '代购地址'
    return '取件地址'
  })

  const pickupCodeLabel = computed(() => {
    if (taskTypeCode.value === 2) return '取餐码'
    return '取件码'
  })

  // --- 描述文本 ---

  const displayDescription = computed(() => {
    const desc = source.value.publicDesc || ''
    const specs = taskSpecs.value
    if (taskTypeCode.value === 1) {
      const pkgSpecs = parseExpressPackagesFromSpecs(specs)
      if (pkgSpecs) {
        let text = `快递规格：${pkgSpecs.sizes}`
        if (desc) text += `\n需求描述：${desc}`
        return text
      }
    }
    return desc || '暂无描述'
  })

  return {
    taskSpecs, taskTypeCode, typeLabel, rewardText,
    typeMeta, typeIcon, typeIconColor, typeColor,
    isQueueWait, isPaperExpress,
    productFeeText, productTags, bookCount, printSpecs, merchantTag,
    foodItems, serviceDuration, itemExpress, extraFee,
    pickupSectionTitle, pickupAddressLabel, pickupCodeLabel,
    displayDescription,
  }
}
