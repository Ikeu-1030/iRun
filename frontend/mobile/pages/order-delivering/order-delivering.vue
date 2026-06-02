<template>
  <view class="page">
    <uni-nav-bar title="订单详情" backgroundColor="#FAFAF8" :border="false" statusBar fixed leftIcon="left" @clickLeft="onBack" color="#1C1B1A" />

    <scroll-view class="main-scroll" :style="{ height: scrollHeight + 'px' }" scroll-y enhanced :show-scrollbar="false" refresher-enabled @refresherrefresh="loadData" :refresher-triggered="refreshing">
      <!-- 状态横幅 -->
      <view class="status-banner animate-scale-pop" :style="{ background: statusBg }" :class="{ 'animate-status-breathe': order.orderStatus === 2 }">
        <view class="status-icon-wrap" :style="{ background: statusIconBg }">
          <iconpark-icon :name="statusIcon" size="40" :color="statusColor" />
        </view>
        <text class="status-title">{{ displayStatusTitle }}</text>
        <text class="status-hint">{{ statusHint }}</text>
        <text v-if="order.expectFinishTime && order.orderStatus === 1" class="status-accept-time">{{ isQueueWait ? '预计完成：' : '预计送达：' }}{{ order.expectFinishTime }}</text>
      </view>

      <!-- 对方信息卡片 -->
      <view class="contact-card" v-if="otherParty.name" @click="goRiderProfile">
        <image v-if="otherParty.avatar" class="contact-avatar-img" :src="otherParty.avatar" mode="aspectFill" />
        <view v-else class="contact-avatar" :style="{ background: otherParty.avatarBg }">{{ otherParty.initial }}</view>
        <view class="contact-info">
          <text class="contact-name">{{ otherParty.name }}</text>
          <text class="contact-role">{{ otherParty.roleLabel }}</text>
          <text class="contact-phone" v-if="otherParty.phone">{{ otherParty.phone }}</text>
        </view>
        <view class="contact-actions">
          <view class="c-action" @click.stop="onChat">
            <iconpark-icon name="chat" size="22" color="#FF6B4A" />
            <text>联系</text>
          </view>
          <view class="c-action c-action--call" @click.stop="onCall">
            <iconpark-icon name="phone" size="22" color="#fff" />
            <text>电话</text>
          </view>
        </view>
      </view>

      <!-- 订单编号与类型 -->
      <view class="info-card">
        <view class="card-header">
          <view class="type-badge" :class="'type-badge--' + typeColor">
            <iconpark-icon :name="typeIcon" size="20" :color="typeIconColor" />
            <text>{{ typeLabel }}</text>
          </view>
          <view class="order-no-row"><text class="order-no">订单号：{{ order.taskNo || order.orderId || '--' }}</text><view class="copy-btn" v-if="order.taskNo || order.orderId" @click.stop="copyOrderNo(order.taskNo || order.orderId)"><iconpark-icon name="copy" size="16" color="#FF6B4A" /></view></view>
        </view>
        <view class="card-row card-row--highlight">
          <text class="row-label">报酬金额</text>
          <text class="row-value row-value--price">¥{{ rewardText }}</text>
        </view>
        <view v-if="productFeeText" class="card-row">
          <text class="row-label">预估商品费</text>
          <text class="row-value">¥{{ productFeeText.toFixed(2) }}（含在总报酬中）</text>
        </view>
      </view>

      <!-- 任务描述 -->
      <view class="info-card" v-if="order.description || order.taskDescription">
        <view class="card-title">
          <iconpark-icon name="compose" size="18" color="#FF6B4A" />
          <text>任务描述</text>
        </view>
        <view v-if="productTags.length" class="package-tags">
          <text class="package-tag" v-for="(tag, ti) in productTags" :key="ti">{{ tag }}</text>
        </view>
        <view v-if="bookCount" class="package-tags">
          <text class="package-tag">书本数量：{{ bookCount }}本</text>
        </view>
        <view v-if="printSpecs" class="package-tags">
          <text class="package-tag">{{ printSpecs.printType }}</text>
          <text class="package-tag">{{ printSpecs.printSide }}</text>
        </view>
        <view v-if="merchantTag" class="package-tags">
          <text class="package-tag">商家：{{ merchantTag }}</text>
        </view>
        <text class="desc-text">{{ displayDescription }}</text>
      </view>

      <!-- 取件信息 -->
      <view class="info-card" v-if="order.pickupAddress || order.pickupCode">
        <view class="card-title">
          <iconpark-icon name="location-filled" size="18" color="#4c5e86" />
          <text>取件信息</text>
        </view>
        <view class="card-row" v-if="order.pickupAddress">
          <text class="row-label">取件地址</text>
          <text class="row-value">{{ order.pickupAddress }}</text>
        </view>
        <view class="card-row" v-if="order.pickupCode">
          <text class="row-label">取件码</text>
          <text class="row-value row-value--code">{{ order.pickupCode }}</text>
        </view>
        <view v-if="order.pickupProofImgs && order.pickupProofImgs.length" class="proof-section">
          <text class="proof-label">{{ isQueueWait ? '到达凭证' : '取货凭证' }}</text>
          <view class="proof-images">
            <image v-for="(url, i) in order.pickupProofImgs" :key="i" :src="url" mode="aspectFill" class="proof-img" @click="previewImage(url)" />
          </view>
        </view>
      </view>

      <!-- 送达信息 -->
      <view class="info-card" v-if="order.deliveryAddress">
        <view class="card-title">
          <iconpark-icon name="flag-filled" size="18" color="#34d399" />
          <text>送达信息</text>
        </view>
        <view class="card-row">
          <text class="row-label">收货人</text>
          <text class="row-value">{{ deliveryContactName }}</text>
        </view>
        <view class="card-row" v-if="deliveryContactPhone">
          <text class="row-label">联系电话</text>
          <text class="row-value row-value--phone">{{ deliveryContactPhone }}</text>
        </view>
        <view class="card-row">
          <text class="row-label">送达地址</text>
          <text class="row-value">{{ deliveryAddressDetail }}</text>
        </view>
        <view v-if="order.deliverProofImgs && order.deliverProofImgs.length" class="proof-section">
          <text class="proof-label">{{ isQueueWait ? '完成凭证' : '送达凭证' }}</text>
          <view class="proof-images">
            <image v-for="(url, i) in order.deliverProofImgs" :key="i" :src="url" mode="aspectFill" class="proof-img" @click="previewImage(url)" />
          </view>
        </view>
      </view>

      <!-- 任务图片 -->
      <view class="info-card" v-if="order.imageUrls && order.imageUrls.length">
        <view class="card-title">
          <iconpark-icon name="image-filled" size="18" color="#FF6B4A" />
          <text>任务截图</text>
        </view>
        <view class="proof-images">
          <image v-for="(url, i) in order.imageUrls" :key="i" :src="url" mode="aspectFill" class="proof-img" @click="previewImage(url)" />
        </view>
      </view>

      <!-- 配送进度 -->
      <view class="info-card">
        <view class="card-title">
          <iconpark-icon name="clock-filled" size="18" color="#F59E0B" />
          <text>{{ isQueueWait ? '任务进度' : '配送进度' }}</text>
        </view>
        <uni-steps :options="stepItems" :active="stepActive" activeColor="#FF6B4A" direction="column" />
        <view class="detail-items">
          <view v-if="order.acceptTime" class="detail-item">
            <text>接单时间</text>
            <text class="detail-value">{{ order.acceptTime }}</text>
          </view>
          <view v-if="order.pickupTime" class="detail-item">
            <text>{{ isQueueWait ? '到达时间' : '取货时间' }}</text>
            <text class="detail-value">{{ order.pickupTime }}</text>
          </view>
          <view v-if="order.deliverTime" class="detail-item">
            <text>{{ isQueueWait ? '结束时间' : '送达时间' }}</text>
            <text class="detail-value">{{ order.deliverTime }}</text>
          </view>
          <view v-if="order.confirmTime" class="detail-item detail-item--total">
            <text>完成时间</text>
            <text class="detail-value detail-value--total">{{ order.confirmTime }}</text>
          </view>
        </view>
      </view>

      <!-- 操作按钮 -->
      <view class="action-section">
        <view v-if="isRunner && order.orderStatus === 1" class="pickup-section">
          <UploadGrid v-model="uploadedPickupImages" :maxCount="3" />
          <view class="submit-btn" :class="{ 'submit-btn--disabled': uploadedPickupImages.length === 0 || actionLocked }" @click="onPickup">
            <iconpark-icon name="checkbox-filled" size="22" color="#fff" />
            <text>{{ isQueueWait ? '我已到达' : '我已取货' }}</text>
          </view>
          <view class="cancel-btn" :class="{ 'cancel-btn--disabled': actionLocked }" @click="onCancel">
            <text>取消订单</text>
          </view>
        </view>
        <view v-if="isRunner && order.orderStatus === 2" class="pickup-section">
          <UploadGrid v-model="uploadedDeliverImages" :maxCount="3" />
          <view class="submit-btn submit-btn--deliver" :class="{ 'submit-btn--disabled': uploadedDeliverImages.length === 0 || actionLocked }" @click="onDeliver">
            <iconpark-icon name="location-filled" size="22" color="#fff" />
            <text>{{ isQueueWait ? '代办完成' : '我已送达' }}</text>
          </view>
        </view>
        <view v-if="isPublisher && order.orderStatus === 3" class="submit-btn submit-btn--confirm" :class="{ 'submit-btn--disabled': actionLocked }" @click="onConfirm">
          <iconpark-icon name="checkmarkempty" size="22" color="#fff" />
          <text>{{ isQueueWait ? '确认完成' : '确认收货' }}</text>
        </view>
      </view>

      <view class="bottom-placeholder"></view>
    </scroll-view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onLoad, onUnload } from '@dcloudio/uni-app'
