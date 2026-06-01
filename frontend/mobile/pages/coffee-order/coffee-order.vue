<template>
  <view class="page">
    <uni-nav-bar title="奶茶咖啡代取" backgroundColor="#FAFAF8" :border="false" statusBar fixed leftIcon="left" @clickLeft="onBack" color="#1C1B1A" />

    <scroll-view class="main-scroll" :style="{ height: scrollHeight + 'px' }" scroll-y enhanced :show-scrollbar="false">
      <view class="page-header">
        <text class="page-title">奶茶咖啡代取</text>
        <text class="page-subtitle">奶茶咖啡代取，帮您送到手中</text>
      </view>

      <view class="form-card">
        <view class="card-title">取餐信息 <text class="required">*</text></view>
        <view class="form-label">取餐地点</view>
        <view class="addr-row">
          <view class="addr-badge addr-badge--pickup">取</view>
          <picker mode="selector" :range="pickupLocations" @change="onPickupLocationChange" style="flex:1">
            <view class="form-select"><text :class="{ 'form-select-placeholder': !pickupAddress }">{{ pickupAddress || '请选择取餐地点' }}</text><text class="select-arrow">▼</text></view>
          </picker>
        </view>
        <view v-if="pickupAddress === '自定义地点'" class="form-label">自定义取餐地点</view>
        <input v-if="pickupAddress === '自定义地点'" class="form-input" placeholder="请输入具体取餐地点" v-model="customPickupAddress" />
        <template v-if="pickupAddress !== '自定义地点'">
          <view class="form-label">商家信息</view>
          <input class="form-input" placeholder="如：XX奶茶店、XX咖啡店" v-model="merchantInfo" />
        </template>
        <view class="form-label">取货码 / 订单号</view>
        <input class="form-input form-input--large" placeholder="输入取货码，如：A042" v-model="pickupCode" />
        <view class="form-label">商品详情</view>
        <textarea class="form-textarea" placeholder="请输入具体奶茶/咖啡的名称、规格、要求等…" v-model="description" />
      </view>

      <view class="form-card">
        <view class="card-title">上传信息</view>
        <UploadGrid v-model="uploadedUrls" :maxCount="3" />
      </view>

      <view class="form-card">
        <view class="card-title">配送信息 <text class="required">*</text></view>
        <view class="addr-row">
          <view class="addr-badge addr-badge--deliver">收</view>
          <view class="form-addr-card" @click="onSelectAddress">
            <view class="addr-info"><text class="addr-main">{{ deliveryLabel || '选择配送终点' }}</text></view>
          </view>
        </view>
      </view>

      <view class="form-card">
        <view class="card-title">接单限制</view>
        <view class="chip-row">
          <view class="chip" :class="{ 'chip--active': requireSex === undefined }" @click="requireSex = undefined">不限</view>
          <view class="chip" :class="{ 'chip--active': requireSex === '男' }" @click="requireSex = '男'">仅男生</view>
          <view class="chip" :class="{ 'chip--active': requireSex === '女' }" @click="requireSex = '女'">仅女生</view>
        </view>
      </view>

      <view class="form-card form-card--pay">
        <view class="card-title">费用明细</view>
        <view class="fee-row"><text class="fee-label">基础配送费</text><text class="fee-value">¥ {{ baseFee.toFixed(2) }}</text></view>
        <view class="fee-divider"></view>
        <view class="form-label">赏金（提高接单率）</view>
        <view class="chip-row">
          <view class="chip" :class="{ 'chip--active': reward === 2 && !showCustomBounty }" @click="setReward(2)">¥2</view>
          <view class="chip" :class="{ 'chip--active': reward === 5 && !showCustomBounty }" @click="setReward(5)">¥5</view>
          <view class="chip" :class="{ 'chip--active': reward === 10 && !showCustomBounty }" @click="setReward(10)">¥10</view>
          <view class="chip" :class="{ 'chip--active': showCustomBounty }" @click="toggleCustomReward">自定义</view>
        </view>
        <view class="custom-bounty-row" v-if="showCustomBounty">
          <text class="custom-bounty-unit">¥</text>
          <input class="custom-bounty-input" name="digit" v-model.number="customBounty" placeholder="输入赏金金额" />
        </view>
        <view class="fee-divider"></view>
        <view class="fee-row fee-row--total"><text class="fee-label">合计支付</text><text class="fee-total">¥ {{ totalPrice.toFixed(2) }}</text></view>
      </view>

      <view class="bottom-placeholder"></view>
    </scroll-view>

    <view class="bottom-bar safe-area-bottom">
      <view class="bottom-bar-row">
        <view class="bottom-total"><text class="total-label">合计支付</text><text class="total-price">¥ {{ totalPrice.toFixed(2) }}</text></view>
        <view class="submit-btn" @click="onSubmit"><text>{{ submitting ? '发布中…' : '发布需求' }}</text></view>
      </view>
    </view>

    <PayPasswordDialog />
  </view>
</template>

