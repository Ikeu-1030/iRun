<template>
  <view class="page">
    <uni-nav-bar title="骑手主页" backgroundColor="#FAFAF8" :border="false" statusBar fixed leftIcon="left" @clickLeft="onBack" color="#1C1B1A" />

    <scroll-view class="main-scroll" :style="{ height: scrollHeight + 'px' }" scroll-y enhanced :show-scrollbar="false">
      <view v-if="loading" class="loading-state"><text class="loading-text">加载中…</text></view>

      <template v-else>
        <!-- 骑手信息卡片 -->
        <view class="profile-card animate-scale-pop">
          <view class="profile-avatar-wrap">
            <image v-if="info.avatarUrl" class="profile-avatar" :src="normalizeUrl(info.avatarUrl)" mode="aspectFill" />
            <view v-else class="profile-avatar profile-avatar--placeholder">{{ initial }}</view>
          </view>
          <view class="profile-info">
            <view class="profile-name-row">
              <text class="profile-name">{{ info.nickname || '骑手' }}</text>
              <text v-if="genderSymbol" class="profile-gender" :style="{ color: genderColor, background: genderBg }">{{ genderSymbol }}</text>
            </view>
            <text class="profile-stat">信用分 {{ info.creditScore || 100 }} · 接单 {{ info.totalOrders || 0 }} · 完成 {{ info.successOrders || 0 }}</text>
            <text class="profile-rate" v-if="info.completionRate">完成率 {{ Number(info.completionRate).toFixed(1) }}%</text>
          </view>
        </view>

        <!-- 评分概览 -->
        <view class="rating-overview">
          <view class="rating-big">{{ avgRating }}</view>
          <view class="rating-stars">
            <iconpark-icon v-for="i in 5" :key="i" name="star-filled" size="20" :color="i <= Math.round(avgRatingNum) ? '#fbbf24' : '#e5e7eb'" />
          </view>
          <text class="rating-count">{{ reviews.length }} 条评价</text>
        </view>

        <!-- 评价列表 -->
        <view class="section-title">全部评价</view>
        <view v-if="reviews.length === 0" class="empty-state">
          <iconpark-icon name="info-filled" size="36" color="#D4D2CC" />
          <text class="empty-text">暂无评价</text>
        </view>
        <view v-for="(r, ri) in reviews" :key="r.reviewId" class="review-card animate-fade-up" :style="{ animationDelay: (ri * 0.08) + 's' }" @click="openReviewDetail(r)">
          <view class="review-header">
            <image v-if="r.reviewerAvatar" class="review-avatar" :src="normalizeUrl(r.reviewerAvatar)" mode="aspectFill" />
            <view v-else class="review-avatar review-avatar--txt">{{ (r.reviewerNickname || '匿').charAt(0) }}</view>
            <view class="review-meta">
              <text class="review-name">{{ r.reviewerNickname || '匿名' }}</text>
              <text class="review-time">{{ formatTime(r.createdAt) }}</text>
            </view>
            <view class="review-stars">
              <iconpark-icon v-if="r.rating" v-for="i in 5" :key="i" name="star-filled" size="14" :color="i <= r.rating ? '#fbbf24' : '#e5e7eb'" />
            </view>
          </view>
          <text v-if="r.content" class="review-content">{{ r.content }}</text>
          <view v-if="r.tags && r.tags.length" class="review-tags">
            <text v-for="t in r.tags" :key="t" class="review-tag">{{ t }}</text>
          </view>
          <!-- 追评 -->
          <view v-if="r.followUps && r.followUps.length" class="followup-list">
            <view v-for="f in r.followUps" :key="f.reviewId" class="followup-item">
              <text class="followup-text">追评：{{ f.content }}</text>
            </view>
          </view>
        </view>
      </template>
      <view class="bottom-placeholder"></view>
    </scroll-view>

    <!-- 评价详情弹窗 -->
    <view v-if="showDetail && selectedReview" class="detail-overlay" @click="closeDetail">
      <view class="detail-sheet" @click.stop>
        <view class="detail-header">
          <text class="detail-title">评价详情</text>
          <view class="detail-close" @click="closeDetail">
            <iconpark-icon name="close" size="22" color="#8F8D88" />
          </view>
        </view>
        <scroll-view class="detail-body" scroll-y>
          <view class="detail-reviewer">
            <image v-if="selectedReview.reviewerAvatar" class="detail-avatar" :src="normalizeUrl(selectedReview.reviewerAvatar)" mode="aspectFill" />
            <view v-else class="detail-avatar detail-avatar--txt">{{ (selectedReview.reviewerNickname || '匿').charAt(0) }}</view>
            <view class="detail-reviewer-info">
              <text class="detail-name">{{ selectedReview.reviewerNickname || '匿名' }}</text>
              <text class="detail-time">{{ formatTime(selectedReview.createdAt) }}</text>
            </view>
          </view>
          <view class="detail-stars" v-if="selectedReview.rating">
            <iconpark-icon v-for="i in 5" :key="i" name="star-filled" size="24" :color="i <= selectedReview.rating ? '#fbbf24' : '#e5e7eb'" />
          </view>
          <text v-if="selectedReview.content" class="detail-content">{{ selectedReview.content }}</text>
          <view v-if="selectedReview.tags && selectedReview.tags.length" class="detail-tags">
            <text v-for="t in selectedReview.tags" :key="t" class="detail-tag">{{ t }}</text>
          </view>
          <view v-if="selectedReview.followUps && selectedReview.followUps.length" class="detail-followups">
            <view class="detail-followup-title">追评</view>
            <view v-for="f in selectedReview.followUps" :key="f.reviewId" class="detail-followup-item">
              <view class="detail-followup-header">
                <image v-if="f.reviewerAvatar" class="detail-fu-avatar" :src="normalizeUrl(f.reviewerAvatar)" mode="aspectFill" />
                <view v-else class="detail-fu-avatar detail-fu-avatar--txt">{{ (f.reviewerNickname || '匿').charAt(0) }}</view>
                <text class="detail-fu-name">{{ f.reviewerNickname || '匿名' }}</text>
                <text class="detail-fu-time">{{ formatTime(f.createdAt) }}</text>
              </view>
              <text class="detail-fu-content">{{ f.content }}</text>
            </view>
          </view>
        </scroll-view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { reviewApi, runnerApi } from '@/api'
