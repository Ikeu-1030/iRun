<template>
  <view class="page">
    <uni-nav-bar backgroundColor="#f9f9ff" :border="false" statusBar>
      <template v-slot:left>
        <view class="nav-avatar-wrap">
          <view class="nav-avatar">
            <image v-if="store.userInfo.avatarUrl" class="nav-avatar-img" :src="store.userInfo.avatarUrl" mode="aspectFill" />
            <text v-else class="nav-avatar-text">{{ store.avatarText }}</text>
          </view>
        </view>
      </template>
      <view class="nav-title-wrap"><text class="nav-title-text">任务大厅</text></view>
      <template v-slot:right>
        <view class="nav-btn" @click="onRefresh">
          <iconpark-icon name="refresh" size="22" color="#FF6B4A" />
        </view>
      </template>
    </uni-nav-bar>

    <!-- 搜索栏 -->
    <view class="search-bar">
      <view class="search-input-wrap">
        <iconpark-icon name="search" size="18" color="#737784" />
        <input class="search-input" v-model="searchKeyword" placeholder="搜索任务描述…" confirm-name="search" @confirm="onSearch" />
        <iconpark-icon v-if="searchKeyword" name="clear" size="18" color="#c2c6d5" @click="clearSearch" />
      </view>
      <view class="filter-btn" :class="{ 'filter-btn--active': hasActiveFilters }" @click="openFilterPanel">
        <iconpark-icon name="bars" size="20" :color="hasActiveFilters ? '#fff' : '#FF6B4A'" />
      </view>
    </view>

    <!-- 筛选栏 -->
    <view class="filter-section">
      <scroll-view class="filter-scroll" scroll-x enhanced :show-scrollbar="false">
        <view v-for="f in typeFilters" :key="f.value" class="filter-chip" :class="{ 'filter-chip--active': activeType === f.value }" @click="filterType(f.value)">
          <text>{{ f.label }}</text>
        </view>
      </scroll-view>
    </view>

    <!-- 活跃筛选标签 -->
    <view class="active-filters" v-if="hasActiveFilters || searchKeyword">
      <view class="active-tag" v-if="searchKeyword" @click="clearSearch">
        <text>搜索: {{ searchKeyword }}</text>
        <iconpark-icon name="closeempty" size="14" color="#FF6B4A" />
      </view>
      <view class="active-tag" v-for="tag in activeFilterTags" :key="tag.key" @click="removeFilter(tag.key)">
        <text>{{ tag.label }}</text>
        <iconpark-icon name="closeempty" size="14" color="#FF6B4A" />
      </view>
    </view>

    <!-- 任务列表 -->
    <scroll-view class="main-scroll" :style="{ height: scrollHeight + 'px' }" scroll-y enhanced :show-scrollbar="false" @scrolltolower="loadMore" refresher-enabled @refresherrefresh="onRefresh" :refresher-triggered="refreshing">
      <view v-if="loading && list.length === 0" class="loading-state">
        <text class="loading-text">加载中…</text>
      </view>

      <view v-else-if="list.length === 0" class="empty-state">
        <iconpark-icon name="search" size="48" color="#c2c6d5" class="animate-bounce-in" />
        <text class="empty-text">暂无可用任务</text>
        <text class="empty-sub">试试调整筛选条件</text>
      </view>

      <view v-for="(item, index) in list" :key="item.uniqueKey" class="task-card animate-fade-up" :style="{ animationDelay: (index * 0.06) + 's' }" @click="onItemTap(item)">
        <view class="task-header">
          <view class="task-type">
            <view class="type-icon" :class="'type-icon--' + item.iconStyle">
              <iconpark-icon :name="item.iconType" size="20" :color="item.iconColor" />
            </view>
            <view class="type-info">
              <text class="type-label">{{ item.title }}</text>
              <text class="type-time">{{ item.publishTime }}</text>
            </view>
          </view>
          <view class="task-reward-wrap">
            <text class="task-reward">¥{{ item.rewardText }}</text>
            <text v-if="item.productFee" class="task-product-fee">含商品费 ¥{{ item.productFee.toFixed(2) }}</text>
          </view>
        </view>

        <view class="task-no-row" v-if="item.taskNo">
          <text class="task-no-label">订单号</text>
          <text class="task-no-value">{{ item.taskNo }}</text>
          <view class="copy-btn" @click.stop="copyOrderNo(item.taskNo)">
            <iconpark-icon name="copy" size="16" color="#FF6B4A" />
          </view>
        </view>

        <view class="task-desc" v-if="item.description">{{ item.description }}</view>
        <view class="task-desc task-desc--hidden" v-else-if="!item.description && item.taskType === 1">
          <iconpark-icon name="locked-filled" size="14" color="#737784" />
          <text>取件信息已隐藏</text>
        </view>

        <!-- 代取快递包裹信息 -->
        <view class="package-tags" v-if="item.packageInfo">
          <text class="package-tag" v-for="(tag, ti) in item.packageInfo.sizes.split('，')" :key="ti">{{ tag }}</text>
        </view>

        <!-- 代购物品商品标签 -->
        <view class="package-tags" v-if="item.productTags.length">
          <text class="package-tag" v-for="(tag, ti) in item.productTags" :key="ti">{{ tag }}</text>
        </view>

        <!-- 办事代排服务时长 -->
        <view class="package-tags" v-if="item.serviceDuration">
          <text class="package-tag">服务时长: {{ item.serviceDuration.label }}</text>
        </view>

        <!-- 物品急送信息（橙色药丸标签） -->
        <view class="item-express-tags" v-if="item.itemExpress">
          <text class="item-express-tag">{{ item.itemExpress.itemName }}</text>
          <text class="item-express-tag item-express-tag--weight">{{ item.itemExpress.weight }}</text>
        </view>

        <!-- 图书馆还书书本数量 -->
        <view class="package-tags" v-if="item.bookCount">
          <text class="package-tag">书本数量：{{ item.bookCount }}本</text>
        </view>

        <!-- 资料打印信息 -->
        <view class="package-tags" v-if="item.printSpecs">
          <text class="package-tag">{{ item.printSpecs.printType }}</text>
          <text class="package-tag">{{ item.printSpecs.printSide }}</text>
        </view>

        <!-- 奶茶咖啡代取商家信息 -->
        <view class="package-tags" v-if="item.merchantTag">
          <text class="package-tag">商家：{{ item.merchantTag }}</text>
        </view>

        <view class="task-route">
          <view class="route-segment" v-if="item.pickupAddress">
            <text class="route-badge route-badge--from">起</text>
            <text class="route-text">{{ item.pickupAddress }}</text>
          </view>
          <view class="route-segment" v-if="item.deliveryAddr">
            <text class="route-badge route-badge--to">终</text>
            <text class="route-text">{{ item.deliveryAddr }}</text>
          </view>
        </view>

        <view class="task-footer">
          <view class="publisher-info">
            <image v-if="item.publisherAvatar" class="pub-avatar-img" :src="normalizeUrl(item.publisherAvatar)" mode="aspectFill" />
            <view v-else class="pub-avatar">{{ item.publisherInitial }}</view>
            <text class="pub-name">{{ item.publisherNickname }}</text>
            <text class="pub-time">{{ item.timeText }}</text>
          </view>
          <view class="accept-btn" :class="{ 'accept-btn--disabled': accepting }" @click.stop="onAccept(item)" v-if="item.canAccept && store.isCertifiedRunner">
            <text>{{ accepting ? '接单中…' : '接单' }}</text>
          </view>
          <view class="accept-btn accept-btn--lock" @click.stop="onAcceptBlocked(item)" v-else-if="item.canAccept && !store.isCertifiedRunner">
            <iconpark-icon name="locked-filled" size="14" color="#fff" />
            <text v-if="!store.isCertified">认证后接单</text>
            <text v-else>申请成为配送员</text>
          </view>
          <view class="self-tag" v-else-if="!item.canAccept && item.publisherId === store.userId">
            <text>我的发布</text>
          </view>
        </view>
      </view>

      <uni-load-more v-if="list.length > 0" :status="loadMoreStatus" />
      <view class="bottom-placeholder"></view>
    </scroll-view>

    <!-- 筛选面板遮罩 -->
    <view class="filter-overlay" v-if="showFilterPanel" @click="closeFilterPanel">
      <view class="filter-panel" :class="{ 'filter-panel--morph': panelMorph === 'opening', 'filter-panel--unmorph': panelMorph === 'closing' }" :style="{ '--morph-offset': panelOffset }" @click.stop>
        <view class="filter-panel-header">
          <text class="filter-panel-title">高级筛选</text>
          <view class="filter-panel-close" @click="closeFilterPanel">
            <iconpark-icon name="close" size="20" color="#737784" />
          </view>
        </view>

        <view class="filter-panel-body">
          <view class="filter-field">
            <text class="filter-label">取件地址</text>
            <input class="filter-input" v-model="filterPickupAddress" placeholder="输入取件地址关键词" />
          </view>
          <view class="filter-field">
            <text class="filter-label">送达地址</text>
            <input class="filter-input" v-model="filterDeliveryAddress" placeholder="输入送达地址关键词" />
          </view>
          <view class="filter-field">
            <text class="filter-label">性别要求</text>
            <view class="filter-chips">
              <view class="filter-chip-sm" :class="{ 'filter-chip-sm--active': !filterRequireSex }" @click="filterRequireSex = ''"><text>不限</text></view>
              <view class="filter-chip-sm" :class="{ 'filter-chip-sm--active': filterRequireSex === '男' }" @click="filterRequireSex = '男'"><text>男</text></view>
              <view class="filter-chip-sm" :class="{ 'filter-chip-sm--active': filterRequireSex === '女' }" @click="filterRequireSex = '女'"><text>女</text></view>
            </view>
          </view>
          <view class="filter-field">
            <text class="filter-label">报酬范围</text>
            <view class="filter-reward-row">
              <input class="filter-input filter-input--half" v-model="filterMinReward" name="digit" placeholder="最低 ¥" />
              <text class="filter-reward-sep">—</text>
              <input class="filter-input filter-input--half" v-model="filterMaxReward" name="digit" placeholder="最高 ¥" />
            </view>
          </view>
        </view>

        <view class="filter-panel-footer safe-area-bottom">
          <view class="filter-reset-btn" @click="resetFilters"><text>重置</text></view>
          <view class="filter-apply-btn" @click="applyFilters"><text>确定</text></view>
        </view>
      </view>
    </view>

    <custom-tabbar :selected="1" />
  </view>
