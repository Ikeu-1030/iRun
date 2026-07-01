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

    <!-- Select mode toolbar -->
    <view v-if="selectMode" class="select-toolbar">
      <view class="toolbar-left">
        <view class="toolbar-btn" @click="toggleSelectAll">
          <iconpark-icon :name="isAllSelected ? 'checkbox-filled' : 'circle'" size="22" :color="isAllSelected ? '#FF6B4A' : '#D4D2CC'" />
          <text :style="{ color: isAllSelected ? '#FF6B4A' : '#1C1B1A' }">{{ isAllSelected ? '取消全选' : '全选' }}</text>
        </view>
      </view>
      <view class="toolbar-right">
        <view class="toolbar-btn toolbar-btn--green" :class="{ 'toolbar-btn--disabled': !hasUnreadSelected }" @click="onBatchRead">
          <iconpark-icon name="check" size="18" :color="hasUnreadSelected ? '#2EC4B6' : '#D4D2CC'" />
          <text :style="{ color: hasUnreadSelected ? '#2EC4B6' : '#D4D2CC' }">已读</text>
        </view>
        <view class="toolbar-btn toolbar-btn--red" :class="{ 'toolbar-btn--disabled': selectedIds.size === 0 }" @click="onBatchDelete">
          <iconpark-icon name="trash" size="18" :color="selectedIds.size > 0 ? '#EF4444' : '#D4D2CC'" />
          <text :style="{ color: selectedIds.size > 0 ? '#EF4444' : '#D4D2CC' }">删除{{ selectedIds.size > 0 ? '(' + selectedIds.size + ')' : '' }}</text>
        </view>
      </view>
    </view>

    <!-- List -->
    <scroll-view
      class="main-scroll" :style="{ height: scrollHeight + 'px' }"
      scroll-y enhanced :show-scrollbar="false"
      @scrolltolower="loadMore"
      @scroll="closeSwipe"
      refresher-enabled @refresherrefresh="onRefresh" :refresher-triggered="refreshing"
    >
      <view v-if="loading && list.length === 0" class="loading-state">
        <text class="loading-text">加载中…</text>
      </view>
      <view v-else-if="list.length === 0" class="empty-state">
        <iconpark-icon name="sound" size="48" color="#D4D2CC" />
        <text class="empty-text">暂无通知</text>
      </view>

      <view
        v-for="(item, index) in list"
        :key="'n-' + item.id"
        class="notify-swipe-wrapper"
      >
        <!-- Action buttons behind content -->
        <view class="notify-swipe-actions">
          <view v-if="item.isRead === 0" class="swipe-btn swipe-btn--read" @click.stop="onSwipeRead(item)">
            <iconpark-icon name="check" size="20" color="#fff" />
            <text>已读</text>
          </view>
          <view class="swipe-btn swipe-btn--del" @click.stop="onSwipeDelete(item)">
            <iconpark-icon name="trash" size="20" color="#fff" />
            <text>删除</text>
          </view>
        </view>

        <!-- Sliding content -->
        <view
          class="notify-swipe-content animate-fade-up"
          :class="{ 'notify-swipe-content--open': swipedId === item.id }"
          :style="{ animationDelay: (index * 0.04) + 's' }"
          @touchstart="onTouchStart($event, item.id)"
          @touchmove="onTouchMove($event, item.id)"
          @touchend="onTouchEnd(item.id)"
          @click="onItemClick(item)"
          @longpress="onLongPress(item)"
        >
          <view class="notify-item" :class="{ 'notify-item--unread': item.isRead === 0 }">
            <view v-if="selectMode" class="notify-check" @click.stop="toggleSelect(item)">
              <iconpark-icon :name="isSelected(item) ? 'checkbox-filled' : 'circle'" size="22" :color="isSelected(item) ? '#FF6B4A' : '#D4D2CC'" />
            </view>

            <view class="notify-avatar-wrap">
              <view class="notify-avatar" :class="'notify-avatar--' + item.iconStyle">
                <iconpark-icon :name="item.iconType" size="22" :color="item.iconColor" />
              </view>
            </view>

            <view class="notify-body">
              <view class="notify-top">
                <text class="notify-title">{{ item.title }}</text>
                <text class="notify-time" :class="{ 'notify-time--new': item.isRead === 0 }">{{ formatTime(item.createdAt) }}</text>
              </view>
              <view class="notify-bottom">
                <text class="notify-preview">{{ item.content }}</text>
                <view v-if="!selectMode && item.isRead === 0" class="unread-dot"></view>
              </view>
            </view>
          </view>
        </view>
      </view>

      <uni-load-more v-if="list.length > 0" :status="loadMoreStatus" />
      <view class="bottom-placeholder"></view>
    </scroll-view>

    <!-- Detail modal -->
    <view v-if="detailVisible" class="modal-overlay" @click="closeDetail">
      <view class="modal-card animate-scale-pop" @click.stop>
        <view class="modal-header">
          <view class="modal-type-tag" :style="{ background: detailItem.iconBg, color: detailItem.iconColor }">
            {{ detailItem.typeLabel }}
          </view>
          <view class="modal-close" @click="closeDetail">
            <iconpark-icon name="close" size="20" color="#8F8D88" />
          </view>
        </view>
        <text class="modal-title">{{ detailItem.title }}</text>
        <text class="modal-time">{{ formatFullTime(detailItem.createdAt) }}</text>
        <view class="modal-divider"></view>
        <text class="modal-content">{{ detailItem.content }}</text>
        <view v-if="detailItem.taskId || detailItem.orderId" class="modal-link" @click="onViewOrder(detailItem)">
          <text>查看关联订单</text>
          <iconpark-icon name="right" size="14" color="#FF6B4A" />
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { notificationApi } from '@/api'
import { NOTIFICATION_TYPES } from '@/utils/constants.js'
import { showToast } from '@/utils/toast'

