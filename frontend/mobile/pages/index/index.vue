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
      <view class="nav-title-wrap"><text class="nav-title-text">首页</text></view>
      <template v-slot:right>
        <view class="nav-btn" @click="goMessages">
          <iconpark-icon name="notification-filled" size="24" color="#FF6B4A" />
          <view v-if="unreadCount > 0" class="nav-notify-dot animate-pulse-glow"></view>
        </view>
      </template>
    </uni-nav-bar>

    <scroll-view class="main-scroll" :style="{ height: scrollHeight + 'px' }" scroll-y enhanced :show-scrollbar="false">
      <!-- 认证状态横幅 -->
      <view v-if="store.isLoggedIn && store.userInfo.isCertify !== 2" class="cert-banner" :class="'cert-banner--' + store.userInfo.isCertify" @click="goCertify">
        <view class="cert-banner-icon">
          <iconpark-icon v-if="store.userInfo.isCertify === 0" name="locked-filled" size="20" color="#e67e22" />
          <iconpark-icon v-else-if="store.userInfo.isCertify === 1" name="clock-filled" size="20" color="#FF6B4A" />
          <iconpark-icon v-else name="closeempty" size="20" color="#ba1a1a" />
        </view>
        <view class="cert-banner-body">
          <text class="cert-banner-text" v-if="store.userInfo.isCertify === 0">请先完成实名认证，解锁全部功能</text>
          <text class="cert-banner-text" v-else-if="store.userInfo.isCertify === 1">认证审核中，通过后解锁全部功能</text>
          <text class="cert-banner-text" v-else>认证被驳回，请重新提交</text>
        </view>
        <view class="cert-banner-arrow">
          <text v-if="store.userInfo.isCertify === 0 || store.userInfo.isCertify === 3">去认证</text>
          <iconpark-icon name="right" size="14" color="#737784" />
        </view>
      </view>

      <!-- 搜索栏 -->
      <view class="search-section">
        <uni-search-bar
          v-model="searchValue"
          placeholder="输入快递单号、餐厅名称或服务需求..."
          radius="48"
          bgColor="#ffffff"
          :clearButton="'none'"
          :cancelButton="'none'"
          @confirm="onSearch"
        >
          <template v-slot:searchIcon>
            <iconpark-icon name="search" size="18" color="#737784" />
          </template>
        </uni-search-bar>
      </view>

      <!-- 活动横幅 -->
      <view class="banner-section">
        <view class="banner-bg"></view>
        <view class="banner-overlay">
          <view class="banner-tag"><text>限时活动</text></view>
          <text class="banner-title">新生特惠季</text>
          <text class="banner-desc">首单免费帮取，体验飞速服务</text>
        </view>
      </view>

      <uni-notice-bar
        class="mt-24"
        showIcon
        scrollable
        text="🎉 代购物品新上线！前100单免配送费，快来体验吧～"
        backgroundColor="transparent"
        color="#FF6B4A"
      />

      <!-- 四大服务入口 -->
      <view class="service-grid">
        <view class="service-card animate-scale-pop" v-for="(service, si) in services" :key="service.typeValue" :style="{ animationDelay: (si * 0.1) + 's' }" @click="onService(service.typeValue)">
          <view class="service-card-icon" :class="'service-card-icon--' + service.color">
            <iconpark-icon :name="service.icon" size="28" :color="service.iconColor" />
          </view>
          <text class="service-card-title">{{ service.title }}</text>
          <text class="service-card-desc">{{ service.desc }}</text>
        </view>
      </view>

      <!-- 热门服务 -->
      <view class="section-header mt-32">
        <text class="section-title">热门服务</text>
        <view class="section-more" @click="onViewAll">
          <text>查看全部</text>
          <iconpark-icon name="right" size="14" color="#FF6B4A" />
        </view>
      </view>

      <uni-card
        title="奶茶咖啡代取"
        :subTitle="'想喝奶茶咖啡？帮你代取送到手'"
        :isShadow="true"
        :shadow="'0 4rpx 20rpx rgba(37, 99, 235, 0.04)'"
        :margin="'0 0 24rpx 0'"
        :padding="'0'"
        :spacing="'24rpx 32rpx'"
        :border="false"
        @click="onHotCoffee"
      >
        <view class="hot-card-content">
          <text class="hot-card-fee">基础配送费 ¥5.00</text>
          <uni-tag text="代取餐食" type="primary" size="small" :inverted="true" customStyle="border-color:#FFD1C7;color:#FF6B4A;background:#FFF0ED;" />
        </view>
      </uni-card>

      <uni-card
        title="资料打印"
        :subTitle="'上传文件即可打印，黑白彩印任选'"
        :isShadow="true"
        :shadow="'0 4rpx 20rpx rgba(37, 99, 235, 0.04)'"
        :margin="'0 0 24rpx 0'"
        :padding="'0'"
        :spacing="'24rpx 32rpx'"
        :border="false"
        @click="onHotPrint"
      >
        <view class="hot-card-content">
          <text class="hot-card-fee">基础配送费 ¥5.00</text>
          <uni-tag text="校内代办" type="warning" size="small" :inverted="true" customStyle="border-color:#FED7AA;color:#EA580C;background:#FFF7ED;" />
        </view>
      </uni-card>

      <uni-card
        title="纸品速达"
        :subTitle="'校园超市纸品代购，即刻送达'"
        :isShadow="true"
        :shadow="'0 4rpx 20rpx rgba(37, 99, 235, 0.04)'"
        :margin="'0 0 24rpx 0'"
        :padding="'0'"
        :spacing="'24rpx 32rpx'"
        :border="false"
        @click="onHotPaperExpress"
      >
        <view class="hot-card-content">
          <text class="hot-card-fee">基础配送费 ¥5.00 + 商品费</text>
          <uni-tag text="代购物品" type="warning" size="small" :inverted="true" customStyle="border-color:#A5F3FC;color:#0891B2;background:#ECFEFF;" />
        </view>
      </uni-card>

      <view class="bottom-placeholder"></view>
    </scroll-view>

    <!-- 快速发布按钮 -->
    <view class="custom-fab animate-bounce-in delay-6" @click="onFabClick">
      <iconpark-icon name="plusempty" size="28" color="#fff" />
    </view>

    <custom-tabbar :selected="0" />
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useStore } from '@/store/index.js'
import { taskApi, notificationApi } from '@/api'
import { requireCertified } from '@/utils/error'
import CustomTabbar from '@/components/custom-tabbar/custom-tabbar.vue'
import { showToast } from '@/utils/toast'