<script setup>
import { ref, computed, onUnmounted } from 'vue'
import { taskApi } from '@/api'
import { TYPE_TO_API, SUBTYPE_TO_VALUE } from '@/utils/constants.js'
import UploadGrid from '@/components/upload-grid/upload-grid.vue'
import { promptPayPassword } from '@/utils/pay-password.js'
import PayPasswordDialog from '@/components/pay-password-dialog/pay-password-dialog.vue'

const sysInfo = uni.getSystemInfoSync()
const scrollHeight = sysInfo.windowHeight - sysInfo.statusBarHeight - 44

const pickupLocations = ['正阳餐厅', '正阳餐厅后花园餐厅', '霞光餐厅', '晨曦餐厅一楼', '晨曦餐厅二楼', '蓝区体育场小吃街', '霞光片区小吃街', '晨曦片区小吃街', '自定义地点']
const pickupAddress = ref('')
const customPickupAddress = ref('')
const merchantInfo = ref('')
const pickupCode = ref('')
const description = ref('')
const uploadedUrls = ref([])
const reward = ref(0)
const customBounty = ref(0)
const showCustomBounty = ref(false)
const deliveryAddressId = ref(null)
const deliveryLabel = ref('')
const deliveryContactName = ref('')
const deliveryContactPhone = ref('')
const requireSex = ref(undefined)
const submitting = ref(false)

const baseFee = 5
const totalPrice = computed(() => {
  const bounty = showCustomBounty.value ? customBounty.value || 0 : reward.value
  return baseFee + bounty
})

function onPickupLocationChange(e) {
  pickupAddress.value = pickupLocations[e.detail.value]
}

function setReward(val) {
  showCustomBounty.value = false
  reward.value = val
}

function toggleCustomReward() {
  showCustomBounty.value = !showCustomBounty.value
  if (showCustomBounty.value && customBounty.value === 0) {
    customBounty.value = reward.value || 5
  }
}

function onSelectAddress() {
  uni.$off('addressSelected')
  uni.$on('addressSelected', (addr) => {
    deliveryAddressId.value = addr.id
    deliveryLabel.value = `${addr.contactName} ${addr.detail}`
    deliveryContactName.value = addr.contactName || ''
    deliveryContactPhone.value = addr.contactPhone || ''
    uni.$off('addressSelected')
  })
  uni.navigateTo({ url: '/pages/address-list/address-list?selectMode=1' })
}

onUnmounted(() => {
  uni.$off('addressSelected')
})

async function onSubmit() {
  if (!pickupAddress.value) {
    uni.showToast({ title: '请选择取餐地点', icon: 'none' })
    return
  }
  if (pickupAddress.value === '自定义地点' && !customPickupAddress.value) {
    uni.showToast({ title: '请输入取餐地点', icon: 'none' })
    return
  }
  if (!deliveryAddressId.value) {
    uni.showToast({ title: '请选择配送地址', icon: 'none' })
    return
  }

  const pw = await promptPayPassword('支付赏金')
  if (!pw) return
  submitting.value = true
  try {
    let actualPickup = pickupAddress.value
    if (pickupAddress.value === '自定义地点') {
      actualPickup = customPickupAddress.value
    }

    const taskSpecsObj = {}
    if (merchantInfo.value) taskSpecsObj.商家信息 = merchantInfo.value
    const taskSpecsStr = Object.keys(taskSpecsObj).length > 0 ? JSON.stringify(taskSpecsObj) : undefined

    await taskApi.publishTask({
      type: TYPE_TO_API[2], subType: SUBTYPE_TO_VALUE[23],
      publicDesc: description.value || undefined,
      taskSpecs: taskSpecsStr,
      pickupCode: pickupCode.value || undefined,
      reward: totalPrice.value,
      payPassword: pw,
      pickupAddress: actualPickup || undefined,
      deliveryAddressId: deliveryAddressId.value || undefined,
      contactName: deliveryContactName.value || undefined,
      contactPhone: deliveryContactPhone.value || undefined,
      requireSex: requireSex.value,
      imageUrls: uploadedUrls.value.length > 0 ? [...uploadedUrls.value] : undefined
    })
    uni.showToast({ title: '发布成功', icon: 'success' })
    setTimeout(() => uni.navigateBack(), 1000)
  } catch (e) {
    if (e.message?.includes('请先设置支付密码')) guideToSetPayPassword()
  }
  submitting.value = false
}

function guideToSetPayPassword() {
  uni.showModal({
    title: '未设置支付密码',
    content: '您还未设置支付密码，请先前往设置。',
    confirmText: '去设置',
    success: (res) => {
      if (res.confirm) {
        uni.navigateTo({ url: '/pages/pay-password-edit/pay-password-edit?mode=set' })
      }
    }
  })
}

function onBack() { uni.navigateBack() }
</script>

