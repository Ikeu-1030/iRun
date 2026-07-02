<template>
  <view class="page">
    <uni-nav-bar :title="navTitle" backgroundColor="#FAFAF8" :border="false" statusBar fixed leftIcon="left" @clickLeft="onBack" color="#1C1B1A" />

    <scroll-view class="main-scroll" :style="{ height: scrollHeight + 'px' }" scroll-y enhanced :show-scrollbar="false">
      <view v-if="loading" class="loading-state">
        <text class="loading-text">加载中…</text>
      </view>

      <template v-else>
        <!-- 首次设置支付密码：无需身份校验 -->
        <template v-if="mode === 'set'">
          <view class="section-card">
            <view class="form-field">
              <text class="field-label">支付密码</text>
              <input class="field-input" name="number" password v-model="newPayPassword" placeholder="请输入6位数字支付密码" maxlength="6" />
            </view>
            <view class="form-field form-field--last">
              <text class="field-label">确认支付密码</text>
              <input class="field-input" name="number" password v-model="confirmPayPassword" placeholder="请再次输入支付密码" maxlength="6" />
            </view>
          </view>
          <view class="form-hint">
            <iconpark-icon name="info" size="16" color="#F59E0B" />
            <text>支付密码用于发布任务、充值、提现等资金操作，仅支持6位数字，请牢记</text>
          </view>
          <view class="save-btn" :class="{ 'save-btn--disabled': saving }" @click="onSetPayPassword"><text>{{ saving ? '设置中…' : '完成设置' }}</text></view>
        </template>

        <!-- 修改支付密码：需原支付密码 -->
        <template v-if="mode === 'change'">
          <view class="section-card">
            <view class="form-field">
              <text class="field-label">原支付密码</text>
              <input class="field-input" name="number" password v-model="oldPayPassword" placeholder="请输入原支付密码" maxlength="6" />
            </view>
            <view class="form-field">
              <text class="field-label">新支付密码</text>
              <input class="field-input" name="number" password v-model="newPayPassword2" placeholder="请输入新的6位数字支付密码" maxlength="6" />
            </view>
            <view class="form-field form-field--last">
              <text class="field-label">确认新密码</text>
              <input class="field-input" name="number" password v-model="confirmPayPassword2" placeholder="请再次输入新支付密码" maxlength="6" />
            </view>
          </view>
          <view class="form-hint">
            <iconpark-icon name="info" size="16" color="#F59E0B" />
            <text>修改后原支付密码将失效，请牢记新密码</text>
          </view>
          <view class="save-btn" :class="{ 'save-btn--disabled': saving }" @click="onChangePayPassword"><text>{{ saving ? '修改中…' : '确认修改' }}</text></view>
          <text class="forgot-link" @click="goForgot('pay')">忘记密码？</text>
        </template>

        <!-- 修改登录密码：需旧密码 -->
        <template v-if="mode === 'login'">
          <view class="section-card">
            <view class="form-field">
              <text class="field-label">旧密码</text>
              <input class="field-input" name="password" v-model="oldLoginPassword" placeholder="请输入旧密码" />
            </view>
            <view class="form-field form-field--last">
              <text class="field-label">新密码</text>
              <input class="field-input" name="password" v-model="newLoginPassword" placeholder="请输入新密码" />
            </view>
          </view>
          <view class="form-hint" v-if="isWechatUser">
            <iconpark-icon name="info" size="16" color="#F59E0B" />
            <text>您是微信注册用户，请通过下方"忘记密码"修改您的登录密码</text>
          </view>
          <view class="save-btn" :class="{ 'save-btn--disabled': saving }" @click="onChangeLoginPassword"><text>{{ saving ? '修改中…' : '确定修改' }}</text></view>
          <text class="forgot-link" @click="goForgot('login')">忘记密码？</text>
        </template>

        <!-- 忘记密码：短信验证码验证后重置 -->
        <template v-if="mode === 'forgot'">
          <view class="section-card">
            <view class="form-field">
              <text class="field-label">手机号</text>
              <input class="field-input" name="tel" type="number" v-model="forgotPhone" placeholder="请输入手机号" maxlength="11" />
            </view>
            <view class="form-field">
              <text class="field-label">短信验证码</text>
              <view class="code-row">
                <input class="field-input code-input" name="number" type="number" v-model="forgotCode" placeholder="请输入验证码" maxlength="6" />
                <view class="code-send-btn" :class="{ 'code-send-btn--counting': countdown > 0 }" @click="onSendCode">
                  <text>{{ countdown > 0 ? countdown + 's' : '获取验证码' }}</text>
                </view>
              </view>
            </view>
            <!-- 重置支付密码：6位数字 -->
            <template v-if="forgotTarget === 'pay'">
              <view class="form-field">
                <text class="field-label">新支付密码</text>
                <input class="field-input" name="number" password v-model="forgotPayPassword" placeholder="请输入6位数字支付密码" maxlength="6" />
              </view>
              <view class="form-field form-field--last">
                <text class="field-label">确认支付密码</text>
                <input class="field-input" name="number" password v-model="forgotPayPasswordConfirm" placeholder="请再次输入支付密码" maxlength="6" />
              </view>
            </template>
            <!-- 重置登录密码 -->
            <template v-else>
              <view class="form-field form-field--last">
                <text class="field-label">新密码</text>
                <input class="field-input" name="password" v-model="forgotNewPassword" placeholder="请输入新登录密码" />
              </view>
            </template>
          </view>
          <view class="form-hint">
            <iconpark-icon name="info" size="16" color="#F59E0B" />
            <text>重置后请使用新密码，原密码将失效</text>
          </view>
          <view class="save-btn" :class="{ 'save-btn--disabled': saving }" @click="onResetSubmit"><text>{{ saving ? '重置中…' : '确认重置' }}</text></view>
        </template>
      </template>

      <view class="bottom-placeholder"></view>
    </scroll-view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { useStore } from '@/store/index.js'