const store = useStore()
const sysInfo = uni.getSystemInfoSync()
const scrollHeight = sysInfo.windowHeight - sysInfo.statusBarHeight - 44
const searchValue = ref('')
const unreadCount = ref(0)

const services = [
  { typeValue: 1, title: '代取快递', desc: '驿站包裹极速达', icon: 'express', color: 'blue', iconColor: '#3871d6' },
  { typeValue: 2, title: '代取餐食', desc: '食堂外卖送到寝', icon: 'fire', color: 'orange', iconColor: '#e67e22' },
  { typeValue: 3, title: '校内代办', desc: '急送跑腿帮办事', icon: 'campusErrand', color: 'green', iconColor: '#4c5e86' },
  { typeValue: 4, title: '代购物品', desc: '超市代购送到寝', icon: 'shop', color: 'teal', iconColor: '#0891B2' }
]

// 加载通知未读数
async function loadUnread() {
  try {
    const data = await notificationApi.getNotifications({ isRead: 0, page: 1, size: 1 })
    unreadCount.value = data?.total || 0
  } catch (e) { /* ignore */ }
}

async function onSearch(e) {
  const keyword = e.value?.trim()
  if (!keyword) return
  try {
    const res = await taskApi.searchTasks({ keyword, page: 1, size: 20 })
    if (res.records?.length) {
      showToast(`找到 ${res.total} 个任务`)
    } else {
      uni.showToast({ title: '未找到匹配任务', icon: 'none' })
    }
  } catch (e) { /* handled */ }
}

function onService(typeValue) {
  if (!requireCertified(store)) return
  uni.navigateTo({ url: `/pages/service-publish/service-publish?type=${typeValue}` })
}

function goCertify() {
  uni.navigateTo({ url: '/pages/certify/certify' })
}

function onViewAll() {
  uni.showToast({ title: '更多服务开发中', icon: 'none' })
}

function onHotCoffee() {
  if (!requireCertified(store)) return
  uni.navigateTo({ url: '/pages/coffee-order/coffee-order' })
}

function onHotPrint() {
  if (!requireCertified(store)) return
  uni.navigateTo({ url: '/pages/print-order/print-order' })
}

