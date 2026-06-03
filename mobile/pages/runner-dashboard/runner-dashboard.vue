<template>
  <view class="page">
    <uni-nav-bar title="我的骑手" backgroundColor="#FAFAF8" :border="false" statusBar fixed leftIcon="left" @clickLeft="onBack" color="#1C1B1A" />

    <scroll-view class="main-scroll" :style="{ height: scrollHeight + 'px' }" scroll-y enhanced :show-scrollbar="false">
      <view v-if="loading" class="loading-state"><text class="loading-text">加载中…</text></view>

      <template v-else>
        <!-- 上下线状态卡片 -->
        <view class="status-card animate-fade-up" :class="isOnline ? 'status-card--online' : 'status-card--offline'">
          <view class="status-card-left">
            <view class="status-dot" :class="[isOnline ? 'status-dot--online animate-pulse-glow' : 'status-dot--offline']" />
            <view class="status-info">
              <text class="status-title">{{ isOnline ? '接单中' : '已离线' }}</text>
              <text class="status-hint">{{ isOnline ? '您当前可接收新任务' : '上线后可开始接收任务' }}</text>
            </view>
          </view>
          <view class="status-toggle" :class="{ 'status-toggle--active': isOnline }" @click="toggleOnline">
            <view class="status-toggle-thumb" />
          </view>
        </view>

        <!-- 接单容量 -->
        <view class="capacity-card animate-fade-up">
          <view class="capacity-header">
            <text class="capacity-label">当前接单</text>
            <text class="capacity-value">{{ profile.currentOrders || 0 }} / {{ profile.maxConcurrentOrders || 3 }} 单</text>
          </view>
          <view class="capacity-bar">
            <view class="capacity-fill" :style="{ width: capacityPercent + '%' }" />
          </view>
        </view>

        <!-- 绩效概览 -->
        <view class="section-title">绩效概览</view>
        <view class="stats-grid">
          <view class="stat-card animate-fade-up delay-1">
            <text class="stat-value">{{ profile.totalOrders || 0 }}</text>
            <text class="stat-label">总接单数</text>
          </view>
          <view class="stat-card animate-fade-up delay-2">
            <text class="stat-value">{{ profile.successOrders || 0 }}</text>
            <text class="stat-label">成功完成</text>
          </view>
          <view class="stat-card animate-fade-up delay-3">
            <text class="stat-value stat-value--rate">{{ completionRate }}%</text>
            <text class="stat-label">完成率</text>
          </view>
          <view class="stat-card animate-fade-up delay-4">
            <text class="stat-value stat-value--rate">{{ onTimeRate }}%</text>
            <text class="stat-label">准时率</text>
          </view>
          <view class="stat-card animate-fade-up delay-5">
            <text class="stat-value">{{ perf.creditScore || 100 }}</text>
            <text class="stat-label">信用分</text>
          </view>
          <view class="stat-card animate-fade-up delay-6">
            <text class="stat-value">¥{{ totalEarnings }}</text>
            <text class="stat-label">累计收入</text>
          </view>
        </view>

        <!-- 评分（仅展示） -->
        <view class="rating-card">
          <view class="rating-icon-wrap">
            <iconpark-icon name="star-filled" size="28" color="#fbbf24" />
          </view>
          <view class="rating-body">
            <text class="rating-value">{{ avgRating }}</text>
            <text class="rating-label">平均评分</text>
          </view>
        </view>

        <!-- 更多 -->
        <view class="section-title">更多</view>
        <view class="menu-list">
          <view class="menu-item" @click="openMaxOrdersPicker">
            <view class="menu-icon menu-icon--orders">
              <image :src="ICON_MAX_ORDERS" style="width:24px;height:24px" mode="aspectFit" />
            </view>
            <text class="menu-label">最大接单数</text>
            <text class="menu-value">{{ profile.maxConcurrentOrders || 3 }} 单</text>
            <iconpark-icon name="right" size="16" color="#D4D2CC" />
          </view>
          <view class="menu-item" @click="goReviews">
            <view class="menu-icon menu-icon--star">
              <image :src="ICON_REVIEWS" style="width:24px;height:24px" mode="aspectFit" />
            </view>
            <text class="menu-label">我的评价</text>
            <iconpark-icon name="right" size="16" color="#D4D2CC" />
          </view>
          <view class="menu-item menu-item--last" @click="goCert">
            <view class="menu-icon menu-icon--cert">
              <iconpark-icon name="medal" size="20" color="#FF6B4A" />
            </view>
            <text class="menu-label">骑手认证</text>
            <iconpark-icon name="right" size="16" color="#D4D2CC" />
          </view>
        </view>
      </template>

      <view class="bottom-placeholder"></view>
    </scroll-view>

    <!-- 最大接单数选择器 -->
    <view class="picker-overlay" v-if="showMaxOrdersPicker" @click="showMaxOrdersPicker = false">
      <view class="picker-panel" @click.stop>
        <view class="picker-title">设置最大接单数</view>
        <view class="picker-stepper">
          <view class="picker-btn" :class="{ 'picker-btn--disabled': maxOrdersEdit <= 1 }" @click="maxOrdersEdit > 1 && maxOrdersEdit--">−</view>
          <text class="picker-value">{{ maxOrdersEdit }}</text>
          <view class="picker-btn" :class="{ 'picker-btn--disabled': maxOrdersEdit >= 5 }" @click="maxOrdersEdit < 5 && maxOrdersEdit++">+</view>
        </view>
        <view class="picker-hint">可同时接取 1 ~ 5 个任务</view>
        <view class="picker-actions">
          <view class="picker-action picker-action--cancel" @click="showMaxOrdersPicker = false"><text>取消</text></view>
          <view class="picker-action picker-action--confirm" @click="saveMaxOrders"><text>{{ savingMax ? '保存中…' : '确定' }}</text></view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useStore } from '@/store/index.js'
