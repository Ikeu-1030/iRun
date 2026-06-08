<template>
  <view class="page">
    <uni-nav-bar title="任务详情" backgroundColor="#FAFAF8" :border="false" statusBar fixed leftIcon="left" @clickLeft="onBack" color="#1C1B1A" />

    <scroll-view class="main-scroll" :style="{ height: scrollHeight + 'px' }" scroll-y enhanced :show-scrollbar="false" refresher-enabled @refresherrefresh="loadData" :refresher-triggered="refreshing">
      <!-- 状态横幅 -->
      <view class="status-banner animate-scale-pop" :class="'status-banner--' + statusStyle">
        <view class="status-icon-wrap">
          <custom-icon v-if="statusIconName" :name="statusIconName" size="56" />
          <iconpark-icon v-else :name="statusIcon" size="44" :color="statusColor" />
        </view>
        <text class="status-title">{{ statusTitle }}</text>
        <text class="status-desc">{{ statusDesc }}</text>
        <view v-if="task.status === 1 && task.expireTime" class="expire-row">
          <iconpark-icon name="info" size="14" color="#F59E0B" />
          <text>过期时间：{{ task.expireTime }}</text>
        </view>
        <view v-else-if="task.status >= 2 && task.status < 5" class="status-action" @click="goDelivering">
          <text>查看配送详情</text>
        </view>
      </view>

      <!-- ========== 发布者 / 接单方视图 ========== -->
      <template v-if="isAcceptedRunner || isOwnerPublisher">
        <!-- 跑腿员信息（已接单后可见） -->
        <view class="contact-card animate-slide-right delay-1" v-if="runnerInfo.name" @click="goRiderProfile(runnerInfo.id)">
          <image v-if="runnerInfo.avatar" class="contact-avatar-img" :src="runnerInfo.avatar" mode="aspectFill" />
          <view v-else class="contact-avatar" :style="{ background: runnerInfo.avatarBg }">{{ runnerInfo.initial }}</view>
          <view class="contact-info">
            <text class="contact-name">{{ runnerInfo.name }}</text>
            <text class="contact-role">跑腿员</text>
            <text class="contact-phone" v-if="runnerInfo.phone">{{ runnerInfo.phone }}</text>
          </view>
          <view class="contact-actions">
            <view class="c-action" @click="onChatRunner">
              <iconpark-icon name="chat" size="22" color="#FF6B4A" />
              <text>联系</text>
            </view>
            <view class="c-action c-action--call" @click="onCallRunner">
              <iconpark-icon name="phone" size="22" color="#fff" />
              <text>电话</text>
            </view>
          </view>
        </view>

        <view class="info-card animate-fade-up delay-2">
          <view class="card-header">
            <view class="type-badge" :class="'type-badge--' + typeColor">
              <iconpark-icon :name="typeIcon" size="22" :color="typeIconColor" />
              <text>{{ typeLabel }}</text>
            </view>
            <view class="order-no-row">
              <text class="order-no">订单号：{{ task.taskNo || '--' }}</text>
              <view class="copy-btn" v-if="task.taskNo" @click.stop="copyOrderNo(task.taskNo)">
                <iconpark-icon name="copy" size="14" color="#FF6B4A" />
              </view>
            </view>
          </view>
          <view class="card-row card-row--price">
            <text class="row-label">报酬金额</text>
            <text class="row-value row-value--price">¥{{ rewardText }}</text>
          </view>
          <view v-if="productFeeText" class="card-row">
            <text class="row-label">预估商品费</text>
            <text class="row-value">¥{{ productFeeText.toFixed(2) }}（含在总报酬中）</text>
          </view>
          <view class="card-row">
            <text class="row-label">创建时间</text>
            <text class="row-value">{{ task.publishTime || '--' }}</text>
          </view>
          <view class="card-row" v-if="task.requireSex !== undefined && task.requireSex !== null">
            <text class="row-label">性别限制</text>
            <text class="row-value">{{ task.requireSex === '男' ? '仅限男生' : task.requireSex === '女' ? '仅限女生' : '不限' }}</text>
          </view>
        </view>

        <view class="info-card">
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

        <view class="info-card" v-if="task.pickupAddress || task.pickupCode">
          <view class="card-title">
            <custom-icon name="pickup-info" size="32" />
            <text>{{ isQueueWait ? '代办信息' : '取件信息' }}</text>
          </view>
          <view class="card-row" v-if="task.pickupAddress">
            <text class="row-label">{{ isQueueWait ? '代办地址' : '取件地址' }}</text>
            <text class="row-value">{{ task.pickupAddress }}</text>
          </view>
          <view class="card-row" v-if="task.pickupCode">
            <text class="row-label">取件码</text>
            <text class="row-value row-value--code">{{ task.pickupCode }}</text>
          </view>
        </view>

        <view class="info-card" v-if="task.deliveryAddress">
          <view class="card-title">
            <custom-icon name="delivery-info" size="32" />
            <text>送达地址</text>
          </view>
          <view class="card-row">
            <text class="row-label">收货人</text>
            <text class="row-value">{{ deliveryFullName }}</text>
          </view>
          <view class="card-row" v-if="deliveryContactPhone">
            <text class="row-label">联系电话</text>
            <text class="row-value row-value--phone">{{ deliveryContactPhone }}</text>
          </view>
          <view class="card-row">
            <text class="row-label">收货地址</text>
            <text class="row-value">{{ deliveryAddressDetail }}</text>
          </view>
        </view>

        <view class="info-card" v-if="task.imageUrls && task.imageUrls.length">
          <view class="card-title">
            <custom-icon name="task-screenshot" size="32" />
            <text>任务截图</text>
          </view>
          <view class="proof-images">
            <image v-for="(url, i) in task.imageUrls" :key="i" :src="url" mode="aspectFill" class="proof-img" @click="previewImage(url)" />
          </view>
        </view>

      </template>

      <!-- ========== 其他用户视图（信息受限） ========== -->
      <template v-else>
        <view class="info-card">
          <view class="card-header">
            <view class="type-badge" :class="'type-badge--' + typeColor">
              <iconpark-icon :name="typeIcon" size="22" :color="typeIconColor" />
              <text>{{ typeLabel }}</text>
            </view>
            <view class="order-no-row">
              <text class="order-no">订单号：{{ task.taskNo || '--' }}</text>
              <view class="copy-btn" v-if="task.taskNo" @click.stop="copyOrderNo(task.taskNo)">
                <iconpark-icon name="copy" size="14" color="#FF6B4A" />
              </view>
            </view>
          </view>
          <view class="card-row card-row--price">
            <text class="row-label">报酬金额</text>
            <text class="row-value row-value--price">¥{{ rewardText }}</text>
          </view>
          <view v-if="productFeeText" class="card-row">
            <text class="row-label">预估商品费</text>
            <text class="row-value">¥{{ productFeeText.toFixed(2) }}（含在总报酬中）</text>
          </view>
          <view class="card-row">
            <text class="row-label">创建时间</text>
            <text class="row-value">{{ task.publishTime || '--' }}</text>
          </view>
          <view class="card-row" v-if="task.requireSex !== undefined && task.requireSex !== null">
            <text class="row-label">性别限制</text>
            <text class="row-value">{{ task.requireSex === '男' ? '仅限男生' : task.requireSex === '女' ? '仅限女生' : '不限' }}</text>
          </view>
        </view>

        <view class="info-card" v-if="task.pickupAddress">
          <view class="card-title">
            <view class="route-badge route-badge--from">起</view>
            <text>{{ isQueueWait ? '代办信息' : '取件信息' }}</text>
          </view>
          <view class="card-row">
            <text class="row-label">{{ isQueueWait ? '代办地址' : '取件地址' }}</text>
            <text class="row-value">{{ task.pickupAddress }}</text>
          </view>
          <view class="desc-hidden" v-if="task.hasPickupCode">
            <iconpark-icon name="locked-filled" size="16" color="#8F8D88" />
            <text>取件码已隐藏，接单后方可查看</text>
          </view>
        </view>

        <view class="info-card">
          <view class="card-title">
            <iconpark-icon name="compose" size="18" color="#FF6B4A" />
            <text>任务描述</text>
          </view>
          <view class="desc-hidden">
            <iconpark-icon name="locked-filled" size="16" color="#8F8D88" />
            <text>部分任务描述已隐藏，接单后方可查看</text>
          </view>
        </view>

        <view class="info-card">
          <view class="card-title">
            <iconpark-icon name="image-filled" size="18" color="#FF6B4A" />
            <text>上传信息</text>
          </view>
          <view class="desc-hidden">
            <iconpark-icon name="locked-filled" size="16" color="#8F8D88" />
            <text>任务截图/文件已隐藏，接单后方可查看</text>
          </view>
        </view>

        <view class="info-card" v-if="task.deliveryAddress">
          <view class="card-title">
            <view class="route-badge route-badge--to">终</view>
            <text>送达地址</text>
          </view>
          <view class="card-row">
            <text class="row-label">收货人</text>
            <text class="row-value row-value--masked">{{ deliveryMaskedName }}</text>
          </view>
          <view class="card-row">
            <text class="row-label">联系电话</text>
            <text class="row-value row-value--masked">{{ deliveryContactPhone }}</text>
          </view>
          <view class="card-row">
            <text class="row-label">收货地址</text>
            <text class="row-value">{{ deliveryAddressDetail }}</text>
          </view>
        </view>
      </template>

      <!-- 配送时间线（所有人可见） -->
      <view class="info-card">
        <view class="card-title">
          <custom-icon name="delivery-progress" size="32" />
          <text>配送进度</text>
        </view>
        <uni-steps :options="stepItems" :active="stepActive" activeColor="#FF6B4A" direction="column" />
      </view>

      <!-- 取消任务按钮（仅任务发布者在等待接单时可见） -->
      <view v-if="isOwnerPublisher && !isRunner && task.status === 1" class="cancel-btn" :class="{ 'cancel-btn--disabled': cancelLocked }" @click="onCancel">
        <text>取消任务</text>
      </view>

      <view class="bottom-placeholder"></view>
    </scroll-view>
  </view>