const sysInfo = uni.getSystemInfoSync()
const scrollHeight = sysInfo.windowHeight - sysInfo.statusBarHeight - 44

const swipedId = ref(null)
let touchStartX = 0
let touchStartY = 0

const filterType = ref(0)
const list = ref([])
const page = ref(1)
const hasMore = ref(true)
const loading = ref(false)
const refreshing = ref(false)
const selectMode = ref(false)
const selectedIds = ref(new Set())
const detailVisible = ref(false)
const detailItem = ref({})

const hasUnread = computed(() => list.value.some(item => item.isRead === 0))
const hasUnreadSelected = computed(() => {
  for (const id of selectedIds.value) {
    const item = list.value.find(i => i.id === id)
    if (item && item.isRead === 0) return true
  }
  return false
})
const loadMoreStatus = computed(() => {
  if (loading.value) return 'loading'
  if (!hasMore.value) return 'noMore'
  return 'more'
})
const isAllSelected = computed(() => list.value.length > 0 && selectedIds.value.size === list.value.length)

function isSelected(item) { return selectedIds.value.has(item.id) }
function toggleSelect(item) {
  const ns = new Set(selectedIds.value)
  ns.has(item.id) ? ns.delete(item.id) : ns.add(item.id)
  selectedIds.value = ns
}
function toggleSelectAll() {
  selectedIds.value = isAllSelected.value ? new Set() : new Set(list.value.map(i => i.id))
}
function enterSelectMode() { selectMode.value = true; selectedIds.value = new Set() }
function exitSelectMode() { selectMode.value = false; selectedIds.value = new Set() }

onLoad((options) => {
  if (options?.type) filterType.value = Number(options.type) || 0
  fetchList()
})

async function fetchList() {
  loading.value = true
  try {
    const data = await notificationApi.getNotifications({ page: page.value, size: 50 }, { showLoading: false })
    const rawRecords = (data.records || []).map(normalizeItem)
    hasMore.value = rawRecords.length >= 50
    let records = rawRecords
    if (filterType.value > 0) {
      records = rawRecords.filter(r => r.type === filterType.value)
    }
    if (page.value === 1) {
      list.value = records
    } else {
      list.value.push(...records)
    }
  } catch (e) { /* handled */ }
  loading.value = false
  refreshing.value = false
}

const iconMetaMap = {
  1: { icon: 'sound', color: '#FF6B4A', bg: '#FFF0ED', style: 'coral' },
  2: { icon: 'delivery', color: '#E8734A', bg: '#FFF2ED', style: 'orange' },
  3: { icon: 'fireworks', color: '#8B6BAE', bg: '#F6F1FA', style: 'purple' },
}

function normalizeItem(raw) {
  const type = raw.type || 1
  const meta = iconMetaMap[type] || iconMetaMap[1]
  return { ...raw, iconType: meta.icon, iconColor: meta.color, iconBg: meta.bg, iconStyle: meta.style, typeLabel: NOTIFICATION_TYPES[type] || '通知' }
}

