<template>
  <view class="page">
    <uni-nav-bar title="我的评价" backgroundColor="#FAFAF8" :border="false" statusBar fixed leftIcon="left" @clickLeft="onBack" color="#1C1B1A" />

    <scroll-view class="main-scroll" :style="{ height: scrollHeight + 'px' }" scroll-y enhanced :show-scrollbar="false">
      <!-- 评分头部 -->
      <view v-if="!loading" class="rating-header">
        <view class="rating-big">
          <text class="rating-number">{{ avgRating }}</text>
          <text class="rating-unit">/ 5.0</text>
        </view>
        <view class="rating-stars">
          <uni-rate :value="Math.round(avgRating)" :max="5" size="28" activeColor="#fbbf24" :isFill="true" :readonly="true" />
        </view>
        <text class="rating-count">共 {{ totalReviews }} 条评价</text>
      </view>

      <!-- 加载中 -->
      <view v-if="loading" class="loading-state">
        <text class="loading-text">加载中…</text>
      </view>

      <!-- 空状态 -->
      <view v-else-if="reviews.length === 0" class="empty-state">
        <iconpark-icon name="star" size="48" color="#D4D2CC" />
        <text class="empty-text">暂无评价</text>
        <text class="empty-sub">完成更多订单后，用户会为您留下评价</text>
      </view>

      <!-- 评价列表 -->
      <view v-for="review in reviews" :key="review.reviewId" class="review-card">
        <view class="review-header">
          <view class="reviewer-avatar">
            <image v-if="review.reviewerAvatar" class="avatar-img" :src="normalizeUrl(review.reviewerAvatar)" mode="aspectFill" />
            <text v-else class="avatar-text">{{ review.reviewerInitial }}</text>
          </view>
          <view class="reviewer-info">
            <text class="reviewer-name">{{ review.reviewerNickname }}</text>
            <text class="review-time">{{ formatTime(review.createdAt) }}</text>
          </view>
          <uni-rate :value="review.rating || 0" :max="5" size="18" activeColor="#fbbf24" :isFill="true" :readonly="true" />
        </view>

        <view v-if="review.content" class="review-content">{{ review.content }}</view>

        <view v-if="review.tags && review.tags.length" class="review-tags">
          <text v-for="t in review.tags" :key="t" class="review-tag">{{ t }}</text>
        </view>

        <!-- 追加评价（问答式嵌套展示） -->
        <view v-if="review.followUps && review.followUps.length" class="followups">
          <view v-for="fu in flattenThread(review)" :key="fu.reviewId" class="followup-item" :style="{ marginLeft: fu._depth * 40 + 'rpx' }">
            <view class="followup-header">
              <text class="followup-author">{{ fu.reviewerNickname || '对方' }}</text>
              <text class="followup-time">{{ formatTime(fu.createdAt) }}</text>
            </view>
            <text class="followup-text">{{ fu.content }}</text>
          </view>
        </view>

        <!-- 追评回复 -->
        <view v-if="replyingTo === review.reviewId" class="reply-box">
          <textarea class="reply-input" v-model="followUpText" placeholder="输入追评内容…" :maxlength="500" />
          <view class="reply-actions">
            <text class="reply-cancel" @click="cancelReply">取消</text>
            <text class="reply-submit" :class="{ 'reply-submit--disabled': !followUpText.trim() || submittingFollowUp }" @click="submitFollowUp(review.reviewId)">发送</text>
          </view>
        </view>
        <view v-else class="reply-btn" @click="startReply(review.reviewId)">
          <text>回复</text>
        </view>
      </view>

      <view class="bottom-placeholder"></view>
    </scroll-view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useStore } from '@/store/index.js'
import { reviewApi, runnerApi } from '@/api'
import { SERVER_ORIGIN } from '@/utils/config'

function normalizeUrl(url) {
  if (!url) return ''
  if (url.startsWith('http')) return url
  if (url.startsWith('/')) return SERVER_ORIGIN + url
  return url
}

const store = useStore()
const sysInfo = uni.getSystemInfoSync()
const scrollHeight = sysInfo.windowHeight - sysInfo.statusBarHeight - 44

const loading = ref(true)
const reviews = ref([])
const avgRating = ref('0.0')
const totalReviews = ref(0)
const replyingTo = ref(null)
const followUpText = ref('')
const submittingFollowUp = ref(false)

onShow(async () => {
  await loadData()
})

async function loadData() {
  loading.value = true
  try {
    const [profile, reviewList] = await Promise.all([
      runnerApi.getRunnerProfile({ showLoading: false }),
      reviewApi.getUserReviews(store.userId)
    ])
    avgRating.value = profile.avgRating ? Number(profile.avgRating).toFixed(1) : '0.0'
    reviews.value = (reviewList || []).map(normalizeReview)
    totalReviews.value = reviews.value.length
  } catch (e) { /* handled */ }
  loading.value = false
}

function normalizeReview(raw) {
  return {
    ...raw,
    reviewerInitial: (raw.reviewerNickname || '用').charAt(0)
  }
}

/**
 * 将嵌套追评树展平为带深度标记的列表
 */