</template>

<script setup>
import { ref, computed, onUnmounted } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { useStore } from '@/store/index.js'
import { taskApi, orderApi } from '@/api'
import { TASK_STATUS, TASK_TYPES, TASK_TYPE_META, TYPE_FROM_API, isQueueWaitType } from '@/utils/constants.js'
import { parseDeliveryAddress, parseTaskSpecs, parseExpressPackagesFromSpecs, parseShoppingItemsFromSpecs, parseBookCountFromSpecs, parsePrintSpecsFromSpecs, parseMerchantInfoFromSpecs } from '@/utils/campus-data.js'
import { useSubmitLock } from '@/utils/submit-guard'
import { SERVER_ORIGIN } from '@/utils/config'

function normalizeUrl(url) {
  if (!url) return ''
  if (url.startsWith('http')) return url
  if (url.startsWith('/')) return SERVER_ORIGIN + url
  return url
}

const store = useStore()
const sysInfo = uni.getSystemInfoSync()
const scrollHeight = sysInfo.windowHeight - sysInfo.statusBarHeight - 44

const taskId = ref('')
const task = ref({ status: 1 })
const refreshing = ref(false)
const runnerInfo = ref({})
const isRunner = ref(false)
const isOwnerPublisher = ref(false)
const { lock: cancelLock, unlock: cancelUnlock, locked: cancelLocked } = useSubmitLock()