function onTouchStart(e, id) { touchStartX = e.touches[0].clientX; touchStartY = e.touches[0].clientY }
function onTouchMove(e, id) {
  const dx = e.touches[0].clientX - touchStartX
  const dy = e.touches[0].clientY - touchStartY
  if (Math.abs(dx) > Math.abs(dy) && dx < -30) swipedId.value = id
  else if (dx > 30) swipedId.value = null
}
let swipeTimer = null
function onTouchEnd(id) {
  if (swipeTimer) clearTimeout(swipeTimer)
  swipeTimer = setTimeout(() => { if (swipedId.value === id) swipedId.value = null }, 4000)
}
function closeSwipe() { swipedId.value = null }

function onSwipeRead(item) {
  swipedId.value = null
  markSingleReadSilent(item)
}
function onSwipeDelete(item) {
  swipedId.value = null
  deleteOneItem(item)
}

function onItemClick(item) {
  if (selectMode.value) { toggleSelect(item); return }
  detailItem.value = item
  detailVisible.value = true
  if (item.isRead === 0) markSingleReadSilent(item)
}

function onLongPress(item) {
  if (!selectMode.value) { enterSelectMode(); toggleSelect(item) }
}

function closeDetail() { detailVisible.value = false }

async function deleteOneItem(item) {
  const res = await new Promise(r => {
    uni.showModal({ title: '删除通知', content: '确定要删除这条通知吗？', success: r2 => r(r2.confirm) })
  })
  if (!res) return
  try {
    await notificationApi.deleteNotification(item.id)
    const idx = list.value.indexOf(item)
    if (idx > -1) list.value.splice(idx, 1)
  } catch (e) { /* handled */ }
}

async function markSingleReadSilent(item) {
  try { await notificationApi.markRead(item.id); item.isRead = 1 } catch (e) { /* ignore */ }
}

async function onMarkAllRead() {
  try {
    await notificationApi.markAllRead()
    list.value.forEach(item => { item.isRead = 1 })
    showToast('已全部已读', { icon: 'success' })
  } catch (e) { /* handled */ }
}

async function onBatchRead() {
  if (!hasUnreadSelected.value) return
  const ids = Array.from(selectedIds.value).filter(id => {
    const item = list.value.find(i => i.id === id)
    return item && item.isRead === 0
  })
  if (ids.length === 0) return
  try {
    await notificationApi.markBatchRead(ids)
    ids.forEach(id => { const item = list.value.find(i => i.id === id); if (item) item.isRead = 1 })
    exitSelectMode()
    showToast(`已标记${ids.length}条已读`, { icon: 'success' })
  } catch (e) { /* handled */ }
}

async function onBatchDelete() {
  if (selectedIds.value.size === 0) return
  const res = await new Promise(r => {
    uni.showModal({ title: '批量删除', content: `确定要删除选中的 ${selectedIds.value.size} 条通知吗？`, success: r2 => r(r2.confirm) })
  })
  if (!res) return
  uni.showLoading({ title: '删除中…' })
  const idsToDelete = Array.from(selectedIds.value)
  let ok = 0
  for (const id of idsToDelete) {
    try { await notificationApi.deleteNotification(id); ok++ } catch (e) { /* skip */ }
  }
  const idSet = new Set(idsToDelete)
  list.value = list.value.filter(item => !idSet.has(item.id))
  exitSelectMode()
  uni.hideLoading({ fail: () => {} })
  showToast(`已删除${ok}条`, { icon: 'success' })
}

function onViewOrder(item) {
  if (item.taskId) {
    uni.navigateTo({ url: `/pages/order-delivering/order-delivering?taskId=${item.taskId}&role=publisher` })
  } else if (item.orderId) {
    uni.navigateTo({ url: `/pages/order-delivering/order-delivering?orderId=${item.orderId}&role=runner` })
  }
}

function formatTime(ts) {
  if (!ts) return ''
  const now = new Date()
  const t = new Date(ts.replace(/-/g, '/'))
  const diff = now - t
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
  if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前'
  return ts.split(' ')[0] || ts
}