function onHotPaperExpress() {
  if (!requireCertified(store)) return
  uni.navigateTo({ url: '/pages/paper-express/paper-express' })
}

function onNearbyTask(task) {
  uni.navigateTo({ url: `/pages/order-waiting/order-waiting?taskId=${task.taskId}` })
}

function onFabClick() {
  if (!requireCertified(store)) return
  uni.navigateTo({ url: '/pages/general-publish/general-publish?type=通用代办' })
}

function goMessages() {
  uni.switchTab({ url: '/pages/message/message' })
}

function goOrders() {
  uni.switchTab({ url: '/pages/orders/orders' })
}

onShow(() => {
  if (store.isLoggedIn && !store.userInfo.ready) {
    store.fetchUserInfo().catch(() => {})
  }
  loadUnread()
})

loadUnread()
</script>

<style scoped>
.page { width: 100%; height: 100vh; display: flex; flex-direction: column; background: var(--background); overflow: hidden; }

/* Nav */
.nav-avatar-wrap { width: 68rpx; height: 68rpx; border-radius: 50%; background: var(--primary-container); display: flex; align-items: center; justify-content: center; }
.nav-avatar { width: 56rpx; height: 56rpx; border-radius: 50%; background: var(--primary); display: flex; align-items: center; justify-content: center; }
.nav-avatar-img { width: 100%; height: 100%; border-radius: 50%; object-fit: cover; }
.nav-avatar-text { font-size: 26rpx; font-weight: 700; color: #fff; }
.nav-title-wrap { display: flex; align-items: center; justify-content: center; }
.nav-title-text { font-size: 34rpx; font-weight: 600; color: var(--text-primary); }
.nav-btn { width: 68rpx; height: 68rpx; display: flex; align-items: center; justify-content: center; border-radius: 50%; position: relative; }
.nav-btn:active { background: var(--primary-container); }
.nav-notify-dot { position: absolute; top: 14rpx; right: 14rpx; width: 14rpx; height: 14rpx; background: var(--error); border-radius: 50%; border: 2rpx solid var(--background); }

.main-scroll { box-sizing: border-box; width: 100%; padding: 0 32rpx; padding-bottom: 180rpx; }

.search-section { margin-top: 16rpx; }

/* 认证横幅 */
.cert-banner { display: flex; align-items: center; gap: 14rpx; background: var(--surface-raised); border-radius: var(--radius-md); padding: 20rpx 24rpx; margin-top: 16rpx; box-shadow: var(--shadow-sm); }
.cert-banner:active { background: var(--surface-hover); }
.cert-banner--0 { border-left: 6rpx solid var(--warning); }
.cert-banner--1 { border-left: 6rpx solid var(--primary); }
.cert-banner--3 { border-left: 6rpx solid var(--error); }
.cert-banner-icon { width: 52rpx; height: 52rpx; border-radius: 50%; background: var(--surface); display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.cert-banner-body { flex: 1; min-width: 0; }
.cert-banner-text { font-size: var(--text-sm); color: var(--text-secondary); line-height: 1.4; }
.cert-banner-arrow { display: flex; align-items: center; gap: 4rpx; flex-shrink: 0; }
.cert-banner-arrow text { font-size: var(--text-sm); font-weight: 500; color: var(--primary); }
.search-section :deep(.uni-searchbar__box) { border-radius: 48rpx !important; box-shadow: var(--shadow-sm); border: 1rpx solid var(--outline-light); height: 88rpx !important; }
.search-section :deep(.uni-searchbar) { padding: 0 !important; }

/* Banner — 珊瑚→青绿渐变 */
.banner-section { margin-top: 24rpx; height: 320rpx; border-radius: var(--radius-lg); overflow: hidden; position: relative; }
.banner-bg { width: 100%; height: 100%; position: absolute; top: 0; left: 0; background: linear-gradient(135deg, var(--primary) 0%, #FF8B72 35%, var(--secondary) 80%, #1FA89B 100%); background-size: 200% 200%; animation: bannerShift 8s ease-in-out infinite alternate; }
@keyframes bannerShift { 0% { background-position: 0% 0%; } 100% { background-position: 100% 100%; } }
.banner-bg::before { content: ''; position: absolute; top: -60rpx; right: -30rpx; width: 240rpx; height: 240rpx; background: rgba(255,255,255,0.12); border-radius: 50%; }
.banner-bg::after { content: ''; position: absolute; bottom: -50rpx; left: 50rpx; width: 140rpx; height: 140rpx; background: rgba(255,255,255,0.08); border-radius: 50%; }
.banner-overlay { position: absolute; inset: 0; background: linear-gradient(to right, rgba(28,27,26,0.45), transparent 60%); display: flex; flex-direction: column; justify-content: center; padding-left: 40rpx; }
.banner-tag { display: inline-flex; padding: 6rpx 20rpx; background: rgba(255,255,255,0.20); backdrop-filter: blur(8rpx); -webkit-backdrop-filter: blur(8rpx); border-radius: var(--radius-full); margin-bottom: 16rpx; align-self: flex-start; border: 1px solid rgba(255,255,255,0.25); }
.banner-tag text { font-size: 22rpx; font-weight: 600; color: #fff; letter-spacing: -0.01em; }
.banner-title { font-size: 56rpx; font-weight: 600; color: #fff; margin-bottom: 8rpx; letter-spacing: -0.015em; }
.banner-desc { font-size: 28rpx; font-weight: 400; color: rgba(255,255,255,0.85); }

/* 服务网格 — 粉彩着色卡片 */
.service-grid { display: flex; flex-wrap: wrap; justify-content: space-between; margin-top: 32rpx; }
.service-card { width: calc((100% - 24rpx) / 2); border-radius: var(--radius-lg); padding: 36rpx 28rpx 32rpx; display: flex; flex-direction: column; align-items: flex-start; margin-bottom: 24rpx; box-sizing: border-box; transition: transform var(--duration-fast) var(--easing-out); }
.service-card:active { transform: scale(0.97); }
.service-card:nth-child(1) { background: var(--primary-container); }
.service-card:nth-child(2) { background: var(--accent-container); }
.service-card:nth-child(3) { background: var(--secondary-container); }
.service-card:nth-child(4) { background: var(--info-container); }
.service-card-icon { width: 80rpx; height: 80rpx; border-radius: 24rpx; display: flex; align-items: center; justify-content: center; margin-bottom: 22rpx; }
.service-card-icon--blue { background: rgba(255,107,74,0.15); }
.service-card-icon--orange { background: rgba(255,179,71,0.18); }
.service-card-icon--green { background: rgba(46,196,182,0.15); }
.service-card-icon--teal { background: rgba(8,145,178,0.15); }
.service-card-title { font-size: 30rpx; font-weight: 600; color: var(--text-primary); margin-bottom: 8rpx; letter-spacing: -0.01em; }
.service-card-desc { font-size: var(--text-sm); color: var(--text-secondary); line-height: 1.3; }

.section-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20rpx; margin-top: 40rpx; }
.section-title { font-size: var(--text-lg); font-weight: 700; color: var(--text-primary); }
.section-more { display: flex; align-items: center; gap: 4rpx; }
.section-more text { font-size: 26rpx; font-weight: 500; color: var(--primary); }

.hot-card-content { display: flex; align-items: center; justify-content: flex-end; gap: 16rpx; padding-top: 8rpx; }
.hot-card-fee { font-size: 24rpx; color: var(--text-secondary); }
.hot-card-unit { font-size: 22rpx; font-weight: 500; color: var(--text-tertiary); margin-left: 4rpx; }

.loading-hint { text-align: center; padding: 32rpx; }
.loading-hint text { font-size: var(--text-sm); color: var(--text-tertiary); }

:deep(.uni-card) { border-radius: var(--radius-card) !important; overflow: hidden; }
:deep(.uni-card__header) { border-bottom: none !important; }
:deep(.uni-card__header-content-title) { font-size: 32rpx !important; font-weight: 600 !important; color: var(--text-primary) !important; }
:deep(.uni-card__header-content-subtitle) { font-size: var(--text-sm) !important; color: var(--text-secondary) !important; margin-top: 8rpx !important; }

.bottom-placeholder { height: 160rpx; }

/* FAB */
.custom-fab { position: fixed; right: 40rpx; bottom: 200rpx; width: 96rpx; height: 96rpx; border-radius: 50%; background: var(--primary); display: flex; align-items: center; justify-content: center; z-index: 9999; box-shadow: var(--shadow-primary); transition: transform var(--duration-fast) var(--easing-out); }
.custom-fab:active { transform: scale(0.90); opacity: 0.85; }
.mt-24 { margin-top: 24rpx; }
.mt-32 { margin-top: 32rpx; }
</style>
