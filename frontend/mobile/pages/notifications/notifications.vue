<template>
  <view class="page">
    <uni-nav-bar title="通知中心" backgroundColor="#FAFAF8" :border="false" statusBar fixed leftIcon="left" @clickLeft="onBack" color="#1C1B1A">
      <template v-slot:right>
        <view class="nav-actions">
          <view v-if="selectMode" class="nav-action" @click="exitSelectMode">
            <text>取消</text>
          </view>
          <template v-else>
            <view v-if="hasUnread" class="nav-action" @click="onMarkAllRead">
              <text>全部已读</text>
            </view>
            <view v-if="list.length > 0" class="nav-action" @click="enterSelectMode">
              <text>管理</text>
            </view>
          </template>
        </view>
      </template>
    </uni-nav-bar>

    <!-- 类型切换 -->
    <view class="tab-section" v-if="!selectMode">
      <view class="type-tabs">
        <view class="type-tab" :class="{ 'type-tab--active': activeType === '' }" @click="switchType('')">
          <text>全部</text>
        </view>
        <view class="type-tab" :class="{ 'type-tab--active': activeType === 1 }" @click="switchType(1)">
          <text>系统通知</text>
        </view>
        <view class="type-tab" :class="{ 'type-tab--active': activeType === 2 }" @click="switchType(2)">
          <text>物流通知</text>
        </view>
      </view>
    </view>

    <!-- 选中模式工具栏 -->
    <view v-if="selectMode" class="select-toolbar">
      <view class="select-all-btn" @click="toggleSelectAll">
        <iconpark-icon :name="isAllSelected ? 'checkbox-filled' : 'circle'" size="22" :color="isAllSelected ? '#FF6B4A' : '#D4D2CC'" />
        <text :style="{ color: isAllSelected ? '#FF6B4A' : '#1C1B1A' }">{{ isAllSelected ? '取消全选' : '全选' }}</text>
      </view>
      <view class="batch-delete-btn" :class="{ 'batch-delete-btn--disabled': selectedIds.size === 0 }" @click="onBatchDelete">
        <iconpark-icon name="trash" size="18" :color="selectedIds.size > 0 ? '#EF4444' : '#D4D2CC'" />
        <text :style="{ color: selectedIds.size > 0 ? '#EF4444' : '#D4D2CC' }">删除{{ selectedIds.size > 0 ? '(' + selectedIds.size + ')' : '' }}</text>
      </view>
    </view>

    <scroll-view class="main-scroll" :style="{ height: scrollHeight + 'px' }" scroll-y enhanced :show-scrollbar="false" @scrolltolower="loadMore" refresher-enabled @refresherrefresh="onRefresh" :refresher-triggered="refreshing">
      <view v-if="loading && list.length === 0" class="loading-state">
        <text class="loading-text">加载中…</text>
      </view>

      <view v-else-if="list.length === 0" class="empty-state">
        <iconpark-icon name="sound" size="48" color="#c2c6d5" />
        <text class="empty-text">{{ emptyText }}</text>
      </view>

      <view v-for="item in list" :key="item.id" class="notify-item" :class="{ 'notify-item--unread': item.isRead === 0 }" @click="onItemTap(item)">
        <!-- 选择模式：复选框 -->
        <view v-if="selectMode" class="notify-check" @click.stop="toggleSelect(item)">
          <iconpark-icon :name="isSelected(item) ? 'checkbox-filled' : 'circle'" size="22" :color="isSelected(item) ? '#FF6B4A' : '#D4D2CC'" />
        </view>

        <view class="notify-left">
          <view class="notify-icon" :class="'notify-icon--' + item.iconStyle">
            <iconpark-icon :name="item.iconType" size="20" :color="item.iconColor" />
          </view>
        </view>
        <view class="notify-body">
          <view class="notify-top">
            <text class="notify-title">{{ item.title }}</text>
            <text class="notify-time">{{ formatTime(item.createdAt) }}</text>
          </view>
          <text class="notify-content">{{ item.content }}</text>
        </view>
        <view v-if="!selectMode" class="notify-right">
          <view v-if="item.isRead === 0" class="unread-dot"></view>
          <view class="delete-btn" @click.stop="onDeleteOne(item)">
            <iconpark-icon name="trash" size="16" color="#c2c6d5" />
          </view>
        </view>
      </view>

      <uni-load-more v-if="list.length > 0" :status="loadMoreStatus" />

      <view class="bottom-placeholder"></view>
    </scroll-view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { notificationApi } from '@/api'
