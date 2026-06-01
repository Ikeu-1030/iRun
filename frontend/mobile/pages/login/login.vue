<template>
  <view class="page">
    <!-- 启动验 token 中的加载 -->
    <view v-if="checking" class="checking-screen">
      <text class="checking-text">加载中…</text>
    </view>

    <template v-else>
      <!-- 品牌区 -->
      <view class="brand-section">
        <image class="brand-logo" src="/static/icons/logo.png" mode="aspectFill" />
        <text class="brand-title">小i跑腿</text>
        <text class="brand-subtitle">助力您的"校园最后一公里"</text>
      </view>

      <!-- 登录方式 -->
      <view class="login-tabs">
        <view class="login-tab" :class="{ 'login-tab--active': loginMode === 'password' }" @click="loginMode = 'password'">
          <text>密码登录</text>
        </view>
        <view class="login-tab" :class="{ 'login-tab--active': loginMode === 'code' }" @click="loginMode = 'code'">
          <text>验证码登录</text>
        </view>
      </view>

      <!-- 表单 -->
      <view class="form-card">
        <!-- 密码登录 → 账号 + 密码 -->
        <template v-if="loginMode === 'password'">
          <view class="field-row">
            <input class="field-input" v-model="username" placeholder="请输入账号/手机号" />
          </view>
          <view class="field-row field-row--pwd">
            <input class="field-input" :password="!showPwd" v-model="password" placeholder="请输入密码" />
            <view class="eye-toggle" @click="showPwd = !showPwd">
              <iconpark-icon :name="showPwd ? 'preview-open' : 'preview-close'" size="20" color="#8F8D88" />
            </view>
          </view>
        </template>

        <!-- 验证码登录 → 手机号 + 验证码 -->
        <template v-else>
          <view class="phone-row">
            <text class="phone-prefix">+86</text>
            <input class="phone-input" name="tel" v-model="phone" placeholder="请输入手机号" maxlength="11" />
          </view>
          <view class="code-row">
            <input class="code-input" name="number" v-model="code" placeholder="输入验证码" maxlength="6" />
            <view class="send-code-btn" :class="{ 'send-code-btn--disabled': countdown > 0 || !phoneValid }" @click="onSendCode">
              <text>{{ countdown > 0 ? `${countdown}s` : '获取验证码' }}</text>
            </view>
          </view>
        </template>

        <view class="submit-btn" :class="{ 'submit-btn--disabled': !canSubmit || submitting }" @click="onLogin">
          <text>{{ submitting ? '登录中…' : '登录' }}</text>
        </view>

        <view class="link-row">
          <text class="link-text" @click="showRegister = true">还没有账号？立即注册</text>
        </view>
      </view>

      <!-- 微信登录 -->
      <view class="wechat-section">
        <view class="divider-row">
          <view class="divider-line"></view>
          <text class="divider-text">其他登录方式</text>
          <view class="divider-line"></view>
        </view>
        <view class="wechat-btn" @click="onWechatLogin">
          <uni-icons type="weixin" size="28" color="#07c160"></uni-icons>
          <text class="wechat-btn-text">微信一键登录</text>
        </view>
      </view>

      <view class="agreement-text">
        <text>登录即表示同意</text>
        <text class="agreement-link">《用户协议》</text>
        <text>和</text>
        <text class="agreement-link">《隐私政策》</text>
      </view>

      <!-- ===== 注册面板（覆盖层） ===== -->
      <view v-if="showRegister" class="register-overlay" @click="showRegister = false">
        <view class="register-panel" @click.stop>
          <view class="register-header">
            <text class="register-title">注册账号</text>
            <view class="register-close" @click="showRegister = false">
              <text class="register-close-text">✕</text>
            </view>
          </view>

          <view class="reg-field">
            <text class="reg-label">手机号</text>
            <input class="reg-input" name="tel" v-model="regPhone" placeholder="请输入手机号" maxlength="11" />
          </view>
          <view class="reg-field reg-field--code">
            <input class="reg-input" name="number" v-model="regCode" placeholder="输入验证码" maxlength="6" />
            <view class="send-code-btn" :class="{ 'send-code-btn--disabled': regCountdown > 0 || !regPhoneValid }" @click="onSendRegCode">
              <text>{{ regCountdown > 0 ? `${regCountdown}s` : '获取验证码' }}</text>
            </view>
          </view>
          <view class="reg-field">
            <text class="reg-label">设置用户名</text>
            <input class="reg-input" v-model="regUsername" placeholder="给自己取个名字" maxlength="16" />
          </view>
          <view class="reg-field">
            <text class="reg-label">设置密码</text>
            <view class="reg-pwd-row">
              <input class="reg-input" :password="!showRegPwd" v-model="regPassword" placeholder="6-20位密码" maxlength="20" />
              <view class="eye-toggle" @click="showRegPwd = !showRegPwd">
                <iconpark-icon :name="showRegPwd ? 'preview-open' : 'preview-close'" size="20" color="#8F8D88" />
              </view>
            </view>
          </view>

          <view class="submit-btn" :class="{ 'submit-btn--disabled': !canRegSubmit || regSubmitting }" @click="onRegister">
            <text>{{ regSubmitting ? '注册中…' : '注册并登录' }}</text>
          </view>
        </view>
      </view>
    </template>
  </view>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useStore } from '@/store/index.js'