import { orderApi } from '@/api'
import { TASK_TYPES, TASK_TYPE_META, TYPE_FROM_API, isQueueWaitType } from '@/utils/constants.js'
import { parseTaskSpecs, parseExpressPackagesFromSpecs, parseShoppingItemsFromSpecs, parseBookCountFromSpecs, parsePrintSpecsFromSpecs, parseMerchantInfoFromSpecs } from '@/utils/campus-data.js'
import { useSubmitLock } from '@/utils/submit-guard'
import UploadGrid from '@/components/upload-grid/upload-grid.vue'
import { showToast } from '@/utils/toast'
import { SERVER_ORIGIN } from '@/utils/config'

function normalizeUrl(url) {
  if (!url) return ''
  if (url.startsWith('http')) return url
  if (url.startsWith('/')) return SERVER_ORIGIN + url
  return url
}

const sysInfo = uni.getSystemInfoSync()
const scrollHeight = sysInfo.windowHeight - sysInfo.statusBarHeight - 44

const taskId = ref('')
const orderId = ref('')
const isRunner = ref(false)
const isPublisher = ref(false)
const order = ref({ orderStatus: 1 })
const refreshing = ref(false)
const dataReady = ref(false)
const { lock: actionLock, unlock: actionUnlock, locked: actionLocked } = useSubmitLock()
const uploadedPickupImages = ref([])
const uploadedDeliverImages = ref([])