// 当前用户是否是已接单的跑腿员（只有真正接单的跑腿员才能看敏感信息）
const isAcceptedRunner = computed(() => {
  return isRunner.value && runnerInfo.value.id && String(runnerInfo.value.id) === String(store.userId)
})

const taskTypeCode = computed(() => typeof task.value.type === 'number' ? task.value.type : (TYPE_FROM_API[task.value.type] || 1))

const typeLabel = computed(() => TASK_TYPES[taskTypeCode.value] || '任务')
const rewardText = computed(() => Number(task.value.reward || 0).toFixed(2))
const taskSpecs = computed(() => parseTaskSpecs(task.value.taskSpecs))

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
  const val = task.value.subType || ''
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
const typeMeta = computed(() => TASK_TYPE_META[taskTypeCode.value] || TASK_TYPE_META[1])
const typeIcon = computed(() => typeMeta.value.icon)
const typeIconColor = computed(() => typeMeta.value.color)
const typeColor = computed(() => ({ 1: 'blue', 2: 'orange', 3: 'green', 4: 'teal' }[taskTypeCode.value] || 'blue'))

const statusMap = {
  1: { style: 'waiting', icon: 'search', iconName: 'pending-confirm', color: '#FF6B4A', title: '等待接单中', desc: '任务已发布，正在为您匹配跑腿同学' },
  2: { style: 'accepted', icon: 'checkbox-filled', color: '#e67e22', title: '已接单', desc: '跑腿同学已接单，准备为您取件' },
  3: { style: 'delivering', icon: 'location-filled', iconName: 'delivering', color: '#FF6B4A', title: '配送中', desc: '跑腿同学正在为您配送' },
  4: { style: 'confirming', icon: 'checkmarkempty', color: '#34d399', title: '待确认收货', desc: '跑腿同学已送达，请确认收货' },
  5: { style: 'done', icon: 'checkbox-filled', color: '#34d399', title: '已完成', desc: '感谢使用 Campus Express' },
  6: { style: 'cancelled', icon: 'closeempty', color: '#8F8D88', title: '已取消', desc: '该任务已被取消' }
}
// 接单方视角的状态文案
const runnerStatusMap = {
  1: { title: '待接单', desc: '您可接取此任务' },
  2: { title: '待取件', desc: '您已接单，请前往取件' },
  3: { title: '配送中', desc: '正在配送，请及时送达' },
  4: { title: '待确认', desc: '已送达，等待发布者确认收货' }
}
const statusStyle = computed(() => statusMap[task.value.status]?.style || 'waiting')
const statusIcon = computed(() => statusMap[task.value.status]?.icon || 'search')
const statusIconName = computed(() => statusMap[task.value.status]?.iconName || '')
const statusColor = computed(() => statusMap[task.value.status]?.color || '#FF6B4A')
const statusTitle = computed(() => {
  if (isRunner.value && runnerStatusMap[task.value.status]) return runnerStatusMap[task.value.status].title
  return statusMap[task.value.status]?.title || '加载中'
})
const statusDesc = computed(() => {
  if (isOwnerPublisher.value) return statusMap[task.value.status]?.desc || ''
  if (isRunner.value && runnerStatusMap[task.value.status]) return runnerStatusMap[task.value.status].desc
  return statusMap[task.value.status]?.desc || ''
})

