<template>
  <view class="page">
    <uni-nav-bar backgroundColor="#FAFAF8" :border="false" statusBar>
      <template v-slot:left>
        <view class="nav-avatar-wrap">
          <view class="nav-avatar">
            <image v-if="store.userInfo.avatarUrl" class="nav-avatar-img" :src="store.userInfo.avatarUrl" mode="aspectFill" />
            <text v-else class="nav-avatar-text">{{ store.avatarText }}</text>
          </view>
        </view>
      </template>
      <view class="nav-title-wrap"><text class="nav-title-text">订单</text></view>
      <template v-slot:right>
        <view class="nav-btn" @click="goMessages">
          <iconpark-icon name="notification-filled" size="24" color="#FF6B4A" />
          <view v-if="unreadCount > 0" class="nav-notify-dot"></view>
        </view>
      </template>
    </uni-nav-bar>

    <view class="tab-section">
      <view v-if="store.isCertifiedRunner" class="role-tabs">
        <view class="role-tab" :class="{ 'role-tab--active': activeRole === 'publisher' }" @click="activeRole = 'publisher'; page = 1; list = []; fetchList()">
          <text>我的任务</text>
        </view>
        <view class="role-tab" :class="{ 'role-tab--active': activeRole === 'runner' }" @click="activeRole = 'runner'; page = 1; list = []; fetchList()">
          <text>我的接单</text>
        </view>
      </view>
      <view class="status-filter-wrap">
        <scroll-view class="status-filter" scroll-x enhanced :show-scrollbar="false">
          <view v-for="f in currentFilters" :key="f.value" class="filter-chip" :class="{ 'filter-chip--active': activeStatus === f.value }" @click="filterStatus(f.value)">
            <text>{{ f.label }}</text>
          </view>
        </scroll-view>
      </view>
    </view>

    <scroll-view class="main-scroll" :style="{ height: scrollHeight + 'px' }" scroll-y enhanced :show-scrollbar="false" @scrolltolower="loadMore" refresher-enabled @refresherrefresh="onRefresh" :refresher-triggered="refreshing">
      <view v-if="loading && list.length === 0" class="loading-state">
        <text class="loading-text">加载中…</text>
      </view>

      <view v-else-if="list.length === 0" class="empty-state">
        <iconpark-icon name="list" size="48" color="#D4D2CC" />
        <text class="empty-text" v-if="activeRole === 'publisher'">暂无发布的任务</text>
        <text class="empty-text" v-else>暂无接单记录</text>
        <text class="empty-sub">试试调整筛选条件</text>
      </view>

      <view :key="'orders-' + listAnimKey" style="width:100%">
        <view v-for="(item, index) in list" :key="item.uniqueKey" class="order-card animate-fade-up" :style="{ animationDelay: (index * 0.06) + 's' }" @click="onItemTap(item)">
        <view class="order-header">
          <view class="order-type">
            <view class="type-icon" :class="'type-icon--' + item.iconStyle">
              <iconpark-icon :name="item.iconType" size="20" :color="item.iconColor" />
            </view>
            <view class="type-info">
              <text class="type-label">{{ item.title }}</text>
              <text class="type-time">{{ item.timeText }}</text>
            </view>
          </view>
          <view class="order-status-badge" :style="{ background: item.statusBadge.bg, color: item.statusBadge.color }">{{ item.statusBadge.text }}</view>
        </view>

        <view class="order-no-row" v-if="item.taskNo">
          <text class="order-no-label">订单号</text>
          <text class="order-no-value">{{ item.taskNo }}</text>
          <view class="copy-btn" @click.stop="copyOrderNo(item.taskNo)">
            <iconpark-icon name="copy" size="16" color="#FF6B4A" />
          </view>
        </view>

        <view class="package-tags" v-if="item.packageInfo">
          <text class="package-tag" v-for="(tag, ti) in item.packageInfo.sizes.split('，')" :key="ti">{{ tag }}</text>
        </view>
        <view class="package-tags" v-if="item.productTags.length">
          <text class="package-tag" v-for="(tag, ti) in item.productTags" :key="ti">{{ tag }}</text>
        </view>
        <view class="package-tags" v-if="item.serviceDuration">
          <text class="package-tag">服务时长: {{ item.serviceDuration.label }}</text>
        </view>

        <view class="package-tags" v-if="item.bookCount">
          <text class="package-tag">书本数量：{{ item.bookCount }}本</text>
        </view>

        <view class="order-desc" v-if="item.description">{{ item.description }}</view>

        <view class="order-route">
          <view class="route-segment" v-if="item.pickupAddress">
            <text class="route-badge route-badge--from">起</text>
            <text class="route-text">{{ item.pickupAddress }}</text>
          </view>
          <view class="route-segment" v-if="item.deliveryAddr">
            <text class="route-badge route-badge--to">终</text>
            <text class="route-text">{{ item.deliveryAddr }}</text>
          </view>
        </view>

        <view class="order-contact" v-if="item.isRunner && (item.contactName || item.contactPhone)">
          <text class="contact-label">联系人：</text>
          <text class="contact-value">{{ item.contactName || '--' }}</text>
          <text v-if="item.contactPhone" class="contact-phone">{{ item.contactPhone }}</text>
        </view>

        <view class="order-footer">
          <view class="footer-left">
            <text class="footer-price">¥{{ item.rewardText }}</text>
            <text v-if="item.productFee && item.type === 4" class="footer-product-fee">含商品费 ¥{{ item.productFee.toFixed(2) }}</text>
          </view>
          <view class="footer-actions" @click.stop>
            <view v-if="item.canCancel" class="action-btn action-btn--ghost" @click="onCancelTask(item)">
              <text>取消</text>
            </view>
          </view>
        </view>
      </view>
      </view>

      <uni-load-more v-if="list.length > 0" :status="loadMoreStatus" />

      <view class="bottom-placeholder"></view>
    </scroll-view>

    <custom-tabbar :selected="2" />
  </view>
