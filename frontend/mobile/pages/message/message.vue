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
      <view class="nav-title-wrap"><text class="nav-title-text">消息</text></view>
      <template v-slot:right>
        <view class="nav-btn" @click="onNotify">
          <iconpark-icon name="notification-filled" size="24" color="#FF6B4A" />
        </view>
      </template>
    </uni-nav-bar>

    <scroll-view class="main-scroll" :style="{ height: scrollHeight + 'px' }" scroll-y enhanced :show-scrollbar="false">
      <uni-search-bar
        v-model="searchValue"
        placeholder="搜索聊天记录或联系人"
        radius="48"
        bgColor="#e7e8f1"
        :clearButton="'auto'"
        :cancelButton="'none'"
        @confirm="onSearch"
      >
        <template v-slot:searchIcon><iconpark-icon name="search" size="18" color="#737784" /></template>
      </uni-search-bar>

      <view class="entry-grid">
        <view class="entry-card-item" @click="goNotifications(1)">
          <view class="entry-inner">
            <view class="entry-icon entry-icon--blue"><iconpark-icon name="sound" size="24" color="#fff" /></view>
            <view v-if="sysUnreadCount > 0" class="entry-dot-badge"></view>
            <text class="entry-title">系统通知</text>
            <text class="entry-desc">账户安全与活动上新</text>
          </view>
        </view>
        <view class="entry-card-item" @click="goNotifications(2)">
          <view class="entry-inner">
            <view class="entry-icon entry-icon--green"><iconpark-icon name="delivery" size="24" color="#fff" /></view>
            <view v-if="orderUnreadCount > 0" class="entry-dot-badge"></view>
            <text class="entry-title">物流通知</text>
            <text class="entry-desc">您的订单状态更新</text>
          </view>
        </view>
      </view>

      <view class="section-header"><text class="section-label">最近消息</text></view>

      <template v-if="!store.isCertified">
        <view class="empty-contacts">
          <iconpark-icon name="locked-filled" size="36" color="#c2c6d5" />
          <text class="empty-text">认证后可使用聊天功能</text>
          <text class="empty-sub" @click="goCertify">去认证</text>
        </view>
      </template>

      <uni-card v-else :isShadow="false" :margin="'0'" :padding="'0'" :spacing="'0'" :border="false" class="chat-list-card">
        <view v-if="displayedContacts.length === 0 && !loadingContacts" class="empty-contacts">
          <text class="empty-text">暂无聊天记录</text>
        </view>

        <view v-for="contact in displayedContacts" :key="contact.userId" class="chat-item" @click="onChat(contact)">
          <view class="chat-avatar-wrap">
            <image v-if="contact.avatarUrl" class="chat-avatar-img" :src="normalizeUrl(contact.avatarUrl)" mode="aspectFill" />
            <view v-else class="chat-avatar">{{ contact.initial }}</view>
          </view>
          <view class="chat-body">
            <view class="chat-top">
              <text class="chat-name">{{ contact.nickname }}</text>
              <text class="chat-time" :class="{ 'chat-time--new': contact.unreadCount > 0 }">{{ contact.lastMessageTime }}</text>
            </view>
            <view class="chat-bottom">
              <text class="chat-preview">{{ contact.lastMessage || '暂无消息' }}</text>
              <uni-badge v-if="contact.unreadCount > 0" :text="String(contact.unreadCount)" name="error" size="small" />
            </view>
          </view>
        </view>
      </uni-card>

      <view class="bottom-placeholder"></view>
    </scroll-view>

    <custom-tabbar :selected="3" />
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useStore } from '@/store/index.js'
import { useChatStore } from '@/store/chat.js'
import { notificationApi } from '@/api'
import CustomTabbar from '@/components/custom-tabbar/custom-tabbar.vue'
import { SERVER_ORIGIN } from '@/utils/config'

function normalizeUrl(url) {
  if (!url) return ''
  if (url.startsWith('http')) return url
  if (url.startsWith('/')) return SERVER_ORIGIN + url
  return url
}

const store = useStore()
const chatStore = useChatStore()
const sysInfo = uni.getSystemInfoSync()
const scrollHeight = sysInfo.windowHeight - sysInfo.statusBarHeight - 44

const searchValue = ref('')
const loadingContacts = ref(false)
const sysUnreadCount = ref(0)
const orderUnreadCount = ref(0)

const displayedContacts = computed(() => {
  const kw = (searchValue.value || '').trim().toLowerCase()
  if (!kw) return chatStore.contacts
  return chatStore.contacts.filter(c =>
    (c.nickname || '').toLowerCase().includes(kw)
  )
})

async function loadContacts() {
  loadingContacts.value = true
  try {
    await chatStore.loadContacts()
  } catch (e) { /* handled */ }
  loadingContacts.value = false
}

