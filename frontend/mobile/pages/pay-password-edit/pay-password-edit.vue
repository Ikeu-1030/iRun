<template>
  <view class="page">
    <uni-nav-bar :title="navTitle" backgroundColor="#FAFAF8" :border="false" statusBar fixed leftIcon="left" @clickLeft="onBack" color="#1C1B1A" />

    <scroll-view class="main-scroll" :style="{ height: scrollHeight + 'px' }" scroll-y enhanced :show-scrollbar="false">
      <!-- 加载中 -->
      <view v-if="loading" class="loading-state">
        <text class="loading-text">加载中…</text>
      </view>

      <template v-else>
        <!-- 首次设置支付密码：需登录密码验证 -->
        <template v-if="mode === 'set'">
          <view class="section-card">
            <view class="form-field">
              <text class="field-label">登录密码</text>
              <input class="field-input" name="password" v-model="loginPassword" placeholder="请输入登录密码验证身份" />
              <text class="field-tip" v-if="store.userInfo?.registerType === 2">您是微信注册用户，初始登录密码为 123456</text>
            </view>
            <view class="form-field">
              <text class="field-label">新支付密码</text>
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
        </template>

        <!-- 登录密码修改模式 -->
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
          <view class="form-hint">
            <iconpark-icon name="info" size="16" color="#F59E0B" />
            <text>如果您是小程序一键注册登录用户，默认初始密码是 123456，请及时修改</text>
          </view>
          <view class="save-btn" :class="{ 'save-btn--disabled': saving }" @click="onChangeLoginPassword"><text>{{ saving ? '修改中…' : '确定修改' }}</text></view>
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
import { userApi } from '@/api'
import { useSubmitLock } from '@/utils/submit-guard'

const store = useStore()
const sysInfo = uni.getSystemInfoSync()
const scrollHeight = sysInfo.windowHeight - sysInfo.statusBarHeight - 44

const mode = ref('')
const { lock, unlock, locked: saving } = useSubmitLock()
const loading = ref(true)

// 首次设置支付密码
const loginPassword = ref('')
const newPayPassword = ref('')
const confirmPayPassword = ref('')

// 修改支付密码
const oldPayPassword = ref('')
const newPayPassword2 = ref('')
const confirmPayPassword2 = ref('')

// 修改登录密码
const oldLoginPassword = ref('')
const newLoginPassword = ref('')

const navTitle = computed(() => {
  if (mode.value === 'login') return '修改登录密码'
  if (mode.value === 'change') return '修改支付密码'
  return '设置支付密码'
})

onLoad((options) => {
  const routeMode = options?.mode || ''
  if (routeMode === 'login') {
    mode.value = 'login'
    loading.value = false
    return
  }
  // 优先使用路由传入的 mode，同时异步查询后端状态
  if (routeMode === 'change') mode.value = 'change'
  else if (routeMode === 'set') mode.value = 'set'
  // 异步验证后端状态，覆盖可能不准确的本地标记
  checkPayPasswordStatus()
})

async function checkPayPasswordStatus() {
  try {
    const hasPassword = await userApi.getPayPasswordStatus({ showLoading: false })
    mode.value = hasPassword === true ? 'change' : 'set'
  } catch (e) {
    // 后端不可用时降级使用路由参数或本地标记
    if (!mode.value) {
      mode.value = store.hasPayPassword ? 'change' : 'set'
    }
  }
  loading.value = false
}

async function onSetPayPassword() {
  if (!loginPassword.value || !newPayPassword.value || !confirmPayPassword.value) {
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
    await userApi.setPayPassword(loginPassword.value, newPayPassword.value)
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
    if (e.message?.includes('请先设置支付密码')) {
      mode.value = 'set'
    }
  } finally {
    unlock()
  }
}

async function onChangeLoginPassword() {
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
.field-tip{font-size:22rpx;color:var(--text-tertiary);margin-top:8rpx;display:block}

.form-hint{display:flex;align-items:flex-start;gap:8rpx;padding:20rpx 8rpx 0;opacity:.7}
.form-hint text{font-size:24rpx;color:#ad6200;line-height:1.5}

.save-btn{height:96rpx;background:var(--primary);border-radius:48rpx;display:flex;align-items:center;justify-content:center;margin-top:48rpx;box-shadow:var(--shadow-sm)}
.save-btn:active{transform:scale(.95)}
.save-btn--disabled{pointer-events:none;opacity:.6}
.save-btn text{font-size:28rpx;font-weight:600;color:#fff}
.bottom-placeholder{height:60rpx}
</style>