const rewardText = computed(() => Number(order.value.reward || 0).toFixed(2))
const taskSpecs = computed(() => parseTaskSpecs(order.value.taskSpecs))
const stepActive = computed(() => {
  const s = order.value.orderStatus
  return s === 1 ? 0 : s === 2 ? 1 : s === 3 ? 2 : s >= 4 ? 3 : 0
})

const taskTypeCode = computed(() => typeof order.value.type === 'number' ? order.value.type : (TYPE_FROM_API[order.value.type] || 1))

const typeLabel = computed(() => TASK_TYPES[taskTypeCode.value] || '任务')
const typeMeta = computed(() => TASK_TYPE_META[taskTypeCode.value] || TASK_TYPE_META[1])
const typeIcon = computed(() => typeMeta.value.icon)
const typeIconColor = computed(() => typeMeta.value.color)
const typeColor = computed(() => ({ 1: 'blue', 2: 'orange', 3: 'green', 4: 'teal' }[taskTypeCode.value] || 'blue'))
const isQueueWait = computed(() => isQueueWaitType(order.value.subType))

// 用户端：已接单(1)和配送中(2)统一显示"配送中"
const displayStatusTitle = computed(() => {
  if (!dataReady.value) return '加载中'
  const s = order.value.orderStatus
  if (isQueueWait.value) {
    if (isPublisher.value && (s === 1 || s === 2)) return '代办中'
    if (s === 1) return '待到达'
    if (s === 2) return '代办中'
  } else {
    if (isPublisher.value && (s === 1 || s === 2)) return '配送中'
    if (s === 1) return '待取货'
    if (s === 2) return '待送达'
  }
  if (s === 3) return '待确认'
  if (s === 4) return '已完成'
  if (s === 5) return '已取消'
  return '加载中'
})

