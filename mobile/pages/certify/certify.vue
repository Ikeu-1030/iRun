<template>
  <view class="page">
    <uni-nav-bar backgroundColor="#FAFAF8" :border="false" statusBar>
      <template v-slot:left>
        <view class="nav-back" @click="goBack">
          <iconpark-icon name="left" size="22" color="#FF6B4A" />
        </view>
      </template>
      <view class="nav-title-wrap"><text class="nav-title-text">实名认证</text></view>
      <template v-slot:right><view></view></template>
    </uni-nav-bar>

    <scroll-view class="main-scroll" :style="{ height: scrollHeight + 'px' }" scroll-y enhanced :show-scrollbar="false">
      <!-- State 0: 未认证 — form -->
      <template v-if="state === 'form'">
        <view class="cert-card">
          <view class="cert-icon-wrap">
            <view class="cert-icon">
              <iconpark-icon name="locked-filled" size="40" color="#FF6B4A" />
            </view>
          </view>
          <text class="cert-title">学生身份认证</text>
          <text class="cert-desc">为保障校园安全，所有用户需完成学生身份认证后才能使用发布任务、聊天等功能</text>

          <view class="checklist">
            <view class="check-item">
              <iconpark-icon name="checkmarkempty" size="18" color="#34d399" />
              <text>真实姓名</text>
            </view>
            <view class="check-item">
              <iconpark-icon name="checkmarkempty" size="18" color="#34d399" />
              <text>学号</text>
            </view>
            <view class="check-item">
              <iconpark-icon name="checkmarkempty" size="18" color="#34d399" />
              <text>学生证照片</text>
            </view>
          </view>
        </view>

        <view class="form-section">
          <view class="form-item">
            <text class="form-label">真实姓名</text>
            <input class="form-input" v-model="form.realName" placeholder="请输入真实姓名" placeholder-style="color:#D4D2CC" />
          </view>
          <view class="form-item">
            <text class="form-label">学号</text>
            <input class="form-input" v-model="form.studentId" placeholder="请输入学号" placeholder-style="color:#D4D2CC" />
          </view>
          <view class="form-item form-item--upload">
            <text class="form-label">学生证照片</text>
            <UploadGrid v-model="certImages" :maxCount="1" />
          </view>
        </view>

        <view class="cert-notice">
          <iconpark-icon name="info-filled" size="16" color="#8F8D88" />
          <text>认证前你可以浏览任务大厅和排行榜</text>
        </view>

        <view class="submit-btn" :class="{ 'submit-btn--disabled': !canSubmit }" @click="onSubmit">
          <text>{{ submitting ? '提交中…' : '提交认证' }}</text>
        </view>
      </template>

      <!-- State 1: 审核中 -->
      <template v-else-if="state === 'pending'">
        <view class="status-card">
          <view class="status-icon status-icon--pending">
            <iconpark-icon name="clock-filled" size="48" color="#e67e22" />
          </view>
          <text class="status-title">认证审核中</text>
          <text class="status-desc">你的学生身份认证正在审核中，通常会在24小时内完成</text>
          <view class="status-info">
            <view class="info-row">
              <text class="info-label">姓名</text>
              <text class="info-value">{{ certInfo.realName || '—' }}</text>
            </view>
            <view class="info-row">
              <text class="info-label">学号</text>
              <text class="info-value">{{ certInfo.studentId || '—' }}</text>
            </view>
            <view class="info-row info-row--img" @click="previewCertImg">
              <text class="info-label">学生证</text>
              <text class="info-value info-value--link">查看照片</text>
            </view>
          </view>
          <view class="status-notice">
            <text>审核期间你可以浏览任务大厅，审核通过后即可使用全部功能</text>
          </view>
        </view>

        <view class="go-hall-btn" @click="goHall">
          <text>浏览任务大厅</text>
          <iconpark-icon name="right" size="16" color="#FF6B4A" />
        </view>
      </template>

      <!-- State 2: 已认证 -->
      <template v-else-if="state === 'approved'">
        <view class="status-card">
          <view class="status-icon status-icon--approved">
            <iconpark-icon name="checkmarkempty" size="48" color="#34d399" />
          </view>
          <text class="status-title">认证通过</text>
          <text class="status-desc">你已完成学生身份认证，可以使用全部功能</text>
          <view class="status-info">
            <view class="info-row">
              <text class="info-label">姓名</text>
              <text class="info-value">{{ certInfo.realName || '—' }}</text>
            </view>
            <view class="info-row">
              <text class="info-label">学号</text>
              <text class="info-value">{{ certInfo.studentId || '—' }}</text>
            </view>
          </view>
        </view>

        <view class="go-hall-btn" @click="goBack">
          <text>返回</text>
          <iconpark-icon name="right" size="16" color="#FF6B4A" />
        </view>
      </template>

      <!-- State 3: 驳回 -->
      <template v-else-if="state === 'rejected'">
        <view class="status-card">
          <view class="status-icon status-icon--rejected">
            <iconpark-icon name="closeempty" size="48" color="#ba1a1a" />
          </view>
          <text class="status-title">认证已驳回</text>
          <text class="status-desc" v-if="certInfo.certifyRemark">{{ certInfo.certifyRemark }}</text>
          <text class="status-desc" v-else>你的学生身份认证未通过审核，请重新提交</text>
          <view class="status-info">
            <view class="info-row">
              <text class="info-label">姓名</text>
              <text class="info-value">{{ certInfo.realName || '—' }}</text>
            </view>
            <view class="info-row">
              <text class="info-label">学号</text>
              <text class="info-value">{{ certInfo.studentId || '—' }}</text>
            </view>
          </view>
        </view>

        <view class="submit-btn" @click="onRetry">
          <text>重新提交认证</text>
        </view>
      </template>

      <view class="bottom-placeholder"></view>
    </scroll-view>
  </view>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useStore } from '@/store/index.js'
