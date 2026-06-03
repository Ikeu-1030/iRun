<template>
  <div class="page" ref="pageRef">
    <el-page-header @back="$router.back()" title="返回">
      <template #content>
        <span class="page-title">订单详情 — {{ detail.taskNo }}</span>
      </template>
    </el-page-header>

    <el-card v-loading="loading" class="detail-card">
      <template #header>
        <div class="card-header">
          <span>基本信息</span>
          <el-tag :type="statusTag(detail.orderStatus)" size="default">
            {{ ORDER_STATUS[detail.orderStatus as keyof typeof ORDER_STATUS] }}
          </el-tag>
        </div>
      </template>

      <el-descriptions :column="2" border>
        <el-descriptions-item label="订单ID">{{ detail.orderId }}</el-descriptions-item>
        <el-descriptions-item label="任务编号">{{ detail.taskNo }}</el-descriptions-item>
        <el-descriptions-item label="报酬">
          <span class="reward">¥{{ formatReward(detail.reward) }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="任务大类">{{ detail.type || '-' }}</el-descriptions-item>
        <el-descriptions-item label="任务小类">{{ detail.subType || '-' }}</el-descriptions-item>
        <el-descriptions-item label="取件码">{{ detail.pickupCode || '-' }}</el-descriptions-item>
        <el-descriptions-item label="公开描述" :span="2">{{ detail.publicDesc || '-' }}</el-descriptions-item>
        <el-descriptions-item v-if="detail.privateNote" label="私密备注" :span="2">{{ detail.privateNote }}</el-descriptions-item>
        <el-descriptions-item label="取件地址" :span="2">{{ detail.pickupAddress || '-' }}</el-descriptions-item>
        <el-descriptions-item label="送达地址" :span="2">{{ detail.deliveryAddress || '-' }}</el-descriptions-item>
        <el-descriptions-item label="收货联系人">{{ detail.contactName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="收货联系电话">{{ detail.contactPhone || '-' }}</el-descriptions-item>
      </el-descriptions>

      <el-descriptions :column="2" border style="margin-top: 16px" title="人员信息">
        <el-descriptions-item label="发布者昵称">{{ detail.publisherNickname || '-' }}</el-descriptions-item>
        <el-descriptions-item label="跑腿员昵称">{{ detail.runnerNickname || '-' }}</el-descriptions-item>
        <el-descriptions-item label="发布者用户名">{{ detail.publisherUsername || '-' }}</el-descriptions-item>
        <el-descriptions-item label="跑腿员用户名">{{ detail.runnerUsername || '-' }}</el-descriptions-item>
        <el-descriptions-item label="发布者手机">{{ detail.publisherPhone || '-' }}</el-descriptions-item>
        <el-descriptions-item label="跑腿员手机">{{ detail.runnerPhone || '-' }}</el-descriptions-item>
      </el-descriptions>

      <el-descriptions :column="2" border style="margin-top: 16px" title="时间记录">
        <el-descriptions-item label="接单时间">{{ detail.acceptTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="预计完成">{{ detail.expectFinishTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="取货时间">{{ detail.pickupTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="送达时间">{{ detail.deliverTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="确认时间">{{ detail.confirmTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="取消时间">{{ detail.cancelTime || '-' }}</el-descriptions-item>
      </el-descriptions>

      <div v-if="detail.cancelReason" class="cancel-info">
        <el-alert :title="'取消原因：' + detail.cancelReason" type="warning" show-icon :closable="false" />
      </div>

      <!-- 任务图片 -->
      <div v-if="detail.imageUrls && detail.imageUrls.length > 0" class="image-section">
        <h4>任务图片</h4>
        <div class="image-grid">
          <el-image v-for="(url, i) in detail.imageUrls" :key="i" :src="url" :preview-src-list="detail.imageUrls" :initial-index="i" fit="cover" class="task-image">
            <template #error>
              <div class="image-error"><el-icon><Picture /></el-icon><span>加载失败</span></div>
            </template>
          </el-image>
        </div>
      </div>

      <!-- 取货凭证 -->
      <div v-if="detail.pickupProofImgs && detail.pickupProofImgs.length > 0" class="image-section">
        <h4>取货凭证</h4>
        <div class="image-grid">
          <el-image v-for="(url, i) in detail.pickupProofImgs" :key="i" :src="url" :preview-src-list="detail.pickupProofImgs" :initial-index="i" fit="cover" class="task-image">
            <template #error>
              <div class="image-error"><el-icon><Picture /></el-icon><span>加载失败</span></div>
            </template>
          </el-image>
        </div>
      </div>

      <!-- 送达凭证 -->
      <div v-if="detail.deliverProofImgs && detail.deliverProofImgs.length > 0" class="image-section">
        <h4>送达凭证</h4>
        <div class="image-grid">
          <el-image v-for="(url, i) in detail.deliverProofImgs" :key="i" :src="url" :preview-src-list="detail.deliverProofImgs" :initial-index="i" fit="cover" class="task-image">
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
import { onMounted, reactive, ref, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Picture } from '@element-plus/icons-vue'
import gsap from 'gsap'
import { getOrderDetail } from '@/api/orders'
import { ORDER_STATUS } from '@/utils/constants'

const route = useRoute()
const loading = ref(false)
const detail = reactive<any>({})
const pageRef = ref<HTMLElement>()

function statusTag(s: number) {
  const map: Record<number, string> = { 1: '', 2: 'warning', 3: 'warning', 4: 'success', 5: 'danger' }
  return map[s] ?? 'info'
}

function formatReward(val: any) {
  if (val == null) return '0.00'
  return Number(val).toFixed(2)
}

onMounted(async () => {
  loading.value = true
  try {
    const res = await getOrderDetail(Number(route.params.id)) as any
    Object.assign(detail, res)
    await nextTick()
    runEntrance()
  } finally { loading.value = false }
})

function runEntrance() {
  const el = pageRef.value
  if (!el) return

  const tl = gsap.timeline({ defaults: { ease: 'power3.out' } })

  // Page header
  const header = el.querySelector('.el-page-header') as HTMLElement
  if (header) tl.from(header, { x: -16, opacity: 0, duration: 0.4 }, 0)

  // Main detail card
  const card = el.querySelector('.detail-card') as HTMLElement
  if (card) tl.from(card, { y: 24, opacity: 0, duration: 0.55 }, '-=0.25')

  // Description blocks stagger
  const descBlocks = el.querySelectorAll('.el-descriptions')
  if (descBlocks.length) {
    tl.from(descBlocks, {
      y: 16, opacity: 0, duration: 0.45, stagger: 0.1, ease: 'power3.out',
    }, '-=0.3')
  }

  // Cancel alert
  const cancel = el.querySelector('.cancel-info') as HTMLElement
  if (cancel) tl.from(cancel, { x: -10, opacity: 0, duration: 0.35 }, '-=0.15')

  // Image grids stagger
  const images = el.querySelectorAll('.task-image')
  if (images.length) {
    tl.from(images, {
      scale: 0.85, opacity: 0, duration: 0.45, stagger: 0.07, ease: 'back.out(1.4)',
    }, '-=0.2')
  }
}
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
</style>
