<template>
  <div class="page" :class="{ entered }">
    <el-page-header @back="$router.back()" title="返回" class="anim-header">
      <template #content>
        <span class="page-title">订单详情 — {{ detail.taskNo }}</span>
        <span class="type-badge" :style="{ color: taskTypeConfig.color, background: taskTypeConfig.bgColor }">
          <el-icon class="type-badge-icon"><component :is="taskTypeConfig.icon" /></el-icon>
          {{ taskTypeConfig.label }}
        </span>
      </template>
    </el-page-header>

    <div class="detail-blocks" v-loading="loading">
      <!-- 订单信息 -->
      <el-card class="detail-block anim-block type-accent" :style="{ '--anim-order': 0, '--type-color': taskTypeConfig.color, '--type-bg': taskTypeConfig.bgColor }">
        <template #header>
          <div class="card-header">
            <span class="card-title-text">订单信息</span>
            <span class="status-tag" :style="{ color: orderStatusStyle(detail.orderStatus).color, background: orderStatusStyle(detail.orderStatus).bgColor }">
              {{ ORDER_STATUS[detail.orderStatus as keyof typeof ORDER_STATUS] }}
            </span>
          </div>
        </template>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="订单ID">{{ detail.orderId }}</el-descriptions-item>
          <el-descriptions-item label="任务编号">{{ detail.taskNo }}</el-descriptions-item>
          <el-descriptions-item label="合计支付">
            <el-popover placement="bottom" :width="200" trigger="hover" :show-after="200">
              <template #reference>
                <span class="reward reward-hover">¥{{ formatReward(detail.reward) }}</span>
              </template>
              <div class="fee-popover">
                <div class="fee-row"><span>小费</span><span>¥{{ formatReward(detail.tip || 0) }}</span></div>
                <div class="fee-row"><span>配送费</span><span>¥{{ formatReward(detail.deliveryFee || 0) }}</span></div>
                <div class="fee-row"><span>预估商品费</span><span>¥{{ formatReward(detail.productCost || 0) }}</span></div>
                <div class="fee-divider"></div>
                <div class="fee-row fee-total"><span>合计</span><span>¥{{ formatReward(detail.reward) }}</span></div>
              </div>
            </el-popover>
          </el-descriptions-item>
          <el-descriptions-item label="任务大类">
            <span class="type-tag" :style="{ color: taskTypeConfig.color, background: taskTypeConfig.bgColor }">
              <el-icon class="type-tag-icon"><component :is="taskTypeConfig.icon" /></el-icon>
              {{ detail.type || '-' }}
            </span>
          </el-descriptions-item>
          <el-descriptions-item label="任务小类">
            <span v-if="detail.subType" class="type-subtag" :style="{ color: taskTypeConfig.color, borderColor: taskTypeConfig.color }">{{ detail.subType }}</span>
            <span v-else>-</span>
          </el-descriptions-item>
          <el-descriptions-item label="任务规格" :span="2">{{ taskSpecsDisplay }}</el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- 取送信息 -->
      <el-card class="detail-block anim-block info-card" style="--anim-order: 1">
        <template #header>
          <span class="card-title-text">取送信息</span>
        </template>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="取件码">{{ detail.pickupCode || '-' }}</el-descriptions-item>
          <el-descriptions-item label="取件地址" :span="2">{{ detail.pickupAddress || '-' }}</el-descriptions-item>
          <el-descriptions-item label="送达地址" :span="2">{{ detail.deliveryAddress || '-' }}</el-descriptions-item>
          <el-descriptions-item label="收货联系人" :span="2">{{ detail.contactName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="收货联系电话" :span="2">{{ isSuperAdmin ? (detail.contactPhone || '-') : maskPhone(detail.contactPhone) }}</el-descriptions-item>
          <el-descriptions-item label="公开描述" :span="2">{{ detail.publicDesc || '-' }}</el-descriptions-item>
          <el-descriptions-item v-if="detail.privateNote" label="私密备注" :span="2">{{ detail.privateNote }}</el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- 人员信息 -->
      <el-card class="detail-block anim-block info-card" style="--anim-order: 2">
        <template #header>
          <span class="card-title-text">人员信息</span>
        </template>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="发布者昵称">{{ detail.publisherNickname || '-' }}</el-descriptions-item>
          <el-descriptions-item label="跑腿员昵称">{{ detail.runnerNickname || '-' }}</el-descriptions-item>
          <el-descriptions-item label="发布者用户名">{{ detail.publisherUsername || '-' }}</el-descriptions-item>
          <el-descriptions-item label="跑腿员用户名">{{ detail.runnerUsername || '-' }}</el-descriptions-item>
          <el-descriptions-item label="发布者手机">{{ isSuperAdmin ? (detail.publisherPhone || '-') : maskPhone(detail.publisherPhone) }}</el-descriptions-item>
          <el-descriptions-item label="跑腿员手机">{{ isSuperAdmin ? (detail.runnerPhone || '-') : maskPhone(detail.runnerPhone) }}</el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- 时间记录 -->
      <el-card class="detail-block anim-block info-card" style="--anim-order: 3">
        <template #header>
          <span class="card-title-text">时间记录</span>
        </template>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="接单时间">{{ detail.acceptTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="预计完成">{{ detail.expectFinishTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="取货时间">{{ detail.pickupTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="送达时间">{{ detail.deliverTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="确认时间">{{ detail.confirmTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="取消时间">{{ detail.cancelTime || '-' }}</el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- 取消原因 -->
      <el-card v-if="detail.cancelReason" class="detail-block anim-block" style="--anim-order: 4">
        <template #header>
          <span class="card-title-text">取消原因</span>
        </template>
        <el-alert :title="detail.cancelReason" type="warning" show-icon :closable="false" />
      </el-card>

      <!-- 任务图片 -->
      <el-card v-if="detail.imageUrls?.length" class="detail-block anim-block" style="--anim-order: 5">
        <template #header>
          <span class="card-title-text">任务图片</span>
        </template>
        <div class="image-grid">
          <el-image v-for="(url, i) in detail.imageUrls" :key="i" :src="url" :preview-src-list="detail.imageUrls" :initial-index="i" fit="cover" class="task-image anim-img" :style="{ animationDelay: (0.3 + i * 0.07) + 's' }">
            <template #error>
              <div class="image-error"><el-icon><Picture /></el-icon><span>加载失败</span></div>
            </template>
          </el-image>
        </div>
      </el-card>

      <!-- 取货凭证 -->
      <el-card v-if="detail.pickupProofImgs?.length" class="detail-block anim-block" style="--anim-order: 6">
        <template #header>
          <span class="card-title-text">取货凭证</span>
        </template>
        <div class="image-grid">
          <el-image v-for="(url, i) in detail.pickupProofImgs" :key="i" :src="url" :preview-src-list="detail.pickupProofImgs" :initial-index="i" fit="cover" class="task-image anim-img" :style="{ animationDelay: (0.3 + i * 0.07) + 's' }">
            <template #error>
              <div class="image-error"><el-icon><Picture /></el-icon><span>加载失败</span></div>
            </template>
          </el-image>
        </div>
      </el-card>

      <!-- 送达凭证 -->
      <el-card v-if="detail.deliverProofImgs?.length" class="detail-block anim-block" style="--anim-order: 7">
        <template #header>
          <span class="card-title-text">送达凭证</span>
        </template>
        <div class="image-grid">
          <el-image v-for="(url, i) in detail.deliverProofImgs" :key="i" :src="url" :preview-src-list="detail.deliverProofImgs" :initial-index="i" fit="cover" class="task-image anim-img" :style="{ animationDelay: (0.3 + i * 0.07) + 's' }">
            <template #error>
              <div class="image-error"><el-icon><Picture /></el-icon><span>加载失败</span></div>
            </template>
          </el-image>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref, nextTick, computed } from 'vue'
import { useRoute } from 'vue-router'
import { Picture, Box, KnifeFork, Document, ShoppingCart, MoreFilled } from '@element-plus/icons-vue'
import { getOrderDetail } from '@/api/orders'
import { ORDER_STATUS } from '@/utils/constants'
import { parseTaskSpecsForAdmin } from '@/utils/task-specs-parser'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const authStore = useAuthStore()
const isSuperAdmin = computed(() => authStore.adminInfo?.role === 1)
const loading = ref(false)
const entered = ref(false)
const detail = reactive<any>({})

function maskPhone(phone: string | null | undefined): string {
  if (!phone || phone.length < 7) return phone || '-'
  return phone.slice(0, 3) + '****' + phone.slice(-4)
}

const orderStatusStyleMap: Record<number, { color: string; bgColor: string }> = {
  1: { color: '#8492A6', bgColor: '#EFF2F7' },  // 待取货
  2: { color: '#5B9BD5', bgColor: '#EFF5FB' },  // 配送中
  3: { color: '#8B6BAE', bgColor: '#F6F1FA' },  // 待确认
  4: { color: '#2EB89E', bgColor: '#EDFAF7' },  // 已完成
  5: { color: '#E87474', bgColor: '#FEF0F0' },  // 已取消
}

function orderStatusStyle(s: number) {
  return orderStatusStyleMap[s] ?? { color: '#8492A6', bgColor: '#EFF2F7' }
}

function formatReward(val: any) {
  if (val == null) return '0.00'
  return Number(val).toFixed(2)
}

const taskSpecsDisplay = computed(() => parseTaskSpecsForAdmin(detail.taskSpecs))

const taskTypeConfig = computed(() => {
  const map: Record<string, { color: string; bgColor: string; icon: any; label: string }> = {
    '代取快递': { color: '#E8734A', bgColor: '#FFF2ED', icon: Box, label: '代取快递' },
    '代拿餐食': { color: '#2EB89E', bgColor: '#EDFAF7', icon: KnifeFork, label: '代拿餐食' },
    '校内代办': { color: '#5B9BD5', bgColor: '#EFF5FB', icon: Document, label: '校内代办' },
    '代购物品': { color: '#8B6BAE', bgColor: '#F6F1FA', icon: ShoppingCart, label: '代购物品' },
    '通用代办': { color: '#C8925D', bgColor: '#FDF3EB', icon: MoreFilled, label: '通用代办' },
  }
  return map[detail.type] ?? { color: '#909399', bgColor: '#F5F5F5', icon: Document, label: detail.type || '未知' }
})

onMounted(async () => {
  loading.value = true
  try {
    const res = await getOrderDetail(Number(route.params.id)) as any
    Object.assign(detail, res)
  } finally { loading.value = false }
  await nextTick()
  entered.value = true
})
</script>

<style scoped>
.detail-blocks {
  margin-top: 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.detail-block :deep(.el-descriptions__label) {
  width: 9em;
  white-space: nowrap;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-title-text {
  font-weight: 600;
  color: var(--text-primary);
}

/* ===== 任务类型色彩体系 ===== */
.type-badge {
  margin-left: 12px;
  padding: 3px 12px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 500;
  vertical-align: middle;
}

.type-badge-icon {
  margin-right: 4px;
  vertical-align: middle;
  font-size: 14px;
}

.type-tag {
  display: inline-flex;
  align-items: center;
  padding: 2px 10px;
  border-radius: 12px;
  font-size: 13px;
  font-weight: 500;
}

.type-tag-icon {
  margin-right: 3px;
  font-size: 14px;
}

.type-subtag {
  display: inline-block;
  padding: 1px 9px;
  border: 1px solid;
  border-radius: 10px;
  font-size: 12px;
}

/* 订单信息卡片 — 头部淡色底纹 + 左侧细强调线 */
.type-accent :deep(.el-card__header) {
  background: var(--type-bg);
  border-bottom-color: var(--type-color);
  border-bottom-width: 1px;
  transition: background 0.3s ease;
}

.type-accent {
  border-left: 3px solid var(--type-color);
}

/* ===== 普通信息卡片 ===== */
.info-card {
  border-left: 3px solid #E4E7ED;
}

.info-card :deep(.el-card__header) {
  background: #FAFBFC;
}

/* ===== 状态标签浅色系 ===== */
.status-tag {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 10px;
  font-size: 13px;
  font-weight: 500;
}

.reward {
  color: var(--brand-accent);
  font-weight: 600;
  font-size: 16px;
}
.fee-sub { color: var(--text-secondary); font-size: 13px; }
.reward-hover { cursor: help; border-bottom: 1px dashed var(--brand-accent); }
.fee-popover { font-size: 13px; }
.fee-row { display: flex; justify-content: space-between; padding: 4px 0; color: var(--text-secondary); }
.fee-divider { border-top: 1px solid var(--neutral-outline); margin: 4px 0; }
.fee-total { font-weight: 600; color: var(--text-primary); }

.image-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.task-image {
  width: 160px;
  height: 160px;
  border-radius: var(--radius-sm);
}

.image-error {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: var(--text-placeholder);
  font-size: 12px;
  background: var(--neutral-surface);
  gap: 4px;
}

/* ===== 入场动画 ===== */
.anim-header,
.anim-block,
.anim-img {
  opacity: 0;
}

.entered .anim-header {
  animation: slideInLeft 0.4s var(--ease-out) both;
}

.entered .anim-block {
  animation: fadeUp 0.45s calc(0.05s + var(--anim-order, 0) * 0.06s) var(--ease-out) both;
}

.entered .anim-img {
  animation: imgPop 0.45s var(--ease-spring) both;
}

@keyframes slideInLeft {
  from { opacity: 0; transform: translateX(-16px); }
  to   { opacity: 1; transform: translateX(0); }
}

@keyframes fadeUp {
  from { opacity: 0; transform: translateY(16px); }
  to   { opacity: 1; transform: translateY(0); }
}

@keyframes imgPop {
  from { opacity: 0; transform: scale(0.88); }
  to   { opacity: 1; transform: scale(1); }
}
</style>