// 收货人信息（后端直接返回地址簿联系人）
const rawContactName = computed(() => {
  return task.value.contactName || ''
})

// 收货人全名（含性别后缀），仅接单方/发布者可见
const deliveryFullName = computed(() => {
  const name = rawContactName.value
  if (!name) return "未知"
  const gender = task.value.deliveryGender
  if (gender === 1 || gender === '1' || gender === '男') return name + '先生'
  if (gender === 0 || gender === '0' || gender === '女') return name + '女士'
  return name
})

// 收货人脱敏显示：仅显示姓氏，名用**代替
const deliveryMaskedName = computed(() => {
  const name = rawContactName.value
  if (!name) return '***'
  if (name.length <= 1) return name + '**'
  return name.charAt(0) + '**'
})
const deliveryContactPhone = computed(() => {
  const phone = task.value.contactPhone || ''
  if (isAcceptedRunner.value || isOwnerPublisher.value) return phone
  if (!phone || phone.length < 4) return '****'
  return '*'.repeat(phone.length - 4) + phone.slice(-4)
})
const deliveryAddressDetail = computed(() => {
  const addr = task.value.deliveryAddress || ''
  if (isAcceptedRunner.value || isOwnerPublisher.value) return addr
  if (!addr) return ''
  const parsed = parseDeliveryAddress(addr)
  // 有楼层 → 仅展示地点（隐藏楼层和门牌号）
  if (parsed.floor !== null) return parsed.location
  // 无楼层 → 地址不含隐私信息，完整展示
  return addr
})