</template>

<script setup>
import { ref, computed, nextTick } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useStore } from '@/store/index.js'
import { taskApi, orderApi } from '@/api'
import { TASK_TYPE_META, getTaskTypeLabel, TYPE_TO_API, TYPE_FROM_API, subTypeToCode } from '@/utils/constants.js'
import { parseDeliveryAddress } from '@/utils/campus-data.js'
import { normalizeTaskCard } from '@/utils/task-normalizer.js'
import CustomTabbar from '@/components/custom-tabbar/custom-tabbar.vue'
import { SERVER_ORIGIN } from '@/utils/config'

function normalizeUrl(url) {
  if (!url) return ''
  if (url.startsWith('http')) return url
  if (url.startsWith('/')) return SERVER_ORIGIN + url
  return url
}

const store = useStore()
const sysInfo = uni.getSystemInfoSync()
const scrollHeight = sysInfo.windowHeight - sysInfo.statusBarHeight - 44 - 72 - 44 - 120

const activeType = ref(0)
const list = ref([])
const page = ref(1)
const hasMore = ref(true)
const loading = ref(false)
const refreshing = ref(false)
const accepting = ref(false)

// 搜索与筛选
const searchKeyword = ref('')
const showFilterPanel = ref(false)
const panelMorph = ref('') // '' | 'opening' | 'closing'
const panelOffset = ref('translate(0,0)')

