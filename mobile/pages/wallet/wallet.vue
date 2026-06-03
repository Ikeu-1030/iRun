<template>
  <view class="page">
    <uni-nav-bar title="钱包" backgroundColor="#FAFAF8" :border="false" statusBar fixed leftIcon="left" @clickLeft="onBack" color="#1C1B1A" />

    <scroll-view class="main-scroll" :style="{ height: scrollHeight + 'px' }" scroll-y enhanced :show-scrollbar="false">
      <!-- 余额卡片 -->
      <view class="balance-card animate-scale-pop">
        <view class="balance-glow balance-glow--1"></view>
        <view class="balance-glow balance-glow--2"></view>
        <view class="balance-body">
          <text class="balance-label">当前余额 (元)</text>
          <text class="balance-value">{{ store.balanceText }}</text>
        </view>
      </view>

      <!-- 充值 -->
      <view class="section-card animate-fade-up delay-3">
        <text class="section-title">充值金额</text>
        <view class="amount-grid">
          <view v-for="amount in presetAmounts" :key="amount" class="amount-chip" :class="{ 'amount-chip--active': rechargeSelected === amount && !rechargeCustom }" @click="onSelectRecharge(amount)">
            <text class="amount-chip-value">¥ {{ amount }}</text>
          </view>
        </view>
        <view class="custom-row">
          <text class="custom-label">自定义金额</text>
          <view class="custom-input-wrap">
            <text class="custom-unit">¥</text>
            <input class="custom-input" name="digit" v-model="rechargeCustom" placeholder="输入其他金额" @input="onCustomRechargeInput" />
          </view>
        </view>
        <view class="action-btn action-btn--primary" :class="{ 'action-btn--disabled': submitting }" @click="onRecharge">
          <text>{{ submitting ? '处理中…' : '立即充值' }}</text>
        </view>
      </view>

      <!-- 提现 -->
      <view class="section-card animate-fade-up delay-4">
        <view class="section-header-row">
          <text class="section-title">提现</text>
          <text class="link-text" @click="onAllWithdraw">全部提现</text>
        </view>
        <view class="bank-card">
          <view class="bank-icon"><iconpark-icon name="home" size="24" color="#FF6B4A" /></view>
          <view class="bank-info">
            <text class="bank-name">提现到银行卡</text>
            <text class="bank-number">尾号 8888</text>
          </view>
          <iconpark-icon name="right" size="16" color="#8F8D88" />
        </view>
        <view class="custom-row">
          <text class="custom-label">提现金额</text>
          <view class="custom-input-wrap">
            <text class="custom-unit">¥</text>
            <input class="custom-input" name="digit" v-model="withdrawAmount" placeholder="输入提现金额" />
          </view>
        </view>
        <view class="action-btn action-btn--secondary" :class="{ 'action-btn--disabled': submitting }" @click="onWithdraw">
          <text>{{ submitting ? '处理中…' : '申请提现' }}</text>
        </view>
      </view>

      <view class="bottom-placeholder"></view>
    </scroll-view>

    <PayPasswordDialog />
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useStore } from '@/store/index.js'
import { transactionApi } from '@/api'
import { promptPayPassword } from '@/utils/pay-password.js'
import { guideToSetPayPassword } from '@/utils/error'
import { useSubmitLock } from '@/utils/submit-guard'
import PayPasswordDialog from '@/components/pay-password-dialog/pay-password-dialog.vue'

const store = useStore()
const sysInfo = uni.getSystemInfoSync()
const scrollHeight = sysInfo.windowHeight - sysInfo.statusBarHeight - 44

const presetAmounts = ['10', '50', '100', '200']
const rechargeSelected = ref('50')
const rechargeCustom = ref('')
const withdrawAmount = ref('')
const { lock, unlock, locked: submitting } = useSubmitLock()

const effectiveRecharge = computed(() => {
  if (rechargeCustom.value) return parseFloat(rechargeCustom.value) || 0
  return parseFloat(rechargeSelected.value) || 0
})

function onBack() { uni.navigateBack() }

function onSelectRecharge(amount) {
  rechargeSelected.value = amount
  rechargeCustom.value = ''
}

function onCustomRechargeInput() {
  rechargeSelected.value = ''
}

async function onRecharge() {
  const amount = effectiveRecharge.value
  if (!amount || amount <= 0) {
    uni.showToast({ title: '请输入有效金额', icon: 'none' })
    return
  }
  const pw = await promptPayPassword('充值')
  if (!pw) return
  if (!lock()) return
  try {
    await transactionApi.recharge(amount, pw)
    store.updateBalance(store.userInfo.balance + amount)
    uni.showToast({ title: '充值成功', icon: 'success' })
    rechargeCustom.value = ''
    rechargeSelected.value = '50'
  } catch (e) {
    if (e.message?.includes('请先设置支付密码')) guideToSetPayPassword()
  } finally {
    unlock()
  }
}