import { getToken } from '@/utils/request'
import { userApi } from '@/api'

const store = useStore()

// ---------- 启动时检查已有 token ----------
const checking = ref(true)

onMounted(async () => {
  const token = getToken()
  if (!token) {
    checking.value = false
    return
  }
  store.token = token
  try {
    await store.fetchUserInfo()
    // token 有效，跳转首页
    uni.reLaunch({ url: '/pages/index/index' })
  } catch (e) {
    // token 无效，清除并展示登录页
    store.clearLogin()
    checking.value = false
  }
})

// ---------- 登录态 ----------
const loginMode = ref('password')
const username = ref('')
const phone = ref('')
const code = ref('')
const password = ref('')
const showPwd = ref(false)
const submitting = ref(false)
const countdown = ref(0)

const phoneValid = computed(() => /^1[3-9]\d{9}$/.test(phone.value))
const canSubmit = computed(() => {
  if (loginMode.value === 'password') return username.value.trim().length >= 1 && password.value.length >= 6
  return phoneValid.value && code.value.length === 6
})

// ---------- 发送验证码 ----------
let codeTimer = null

async function onSendCode() {
  if (countdown.value > 0 || !phoneValid.value) return
  try {
    await userApi.sendCode(phone.value)
    uni.showToast({ title: '验证码已发送', icon: 'success' })
    countdown.value = 60
    codeTimer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) clearInterval(codeTimer)
    }, 1000)
  } catch (e) { /* handled */ }
}

// ---------- 登录 ----------
async function onLogin() {
  if (!canSubmit.value || submitting.value) return
  submitting.value = true
  try {
    if (loginMode.value === 'code') {
      await store.login({ loginType: 2, phone: phone.value, code: code.value })
    } else {
      await store.login({ loginType: 1, username: username.value.trim(), password: password.value })
    }
    await store.fetchUserInfo()
    onLoginSuccess()
  } catch (e) { /* handled */ }
  submitting.value = false
}

// ---------- 微信登录 ----------
async function onWechatLogin() {
  submitting.value = true
  try {
    const loginRes = await new Promise((resolve, reject) => {
      // #ifdef MP-WEIXIN
      uni.login({ provider: 'weixin', success: resolve, fail: reject })
      // #endif
      // #ifndef MP-WEIXIN
      reject(new Error('非微信环境'))
      // #endif
    })
    await store.wechatLogin(loginRes.code)
    await store.fetchUserInfo()
    onLoginSuccess()
  } catch (e) {
    uni.showToast({ title: '微信登录失败', icon: 'none', duration: 2000 })
  }
  submitting.value = false
}

async function onLoginSuccess() {
  uni.showToast({ title: '登录成功', icon: 'success' })
  setTimeout(() => {
    uni.reLaunch({ url: '/pages/index/index' })
  }, 600)
}

