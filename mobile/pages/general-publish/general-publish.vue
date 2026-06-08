<template>
  <view class="page">
    <uni-nav-bar :title="pageTitle" backgroundColor="#FAFAF8" :border="false" statusBar fixed leftIcon="left" @clickLeft="onBack" color="#1C1B1A" />

    <scroll-view class="main-scroll" :style="{ height: scrollHeight + 'px' }" scroll-y enhanced :show-scrollbar="false">
      <view class="page-header">
        <text class="page-title">{{ pageTitle }}</text>
        <text class="page-subtitle">快速发布各类代办需求，帮取马即刻为您服务</text>
      </view>

      <!-- 任务描述（公开） -->
      <view class="form-card">
        <view class="card-title">任务描述（公开）</view>
        <textarea class="form-textarea" placeholder="所有用户可见的任务描述，例如：帮忙取快递到宿舍、帮我去图书馆还书…" v-model="description" />
      </view>

      <!-- 备注（仅接单员可见） -->
      <view class="form-card">
        <view class="card-title">备注（仅接单员可见）</view>
        <textarea class="form-textarea" placeholder="仅对成功接单方可见" v-model="privateDescription" />
      </view>

      <!-- 取件信息 -->
      <view class="form-card">
        <view class="card-title">取件信息 <text class="required">*</text></view>
        <view class="addr-row">
          <view class="addr-badge addr-badge--pickup">取</view>
          <input class="form-input" placeholder="从哪里取件？如：东区宿舍3号楼 501" v-model="pickupAddress" style="flex:1" />
        </view>
        <view class="toggle-row">
          <text class="form-label" style="margin-top:0;flex:1">取件码</text>
          <switch :checked="requirePickupCode" @change="requirePickupCode = !requirePickupCode" color="var(--primary)" style="transform:scale(0.8)" />
        </view>
        <input v-if="requirePickupCode" class="form-input form-input--large" placeholder="输入取件码，如：1-2-3456" v-model="pickupCode" style="margin-top:12rpx" />
      </view>

      <!-- 配送信息 -->
      <view class="form-card">
        <view class="card-title">配送信息 <text class="required">*</text></view>
        <view class="addr-row">
          <view class="addr-badge addr-badge--deliver">收</view>
          <view class="form-addr-card" @click="onSelectAddress">
            <view class="addr-info">
              <text class="addr-main">{{ deliveryLabel || '选择配送终点（请从地址簿选择）' }}</text>
            </view>
            <text class="select-arrow">›</text>
          </view>
        </view>
      </view>

      <!-- 上传信息 -->
      <view class="form-card">
        <view class="card-title">上传信息</view>
        <UploadGrid v-model="uploadedUrls" :maxCount="3" />
      </view>

      <!-- 接单限制 -->
      <view class="form-card">
        <view class="card-title">接单限制</view>
        <view class="chip-row">
          <view class="chip" :class="{ 'chip--active': requireSex === undefined }" @click="requireSex = undefined">不限</view>
          <view class="chip" :class="{ 'chip--active': requireSex === '男' }" @click="requireSex = '男'">仅男生</view>
          <view class="chip" :class="{ 'chip--active': requireSex === '女' }" @click="requireSex = '女'">仅女生</view>
        </view>
      </view>

      <!-- 费用明细 -->
      <view class="form-card form-card--pay">
        <view class="card-title">费用明细</view>
        <view class="fee-row">
          <text class="fee-label">基础配送费</text>
          <text class="fee-value">¥ 5.00</text>
        </view>
        <view class="fee-divider"></view>
        <view class="form-label">额外费用（选填，如商品费、材料费）</view>
        <view class="custom-bounty-row">
          <text class="custom-bounty-unit">¥</text>
          <input class="custom-bounty-input" name="digit" v-model.number="extraFee" placeholder="输入额外费用" />
        </view>
        <view class="info-hint" style="margin-top:12rpx">
          <iconpark-icon name="info" size="18" color="#FF6B4A" />
          <text>如有垫付费用请填写，配送员送达后当面结算</text>
        </view>
        <view class="fee-divider"></view>
        <view class="form-label">赏金（提高接单率）</view>
        <view class="chip-row bounty-chips">
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
        <view class="fee-row fee-row--total">
          <text class="fee-label">合计支付</text>
          <text class="fee-total">¥ {{ totalReward.toFixed(2) }}</text>
        </view>
      </view>

      <view class="bottom-placeholder"></view>
    </scroll-view>

    <!-- 底部发布栏 -->
    <view class="bottom-bar safe-area-bottom">
      <view class="bottom-bar-row">
        <view class="bottom-total">
          <text class="total-label">合计支付</text>
          <text class="total-price">¥ {{ totalReward.toFixed(2) }}</text>
        </view>
        <view class="submit-btn" :class="{ 'submit-btn--disabled': submitting }" @click="onSubmit">
          <text>{{ submitting ? '发布中…' : '发布需求' }}</text>
        </view>
      </view>
    </view>

    <PayPasswordDialog />
  </view>
</template>

<script setup>
import { ref, computed, onUnmounted } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { taskApi } from '@/api'
import UploadGrid from '@/components/upload-grid/upload-grid.vue'
import PayPasswordDialog from '@/components/pay-password-dialog/pay-password-dialog.vue'
// encodeDescription removed — backend reads publicDesc + privateNote directly
import { promptPayPassword } from '@/utils/pay-password.js'
import { guideToSetPayPassword } from '@/utils/error'
import { useSubmitLock } from '@/utils/submit-guard'

