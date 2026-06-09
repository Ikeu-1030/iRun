/**
 * 管理端 taskSpecs JSON 解析工具
 *
 * 将任务规格 JSON 字符串解析为人类可读的摘要文本，用于在管理端详情页展示。
 * 覆盖所有任务子类型的规格字段。
 */

interface ExpressPackage {
  规格: string
  费用: number
  数量: number
}

interface ShoppingItem {
  名称: string
  数量: number
  规格?: string
}

interface TaskSpecs {
  包裹列表?: ExpressPackage[]
  商品列表?: ShoppingItem[]
  预估商品费?: number
  商家?: string
  餐品?: string
  服务时长?: number
  基础服务费?: number
  服务截止时间?: string
  物品名称?: string
  重量?: string
  书本数量?: number
  打印类型?: string
  打印方式?: string
  额外费用?: number
}

function safeParse(jsonStr: string | null | undefined): TaskSpecs | null {
  if (!jsonStr) return null
  try {
    return typeof jsonStr === 'string' ? JSON.parse(jsonStr) : jsonStr
  } catch {
    return null
  }
}

/**
 * 解析 taskSpecs JSON 为管理端展示用摘要字符串
 * @param specsStr task_specs 原始 JSON 字符串
 * @returns 人类可读的摘要文本，无数据时返回 '-'
 */
export function parseTaskSpecsForAdmin(specsStr: string | null | undefined): string {
  const specs = safeParse(specsStr)
  if (!specs) return '-'

  const parts: string[] = []

  // 代取快递 - 包裹列表
  if (specs.包裹列表 && specs.包裹列表.length > 0) {
    const pkgText = specs.包裹列表.map(p => `${p.规格} x${p.数量} (¥${p.费用})`).join(', ')
    parts.push(`包裹: ${pkgText}`)
  }

  // 代购物品 - 商品列表
  if (specs.商品列表 && specs.商品列表.length > 0) {
    const itemText = specs.商品列表.map(item =>
      item.规格 ? `${item.名称} x${item.数量} (${item.规格})` : `${item.名称} x${item.数量}`
    ).join(', ')
    parts.push(`商品: ${itemText}`)
    if (specs.预估商品费 != null) {
      parts.push(`预估商品费: ¥${specs.预估商品费}`)
    }
  }

  // 办事代排 - 服务时长
  if (specs.服务时长 != null) {
    const label = `${specs.服务时长}分钟`
    const fee = specs.基础服务费 != null ? ` (基础费: ¥${specs.基础服务费})` : ''
    parts.push(`服务时长: ${label}${fee}`)
  }

  // 物品急送
  if (specs.物品名称) {
    parts.push(`物品名称: ${specs.物品名称}${specs.重量 ? `, 重量: ${specs.重量}` : ''}`)
  }

  // 书本数量
  if (specs.书本数量 != null) {
    parts.push(`书本数量: ${specs.书本数量}本`)
  }

  // 资料打印
  if (specs.打印类型 || specs.打印方式) {
    parts.push(`打印: ${[specs.打印类型, specs.打印方式].filter(Boolean).join(' / ')}`)
  }

  // 代拿餐食 - 商家/餐品
  if (specs.商家) {
    parts.push(`商家: ${specs.商家}`)
  }
  if (specs.餐品) {
    parts.push(`餐品: ${specs.餐品}`)
  }

  // 通用代办 - 额外费用
  if (specs.额外费用 != null) {
    parts.push(`额外费用: ¥${specs.额外费用}`)
  }

  return parts.length > 0 ? parts.join('; ') : '-'
}