</template>

<script setup>
import { ref, computed, onUnmounted, nextTick } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useStore } from '@/store/index.js'
import { taskApi, orderApi, notificationApi } from '@/api'
import { TASK_STATUS, ORDER_STATUS, ORDER_STATUS_BADGE, TASK_STATUS_BADGE, TASK_TYPE_META, getTaskTypeLabel, getTaskStatusLabel, TYPE_FROM_API, isQueueWaitType } from '@/utils/constants.js'
import { normalizeTaskCard } from '@/utils/task-normalizer.js'
import CustomTabbar from '@/components/custom-tabbar/custom-tabbar.vue'

const store = useStore()
const sysInfo = uni.getSystemInfoSync()
const scrollHeight = sysInfo.windowHeight - sysInfo.statusBarHeight - 44 - 90 - 60

const activeRole = ref('publisher')
const activeStatus = ref('')
const list = ref([])
const page = ref(1)
const hasMore = ref(true)
const listAnimKey = ref(0)
const loading = ref(false)
const refreshing = ref(false)
const unreadCount = ref(0)

const publisherFilters = [
  { value: '', label: '全部' },
  { value: 1, label: '待接单' },
  { value: 2, label: '已接单' },
  { value: 3, label: '配送中' },
  { value: 4, label: '待确认' },
  { value: 5, label: '已完成' },
  { value: 6, label: '已取消' },
  { value: 7, label: '待评价' }
]

const runnerFilters = [
  { value: '', label: '全部' },
  { value: 1, label: '待取货' },
  { value: 2, label: '待送达' },
  { value: 3, label: '待确认' },
  { value: 4, label: '已完成' },
  { value: 5, label: '已取消' }
]

const currentFilters = computed(() => activeRole.value === 'publisher' ? publisherFilters : runnerFilters)
const loadMoreStatus = computed(() => {
  if (loading.value) return 'loading'
  if (!hasMore.value) return 'noMore'
  return 'more'
})

async function loadUnread() {
  try {
    const data = await notificationApi.getNotifications({ isRead: 0, page: 1, size: 1 }, { showLoading: false })
    unreadCount.value = data?.total || 0
  } catch (e) { /* ignore */ }
}

function filterStatus(status) {
  activeStatus.value = status
  page.value = 1
  list.value = []
  fetchList()
}