// ---------- 注册 ----------
const showRegister = ref(false)
const regPhone = ref('')
const regCode = ref('')
const regUsername = ref('')
const regPassword = ref('')
const showRegPwd = ref(false)
const regSubmitting = ref(false)
const regCountdown = ref(0)

const regPhoneValid = computed(() => /^1[3-9]\d{9}$/.test(regPhone.value))
const canRegSubmit = computed(() =>
  regPhoneValid.value &&
  regCode.value.length === 6 &&
  regUsername.value.trim().length >= 1 &&
  regPassword.value.length >= 6
)

let regCodeTimer = null

async function onSendRegCode() {
  if (regCountdown.value > 0 || !regPhoneValid.value) return
  try {
    await userApi.sendCode(regPhone.value)
    uni.showToast({ title: '验证码已发送', icon: 'success' })
    regCountdown.value = 60
    regCodeTimer = setInterval(() => {
      regCountdown.value--
      if (regCountdown.value <= 0) clearInterval(regCodeTimer)
    }, 1000)
  } catch (e) { /* handled */ }
}

async function onRegister() {
  if (!canRegSubmit.value || regSubmitting.value) return
  regSubmitting.value = true
  try {
    await userApi.register({
      phone: regPhone.value,
      code: regCode.value,
      username: regUsername.value.trim(),
      password: regPassword.value
    })
    // 注册成功后自动登录
    await store.login({ loginType: 1, username: regUsername.value.trim(), password: regPassword.value })
    await store.fetchUserInfo()
    uni.showToast({ title: '注册成功', icon: 'success' })
    setTimeout(() => {
      uni.reLaunch({ url: '/pages/index/index' })
    }, 600)
  } catch (e) { /* handled */ }
  regSubmitting.value = false
}

// 打开注册时复用已输入的手机号
watch(showRegister, (val) => {
  if (val) regPhone.value = phone.value
})
</script>

<style scoped>
.page { width: 100%; min-height: 100vh; background: var(--background); display: flex; flex-direction: column; align-items: center; padding: 0 48rpx; box-sizing: border-box; }

.checking-screen { width: 100%; min-height: 100vh; display: flex; align-items: center; justify-content: center; }
.checking-text { font-size: 28rpx; color: var(--outline); }

/* 品牌区 */
.brand-section { display: flex; flex-direction: column; align-items: center; padding-top: 120rpx; padding-bottom: 64rpx; }
.brand-logo { width: 160rpx; height: 160rpx; border-radius: 50%; margin-bottom: 24rpx; box-shadow: var(--shadow-md); }
.brand-title { font-size: 48rpx; font-weight: 700; color: var(--text-primary); letter-spacing: 2rpx; }
.brand-subtitle { font-size: 28rpx; color: var(--text-secondary); margin-top: 8rpx; }