function formatFullTime(ts) {
  if (!ts) return ''
  return ts.replace('T', ' ')
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
.page { width: 100%; height: 100vh; display: flex; flex-direction: column; background: var(--background); overflow: hidden; }

.nav-actions { display: flex; gap: 8rpx; }
.nav-action { padding: 8rpx 16rpx; font-size: 26rpx; color: var(--primary); font-weight: 500; }
.nav-action:active { opacity: .6; }

.select-toolbar { display: flex; justify-content: space-between; align-items: center; padding: 16rpx 32rpx; background: var(--surface-raised); border-bottom: 1rpx solid var(--outline-light); flex-shrink: 0; }
.toolbar-left, .toolbar-right { display: flex; gap: 16rpx; align-items: center; }
.toolbar-btn { display: flex; align-items: center; gap: 8rpx; padding: 10rpx 20rpx; border-radius: 20rpx; }
.toolbar-btn--green { background: #EDFAF7; }
.toolbar-btn--red { background: #FEF0F0; }
.toolbar-btn--disabled { opacity: .4; }
.toolbar-btn text { font-size: 26rpx; font-weight: 500; }

.main-scroll { box-sizing: border-box; width: 100%; }

/* Swipe wrapper */
.notify-swipe-wrapper { position: relative; overflow: hidden; margin-bottom: 1rpx; }
.notify-swipe-actions { position: absolute; right: 0; top: 0; bottom: 0; display: flex; }
.swipe-btn { width: 120rpx; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 4rpx; }
.swipe-btn--read { background: #2EC4B6; }
.swipe-btn--del { background: var(--error); }
.swipe-btn text { font-size: 20rpx; color: #fff; }
.notify-swipe-content { position: relative; z-index: 1; background: var(--surface-raised); transition: transform .2s ease; }
.notify-swipe-content--open { transform: translateX(-120rpx); }
.notify-item { display: flex; align-items: center; padding: 20rpx 28rpx; border-bottom: 1rpx solid var(--outline-light); transition: background .15s; }
.notify-item:active { background: var(--surface-hover); }
.notify-item--unread { background: var(--primary-container); }

.notify-check { display: flex; align-items: center; margin-right: 12rpx; flex-shrink: 0; }

.notify-avatar-wrap { position: relative; width: 68rpx; height: 68rpx; flex-shrink: 0; margin-right: 20rpx; }
.notify-avatar { width: 100%; height: 100%; border-radius: 50%; display: flex; align-items: center; justify-content: center; }
.notify-avatar--coral { background: #FFF0ED; }
.notify-avatar--orange { background: #FFF2ED; }
.notify-avatar--purple { background: #F6F1FA; }

.notify-body { flex: 1; min-width: 0; }
.notify-top { display: flex; justify-content: space-between; align-items: baseline; margin-bottom: 4rpx; }
.notify-title { font-size: 28rpx; font-weight: 500; color: var(--text-primary); flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; margin-right: 12rpx; }
.notify-time { font-size: 22rpx; color: var(--text-tertiary); flex-shrink: 0; }
.notify-time--new { color: var(--primary); font-weight: 600; }
.notify-bottom { display: flex; justify-content: space-between; align-items: center; }
.notify-preview { font-size: 24rpx; color: var(--text-secondary); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; flex: 1; padding-right: 12rpx; }
.unread-dot { width: 12rpx; height: 12rpx; background: var(--error); border-radius: 50%; flex-shrink: 0; }

.loading-state, .empty-state { display: flex; flex-direction: column; align-items: center; padding: 120rpx 0; gap: 20rpx; opacity: .5; }
.loading-text, .empty-text { font-size: 28rpx; color: var(--text-tertiary); }
.bottom-placeholder { height: 40rpx; }

.modal-overlay { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0,0,0,.45); z-index: 999; display: flex; align-items: flex-end; justify-content: center; }
.modal-card { width: 100%; max-height: 70vh; background: var(--surface-raised); border-radius: 32rpx 32rpx 0 0; padding: 40rpx 32rpx 60rpx; overflow-y: auto; }
.modal-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20rpx; }
.modal-type-tag { padding: 6rpx 16rpx; border-radius: 12rpx; font-size: 22rpx; font-weight: 500; }
.modal-close { width: 56rpx; height: 56rpx; border-radius: 50%; display: flex; align-items: center; justify-content: center; background: var(--surface); }
.modal-title { font-size: 32rpx; font-weight: 600; color: var(--text-primary); display: block; margin-bottom: 12rpx; }
.modal-time { font-size: 24rpx; color: var(--text-tertiary); display: block; margin-bottom: 16rpx; }
.modal-divider { height: 1rpx; background: var(--outline-light); margin: 20rpx 0; }
.modal-content { font-size: 28rpx; color: var(--text-secondary); line-height: 1.7; white-space: pre-wrap; display: block; }
.modal-link { display: flex; align-items: center; justify-content: center; gap: 8rpx; margin-top: 32rpx; padding: 20rpx; border-radius: 16rpx; background: var(--primary-container); }
.modal-link text { font-size: 28rpx; color: var(--primary); font-weight: 500; }
.modal-link:active { opacity: .7; }

.animate-scale-pop { animation: scalePop 0.25s var(--ease-spring) both; transform-origin: bottom center; }
@keyframes scalePop {
  from { opacity: 0; transform: translateY(40rpx) scale(0.95); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}
</style>