import { useSmsCooldown } from '@/utils/useSmsCooldown'
import { userApi } from '@/api'
import { useSubmitLock } from '@/utils/submit-guard'

const store = useStore()
const sysInfo = uni.getSystemInfoSync()
const scrollHeight = sysInfo.windowHeight - sysInfo.statusBarHeight - 44

const mode = ref('')
const forgotTarget = ref('login')
const { lock, unlock, locked: saving } = useSubmitLock()
const loading = ref(true)

// 首次设置支付密码
const newPayPassword = ref('')
const confirmPayPassword = ref('')

// 修改支付密码
const oldPayPassword = ref('')
const newPayPassword2 = ref('')
const confirmPayPassword2 = ref('')

// 修改登录密码
const oldLoginPassword = ref('')
const newLoginPassword = ref('')

// 忘记密码
const forgotPhone = ref('')
const forgotCode = ref('')
const forgotNewPassword = ref('')
const forgotPayPassword = ref('')
const forgotPayPasswordConfirm = ref('')

// 短信验证码
const { countdown, startCooldown } = useSmsCooldown()

const isWechatUser = computed(() => store.userInfo?.registerType === 2)

const navTitle = computed(() => {
  if (mode.value === 'login') return '修改登录密码'
  if (mode.value === 'change') return '修改支付密码'
  if (mode.value === 'forgot') return forgotTarget.value === 'pay' ? '重置支付密码' : '重置登录密码'
  return '设置支付密码'
})

onLoad((options) => {
  const routeMode = options?.mode || ''
  if (routeMode === 'login' || routeMode === 'forgot') {
    mode.value = routeMode
    if (routeMode === 'forgot') {
      forgotTarget.value = options?.target || 'login'
      forgotPhone.value = store.userInfo?.phone || ''
    }
    loading.value = false
    return
  }
  if (routeMode === 'change') mode.value = 'change'
  else if (routeMode === 'set') mode.value = 'set'
  checkPayPasswordStatus()
})

