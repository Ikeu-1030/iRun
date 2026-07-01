<template>
  <view class="page">
    <uni-nav-bar title="隐私与安全" backgroundColor="#FAFAF8" :border="false" statusBar fixed leftIcon="left" @clickLeft="onBack" color="#1C1B1A" />

    <scroll-view class="main-scroll" :style="{ height: scrollHeight + 'px' }" scroll-y enhanced :show-scrollbar="false">
      <view class="section-title">安全设置</view>
      <view class="menu-list">
        <view class="menu-item" @click="onNavigate('/pages-user/phone-edit/phone-edit')">
          <view class="menu-icon menu-icon--blue"><iconpark-icon name="phone" size="20" color="#FF6B4A" /></view>
          <view class="menu-body">
            <text class="menu-label">修改手机号</text>
            <text class="menu-note">{{ store.userInfo.phone || '未绑定' }}</text>
          </view>
          <iconpark-icon name="right" size="16" color="#D4D2CC" />
        </view>
        <view class="menu-item" @click="onPayPassword">
          <view class="menu-icon menu-icon--green"><custom-icon name="fingerprint" size="36" /></view>
          <view class="menu-body">
            <text class="menu-label">{{ payPasswordLabel }}</text>
            <text class="menu-note">{{ payPasswordNote }}</text>
          </view>
          <view class="menu-right" v-if="!checkingPayStatus">
            <text v-if="!hasPayPassword" class="go-verify">去设置</text>
            <text v-else class="go-modify">去修改</text>
          </view>
          <iconpark-icon name="right" size="16" color="#D4D2CC" />
        </view>
        <view class="menu-item" v-if="!isWechatUser" @click="onNavigate('/pages-user/password-manage/password-manage?mode=login')">
          <view class="menu-icon menu-icon--purple"><iconpark-icon name="keyEdit" size="20" color="#8b5cf6" /></view>
          <view class="menu-body">
            <text class="menu-label">修改登录密码</text>
            <text class="menu-note">定期修改更安全</text>
          </view>
          <iconpark-icon name="right" size="16" color="#D4D2CC" />
        </view>
      </view>

      <view class="footer-info">
        <iconpark-icon name="locked" size="18" color="#8F8D88" />
        <text class="footer-text">保障您的账户与资产安全</text>
      </view>
      <view class="bottom-placeholder"></view>
    </scroll-view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useStore } from '@/store/index.js'
import { userApi } from '@/api'

const store = useStore()
const sysInfo = uni.getSystemInfoSync()
const scrollHeight = sysInfo.windowHeight - sysInfo.statusBarHeight - 44

const isWechatUser = computed(() => store.userInfo?.registerType === 2)

const hasPayPassword = ref(store.hasPayPassword)
const checkingPayStatus = ref(true)

const payPasswordLabel = computed(() => hasPayPassword.value ? '修改支付密码' : '设置支付密码')
const payPasswordNote = computed(() => hasPayPassword.value ? '定期修改更安全' : '保护资金安全，交易必用')

async function checkPayPasswordStatus() {
  try {
    const result = await userApi.getPayPasswordStatus({ showLoading: false })
    hasPayPassword.value = result === true
    // 同步更新 store 标记
    if (result === true) store.markPayPasswordSet()
  } catch (e) {
    hasPayPassword.value = store.hasPayPassword
  }
  checkingPayStatus.value = false
}

function onPayPassword() {
  const mode = hasPayPassword.value ? 'change' : 'set'
  uni.navigateTo({ url: `/pages/password-manage/password-manage?mode=${mode}` })
}

function onBack() { uni.navigateBack() }
function onNavigate(url) { uni.navigateTo({ url }) }

onShow(() => { checkPayPasswordStatus() })
checkPayPasswordStatus()
</script>

<style scoped>
.page{width:100%;height:100vh;display:flex;flex-direction:column;background:var(--background);overflow:hidden}
.main-scroll{box-sizing:border-box;width:100%;padding:0 32rpx;padding-bottom:60rpx}
.section-title{font-size:24rpx;font-weight:500;color:var(--text-tertiary);letter-spacing:2rpx;margin-top:40rpx;margin-bottom:16rpx;padding-left:8rpx}
.menu-list{background:var(--surface-raised);border-radius:var(--radius-lg);overflow:hidden;box-shadow:var(--shadow-sm)}
.menu-item{display:flex;align-items:center;padding:28rpx 28rpx;border-bottom:1rpx solid var(--outline-light)}
.menu-item:last-child{border-bottom:none}
.menu-item:active{background:var(--surface)}
.menu-icon{width:64rpx;height:64rpx;border-radius:50%;display:flex;align-items:center;justify-content:center;margin-right:20rpx;flex-shrink:0}
.menu-icon--blue{background:var(--primary-container)}
.menu-icon--green{background:rgba(52,211,153,.1)}
.menu-icon--orange{background:#fff7ed}
.menu-icon--purple{background:#f5f3ff}
.menu-body{flex:1;min-width:0}
.menu-label{font-size:28rpx;font-weight:500;color:var(--text-primary);display:block}
.menu-note{font-size:24rpx;color:var(--text-secondary);margin-top:4rpx;display:block}
.menu-right{display:flex;align-items:center;gap:8rpx;flex-shrink:0}
.go-verify{font-size:24rpx;color:var(--primary);font-weight:500}
.go-modify{font-size:24rpx;color:var(--warning);font-weight:500}
.footer-info{display:flex;align-items:center;justify-content:center;padding:48rpx 0;gap:10rpx;opacity:.6}
.footer-text{font-size:24rpx;color:var(--text-tertiary)}
.bottom-placeholder{height:60rpx}
</style>