import { SERVER_ORIGIN } from '@/utils/config'

function normalizeUrl(url) {
  if (!url) return ''
  if (url.startsWith('http')) return url
  if (url.startsWith('/')) return SERVER_ORIGIN + url
  return url
}

const sysInfo = uni.getSystemInfoSync()
const scrollHeight = sysInfo.windowHeight - sysInfo.statusBarHeight - 44

const runnerId = ref('')
const loading = ref(true)
const info = ref({})
const reviews = ref([])
const selectedReview = ref(null)
const showDetail = ref(false)

const avgRating = computed(() => info.value.avgRating ? Number(info.value.avgRating).toFixed(1) : '5.0')
const avgRatingNum = computed(() => Number(avgRating.value))
const initial = computed(() => (info.value.nickname || '骑').charAt(0))
const genderSymbol = computed(() => {
  if (info.value.sex === '男') return '♂'
  if (info.value.sex === '女') return '♀'
  return ''
})
const genderColor = computed(() => info.value.sex === '男' ? '#3b82f6' : '#ec4899')
const genderBg = computed(() => info.value.sex === '男' ? '#eff6ff' : '#fdf2f8')

onLoad((options) => {
  if (options?.runnerId) runnerId.value = options.runnerId
  loadData()
})

async function loadData() {
  loading.value = true
  try {
    const rid = runnerId.value
    const [perf, revs] = await Promise.all([
      runnerApi.getPerformance(rid, { showLoading: false }),
      reviewApi.getUserReviews(rid, { showLoading: false })
    ])
    info.value = perf || {}
    reviews.value = revs || []
  } catch (e) { /* handled */ }
  loading.value = false
}

function openReviewDetail(r) {
  selectedReview.value = r
  showDetail.value = true
}

function closeDetail() {
  showDetail.value = false
  selectedReview.value = null
}

function formatTime(t) {
  if (!t) return ''
  return t.replace('T', ' ').slice(0, 16)
}

function onBack() { uni.navigateBack() }
</script>

<style scoped>
.page{width:100%;height:100vh;display:flex;flex-direction:column;background:var(--background);overflow:hidden}
.main-scroll{box-sizing:border-box;width:100%;padding:0 32rpx}
.section-title{font-size:24rpx;font-weight:500;color:var(--text-tertiary);letter-spacing:2rpx;margin-top:36rpx;margin-bottom:16rpx;padding-left:8rpx}

.profile-card{display:flex;align-items:center;gap:20rpx;background:var(--surface-raised);border-radius:20rpx;padding:28rpx;margin-top:16rpx;box-shadow:var(--shadow-sm)}
.profile-avatar-wrap{flex-shrink:0}
.profile-avatar{width:96rpx;height:96rpx;border-radius:50%}
.profile-avatar--placeholder{background:var(--primary-container);color:var(--primary);display:flex;align-items:center;justify-content:center;font-size:36rpx;font-weight:700}
.profile-info{flex:1;min-width:0}
.profile-name-row{display:flex;align-items:center;gap:10rpx}
.profile-name{font-size:32rpx;font-weight:600;color:var(--text-primary)}
.profile-gender{font-size:26rpx;border-radius:50%;width:44rpx;height:44rpx;display:flex;align-items:center;justify-content:center}
.profile-stat{font-size:24rpx;color:var(--text-secondary);margin-top:6rpx;display:block}
.profile-rate{font-size:22rpx;color:var(--primary);margin-top:4rpx;display:block}

.rating-overview{display:flex;align-items:center;gap:16rpx;background:var(--surface-raised);border-radius:20rpx;padding:28rpx;margin-top:16rpx;box-shadow:var(--shadow-sm)}
.rating-big{font-size:52rpx;font-weight:700;color:var(--text-primary)}
.rating-stars{display:flex;gap:4rpx}
.rating-count{font-size:24rpx;color:var(--text-secondary);margin-left:auto}