/* 登录方式切换 */
.login-tabs { display: flex; width: 100%; background: var(--surface-raised); border-radius: var(--radius-card); padding: 6rpx; margin-bottom: 24rpx; box-shadow: var(--shadow-sm); }
.login-tab { flex: 1; text-align: center; padding: 20rpx 0; border-radius: 20rpx; font-size: 28rpx; font-weight: 500; color: var(--text-secondary); }
.login-tab--active { background: var(--primary); color: #fff; font-weight: 600; }

/* 表单 */
.form-card { width: 100%; background: var(--surface-raised); border-radius: var(--radius-lg); padding: 32rpx; box-shadow: var(--shadow-sm); box-sizing: border-box; }
.phone-row { display: flex; align-items: center; gap: 16rpx; margin-bottom: 20rpx; }
.phone-prefix { font-size: 32rpx; font-weight: 600; color: var(--text-primary); flex-shrink: 0; }
.phone-input { flex: 1; height: 88rpx; background: var(--surface); border-radius: 20rpx; padding: 0 24rpx; font-size: 30rpx; color: var(--text-primary); box-sizing: border-box; }
.code-row { display: flex; align-items: center; gap: 16rpx; margin-bottom: 20rpx; }
.code-input { flex: 1; height: 88rpx; background: var(--surface); border-radius: 20rpx; padding: 0 24rpx; font-size: 30rpx; color: var(--text-primary); box-sizing: border-box; }
.field-row { margin-bottom: 20rpx; }
.field-row--pwd { position: relative; }
.field-row--pwd .field-input { padding-right: 80rpx; }
.field-input { width: 100%; height: 88rpx; background: var(--surface); border-radius: 20rpx; padding: 0 24rpx; font-size: 30rpx; color: var(--text-primary); box-sizing: border-box; }
.eye-toggle { position: absolute; right: 0; top: 0; width: 80rpx; height: 88rpx; display: flex; align-items: center; justify-content: center; z-index: 2; }

.send-code-btn { padding: 18rpx 20rpx; background: var(--primary); border-radius: 20rpx; flex-shrink: 0; min-width: 160rpx; text-align: center; }
.send-code-btn--disabled { background: var(--outline); }
.send-code-btn text { font-size: 24rpx; color: #fff; font-weight: 500; white-space: nowrap; }

.submit-btn { height: 92rpx; background: var(--primary); border-radius: 48rpx; display: flex; align-items: center; justify-content: center; margin-top: 12rpx; box-shadow: var(--shadow-sm); }
.submit-btn--disabled { background: var(--outline); box-shadow: none; }
.submit-btn text { font-size: 30rpx; font-weight: 600; color: #fff; }
.submit-btn:active { transform: scale(0.97); }

.link-row { display: flex; justify-content: center; margin-top: 24rpx; }
.link-text { font-size: 26rpx; color: var(--primary); font-weight: 500; }

/* 微信登录 */
.wechat-section { width: 100%; margin-top: 64rpx; }
.divider-row { display: flex; align-items: center; gap: 20rpx; margin-bottom: 32rpx; }
.divider-line { flex: 1; height: 1rpx; background: var(--outline-variant); }
.divider-text { font-size: 24rpx; color: var(--text-tertiary); flex-shrink: 0; }
.wechat-btn { display: flex; align-items: center; justify-content: center; gap: 12rpx; height: 92rpx; background: var(--surface-raised); border: 1rpx solid var(--outline-light); border-radius: 48rpx; }
.wechat-btn:active { background: var(--surface); }
.wechat-btn-text { font-size: 28rpx; font-weight: 500; color: var(--text-primary); }

.agreement-text { margin-top: 48rpx; font-size: 22rpx; color: var(--text-tertiary); text-align: center; padding-bottom: 60rpx; }
.agreement-link { color: var(--primary); }

/* 注册覆盖层 */
.register-overlay { position: fixed; inset: 0; z-index: 999; background: rgba(0,0,0,0.4); display: flex; align-items: flex-end; }
.register-panel { width: 100%; background: var(--surface-raised); border-radius: var(--radius-lg) var(--radius-lg) 0 0; padding: 32rpx 36rpx; padding-bottom: calc(32rpx + env(safe-area-inset-bottom)); }
.register-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 28rpx; }
.register-title { font-size: 34rpx; font-weight: 700; color: var(--text-primary); }
.register-close { width: 56rpx; height: 56rpx; border-radius: 50%; background: var(--surface); display: flex; align-items: center; justify-content: center; }
.register-close-text { font-size: 24rpx; color: var(--text-tertiary); font-weight: 600; }

.reg-field { margin-bottom: 20rpx; }
.reg-field--code { display: flex; align-items: center; gap: 16rpx; }
.reg-field--code .reg-input { flex: 1; }
.reg-label { font-size: 24rpx; color: var(--text-secondary); display: block; margin-bottom: 10rpx; }
.reg-input { width: 100%; height: 84rpx; background: var(--surface); border-radius: 16rpx; padding: 0 22rpx; font-size: 28rpx; color: var(--text-primary); box-sizing: border-box; }
.reg-pwd-row { position: relative; }
.reg-pwd-row .reg-input { padding-right: 80rpx; }
.reg-pwd-row .eye-toggle { height: 84rpx; }
</style>
