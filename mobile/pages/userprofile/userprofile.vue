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
      <view class="nav-title-wrap"><text class="nav-title-text">我的</text></view>
      <template v-slot:right>
        <view class="nav-btn" @click="goMessages">
          <iconpark-icon name="notification-filled" size="24" color="#FF6B4A" />
        </view>
      </template>
    </uni-nav-bar>

    <scroll-view class="main-scroll" :style="{ height: scrollHeight + 'px' }" scroll-y enhanced :show-scrollbar="false">
      <!-- 用户信息卡片 -->
      <uni-card :isShadow="true" :shadow="'0 4rpx 20rpx rgba(255, 107, 74, 0.05)'" :margin="'16rpx 0 0 0'" :spacing="'32rpx 36rpx'" :border="false" @click="onProfileTap">
        <template v-slot:title>
          <view class="profile-header">
            <view class="profile-avatar">
              <image v-if="store.userInfo.avatarUrl" class="profile-avatar-img" :src="store.userInfo.avatarUrl" mode="aspectFill" />
              <text v-else class="profile-avatar-text">{{ store.avatarText }}</text>
            </view>
            <view class="profile-info">
              <text class="profile-name">{{ store.displayName }}</text>
              <text class="profile-dept">{{ store.userInfo.campus || '未设置学院' }}</text>
              <text class="profile-bio" v-if="store.userInfo.signature">{{ store.userInfo.signature }}</text>
            </view>
            <iconpark-icon name="right" size="18" color="#8F8D88" />
          </view>
        </template>
      </uni-card>

      <!-- 钱包卡片 -->
      <uni-card title="我的钱包" :isShadow="true" :shadow="'0 4rpx 20rpx rgba(255, 107, 74, 0.05)'" :margin="'48rpx 0 0 0'" :spacing="'0 0 0 0'" :padding="'0'" :border="false">
        <view class="wallet-card">
          <view class="wallet-glow wallet-glow--1"></view>
          <view class="wallet-glow wallet-glow--2"></view>
          <view class="wallet-content">
            <view class="wallet-left">
              <text class="wallet-label">可用余额 (元)</text>
              <text class="wallet-balance">{{ store.balanceText }}</text>
            </view>
            <view class="wallet-action" @click="onRecharge">
              <text>充值 / 提现</text>
            </view>
          </view>
        </view>
      </uni-card>

      <!-- ========== 配送员专属内容 ========== -->
      <template v-if="store.isCertifiedRunner">
        <!-- 骑手功能菜单 -->
        <view class="func-list">
          <view class="func-item func-item--last" @click="onMenuItem('dashboard')">
            <view class="func-icon func-icon--blue"><iconpark-icon name="analysis" size="22" color="#FF6B4A" /></view>
            <text class="func-name">我的骑手</text>
            <iconpark-icon name="right" size="16" color="#D4D2CC" />
          </view>
        </view>
      </template>

      <!-- ========== 通用功能菜单 ========== -->
      <view class="func-list">
        <view class="func-item" @click="onCertify">
          <view class="func-icon func-icon--blue"><iconpark-icon name="medal-filled" size="22" color="#FF6B4A" /></view>
          <text class="func-name">实名认证</text>
          <view class="func-status">
            <text :class="userCertClass">{{ store.certifyStatusLabel }}</text>
          </view>
          <iconpark-icon name="right" size="16" color="#D4D2CC" />
        </view>
        <view class="func-item" @click="onMenuItem('bills')">
          <view class="func-icon func-icon--blue"><iconpark-icon name="wallet-filled" size="22" color="#FF6B4A" /></view>
          <text class="func-name">我的账单</text>
          <iconpark-icon name="right" size="16" color="#D4D2CC" />
        </view>
        <view class="func-item" @click="onMenuItem('rider')" v-if="store.isCertified && !store.isCertifiedRunner">
          <view class="func-icon func-icon--blue"><iconpark-icon name="deliveryTruck" size="22" color="#FF6B4A" /></view>
          <text class="func-name">申请成为配送员</text>
          <view class="func-status">
            <text :class="runnerCertClass">{{ runnerCertLabel }}</text>
          </view>
          <iconpark-icon name="right" size="16" color="#D4D2CC" />
        </view>
        <view class="func-item" @click="onMenuItem('address')">
          <view class="func-icon func-icon--blue"><iconpark-icon name="addressBook" size="22" color="#FF6B4A" /></view>
          <text class="func-name">收货地址管理</text>
          <iconpark-icon name="right" size="16" color="#D4D2CC" />
        </view>
        <view class="func-item" @click="onMenuItem('privacy')">
          <view class="func-icon func-icon--blue"><iconpark-icon name="locked-filled" size="22" color="#FF6B4A" /></view>
          <text class="func-name">隐私与安全设置</text>
          <iconpark-icon name="right" size="16" color="#D4D2CC" />
        </view>
        <view class="func-item func-item--last" @click="onMenuItem('support')">
          <view class="func-icon func-icon--blue"><iconpark-icon name="message" size="22" color="#FF6B4A" /></view>
          <text class="func-name">联系客服</text>
          <iconpark-icon name="right" size="16" color="#D4D2CC" />
        </view>
      </view>

      <!-- 退出登录 -->
      <view class="logout-btn" @click="onLogout">
        <text>退出登录</text>
      </view>

      <view class="bottom-placeholder"></view>
    </scroll-view>

    <custom-tabbar :selected="4" />
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useStore } from '@/store/index.js'
import { CERTIFY_STATUS } from '@/utils/constants.js'
import { userApi } from '@/api'
import CustomTabbar from '@/components/custom-tabbar/custom-tabbar.vue'