const sysInfo = uni.getSystemInfoSync()
const scrollHeight = sysInfo.windowHeight - sysInfo.statusBarHeight - 44

const taskTypeString = ref('通用代办')
const description = ref('')
const privateDescription = ref('')
const pickupAddress = ref('')
const requirePickupCode = ref(false)
const pickupCode = ref('')
const deliveryAddressId = ref(null)
const deliveryLabel = ref('')
const deliveryContactName = ref('')
const deliveryContactPhone = ref('')
const requireSex = ref(undefined)
const reward = ref(0)
const customBounty = ref(0)
const showCustomBounty = ref(false)
const extraFee = ref(0)
const uploadedUrls = ref([])
const { lock, unlock, locked: submitting } = useSubmitLock()

const pageTitle = computed(() => taskTypeString.value || '发布需求')

const totalReward = computed(() => {
  const bounty = showCustomBounty.value ? (customBounty.value || 0) : reward.value
  const extra = extraFee.value || 0
  return 5 + extra + bounty
})

onLoad((options) => {
  if (options?.type) taskTypeString.value = options.type
})

onUnmounted(() => {
  uni.$off('addressSelected')
})

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

function onBack() { uni.navigateBack() }

async function onSubmit() {
  if (!description.value && !privateDescription.value) {
    uni.showToast({ title: '请填写描述或备注', icon: 'none' })
    return
  }
  if (!pickupAddress.value) {
    uni.showToast({ title: '请填写取件地址', icon: 'none' })
    return
  }
  if (!deliveryAddressId.value) {
    uni.showToast({ title: '请选择配送终点', icon: 'none' })
    return
  }

  const pw = await promptPayPassword('支付赏金')
  if (!pw) return

  if (!lock()) return
  try {
    const publicDesc = description.value || ''
    const privateNote = privateDescription.value || undefined

    let taskSpecsStr
    if (extraFee.value > 0) {
      taskSpecsStr = JSON.stringify({ 额外费用: Number(extraFee.value) })
    }

    const bounty = showCustomBounty.value ? (customBounty.value || 0) : reward.value

    const payload = {
      type: taskTypeString.value,
      subType: undefined,
      publicDesc: publicDesc || undefined,
      privateNote: privateNote,
      taskSpecs: taskSpecsStr || undefined,
      reward: parseFloat(Number(bounty).toFixed(2)),
      deliveryFee: 5,
      productCost: extraFee.value > 0 ? Number(extraFee.value) : 0,
      payPassword: pw,
      pickupCode: requirePickupCode.value && pickupCode.value ? pickupCode.value : undefined,
      pickupAddress: pickupAddress.value || undefined,
      deliveryAddressId: deliveryAddressId.value,
      contactName: deliveryContactName.value || undefined,
      contactPhone: deliveryContactPhone.value || undefined,
      requireSex: requireSex.value,
      imageUrls: uploadedUrls.value.length > 0 ? [...uploadedUrls.value] : undefined
    }

    await taskApi.publishTask(payload)
    uni.showToast({ title: '发布成功', icon: 'success' })
    setTimeout(() => {
      uni.redirectTo({ url: `/pages/order-success/order-success?reward=${totalReward.value}&type=通用代办` })
    }, 800)
  } catch (e) {
    if (e.message?.includes('请先设置支付密码')) {
      guideToSetPayPassword()
    }
  } finally {
    unlock()
  }
}
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
.form-label{font-size:24rpx;font-weight:500;color:var(--text-secondary);margin-bottom:10rpx;margin-top:18rpx}
.form-label:first-child{margin-top:0}
.form-input{width:100%;height:84rpx;background:var(--surface);border-radius:20rpx;padding:0 28rpx;font-size:28rpx;color:var(--text-primary);box-sizing:border-box}
.form-input--large{height:100rpx;font-size:30rpx;font-weight:600;letter-spacing:4rpx}
.form-textarea{width:100%;background:var(--surface);border-radius:20rpx;padding:20rpx 28rpx;font-size:28rpx;color:var(--text-primary);box-sizing:border-box;min-height:150rpx}
.form-addr-card{width:100%;background:var(--surface);border-radius:20rpx;padding:22rpx 28rpx;display:flex;align-items:center;justify-content:space-between;box-sizing:border-box}
.addr-main{font-size:28rpx;color:var(--text-primary);display:block}
.select-arrow{font-size:32rpx;color:var(--text-secondary)}

.addr-row{display:flex;align-items:center;gap:14rpx}
.addr-badge{width:40rpx;height:40rpx;border-radius:10rpx;display:flex;align-items:center;justify-content:center;font-size:22rpx;font-weight:700;color:#fff;flex-shrink:0}
.addr-badge--pickup{background:var(--text-primary)}
.addr-badge--deliver{background:var(--primary)}

.toggle-row{display:flex;align-items:center;justify-content:space-between;margin-top:18rpx}

.chip-row{display:flex;flex-wrap:wrap;gap:14rpx}
.chip{padding:14rpx 28rpx;border-radius:48rpx;font-size:26rpx;font-weight:500;color:var(--text-secondary);background:var(--surface);text-align:center}
.chip--active{background:var(--primary);color:#fff;font-weight:600}

.info-hint{display:flex;align-items:center;gap:10rpx;padding:16rpx;background:var(--primary-container);border-radius:12rpx}
.info-hint text{font-size:24rpx;color:var(--primary)}

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
.submit-btn--disabled{pointer-events:none;opacity:.6}
.submit-btn text{font-size:28rpx;font-weight:600;color:#fff}

.bottom-placeholder{height:180rpx}
</style>
