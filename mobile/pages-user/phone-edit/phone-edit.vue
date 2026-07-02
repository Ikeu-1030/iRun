<template>
  <view class="page">
    <uni-nav-bar title="修改手机号" backgroundColor="#FAFAF8" :border="false" statusBar fixed leftIcon="left" @clickLeft="onBack" color="#1C1B1A" />

    <scroll-view class="main-scroll" :style="{ height: scrollHeight + 'px' }" scroll-y enhanced :show-scrollbar="false">
      <view class="section-card">
        <text class="section-title">当前手机号</text>
        <text class="current-phone">{{ store.userInfo.phone || '未绑定' }}</text>
      </view>

      <view class="section-card">
        <text class="section-title">新手机号</text>
        <view class="phone-row">
          <text class="phone-prefix">+86</text>
          <input class="phone-input" name="tel" v-model="newPhone" placeholder="请输入新手机号" maxlength="11" />
        </view>
        <view class="code-row">
          <input class="code-input" v-model="code" placeholder="输入验证码" maxlength="6" />
          <view class="send-btn" :class="{ 'send-btn--disabled': countdown > 0 }" @click="onSendCode">
            <text>{{ countdown > 0 ? `${countdown}s` : '获取验证码' }}</text>
          </view>
        </view>
      </view>

      <view class="save-btn" @click="onSave"><text>{{ saving ? '修改中…' : '确定修改' }}</text></view>
      <view class="bottom-placeholder"></view>
    </scroll-view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { useStore } from '@/store/index.js'
import { useSmsCooldown } from '@/utils/useSmsCooldown'
import { userApi } from '@/api'

const store = useStore()
const sysInfo = uni.getSystemInfoSync()
const scrollHeight = sysInfo.windowHeight - sysInfo.statusBarHeight - 44

const newPhone = ref('')
const code = ref('')
const { countdown, startCooldown } = useSmsCooldown()
const saving = ref(false)

async function onSendCode() {
  if (countdown.value > 0) return
  if (!/^1[3-9]\d{9}$/.test(newPhone.value)) {
    uni.showToast({ title: '请输入正确手机号', icon: 'none' })
    return
  }
  try {
    await userApi.sendCode(newPhone.value, 'change_phone')
    uni.showToast({ title: '验证码已发送', icon: 'success' })
    startCooldown()
  } catch (e) { /* handled */ }
}

async function onSave() {
  if (!newPhone.value || !code.value) {
    uni.showToast({ title: '请填写完整', icon: 'none' })
    return
  }
  saving.value = true
  try {
    await userApi.updatePhone(newPhone.value, code.value)
    store.userInfo.phone = newPhone.value
    uni.showToast({ title: '手机号已修改', icon: 'success' })
    setTimeout(() => uni.navigateBack(), 1000)
  } catch (e) { /* handled */ }
  saving.value = false
}

function onBack() { uni.navigateBack() }
</script>

<style scoped>
.page{width:100%;height:100vh;display:flex;flex-direction:column;background:var(--background);overflow:hidden}
.main-scroll{box-sizing:border-box;width:100%;padding:0 32rpx;padding-bottom:60rpx}
.section-card{background:var(--surface-raised);border-radius:var(--radius-lg);padding:32rpx;margin-top:24rpx;box-shadow:var(--shadow-sm)}
.section-title{font-size:28rpx;font-weight:600;color:var(--text-primary);display:block;margin-bottom:16rpx}
.current-phone{font-size:36rpx;font-weight:600;color:var(--text-secondary)}
.phone-row{display:flex;align-items:center;gap:16rpx;margin-bottom:20rpx}
.phone-prefix{font-size:28rpx;color:var(--text-primary);flex-shrink:0}
.phone-input{flex:1;height:84rpx;background:var(--surface);border-radius:16rpx;padding:0 22rpx;font-size:28rpx;color:var(--text-primary)}
.code-row{display:flex;align-items:center;gap:16rpx}
.code-input{flex:1;height:84rpx;background:var(--surface);border-radius:16rpx;padding:0 22rpx;font-size:28rpx;color:var(--text-primary)}
.send-btn{padding:18rpx 24rpx;background:var(--primary);border-radius:16rpx;flex-shrink:0}
.send-btn--disabled{background:var(--text-tertiary)}
.send-btn text{font-size:24rpx;color:#fff;font-weight:500}
.save-btn{height:96rpx;background:var(--primary);border-radius:48rpx;display:flex;align-items:center;justify-content:center;margin-top:48rpx;box-shadow:var(--shadow-sm)}
.save-btn:active{transform:scale(.95)}
.save-btn text{font-size:28rpx;font-weight:600;color:#fff}
.bottom-placeholder{height:60rpx}
</style>
