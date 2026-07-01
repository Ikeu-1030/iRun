<template>
  <view class="page">
    <uni-nav-bar title="我的账单" backgroundColor="#FAFAF8" :border="false" statusBar fixed leftIcon="left" @clickLeft="onBack" color="#1C1B1A" />

    <scroll-view class="main-scroll" :style="{ height: scrollHeight + 'px' }" scroll-y enhanced :show-scrollbar="false" @scrolltolower="loadMore">
      <!-- 月度汇总 -->
      <view class="summary-card">
        <view class="summary-row">
          <view class="summary-item">
            <text class="summary-label">总支出</text>
            <text class="summary-value summary-value--expense">¥ {{ totalExpense.toFixed(2) }}</text>
          </view>
          <view class="summary-divider"></view>
          <view class="summary-item">
            <text class="summary-label">总收入</text>
            <text class="summary-value summary-value--income">¥ {{ totalIncome.toFixed(2) }}</text>
          </view>
        </view>
      </view>

      <!-- 分类筛选 -->
      <view class="filter-row">
        <view v-for="f in filters" :key="f.key" class="filter-chip" :class="{ 'filter-chip--active': activeFilter === f.key }" @click="onFilter(f.key)">
          <text>{{ f.label }}</text>
        </view>
      </view>

      <!-- 账单列表 -->
      <view v-if="list.length > 0" class="bill-list">
        <view v-for="bill in list" :key="bill.id" class="bill-item">
          <view class="bill-icon" :class="'bill-icon--' + bill.category">
            <iconpark-icon :name="bill.iconType" size="22" :color="bill.iconColor" />
          </view>
          <view class="bill-body">
            <text class="bill-title">{{ bill.title }}</text>
            <text class="bill-desc">{{ bill.desc || '' }}</text>
            <text class="bill-date">{{ bill.createdAt || bill.date }}</text>
          </view>
          <text class="bill-amount" :class="{ 'bill-amount--income': bill.type === 2 || bill.type === 3 || bill.type === 5 }">
            {{ bill.amount >= 0 ? '+' : '' }}¥ {{ Math.abs(bill.amount).toFixed(2) }}
          </text>
        </view>
      </view>

      <view v-else-if="!loading" class="empty-state">
        <iconpark-icon name="list" size="48" color="#D4D2CC" />
        <text class="empty-text">暂无账单记录</text>
      </view>

      <view v-if="loading" class="loading-state"><text>加载中…</text></view>

      <view class="bottom-placeholder"></view>
    </scroll-view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { transactionApi } from '@/api'
import { TRANSACTION_TYPES } from '@/utils/constants.js'

const sysInfo = uni.getSystemInfoSync()
const scrollHeight = sysInfo.windowHeight - sysInfo.statusBarHeight - 44

const activeFilter = ref('')
const list = ref([])
const page = ref(1)
const loading = ref(false)
const hasMore = ref(true)

const filters = [
  { key: '', label: '全部' },
  { key: 1, label: '任务支出' },
  { key: 2, label: '跑腿收入' },
  { key: 3, label: '充值' },
  { key: 4, label: '提现' },
  { key: 5, label: '退款' }
]

const categoryMap = {
  1: { category: 'express', iconType: 'billExpense', iconColor: '#FF6B4A' },
  2: { category: 'recharge', iconType: 'wallet', iconColor: '#34d399' },
  3: { category: 'recharge', iconType: 'wallet', iconColor: '#FF6B4A' },
  4: { category: 'withdraw', iconType: 'withdraw', iconColor: '#8F8D88' },
  5: { category: 'express', iconType: 'refresh', iconColor: '#34d399' }
}

const totalIncome = ref(0)
const totalExpense = ref(0)

async function loadSummary() {
  try {
    const data = await transactionApi.getSummary()
    totalIncome.value = data?.total_income || 0
    totalExpense.value = data?.total_expense || 0
  } catch (e) { console.error('loadSummary failed:', e) }
}