import { NOTIFICATION_TYPES } from '@/utils/constants.js'
import { showToast } from '@/utils/toast'

const sysInfo = uni.getSystemInfoSync()
const toolbarHeight = 72
const scrollHeight = sysInfo.windowHeight - sysInfo.statusBarHeight - 44 - (toolbarHeight)

const activeType = ref('')
const list = ref([])
const page = ref(1)
const hasMore = ref(true)
const loading = ref(false)
const refreshing = ref(false)
const selectMode = ref(false)
const selectedIds = ref(new Set())

const emptyText = computed(() => {
  if (activeType.value === 1) return '暂无系统通知'
  if (activeType.value === 2) return '暂无物流通知'
  return '暂无通知'
})

const hasUnread = computed(() => list.value.some(item => item.isRead === 0))

const loadMoreStatus = computed(() => {
  if (loading.value) return 'loading'
  if (!hasMore.value) return 'noMore'
  return 'more'
})

const isAllSelected = computed(() => {
  return list.value.length > 0 && selectedIds.value.size === list.value.length
})

function isSelected(item) {
  return selectedIds.value.has(item.id)
}

function toggleSelect(item) {
  const newSet = new Set(selectedIds.value)
  if (newSet.has(item.id)) {
    newSet.delete(item.id)
  } else {
    newSet.add(item.id)
  }
  selectedIds.value = newSet
}

function toggleSelectAll() {
  if (isAllSelected.value) {
    selectedIds.value = new Set()
  } else {
    selectedIds.value = new Set(list.value.map(item => item.id))
  }
}

function enterSelectMode() {
  selectMode.value = true
  selectedIds.value = new Set()
}

function exitSelectMode() {
  selectMode.value = false
  selectedIds.value = new Set()
}

onLoad((options) => {
  if (options?.type) activeType.value = Number(options.type) || ''
  fetchList()
})

function switchType(type) {
  activeType.value = type
  page.value = 1
  list.value = []
  exitSelectMode()
  fetchList()
}

async function fetchList() {
  loading.value = true
  try {
    const params = { page: page.value, size: 50 }
    const data = await notificationApi.getNotifications(params, { showLoading: false })
    let records = (data.records || []).map(normalizeItem)
    if (activeType.value !== '') {
      records = records.filter(r => r.type === activeType.value)
    }
    if (page.value === 1) {
      list.value = records
    } else {
      list.value.push(...records)
    }
    hasMore.value = records.length >= 50
  } catch (e) { /* handled */ }
  loading.value = false
  refreshing.value = false
}

function normalizeItem(raw) {
  const type = raw.type || 1
  const iconMap = {
    1: { icon: 'sound', color: '#3871d6', style: 'blue' },
    2: { icon: 'delivery', color: '#e67e22', style: 'orange' },
    3: { icon: 'notification', color: '#34d399', style: 'green' }
  }
  const iconMeta = iconMap[type] || iconMap[1]
  return {
    ...raw,
    iconType: iconMeta.icon,
    iconColor: iconMeta.color,
    iconStyle: iconMeta.style,
    typeLabel: NOTIFICATION_TYPES[type] || '通知'
  }
}

function onItemTap(item) {
  if (selectMode.value) {
    toggleSelect(item)
    return
  }
  // Mark as read
  if (item.isRead === 0) {
    notificationApi.markRead(item.id || item.notificationId).then(() => {
      item.isRead = 1
    }).catch(() => {})
  }
  // Navigate based on type and related IDs
  if (item.taskId) {
    uni.navigateTo({ url: `/pages/order-delivering/order-delivering?taskId=${item.taskId}&role=publisher` })
  } else if (item.orderId) {
    uni.navigateTo({ url: `/pages/order-delivering/order-delivering?orderId=${item.orderId}&role=runner` })
  }
}

async function onMarkAllRead() {
  try {
    await notificationApi.markAllRead()
    list.value.forEach(item => { item.isRead = 1 })
    uni.showToast({ title: '已全部已读', icon: 'success' })
  } catch (e) { /* handled */ }
}

async function onDeleteOne(item) {
  const res = await new Promise(r => {
    uni.showModal({ title: '删除通知', content: '确定要删除这条通知吗？', success: r2 => r(r2.confirm) })
  })
  if (!res) return
  try {
    const id = item.id || item.notificationId
    await notificationApi.deleteNotification(id)
    const idx = list.value.indexOf(item)
    if (idx > -1) list.value.splice(idx, 1)
  } catch (e) { /* handled */ }
}