// 描述：直接使用 publicDesc + privateNote
const displayDescription = computed(() => {
  const specs = taskSpecs.value
  const publicDesc = task.value.publicDesc || ''
  const privateNote = task.value.privateNote || ''
  // type=1 代取快递：包裹规格
  if (taskTypeCode.value === 1) {
    const pkgSpecs = parseExpressPackagesFromSpecs(specs)
    if (pkgSpecs) {
      let text = `快递规格：${pkgSpecs.sizes}`
      if (publicDesc) text += `\n需求描述：${publicDesc}`
      return text
    }
  }
  if (isAcceptedRunner.value || isOwnerPublisher.value) {
    return (publicDesc + (privateNote ? '\n[仅接单方可见] ' + privateNote : '')) || '暂无描述'
  }
  return publicDesc || '暂无描述'
})

const isQueueWait = computed(() => isQueueWaitType(task.value.subType))

const stepItems = computed(() => {
  if (isQueueWait.value) {
    return [
      { title: '等待接单', desc: '任务已发布' },
      { title: '已接单', desc: '跑腿员已接单' },
      { title: '待到达', desc: '跑腿员已到达，代办中' },
      { title: '待确认', desc: '跑腿员已完成代办' },
      { title: '已完成', desc: '确认代办完成' }
    ]
  }
  return [
    { title: '等待接单', desc: '任务已发布' },
    { title: '已接单', desc: '跑腿员已接单' },
    { title: '待送达', desc: '跑腿员已取货，配送中' },
    { title: '待确认', desc: '跑腿员已送达' },
    { title: '已完成', desc: '确认收货完成' }
  ]
})
const stepActive = computed(() => {
  const s = task.value.status
  return s === 1 ? 0 : s === 2 ? 1 : s === 3 ? 2 : s === 4 ? 3 : s >= 5 ? 4 : 0
})

onLoad((options) => {
  taskId.value = options?.taskId || ''
  loadData()
})

async function loadData() {
  refreshing.value = true
  // 重置状态，防止从上一页泄露
  isRunner.value = false
  isOwnerPublisher.value = false
  runnerInfo.value = {}
  try {
    const data = await taskApi.getTaskDetail(taskId.value, { showLoading: false })
    task.value = data
    isOwnerPublisher.value = data.isOwner === true

    // 已接单后尝试加载跑腿员信息
    if (data.status >= 2 && data.status < 6) {
      try {
        const orderData = await orderApi.getOrderByTask(taskId.value, { showLoading: false })
        runnerInfo.value = {
          name: orderData.runnerNickname || '跑腿员',
          initial: (orderData.runnerNickname || '跑').charAt(0),
          avatar: normalizeUrl(orderData.runnerAvatar),
          avatarBg: 'linear-gradient(135deg,#4facfe,#43e97b)',
          id: orderData.runnerId,
          phone: orderData.runnerPhone
        }
        // 确认当前用户是否为实际接单方
        if (orderData.runnerId && String(orderData.runnerId) === String(store.userId)) {
          isRunner.value = true
        }
        // 已完成 → 跳转完成页
        if (orderData.orderStatus === 4 || orderData.orderStatus === 5) {
          uni.redirectTo({ url: `/pages/order-completed/order-completed?orderId=${orderData.orderId}` })
          return
        }
      } catch (e) { /* order not found or not accessible */ }
    }

    // 已完成/已取消 → 跳转完成页
    if (data.status === 5 || data.status === 6) {
      uni.redirectTo({ url: `/pages/order-completed/order-completed?taskId=${taskId.value}` })
      return
    }

    // 已接单/配送中/待确认 → 仅订单双方自动跳转配送详情，其他人留在本页（隐私保护）
    if ((isOwnerPublisher.value || isRunner.value) && data.status >= 2 && data.status <= 4) {
      goDelivering()
    }
  } catch (e) { /* handled */ }
  refreshing.value = false
}

