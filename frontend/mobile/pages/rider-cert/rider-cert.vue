<template>
  <view class="page">
    <uni-nav-bar backgroundColor="#FAFAF8" :border="false" statusBar>
      <template v-slot:left>
        <view class="nav-back" @click="onBack">
          <iconpark-icon name="left" size="22" color="#FF6B4A" />
        </view>
      </template>
      <view class="nav-title-wrap"><text class="nav-title-text">配送员认证</text></view>
      <template v-slot:right><view></view></template>
    </uni-nav-bar>

    <scroll-view class="main-scroll" :style="{ height: scrollHeight + 'px' }" scroll-y enhanced :show-scrollbar="false">
      <!-- Step 0: 用户未完成实名认证 → 引导先去认证 -->
      <template v-if="!store.isCertified">
        <view class="status-card">
          <view class="status-icon status-icon--warn">
            <iconpark-icon name="locked-filled" size="48" color="#F59E0B" />
          </view>
          <text class="status-title">请先完成学生实名认证</text>
          <text class="status-desc">申请成为配送员前，需要先通过学生身份认证</text>
        </view>

        <view class="info-card">
          <view class="info-row">
            <text class="info-label">实名状态</text>
            <text class="info-value info-value--warn">{{ store.certifyStatusLabel }}</text>
          </view>
        </view>

        <view class="submit-btn" @click="goCertify">
          <text>去完成实名认证</text>
        </view>
      </template>

      <!-- Step 1: 已认证 → 根据 verifyStatus 展示 -->
      <template v-else>
        <!-- verifyStatus === 0: 未申请 → 展示认证信息 + 申请按钮 -->
        <template v-if="runnerState === 'none'">
          <view class="status-card">
            <view class="status-icon status-icon--info">
              <iconpark-icon name="deliveryTruck" size="48" color="#FF6B4A" />
            </view>
            <text class="status-title">申请成为配送员</text>
            <text class="status-desc">成为配送员后可在任务大厅接单，赚取报酬</text>
          </view>

          <view class="info-card">
            <view class="info-row">
              <text class="info-label">姓名</text>
              <text class="info-value">{{ store.userInfo.realName || '—' }}</text>
            </view>
            <view class="info-row">
              <text class="info-label">学号</text>
              <text class="info-value">{{ store.userInfo.studentId || '—' }}</text>
            </view>
            <view class="info-row">
              <text class="info-label">实名状态</text>
              <text class="info-value info-value--done">{{ store.certifyStatusLabel }}</text>
            </view>
          </view>

          <view class="notice-card">
            <iconpark-icon name="info-filled" size="16" color="#FF6B4A" />
            <text>申请提交后由管理员审核，通常1-3个工作日内完成</text>
          </view>

          <view class="submit-btn" :class="{ 'submit-btn--disabled': applying }" @click="onApply">
            <text>{{ applying ? '提交中…' : '提交申请' }}</text>
          </view>
        </template>

        <!-- verifyStatus === 1: 审核中 -->
        <template v-else-if="runnerState === 'pending'">
          <view class="status-card">
            <view class="status-icon status-icon--pending">
              <iconpark-icon name="clock-filled" size="48" color="#F59E0B" />
            </view>
            <text class="status-title">配送员申请审核中</text>
            <text class="status-desc">你的配送员申请正在审核中，通常1-3个工作日内完成，请留意通知</text>
          </view>

          <view class="info-card">
            <view class="info-row">
              <text class="info-label">姓名</text>
              <text class="info-value">{{ store.userInfo.realName || '—' }}</text>
            </view>
            <view class="info-row">
              <text class="info-label">学号</text>
              <text class="info-value">{{ store.userInfo.studentId || '—' }}</text>
            </view>
            <view class="info-row">
              <text class="info-label">申请状态</text>
              <text class="info-value info-value--pending">审核中</text>
            </view>
          </view>
        </template>

        <!-- verifyStatus === 2: 已通过 -->
        <template v-else-if="runnerState === 'approved'">
          <view class="status-card">
            <view class="status-icon status-icon--done">
              <iconpark-icon name="checkbox-filled" size="48" color="#34d399" />
            </view>
            <text class="status-title">配送员认证已通过</text>
            <text class="status-desc">你已是认证配送员，可在任务大厅接单</text>
          </view>

          <view class="info-card">
            <view class="info-row">
              <text class="info-label">姓名</text>
              <text class="info-value">{{ store.userInfo.realName || '—' }}</text>
            </view>
            <view class="info-row">
              <text class="info-label">学号</text>
              <text class="info-value">{{ store.userInfo.studentId || '—' }}</text>
            </view>
            <view class="info-row">
              <text class="info-label">认证状态</text>
              <text class="info-value info-value--done">已通过</text>
            </view>
          </view>

          <view class="go-hall-btn" @click="goHall">
            <text>前往任务大厅接单</text>
            <iconpark-icon name="right" size="16" color="#FF6B4A" />
          </view>
        </template>

        <!-- verifyStatus === 3: 驳回 -->
        <template v-else-if="runnerState === 'rejected'">
          <view class="status-card">
            <view class="status-icon status-icon--reject">
              <iconpark-icon name="closeempty" size="48" color="#ba1a1a" />
            </view>
            <text class="status-title">申请已驳回</text>
            <text class="status-desc">{{ runnerRemark || '你的配送员申请未通过审核，可重新提交' }}</text>
          </view>

          <view class="info-card">
            <view class="info-row">
              <text class="info-label">姓名</text>
              <text class="info-value">{{ store.userInfo.realName || '—' }}</text>
            </view>
            <view class="info-row">
              <text class="info-label">学号</text>
              <text class="info-value">{{ store.userInfo.studentId || '—' }}</text>
            </view>
          </view>

          <view class="submit-btn" @click="onReapply">
            <text>重新提交申请</text>
          </view>
        </template>
      </template>

      <view class="bottom-placeholder"></view>
    </scroll-view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useStore } from '@/store/index.js'