async function checkPayPasswordStatus() {
  try {
    const hasPassword = await userApi.getPayPasswordStatus({ showLoading: false })
    mode.value = hasPassword === true ? 'change' : 'set'
  } catch (e) {
    if (!mode.value) mode.value = store.hasPayPassword ? 'change' : 'set'
  }
  loading.value = false
}

function goForgot(target) {
  forgotTarget.value = target
  forgotPhone.value = store.userInfo?.phone || ''
  mode.value = 'forgot'
}

async function onSendCode() {
  if (countdown.value > 0) return
  const phone = mode.value === 'forgot' ? forgotPhone.value : store.userInfo?.phone
  if (!phone || !/^1[3-9]\d{9}$/.test(phone)) {
    uni.showToast({ title: '请输入正确的手机号', icon: 'none' })
    return
  }
  try {
    const operation = mode.value === 'forgot'
      ? (forgotTarget.value === 'pay' ? 'reset_pay_password' : 'reset_password')
      : 'reset_password'
    await userApi.sendCode(phone, operation)
    startCooldown()
  } catch (e) { /* handled */ }
}

async function onSetPayPassword() {
  if (!newPayPassword.value || !confirmPayPassword.value) {
    uni.showToast({ title: '请填写完整', icon: 'none' })
    return
  }
  if (newPayPassword.value.length < 6) {
    uni.showToast({ title: '至少6位数字密码', icon: 'none' })
    return
  }
  if (!/^\d+$/.test(newPayPassword.value)) {
    uni.showToast({ title: '密码仅支持数字', icon: 'none' })
    return
  }
  if (newPayPassword.value !== confirmPayPassword.value) {
    uni.showToast({ title: '两次密码不一致', icon: 'none' })
    return
  }
  if (!lock()) return
  try {
    await userApi.setPayPassword(newPayPassword.value)
    store.markPayPasswordSet()
    uni.showToast({ title: '设置成功', icon: 'success' })
    setTimeout(() => uni.navigateBack(), 1000)
  } catch (e) {
    if (e.message?.includes('已设置支付密码')) {
      store.markPayPasswordSet()
      mode.value = 'change'
    }
  } finally {
    unlock()
  }
}

async function onChangePayPassword() {
  if (!oldPayPassword.value || !newPayPassword2.value || !confirmPayPassword2.value) {
    uni.showToast({ title: '请填写完整', icon: 'none' })
    return
  }
  if (newPayPassword2.value.length < 6) {
    uni.showToast({ title: '至少6位数字密码', icon: 'none' })
    return
  }
  if (!/^\d+$/.test(newPayPassword2.value)) {
    uni.showToast({ title: '密码仅支持数字', icon: 'none' })
    return
  }
  if (newPayPassword2.value !== confirmPayPassword2.value) {
    uni.showToast({ title: '两次密码不一致', icon: 'none' })
    return
  }
  if (!lock()) return
  try {
    await userApi.changePayPassword(oldPayPassword.value, newPayPassword2.value)
    store.markPayPasswordSet()
    uni.showToast({ title: '修改成功', icon: 'success' })
    setTimeout(() => uni.navigateBack(), 1000)
  } catch (e) {
    if (e.message?.includes('请先设置支付密码')) mode.value = 'set'
  } finally {
    unlock()
  }
}

async function onChangeLoginPassword() {
  // 微信注册用户没有原始密码，引导使用"忘记密码"流程
  if (isWechatUser.value) {
    uni.showToast({ title: '微信用户请通过下方"忘记密码"修改', icon: 'none', duration: 2500 })
    return
  }
  if (!oldLoginPassword.value || !newLoginPassword.value) {
    uni.showToast({ title: '请填写完整', icon: 'none' })
    return
  }
  if (!lock()) return
  try {
    await userApi.updatePassword(oldLoginPassword.value, newLoginPassword.value)
    uni.showToast({ title: '密码已修改', icon: 'success' })
    setTimeout(() => uni.navigateBack(), 1000)
  } catch (e) { /* handled */ } finally {
    unlock()
  }
}