function goDelivering() {
  const role = isRunner.value ? 'runner' : 'publisher'
  uni.redirectTo({ url: `/pages/order-delivering/order-delivering?taskId=${taskId.value}&role=${role}` })
}

function onCancel() {
  if (!isOwnerPublisher.value) {
    uni.showToast({ title: '仅发布者可取消任务', icon: 'none' })
    return
  }
  uni.$on('cancelReasonSelected', async (reason) => {
    uni.$off('cancelReasonSelected')
    if (!cancelLock()) return
    try {
      await taskApi.cancelTask(taskId.value, reason)
      uni.showToast({ title: '已取消', icon: 'success' })
      task.value.status = 6
    } catch (e) { /* handled */ } finally {
      cancelUnlock()
    }
  })
  uni.navigateTo({ url: '/pages/cancel-reason/cancel-reason' })
}

onUnmounted(() => {
  uni.$off('cancelReasonSelected')
})

function onChatRunner() {
  const userId = runnerInfo.value.id || ''
  const nickname = encodeURIComponent(runnerInfo.value.name || '')
  uni.navigateTo({ url: `/pages/chat-detail/chat-detail?userId=${userId}&nickname=${nickname}` })
}

function onCallRunner() {
  if (runnerInfo.value.phone) {
    uni.makePhoneCall({ phoneNumber: runnerInfo.value.phone })
  } else {
    uni.showToast({ title: '暂无对方联系方式', icon: 'none' })
  }
}

