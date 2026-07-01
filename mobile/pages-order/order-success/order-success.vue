<template>
  <view class="page">
    <uni-nav-bar title="下单成功" backgroundColor="#FAFAF8" :border="false" statusBar fixed leftIcon="close" @clickLeft="onBack" color="#1C1B1A" />

    <view class="main-content">
      <view class="success-icon-wrap">
        <view class="success-icon-ring">
          <view class="success-icon-border">
            <iconpark-icon name="checkmarkempty" size="44" color="#34d399" />
          </view>
        </view>
      </view>
      <text class="success-title">下单成功</text>
      <text class="success-desc">任务已发布，正为您匹配跑腿同学，请耐心等待</text>

      <uni-card :isShadow="false" :margin="'48rpx 32rpx 0'" :spacing="'32rpx'" :border="false">
        <text class="summary-header">任务详情</text>
        <view class="summary-item">
          <text class="summary-label">支付金额</text>
          <text class="summary-value summary-value--price">¥{{ reward }}</text>
        </view>
        <view class="summary-item">
          <text class="summary-label">任务类型</text>
          <uni-tag :text="typeLabel" type="primary" size="small" :inverted="true" customStyle="border-color:#FFD1C7;color:#FF6B4A;background:#FFF0ED;" />
        </view>
      </uni-card>

      <view class="action-btns" style="padding:0 32rpx">
        <view class="btn btn--primary" @click="onViewOrder"><text>查看任务</text></view>
        <view class="btn btn--secondary" @click="onGoHome"><text>返回首页</text></view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { TASK_TYPES } from '@/utils/constants.js'

const reward = ref('0.00')
const typeValue = ref(1)
const typeLabel = ref('')

onLoad((options) => {
  reward.value = Number(options?.reward || 0).toFixed(2)
  typeValue.value = Number(options?.type || 1)
  typeLabel.value = TASK_TYPES[typeValue.value] || '任务'
})

function onBack() { uni.navigateBack() }
function onViewOrder() {
  uni.switchTab({ url: '/pages/orders/orders' })
}
function onGoHome() { uni.switchTab({ url: '/pages/index/index' }) }
</script>

<style scoped>
.page{width:100%;height:100%;display:flex;flex-direction:column;background:var(--background);overflow:hidden}
.main-content{flex:1}

.success-icon-wrap{display:flex;align-items:center;justify-content:center;padding:48rpx 0 16rpx}
.success-icon-ring{width:160rpx;height:160rpx;border-radius:50%;background:rgba(52,211,153,.06);display:flex;align-items:center;justify-content:center}
.success-icon-border{width:104rpx;height:104rpx;border-radius:50%;border:4rpx solid #34d399;display:flex;align-items:center;justify-content:center}
.success-title{font-size:44rpx;font-weight:700;color:var(--text-primary);text-align:center;display:block;margin-bottom:10rpx}
.success-desc{font-size:28rpx;color:var(--text-tertiary);text-align:center;display:block;padding:0 64rpx}

.summary-header{font-size:32rpx;font-weight:600;color:var(--text-primary);display:block;margin-bottom:24rpx;padding-bottom:20rpx;border-bottom:1rpx solid var(--surface)}
.summary-item{display:flex;justify-content:space-between;align-items:center;margin-bottom:18rpx}
.summary-item:last-child{margin-bottom:0}
.summary-label{font-size:28rpx;color:var(--text-secondary)}
.summary-value{font-size:28rpx;font-weight:500;color:var(--text-primary);max-width:320rpx;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
.summary-value--price{font-weight:700;color:var(--primary);font-size:32rpx}
.summary-value--expire{font-size:22rpx;color:var(--warning)}

.action-btns{margin-top:56rpx;display:flex;flex-direction:column;gap:24rpx;padding:0 32rpx}
.btn{padding:28rpx;border-radius:48rpx;display:flex;align-items:center;justify-content:center}
.btn:active{transform:scale(.95)}
.btn--primary{background:var(--primary);box-shadow:var(--shadow-sm)}
.btn--primary text{font-size:30rpx;font-weight:600;color:var(--surface-raised)}
.btn--secondary{background:var(--surface-raised);border:1rpx solid var(--outline-light)}
.btn--secondary text{font-size:30rpx;font-weight:500;color:var(--text-secondary)}
</style>