import { runnerApi } from '@/api'

const store = useStore()
const sysInfo = uni.getSystemInfoSync()
const scrollHeight = sysInfo.windowHeight - sysInfo.statusBarHeight - 44

const ICON_MAX_ORDERS = 'data:image/svg+xml,%3Csvg%20xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22%20width%3D%2224%22%20height%3D%2224%22%20viewBox%3D%220%200%2048%2048%22%20fill%3D%22none%22%3E%3Crect%20x%3D%228%22%20y%3D%224%22%20width%3D%2232%22%20height%3D%2240%22%20rx%3D%222%22%20fill%3D%22none%22%20stroke%3D%22%23FF6B4A%22%20stroke-width%3D%224%22%20stroke-linejoin%3D%22round%22%2F%3E%3Cpath%20d%3D%22M21%2014H33%22%20stroke%3D%22%23FF6B4A%22%20stroke-width%3D%224%22%20stroke-linecap%3D%22round%22%20stroke-linejoin%3D%22round%22%2F%3E%3Cpath%20d%3D%22M21%2024H33%22%20stroke%3D%22%23FF6B4A%22%20stroke-width%3D%224%22%20stroke-linecap%3D%22round%22%20stroke-linejoin%3D%22round%22%2F%3E%3Cpath%20d%3D%22M21%2034H33%22%20stroke%3D%22%23FF6B4A%22%20stroke-width%3D%224%22%20stroke-linecap%3D%22round%22%20stroke-linejoin%3D%22round%22%2F%3E%3Ccircle%20cx%3D%2215%22%20cy%3D%2214%22%20r%3D%222%22%20fill%3D%22%23FF6B4A%22%2F%3E%3Ccircle%20cx%3D%2215%22%20cy%3D%2224%22%20r%3D%222%22%20fill%3D%22%23FF6B4A%22%2F%3E%3Ccircle%20cx%3D%2215%22%20cy%3D%2234%22%20r%3D%222%22%20fill%3D%22%23FF6B4A%22%2F%3E%3C%2Fsvg%3E'
const ICON_REVIEWS = 'data:image/svg+xml,%3Csvg%20xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22%20width%3D%2224%22%20height%3D%2224%22%20viewBox%3D%220%200%2048%2048%22%20fill%3D%22none%22%3E%3Cpath%20d%3D%22M44%206H4V36H13V41L23%2036H44V6Z%22%20fill%3D%22none%22%20stroke%3D%22%23fbbf24%22%20stroke-width%3D%224%22%20stroke-linecap%3D%22round%22%20stroke-linejoin%3D%22round%22%2F%3E%3Cpath%20d%3D%22M14%2021H34%22%20stroke%3D%22%23fbbf24%22%20stroke-width%3D%224%22%20stroke-linecap%3D%22round%22%20stroke-linejoin%3D%22round%22%2F%3E%3C%2Fsvg%3E'

const loading = ref(true)
const profile = ref({})
const perf = ref({})
const avgRating = ref('0.0')
const totalEarnings = ref('0.00')

const showMaxOrdersPicker = ref(false)
const maxOrdersEdit = ref(3)
const savingMax = ref(false)

const isOnline = computed(() => profile.value.isOnline === 1)

const capacityPercent = computed(() => {
  const current = profile.value.currentOrders || 0
  const max = profile.value.maxConcurrentOrders || 1
  return Math.min(100, current / max * 100)
})

