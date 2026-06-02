<template>
  <view class="page">
    <uni-nav-bar title="订单详情" backgroundColor="#FAFAF8" :border="false" statusBar fixed leftIcon="left" @clickLeft="onBack" color="#1C1B1A" />

    <scroll-view class="main-scroll" :style="{ height: scrollHeight + 'px' }" scroll-y enhanced :show-scrollbar="false">
      <!-- 完成状态横幅 -->
      <view class="status-banner animate-bounce-in" :class="'status-banner--' + (isCompleted ? 'done' : 'cancelled')">
        <view class="status-icon-wrap">
          <iconpark-icon :name="isCompleted ? 'checkbox-filled' : 'closeempty'" size="48" :color="isCompleted ? '#34d399' : '#737784'" />
        </view>
        <text class="status-title">{{ isCompleted ? '订单已完成' : '订单已取消' }}</text>
        <text class="status-desc">{{ isCompleted ? '感谢使用小i跑腿，期待下次为您服务' : cancelReasonText }}</text>
        <text v-if="isCompleted && order.confirmTime" class="status-time">完成时间：{{ order.confirmTime }}</text>
        <text v-if="!isCompleted && order.cancelTime" class="status-time">取消时间：{{ order.cancelTime }}</text>
      </view>

      <!-- 取消详情卡片 -->
      <view class="info-card" v-if="!isCompleted">
        <view class="card-title">
          <iconpark-icon name="info-filled" size="18" color="#737784" />
          <text>取消详情</text>
        </view>
        <view class="card-row">
          <text class="row-label">取消原因</text>
          <text class="row-value">{{ order.cancelReason || '未知原因' }}</text>
        </view>
        <view class="card-row" v-if="order.cancelTime">
          <text class="row-label">取消时间</text>
          <text class="row-value">{{ order.cancelTime }}</text>
        </view>
        <view class="card-row">
          <text class="row-label">取消方式</text>
          <text class="row-value">{{ cancelMethodText }}</text>
        </view>
      </view>

      <!-- 对方信息卡片 -->
      <view class="contact-card animate-slide-right delay-2" v-if="otherParty.name" @click="goRiderProfile">
        <image v-if="otherParty.avatar" class="contact-avatar-img" :src="otherParty.avatar" mode="aspectFill" />
        <view v-else class="contact-avatar" :style="{ background: otherParty.avatarBg }">{{ otherParty.initial }}</view>
        <view class="contact-info">
          <text class="contact-name">{{ otherParty.name }}</text>
          <text class="contact-role">{{ otherParty.roleLabel }}</text>
          <text class="contact-phone" v-if="otherParty.phone">{{ otherParty.phone }}</text>
        </view>
        <view class="contact-actions" v-if="otherParty.phone">
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

      <!-- 评价区域（仅发布方可创建评价） -->
      <view class="info-card" v-if="dataReady && isCompleted && !isRunner && !reviewSubmitted">
        <view class="card-title">
          <iconpark-icon name="star-filled" size="18" color="#fbbf24" />
          <text>评价订单</text>
        </view>
        <uni-rate v-model="rating" :max="5" size="40" activeColor="#fbbf24" :isFill="true" />
        <textarea class="rating-input" v-model="reviewContent" placeholder="说点什么吧，您的评价对我们很重要…" />
        <view class="tag-row">
          <view v-for="t in ratingTags" :key="t" class="rating-tag" :class="{ 'rating-tag--active': selectedTags.includes(t) }" @click="toggleTag(t)">{{ t }}</view>
        </view>
        <view class="review-submit-btn" :class="{ 'review-submit-btn--disabled': reviewSubmitting }" @click="onSubmitReview"><text>{{ reviewSubmitting ? '提交中…' : '提交评价' }}</text></view>
      </view>

      <view v-else-if="dataReady && isCompleted && !isRunner && reviewSubmitted" class="review-done">
        <iconpark-icon name="checkbox-filled" size="28" color="#34d399" />
        <text>评价已提交，感谢您的反馈！</text>
      </view>

      <!-- 已存在的评价展示（配送员可见发布方评价并可追加反馈） -->
      <view class="info-card" v-if="dataReady && isCompleted && publisherReview">
        <view class="card-title">
          <iconpark-icon name="star-filled" size="18" color="#fbbf24" />
          <text>发布方评价</text>
        </view>
        <view class="existing-review">
          <view class="review-header">
            <text class="review-rating">{{ '★'.repeat(publisherReview.rating) }}{{ '☆'.repeat(5 - publisherReview.rating) }}</text>
            <text class="review-time">{{ publisherReview.createdAt || '' }}</text>
          </view>
          <text class="review-body" v-if="publisherReview.content">{{ publisherReview.content }}</text>
          <view class="tag-row" v-if="publisherReview.tags && publisherReview.tags.length">
            <text v-for="t in publisherReview.tags" :key="t" class="rating-tag rating-tag--done">{{ t }}</text>
          </view>
        </view>
        <!-- 追评列表 -->
        <view v-if="publisherReview.followUps && publisherReview.followUps.length" class="follow-ups">
          <view v-for="f in publisherReview.followUps" :key="f.reviewId" class="follow-up-item">
            <text class="follow-up-author">{{ f.reviewerNickname || '对方' }}</text>
            <text class="follow-up-body">{{ f.content }}</text>
          </view>
        </view>
        <!-- 配送员追加反馈（仅配送员可见，且原评价人不是自己） -->
        <view v-if="isRunner && !myFollowUpDone" class="follow-up-input">
          <input class="form-input form-input--follow" v-model="followUpContent" placeholder="对发布方的评价进行反馈…" />
          <view class="follow-up-btn" @click="onSubmitFollowUp" :class="{ 'follow-up-btn--disabled': !followUpContent.trim() || submittingFollowUp }">
            <text>{{ submittingFollowUp ? '提交中…' : '追加反馈' }}</text>
          </view>
        </view>
        <view v-else-if="isRunner && myFollowUpDone" class="review-done">
          <iconpark-icon name="checkbox-filled" size="20" color="#34d399" />
          <text>反馈已提交</text>
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
          <text class="proof-label">取货凭证</text>
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
          <text class="row-label">送达地址</text>
          <text class="row-value">{{ order.deliveryAddress }}</text>
        </view>
        <view v-if="order.deliverProofImgs && order.deliverProofImgs.length" class="proof-section">
          <text class="proof-label">送达凭证</text>
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

      <!-- 时间线 -->
      <view class="info-card">
        <view class="card-title">
          <iconpark-icon name="clock-filled" size="18" color="#F59E0B" />
          <text>时间记录</text>
        </view>
        <view class="detail-items">
          <view v-if="order.acceptTime" class="detail-item animate-fade-up delay-3">
            <text>接单时间</text>
            <text class="detail-value">{{ order.acceptTime }}</text>
          </view>
          <view v-if="order.pickupTime" class="detail-item animate-fade-up delay-4">
            <text>{{ isQueueWait ? '到达时间' : '取货时间' }}</text>
            <text class="detail-value">{{ order.pickupTime }}</text>
          </view>
          <view v-if="order.deliverTime" class="detail-item animate-fade-up delay-5">
            <text>{{ isQueueWait ? '结束时间' : '送达时间' }}</text>
            <text class="detail-value">{{ order.deliverTime }}</text>
          </view>
          <view v-if="order.confirmTime" class="detail-item detail-item--total animate-fade-up delay-6">
            <text>完成时间</text>
            <text class="detail-value detail-value--total">{{ order.confirmTime }}</text>
          </view>
        </view>
      </view>

      <!-- 再来一单 -->
      <view v-if="isCompleted && !isRunner" class="reorder-btn" @click="onReorder">
        <iconpark-icon name="refresh" size="18" color="#1C1B1A" />
        <text>再来一单</text>
      </view>

      <!-- 删除订单（已完成超过7天） -->
      <view v-if="canDelete" class="delete-btn" :class="{ 'delete-btn--disabled': deleteSubmitting }" @click="onDeleteOrder">
        <text>{{ deleteSubmitting ? '删除中…' : '删除订单' }}</text>
      </view>

      <view class="bottom-placeholder"></view>
    </scroll-view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { orderApi, reviewApi, taskApi } from '@/api'