async function onWithdraw() {
  const amount = parseFloat(withdrawAmount.value) || 0
  if (!amount || amount <= 0) {
    uni.showToast({ title: '请输入有效金额', icon: 'none' })
    return
  }
  if (amount > store.userInfo.balance) {
    uni.showToast({ title: '余额不足', icon: 'none' })
    return
  }
  const pw = await promptPayPassword('提现')
  if (!pw) return
  if (!lock()) return
  try {
    await transactionApi.withdraw(amount, pw)
    store.updateBalance(store.userInfo.balance - amount)
    uni.showToast({ title: '提现成功', icon: 'success' })
    withdrawAmount.value = ''
  } catch (e) {
    if (e.message?.includes('请先设置支付密码')) guideToSetPayPassword()
  } finally {
    unlock()
  }
}

async function onAllWithdraw() {
  if (store.userInfo.balance <= 0) {
    uni.showToast({ title: '无可提现余额', icon: 'none' })
    return
  }
  withdrawAmount.value = store.userInfo.balance.toFixed(2)
  onWithdraw()
}

</script>

<style scoped>
.page{width:100%;height:100vh;display:flex;flex-direction:column;background:var(--background);overflow:hidden}
.main-scroll{box-sizing:border-box;width:100%;padding:0 32rpx;padding-bottom:60rpx}

.balance-card{background:var(--primary);border-radius:var(--radius-card);padding:40rpx;position:relative;overflow:hidden;margin-top:16rpx;box-shadow:var(--shadow-md)}
.balance-glow{position:absolute;border-radius:50%;background:rgba(255,255,255,.1)}
.balance-glow--1{width:300rpx;height:300rpx;top:-150rpx;right:-80rpx}
.balance-glow--2{width:220rpx;height:220rpx;bottom:-80rpx;left:-40rpx}
.balance-body{position:relative;z-index:1}
.balance-label{font-size:24rpx;font-weight:500;color:rgba(255,255,255,.8);display:block}
.balance-value{font-size:60rpx;font-weight:700;color:#fff;letter-spacing:-2rpx;display:block;margin-top:14rpx}

.section-card{background:var(--surface-raised);border-radius:var(--radius-card);padding:32rpx;margin-top:32rpx;box-shadow:var(--shadow-sm)}
.section-title{font-size:32rpx;font-weight:600;color:var(--text-primary);display:block;margin-bottom:20rpx}
.section-header-row{display:flex;justify-content:space-between;align-items:center;margin-bottom:20rpx}
.section-header-row .section-title{margin-bottom:0}
.link-text{font-size:26rpx;color:var(--primary);font-weight:500}

.amount-grid{display:flex;flex-wrap:wrap;gap:16rpx}
.amount-chip{width:calc((100% - 16rpx) / 2);padding:28rpx 0;background:var(--surface);border-radius:20rpx;display:flex;align-items:center;justify-content:center;border:2rpx solid transparent;box-sizing:border-box}
.amount-chip--active{border-color:var(--primary);background:var(--primary-container)}
.amount-chip-value{font-size:30rpx;font-weight:600;color:var(--text-primary)}
.amount-chip--active .amount-chip-value{color:var(--primary)}

.custom-row{margin-top:24rpx}
.custom-label{font-size:26rpx;color:var(--text-secondary);display:block;margin-bottom:14rpx}
.custom-input-wrap{display:flex;align-items:center;background:var(--surface);border-radius:20rpx;padding:0 24rpx}
.custom-unit{font-size:36rpx;font-weight:700;color:var(--primary);margin-right:12rpx}
.custom-input{flex:1;height:88rpx;font-size:30rpx;color:var(--text-primary)}

.action-btn{height:90rpx;border-radius:48rpx;display:flex;align-items:center;justify-content:center;margin-top:28rpx}
.action-btn:active{transform:scale(.95)}
.action-btn--primary{background:var(--primary);box-shadow:var(--shadow-sm)}
.action-btn--primary text{color:#fff;font-size:28rpx;font-weight:600}
.action-btn--secondary{background:var(--surface);border:1rpx solid var(--outline-light)}
.action-btn--secondary text{color:var(--text-secondary);font-size:28rpx;font-weight:600}
.action-btn--disabled{pointer-events:none;opacity:.6}

.bank-card{display:flex;align-items:center;background:var(--surface);border-radius:20rpx;padding:24rpx;gap:18rpx;margin-bottom:8rpx}
.bank-icon{width:68rpx;height:68rpx;border-radius:50%;background:var(--surface-raised);display:flex;align-items:center;justify-content:center}
.bank-info{flex:1}
.bank-name{font-size:28rpx;color:var(--text-primary);display:block}
.bank-number{font-size:22rpx;color:var(--text-secondary);margin-top:4rpx;display:block}

.bottom-placeholder{height:60rpx}
</style>