function copyOrderNo(no) {
  uni.setClipboardData({
    data: no,
    success: () => uni.showToast({ title: '已复制', icon: 'success' })
  })
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

function goRiderProfile(runnerId) { uni.navigateTo({ url: `/pages/rider-profile/rider-profile?runnerId=${runnerId}` }) }
function onBack() { uni.navigateBack() }
</script>

<style scoped>
.page{width:100%;height:100vh;display:flex;flex-direction:column;background:var(--background);overflow:hidden}
.main-scroll{box-sizing:border-box;width:100%;padding:0 32rpx}

/* 状态横幅 */
.status-banner{display:flex;flex-direction:column;align-items:center;padding:40rpx 32rpx 28rpx;border-radius:var(--radius-card);margin-top:16rpx}
.status-banner--waiting{background:linear-gradient(135deg,var(--primary-container),var(--surface))}
.status-banner--accepted{background:linear-gradient(135deg,#fff7ed,var(--surface))}
.status-banner--delivering{background:linear-gradient(135deg,var(--primary-container),var(--surface))}
.status-banner--confirming{background:linear-gradient(135deg,var(--secondary-container),var(--surface))}
.status-banner--done{background:linear-gradient(135deg,var(--secondary-container),var(--surface))}
.status-banner--cancelled{background:linear-gradient(135deg,var(--surface-hover),var(--surface))}
.status-icon-wrap{width:100rpx;height:100rpx;border-radius:50%;display:flex;align-items:center;justify-content:center;margin-bottom:16rpx}
.status-banner--waiting .status-icon-wrap{background:var(--primary-container)}
.status-banner--accepted .status-icon-wrap{background:rgba(245,158,11,.1)}
.status-banner--delivering .status-icon-wrap{background:var(--primary-container)}
.status-banner--confirming .status-icon-wrap{background:rgba(52,211,153,.1)}
.status-banner--done .status-icon-wrap{background:rgba(52,211,153,.1)}
.status-banner--cancelled .status-icon-wrap{background:rgba(143,141,136,.1)}
.status-title{font-size:34rpx;font-weight:700;color:var(--text-primary);margin-bottom:6rpx}
.status-desc{font-size:26rpx;color:var(--text-secondary);text-align:center}
.expire-row{display:flex;align-items:center;gap:6rpx;margin-top:14rpx;padding:10rpx 20rpx;background:rgba(255,255,255,.7);border-radius:20rpx}
.expire-row text{font-size:22rpx;color:#ad6200}
.status-action{margin-top:16rpx;padding:10rpx 28rpx;background:rgba(255,255,255,.7);border-radius:var(--radius-card)}
.status-action text{font-size:26rpx;font-weight:500;color:var(--primary)}

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
.type-badge--blue{background:var(--primary-container);color:#FF6B4A}
.type-badge--orange{background:#fff7ed;color:#ad6200}
.type-badge--green{background:#f0fdf4;color:#166534}
.type-badge--teal{background:#ecfeff;color:#0e7490}
.order-no{font-size:22rpx;color:var(--text-tertiary)}
.order-no-row{display:flex;align-items:center;gap:8rpx}
.copy-btn{width:44rpx;height:44rpx;border-radius:50%;background:var(--primary-container);display:flex;align-items:center;justify-content:center}
.copy-btn:active{background:#FFD1C7}
.card-row{display:flex;justify-content:space-between;align-items:flex-start;padding:12rpx 0}
.package-tags{display:flex;flex-wrap:wrap;gap:10rpx;margin-bottom:14rpx}
.package-tag{font-size:22rpx;padding:6rpx 16rpx;background:var(--primary-container);color:var(--primary);border-radius:8rpx}
.card-row--price{background:var(--surface);border-radius:14rpx;padding:18rpx 20rpx;margin-top:4rpx}
.row-label{font-size:26rpx;color:var(--text-secondary);flex-shrink:0;min-width:140rpx}
.row-value{font-size:26rpx;font-weight:500;color:var(--text-primary);text-align:right;flex:1}
.row-value--price{font-size:34rpx;font-weight:700;color:var(--primary)}
.row-value--code{font-size:30rpx;font-weight:700;color:var(--warning);letter-spacing:2rpx}
.row-value--phone{font-size:26rpx;font-weight:500;color:var(--primary);letter-spacing:2rpx}
.row-value--masked{font-size:26rpx;color:var(--text-tertiary);letter-spacing:4rpx}
.desc-text{font-size:26rpx;color:var(--text-primary);line-height:1.6;white-space:pre-wrap}
.desc-hidden{display:flex;align-items:center;gap:10rpx;padding:20rpx;background:var(--surface);border-radius:14rpx}
.desc-hidden text{font-size:24rpx;color:var(--text-tertiary)}

.proof-images{display:flex;gap:12rpx;flex-wrap:wrap}
.proof-img{width:120rpx;height:120rpx;border-radius:14rpx;object-fit:cover}

.route-badge{width:40rpx;height:40rpx;border-radius:10rpx;display:inline-flex;align-items:center;justify-content:center;font-size:22rpx;font-weight:700;color:#fff;flex-shrink:0;margin-right:8rpx}
.route-badge--from{background:#1C1B1A}
.route-badge--to{background:var(--primary)}

.cancel-btn{display:flex;justify-content:center;padding:24rpx;border:1rpx solid var(--error);border-radius:48rpx;margin:28rpx auto 0;width:fit-content;min-width:240rpx}
.cancel-btn text{font-size:28rpx;font-weight:600;color:var(--error)}
.cancel-btn:active{background:var(--error-container)}
.cancel-btn--disabled{pointer-events:none;opacity:.5}

.bottom-placeholder{height:120rpx}
:deep(.uni-card){border-radius:var(--radius-card)!important}
</style>