.empty-state{display:flex;flex-direction:column;align-items:center;padding:60rpx 0;gap:12rpx}
.empty-text{font-size:26rpx;color:var(--text-tertiary)}

.review-card{background:var(--surface-raised);border-radius:20rpx;padding:24rpx;margin-bottom:16rpx;box-shadow:var(--shadow-sm)}
.review-header{display:flex;align-items:center;gap:12rpx}
.review-avatar{width:56rpx;height:56rpx;border-radius:50%}
.review-avatar--txt{background:var(--primary-container);color:var(--primary);display:flex;align-items:center;justify-content:center;font-size:24rpx;font-weight:600}
.review-meta{flex:1}
.review-name{font-size:26rpx;font-weight:500;color:var(--text-primary);display:block}
.review-time{font-size:22rpx;color:var(--text-tertiary);margin-top:2rpx;display:block}
.review-stars{display:flex;gap:2rpx}
.review-content{font-size:26rpx;color:var(--text-primary);margin-top:14rpx;line-height:1.5}
.review-tags{display:flex;flex-wrap:wrap;gap:8rpx;margin-top:12rpx}
.review-tag{font-size:22rpx;padding:4rpx 14rpx;background:var(--primary-container);color:var(--primary);border-radius:8rpx}

.followup-list{margin-top:14rpx;padding-top:14rpx;border-top:1rpx solid var(--surface-hover)}
.followup-item{display:block;padding:4rpx 0}
.followup-text{font-size:24rpx;color:var(--text-secondary);line-height:1.5;word-break:break-all}

.loading-state{display:flex;align-items:center;justify-content:center;padding:120rpx 0}
.loading-text{font-size:28rpx;color:var(--text-tertiary)}
.bottom-placeholder{height:60rpx}

/* 评价详情弹窗 */
.detail-overlay{position:fixed;top:0;left:0;right:0;bottom:0;background:rgba(0,0,0,.45);z-index:999;display:flex;align-items:flex-end;justify-content:center;animation:fadeIn .25s var(--easing-out)}
@keyframes fadeIn{from{opacity:0}to{opacity:1}}
.detail-sheet{width:100%;max-height:70vh;background:var(--surface);border-radius:28rpx 28rpx 0 0;display:flex;flex-direction:column;overflow:hidden;animation:slideUp .35s var(--ease-spring)}
@keyframes slideUp{from{transform:translateY(100%)}to{transform:translateY(0)}}
.detail-header{display:flex;align-items:center;justify-content:space-between;padding:28rpx 32rpx 16rpx;flex-shrink:0}
.detail-title{font-size:30rpx;font-weight:600;color:var(--text-primary)}
.detail-close{width:48rpx;height:48rpx;display:flex;align-items:center;justify-content:center}
.detail-body{flex:1;padding:0 32rpx 40rpx;max-height:60vh}
.detail-reviewer{display:flex;align-items:center;gap:16rpx;margin-bottom:20rpx}
.detail-avatar{width:72rpx;height:72rpx;border-radius:50%}
.detail-avatar--txt{background:var(--primary-container);color:var(--primary);display:flex;align-items:center;justify-content:center;font-size:32rpx;font-weight:600}
.detail-reviewer-info{flex:1}
.detail-name{font-size:28rpx;font-weight:500;color:var(--text-primary);display:block}
.detail-time{font-size:22rpx;color:var(--text-tertiary);margin-top:2rpx;display:block}
.detail-stars{display:flex;gap:6rpx;margin-bottom:20rpx}
.detail-content{font-size:28rpx;color:var(--text-primary);line-height:1.6}
.detail-tags{display:flex;flex-wrap:wrap;gap:10rpx;margin-top:16rpx}
.detail-tag{font-size:24rpx;padding:6rpx 16rpx;background:var(--primary-container);color:var(--primary);border-radius:10rpx}
.detail-followups{margin-top:28rpx;padding-top:24rpx;border-top:1rpx solid var(--surface-hover)}
.detail-followup-title{font-size:24rpx;font-weight:500;color:var(--text-secondary);margin-bottom:16rpx}
.detail-followup-item{background:var(--surface-raised);border-radius:14rpx;padding:20rpx;margin-bottom:12rpx}
.detail-followup-header{display:flex;align-items:center;gap:10rpx;margin-bottom:10rpx}
.detail-fu-avatar{width:40rpx;height:40rpx;border-radius:50%}
.detail-fu-avatar--txt{background:var(--primary-container);color:var(--primary);display:flex;align-items:center;justify-content:center;font-size:20rpx;font-weight:600}
.detail-fu-name{font-size:24rpx;font-weight:500;color:var(--text-primary)}
.detail-fu-time{font-size:20rpx;color:var(--text-tertiary);margin-left:auto}
.detail-fu-content{font-size:26rpx;color:var(--text-primary);line-height:1.5}
</style>
