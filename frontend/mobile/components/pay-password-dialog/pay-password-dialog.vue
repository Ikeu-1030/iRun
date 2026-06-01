<template>
  <view v-if="visible" class="overlay" @click.stop>
    <view class="dialog">
      <text class="dialog-title">{{ title }}</text>
      <text class="dialog-hint">{{ hint }}</text>
      <input
        class="dialog-input"
        type="number"
        password
        v-model="inputValue"
        maxlength="6"
        :focus="visible"
        @input="onInput"
      />
      <view class="dialog-actions">
        <view class="dialog-btn dialog-btn--cancel" @click="onCancel">
          <text>取消</text>
        </view>
        <view class="dialog-btn dialog-btn--confirm" @click="onConfirm">
          <text>确定</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, watch } from 'vue'
import { useDialogState, confirmPayPassword, cancelPayPassword } from '@/utils/pay-password.js'
import { showToast } from '@/utils/toast'

const { visible, title, hint } = useDialogState()
const inputValue = ref('')

watch(visible, (val) => {
  if (val) inputValue.value = ''
})

function onInput(e) {
  // 实时过滤非数字字符
  const raw = e.detail?.value || inputValue.value || ''
  const filtered = raw.replace(/\D/g, '')
  if (filtered !== raw) {
    inputValue.value = filtered
  }
}

function onConfirm() {
  const pw = (inputValue.value || '').replace(/\D/g, '')
  if (!pw || pw.length < 6) {
    showToast('请输入6位数字支付密码')
    return
  }
  confirmPayPassword(pw)
}

function onCancel() {
  cancelPayPassword()
}
</script>

<style scoped>
.overlay {
  position: fixed; top: 0; left: 0; right: 0; bottom: 0; z-index: 9999;
  background: rgba(28, 27, 26, 0.45);
  display: flex; align-items: center; justify-content: center;
}
.dialog {
  width: 580rpx; background: var(--surface-raised); border-radius: var(--radius-lg);
  padding: 48rpx 40rpx 36rpx;
  display: flex; flex-direction: column; align-items: center;
}
.dialog-title { font-size: 34rpx; font-weight: 600; color: var(--text-primary); margin-bottom: 10rpx }
.dialog-hint { font-size: 26rpx; color: var(--text-secondary); margin-bottom: 32rpx; text-align: center }
.dialog-input {
  width: 100%; height: 88rpx; background: var(--surface); border-radius: var(--radius-md);
  padding: 0 28rpx; font-size: 32rpx; letter-spacing: 8rpx;
  color: var(--text-primary); box-sizing: border-box; text-align: center;
  margin-bottom: 36rpx;
}
.dialog-actions { display: flex; gap: 24rpx; width: 100% }
.dialog-btn { flex: 1; height: 80rpx; border-radius: var(--radius-full); display: flex; align-items: center; justify-content: center; transition: transform var(--duration-fast) var(--easing-out) }
.dialog-btn:active { transform: scale(0.95) }
.dialog-btn--cancel { background: var(--surface) }
.dialog-btn--cancel text { font-size: 28rpx; font-weight: 500; color: var(--text-secondary) }
.dialog-btn--confirm { background: var(--primary); box-shadow: var(--shadow-primary) }
.dialog-btn--confirm text { font-size: 28rpx; font-weight: 600; color: #fff }
</style>