const completionRate = computed(() => {
  return perf.value.completionRate ? Number(perf.value.completionRate).toFixed(1) : '0.0'
})

const onTimeRate = computed(() => {
  return perf.value.onTimeRate ? Number(perf.value.onTimeRate).toFixed(1) : '0.0'
})

onShow(async () => {
  await loadData()
})

async function loadData() {
  loading.value = true
  try {
    const [p, performance] = await Promise.all([
      runnerApi.getRunnerProfile({ showLoading: false }),
      runnerApi.getPerformance(store.userId, { showLoading: false })
    ])
    profile.value = p || {}
    perf.value = performance || {}
    avgRating.value = p?.avgRating ? Number(p.avgRating).toFixed(1) : '0.0'
    totalEarnings.value = (performance?.totalEarnings || 0).toFixed ? Number(performance.totalEarnings).toFixed(2) : '0.00'
  } catch (e) { /* handled */ }
  loading.value = false
}

async function toggleOnline() {
  try {
    if (isOnline.value) {
      await runnerApi.goOffline()
      profile.value.isOnline = 0
      uni.showToast({ title: '已下线', icon: 'success' })
    } else {
      await runnerApi.goOnline()
      profile.value.isOnline = 1
      uni.showToast({ title: '已上线', icon: 'success' })
    }
  } catch (e) { /* handled */ }
}

function openMaxOrdersPicker() {
  maxOrdersEdit.value = profile.value.maxConcurrentOrders || 3
  showMaxOrdersPicker.value = true
}

async function saveMaxOrders() {
  if (savingMax.value) return
  savingMax.value = true
  try {
    await runnerApi.setMaxOrders(maxOrdersEdit.value)
    profile.value.maxConcurrentOrders = maxOrdersEdit.value
    showMaxOrdersPicker.value = false
    uni.showToast({ title: '已更新', icon: 'success' })
  } catch (e) { /* handled */ }
  savingMax.value = false
}

function goReviews() { uni.navigateTo({ url: '/pages/runner-reviews/runner-reviews' }) }
function goCert() { uni.navigateTo({ url: '/pages/rider-cert/rider-cert' }) }
function onBack() { uni.navigateBack() }
</script>

<style scoped>
.page{width:100%;height:100vh;display:flex;flex-direction:column;background:var(--background);overflow:hidden}
.main-scroll{box-sizing:border-box;width:100%;padding:0 32rpx;padding-bottom:60rpx}

.section-title{font-size:24rpx;font-weight:500;color:var(--text-tertiary);letter-spacing:2rpx;margin-top:40rpx;margin-bottom:16rpx;padding-left:8rpx}

