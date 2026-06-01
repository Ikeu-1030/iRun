<template>
  <view class="page">
    <uni-nav-bar title="纸品速达" backgroundColor="#FAFAF8" :border="false" statusBar fixed leftIcon="left" @clickLeft="onBack" color="#1C1B1A" />

    <scroll-view class="main-scroll" :style="{ height: scrollHeight + 'px' }" scroll-y enhanced :show-scrollbar="false">
      <view class="page-header">
        <text class="page-title">纸品速达</text>
        <text class="page-subtitle">代购纸品，快速送达</text>
      </view>

      <view class="form-card">
        <view class="card-title">物品信息 <text class="required">*</text></view>
        <view class="form-label">物品名称</view>
        <input class="form-input" placeholder="例如：洁柔纸面巾" v-model="description" />
        <view class="form-label">纸品规格</view>
        <view class="chip-row">
          <view v-for="s in specs" :key="s" class="chip" :class="{ 'chip--active': itemSpec === s }" @click="itemSpec = s">{{ s }}</view>
        </view>
        <view class="qty-row">
          <text class="qty-label">数量</text>
          <view class="qty-stepper">
            <view class="qty-btn" :class="{ 'qty-btn--disabled': itemQty <= 1 }" @click="decreaseQty">−</view>
            <text class="qty-value">{{ itemQty }}</text>
            <view class="qty-btn" :class="{ 'qty-btn--disabled': itemQty >= 10 }" @click="increaseQty">+</view>
          </view>
        </view>
      </view>

      <view class="form-card">
        <view class="card-title">购买信息 <text class="required">*</text></view>
        <view class="form-label">购买地点</view>
        <view class="addr-row">
          <view class="addr-badge addr-badge--pickup">取</view>
          <picker mode="selector" :range="pickupLocations" @change="onPickupLoc" style="flex:1">
            <view class="form-select"><text :class="{ 'form-select-placeholder': !pickupAddress }">{{ pickupAddress || '请选择购买地点' }}</text><text class="select-arrow">▼</text></view>
          </picker>
        </view>
        <view v-if="pickupAddress === '自定义地点'" class="form-label">自定义购买地点</view>
        <input v-if="pickupAddress === '自定义地点'" class="form-input" placeholder="请输入具体购买地点" v-model="customPickupAddress" />
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
        <view class="card-title">备注说明</view>
        <textarea class="form-textarea" placeholder="补充说明" v-model="remark" />
      </view>

      <view class="form-card">
        <view class="card-title">上传信息</view>
        <UploadGrid v-model="uploadedUrls" :maxCount="3" />
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
        <view class="fee-row"><text class="fee-label">基础配送费</text><text class="fee-value">¥ 5.00</text></view>
        <view class="fee-divider"></view>
        <view class="form-label">预估商品费</view>
        <view class="custom-bounty-row">
          <text class="custom-bounty-unit">¥</text>
          <input class="custom-bounty-input" name="digit" v-model.number="estimatedProductFee" placeholder="输入预估商品费用" />
        </view>
        <view class="info-hint" style="margin-top:12rpx">
          <iconpark-icon name="info" size="18" color="#FF6B4A" />
          <text>配送员垫付商品费，送达后当面结算</text>
        </view>
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
import UploadGrid from '@/components/upload-grid/upload-grid.vue'
import { TYPE_TO_API, SUBTYPE_TO_VALUE } from '@/utils/constants.js'
import { promptPayPassword } from '@/utils/pay-password.js'
import PayPasswordDialog from '@/components/pay-password-dialog/pay-password-dialog.vue'

const sysInfo = uni.getSystemInfoSync()
const scrollHeight = sysInfo.windowHeight - sysInfo.statusBarHeight - 44

const description = ref('')
const remark = ref('')
const itemSpec = ref('一般')
const itemQty = ref(1)
const pickupAddress = ref('')
const customPickupAddress = ref('')
const uploadedUrls = ref([])
const reward = ref(0)
const customBounty = ref(0)
const showCustomBounty = ref(false)
const deliveryAddressId = ref(null)
const deliveryLabel = ref('')
const deliveryContactName = ref('')
const deliveryContactPhone = ref('')
const requireSex = ref(undefined)
const estimatedProductFee = ref(0)
const submitting = ref(false)

const baseFee = 5
const totalPrice = computed(() => {
  const bounty = showCustomBounty.value ? customBounty.value || 0 : reward.value
  const productFee = estimatedProductFee.value || 0
  return baseFee + productFee + bounty
})

function setReward(val) {
  showCustomBounty.value = false
  reward.value = val
}