const sysInfo = uni.getSystemInfoSync()
const scrollHeight = sysInfo.windowHeight - sysInfo.statusBarHeight - 44
const store = useStore()

const avgRating = ref(0)

onShow(async () => {
  if (store.isLoggedIn) {
    store.fetchUserInfo().catch(() => {})
  }
})

const userCertClass = computed(() => {
  const map = { 0: 'cert--none', 1: 'cert--pending', 2: 'cert--done', 3: 'cert--reject' }
  return map[store.userInfo.isCertify] || 'cert--none'
})
const runnerCertLabel = computed(() => CERTIFY_STATUS[store.userInfo.verifyStatus] || CERTIFY_STATUS[0])
const runnerCertClass = computed(() => {
  const map = { 0: 'cert--none', 1: 'cert--pending', 2: 'cert--done', 3: 'cert--reject' }
  return map[store.userInfo.verifyStatus] || 'cert--none'
})

function onCertify() {
  uni.navigateTo({ url: '/pages/certify/certify' })
}

function goMessages() { uni.switchTab({ url: '/pages/message/message' }) }

function onMenuItem(key) {
  const routes = {
    bills: '/pages/bills/bills',
    rider: '/pages/rider-cert/rider-cert',
    address: '/pages/address-list/address-list',
    privacy: '/pages/privacy-security/privacy-security',
    dashboard: '/pages/runner-dashboard/runner-dashboard',
    reviews: '/pages/runner-reviews/runner-reviews',
    support: null
  }
  if (routes[key]) {
    uni.navigateTo({ url: routes[key] })
  } else {
    uni.showToast({ title: '功能开发中', icon: 'none' })
  }
}

function onProfileTap() { uni.navigateTo({ url: '/pages/profile-settings/profile-settings' }) }
function onRecharge() { uni.navigateTo({ url: '/pages/wallet/wallet' }) }

async function onLogout() {
  const res = await new Promise(r => {
    uni.showModal({
      title: '退出登录',
      content: '确定要退出当前账号吗？',
      success: r2 => r(r2.confirm)
    })
  })
  if (!res) return
  await store.logout()
  uni.reLaunch({ url: '/pages/login/login' })
}
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

.main-scroll { box-sizing: border-box; width: 100%; padding: 0 32rpx; padding-bottom: 180rpx; }