const otherParty = computed(() => {
  if (isRunner.value) {
    return {
      name: order.value.publisherNickname || '发布者',
      initial: (order.value.publisherNickname || '发').charAt(0),
      roleLabel: '任务发布者',
      avatarBg: 'linear-gradient(135deg,#f97316,#f59e0b)',
      avatar: normalizeUrl(order.value.publisherAvatar),
      id: order.value.publisherId,
      phone: order.value.publisherPhone
    }
  }
  if (isPublisher.value) {
    return {
      name: order.value.runnerNickname || '跑腿员',
      initial: (order.value.runnerNickname || '跑').charAt(0),
      roleLabel: '跑腿员',
      avatarBg: 'linear-gradient(135deg,#4facfe,#43e97b)',
      avatar: normalizeUrl(order.value.runnerAvatar),
      id: order.value.runnerId,
      phone: order.value.runnerPhone
    }
  }
  return {}
})

const statusMap = {
  1: { color: '#e67e22', bg: 'linear-gradient(135deg,#fff7ed,#f9f9ff)', iconBg: 'rgba(230,126,34,.12)', icon: 'info', hint: '请尽快前往取货' },
  2: { color: '#3871d6', bg: 'linear-gradient(135deg,#eff6ff,#f9f9ff)', iconBg: 'rgba(56,113,214,.12)', icon: 'location-filled', hint: '正在配送中' },
  3: { color: '#34d399', bg: 'linear-gradient(135deg,#f0fdf4,#f9f9ff)', iconBg: 'rgba(52,211,153,.12)', icon: 'checkmarkempty', hint: '' },
  4: { color: '#4c5e86', bg: 'linear-gradient(135deg,#f2f3fc,#f9f9ff)', iconBg: 'rgba(76,94,134,.12)', icon: 'checkbox-filled', hint: '' },
  5: { color: '#737784', bg: 'linear-gradient(135deg,#ecedf6,#f9f9ff)', iconBg: 'rgba(115,119,132,.12)', icon: 'closeempty', hint: '' }
}
const statusColor = computed(() => statusMap[order.value.orderStatus]?.color || '#3871d6')
const statusBg = computed(() => statusMap[order.value.orderStatus]?.bg || 'linear-gradient(135deg,#eff6ff,#f9f9ff)')
const statusIconBg = computed(() => statusMap[order.value.orderStatus]?.iconBg || 'rgba(56,113,214,.12)')
const statusIcon = computed(() => statusMap[order.value.orderStatus]?.icon || 'info')
const statusHint = computed(() => {
  if (!dataReady.value) return ''
  const s = order.value.orderStatus
  if (isPublisher.value) {
    if (isQueueWait.value) {
      if (s === 1 || s === 2) return '跑腿员正在为您代办'
      if (s === 3) return '跑腿员已完成代办，请确认'
    } else {
      if (s === 1 || s === 2) return '跑腿员正在为您配送'
      if (s === 3) return '跑腿员已送达，请确认收货'
    }
  }
  if (isQueueWait.value) {
    if (s === 1) return '请尽快前往办事地点'
    if (s === 2) return '正在代办中'
  }
  return statusMap[s]?.hint || ''
})

