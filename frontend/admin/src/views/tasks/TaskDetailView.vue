<template>
  <div class="page">
    <el-page-header @back="$router.back()" title="返回">
      <template #content>
        <span class="page-title">任务详情 — {{ detail.taskNo }}</span>
      </template>
    </el-page-header>

    <el-card v-loading="loading" class="detail-card">
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

      <div v-if="detail.cancelReason" class="cancel-info">
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
            class="task-image"
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
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { Picture } from '@element-plus/icons-vue'
import { getTaskDetail } from '@/api/tasks'
import { TASK_STATUS } from '@/utils/constants'

const route = useRoute()
const loading = ref(false)
const detail = reactive<any>({})

function statusTag(s: number) {
  const map: Record<number, string> = { 1: '', 2: 'warning', 3: '', 4: 'warning', 5: 'success', 6: 'danger' }
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
})
</script>

<style scoped>
.detail-card { margin-top: 20px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.reward { color: #e67e22; font-weight: 600; font-size: 16px; }
.cancel-info { margin-top: 16px; }
.image-section { margin-top: 20px; }
.image-section h4 { margin-bottom: 12px; }
.image-grid { display: flex; flex-wrap: wrap; gap: 12px; }
.task-image { width: 160px; height: 160px; border-radius: 8px; }
.image-error { width: 100%; height: 100%; display: flex; flex-direction: column; align-items: center; justify-content: center; color: #c0c4cc; font-size: 12px; background: #f5f7fa; gap: 4px; }
</style>
