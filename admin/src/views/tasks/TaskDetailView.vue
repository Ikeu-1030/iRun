<template>
  <div class="page" :class="{ entered }">
    <el-page-header @back="$router.back()" title="返回" class="anim-header">
      <template #content>
        <span class="page-title">任务详情 — {{ detail.taskNo }}</span>
        <span class="type-badge" :style="{ color: taskTypeConfig.color, background: taskTypeConfig.bgColor }">
          <el-icon class="type-badge-icon"><component :is="taskTypeConfig.icon" /></el-icon>
          {{ taskTypeConfig.label }}
        </span>
      </template>
    </el-page-header>

    <div class="detail-blocks" v-loading="loading">
      <!-- 任务信息 -->
      <el-card class="detail-block anim-block type-accent" :style="{ '--anim-order': 0, '--type-color': taskTypeConfig.color, '--type-bg': taskTypeConfig.bgColor }">
        <template #header>
          <div class="card-header">
            <span class="card-title-text">任务信息</span>
            <span class="status-tag" :style="{ color: statusStyle(detail.status).color, background: statusStyle(detail.status).bgColor }">
              {{ TASK_STATUS[detail.status as keyof typeof TASK_STATUS] }}
            </span>
          </div>
        </template>
        <el-descriptions :column="2" border>
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
              {{ taskTypeConfig.label }}
            </span>
          </el-descriptions-item>
          <el-descriptions-item label="任务小类">
            <span v-if="detail.subType" class="type-subtag" :style="{ color: taskTypeConfig.color, borderColor: taskTypeConfig.color }">{{ detail.subType }}</span>
            <span v-else>-</span>
          </el-descriptions-item>
          <el-descriptions-item label="任务规格">{{ taskSpecsDisplay }}</el-descriptions-item>
          <el-descriptions-item label="性别要求">{{ detail.requireSex || '不限' }}</el-descriptions-item>
          <el-descriptions-item label="发布时间" :span="2">{{ detail.publishTime }}</el-descriptions-item>
          <el-descriptions-item label="过期时间" :span="2">{{ detail.expireTime }}</el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- 取送信息 -->
      <el-card class="detail-block anim-block info-card" style="--anim-order: 1">
        <template #header>
          <span class="card-title-text"><el-icon class="card-title-icon"><MapLocation /></el-icon>取送信息</span>
        </template>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="取件地址" :span="2">{{ detail.pickupAddress || '-' }}</el-descriptions-item>
          <el-descriptions-item label="送达地址" :span="2">{{ detail.deliveryAddress || '-' }}</el-descriptions-item>
          <el-descriptions-item label="联系人" :span="2">{{ detail.contactName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="联系电话" :span="2">{{ detail.contactPhone || '-' }}</el-descriptions-item>
          <el-descriptions-item v-if="detail.pickupCode" label="取件码">{{ detail.pickupCode }}</el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- 描述信息 -->
      <el-card class="detail-block anim-block info-card" style="--anim-order: 2">
        <template #header>
          <span class="card-title-text"><el-icon class="card-title-icon"><Notebook /></el-icon>描述信息</span>
        </template>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="公开描述" :span="2">{{ detail.publicDesc || '-' }}</el-descriptions-item>
          <el-descriptions-item v-if="detail.privateNote" label="私密备注" :span="2">{{ detail.privateNote }}</el-descriptions-item>
          <el-descriptions-item label="发布者昵称" :span="2">{{ detail.publisherNickname }}</el-descriptions-item>
          <el-descriptions-item label="发布者用户名" :span="2">{{ detail.publisherUsername || '-' }}</el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- 取消原因 -->
      <el-card v-if="detail.cancelReason" class="detail-block anim-block" style="--anim-order: 3">
        <template #header>
          <span class="card-title-text">取消原因</span>
        </template>
        <el-alert :title="detail.cancelReason" type="warning" show-icon :closable="false" />
      </el-card>

      <!-- 任务图片 -->
      <el-card v-if="detail.imageUrls && detail.imageUrls.length > 0" class="detail-block anim-block" style="--anim-order: 4">
        <template #header>
          <span class="card-title-text">任务图片</span>
        </template>
        <div class="image-grid">
          <el-image
            v-for="(url, i) in detail.imageUrls"
            :key="i"
            :src="url"
            :preview-src-list="detail.imageUrls"
            :initial-index="i"
            fit="cover"
            class="task-image anim-img"
            :style="{ animationDelay: (0.3 + i * 0.07) + 's' }"
          >
            <template #error>
              <div class="image-error">
                <el-icon><Picture /></el-icon>
                <span>加载失败</span>
              </div>
            </template>
          </el-image>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
// v2 block-based layout — 2026-06-10
import { onMounted, reactive, ref, nextTick, computed } from 'vue'
import { useRoute } from 'vue-router'
import { Picture, Box, KnifeFork, Document, ShoppingCart, MoreFilled, MapLocation, Notebook } from '@element-plus/icons-vue'
import { getTaskDetail } from '@/api/tasks'
import { TASK_STATUS } from '@/utils/constants'
import { parseTaskSpecsForAdmin } from '@/utils/task-specs-parser'

const route = useRoute()
const loading = ref(false)
const entered = ref(false)
const detail = reactive<any>({})

const statusStyleMap: Record<number, { color: string; bgColor: string }> = {
  1: { color: '#8492A6', bgColor: '#EFF2F7' },  // 待接单
  2: { color: '#C8925D', bgColor: '#FDF3EB' },  // 已接单
  3: { color: '#5B9BD5', bgColor: '#EFF5FB' },  // 配送中
  4: { color: '#8B6BAE', bgColor: '#F6F1FA' },  // 待确认
  5: { color: '#2EB89E', bgColor: '#EDFAF7' },  // 已完成
  6: { color: '#E87474', bgColor: '#FEF0F0' },  // 已取消
}

function statusStyle(s: number) {
  return statusStyleMap[s] ?? { color: '#8492A6', bgColor: '#EFF2F7' }
}

const taskTypeConfig = computed(() => {
  const map: Record<string, { color: string; bgColor: string; icon: any; label: string }> = {
    '代取快递': { color: '#E8734A', bgColor: '#FFF2ED', icon: Box, label: '代取快递' }, 'daiqukuaidi': { color: '#E8734A', bgColor: '#FFF2ED', icon: Box, label: '代取快递' },
    '代拿餐食': { color: '#2EB89E', bgColor: '#EDFAF7', icon: KnifeFork, label: '代拿餐食' }, 'dana canshi': { color: '#2EB89E', bgColor: '#EDFAF7', icon: KnifeFork, label: '代拿餐食' },
    '校内代办': { color: '#5B9BD5', bgColor: '#EFF5FB', icon: Document, label: '校内代办' }, 'xiaonei daiban': { color: '#5B9BD5', bgColor: '#EFF5FB', icon: Document, label: '校内代办' },
    '代购物品': { color: '#8B6BAE', bgColor: '#F6F1FA', icon: ShoppingCart, label: '代购物品' }, 'daigou wupin': { color: '#8B6BAE', bgColor: '#F6F1FA', icon: ShoppingCart, label: '代购物品' },
    '通用代办': { color: '#C8925D', bgColor: '#FDF3EB', icon: MoreFilled, label: '通用代办' }, 'tongyong daiban': { color: '#C8925D', bgColor: '#FDF3EB', icon: MoreFilled, label: '通用代办' },
  }
  return map[detail.type] ?? { color: '#909399', bgColor: '#F5F5F5', icon: Document, label: detail.type || '未知' }
})

function formatReward(val: any) {
  if (val == null) return '0.00'
  return Number(val).toFixed(2)
}

const taskSpecsDisplay = computed(() => parseTaskSpecsForAdmin(detail.taskSpecs))

onMounted(async () => {
  loading.value = true
  try {
    const res = await getTaskDetail(Number(route.params.id)) as any
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

/* ===== 普通信息卡片 ===== */
.info-card {
  border-left: 3px solid #E4E7ED;
}

.info-card :deep(.el-card__header) {
  background: #FAFBFC;
}

.card-title-icon {
  margin-right: 6px;
  font-size: 15px;
  vertical-align: -2px;
  color: var(--text-secondary);
}

.type-accent :deep(.el-card__header) {
  background: var(--type-bg);
  border-bottom-color: var(--type-color);
  border-bottom-width: 1px;
  transition: background 0.3s ease;
}

.type-accent {
  border-left: 3px solid var(--type-color);
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