import { TASK_TYPES, TASK_TYPE_META, TYPE_FROM_API, isQueueWaitType } from '@/utils/constants.js'
import { parseTaskSpecs, parseShoppingItemsFromSpecs, parseBookCountFromSpecs, parsePrintSpecsFromSpecs, parseMerchantInfoFromSpecs } from '@/utils/campus-data.js'
import { useSubmitLock } from '@/utils/submit-guard'
import { SERVER_ORIGIN } from '@/utils/config'

function normalizeUrl(url) {
  if (!url) return ''
  if (url.startsWith('http')) return url
  if (url.startsWith('/')) return SERVER_ORIGIN + url
  return url
}

const sysInfo = uni.getSystemInfoSync()
const scrollHeight = sysInfo.windowHeight - sysInfo.statusBarHeight - 44

const orderId = ref('')
const taskId = ref('')
const dataReady = ref(false)
const order = ref({ orderStatus: 4 })
const isCompleted = computed(() => order.value.orderStatus === 4)
const canDelete = computed(() => {
  if (order.value.orderStatus !== 4 || !order.value.confirmTime) return false
  const daysDiff = (Date.now() - new Date(order.value.confirmTime).getTime()) / (86400000)
  return daysDiff > 7
})
const isRunner = computed(() => !!(order.value.isOwnerRunner))
const cancelReasonText = computed(() => order.value.cancelReason || '用户主动取消')