async function fetchList() {
  loading.value = true
  try {
    const params = { page: page.value, size: 15 }
    if (activeFilter.value) params.type = activeFilter.value
    const res = await transactionApi.getTransactions(params)
    const records = (res.records || []).map(r => {
      const meta = categoryMap[r.type] || { category: 'express', iconType: 'list', iconColor: '#8F8D88' }
      return {
        ...r,
        title: TRANSACTION_TYPES[r.type] || '交易',
        desc: r.userNickname ? `${r.userNickname}` : '',
        ...meta
      }
    })
    if (page.value === 1) list.value = records
    else list.value.push(...records)
    hasMore.value = records.length >= 15
  } catch (e) { /* handled */ }
  loading.value = false
}

function onFilter(key) {
  activeFilter.value = key
  page.value = 1
  list.value = []
  fetchList()
}

function loadMore() {
  if (!hasMore.value || loading.value) return
  page.value++
  fetchList()
}

function onBack() { uni.navigateBack() }

loadSummary()
fetchList()
</script>

<style scoped>
.page{width:100%;height:100vh;display:flex;flex-direction:column;background:var(--background);overflow:hidden}
.main-scroll{box-sizing:border-box;width:100%;padding:0 32rpx;padding-bottom:60rpx}

.summary-card{background:var(--surface-raised);border-radius:var(--radius-lg);padding:36rpx;margin-top:16rpx;box-shadow:var(--shadow-sm)}
.summary-row{display:flex;align-items:center}
.summary-item{flex:1;display:flex;flex-direction:column;align-items:center}
.summary-label{font-size:24rpx;color:var(--text-tertiary);margin-bottom:10rpx}
.summary-value{font-size:40rpx;font-weight:700;color:var(--text-primary)}
.summary-value--expense{color:var(--error)}
.summary-value--income{color:#34d399}
.summary-divider{width:1rpx;height:80rpx;background:var(--outline-light)}

.filter-row{display:flex;flex-wrap:wrap;gap:12rpx;margin-top:24rpx;padding:0 4rpx}
.filter-chip{padding:12rpx 24rpx;border-radius:48rpx;background:var(--surface-raised);border:1rpx solid var(--outline-light)}
.filter-chip text{font-size:24rpx;color:var(--text-secondary)}
.filter-chip--active{background:var(--primary-container);border-color:var(--primary)}
.filter-chip--active text{color:var(--primary);font-weight:500}

.bill-list{margin-top:24rpx;background:var(--surface-raised);border-radius:var(--radius-lg);overflow:hidden;box-shadow:var(--shadow-sm)}
.bill-item{display:flex;align-items:center;padding:28rpx 32rpx;border-bottom:1rpx solid var(--outline-light)}
.bill-item:last-child{border-bottom:none}
.bill-icon{width:68rpx;height:68rpx;border-radius:50%;display:flex;align-items:center;justify-content:center;flex-shrink:0;margin-right:20rpx}
.bill-icon--express{background:var(--primary-container)}
.bill-icon--recharge{background:var(--primary-container)}
.bill-icon--withdraw{background:var(--surface)}
.bill-body{flex:1;min-width:0}
.bill-title{font-size:28rpx;font-weight:500;color:var(--text-primary);display:block}
.bill-desc{font-size:24rpx;color:var(--text-secondary);margin-top:4rpx;display:block}
.bill-date{font-size:22rpx;color:var(--text-tertiary);margin-top:2rpx;display:block}
.bill-amount{font-size:28rpx;font-weight:600;color:var(--text-primary);flex-shrink:0;margin-left:16rpx}
.bill-amount--income{color:#34d399}

.empty-state{display:flex;flex-direction:column;align-items:center;padding:120rpx 0;gap:20rpx;opacity:.5}
.empty-text{font-size:28rpx;color:var(--text-tertiary)}
.loading-state{text-align:center;padding:32rpx}
.loading-state text{font-size:24rpx;color:var(--text-tertiary)}
.bottom-placeholder{height:60rpx}
</style>
