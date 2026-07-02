<template>
  <view class="page">
    <uni-nav-bar title="实名认证" backgroundColor="#FAFAF8" :border="false" statusBar fixed leftIcon="left" @clickLeft="onBack" color="#1C1B1A" />

    <scroll-view class="main-scroll" :style="{ height: scrollHeight + 'px' }" scroll-y enhanced :show-scrollbar="false">
      <view class="section-card">
        <text class="section-title">身份信息</text>
        <view class="form-field">
          <text class="field-label">真实姓名</text>
          <input class="field-input" v-model="form.realName" placeholder="请输入真实姓名" />
        </view>
      </view>

      <view class="section-card">
        <text class="section-title">证件上传</text>
        <text class="section-hint">请上传身份证正面照片或学生证照片</text>
        <UploadGrid v-model="certImages" :maxCount="1" />
      </view>

      <view class="submit-btn" @click="onSubmit"><text>{{ submitting ? '提交中…' : '提交认证' }}</text></view>
      <view class="bottom-placeholder"></view>
    </scroll-view>
  </view>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useStore } from '@/store/index.js'
import { userApi } from '@/api'
import UploadGrid from '@/components/upload-grid/upload-grid.vue'

const store = useStore()
const sysInfo = uni.getSystemInfoSync()
const scrollHeight = sysInfo.windowHeight - sysInfo.statusBarHeight - 44

const form = reactive({ realName: '' })
const certImages = ref([])
const submitting = ref(false)

async function onSubmit() {
  if (!form.realName.trim()) { uni.showToast({ title: '请输入真实姓名', icon: 'none' }); return }
  if (certImages.value.length === 0) { uni.showToast({ title: '请上传证件照片', icon: 'none' }); return }
  submitting.value = true
  try {
    await userApi.certify(form.realName, certImages.value[0])
    store.userInfo.isCertify = 1
    uni.showToast({ title: '实名认证已提交', icon: 'success' })
    setTimeout(() => uni.navigateBack(), 1000)
  } catch (e) { /* handled */ }
  submitting.value = false
}

function onBack() { uni.navigateBack() }
</script>

<style scoped>
.page{width:100%;height:100vh;display:flex;flex-direction:column;background:var(--background);overflow:hidden}
.main-scroll{box-sizing:border-box;width:100%;padding:0 32rpx;padding-bottom:60rpx}
.section-card{background:var(--surface-raised);border-radius:var(--radius-card);padding:32rpx;margin-top:24rpx;box-shadow:var(--shadow-sm)}
.section-title{font-size:32rpx;font-weight:600;color:var(--text-primary);display:block;margin-bottom:8rpx}
.section-hint{font-size:24rpx;color:var(--text-secondary);display:block;margin-bottom:20rpx}
.form-field{padding:20rpx 0}
.field-label{font-size:26rpx;color:var(--text-secondary);display:block;margin-bottom:12rpx}
.field-input{width:100%;height:80rpx;background:var(--surface);border-radius:14rpx;padding:0 22rpx;font-size:28rpx;color:var(--text-primary);box-sizing:border-box}
.submit-btn{height:96rpx;background:var(--primary);border-radius:var(--radius-full);display:flex;align-items:center;justify-content:center;margin-top:48rpx;box-shadow:var(--shadow-primary);transition:transform var(--duration-fast) var(--easing-out)}
.submit-btn:active{transform:scale(.95)}
.submit-btn text{font-size:28rpx;font-weight:600;color:#fff}
.bottom-placeholder{height:60rpx}
</style>