const rating = ref(0)
const reviewContent = ref('')
const selectedTags = ref([])
const reviewSubmitted = ref(false)
const ratingTags = ['态度好', '速度快', '包装仔细', '沟通及时', '准时送达']
// 配送员追评
const publisherReview = ref(null)
const followUpContent = ref('')
const myFollowUpDone = ref(false)
const submittingFollowUp = ref(false)
const { lock: reviewLock, unlock: reviewUnlock, locked: reviewSubmitting } = useSubmitLock()
const { lock: deleteLock, unlock: deleteUnlock, locked: deleteSubmitting } = useSubmitLock()

const rewardText = computed(() => Number(order.value.reward || 0).toFixed(2))
const taskSpecs = computed(() => parseTaskSpecs(order.value.taskSpecs))

const taskTypeCode = computed(() => typeof order.value.type === 'number' ? order.value.type : (TYPE_FROM_API[order.value.type] || 1))

const typeLabel = computed(() => TASK_TYPES[taskTypeCode.value] || '任务')
const typeMeta = computed(() => TASK_TYPE_META[taskTypeCode.value] || TASK_TYPE_META[1])
const typeIcon = computed(() => typeMeta.value.icon)
const typeIconColor = computed(() => typeMeta.value.color)
const typeColor = computed(() => ({ 1: 'blue', 2: 'orange', 3: 'green', 4: 'teal' }[taskTypeCode.value] || 'blue'))
const isQueueWait = computed(() => isQueueWaitType(order.value.subType))

const cancelMethodText = computed(() => {
  const reason = order.value.cancelReason || ''
  if (reason.includes('超时无人接单')) return '系统自动取消（超时无人接单）'
  if (reason.includes('未按时取货')) return '系统自动取消（配送超时）'
  if (reason) return '用户主动取消'
  return '未知'
})