async function onResetSubmit() {
  if (!forgotPhone.value || !/^1[3-9]\d{9}$/.test(forgotPhone.value)) {
    uni.showToast({ title: '请输入正确的手机号', icon: 'none' })
    return
  }
  if (!forgotCode.value) {
    uni.showToast({ title: '请输入验证码', icon: 'none' })
    return
  }
  if (forgotTarget.value === 'pay') {
    if (!forgotPayPassword.value || !forgotPayPasswordConfirm.value) {
      uni.showToast({ title: '请填写完整', icon: 'none' })
      return
    }
    if (forgotPayPassword.value.length < 6 || !/^\d+$/.test(forgotPayPassword.value)) {
      uni.showToast({ title: '密码需为6位数字', icon: 'none' })
      return
    }
    if (forgotPayPassword.value !== forgotPayPasswordConfirm.value) {
      uni.showToast({ title: '两次密码不一致', icon: 'none' })
      return
    }
  } else {
    if (!forgotNewPassword.value || forgotNewPassword.value.length < 6) {
      uni.showToast({ title: '密码长度不能少于6位', icon: 'none' })
      return
    }
  }
  if (!lock()) return
  try {
    if (forgotTarget.value === 'pay') {
      await userApi.resetPayPassword(forgotPhone.value, forgotCode.value, forgotPayPassword.value)
      store.markPayPasswordSet()
    } else {
      await userApi.resetPassword(forgotPhone.value, forgotCode.value, forgotNewPassword.value)
    }
    uni.showToast({ title: '密码重置成功', icon: 'success' })
    setTimeout(() => uni.navigateBack(), 1000)
  } catch (e) { /* handled */ } finally {
    unlock()
  }
}

function onBack() { uni.navigateBack() }
</script>

<style scoped>
.page{width:100%;height:100vh;display:flex;flex-direction:column;background:var(--background);overflow:hidden}
.main-scroll{box-sizing:border-box;width:100%;padding:0 32rpx;padding-bottom:60rpx}

.loading-state{display:flex;align-items:center;justify-content:center;padding:120rpx 0}
.loading-text{font-size:28rpx;color:var(--text-tertiary)}

.section-card{background:var(--surface-raised);border-radius:var(--radius-lg);padding:0;margin-top:24rpx;box-shadow:var(--shadow-sm);overflow:hidden}
.form-field{padding:32rpx;border-bottom:1rpx solid var(--outline-light)}
.form-field--last{border-bottom:none}
.field-label{font-size:26rpx;color:var(--text-secondary);display:block;margin-bottom:12rpx}
.field-input{width:100%;height:84rpx;background:var(--surface);border-radius:14rpx;padding:0 22rpx;font-size:28rpx;color:var(--text-primary);box-sizing:border-box}
.code-row{display:flex;align-items:center;gap:16rpx}
.code-input{flex:1}
.code-send-btn{height:84rpx;padding:0 24rpx;background:var(--surface);border-radius:14rpx;display:flex;align-items:center;justify-content:center;white-space:nowrap;font-size:24rpx;color:var(--primary);border:2rpx solid var(--primary)}
.code-send-btn:active{opacity:.7}
.code-send-btn--counting{border-color:var(--outline);color:var(--text-tertiary);pointer-events:none}

.form-hint{display:flex;align-items:flex-start;gap:8rpx;padding:20rpx 8rpx 0;opacity:.7}
.form-hint text{font-size:24rpx;color:#ad6200;line-height:1.5}

.forgot-link{display:block;text-align:left;font-size:24rpx;color:#3b82f6;padding:16rpx 8rpx 0}

.save-btn{height:96rpx;background:var(--primary);border-radius:48rpx;display:flex;align-items:center;justify-content:center;margin-top:24rpx;box-shadow:var(--shadow-sm)}
.save-btn:active{transform:scale(.95)}
.save-btn--disabled{pointer-events:none;opacity:.6}
.save-btn text{font-size:28rpx;font-weight:600;color:#fff}
.bottom-placeholder{height:60rpx}
</style>