function decreaseQty() { if (itemQty.value > 1) itemQty.value-- }
function increaseQty() { if (itemQty.value < 10) itemQty.value++ }

function toggleCustomReward() {
  showCustomBounty.value = !showCustomBounty.value
  if (showCustomBounty.value && customBounty.value === 0) {
    customBounty.value = reward.value || 5
  }
}

const specs = ['小包', '一般', '小包整提']
const pickupLocations = ['蓝区体育场校园超市', '霞光超市', '霞光餐厅一楼便利店', '晨曦精选便利店', '自定义地点']

function onPickupLoc(e) {
  pickupAddress.value = pickupLocations[e.detail.value]
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
  if (!description.value) {
    uni.showToast({ title: '请填写物品名称', icon: 'none' })
    return
  }
  if (!pickupAddress.value) {
    uni.showToast({ title: '请选择购买地点', icon: 'none' })
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
    const itemName = description.value || '纸品速达'
    const taskSpecsStr = JSON.stringify({
      商品列表: [{ 名称: itemName, 数量: itemQty.value, 规格: itemSpec.value }],
      预估商品费: estimatedProductFee.value > 0 ? Number(estimatedProductFee.value) : 0
    })

    await taskApi.publishTask({
      type: TYPE_TO_API[4], subType: SUBTYPE_TO_VALUE[41],
      publicDesc: remark.value || undefined,
      taskSpecs: taskSpecsStr,
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
.card-title{font-size:30rpx;font-weight:600;color:var(--text-primary);margin-bottom:20rpx;padding-bottom:16rpx;border-bottom:1rpx solid var(--outline-light);display:flex;align-items:center;gap:10rpx}
.required{color:#ef4444;font-size:22rpx;vertical-align:super;line-height:1}
.addr-row{display:flex;align-items:center;gap:14rpx}
.addr-badge{width:40rpx;height:40rpx;border-radius:10rpx;display:flex;align-items:center;justify-content:center;font-size:22rpx;font-weight:700;color:#fff;flex-shrink:0}
.addr-badge--pickup{background:var(--text-primary)}
.addr-badge--deliver{background:var(--primary)}
.form-label{font-size:24rpx;font-weight:500;color:var(--text-secondary);margin-bottom:10rpx;margin-top:18rpx}
.form-label:first-child{margin-top:0}
.form-input{width:100%;height:84rpx;background:var(--surface);border-radius:20rpx;padding:0 28rpx;font-size:28rpx;color:var(--text-primary);box-sizing:border-box}
.form-select{width:100%;height:84rpx;background:var(--surface);border-radius:20rpx;padding:0 28rpx;font-size:28rpx;color:var(--text-secondary);display:flex;align-items:center;justify-content:space-between;box-sizing:border-box}
.form-select-placeholder{color:var(--text-tertiary)}
.select-arrow{font-size:32rpx;color:var(--text-secondary)}
.form-addr-card{width:100%;background:var(--surface);border-radius:20rpx;padding:22rpx 28rpx;display:flex;align-items:center;justify-content:space-between;box-sizing:border-box}
.addr-main{font-size:28rpx;color:var(--text-primary);display:block}
.info-hint{display:flex;align-items:center;gap:10rpx;padding:16rpx;background:var(--primary-container);border-radius:12rpx}
.info-hint text{font-size:24rpx;color:var(--primary)}
.chip-row{display:flex;flex-wrap:wrap;gap:14rpx}
.chip{padding:14rpx 28rpx;border-radius:48rpx;font-size:26rpx;font-weight:500;color:var(--text-secondary);background:var(--surface);text-align:center}
.chip--active{background:var(--primary);color:#fff;font-weight:600}
.qty-row{display:flex;align-items:center;justify-content:space-between}
.qty-label{font-size:26rpx;font-weight:500;color:var(--text-secondary)}
.qty-stepper{display:flex;align-items:center;gap:0}
.qty-btn{width:60rpx;height:56rpx;display:flex;align-items:center;justify-content:center;background:var(--surface);font-size:32rpx;font-weight:600;color:var(--primary);user-select:none}
.qty-btn:first-child{border-radius:16rpx 0 0 16rpx}
.qty-btn:last-child{border-radius:0 16rpx 16rpx 0}
.qty-btn--disabled{color:var(--text-tertiary);pointer-events:none}
.qty-btn:active:not(.qty-btn--disabled){background:var(--outline-light)}
.qty-value{width:72rpx;text-align:center;font-size:30rpx;font-weight:600;color:var(--text-primary)}
.form-textarea{width:100%;background:var(--surface);border-radius:20rpx;padding:20rpx 28rpx;font-size:28rpx;color:var(--text-primary);box-sizing:border-box;min-height:120rpx}
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