// 收货人信息（后端直接返回地址簿联系人）
const rawContactName = computed(() => {
  return order.value.contactName || ''
})
const deliveryContactName = computed(() => {
  const name = rawContactName.value
  if (!name) return "未知"
  const gender = order.value.deliveryGender
  if (gender === 1 || gender === '1' || gender === '男') return name + '先生'
  if (gender === 0 || gender === '0' || gender === '女') return name + '女士'
  return name
})
const deliveryContactPhone = computed(() => {
  return order.value.contactPhone || ''
})
const deliveryAddressDetail = computed(() => {
  return order.value.deliveryAddress || ''
})

// 解析描述：代取快递显示包裹信息，取件码在取件信息区单独展示
const displayDescription = computed(() => {
  const desc = order.value.description || order.value.taskDescription || ''
  const specs = taskSpecs.value
  if (taskTypeCode.value === 1) {
    const pkgSpecs = parseExpressPackagesFromSpecs(specs)
    if (pkgSpecs) {
      let text = `快递规格：${pkgSpecs.sizes}`
      if (desc) text += `\n需求描述：${desc}`
      return text
    }
    if (desc.startsWith('packageSize:')) {
      const idx = desc.indexOf('；description：')
      if (idx !== -1) {
        const sizes = desc.substring('packageSize:'.length, idx)
        let innerDesc = desc.substring(idx + '；description：'.length)
        if (innerDesc === 'null') innerDesc = ''
        let text = `包裹：${sizes}`
        if (innerDesc) text += `\n需求描述：${innerDesc}`
        return text
      }
    }
  }
  if (taskTypeCode.value === 4) {
    if (specs && specs.商品列表) return desc || '暂无描述'
    return desc || '暂无描述'
  }
  return desc || '暂无描述'
})

const productFeeText = computed(() => {
  if (taskTypeCode.value !== 4) return null
  const specs = taskSpecs.value
  if (specs && specs.预估商品费) return specs.预估商品费
  return null
})

const productTags = computed(() => {
  if (taskTypeCode.value !== 4) return []
  const specs = taskSpecs.value
  if (specs && specs.商品列表) {
    return specs.商品列表.map(item => item.规格 ? `${item.名称}x${item.数量}（${item.规格}）` : `${item.名称}x${item.数量}`)
  }
  const val = order.value.subType || ''
  if (!val) return []
  if (val.startsWith('[')) {
    try { return JSON.parse(val) } catch (e) { return [] }
  }
  return val.split('、')
})

const bookCount = computed(() => {
  if (taskTypeCode.value !== 3) return null
  return parseBookCountFromSpecs(taskSpecs.value)
})

const printSpecs = computed(() => {
  if (taskTypeCode.value !== 3) return null
  return parsePrintSpecsFromSpecs(taskSpecs.value)
})

const merchantTag = computed(() => {
  if (taskTypeCode.value !== 2) return null
  return parseMerchantInfoFromSpecs(taskSpecs.value)
})

const stepItems = computed(() => {
  if (isQueueWait.value) {
    return [
      { title: '待到达', desc: '跑腿员前往办事地点' },
      { title: '代办中', desc: '正在为您代办' },
      { title: '待确认', desc: '请确认完成' },
      { title: '已完成', desc: '订单已完成' }
    ]
  }
  return [
    { title: '待取货', desc: '跑腿员前往取件' },
    { title: '待送达', desc: '正在为您配送' },
    { title: '待确认', desc: '请确认收货' },
    { title: '已完成', desc: '订单已完成' }
  ]
})

onLoad((options) => {
  taskId.value = options?.taskId || ''
  orderId.value = options?.orderId || ''
  isPublisher.value = options?.role === 'publisher'
  isRunner.value = options?.role === 'runner'
  loadData()
})