function openFilterPanel() {
  const query = uni.createSelectorQuery()
  query.select('.filter-btn').boundingClientRect()
  query.exec((res) => {
    const rect = res[0]
    if (rect) {
      const btnCX = rect.left + rect.width / 2
      const btnCY = rect.top + rect.height / 2
      const vw = sysInfo.windowWidth
      const vh = sysInfo.windowHeight
      const dx = btnCX - vw / 2
      const dy = btnCY - vh / 2
      panelOffset.value = `translate(${dx}px, ${dy}px)`
    }
    showFilterPanel.value = true
    panelMorph.value = 'opening'
    nextTick(() => { panelMorph.value = '' })
  })
}

function closeFilterPanel() {
  panelMorph.value = 'closing'
  setTimeout(() => {
    showFilterPanel.value = false
    panelMorph.value = ''
  }, 400)
}
const filterPickupAddress = ref('')
const filterDeliveryAddress = ref('')
const filterRequireSex = ref('')
const filterMinReward = ref('')
const filterMaxReward = ref('')

const typeFilters = [
  { value: 0, label: '全部' },
  { value: 1, label: '代取快递' },
  { value: 2, label: '代拿餐食' },
  { value: 3, label: '校内代办' },
  { value: 4, label: '代购物品' },
  { value: 5, label: '通用代办' }
]

