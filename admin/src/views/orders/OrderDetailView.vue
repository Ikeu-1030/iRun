<template>
  <div class="page" :class="{ entered }">
    <el-page-header @back="$router.back()" title="返回" class="anim-header">
      <template #content>
        <span class="page-title">订单详情 — {{ detail.taskNo }}</span>
      </template>
    </el-page-header>

    <el-card v-loading="loading" class="detail-card anim-card">
      <template #header>
        <div class="card-header">
          <span>基本信息</span>
          <el-tag :type="statusTag(detail.orderStatus)" size="default">
            {{ ORDER_STATUS[detail.orderStatus as keyof typeof ORDER_STATUS] }}
          </el-tag>
        </div>
      </template>

      <el-descriptions :column="2" border class="anim-desc" style="--anim-delay: 0.08s">
        <el-descriptions-item label="订单ID">{{ detail.orderId }}</el-descriptions-item>
        <el-descriptions-item label="任务编号">{{ detail.taskNo }}</el-descriptions-item>
        <el-descriptions-item label="报酬">
          <span class="reward">¥{{ formatReward(detail.reward) }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="任务大类">{{ detail.type || '-' }}</el-descriptions-item>
        <el-descriptions-item label="任务小类">{{ detail.subType || '-' }}</el-descriptions-item>
        <el-descriptions-item label="任务规格" :span="2">{{ taskSpecsDisplay }}</el-descriptions-item>
        <el-descriptions-item label="取件码">{{ detail.pickupCode || '-' }}</el-descriptions-item>
        <el-descriptions-item label="公开描述" :span="2">{{ detail.publicDesc || '-' }}</el-descriptions-item>
        <el-descriptions-item v-if="detail.privateNote" label="私密备注" :span="2">{{ detail.privateNote }}</el-descriptions-item>
        <el-descriptions-item label="取件地址" :span="2">{{ detail.pickupAddress || '-' }}</el-descriptions-item>
        <el-descriptions-item label="送达地址" :span="2">{{ detail.deliveryAddress || '-' }}</el-descriptions-item>
        <el-descriptions-item label="收货联系人">{{ detail.contactName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="收货联系电话">{{ detail.contactPhone || '-' }}</el-descriptions-item>
      </el-descriptions>

      <el-descriptions :column="2" border class="anim-desc" style="margin-top:16px;--anim-delay:0.18s" title="人员信息">
        <el-descriptions-item label="发布者昵称">{{ detail.publisherNickname || '-' }}</el-descriptions-item>
        <el-descriptions-item label="跑腿员昵称">{{ detail.runnerNickname || '-' }}</el-descriptions-item>
        <el-descriptions-item label="发布者用户名">{{ detail.publisherUsername || '-' }}</el-descriptions-item>
        <el-descriptions-item label="跑腿员用户名">{{ detail.runnerUsername || '-' }}</el-descriptions-item>
        <el-descriptions-item label="发布者手机">{{ detail.publisherPhone || '-' }}</el-descriptions-item>
        <el-descriptions-item label="跑腿员手机">{{ detail.runnerPhone || '-' }}</el-descriptions-item>
      </el-descriptions>

      <el-descriptions :column="2" border class="anim-desc" style="margin-top:16px;--anim-delay:0.28s" title="时间记录">
        <el-descriptions-item label="接单时间">{{ detail.acceptTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="预计完成">{{ detail.expectFinishTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="取货时间">{{ detail.pickupTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="送达时间">{{ detail.deliverTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="确认时间">{{ detail.confirmTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="取消时间">{{ detail.cancelTime || '-' }}</el-descriptions-item>
      </el-descriptions>

      <div v-if="detail.cancelReason" class="cancel-info anim-cancel">
        <el-alert :title="'取消原因：' + detail.cancelReason" type="warning" show-icon :closable="false" />
      </div>

      <div v-if="detail.imageUrls?.length" class="image-section anim-img-section">
        <h4>任务图片</h4>
        <div class="image-grid">
          <el-image v-for="(url, i) in detail.imageUrls" :key="i" :src="url" :preview-src-list="detail.imageUrls" :initial-index="i" fit="cover" class="task-image anim-img" :style="{ animationDelay: (0.3 + i * 0.07) + 's' }">
            <template #error>
              <div class="image-error"><el-icon><Picture /></el-icon><span>加载失败</span></div>
            </template>
          </el-image>
        </div>
      </div>

      <div v-if="detail.pickupProofImgs?.length" class="image-section anim-img-section">
        <h4>取货凭证</h4>
        <div class="image-grid">
          <el-image v-for="(url, i) in detail.pickupProofImgs" :key="i" :src="url" :preview-src-list="detail.pickupProofImgs" :initial-index="i" fit="cover" class="task-image anim-img" :style="{ animationDelay: (0.3 + i * 0.07) + 's' }">
            <template #error>
              <div class="image-error"><el-icon><Picture /></el-icon><span>加载失败</span></div>
            </template>
          </el-image>
        </div>
      </div>

      <div v-if="detail.deliverProofImgs?.length" class="image-section anim-img-section">
        <h4>送达凭证</h4>
        <div class="image-grid">
          <el-image v-for="(url, i) in detail.deliverProofImgs" :key="i" :src="url" :preview-src-list="detail.deliverProofImgs" :initial-index="i" fit="cover" class="task-image anim-img" :style="{ animationDelay: (0.3 + i * 0.07) + 's' }">
            <template #error>
              <div class="image-error"><el-icon><Picture /></el-icon><span>加载失败</span></div>
            </template>
          </el-image>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref, nextTick, computed } from 'vue'
import { useRoute } from 'vue-router'
import { Picture } from '@element-plus/icons-vue'
import { getOrderDetail } from '@/api/orders'
import { ORDER_STATUS } from '@/utils/constants'
import { parseTaskSpecsForAdmin } from '@/utils/task-specs-parser'

const route = useRoute()
const loading = ref(false)
const entered = ref(false)
const detail = reactive<any>({})

function statusTag(s: number) {
  const map: Record<number, string> = { 1: 'info', 2: 'warning', 3: '', 4: 'success', 5: 'danger' }
  return map[s] ?? 'info'
}

function formatReward(val: any) {
  if (val == null) return '0.00'
  return Number(val).toFixed(2)
}

const taskSpecsDisplay = computed(() => parseTaskSpecsForAdmin(detail.taskSpecs))

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
.detail-card { margin-top: 20px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.reward { color: var(--brand-accent); font-weight: 600; font-size: 16px; }
.cancel-info { margin-top: 16px; }
.image-section { margin-top: 20px; }
.image-section h4 { margin-bottom: 12px; color: var(--text-primary); }
.image-grid { display: flex; flex-wrap: wrap; gap: 12px; }
.task-image { width: 160px; height: 160px; border-radius: var(--radius-sm); }
.image-error { width: 100%; height: 100%; display: flex; flex-direction: column; align-items: center; justify-content: center; color: var(--text-placeholder); font-size: 12px; background: var(--neutral-surface); gap: 4px; }

/* ===== 入场动画 ===== */
.anim-header,
.anim-card,
.anim-desc,
.anim-img-section,
.anim-cancel,
.anim-img {
  opacity: 0;
}

.entered .anim-header {
  animation: slideInLeft 0.4s var(--ease-out) both;
}

.entered .anim-card {
  animation: fadeUp 0.5s 0.05s var(--ease-out) both;
}

.entered .anim-desc {
  animation: fadeUp 0.4s var(--anim-delay, 0.1s) var(--ease-out) both;
}

.entered .anim-cancel {
  animation: slideInLeft 0.3s 0.25s var(--ease-out) both;
}

.entered .anim-img-section {
  animation: fadeUp 0.4s 0.25s var(--ease-out) both;
}

.entered .anim-img {
  animation: imgPop 0.45s var(--ease-spring) both;
}

@keyframes slideInLeft {
  from { opacity: 0; transform: translateX(-16px); }
  to   { opacity: 1; transform: translateX(0); }
}

@keyframes fadeUp {
  from { opacity: 0; transform: translateY(20px); }
  to   { opacity: 1; transform: translateY(0); }
}

@keyframes imgPop {
  from { opacity: 0; transform: scale(0.88); }
  to   { opacity: 1; transform: scale(1); }
}
</style>
