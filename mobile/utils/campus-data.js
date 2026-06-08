/**
 * 校园建筑层级数据 + 快递驿站列表 + 地址解析工具
 */

// 校园区域 → 建筑列表
export const CAMPUS_AREAS = [
  {
    id: 'cd_teaching',
    name: '成都校区教学楼',
    buildings: [
      '综合教学楼', '第二教学楼', '东教学楼A栋', '东教学楼B栋',
      '东办公楼', '西教学楼A栋', '西教学楼B栋', '西办公楼', '实验大楼'
    ]
  },
  {
    id: 'cd_library',
    name: '成都校区图书馆',
    buildings: ['图书馆'],
    maxFloor: 2
  },
  {
    id: 'cd_dorm',
    name: '成都校区宿舍楼',
    buildings: [
      { name: '1栋', tag: '蓝区 | 男寝' },
      { name: '2栋', tag: '蓝区 | 男寝' },
      { name: '3栋', tag: '蓝区 | 男寝' },
      { name: '4栋', tag: '蓝区 | 男寝' },
      { name: '5栋', tag: '蓝区 | 男寝' },
      { name: '6栋', tag: '蓝区 | 男寝' },
      { name: '7栋', tag: '蓝区 | 女寝' },
      { name: '8A栋', tag: '蓝区 | 女寝' },
      { name: '8B栋', tag: '蓝区 | 女寝' },
      { name: '9栋', tag: '蓝区 | 女寝' },
      { name: '10栋', tag: '蓝区 | 男寝' },
      { name: '11A栋', tag: '蓝区 | 男寝' },
      { name: '11B栋', tag: '蓝区 | 男寝' },
      { name: '15栋', tag: '红区 | 男寝' },
      { name: '16栋', tag: '红区 | 男寝' },
      { name: '17栋', tag: '红区' },
      { name: '18栋', tag: '红区 | 女寝' },
      { name: '19栋', tag: '红区 | 女寝' },
      { name: '25栋', tag: '红区 | 女寝' },
      { name: '26栋', tag: '红区 | 女寝' },
      { name: '27栋', tag: '红区' },
      { name: '28栋', tag: '红区 | 男寝' },
      { name: '29栋', tag: '红区 | 男寝' },
      { name: '30栋', tag: '红区 | 男寝' },
      { name: '31栋', tag: '校外' },
      { name: '32栋', tag: '红区 | 男寝' },
      { name: '33栋', tag: '红区 | 女寝' }
    ]
  },
  {
    id: 'sf_dorm',
    name: '什邡校区宿舍楼',
    buildings: ['1A栋', '1B栋', '2A栋', '2B栋', '3A栋', '3B栋']
  }
]

// 默认楼层数（图书馆为2层）
export const DEFAULT_MAX_FLOOR = 6

// 快递驿站列表
export const PICKUP_STATIONS = [
  { id: 1, name: '成都校区校内菜鸟驿站' },
  { id: 2, name: '成都校区校内京东快递' },
  { id: 3, name: '成都校区校内邮政快递' },
  { id: 4, name: '成都校区校内顺丰快递' },
  { id: 5, name: '成都校区校外妈妈驿站' },
  { id: 6, name: '什邡校区3A快递站' },
  { id: 7, name: '什邡校区3B快递站' }
]

/**
 * 获取建筑显示名称（含标签）
 * @param {string|object} building
 * @returns {string}
 */
export function getBuildingDisplay(building) {
  if (typeof building === 'string') return building
  if (building.tag) return `${building.name}(${building.tag})`
  return building.name
}

/**
 * 仅获取建筑名称（不含标签）
 * @param {string|object} building
 * @returns {string}
 */
export function getBuildingName(building) {
  if (typeof building === 'string') return building
  return building.name
}

/**
 * 组合地址字符串存入后端 detail 字段
 * 格式: "{area} {building} {floor}层 {roomDetail}"
 */
export function buildAddressString(area, building, floor, roomDetail) {
  const buildingDisplay = getBuildingDisplay(building)
  const floorStr = floor ? `${floor}层` : ''
  const room = roomDetail || ''
  return [area, buildingDisplay, floorStr, room].filter(Boolean).join(' ')
}

/**
 * 解析完整地址字符串
 * 通过查找 "{n}层" 来切分公开/私密部分
 * @param {string} fullAddress
 * @returns {{ location: string, floor: number|null, roomDetail: string }}
 */
export function parseDeliveryAddress(fullAddress) {
  if (!fullAddress) return { location: '', floor: null, roomDetail: '' }
  const match = fullAddress.match(/(\d+)层/)
  if (!match) {
    return { location: fullAddress, floor: null, roomDetail: '' }
  }
  const location = fullAddress.substring(0, match.index).trim()
  const floor = parseInt(match[1], 10)
  const afterFloor = fullAddress.substring(match.index + match[0].length).trim()
  return { location, floor, roomDetail: afterFloor }
}