async function loadUnread() {
  try {
    const data = await notificationApi.getNotifications({ isRead: 0, page: 1, size: 100 }, { showLoading: false })
    const records = data?.records || []
    sysUnreadCount.value = records.filter(n => n.type === 1).length
    orderUnreadCount.value = records.filter(n => n.type === 2).length
  } catch (e) { /* ignore */ }
}

function onChat(contact) {
  const nickname = encodeURIComponent(contact.nickname || '')
  uni.navigateTo({
    url: `/pages/chat-detail/chat-detail?userId=${contact.userId}&nickname=${nickname}&avatar=${encodeURIComponent(contact.avatarUrl || '')}`
  })
}

function onSearch(e) {
  searchValue.value = (e.value || '').trim()
}

function goNotifications(type) {
  uni.navigateTo({ url: `/pages/notifications/notifications?type=${type || ''}` })
}

function goCertify() { uni.navigateTo({ url: '/pages/certify/certify' }) }
function onNotify() { goNotifications('') }

onShow(() => {
  loadUnread()
  if (store.isCertified) {
    loadContacts()
    if (!chatStore.wsConnected && store.token) {
      chatStore.connectStomp(store.token, store.userId)
    }
  }
})

if (store.isCertified) {
  loadContacts()
  if (!chatStore.wsConnected && store.token) {
    chatStore.connectStomp(store.token, store.userId)
  }
}
loadUnread()
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

.entry-grid { display: flex; gap: 24rpx; margin-top: 24rpx; }
.entry-card-item { flex: 1; background: var(--surface-raised); border-radius: var(--radius-lg); padding: 28rpx; box-shadow: var(--shadow-sm); transition: transform var(--duration-fast) var(--easing-out); }
.entry-card-item:active { transform: scale(0.97); }
.entry-inner { display: flex; flex-direction: column; align-items: flex-start; position: relative; width: 100%; }
.entry-icon { width: 68rpx; height: 68rpx; border-radius: 50%; display: flex; align-items: center; justify-content: center; margin-bottom: 16rpx; }
.entry-icon--blue { background: var(--primary); }
.entry-icon--green { background: linear-gradient(135deg, var(--secondary), var(--secondary-dark)); }
.entry-dot-badge { position: absolute; top: -4rpx; right: -4rpx; width: 20rpx; height: 20rpx; background: var(--error); border-radius: 50%; border: 3rpx solid var(--surface-raised); }
.entry-title { font-size: 28rpx; font-weight: 600; color: var(--text-primary); }
.entry-desc { font-size: 22rpx; color: var(--text-secondary); margin-top: 6rpx; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; max-width: 100%; }

.section-header { margin-top: 48rpx; margin-bottom: 20rpx; }
.section-label { font-size: 24rpx; font-weight: 500; color: var(--text-tertiary); letter-spacing: 2rpx; padding-left: 8rpx; }

.chat-list-card { border-radius: var(--radius-card) !important; overflow: hidden; }
.chat-list-card :deep(.uni-card__content) { padding: 0 !important; }

.chat-item { display: flex; align-items: center; padding: 28rpx 28rpx; border-bottom: 1rpx solid var(--outline-light); }
.chat-item:last-child { border-bottom: none; }
.chat-item:active { background: var(--surface-hover); }
.chat-avatar-wrap { position: relative; width: 96rpx; height: 96rpx; flex-shrink: 0; }
.chat-avatar-img { width: 100%; height: 100%; border-radius: 50%; object-fit: cover; }
.chat-avatar { width: 100%; height: 100%; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 34rpx; font-weight: 600; color: #fff; background: linear-gradient(135deg, var(--primary), var(--secondary)); }
.chat-body { flex: 1; margin-left: 24rpx; min-width: 0; }
.chat-top { display: flex; justify-content: space-between; align-items: baseline; margin-bottom: 8rpx; }
.chat-name { font-size: 30rpx; font-weight: 500; color: var(--text-primary); }
.chat-time { font-size: 22rpx; color: var(--text-tertiary); flex-shrink: 0; }
.chat-time--new { color: var(--primary); font-weight: 600; }
.chat-bottom { display: flex; justify-content: space-between; align-items: center; }
.chat-preview { font-size: 26rpx; color: var(--text-secondary); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; flex: 1; padding-right: 16rpx; }

.empty-contacts { padding: 60rpx 0; text-align: center; display: flex; flex-direction: column; align-items: center; gap: 12rpx; }
.empty-text { font-size: 26rpx; color: var(--text-tertiary); }
.empty-sub { font-size: 26rpx; color: var(--primary); font-weight: 500; }
.empty-sub:active { opacity: 0.6; }

.bottom-placeholder { height: 160rpx; }
</style>