async function onBatchDelete() {
  if (selectedIds.value.size === 0) return
  const res = await new Promise(r => {
    uni.showModal({
      title: '批量删除',
      content: `确定要删除选中的 ${selectedIds.value.size} 条通知吗？`,
      success: r2 => r(r2.confirm)
    })
  })
  if (!res) return

  uni.showLoading({ title: '删除中…' })
  const idsToDelete = Array.from(selectedIds.value)
  let successCount = 0
  for (const id of idsToDelete) {
    try {
      await notificationApi.deleteNotification(id)
      successCount++
    } catch (e) { /* skip failed */ }
  }

  // Remove deleted items from list
  const idSet = new Set(idsToDelete)
  list.value = list.value.filter(item => !idSet.has(item.id))
  selectedIds.value = new Set()
  selectMode.value = false
  uni.hideLoading()
  showToast(`已删除${successCount}条`, { icon: 'success' })
}

function formatTime(timeStr) {
  if (!timeStr) return ''
  const now = new Date()
  const time = new Date(timeStr.replace(/-/g, '/'))
  const diff = now - time
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
  if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前'
  return timeStr.split(' ')[0] || timeStr
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

function onBack() { uni.navigateBack() }
</script>

<style scoped>
.page{width:100%;height:100vh;display:flex;flex-direction:column;background:var(--background);overflow:hidden}

.nav-actions{display:flex;gap:8rpx}
.nav-action{padding:8rpx 16rpx;font-size:26rpx;color:var(--primary);font-weight:500}
.nav-action:active{opacity:.6}

.tab-section{flex-shrink:0;padding:0 32rpx 16rpx;background:var(--background)}
.type-tabs{display:flex;gap:0;background:var(--surface-raised);border-radius:var(--radius-card);padding:6rpx}
.type-tab{flex:1;text-align:center;padding:16rpx 0;border-radius:20rpx;font-size:26rpx;font-weight:500;color:var(--text-secondary)}
.type-tab--active{background:var(--primary);color:#fff;font-weight:600}

.select-toolbar{display:flex;justify-content:space-between;align-items:center;padding:16rpx 32rpx;background:var(--surface-raised);border-bottom:1rpx solid var(--surface-hover);flex-shrink:0}
.select-all-btn{display:flex;align-items:center;gap:10rpx;padding:8rpx 0}
.select-all-btn text{font-size:26rpx;font-weight:500}
.batch-delete-btn{display:flex;align-items:center;gap:8rpx;padding:12rpx 24rpx;border-radius:var(--radius-card);background:var(--error-container)}
.batch-delete-btn--disabled{background:var(--surface)}
.batch-delete-btn text{font-size:26rpx;font-weight:500}

.main-scroll{box-sizing:border-box;width:100%;padding:0 32rpx}

.notify-item{display:flex;align-items:flex-start;gap:20rpx;padding:28rpx;background:var(--surface-raised);border-radius:20rpx;margin-bottom:16rpx;box-shadow:var(--shadow-sm)}
.notify-item--unread{background:var(--primary-container);border:1rpx solid var(--primary-container)}
.notify-item:active{transform:scale(.98)}
.notify-check{display:flex;align-items:center;padding-top:6rpx;flex-shrink:0}
.notify-left{flex-shrink:0}
.notify-icon{width:64rpx;height:64rpx;border-radius:50%;display:flex;align-items:center;justify-content:center}
.notify-icon--blue{background:var(--primary-container)}
.notify-icon--orange{background:#fff7ed}
.notify-icon--green{background:#f0fdf4}
.notify-body{flex:1;min-width:0}
.notify-top{display:flex;justify-content:space-between;align-items:baseline;margin-bottom:8rpx}
.notify-title{font-size:28rpx;font-weight:500;color:var(--text-primary)}
.notify-time{font-size:22rpx;color:var(--text-tertiary);flex-shrink:0;margin-left:16rpx}
.notify-content{font-size:24rpx;color:var(--text-secondary);line-height:1.5;display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical;overflow:hidden}
.notify-right{display:flex;flex-direction:column;align-items:center;gap:12rpx;flex-shrink:0}
.unread-dot{width:14rpx;height:14rpx;background:var(--error);border-radius:50%}
.delete-btn{width:44rpx;height:44rpx;display:flex;align-items:center;justify-content:center;border-radius:50%}
.delete-btn:active{background:rgba(194,198,213,.15)}

.loading-state,.empty-state{display:flex;flex-direction:column;align-items:center;padding:120rpx 0;gap:20rpx;opacity:.5}
.loading-text,.empty-text{font-size:28rpx;color:var(--text-tertiary)}
.bottom-placeholder{height:40rpx}
</style>