import { userApi } from '@/api'
import UploadGrid from '@/components/upload-grid/upload-grid.vue'

const store = useStore()
const sysInfo = uni.getSystemInfoSync()
const scrollHeight = sysInfo.windowHeight - sysInfo.statusBarHeight - 44

const state = ref('form') // form | pending | approved | rejected
const submitting = ref(false)
const certInfo = ref({ realName: '', studentId: '', certifyImg: '', certifyRemark: '' })

const certImages = ref([])

const form = reactive({
  realName: '',
  studentId: '',
  certImageUrl: ''
})

const canSubmit = computed(() => form.realName.trim() && form.studentId.trim() && certImages.value.length > 0 && !submitting.value)

function determineState(isCertify) {
  if (isCertify === 0) return 'form'
  if (isCertify === 1) return 'pending'
  if (isCertify === 2) return 'approved'
  if (isCertify === 3) return 'rejected'
  return 'form'
}

async function loadStatus() {
  try {
    const data = await userApi.getCertifyStatus({ showLoading: false })
    if (data) {
      certInfo.value = {
        realName: data.realName || '',
        studentId: data.studentId || '',
        certifyImg: data.certifyImg || '',
        certifyRemark: data.verifyRemark || ''
      }
      state.value = determineState(data.isCertify)

      // Pre-fill form if rejected or not yet submitted
      if (certInfo.value.realName) form.realName = certInfo.value.realName
      if (certInfo.value.studentId) form.studentId = certInfo.value.studentId
      if (certInfo.value.certifyImg) { form.certImageUrl = certInfo.value.certifyImg; certImages.value = [certInfo.value.certifyImg] }

      // Sync to store
      store.userInfo.isCertify = data.isCertify
    }
  } catch (e) {
    // Fall back to store state
    state.value = determineState(store.userInfo.isCertify)
  }
}

async function onSubmit() {
  if (!canSubmit.value) return
  form.certImageUrl = certImages.value[0] || ''
  submitting.value = true
  try {
    await userApi.certify(form.realName.trim(), form.studentId.trim(), form.certImageUrl)
    uni.showToast({ title: '认证提交成功', icon: 'success' })
    store.userInfo.isCertify = 1
    store.userInfo.realName = form.realName.trim()
    state.value = 'pending'
    certInfo.value = {
      realName: form.realName.trim(),
      studentId: form.studentId.trim(),
      certifyImg: form.certImageUrl,
      certifyRemark: ''
    }
  } catch (e) { /* request interceptor handles toast */ }
  submitting.value = false
}

function onRetry() {
  state.value = 'form'
}

function previewCertImg() {
  if (certInfo.value.certifyImg) {
    uni.previewImage({ urls: [certInfo.value.certifyImg] })
  }
}

function goHall() {
  uni.switchTab({ url: '/pages/task-hall/task-hall' })
}

function goBack() {
  uni.navigateBack({ delta: 1, fail: () => uni.switchTab({ url: '/pages/userprofile/userprofile' }) })
}

onShow(() => { loadStatus() })
loadStatus()
</script>

<style scoped>
.page { width: 100%; height: 100vh; display: flex; flex-direction: column; background: var(--background); overflow: hidden; }

.nav-back { width: 68rpx; height: 68rpx; display: flex; align-items: center; justify-content: center; border-radius: 50%; }
.nav-back:active { background: var(--surface-hover); }
.nav-title-wrap { display: flex; align-items: center; justify-content: center; }
.nav-title-text { font-size: 34rpx; font-weight: 600; color: var(--text-primary); }