function flattenThread(root) {
  const items = []
  function walk(review, depth) {
    if (review.followUps && review.followUps.length) {
      review.followUps.forEach(f => {
        items.push({ ...f, _depth: depth })
        walk(f, depth + 1)
      })
    }
  }
  walk(root, 1)
  return items
}

function startReply(reviewId) {
  replyingTo.value = reviewId
  followUpText.value = ''
}

function cancelReply() {
  replyingTo.value = null
  followUpText.value = ''
}

async function submitFollowUp(reviewId) {
  const content = followUpText.value.trim()
  if (!content || submittingFollowUp.value) return
  submittingFollowUp.value = true
  try {
    await reviewApi.followUpReview(reviewId, content)
    uni.showToast({ title: '追评成功', icon: 'success' })
    cancelReply()
    await loadData()
  } catch (e) { /* handled */ }
  submittingFollowUp.value = false
}

function formatTime(timeStr) {
  if (!timeStr) return ''
  const now = new Date()
  const time = new Date(timeStr.replace(/-/g, '/'))
  const diff = now - time
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
  if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前'
  return (timeStr.split(' ')[0] || '').slice(5)
}

function onBack() { uni.navigateBack() }
</script>

<style scoped>
.page{width:100%;height:100vh;display:flex;flex-direction:column;background:var(--background);overflow:hidden}
.main-scroll{box-sizing:border-box;width:100%;padding:0 32rpx;padding-bottom:60rpx}

/* 评分头部 */
.rating-header{display:flex;flex-direction:column;align-items:center;padding:40rpx 0 24rpx}
.rating-big{display:flex;align-items:baseline;gap:8rpx;margin-bottom:12rpx}
.rating-number{font-size:64rpx;font-weight:700;color:var(--text-primary);line-height:1}
.rating-unit{font-size:28rpx;color:var(--text-secondary)}
.rating-stars{margin-bottom:8rpx}
.rating-count{font-size:24rpx;color:var(--text-tertiary)}

/* 评价卡片 */
.review-card{background:var(--surface-raised);border-radius:20rpx;padding:28rpx;margin-bottom:16rpx;box-shadow:var(--shadow-sm)}
.review-header{display:flex;align-items:center;gap:16rpx;margin-bottom:14rpx}
.reviewer-avatar{width:64rpx;height:64rpx;border-radius:50%;overflow:hidden;flex-shrink:0}
.avatar-img{width:100%;height:100%;object-fit:cover}
.avatar-text{width:100%;height:100%;display:flex;align-items:center;justify-content:center;font-size:26rpx;font-weight:600;color:#fff;background:linear-gradient(135deg,#4facfe,#43e97b)}
.reviewer-info{flex:1;min-width:0}
.reviewer-name{font-size:26rpx;font-weight:500;color:var(--text-primary);display:block}
.review-time{font-size:22rpx;color:var(--text-tertiary);margin-top:2rpx;display:block}

.review-content{font-size:26rpx;color:var(--text-secondary);line-height:1.6;margin-bottom:12rpx}

.review-tags{display:flex;flex-wrap:wrap;gap:8rpx;margin-bottom:12rpx}
.review-tag{padding:6rpx 16rpx;background:var(--surface);border-radius:16rpx;font-size:22rpx;color:var(--text-secondary)}

/* 追加评价 */
.followups{margin-top:16rpx;padding-top:16rpx;border-top:1rpx solid var(--outline-light)}
.followup-item{display:flex;flex-direction:column;gap:4rpx;padding:12rpx 16rpx;background:var(--surface);border-radius:14rpx;margin-bottom:8rpx}
.followup-item:last-child{margin-bottom:0}
.followup-header{display:flex;align-items:center;gap:10rpx}
.followup-author{font-size:22rpx;font-weight:600;color:var(--primary)}
.followup-text{font-size:24rpx;color:var(--text-primary);line-height:1.5;word-break:break-all}
.followup-time{font-size:20rpx;color:var(--text-tertiary)}

/* 追评回复 */
.reply-btn{margin-top:16rpx;padding-top:16rpx;border-top:1rpx solid var(--outline-light);text-align:right}
.reply-btn text{font-size:24rpx;color:var(--primary);padding:8rpx 20rpx}
.reply-box{margin-top:16rpx;padding-top:16rpx;border-top:1rpx solid var(--outline-light)}
.reply-input{width:100%;height:120rpx;padding:16rpx;background:var(--surface);border-radius:12rpx;font-size:24rpx;color:var(--text-primary);box-sizing:border-box}
.reply-actions{display:flex;justify-content:flex-end;gap:24rpx;margin-top:12rpx}
.reply-cancel{font-size:24rpx;color:var(--text-tertiary);padding:8rpx 20rpx}
.reply-submit{font-size:24rpx;color:var(--primary);font-weight:500;padding:8rpx 20rpx}
.reply-submit--disabled{opacity:.4}

.loading-state,.empty-state{display:flex;flex-direction:column;align-items:center;padding:120rpx 0;gap:12rpx;opacity:.5}
.loading-text,.empty-text{font-size:28rpx;color:var(--text-tertiary)}
.empty-sub{font-size:24rpx;color:var(--text-tertiary);text-align:center;max-width:400rpx}
.bottom-placeholder{height:60rpx}
</style>