const loadMoreStatus = computed(() => {
  if (loading.value) return 'loading'
  if (!hasMore.value) return 'noMore'
  return 'more'
})

const hasActiveFilters = computed(() =>
  !!(filterPickupAddress.value || filterDeliveryAddress.value || filterRequireSex.value || filterMinReward.value || filterMaxReward.value)
)

const activeFilterTags = computed(() => {
  const tags = []
  if (filterPickupAddress.value) tags.push({ key: 'pickup', label: `取件: ${filterPickupAddress.value}` })
  if (filterDeliveryAddress.value) tags.push({ key: 'delivery', label: `送达: ${filterDeliveryAddress.value}` })
  if (filterRequireSex.value) tags.push({ key: 'sex', label: `要求: ${filterRequireSex.value}` })
  if (filterMinReward.value) tags.push({ key: 'min', label: `≥¥${filterMinReward.value}` })
  if (filterMaxReward.value) tags.push({ key: 'max', label: `≤¥${filterMaxReward.value}` })
  return tags
})

onShow(() => {
  fetchList()
})

function filterType(type) {
  activeType.value = type
  page.value = 1
  list.value = []
  fetchList()
}

async function fetchList() {
  loading.value = true
  try {
    const params = { page: page.value, size: 10 }
    if (activeType.value) params.type = TYPE_TO_API[activeType.value]

    let res
    if (searchKeyword.value) {
      params.keyword = searchKeyword.value
      if (filterMinReward.value) params.minReward = filterMinReward.value
      if (filterMaxReward.value) params.maxReward = filterMaxReward.value
      res = await taskApi.searchTasks(params)
    } else if (hasActiveFilters.value) {
      if (filterPickupAddress.value) params.pickupAddress = filterPickupAddress.value
      if (filterDeliveryAddress.value) params.deliveryAddress = filterDeliveryAddress.value
      if (filterRequireSex.value) params.requireSex = filterRequireSex.value
      if (filterMinReward.value) params.minReward = filterMinReward.value
      if (filterMaxReward.value) params.maxReward = filterMaxReward.value
      res = await taskApi.filterTasks(params)
    } else {
      res = await taskApi.getTaskList(params)
    }

    const records = (res && res.records ? res.records : []).map(normalizeItem)
    if (page.value === 1) {
      list.value = records
    } else {
      list.value.push(...records)
    }
    hasMore.value = records.length >= 10
  } catch (e) {
    if (page.value === 1) list.value = []
  }
  loading.value = false
  refreshing.value = false
}