.main-scroll { box-sizing: border-box; width: 100%; padding: 0 32rpx; padding-bottom: 120rpx; }

/* Guide card */
.cert-card { background: var(--surface-raised); border-radius: 32rpx; padding: 48rpx 36rpx; margin-top: 16rpx; box-shadow: var(--shadow-sm); display: flex; flex-direction: column; align-items: center; text-align: center; }
.cert-icon-wrap { margin-bottom: 24rpx; }
.cert-icon { width: 112rpx; height: 112rpx; border-radius: 50%; background: var(--primary-container); display: flex; align-items: center; justify-content: center; }
.cert-title { font-size: 36rpx; font-weight: 700; color: var(--text-primary); margin-bottom: 16rpx; }
.cert-desc { font-size: 26rpx; color: var(--text-secondary); line-height: 1.6; margin-bottom: 32rpx; }
.checklist { display: flex; gap: 24rpx; }
.check-item { display: flex; align-items: center; gap: 8rpx; }
.check-item text { font-size: 24rpx; color: var(--text-secondary); }

/* Form */
.form-section { background: var(--surface-raised); border-radius: 32rpx; margin-top: 24rpx; overflow: hidden; box-shadow: var(--shadow-sm); }
.form-item { padding: 32rpx 36rpx; border-bottom: 1rpx solid var(--outline-light); }
.form-item:last-child { border-bottom: none; }
.form-label { font-size: 28rpx; font-weight: 600; color: var(--text-primary); display: block; margin-bottom: 16rpx; }
.form-input { width: 100%; height: 88rpx; background: var(--surface); border-radius: 16rpx; padding: 0 24rpx; font-size: 28rpx; color: var(--text-primary); box-sizing: border-box; }


.cert-notice { display: flex; align-items: center; justify-content: center; gap: 8rpx; margin-top: 24rpx; padding: 20rpx; }
.cert-notice text { font-size: 24rpx; color: var(--text-tertiary); }

.submit-btn { width: 100%; height: 96rpx; background: var(--primary); border-radius: 48rpx; display: flex; align-items: center; justify-content: center; margin-top: 24rpx; box-shadow: var(--shadow-sm); }
.submit-btn:active { transform: scale(0.98); background: var(--primary-dark); }
.submit-btn--disabled { background: var(--text-tertiary); box-shadow: none; pointer-events: none; }
.submit-btn text { font-size: 30rpx; font-weight: 600; color: #fff; }

/* Status cards */
.status-card { background: var(--surface-raised); border-radius: 32rpx; padding: 56rpx 36rpx 40rpx; margin-top: 16rpx; box-shadow: var(--shadow-sm); display: flex; flex-direction: column; align-items: center; text-align: center; }
.status-icon { width: 120rpx; height: 120rpx; border-radius: 50%; display: flex; align-items: center; justify-content: center; margin-bottom: 24rpx; }
.status-icon--pending { background: #fff7ed; }
.status-icon--approved { background: #f0fdf4; }
.status-icon--rejected { background: #fef2f2; }
.status-title { font-size: 36rpx; font-weight: 700; color: var(--text-primary); margin-bottom: 12rpx; }
.status-desc { font-size: 26rpx; color: var(--text-secondary); line-height: 1.6; margin-bottom: 24rpx; max-width: 480rpx; }

.status-info { width: 100%; background: var(--surface); border-radius: 20rpx; padding: 24rpx 28rpx; margin-bottom: 16rpx; }
.info-row { display: flex; justify-content: space-between; align-items: center; padding: 14rpx 0; }
.info-row + .info-row { border-top: 1rpx solid var(--outline-light); }
.info-label { font-size: 26rpx; color: var(--text-tertiary); }
.info-value { font-size: 26rpx; font-weight: 500; color: var(--text-primary); }
.info-value--link { color: var(--primary); }
.info-row--img:active { opacity: 0.6; }

.status-notice { width: 100%; background: var(--primary-container); border-radius: 16rpx; padding: 20rpx 24rpx; }
.status-notice text { font-size: 24rpx; color: var(--primary); line-height: 1.6; }

.go-hall-btn { display: flex; align-items: center; justify-content: center; gap: 8rpx; padding: 28rpx; margin-top: 24rpx; background: var(--surface-raised); border-radius: 48rpx; box-shadow: var(--shadow-sm); }
.go-hall-btn:active { background: var(--primary-container); }
.go-hall-btn text { font-size: 28rpx; font-weight: 500; color: var(--primary); }

.bottom-placeholder { height: 120rpx; }
</style>