import { runnerApi } from '@/api'

const store = useStore()
const sysInfo = uni.getSystemInfoSync()
const scrollHeight = sysInfo.windowHeight - sysInfo.statusBarHeight - 44

const applying = ref(false)
const runnerRemark = ref('')

const runnerState = computed(() => {
  const vs = store.userInfo.verifyStatus
  if (vs === 1) return 'pending'
  if (vs === 2) return 'approved'
  if (vs === 3) return 'rejected'
  return 'none'
})

onShow(() => {
  // 刷新用户信息以获取最新 verifyStatus
  if (store.isLoggedIn) {
    store.fetchUserInfo().catch(() => {})
  }
  // 尝试获取配送员档案（仅在已认证时）
  if (store.isCertified && store.userInfo.verifyStatus > 0) {
    loadRunnerInfo()
  }
})

async function loadRunnerInfo() {
  try {
    const profile = await runnerApi.getRunnerProfile({ showLoading: false })
    if (profile.verifyRemark) runnerRemark.value = profile.verifyRemark
    store.userInfo.verifyStatus = profile.verifyStatus
  } catch (e) { /* 非认证用户或未申请时忽略 */ }
}

async function onApply() {
  if (applying.value) return
  applying.value = true
  try {
    await runnerApi.applyRunner()
    store.userInfo.verifyStatus = 1
    uni.showToast({ title: '申请已提交', icon: 'success' })
  } catch (e) { /* handled */ }
  applying.value = false
}

function onReapply() {
  runnerRemark.value = ''
  onApply()
}

function goCertify() {
  uni.navigateTo({ url: '/pages/certify/certify' })
}

function goHall() {
  uni.switchTab({ url: '/pages/task-hall/task-hall' })
}

function onBack() {
  uni.navigateBack({ delta: 1, fail: () => uni.switchTab({ url: '/pages/userprofile/userprofile' }) })
}
</script>

<style scoped>
.page{width:100%;height:100vh;display:flex;flex-direction:column;background:var(--background);overflow:hidden}

.nav-back{width:68rpx;height:68rpx;display:flex;align-items:center;justify-content:center;border-radius:50%}
.nav-back:active{background:var(--surface-hover)}
.nav-title-wrap{display:flex;align-items:center;justify-content:center}
.nav-title-text{font-size:34rpx;font-weight:600;color:var(--text-primary)}

.main-scroll{box-sizing:border-box;width:100%;padding:0 32rpx;padding-bottom:120rpx}

/* Status cards */
.status-card{background:var(--surface-raised);border-radius:32rpx;padding:56rpx 36rpx 40rpx;margin-top:16rpx;display:flex;flex-direction:column;align-items:center;text-align:center;box-shadow:var(--shadow-sm)}
.status-icon{width:120rpx;height:120rpx;border-radius:50%;display:flex;align-items:center;justify-content:center;margin-bottom:24rpx}
.status-icon--info{background:var(--primary-container)}
.status-icon--warn{background:#fff7ed}
.status-icon--pending{background:#fff7ed}
.status-icon--done{background:#f0fdf4}
.status-icon--reject{background:#fef2f2}
.status-title{font-size:36rpx;font-weight:700;color:var(--text-primary);margin-bottom:12rpx}
.status-desc{font-size:26rpx;color:var(--text-secondary);line-height:1.6;max-width:480rpx}

/* Info card */
.info-card{background:var(--surface-raised);border-radius:var(--radius-lg);margin-top:24rpx;overflow:hidden;box-shadow:var(--shadow-sm)}
.info-row{display:flex;justify-content:space-between;align-items:center;padding:28rpx 32rpx;border-bottom:1rpx solid var(--outline-light)}
.info-row:last-child{border-bottom:none}
.info-label{font-size:28rpx;color:var(--text-tertiary)}
.info-value{font-size:28rpx;font-weight:500;color:var(--text-primary)}
.info-value--done{color:#34d399}
.info-value--pending{color:var(--warning)}
.info-value--warn{color:var(--warning)}

/* Notice */
.notice-card{display:flex;align-items:flex-start;gap:10rpx;background:var(--primary-container);border-radius:16rpx;padding:20rpx 24rpx;margin-top:24rpx}
.notice-card text{font-size:24rpx;color:var(--primary);line-height:1.5;flex:1}

/* Buttons */
.submit-btn{width:100%;height:96rpx;background:var(--primary);border-radius:48rpx;display:flex;align-items:center;justify-content:center;margin-top:32rpx;box-shadow:var(--shadow-sm)}
.submit-btn:active{transform:scale(.98);background:var(--primary-dark)}
.submit-btn--disabled{background:var(--text-tertiary);box-shadow:none;pointer-events:none}
.submit-btn text{font-size:30rpx;font-weight:600;color:#fff}

.go-hall-btn{display:flex;align-items:center;justify-content:center;gap:8rpx;padding:28rpx;margin-top:24rpx;background:var(--surface-raised);border-radius:48rpx;box-shadow:var(--shadow-sm)}
.go-hall-btn:active{background:var(--primary-container)}
.go-hall-btn text{font-size:28rpx;font-weight:500;color:var(--primary)}

.bottom-placeholder{height:120rpx}
</style>