:deep(.uni-card) { border-radius: var(--radius-card) !important; }
.profile-header { display: flex; align-items: center; gap: 28rpx; width: 100%; }
.profile-avatar { width: 112rpx; height: 112rpx; border-radius: 50%; background: linear-gradient(135deg, var(--primary), var(--secondary)); display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.profile-avatar-img { width: 100%; height: 100%; border-radius: 50%; object-fit: cover; }
.profile-avatar-text { font-size: 44rpx; font-weight: 700; color: #fff; }
.profile-info { flex: 1; }
.profile-name { font-size: 36rpx; font-weight: 700; color: var(--text-primary); display: block; margin-bottom: 6rpx; }
.profile-dept { font-size: 26rpx; color: var(--text-secondary); }
.profile-bio { font-size: 24rpx; color: var(--text-tertiary); margin-top: 4rpx; display: block; }

/* 钱包卡片 — 青绿渐变 */
.wallet-card { background: linear-gradient(135deg, var(--secondary) 0%, #1FA89B 50%, #0D9488 100%); border-radius: var(--radius-lg); padding: 36rpx; position: relative; overflow: hidden; margin: 0; }
.wallet-glow { position: absolute; border-radius: 50%; background: rgba(255,255,255,0.1); }
.wallet-glow--1 { width: 280rpx; height: 280rpx; top: -140rpx; right: -60rpx; }
.wallet-glow--2 { width: 200rpx; height: 200rpx; bottom: -60rpx; left: -30rpx; }
.wallet-content { position: relative; z-index: 1; display: flex; justify-content: space-between; align-items: flex-end; }
.wallet-label { font-size: 24rpx; font-weight: 400; color: rgba(255,255,255,0.8); display: block; margin-bottom: 10rpx; letter-spacing: -0.01em; }
.wallet-balance { font-size: 48rpx; font-weight: 600; color: #fff; letter-spacing: -0.02em; display: block; }
.wallet-action { padding: 14rpx 28rpx; background: rgba(255,255,255,0.2); border-radius: var(--radius-full); transition: background var(--duration-fast) var(--easing-out); }
.wallet-action:active { background: rgba(255,255,255,0.35); }
.wallet-action text { font-size: 24rpx; font-weight: 500; color: #fff; }

.func-list { margin-top: 24rpx; background: var(--surface-raised); border-radius: var(--radius-card); box-shadow: var(--shadow-sm); overflow: hidden; }
.func-item { display: flex; align-items: center; padding: 30rpx 32rpx; border-bottom: 1rpx solid var(--outline-light); transition: background var(--duration-fast) var(--easing-out); }
.func-item--last { border-bottom: none; }
.func-item:active { background: var(--surface-hover); }
.func-icon { width: 68rpx; height: 68rpx; border-radius: 50%; display: flex; align-items: center; justify-content: center; margin-right: 24rpx; flex-shrink: 0; }
.func-icon--blue { background: var(--primary-container); }
.func-icon--green { background: var(--success-container); }
.func-icon--yellow { background: var(--accent-container); }
.func-name { flex: 1; font-size: 30rpx; font-weight: 500; color: var(--text-primary); }
.func-status { margin-right: 8rpx; }
.cert--done { font-size: 24rpx; color: var(--success); }
.cert--pending { font-size: 24rpx; color: var(--warning); }
.cert--none { font-size: 24rpx; color: var(--text-tertiary); }
.cert--reject { font-size: 24rpx; color: var(--error); }

.logout-btn { margin-top: 24rpx; display: flex; align-items: center; justify-content: center; padding: 30rpx; background: var(--surface-raised); border-radius: var(--radius-full); box-shadow: var(--shadow-sm); transition: all var(--duration-fast) var(--easing-out); }
.logout-btn:active { background: var(--error-container); transform: scale(0.98); }
.logout-btn text { font-size: 30rpx; font-weight: 500; color: var(--error); }
.bottom-placeholder { height: 160rpx; }
</style>