async function loadData() {
  refreshing.value = true
  try {
    let data
    if (orderId.value) {
      data = await orderApi.getOrderDetail(orderId.value, { showLoading: false })
    } else if (taskId.value) {
      data = await orderApi.getOrderByTask(taskId.value, { showLoading: false })
    } else {
      return
    }
    order.value = data
    orderId.value = data.orderId
    taskId.value = data.taskId
    isPublisher.value = data.isOwnerPublisher ?? isPublisher.value
    isRunner.value = data.isOwnerRunner ?? isRunner.value
    dataReady.value = true

    if (data.orderStatus === 4 || data.orderStatus === 5) {
      setTimeout(() => {
        uni.redirectTo({ url: `/pages/order-completed/order-completed?orderId=${data.orderId}` })
      }, 500)
    }
  } catch (e) { /* handled */ }
  refreshing.value = false
}

async function onPickup() {
  const label = isQueueWait.value ? '到达凭证' : '取货凭证'
  if (uploadedPickupImages.value.length === 0) {
    showToast(`请先上传${label}`)
    return
  }
  if (!actionLock()) return
  try {
    await orderApi.confirmPickup(orderId.value, uploadedPickupImages.value)
    uni.showToast({ title: isQueueWait.value ? '已确认到达' : '已确认取货', icon: 'success' })
    uploadedPickupImages.value = []
    loadData()
  } catch (e) { /* handled */ } finally {
    actionUnlock()
  }
}

async function onDeliver() {
  const label = isQueueWait.value ? '完成凭证' : '送达凭证'
  if (uploadedDeliverImages.value.length === 0) {
    showToast(`请先上传${label}`)
    return
  }
  if (!actionLock()) return
  try {
    await orderApi.confirmDeliver(orderId.value, uploadedDeliverImages.value)
    uni.showToast({ title: isQueueWait.value ? '代办已完成' : '已确认送达', icon: 'success' })
    uploadedDeliverImages.value = []
    loadData()
  } catch (e) { /* handled */ } finally {
    actionUnlock()
  }
}

async function onConfirm() {
  const title = isQueueWait.value ? '确认完成' : '确认收货'
  const content = isQueueWait.value ? '确认代办已完成？确认后报酬将支付给跑腿员。' : '确认任务已完成？确认后报酬将支付给跑腿员。'
  const res = await new Promise(r => {
    uni.showModal({ title, content, success: r2 => r(r2.confirm) })
  })
  if (!res) return
  if (!actionLock()) return
  try {
    await orderApi.confirmOrder(orderId.value)
    uni.showToast({ title: '已完成，去评价吧', icon: 'success' })
    setTimeout(() => {
      uni.redirectTo({ url: `/pages/order-completed/order-completed?orderId=${orderId.value}` })
    }, 1000)
  } catch (e) { /* handled */ } finally {
    actionUnlock()
  }
}

function onCancel() {
  uni.$on('cancelReasonSelected', async (reason) => {
    uni.$off('cancelReasonSelected')
    if (!actionLock()) return
    try {
      await orderApi.cancelOrder(orderId.value, reason)
      uni.showToast({ title: '已取消', icon: 'success' })
      loadData()
    } catch (e) { /* handled */ } finally {
      actionUnlock()
    }
  })
  uni.navigateTo({ url: '/pages/cancel-reason/cancel-reason?type=order' })
}

function onChat() {
  const userId = otherParty.value.id || ''
  const nickname = encodeURIComponent(otherParty.value.name || '')
  uni.navigateTo({ url: `/pages/chat-detail/chat-detail?userId=${userId}&nickname=${nickname}` })
}

function onCall() {
  if (otherParty.value.phone) {
    uni.makePhoneCall({ phoneNumber: otherParty.value.phone })
  } else {
    uni.showToast({ title: '暂无对方联系方式', icon: 'none' })
  }
}

function goRiderProfile() {
  if (!isPublisher.value) return
  const rid = otherParty.value.id
  if (rid) uni.navigateTo({ url: `/pages/rider-profile/rider-profile?runnerId=${rid}` })
}

