<template>
  <view class="page">
    <uni-nav-bar title="收货地址" backgroundColor="#FAFAF8" :border="false" statusBar fixed leftIcon="left" @clickLeft="onBack" color="#1C1B1A" />

    <scroll-view class="main-scroll" :style="{ height: scrollHeight + 'px' }" scroll-y enhanced :show-scrollbar="false" refresher-enabled @refresherrefresh="loadData" :refresher-triggered="refreshing">
      <view v-if="selectMode" class="select-hint">
        <iconpark-icon name="info-filled" size="18" color="#FF6B4A" />
        <text>请选择收货地址</text>
      </view>

      <view v-if="list.length === 0 && !loading" class="empty-state">
        <iconpark-icon name="location" size="48" color="#D4D2CC" />
        <text class="empty-text">暂无收货地址</text>
      </view>

      <view v-for="addr in list" :key="addr.id" class="addr-swipe-wrapper">
        <view class="addr-delete-btn" @click.stop="onDeleteAddr(addr.id)">
          <iconpark-icon name="trash" size="22" color="#fff" />
          <text>删除</text>
        </view>
        <view class="addr-swipe-content" :class="{ 'addr-swipe-content--open': swipedId === addr.id }" @touchstart="onTouchStart($event, addr.id)" @touchmove="onTouchMove($event, addr.id)" @touchend="onTouchEnd(addr.id)" @click="onTapAddr(addr)">
          <view class="addr-card" :class="{ 'addr-card--default': addr.isDefault === 1 }">
            <view v-if="addr.isDefault === 1" class="active-bar"></view>
            <view class="addr-content">
              <view class="addr-top">
                <text class="addr-name">{{ addr.contactName }}</text>
                <text class="addr-phone">{{ addr.contactPhone }}</text>
                <uni-tag v-if="addr.isDefault === 1" text="默认" name="primary" size="small" :inverted="true" customStyle="border-color:#FFD1C7;color:#FF6B4A;background:#FFF0ED;" />
              </view>
              <text class="addr-detail">{{ addr.detail }}</text>
            </view>
            <view class="addr-edit" @click.stop="onEdit(addr.id)">
              <iconpark-icon name="compose" size="18" color="#8F8D88" />
            </view>
          </view>
        </view>
      </view>

      <view class="bottom-placeholder"></view>
    </scroll-view>

    <view class="bottom-bar safe-area-bottom">
      <view class="add-btn" @click="onAdd">
        <iconpark-icon name="plus" size="22" color="#fff" />
        <text>新增收货地址</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onShow, onLoad } from '@dcloudio/uni-app'
import { addressApi } from '@/api'

const sysInfo = uni.getSystemInfoSync()
const scrollHeight = sysInfo.windowHeight - sysInfo.statusBarHeight - 44

const list = ref([])
const loading = ref(false)
const refreshing = ref(false)
const swipedId = ref(null)
const selectMode = ref(false)
let touchStartX = 0
let touchStartY = 0

async function loadData() {
  loading.value = true
  try {
    const data = await addressApi.getAddressList()
    list.value = data || []
  } catch (e) { /* handled */ }
  loading.value = false
  refreshing.value = false
}

onLoad((options) => {
  selectMode.value = options?.selectMode === '1'
})

function onBack() { uni.navigateBack() }

function onTapAddr(addr) {
  if (swipedId.value === addr.id) { swipedId.value = null; return }
  if (selectMode.value) {
    uni.$emit('addressSelected', addr)
    uni.navigateBack()
    return
  }
  uni.navigateTo({ url: `/pages/address-edit/address-edit?id=${addr.id}&view=1` })
}

function onEdit(id) {
  uni.navigateTo({ url: `/pages/address-edit/address-edit?id=${id}` })
}

function onAdd() {
  uni.navigateTo({ url: '/pages/address-edit/address-edit' })
}

async function onDeleteAddr(id) {
  const res = await new Promise(r => {
    uni.showModal({ title: '删除地址', content: '确定要删除该收货地址吗？', success: r2 => r(r2.confirm) })
  })
  if (!res) return
  try {
    await addressApi.deleteAddress(id)
    list.value = list.value.filter(a => a.id !== id)
    swipedId.value = null
    uni.showToast({ title: '已删除', icon: 'success' })
  } catch (e) { /* handled */ }
}

function onTouchStart(e, id) { touchStartX = e.touches[0].clientX; touchStartY = e.touches[0].clientY }
function onTouchMove(e, id) {
  const dx = e.touches[0].clientX - touchStartX
  const dy = e.touches[0].clientY - touchStartY
  if (Math.abs(dx) > Math.abs(dy) && dx < -30) swipedId.value = id
  else if (dx > 30) swipedId.value = null
}
function onTouchEnd(id) {}

loadData()
onShow(() => { loadData() })
</script>

<style scoped>
.page{width:100%;height:100vh;display:flex;flex-direction:column;background:var(--background);overflow:hidden}
.main-scroll{box-sizing:border-box;width:100%;padding:0 32rpx;padding-bottom:60rpx}

.addr-swipe-wrapper{position:relative;margin-bottom:20rpx;overflow:hidden;border-radius:var(--radius-card)}
.addr-delete-btn{position:absolute;right:0;top:0;bottom:0;width:160rpx;background:var(--error);display:flex;flex-direction:column;align-items:center;justify-content:center;gap:6rpx;border-radius:0 var(--radius-card) var(--radius-card) 0}
.addr-delete-btn text{font-size:24rpx;color:#fff}
.addr-swipe-content{background:var(--surface-raised);border-radius:var(--radius-card);position:relative;z-index:1;transition:transform .2s ease}
.addr-swipe-content--open{transform:translateX(-160rpx)}

.addr-card{display:flex;padding:28rpx;position:relative;overflow:hidden;min-height:140rpx;box-sizing:border-box}
.addr-card--default{border:1rpx solid var(--primary-container)}
.active-bar{position:absolute;left:0;top:20rpx;bottom:20rpx;width:6rpx;background:var(--primary);border-radius:3rpx}
.addr-content{flex:1;min-width:0}
.addr-top{display:flex;align-items:center;gap:14rpx;margin-bottom:10rpx}
.addr-name{font-size:30rpx;font-weight:600;color:var(--text-primary)}
.addr-phone{font-size:24rpx;color:var(--text-secondary)}
.addr-detail{font-size:24rpx;color:var(--text-primary);display:block}
.addr-edit{padding:8rpx 8rpx 8rpx 16rpx;flex-shrink:0;display:flex;align-items:center}

.empty-state{display:flex;flex-direction:column;align-items:center;padding:160rpx 0 80rpx;gap:20rpx;opacity:.5}
.empty-text{font-size:28rpx;color:var(--text-tertiary)}

.select-hint{display:flex;align-items:center;gap:10rpx;padding:20rpx 24rpx;margin-bottom:12rpx;background:var(--primary-container);border-radius:16rpx}
.select-hint text{font-size:26rpx;color:var(--primary)}

.bottom-bar{position:fixed;bottom:0;left:0;width:100%;background:var(--surface-raised);border-top:1rpx solid var(--surface-hover);padding:20rpx 32rpx;padding-bottom:calc(20rpx + env(safe-area-inset-bottom));box-sizing:border-box;z-index:50}
.add-btn{height:90rpx;background:var(--primary);border-radius:48rpx;display:flex;align-items:center;justify-content:center;gap:10rpx}
.add-btn text{font-size:28rpx;font-weight:600;color:#fff}
.add-btn:active{transform:scale(.95)}
.bottom-placeholder{height:170rpx}
</style>
