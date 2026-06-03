<template>
  <div class="page" :class="{ entered }">
    <el-page-header @back="$router.back()" title="返回" class="anim-header">
      <template #content>
        <span class="page-title">任务详情 — {{ detail.taskNo }}</span>
      </template>
    </el-page-header>

    <el-card v-loading="loading" class="detail-card anim-card">
      <template #header>
        <div class="card-header">
          <span>基本信息</span>
          <el-tag :type="statusTag(detail.status)" size="default">
            {{ TASK_STATUS[detail.status as keyof typeof TASK_STATUS] }}
          </el-tag>
        </div>
      </template>

      <el-descriptions :column="2" border>
        <el-descriptions-item label="任务编号">{{ detail.taskNo }}</el-descriptions-item>
        <el-descriptions-item label="报酬">
          <span class="reward">¥{{ formatReward(detail.reward) }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="任务大类">{{ detail.type }}</el-descriptions-item>
        <el-descriptions-item label="任务小类">{{ detail.subType || '-' }}</el-descriptions-item>
        <el-descriptions-item label="发布时间">{{ detail.publishTime }}</el-descriptions-item>
        <el-descriptions-item label="过期时间">{{ detail.expireTime }}</el-descriptions-item>
        <el-descriptions-item label="发布者昵称">{{ detail.publisherNickname }}</el-descriptions-item>
        <el-descriptions-item label="发布者用户名">{{ detail.publisherUsername || '-' }}</el-descriptions-item>
        <el-descriptions-item label="性别要求">{{ detail.requireSex || '不限' }}</el-descriptions-item>
        <el-descriptions-item label="取件地址" :span="2">{{ detail.pickupAddress || '-' }}</el-descriptions-item>
        <el-descriptions-item label="送达地址" :span="2">{{ detail.deliveryAddress || '-' }}</el-descriptions-item>
        <el-descriptions-item label="公开描述" :span="2">{{ detail.publicDesc }}</el-descriptions-item>
        <el-descriptions-item v-if="detail.privateNote" label="私密备注" :span="2">
          {{ detail.privateNote }}
        </el-descriptions-item>
        <el-descriptions-item label="联系人">{{ detail.contactName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ detail.contactPhone || '-' }}</el-descriptions-item>
        <el-descriptions-item v-if="detail.pickupCode" label="取件码">{{ detail.pickupCode }}</el-descriptions-item>
      </el-descriptions>

      <div v-if="detail.cancelReason" class="cancel-info anim-cancel">
        <el-alert :title="'取消原因：' + detail.cancelReason" type="warning" show-icon :closable="false" />
      </div>

      <div v-if="detail.imageUrls && detail.imageUrls.length > 0" class="image-section">
        <h4>任务图片</h4>
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
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { Picture } from '@element-plus/icons-vue'
import { getTaskDetail } from '@/api/tasks'
import { TASK_STATUS } from '@/utils/constants'

const route = useRoute()
const loading = ref(false)
const entered = ref(false)
const detail = reactive<any>({})

function statusTag(s: number) {
  const map: Record<number, string> = { 1: 'info', 2: '', 3: 'warning', 4: '', 5: 'success', 6: 'danger' }
  return map[s] ?? 'info'
}

function formatReward(val: any) {
  if (val == null) return '0.00'
  return Number(val).toFixed(2)
}

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

.entered .anim-cancel {
  animation: slideInLeft 0.35s 0.15s var(--ease-out) both;
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