function previewImage(url) {
  const ext = (url || '').split('.').pop()?.toLowerCase()
  if (ext && ['jpg', 'jpeg', 'png', 'gif', 'webp'].includes(ext)) {
    uni.previewImage({ urls: [url], current: url })
  } else {
    uni.downloadFile({
      url,
      success: (res) => uni.openDocument({ filePath: res.tempFilePath, showMenu: true }),
      fail: () => uni.showToast({ title: '文件打开失败', icon: 'none' })
    })
  }
}

function onBack() { uni.navigateBack() }

onUnload(() => {
  uni.$off('cancelReasonSelected')
})

function copyOrderNo(no) {
  uni.setClipboardData({ data: no, success: () => uni.showToast({ title: '已复制', icon: 'success' }) })
}
</script>

<style scoped>
.page{width:100%;height:100vh;display:flex;flex-direction:column;background:var(--background);overflow:hidden}
.main-scroll{box-sizing:border-box;width:100%;padding:0 32rpx}

/* 状态横幅 */
.status-banner{display:flex;flex-direction:column;align-items:center;padding:36rpx 32rpx 24rpx;border-radius:var(--radius-card);margin-top:16rpx}
.animate-status-breathe{animation:statusBreathe 3s ease-in-out infinite}
@keyframes statusBreathe{0%,100%{opacity:1}50%{opacity:.88}}
.status-icon-wrap{width:96rpx;height:96rpx;border-radius:50%;display:flex;align-items:center;justify-content:center;margin-bottom:14rpx}
.status-title{font-size:34rpx;font-weight:700;color:var(--text-primary);margin-bottom:4rpx}
.status-hint{font-size:26rpx;color:var(--text-secondary)}
.status-accept-time{font-size:24rpx;color:var(--text-secondary);margin-top:12rpx;padding:8rpx 24rpx;background:rgba(255,255,255,.6);border-radius:20rpx}