/* 上下线状态卡片 */
.status-card{display:flex;align-items:center;justify-content:space-between;padding:28rpx;border-radius:20rpx;margin-top:16rpx}
.status-card--online{background:linear-gradient(135deg,var(--primary-container),#fff5f3)}
.status-card--offline{background:var(--surface-raised);box-shadow:var(--shadow-sm)}
.status-card-left{display:flex;align-items:center;gap:16rpx}
.status-dot{width:20rpx;height:20rpx;border-radius:50%}
.status-dot--online{background:#34d399;box-shadow:0 0 12rpx rgba(52,211,153,.5)}
.status-dot--offline{background:var(--outline)}
.status-info{display:flex;flex-direction:column}
.status-title{font-size:30rpx;font-weight:600;color:var(--text-primary)}
.status-hint{font-size:22rpx;color:var(--text-secondary);margin-top:4rpx}

.status-toggle{width:88rpx;height:52rpx;border-radius:26rpx;background:var(--outline);display:flex;align-items:center;padding:4rpx;transition:background var(--duration-fast) var(--easing-out)}
.status-toggle--active{background:var(--primary)}
.status-toggle-thumb{width:44rpx;height:44rpx;border-radius:50%;background:#fff;box-shadow:var(--shadow-sm);transition:transform var(--duration-fast) var(--easing-out)}
.status-toggle--active .status-toggle-thumb{transform:translateX(36rpx)}

/* 接单容量 */
.capacity-card{background:var(--surface-raised);border-radius:20rpx;padding:28rpx;margin-top:16rpx;box-shadow:var(--shadow-sm)}
.capacity-header{display:flex;justify-content:space-between;align-items:center;margin-bottom:14rpx}
.capacity-label{font-size:24rpx;color:var(--text-secondary)}
.capacity-value{font-size:26rpx;font-weight:600;color:var(--text-primary)}
.capacity-bar{height:12rpx;background:var(--surface);border-radius:6rpx;overflow:hidden}
.capacity-fill{height:100%;background:var(--primary);border-radius:6rpx;transition:width .5s var(--ease-spring);min-width:0;position:relative}
.capacity-fill::after{content:'';position:absolute;right:0;top:50%;transform:translateY(-50%);width:12rpx;height:12rpx;background:#fff;border-radius:50%;opacity:.6}

/* 统计网格 */
.stats-grid{display:grid;grid-template-columns:repeat(3,1fr);gap:16rpx}
.stat-card{background:var(--surface-raised);border-radius:20rpx;padding:28rpx 20rpx;text-align:center;box-shadow:var(--shadow-sm)}
.stat-value{font-size:36rpx;font-weight:700;color:var(--text-primary);display:block;margin-bottom:8rpx}
.stat-value--rate{color:var(--primary)}
.stat-label{font-size:22rpx;color:var(--text-secondary)}

/* 评分卡片 */
.rating-card{display:flex;align-items:center;gap:16rpx;background:var(--surface-raised);border-radius:20rpx;padding:28rpx;margin-top:24rpx;box-shadow:var(--shadow-sm)}
.rating-icon-wrap{width:72rpx;height:72rpx;border-radius:50%;background:rgba(251,191,36,.12);display:flex;align-items:center;justify-content:center}
.rating-body{flex:1}
.rating-value{font-size:32rpx;font-weight:700;color:var(--text-primary);display:block}
.rating-label{font-size:22rpx;color:var(--text-secondary);margin-top:4rpx;display:block}

/* 菜单 */
.menu-list{background:var(--surface-raised);border-radius:var(--radius-card);overflow:hidden;box-shadow:var(--shadow-sm);margin-top:16rpx}
.menu-item{display:flex;align-items:center;padding:28rpx 28rpx;border-bottom:1rpx solid var(--surface-hover)}
.menu-item--last{border-bottom:none}
.menu-item:active{background:var(--surface)}
.menu-icon{width:64rpx;height:64rpx;border-radius:50%;display:flex;align-items:center;justify-content:center;margin-right:20rpx;flex-shrink:0}
.menu-icon--orders{background:var(--primary-container)}
.menu-icon--star{background:rgba(251,191,36,.12)}
.menu-icon--cert{background:var(--primary-container)}
.menu-label{flex:1;font-size:28rpx;font-weight:500;color:var(--text-primary)}
.menu-value{font-size:24rpx;color:var(--text-secondary);margin-right:12rpx}

/* 最大接单数选择器 */
.picker-overlay{position:fixed;top:0;left:0;right:0;bottom:0;background:rgba(28,27,26,.45);z-index:1000;display:flex;align-items:flex-end;animation:fadeIn .25s var(--easing-out)}
@keyframes fadeIn{from{opacity:0}to{opacity:1}}
.picker-panel{width:100%;max-height:70vh;display:flex;flex-direction:column;background:var(--surface-raised);border-radius:28rpx 28rpx 0 0;padding:40rpx 32rpx;animation:slideUp .35s var(--ease-spring);padding-bottom:calc(40rpx + env(safe-area-inset-bottom))}
@keyframes slideUp{from{transform:translateY(100%)}to{transform:translateY(0)}}
.picker-title{font-size:32rpx;font-weight:600;color:var(--text-primary);text-align:center;margin-bottom:32rpx}
.picker-stepper{display:flex;align-items:center;justify-content:center;gap:40rpx;margin-bottom:24rpx}
.picker-btn{width:88rpx;height:88rpx;border-radius:50%;background:var(--surface);display:flex;align-items:center;justify-content:center;font-size:44rpx;font-weight:600;color:var(--primary)}
.picker-btn:active:not(.picker-btn--disabled){background:var(--primary-container)}
.picker-btn--disabled{color:var(--text-tertiary);pointer-events:none}
.picker-value{font-size:56rpx;font-weight:700;color:var(--text-primary);min-width:80rpx;text-align:center}
.picker-hint{font-size:24rpx;color:var(--text-tertiary);text-align:center;margin-bottom:32rpx}
.picker-actions{display:flex;gap:20rpx}
.picker-action{flex:1;height:88rpx;border-radius:44rpx;display:flex;align-items:center;justify-content:center}
.picker-action--cancel{background:var(--surface)}
.picker-action--cancel text{font-size:28rpx;color:var(--text-secondary);font-weight:500}
.picker-action--confirm{background:var(--primary);box-shadow:var(--shadow-primary)}
.picker-action--confirm text{font-size:28rpx;color:#fff;font-weight:600}
.picker-action:active{transform:scale(.96)}

.loading-state{display:flex;align-items:center;justify-content:center;padding:120rpx 0}
.loading-text{font-size:28rpx;color:var(--text-tertiary)}
.bottom-placeholder{height:60rpx}
</style>