function normalizeItem(raw) {
  const pubId = raw.publisherId || raw.publisher_id
  const isOwner = pubId === store.userId

  const card = normalizeTaskCard(raw, { role: 'viewer', isOwner })
  // type=1 回退旧 description 格式
  if (card.type === 1 && !card.packageInfo) {
    card.packageInfo = parsePackageInfo(raw.description || '', card.type)
    if (card.packageInfo) card.description = isOwner ? (raw.description || '') : ''
  }

  return {
    ...card,
    uniqueKey: `task-${raw.taskId || raw.id || Math.random()}`,
    taskId: card.taskId,
    publisherId: pubId,
    taskType: card.type,
    deliveryAddr: parseDeliveryDisplay(raw.deliveryAddress || '', isOwner),
    publisherNickname: raw.publisherNickname || raw.publisher_nickname || '未知',
    publisherAvatar: raw.publisherAvatar || raw.publisher_avatar || '',
    publisherInitial: (raw.publisherNickname || raw.publisher_nickname || '未').charAt(0),
    publishTime: (raw.publishTime || raw.publish_time || '').replace('T', ' ').slice(0, 16),
    timeText: formatPublishTime(raw.publishTime || raw.publish_time),
    canAccept: pubId ? pubId !== store.userId : true
  }
}

function parsePackageInfo(desc, taskType) {
  if (taskType !== 1 || !desc || !desc.startsWith('快递规格：')) return null
  const idx = desc.indexOf('；备注描述：')
  if (idx === -1) return null
  const sizesStr = desc.substring('快递规格：'.length, idx)
  let innerDesc = desc.substring(idx + '；备注描述：'.length)
  if (innerDesc === 'null') innerDesc = ''
  return { sizes: sizesStr, description: innerDesc }
}

function parseDeliveryDisplay(deliveryAddress, isOwner) {
  if (!deliveryAddress) return ''
  if (isOwner) return deliveryAddress
  // 非发布者仅看地点（不含楼层和详细地址）
  const parsed = parseDeliveryAddress(deliveryAddress)
  return parsed.location || deliveryAddress
}

function formatPublishTime(timeStr) {
  if (!timeStr) return ''
  const now = new Date()
  const time = new Date(timeStr.replace(/-/g, '/'))
  const diff = now - time
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
  if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前'
  const parts = timeStr.split(' ')
  return (parts[0] || timeStr).slice(5)
}

function onAcceptBlocked(item) {
  if (!store.isCertified) {
    uni.navigateTo({ url: '/pages/certify/certify' })
  } else if (!store.isCertifiedRunner) {
    uni.navigateTo({ url: '/pages/rider-cert/rider-cert' })
  }
}

async function onAccept(item) {
  if (accepting.value) return
  const res = await new Promise(r => {
    uni.showModal({ title: '确认接单', content: `确定要接「${item.title}」这个任务吗？报酬 ¥${item.rewardText}`, success: r2 => r(r2.confirm) })
  })
  if (!res) return
  accepting.value = true
  try {
    await orderApi.acceptOrder(item.taskId)
    uni.showToast({ title: '接单成功', icon: 'success' })
    const idx = list.value.indexOf(item)
    if (idx > -1) list.value.splice(idx, 1)
    uni.navigateTo({ url: `/pages/order-delivering/order-delivering?taskId=${item.taskId}&role=runner` })
  } catch (e) { /* handled */ }
  accepting.value = false
}

function onItemTap(item) {
  // 非发布者浏览时不以runner身份进入（隐私信息仅订单双方可见）
  uni.navigateTo({ url: `/pages/order-waiting/order-waiting?taskId=${item.taskId}` })
}

function loadMore() {
  if (!hasMore.value || loading.value) return
  page.value++
  fetchList()
}

function onRefresh() {
  refreshing.value = true
  page.value = 1
  fetchList()
}

// ---- 搜索与筛选 ----
function onSearch() {
  page.value = 1
  list.value = []
  fetchList()
}

function clearSearch() {
  searchKeyword.value = ''
  page.value = 1
  list.value = []
  fetchList()
}

function removeFilter(key) {
  switch (key) {
    case 'pickup': filterPickupAddress.value = ''; break
    case 'delivery': filterDeliveryAddress.value = ''; break
    case 'sex': filterRequireSex.value = ''; break
    case 'min': filterMinReward.value = ''; break
    case 'max': filterMaxReward.value = ''; break
  }
  page.value = 1
  list.value = []
  fetchList()
}