async function fetchList() {
  loading.value = true
  try {
    let res
    if (activeRole.value === 'publisher') {
      const params = { page: page.value, size: 10 }
      // 待评价 = 已完成 + 未评价
      if (activeStatus.value === 7) params.status = 5
      else if (activeStatus.value) params.status = activeStatus.value
      res = await taskApi.getMyTasks(params, { showLoading: false })
    } else {
      const params = { page: page.value, size: 10 }
      if (activeStatus.value) params.status = activeStatus.value
      res = await orderApi.getMyOrders(params, { showLoading: false })
    }
    let records = (res && res.records ? res.records : []).map(normalizeItem)
    // 待评价：过滤掉已评价的
    if (activeRole.value === 'publisher' && activeStatus.value === 7) {
      records = records.filter(item => !item.hasReviewed)
    }
    if (page.value === 1) {
      list.value = records
    } else {
      list.value.push(...records)
    }
    hasMore.value = records.length >= 10
  } catch (e) {
    console.error('fetchList error:', e)
    if (page.value === 1) list.value = []
  } finally {
    loading.value = false
    refreshing.value = false
  }
}

function extractOrderStatus(raw) {
  const candidates = ['orderStatus', 'order_status', 'status', 'taskStatus', 'task_status']
  for (const key of candidates) {
    if (raw[key] != null) {
      const n = parseInt(raw[key], 10)
      if (n >= 1 && n <= 5) return n
    }
  }
  for (const key of Object.keys(raw)) {
    if (/status/i.test(key) && raw[key] != null) {
      const n = parseInt(raw[key], 10)
      if (n >= 1 && n <= 5) return n
    }
  }
  return 0
}

function normalizeItem(raw) {
  if (activeRole.value === 'publisher') {
    const taskStatus = raw.status || 1
    const statusBadge = TASK_STATUS_BADGE[taskStatus] || { text: getTaskStatusLabel(taskStatus), type: 'default', bg: '#f2f3fc', color: '#424753' }

    const card = normalizeTaskCard(raw, { role: 'publisher', isOwner: true })
    return {
      ...card,
      uniqueKey: `task-${raw.taskId || raw.id || Math.random()}`,
      taskId: card.taskId,
      status: taskStatus,
      deliveryAddr: raw.deliveryAddress || '',
      timeText: (raw.publishTime || '').replace('T', ' ').slice(0, 16),
      statusBadge,
      canCancel: taskStatus === 1,
      hasReviewed: raw.hasReviewed || false,
      isPublisher: true
    }
  } else {
    const orderStatus = extractOrderStatus(raw)
    const statusBadge = { ...(ORDER_STATUS_BADGE[orderStatus] || { text: '未知', type: 'default', bg: '#f2f3fc', color: '#424753' }) }
    const orderSubType = raw.subType || raw.sub_type
    if (isQueueWaitType(orderSubType)) {
      if (orderStatus === 1) statusBadge.text = '待到达'
      else if (orderStatus === 2) statusBadge.text = '代办中'
    }

    const card = normalizeTaskCard(raw, { role: 'runner', isOwner: false })
    return {
      ...card,
      uniqueKey: `order-${raw.orderId || raw.order_id || Math.random()}`,
      orderId: raw.orderId || raw.order_id,
      taskId: card.taskId,
      status: orderStatus,
      deliveryAddr: raw.deliveryAddress || '',
      timeText: orderStatus === 5 ? '' : ((raw.acceptTime || '').replace('T', ' ').slice(0, 16)),
      statusBadge,
      canCancel: false,
      isRunner: true
    }
  }
}

function copyOrderNo(no) {
  uni.setClipboardData({ data: no, success: () => uni.showToast({ title: '已复制', icon: 'none' }) })
}

function onItemTap(item) {
  if (item.isPublisher) {
    if (item.status === 5 || item.status === 6) {
      uni.navigateTo({ url: `/pages/order-completed/order-completed?taskId=${item.taskId}` })
    } else if (item.status >= 2) {
      uni.navigateTo({ url: `/pages/order-delivering/order-delivering?taskId=${item.taskId}&role=publisher` })
    } else {
      uni.navigateTo({ url: `/pages/order-waiting/order-waiting?taskId=${item.taskId}` })
    }
  } else {
    if (item.status === 4 || item.status === 5) {
      uni.navigateTo({ url: `/pages/order-completed/order-completed?orderId=${item.orderId}` })
    } else {
      uni.navigateTo({ url: `/pages/order-delivering/order-delivering?orderId=${item.orderId}&role=runner` })
    }
  }
}