/**
 * 反向解析地址字符串，尝试匹配 CAMPUS_AREAS
 * 用于编辑已有地址时回填表单
 * @returns {{ areaIdx: number, buildingIdx: number, floor: number, roomDetail: string }|null}
 */
export function reverseParseAddress(detail) {
  if (!detail) return null
  // 尝试匹配区域
  for (let ai = 0; ai < CAMPUS_AREAS.length; ai++) {
    const area = CAMPUS_AREAS[ai]
    if (!detail.startsWith(area.name)) continue
    const afterArea = detail.substring(area.name.length).trim()
    // 尝试匹配建筑
    const buildings = area.buildings
    for (let bi = 0; bi < buildings.length; bi++) {
      const b = buildings[bi]
      const bName = getBuildingName(b)
      const bDisplay = getBuildingDisplay(b)
      // 检查是否以建筑名或显示名开头
      if (afterArea.startsWith(bDisplay) || afterArea.startsWith(bName)) {
        const prefix = afterArea.startsWith(bDisplay) ? bDisplay : bName
        const afterBuilding = afterArea.substring(prefix.length).trim()
        // 解析楼层
        const floorMatch = afterBuilding.match(/^(\d+)层/)
        let floor = 0
        let roomDetail = afterBuilding
        if (floorMatch) {
          floor = parseInt(floorMatch[1], 10)
          roomDetail = afterBuilding.substring(floorMatch[0].length).trim()
        }
        return { areaIdx: ai, buildingIdx: bi, floor, roomDetail }
      }
    }
    // 匹配到区域但未匹配到建筑
    return { areaIdx: ai, buildingIdx: -1, floor: 0, roomDetail: afterArea }
  }
  // 未匹配到任何区域
  return null
}

// encodeDescription / parsePublicDescription / parsePrivateDescription 已移除
// description 列已拆分为 public_desc + private_note，后端直接返回两列，无需编解码

// parseProductFee 已移除 —— 预估商品费现从 task_specs.预估商品费 读取

/**
 * 安全解析 task_specs JSON 字符串
 * @returns {object|null}
 */
export function parseTaskSpecs(specsStr) {
  if (!specsStr) return null
  try {
    if (typeof specsStr === 'string') return JSON.parse(specsStr)
    return specsStr
  } catch (e) {
    return null
  }
}

/**
 * 从 task_specs 解析代取快递包裹信息 (type=1)
 * @returns {{ sizes: string, totalPkg: number, packages: array }|null}
 */
export function parseExpressPackagesFromSpecs(specs) {
  const pkgs = specs?.包裹列表
  if (!pkgs || !pkgs.length) return null
  const sizes = pkgs.map(p => `${p.规格}x${p.数量}`).join('，')
  const totalPkg = pkgs.reduce((sum, p) => sum + p.数量, 0)
  return { sizes, totalPkg, packages: pkgs }
}

/**
 * 从 task_specs 解析服务时长信息 (type=3 subType=35)
 * @returns {{ duration: number, label: string, fee: number }|null}
 */
export function parseServiceDurationFromSpecs(specs) {
  if (!specs || !specs.服务时长) return null
  return {
    duration: specs.服务时长,
    label: specs.时长标签 || `${specs.服务时长}分钟`,
    fee: specs.基础服务费 || 0
  }
}

/**
 * 从 task_specs 解析代购商品信息 (type=4)
 * @returns {{ items: array, estimatedProductFee: number|null }|null}
 */
export function parseShoppingItemsFromSpecs(specs) {
  const items = specs?.商品列表
  if (!items || !items.length) return null
  return {
    items,
    estimatedProductFee: specs.预估商品费 || null
  }
}

/**
 * 从 task_specs 解析物品急送信息 (type=3 subType=33)
 * @returns {{ itemName: string, weight: string }|null}
 */
export function parseItemExpressFromSpecs(specs) {
  if (!specs || !specs.物品名称) return null
  return { itemName: specs.物品名称, weight: specs.重量 || '' }
}

/**
 * 从 task_specs 解析书本数量 (type=3 subType=32)
 * @returns {number|null}
 */
export function parseBookCountFromSpecs(specs) {
  if (!specs || !specs.书本数量) return null
  return specs.书本数量
}

/**
 * 从 task_specs 解析资料打印信息 (type=3 subType=31)
 * @returns {{ printType: string, printSide: string }|null}
 */
export function parsePrintSpecsFromSpecs(specs) {
  if (!specs || (!specs.打印类型 && !specs.打印方式)) return null
  return {
    printType: specs.打印类型 || '',
    printSide: specs.打印方式 || ''
  }
}

/**
 * 从 task_specs 解析商家信息 (type=2 subType=23)
 * @returns {string|null}
 */
export function parseMerchantInfoFromSpecs(specs) {
  if (!specs || !specs.商家信息) return null
  return specs.商家信息
}

/**
 * 从 task_specs 解析额外费用 (type=5 通用代办)
 * @returns {number|null}
 */
export function parseExtraFeeFromSpecs(specs) {
  if (!specs || specs.额外费用 == null) return null
  return Number(specs.额外费用) || null
}