/* 联系人卡片 */
.contact-card{display:flex;align-items:center;gap:18rpx;background:var(--surface-raised);border-radius:var(--radius-card);padding:24rpx 28rpx;margin-top:20rpx;box-shadow:var(--shadow-sm)}
.contact-avatar{width:80rpx;height:80rpx;border-radius:50%;display:flex;align-items:center;justify-content:center;font-size:34rpx;font-weight:700;color:#fff;flex-shrink:0}
.contact-avatar-img{width:80rpx;height:80rpx;border-radius:50%;object-fit:cover;flex-shrink:0}
.contact-info{flex:1}
.contact-name{font-size:28rpx;font-weight:600;color:var(--text-primary);display:block}
.contact-role{font-size:22rpx;color:var(--text-secondary);margin-top:2rpx;display:block}
.contact-phone{font-size:24rpx;color:var(--text-tertiary);margin-top:4rpx;display:block}
.contact-actions{display:flex;gap:12rpx}
.c-action{display:flex;align-items:center;gap:6rpx;padding:12rpx 20rpx;border-radius:var(--radius-card);background:var(--surface)}
.c-action text{font-size:24rpx;font-weight:500;color:var(--primary)}
.c-action--call{background:var(--primary)}
.c-action--call text{color:#fff}

/* 信息卡片 */
.info-card{background:var(--surface-raised);border-radius:var(--radius-card);padding:28rpx;margin-top:20rpx;box-shadow:var(--shadow-sm)}
.card-header{display:flex;align-items:center;justify-content:space-between;margin-bottom:16rpx;padding-bottom:16rpx;border-bottom:1rpx solid var(--surface-hover)}
.card-title{display:flex;align-items:center;gap:10rpx;font-size:28rpx;font-weight:600;color:var(--text-primary);margin-bottom:20rpx;padding-bottom:16rpx;border-bottom:1rpx solid var(--surface-hover)}
.type-badge{display:flex;align-items:center;gap:8rpx;padding:8rpx 18rpx;border-radius:20rpx;font-size:24rpx;font-weight:500}
.type-badge--blue{background:var(--primary-container);color:#004397}
.type-badge--orange{background:#fff7ed;color:#ad6200}
.type-badge--green{background:#f0fdf4;color:#166534}
.type-badge--teal{background:#ecfeff;color:#0e7490}
.order-no{font-size:22rpx;color:var(--text-tertiary)}
.order-no-row{display:flex;align-items:center;gap:6rpx}
.copy-btn{width:48rpx;height:48rpx;border-radius:50%;background:var(--primary-container);display:flex;align-items:center;justify-content:center;flex-shrink:0}
.copy-btn:active{background:#FFD1C7}
.card-row{display:flex;justify-content:space-between;align-items:flex-start;padding:12rpx 0}
.card-row--highlight{background:var(--surface);border-radius:14rpx;padding:18rpx 20rpx;margin-top:4rpx}
.row-label{font-size:26rpx;color:var(--text-secondary);flex-shrink:0;min-width:140rpx}
.row-value{font-size:26rpx;font-weight:500;color:var(--text-primary);text-align:right;flex:1}
.row-value--price{font-size:34rpx;font-weight:700;color:var(--primary)}
.row-value--code{font-size:30rpx;font-weight:700;color:var(--warning);letter-spacing:2rpx}
.desc-text{font-size:26rpx;color:var(--text-primary);line-height:1.6;white-space:pre-wrap}
.row-value--phone{font-size:26rpx;font-weight:500;color:var(--primary);letter-spacing:2rpx}

.proof-section{margin-top:20rpx;padding-top:18rpx;border-top:1rpx dashed var(--outline-light)}
.proof-label{font-size:24rpx;font-weight:500;color:var(--text-secondary);display:block;margin-bottom:12rpx}
.proof-images{display:flex;gap:12rpx;flex-wrap:wrap}
.proof-img{width:120rpx;height:120rpx;border-radius:14rpx;object-fit:cover}

/* 时间线 */
.detail-items{margin-top:8rpx}
.detail-item{display:flex;justify-content:space-between;padding:16rpx 0;font-size:26rpx;color:var(--text-secondary);border-bottom:1rpx solid var(--surface)}
.detail-item:last-child{border-bottom:none}
.detail-item--total{border-top:1rpx solid var(--outline-light);margin-top:8rpx;padding-top:18rpx}
.detail-value{font-weight:500;color:var(--text-primary)}
.detail-value--total{font-weight:600}
.package-tags{display:flex;flex-wrap:wrap;gap:10rpx;margin-bottom:14rpx}
.package-tag{font-size:22rpx;padding:6rpx 16rpx;background:var(--primary-container);color:var(--primary);border-radius:8rpx}

/* 操作按钮 */
.action-section{display:flex;flex-direction:column;gap:20rpx;margin-top:32rpx}
.submit-btn{height:96rpx;background:var(--primary);border-radius:48rpx;display:flex;align-items:center;justify-content:center;gap:10rpx;box-shadow:var(--shadow-sm)}
.submit-btn:active{transform:scale(.95)}
.submit-btn text{font-size:28rpx;font-weight:600;color:#fff}
.submit-btn--deliver{background:linear-gradient(135deg,var(--success),#10b981)}
.submit-btn--confirm{background:linear-gradient(135deg,var(--warning),#E8553A)}
.submit-btn--disabled{background:var(--outline);box-shadow:none;pointer-events:none}

/* 取货上传 */
.pickup-section{display:flex;flex-direction:column;gap:20rpx}
.upload-hint{font-size:26rpx;color:var(--text-tertiary);margin-top:8rpx}
.cancel-btn{display:flex;justify-content:center;padding:22rpx;border:1rpx solid var(--error);border-radius:48rpx}
.cancel-btn text{font-size:28rpx;font-weight:600;color:var(--error)}
.cancel-btn:active{background:var(--error-container)}
.cancel-btn--disabled{pointer-events:none;opacity:.5}

.bottom-placeholder{height:120rpx}
:deep(.uni-card){border-radius:var(--radius-card)!important}
</style>