function resetFilters() {
  filterPickupAddress.value = ''
  filterDeliveryAddress.value = ''
  filterRequireSex.value = ''
  filterMinReward.value = ''
  filterMaxReward.value = ''
}

function applyFilters() {
  closeFilterPanel()
  page.value = 1
  list.value = []
  fetchList()
}

function copyOrderNo(no) {
  uni.setClipboardData({ data: no, success: () => uni.showToast({ title: '已复制', icon: 'success' }) })
}
</script>

<style scoped>
.page{width:100%;height:100vh;display:flex;flex-direction:column;background:var(--background);overflow:hidden}

.nav-avatar-wrap{width:68rpx;height:68rpx;border-radius:50%;background:var(--primary-container);display:flex;align-items:center;justify-content:center}
.nav-avatar{width:56rpx;height:56rpx;border-radius:50%;background:var(--primary);display:flex;align-items:center;justify-content:center}
.nav-avatar-img{width:100%;height:100%;border-radius:50%;object-fit:cover}
.nav-avatar-text{font-size:26rpx;font-weight:700;color:#fff}
.nav-title-wrap{display:flex;align-items:center;justify-content:center}
.nav-title-text{font-size:34rpx;font-weight:600;color:var(--text-primary)}
.nav-btn{width:68rpx;height:68rpx;display:flex;align-items:center;justify-content:center;border-radius:50%}
.nav-btn:active{background:var(--primary-container)}

.filter-section{padding:0 32rpx 16rpx;flex-shrink:0;background:var(--background)}
.filter-scroll{white-space:nowrap}
.filter-chip{display:inline-block;padding:10rpx 22rpx;border-radius:48rpx;background:var(--surface-raised);border:1rpx solid var(--outline-light);margin-right:12rpx;font-size:24rpx;color:var(--text-secondary);transition:all var(--duration-fast) var(--easing-out)}
.filter-chip--active{background:var(--primary);border-color:var(--primary);color:#fff;font-weight:500;box-shadow:var(--shadow-primary)}

.main-scroll{box-sizing:border-box;width:100%;padding:0 32rpx}

.task-card{background:var(--surface-raised);border-radius:var(--radius-card);padding:28rpx;margin-bottom:16rpx;box-shadow:var(--shadow-sm);transition:transform var(--duration-fast) var(--easing-out);position:relative;overflow:hidden}
.task-card::before{content:'';position:absolute;left:0;top:0;bottom:0;width:6rpx;border-radius:0 6rpx 6rpx 0}
.task-card:active{transform:scale(.98)}
.task-header{display:flex;justify-content:space-between;align-items:center;margin-bottom:16rpx}
.task-type{display:flex;align-items:center;gap:16rpx}
.type-icon{width:64rpx;height:64rpx;border-radius:18rpx;display:flex;align-items:center;justify-content:center}
.type-icon--blue{background:var(--primary-container)}
.type-icon--orange{background:var(--accent-container)}
.type-icon--green{background:var(--secondary-container)}
.type-icon--teal{background:linear-gradient(135deg,#e0f2fe,#bae6fd)}
.type-label{font-size:28rpx;font-weight:600;color:var(--text-primary);display:block}
.type-time{font-size:22rpx;color:var(--text-tertiary);margin-top:2rpx;display:block}
.type-sub{font-size:22rpx;color:var(--text-secondary);margin-top:2rpx;display:block}
.task-reward-wrap{display:flex;flex-direction:column;align-items:flex-end}
.task-reward{font-size:36rpx;font-weight:700;color:var(--primary)}
.task-product-fee{font-size:20rpx;color:var(--text-tertiary);margin-top:2rpx}

.task-no-row{display:flex;align-items:center;gap:12rpx;margin-bottom:10rpx}
.task-no-label{font-size:22rpx;color:var(--text-tertiary)}
.task-no-value{font-size:24rpx;font-weight:500;color:var(--text-secondary);letter-spacing:1rpx}
.copy-btn{width:48rpx;height:48rpx;border-radius:50%;background:var(--primary-container);display:flex;align-items:center;justify-content:center;flex-shrink:0;margin-left:4rpx}
.copy-btn:active{background:#FFD1C7}

.task-desc{font-size:26rpx;color:var(--text-primary);line-height:1.5;margin-bottom:12rpx;display:-webkit-box;-webkit-line-clamp:2;line-clamp:2;-webkit-box-orient:vertical;overflow:hidden}
.task-desc--hidden{display:flex;align-items:center;gap:6rpx;font-size:24rpx;color:var(--text-tertiary);-webkit-line-clamp:1;line-clamp:1}

.task-route{background:var(--surface);border-radius:14rpx;padding:14rpx 18rpx;margin-bottom:16rpx;display:flex;flex-direction:column;gap:10rpx}
.route-segment{display:flex;align-items:center;gap:10rpx}
.route-badge{width:40rpx;height:40rpx;border-radius:10rpx;display:flex;align-items:center;justify-content:center;font-size:22rpx;font-weight:700;color:#fff;flex-shrink:0}
.route-badge--from{background:var(--text-primary)}
.route-badge--to{background:var(--primary)}
.route-text{font-size:24rpx;color:var(--text-secondary);flex:1}

.task-footer{display:flex;justify-content:space-between;align-items:center}
.publisher-info{display:flex;align-items:center;gap:10rpx}
.pub-avatar{width:44rpx;height:44rpx;border-radius:50%;background:linear-gradient(135deg,var(--primary),var(--secondary));display:flex;align-items:center;justify-content:center;font-size:22rpx;font-weight:600;color:#fff;flex-shrink:0}
.pub-avatar-img{width:44rpx;height:44rpx;border-radius:50%;object-fit:cover;flex-shrink:0}
.package-tags{display:flex;flex-wrap:wrap;gap:8rpx;margin-bottom:12rpx}
.package-tag{padding:6rpx 16rpx;background:var(--primary-container);border-radius:8rpx;font-size:22rpx;font-weight:500;color:var(--primary)}
.item-express-tags{display:flex;flex-wrap:wrap;gap:8rpx;margin-bottom:12rpx}
.item-express-tag{padding:6rpx 16rpx;background:#fff7ed;border-radius:8rpx;font-size:22rpx;font-weight:500;color:#e67e22}
.item-express-tag--weight{background:#fff7ed;color:#ad6200}
.pub-name{font-size:24rpx;color:var(--text-secondary)}
.pub-time{font-size:22rpx;color:var(--text-tertiary)}

.accept-btn{padding:14rpx 32rpx;background:var(--primary);border-radius:36rpx;box-shadow:var(--shadow-primary);transition:transform var(--duration-fast) var(--easing-out)}
.accept-btn--disabled{background:var(--outline);pointer-events:none;box-shadow:none}
.accept-btn--lock{background:var(--text-tertiary);display:flex;align-items:center;gap:6rpx;box-shadow:none}
.accept-btn:active{transform:scale(.94)}
.accept-btn text{font-size:26rpx;font-weight:600;color:#fff}

.self-tag{padding:14rpx 24rpx;background:var(--surface);border-radius:36rpx}
.self-tag text{font-size:24rpx;color:var(--text-tertiary)}

.loading-state,.empty-state{display:flex;flex-direction:column;align-items:center;padding:120rpx 0;gap:12rpx;opacity:.5}
.loading-text,.empty-text{font-size:28rpx;color:var(--text-tertiary)}
.empty-sub{font-size:24rpx;color:var(--text-tertiary)}

.bottom-placeholder{height:200rpx}

/* 搜索栏 */
.search-bar{display:flex;align-items:center;padding:0 32rpx 16rpx;gap:16rpx;flex-shrink:0;background:var(--background)}
.search-input-wrap{flex:1;display:flex;align-items:center;height:72rpx;background:var(--surface-raised);border-radius:36rpx;padding:0 24rpx;gap:12rpx;box-shadow:var(--shadow-sm)}
.search-input{flex:1;font-size:26rpx;color:var(--text-primary);height:100%}
.filter-btn{width:72rpx;height:72rpx;border-radius:50%;background:var(--surface-raised);display:flex;align-items:center;justify-content:center;border:1rpx solid var(--outline-light);transition:all var(--duration-fast) var(--easing-out)}
.filter-btn--active{background:var(--primary);border-color:var(--primary);box-shadow:var(--shadow-primary)}
.filter-btn:active{transform:scale(.92)}

/* 活跃筛选标签 */
.active-filters{display:flex;flex-wrap:wrap;padding:0 32rpx 12rpx;gap:12rpx;flex-shrink:0;background:var(--background)}
.active-tag{display:flex;align-items:center;gap:6rpx;padding:8rpx 18rpx;background:var(--primary-container);border:1rpx solid rgba(255,107,74,.3);border-radius:32rpx}
.active-tag text{font-size:22rpx;color:var(--primary);font-weight:500}

/* 筛选面板 */
.filter-overlay{position:fixed;top:0;left:0;right:0;bottom:0;background:rgba(28,27,26,.45);z-index:1000;display:flex;align-items:center;justify-content:center;animation:fadeIn .25s var(--easing-out)}
@keyframes fadeIn{from{opacity:0}to{opacity:1}}
.filter-panel{width:85%;max-height:65vh;background:var(--surface-raised);border-radius:var(--radius-lg);display:flex;flex-direction:column;transition:transform .42s var(--ease-spring),border-radius .35s ease,opacity .3s ease}
.filter-panel--morph{transform:var(--morph-offset,translate(0,0)) scale(.05);border-radius:50%;opacity:0}
.filter-panel--unmorph{transform:var(--morph-offset,translate(0,0)) scale(.05);border-radius:50%;opacity:0}
.filter-panel-header{display:flex;justify-content:space-between;align-items:center;padding:28rpx 32rpx 0;flex-shrink:0}
.filter-panel-title{font-size:32rpx;font-weight:600;color:var(--text-primary)}
.filter-panel-close{width:56rpx;height:56rpx;display:flex;align-items:center;justify-content:center;border-radius:50%}
.filter-panel-close:active{background:var(--primary-container)}
.filter-panel-body{flex:1;overflow-y:auto;padding:20rpx 32rpx}
.filter-field{margin-bottom:24rpx}
.filter-label{font-size:26rpx;font-weight:500;color:var(--text-secondary);margin-bottom:12rpx;display:block}
.filter-input{height:72rpx;background:var(--surface);border-radius:16rpx;padding:0 24rpx;font-size:26rpx;color:var(--text-primary);width:100%;box-sizing:border-box}
.filter-input--half{width:45%}
.filter-chips{display:flex;gap:14rpx}
.filter-chip-sm{padding:14rpx 28rpx;border-radius:32rpx;background:var(--surface);font-size:26rpx;color:var(--text-secondary);transition:all var(--duration-fast) var(--easing-out)}
.filter-chip-sm--active{background:var(--primary);color:#fff;font-weight:500}
.filter-reward-row{display:flex;align-items:center;justify-content:space-between}
.filter-reward-sep{font-size:26rpx;color:var(--text-tertiary)}
.filter-panel-footer{display:flex;gap:20rpx;padding:16rpx 32rpx 24rpx;flex-shrink:0}
.filter-reset-btn{flex:1;height:80rpx;border-radius:40rpx;background:var(--surface);display:flex;align-items:center;justify-content:center}
.filter-reset-btn text{font-size:28rpx;color:var(--text-secondary);font-weight:500}
.filter-reset-btn:active{transform:scale(.96)}
.filter-apply-btn{flex:2;height:80rpx;border-radius:40rpx;background:var(--primary);display:flex;align-items:center;justify-content:center;box-shadow:var(--shadow-primary)}
.filter-apply-btn text{font-size:28rpx;color:#fff;font-weight:600}
.filter-apply-btn:active{transform:scale(.96)}
</style>