<style scoped>
.page{width:100%;height:100vh;display:flex;flex-direction:column;background:var(--background);overflow:hidden}
.main-scroll{box-sizing:border-box;width:100%;padding:0 32rpx;padding-bottom:180rpx}
.page-header{margin-top:16rpx;margin-bottom:20rpx}
.page-title{font-size:44rpx;font-weight:700;color:var(--text-primary);display:block}
.page-subtitle{font-size:26rpx;color:var(--text-secondary);margin-top:6rpx;display:block}
.form-card{background:var(--surface-raised);border-radius:var(--radius-lg);padding:28rpx;margin-bottom:20rpx;box-shadow:var(--shadow-sm);border:1rpx solid var(--outline-light)}
.form-card--pay{border:2rpx solid var(--primary-container)}
.card-title{font-size:30rpx;font-weight:600;color:var(--text-primary);margin-bottom:20rpx;padding-bottom:16rpx;border-bottom:1rpx solid var(--outline-light)}
.required{color:#ef4444;font-size:22rpx;vertical-align:super;line-height:1}
.form-label{font-size:24rpx;font-weight:500;color:var(--text-secondary);margin-bottom:10rpx;margin-top:18rpx}
.form-label:first-child{margin-top:0}
.form-input{width:100%;height:84rpx;background:var(--surface);border-radius:20rpx;padding:0 28rpx;font-size:28rpx;color:var(--text-primary);box-sizing:border-box}
.form-input--large{height:100rpx;font-size:30rpx;font-weight:600;letter-spacing:4rpx}
.form-textarea{width:100%;background:var(--surface);border-radius:20rpx;padding:20rpx 28rpx;font-size:28rpx;color:var(--text-primary);box-sizing:border-box;min-height:120rpx}
.form-select{width:100%;height:84rpx;background:var(--surface);border-radius:20rpx;padding:0 28rpx;font-size:28rpx;color:var(--text-secondary);display:flex;align-items:center;justify-content:space-between;box-sizing:border-box}
.form-select-placeholder{color:var(--text-tertiary)}
.select-arrow{font-size:24rpx;color:var(--text-secondary)}
.form-select--fixed{width:100%;height:84rpx;background:var(--surface);border-radius:20rpx;padding:0 28rpx;font-size:28rpx;color:var(--text-primary);display:flex;align-items:center;box-sizing:border-box}
.form-addr-card{width:100%;background:var(--surface);border-radius:20rpx;padding:22rpx 28rpx;display:flex;align-items:center;justify-content:space-between;box-sizing:border-box}
.addr-main{font-size:28rpx;color:var(--text-primary);display:block}
.addr-row{display:flex;align-items:center;gap:14rpx}
.addr-badge{width:40rpx;height:40rpx;border-radius:10rpx;display:flex;align-items:center;justify-content:center;font-size:22rpx;font-weight:700;color:#fff;flex-shrink:0}
.addr-badge--pickup{background:var(--text-primary)}
.addr-badge--deliver{background:var(--primary)}
.chip-row{display:flex;flex-wrap:wrap;gap:14rpx}
.chip{padding:14rpx 28rpx;border-radius:48rpx;font-size:26rpx;font-weight:500;color:var(--text-secondary);background:var(--surface);text-align:center}
.chip--active{background:var(--primary);color:#fff;font-weight:600}
.fee-row{display:flex;align-items:center;justify-content:space-between;padding:8rpx 0}
.fee-row--total{margin-top:8rpx}
.fee-label{font-size:28rpx;color:var(--text-primary)}
.fee-value{font-size:28rpx;font-weight:600;color:var(--text-secondary)}
.fee-total{font-size:38rpx;font-weight:700;color:var(--primary)}
.fee-divider{height:1rpx;background:rgba(0,0,0,.04);margin:20rpx 0}
.custom-bounty-row{display:flex;align-items:center;margin-top:16rpx;background:var(--surface);border-radius:20rpx;padding:14rpx 24rpx}
.custom-bounty-unit{font-size:32rpx;font-weight:700;color:var(--primary);margin-right:8rpx}
.custom-bounty-input{flex:1;font-size:28rpx;color:var(--text-primary);background:transparent}
.bottom-bar{position:fixed;bottom:0;left:0;width:100%;background:var(--surface-raised);border-top:1rpx solid rgba(0,0,0,.06);padding:20rpx 32rpx;box-sizing:border-box;box-shadow:var(--shadow-md);z-index:50;padding-bottom:calc(20rpx + env(safe-area-inset-bottom))}
.bottom-bar-row{display:flex;align-items:center;justify-content:space-between;gap:28rpx}
.bottom-total{flex-shrink:0}
.total-label{font-size:22rpx;color:var(--text-secondary);display:block}
.total-price{font-size:38rpx;font-weight:700;color:var(--primary)}
.submit-btn{flex:1;height:92rpx;background:var(--primary);border-radius:48rpx;display:flex;align-items:center;justify-content:center;box-shadow:var(--shadow-sm)}
.submit-btn:active{transform:scale(.95)}
.submit-btn text{font-size:28rpx;font-weight:600;color:#fff}
.bottom-placeholder{height:180rpx}
</style>