const cancelTarget = ref(null)

function onCancelTask(item) {
  cancelTarget.value = item
  uni.$on('cancelReasonSelected', async (reason) => {
    uni.$off('cancelReasonSelected')
    const target = cancelTarget.value
    cancelTarget.value = null
    if (!target) return
    try {
      await taskApi.cancelTask(target.taskId, reason)
      uni.showToast({ title: '已取消', icon: 'success' })
      target.status = 6
      target.statusBadge = { text: '已取消', type: 'info', bg: '#F5F5F0', color: '#5E5D58' }
      target.canCancel = false
    } catch (e) { /* handled */ }
  })
  uni.navigateTo({ url: '/pages/cancel-reason/cancel-reason' })
}

onUnmounted(() => { uni.$off('cancelReasonSelected') })

function goMessages() { uni.switchTab({ url: '/pages/message/message' }) }

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

onShow(async () => {
  loadUnread()
  page.value = 1
  listAnimKey.value++
  await nextTick()
  fetchList()
})

loadUnread()
fetchList()
</script>

<style scoped>
.page { width: 100%; height: 100vh; display: flex; flex-direction: column; background: var(--background); overflow: hidden; }

.nav-avatar-wrap { width: 68rpx; height: 68rpx; border-radius: 50%; background: var(--primary-container); display: flex; align-items: center; justify-content: center; }
.nav-avatar { width: 56rpx; height: 56rpx; border-radius: 50%; background: var(--primary); display: flex; align-items: center; justify-content: center; }
.nav-avatar-img { width: 100%; height: 100%; border-radius: 50%; object-fit: cover; }
.nav-avatar-text { font-size: 26rpx; font-weight: 700; color: #fff; }
.nav-title-wrap { display: flex; align-items: center; justify-content: center; }
.nav-title-text { font-size: 34rpx; font-weight: 600; color: var(--text-primary); }
.nav-btn { width: 68rpx; height: 68rpx; display: flex; align-items: center; justify-content: center; border-radius: 50%; position: relative; }
.nav-btn:active { background: var(--primary-container); }
.nav-notify-dot { position: absolute; top: 14rpx; right: 14rpx; width: 14rpx; height: 14rpx; background: var(--error); border-radius: 50%; border: 2rpx solid var(--background); }

.tab-section { padding: 0 32rpx; background: var(--background); flex-shrink: 0; }
.role-tabs { display: flex; gap: 0; background: var(--surface); border-radius: 24rpx; padding: 6rpx; margin-bottom: 16rpx; }
.role-tab { flex: 1; text-align: center; padding: 16rpx 0; border-radius: 20rpx; font-size: 28rpx; font-weight: 500; color: var(--text-secondary); transition: all var(--duration-fast) var(--easing-out); }
.role-tab--active { background: var(--primary); color: #fff; font-weight: 600; box-shadow: var(--shadow-primary); }
.status-filter-wrap { position: relative; }
.status-filter-wrap::after { content: ''; position: absolute; right: 0; top: 0; bottom: 0; width: 48rpx; background: linear-gradient(to right, transparent, var(--background)); pointer-events: none; }
.status-filter { white-space: nowrap; padding-bottom: 16rpx; padding-right: 24rpx; }
.filter-chip { display: inline-block; padding: 10rpx 22rpx; border-radius: 48rpx; background: var(--surface-raised); border: 1rpx solid var(--outline-light); margin-right: 12rpx; font-size: 24rpx; color: var(--text-secondary); transition: all var(--duration-fast) var(--easing-out); }
.filter-chip--active { background: var(--primary-container); border-color: var(--primary); color: var(--primary); font-weight: 500; }

.main-scroll { box-sizing: border-box; width: 100%; padding: 8rpx 32rpx 0; }

.order-card { background: var(--surface-raised); border-radius: var(--radius-card); padding: 28rpx; margin-bottom: 24rpx; box-shadow: var(--shadow-sm); border: 1rpx solid var(--outline-light); position: relative; overflow: hidden; }
.order-card::before { content: ''; position: absolute; left: 0; top: 0; bottom: 0; width: 6rpx; background: var(--primary); border-radius: 0 3rpx 3rpx 0; }
.order-card:active { transform: scale(0.98); }

.order-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 20rpx; }
.order-type { display: flex; align-items: center; gap: 14rpx; }
.type-icon { width: 64rpx; height: 64rpx; border-radius: 18rpx; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.type-icon--blue { background: var(--primary-container); }
.type-icon--orange { background: var(--accent-container); }
.type-icon--green { background: var(--secondary-container); }
.type-icon--teal { background: linear-gradient(135deg, #e0f2fe, #bae6fd); }
.type-info { display: flex; flex-direction: column; }
.type-label { font-size: 30rpx; font-weight: 600; color: var(--text-primary); }
.type-time { font-size: 22rpx; color: var(--text-secondary); margin-top: 2rpx; }
.order-status-badge { padding: 8rpx 18rpx; border-radius: 20rpx; font-size: 22rpx; font-weight: 500; flex-shrink: 0; }

.order-no-row { display: flex; align-items: center; gap: 12rpx; margin-bottom: 14rpx; padding: 10rpx 16rpx; background: var(--surface); border-radius: 10rpx; }
.order-no-label { font-size: 22rpx; color: var(--text-tertiary); }
.order-no-value { font-size: 24rpx; color: var(--text-secondary); flex: 1; }
.copy-btn { width: 44rpx; height: 44rpx; display: flex; align-items: center; justify-content: center; border-radius: 10rpx; }
.copy-btn:active { background: var(--primary-container); }

.package-tags { display: flex; flex-wrap: wrap; gap: 8rpx; margin-bottom: 12rpx; }
.package-tag { padding: 6rpx 16rpx; background: var(--primary-container); border-radius: 8rpx; font-size: 22rpx; font-weight: 500; color: var(--primary); }

.order-desc { font-size: 26rpx; color: var(--text-primary); line-height: 1.5; margin-bottom: 12rpx; display: -webkit-box; -webkit-line-clamp: 2; line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }

.order-route { background: var(--surface); border-radius: 14rpx; padding: 14rpx 18rpx; margin-bottom: 16rpx; display: flex; flex-direction: column; gap: 10rpx; }
.route-segment { display: flex; align-items: center; gap: 10rpx; }
.route-badge { width: 40rpx; height: 40rpx; border-radius: 10rpx; display: flex; align-items: center; justify-content: center; font-size: 22rpx; font-weight: 700; color: #fff; flex-shrink: 0; }
.route-badge--from { background: var(--text-primary); }
.route-badge--to { background: var(--primary); }
.route-text { font-size: 24rpx; color: var(--text-secondary); flex: 1; }

.order-contact { display: flex; align-items: center; gap: 8rpx; margin-bottom: 14rpx; padding: 10rpx 16rpx; background: var(--surface); border-radius: 10rpx; }
.contact-label { font-size: 22rpx; color: var(--text-tertiary); }
.contact-value { font-size: 24rpx; color: var(--text-primary); font-weight: 500; }
.contact-phone { font-size: 24rpx; color: var(--primary); margin-left: 12rpx; }

.order-footer { display: flex; justify-content: space-between; align-items: center; }
.footer-left { display: flex; flex-direction: column; }
.footer-price { font-size: 34rpx; font-weight: 700; color: var(--primary); }
.footer-product-fee { font-size: 20rpx; color: var(--text-secondary); margin-top: 2rpx; }
.footer-actions { display: flex; gap: 16rpx; }
.action-btn { padding: 12rpx 24rpx; border-radius: 20rpx; font-size: 24rpx; font-weight: 500; transition: transform var(--duration-fast) var(--easing-out); }
.action-btn:active { transform: scale(0.95); }
.action-btn--ghost { border: 1rpx solid var(--outline); color: var(--text-secondary); }

.loading-state, .empty-state { display: flex; flex-direction: column; align-items: center; padding: 120rpx 0; gap: 12rpx; opacity: 0.5; }
.loading-text, .empty-text { font-size: 28rpx; color: var(--text-tertiary); }
.empty-sub { font-size: 24rpx; color: var(--text-tertiary); }
.bottom-placeholder { height: 200rpx; }
</style>
