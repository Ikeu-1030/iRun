<template>
  <view class="page">
    <uni-nav-bar :title="navTitle" backgroundColor="#FAFAF8" :border="false" statusBar fixed leftIcon="left" @clickLeft="onBack" color="#1C1B1A" />

    <scroll-view class="main-scroll" :style="{ height: scrollHeight + 'px' }" scroll-y enhanced :show-scrollbar="false">
      <view class="page-header">
        <text class="page-title">取消原因</text>
        <text class="page-subtitle">{{ subtitle }}</text>
      </view>

      <view class="form-card">
        <view class="chip-row">
          <view
            v-for="item in reasons"
            :key="item.value"
            class="chip"
            :class="{ 'chip--active': selectedReason === item.value }"
            @click="selectReason(item.value)"
          >{{ item.label }}</view>
        </view>

        <view class="custom-reason-area" v-if="selectedReason === 'custom'">
          <textarea
            class="form-textarea"
            placeholder="请描述取消原因"
            v-model="customReason"
            :maxlength="200"
          />
          <text class="char-count">{{ customReason.length }}/200</text>
        </view>
      </view>

      <view class="bottom-placeholder"></view>
    </scroll-view>

    <view class="bottom-bar safe-area-bottom">
      <view class="bottom-bar-row">
        <view class="cancel-btn" @click="onBack"><text>返回</text></view>
        <view class="submit-btn" :class="{ 'submit-btn--disabled': !canSubmit }" @click="onConfirm">
          <text>确认取消</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'

const sysInfo = uni.getSystemInfoSync()
const scrollHeight = sysInfo.windowHeight - sysInfo.statusBarHeight - 44

const cancelType = ref('task') // 'task' | 'order'

const taskReasons = [
  { value: 'info_error', label: '信息填写有误' },
  { value: 'no_need', label: '不需要此服务了' },
  { value: 'duplicate', label: '重复发布了' },
  { value: 'time_error', label: '时间设置有误' },
  { value: 'address_error', label: '配送地址选错了' },
  { value: 'custom', label: '其他原因' }
]

const taskReasonLabels = {
  info_error: '信息填写有误',
  no_need: '不需要此服务了',
  duplicate: '重复发布了',
  time_error: '时间设置有误',
  address_error: '配送地址选错了'
}

const orderReasons = [
  { value: 'time_conflict', label: '时间冲突无法配送' },
  { value: 'distance_far', label: '配送距离太远' },
  { value: 'contact_fail', label: '联系不上发布者' },
  { value: 'item_issue', label: '物品无法配送' },
  { value: 'personal_reason', label: '个人原因' },
  { value: 'custom', label: '其他原因' }
]

const orderReasonLabels = {
  time_conflict: '时间冲突无法配送',
  distance_far: '配送距离太远',
  contact_fail: '联系不上发布者',
  item_issue: '物品无法配送',
  personal_reason: '个人原因'
}

const reasons = computed(() => cancelType.value === 'order' ? orderReasons : taskReasons)
const reasonLabels = computed(() => cancelType.value === 'order' ? orderReasonLabels : taskReasonLabels)

const navTitle = computed(() => cancelType.value === 'order' ? '取消订单' : '取消任务')
const subtitle = computed(() => cancelType.value === 'order'
  ? '请选择取消订单的原因，接单后5分钟内可取消'
  : '请选择取消任务的原因，帮助我们改进服务')

const selectedReason = ref('')
const customReason = ref('')

const canSubmit = computed(() => {
  if (!selectedReason.value) return false
  if (selectedReason.value === 'custom' && !customReason.value.trim()) return false
  return true
})

onLoad((options) => {
  if (options?.type === 'order') cancelType.value = 'order'
})

function selectReason(val) {
  selectedReason.value = val
}

function onConfirm() {
  if (!canSubmit.value) return
  const reason = selectedReason.value === 'custom'
    ? customReason.value.trim()
    : reasonLabels.value[selectedReason.value]
  uni.$emit('cancelReasonSelected', reason)
  uni.navigateBack()
}

function onBack() {
  uni.navigateBack()
}
</script>

<style scoped>
.page{width:100%;height:100vh;display:flex;flex-direction:column;background:var(--background);overflow:hidden}
.main-scroll{box-sizing:border-box;width:100%;padding:0 32rpx;padding-bottom:180rpx}
.page-header{margin-top:16rpx;margin-bottom:20rpx}
.page-title{font-size:44rpx;font-weight:700;color:var(--text-primary);display:block}
.page-subtitle{font-size:26rpx;color:var(--text-secondary);margin-top:6rpx;display:block}
.form-card{background:var(--surface-raised);border-radius:var(--radius-lg);padding:28rpx;margin-bottom:20rpx;box-shadow:var(--shadow-sm);border:1rpx solid var(--outline-light)}
.chip-row{display:flex;flex-wrap:wrap;gap:14rpx}
.chip{padding:18rpx 28rpx;border-radius:48rpx;font-size:28rpx;font-weight:500;color:var(--text-secondary);background:var(--surface);text-align:center}
.chip--active{background:var(--primary);color:#fff;font-weight:600}
.custom-reason-area{margin-top:24rpx}
.form-textarea{width:100%;background:var(--surface);border-radius:20rpx;padding:20rpx 28rpx;font-size:28rpx;color:var(--text-primary);box-sizing:border-box;min-height:160rpx}
.char-count{font-size:22rpx;color:var(--text-secondary);text-align:right;display:block;margin-top:8rpx}
.bottom-bar{position:fixed;bottom:0;left:0;width:100%;background:var(--surface-raised);border-top:1rpx solid var(--outline-light);padding:20rpx 32rpx;box-sizing:border-box;box-shadow:0 -8rpx 30rpx rgba(0,0,0,.04);z-index:50;padding-bottom:calc(20rpx + env(safe-area-inset-bottom))}
.bottom-bar-row{display:flex;align-items:center;gap:28rpx}
.cancel-btn{flex-shrink:0;height:92rpx;border-radius:48rpx;display:flex;align-items:center;justify-content:center;padding:0 36rpx;background:var(--surface)}
.cancel-btn:active{transform:scale(.95)}
.cancel-btn text{font-size:28rpx;font-weight:500;color:var(--text-secondary)}
.submit-btn{flex:1;height:92rpx;background:var(--primary);border-radius:48rpx;display:flex;align-items:center;justify-content:center;box-shadow:var(--shadow-sm)}
.submit-btn:active{transform:scale(.95)}
.submit-btn text{font-size:28rpx;font-weight:600;color:#fff}
.submit-btn--disabled{background:var(--text-tertiary);box-shadow:none}
.submit-btn--disabled:active{transform:none}
.submit-btn--disabled text{color:rgba(255,255,255,.7)}
.bottom-placeholder{height:180rpx}
</style>