const otherParty = computed(() => {
  if (order.value.isOwnerPublisher) {
    return {
      name: order.value.runnerNickname || '跑腿员',
      initial: (order.value.runnerNickname || '跑').charAt(0),
      roleLabel: '本次为您服务的跑腿员',
      avatarBg: 'linear-gradient(135deg,#4facfe,#43e97b)',
      avatar: normalizeUrl(order.value.runnerAvatar),
      id: order.value.runnerId,
      phone: order.value.runnerPhone
    }
  }
  if (order.value.isOwnerRunner) {
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
  return {}
})

const displayDescription = computed(() => {
  const desc = order.value.publicDesc || ''
  const specs = taskSpecs.value
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

onLoad((options) => {
  orderId.value = options?.orderId || ''
  taskId.value = options?.taskId || ''
  loadData()
})

async function loadData() {
  try {
    let data
    const silent = { showLoading: false }
    const silentNoError = { showLoading: false, showError: false }
    if (orderId.value) {
      data = await orderApi.getOrderDetail(orderId.value, silent)
    } else if (taskId.value) {
      try {
        data = await orderApi.getOrderByTask(taskId.value, silentNoError)
      } catch (e) {
        // 订单可能已取消/不存在，回退加载任务详情
        const taskData = await taskApi.getTaskDetail(taskId.value, silent)
        order.value = {
          orderStatus: taskData.status === 6 ? 5 : 4,
          taskId: taskData.taskId,
          taskNo: taskData.taskNo || '',
          type: taskData.type,
          subType: taskData.subType,
          taskDescription: taskData.description,
          description: taskData.description,
          reward: taskData.reward,
          pickupAddress: taskData.pickupAddress,
          deliveryAddress: taskData.deliveryAddress,
          pickupCode: taskData.pickupCode,
          imageUrls: taskData.imageUrls || [],
          publisherNickname: taskData.publisherNickname,
          publisherAvatar: taskData.publisherAvatar,
          cancelReason: taskData.cancelReason || '',
          cancelTime: taskData.cancelTime || '',
          isOwnerPublisher: true,
          isOwnerRunner: false
        }
        dataReady.value = true
        return
      }
    }
    if (data) {
      order.value = data
      orderId.value = data.orderId
      taskId.value = data.taskId
    }
    // 加载任务评价
    if (isCompleted.value && taskId.value) {
      loadReviews()
    }
    dataReady.value = true
  } catch (e) {
    uni.showToast({ title: '加载失败，请重试', icon: 'none' })
    dataReady.value = true
  }
}

async function loadReviews() {
  try {
    const reviews = await reviewApi.getTaskReviews(taskId.value)
    // 找到发布方的评价（非追评，有评分）
    const pubReview = (reviews || []).find(r => !r.parentId && r.rating)
    if (pubReview) {
      publisherReview.value = pubReview
      reviewSubmitted.value = true
      // 检查当前配送员是否已经追加过反馈
      if (pubReview.followUps && pubReview.followUps.length) {
        const myId = order.value.isOwnerRunner ? order.value.runnerId : order.value.publisherId
        // 配送员在追评中以reviewerId出现
      }
    }
  } catch (e) { /* ignore */ }
}

function toggleTag(tag) {
  const idx = selectedTags.value.indexOf(tag)
  if (idx > -1) selectedTags.value.splice(idx, 1)
  else selectedTags.value.push(tag)
}

async function onSubmitReview() {
  if (rating.value === 0) {
    uni.showToast({ title: '请选择评分', icon: 'none' })
    return
  }
  if (!reviewLock()) return
  try {
    await reviewApi.createReview({
      taskId: taskId.value,
      rating: rating.value,
      content: reviewContent.value,
      tags: selectedTags.value
    })
    uni.showToast({ title: '感谢评价！', icon: 'success' })
    reviewSubmitted.value = true
    loadReviews()
  } catch (e) { /* handled */ } finally {
    reviewUnlock()
  }
}

async function onSubmitFollowUp() {
  if (!followUpContent.value.trim() || submittingFollowUp.value) return
  if (!publisherReview.value) {
    uni.showToast({ title: '暂无评价可追加', icon: 'none' })
    return
  }
  submittingFollowUp.value = true
  try {
    await reviewApi.followUpReview(publisherReview.value.reviewId, followUpContent.value.trim())
    uni.showToast({ title: '反馈已提交', icon: 'success' })
    myFollowUpDone.value = true
    // 刷新追评列表
    loadReviews()
  } catch (e) { /* handled */ }
  submittingFollowUp.value = false
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
  if (isRunner.value) return
  const rid = otherParty.value.id
  if (rid) uni.navigateTo({ url: `/pages/rider-profile/rider-profile?runnerId=${rid}` })
}

function onReorder() {
  uni.navigateTo({ url: `/pages/service-publish/service-publish?type=${taskTypeCode.value}` })
}

async function onDeleteOrder() {
  const res = await new Promise(r => {
    uni.showModal({ title: '删除订单', content: '删除后仅自己不可见，确定删除？', success: r2 => r(r2.confirm) })
  })
  if (!res) return
  if (!deleteLock()) return
  try {
    await orderApi.deleteOrder(orderId.value)
    uni.showToast({ title: '已删除', icon: 'success' })
    setTimeout(() => uni.navigateBack(), 1000)
  } catch (e) { /* handled */ } finally {
    deleteUnlock()
  }
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

function copyOrderNo(no) {
  uni.setClipboardData({ data: no, success: () => uni.showToast({ title: '已复制', icon: 'success' }) })
}
</script>

<style scoped>
.page{width:100%;height:100vh;display:flex;flex-direction:column;background:var(--background);overflow:hidden}
.main-scroll{box-sizing:border-box;width:100%;padding:0 32rpx}

/* 状态横幅 */
.status-banner{display:flex;flex-direction:column;align-items:center;padding:40rpx 32rpx 28rpx;border-radius:var(--radius-card);margin-top:16rpx}
.status-banner--done{background:linear-gradient(135deg,var(--secondary-container),var(--surface))}
.status-banner--cancelled{background:linear-gradient(135deg,var(--surface-hover),var(--surface))}
.status-icon-wrap{width:104rpx;height:104rpx;border-radius:50%;display:flex;align-items:center;justify-content:center;margin-bottom:18rpx}
.status-banner--done .status-icon-wrap{background:rgba(52,211,153,.1)}
.status-banner--cancelled .status-icon-wrap{background:rgba(143,141,136,.1)}
.status-title{font-size:38rpx;font-weight:700;color:var(--text-primary);margin-bottom:8rpx}
.status-desc{font-size:26rpx;color:var(--text-tertiary);text-align:center;max-width:440rpx}
.status-time{font-size:22rpx;color:var(--text-tertiary);margin-top:14rpx}

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
.desc-text{font-size:26rpx;color:var(--text-primary);line-height:1.6}
.package-tags{display:flex;flex-wrap:wrap;gap:10rpx;margin-bottom:14rpx}
.package-tag{font-size:22rpx;padding:6rpx 16rpx;background:var(--primary-container);color:var(--primary);border-radius:8rpx}

.proof-section{margin-top:20rpx;padding-top:18rpx;border-top:1rpx dashed var(--outline-light)}
.proof-label{font-size:24rpx;font-weight:500;color:var(--text-secondary);display:block;margin-bottom:12rpx}
.proof-images{display:flex;gap:12rpx;flex-wrap:wrap}
.proof-img{width:120rpx;height:120rpx;border-radius:14rpx;object-fit:cover}

/* 时间记录 */
.detail-items{margin-top:8rpx}
.detail-item{display:flex;justify-content:space-between;padding:16rpx 0;font-size:26rpx;color:var(--text-secondary);border-bottom:1rpx solid var(--surface)}
.detail-item:last-child{border-bottom:none}
.detail-item--total{border-top:1rpx solid var(--outline-light);margin-top:8rpx;padding-top:18rpx}
.detail-value{font-weight:500;color:var(--text-primary)}
.detail-value--total{font-weight:600}

/* 评价 */
.rating-input{width:100%;height:140rpx;background:var(--surface);border-radius:14rpx;padding:20rpx;font-size:26rpx;color:var(--text-primary);box-sizing:border-box;margin-top:16rpx}
.tag-row{display:flex;flex-wrap:wrap;gap:12rpx;margin-top:16rpx}
.rating-tag{padding:10rpx 22rpx;border-radius:48rpx;background:var(--surface);font-size:24rpx;color:var(--text-secondary)}
.rating-tag--active{background:var(--primary);color:#fff}
.review-submit-btn{display:flex;justify-content:center;padding:20rpx;background:var(--primary);border-radius:48rpx;margin-top:18rpx}
.review-submit-btn text{font-size:26rpx;font-weight:600;color:#fff}
.review-submit-btn:active{transform:scale(.95)}
.review-submit-btn--disabled{pointer-events:none;opacity:.6}

.review-done{display:flex;align-items:center;justify-content:center;gap:12rpx;padding:28rpx;background:var(--surface-raised);border-radius:var(--radius-card);box-shadow:var(--shadow-sm);margin-top:20rpx}
.review-done text{font-size:28rpx;font-weight:500;color:var(--success)}
.existing-review{padding:16rpx 0}
.review-header{display:flex;justify-content:space-between;align-items:center;margin-bottom:10rpx}
.review-rating{font-size:28rpx;color:#fbbf24;letter-spacing:4rpx}
.review-time{font-size:22rpx;color:var(--text-tertiary)}
.review-body{font-size:26rpx;color:var(--text-primary);line-height:1.6;display:block}
.rating-tag--done{background:#f0fdf4;color:#16a34a}
.follow-ups{margin-top:20rpx;padding-top:16rpx;border-top:1rpx solid var(--surface-hover)}
.follow-up-item{padding:14rpx 0}
.follow-up-author{font-size:24rpx;font-weight:600;color:var(--primary);margin-right:12rpx}
.follow-up-body{font-size:26rpx;color:var(--text-primary)}
.follow-up-input{display:flex;align-items:center;gap:12rpx;margin-top:20rpx;padding-top:20rpx;border-top:1rpx solid var(--surface-hover)}
.form-input--follow{flex:1;height:72rpx;background:var(--surface);border-radius:16rpx;padding:0 20rpx;font-size:26rpx;color:var(--text-primary);box-sizing:border-box}
.follow-up-btn{padding:14rpx 24rpx;background:var(--primary);border-radius:36rpx;flex-shrink:0}
.follow-up-btn:active{transform:scale(.95)}
.follow-up-btn text{font-size:24rpx;font-weight:500;color:#fff}
.follow-up-btn--disabled{background:var(--outline);pointer-events:none}

.reorder-btn{display:flex;align-items:center;justify-content:center;gap:10rpx;padding:28rpx;border:1rpx solid var(--outline);border-radius:48rpx;margin-top:24rpx;background:var(--surface-raised)}
.reorder-btn text{font-size:28rpx;font-weight:500;color:var(--text-primary)}
.reorder-btn:active{background:var(--surface)}

.delete-btn{display:flex;align-items:center;justify-content:center;padding:20rpx;border:1rpx solid var(--error);border-radius:48rpx;margin-top:24rpx;background:var(--surface-raised)}
.delete-btn text{font-size:24rpx;font-weight:500;color:var(--text-tertiary)}
.delete-btn:active{background:var(--error-container)}
.delete-btn--disabled{pointer-events:none;opacity:.5}

.bottom-placeholder{height:120rpx}
:deep(.uni-card){border-radius:var(--radius-card)!important}
</style>
