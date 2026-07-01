<template>
  <view class="page">
    <uni-nav-bar title="选择取件驿站" backgroundColor="#FAFAF8" :border="false" statusBar fixed leftIcon="left" @clickLeft="onBack" color="#1C1B1A" />

    <scroll-view class="main-scroll" :style="{ height: scrollHeight + 'px' }" scroll-y enhanced :show-scrollbar="false">
      <view class="page-hint">请选择快递所在的驿站</view>

      <view class="station-list">
        <view v-for="station in stations" :key="station.id" class="station-item" @click="onSelect(station)">
          <view class="station-logo" :style="{ background: logoColor(station.id) }">
            <iconpark-icon name="transporter" size="28" color="#fff" />
          </view>
          <text class="station-name">{{ station.name }}</text>
          <iconpark-icon name="right" size="18" color="#D4D2CC" />
        </view>
      </view>

      <view class="bottom-placeholder"></view>
    </scroll-view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { PICKUP_STATIONS } from '@/utils/campus-data.js'

const sysInfo = uni.getSystemInfoSync()
const scrollHeight = sysInfo.windowHeight - sysInfo.statusBarHeight - 44

const stations = PICKUP_STATIONS

const logoColors = ['#e67e22', '#ba1a1a', '#FF6B4A', '#34d399', '#f59e0b', '#4c5e86', '#10b981']

function logoColor(id) {
  return logoColors[(id - 1) % logoColors.length]
}

function onSelect(station) {
  uni.$emit('stationSelected', station.name)
  uni.navigateBack()
}

function onBack() { uni.navigateBack() }
</script>

<style scoped>
.page{width:100%;height:100vh;display:flex;flex-direction:column;background:var(--background);overflow:hidden}
.main-scroll{box-sizing:border-box;width:100%;padding:0 32rpx}
.page-hint{font-size:26rpx;color:var(--text-secondary);padding:24rpx 0 16rpx}

.station-list{display:flex;flex-direction:column;gap:0}
.station-item{display:flex;align-items:center;gap:20rpx;padding:28rpx 24rpx;background:var(--surface-raised);border-radius:0;border-bottom:1rpx solid var(--surface-hover)}
.station-item:first-child{border-radius:var(--radius-card) var(--radius-card) 0 0}
.station-item:last-child{border-radius:0 0 var(--radius-card) var(--radius-card);border-bottom:none}
.station-item:only-child{border-radius:var(--radius-card)}
.station-item:active{background:var(--surface)}

.station-logo{width:72rpx;height:72rpx;border-radius:50%;display:flex;align-items:center;justify-content:center;flex-shrink:0}
.station-logo-text{font-size:32rpx;font-weight:700;color:#fff}

.station-name{flex:1;font-size:28rpx;font-weight:500;color:var(--text-primary)}

.bottom-placeholder{height:60rpx}
</style>
