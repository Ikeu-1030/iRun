<template>
  <div class="page" :class="{ entered }">
    <el-page-header @back="$router.back()" title="返回" class="anim-header">
      <template #content>
        <span class="page-title">用户详情 — #{{ userId || '--' }}</span>
      </template>
    </el-page-header>

    <div class="detail-blocks" v-loading="loading">
      <!-- 基本信息 -->
      <el-card class="detail-block anim-block color-card" style="--anim-order: 0; --card-color: #5B9BD5; --card-bg: #EFF5FB">
        <template #header>
          <span class="card-title-text"><el-icon class="card-title-icon"><InfoFilled /></el-icon>基本信息</span>
        </template>
        <el-descriptions v-if="user" :column="2" border>
          <el-descriptions-item label="ID">{{ user.id }}</el-descriptions-item>
          <el-descriptions-item label="用户名">{{ user.username }}</el-descriptions-item>
          <el-descriptions-item label="头像">
            <el-avatar v-if="user.avatarUrl" :src="user.avatarUrl" :size="60" />
            <span v-else>-</span>
          </el-descriptions-item>
          <el-descriptions-item label="昵称">{{ user.nickname }}</el-descriptions-item>
          <el-descriptions-item label="手机号">{{ isSuperAdmin ? (user.phone || '-') : maskPhone(user.phone) }}</el-descriptions-item>
          <el-descriptions-item label="性别">{{ user.sex || '-' }}</el-descriptions-item>
          <el-descriptions-item label="真实姓名">{{ isSuperAdmin ? (user.realName || '-') : maskName(user.realName) }}</el-descriptions-item>
          <el-descriptions-item label="学号">{{ isSuperAdmin ? (user.studentId || '-') : (user.studentId ? '****' + user.studentId.slice(-3) : '-') }}</el-descriptions-item>
          <el-descriptions-item label="学院">{{ user.campus || '-' }}</el-descriptions-item>
          <el-descriptions-item label="个性签名" :span="2">{{ user.signature || '-' }}</el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- 账户与认证 -->
      <el-card class="detail-block anim-block color-card" style="--anim-order: 1; --card-color: #2EB89E; --card-bg: #EDFAF7">
        <template #header>
          <span class="card-title-text"><el-icon class="card-title-icon"><Checked /></el-icon>账户与认证</span>
        </template>
        <el-descriptions v-if="user" :column="2" border>
          <el-descriptions-item label="余额">
            <span class="balance">¥{{ (user.balance ?? 0).toFixed(2) }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="认证状态">
            <span class="status-tag" :style="{ color: certStyle(user.isCertify).color, background: certStyle(user.isCertify).bgColor }">{{ certStyle(user.isCertify).label }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="跑腿员认证">
            <span class="status-tag" :style="{ color: certStyle(user.verifyStatus).color, background: certStyle(user.verifyStatus).bgColor }">{{ certStyle(user.verifyStatus).label }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="账户状态">
            <span class="status-tag" :style="{ color: accountStyle(user.status).color, background: accountStyle(user.status).bgColor }">{{ accountStyle(user.status).label }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="最后登录">{{ user.lastLoginTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="注册时间">{{ user.createdAt }}</el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- 认证材料 -->
      <el-card v-if="user && (certifyImages.length || user.certifyRemark)" class="detail-block anim-block color-card" style="--anim-order: 2; --card-color: #C8925D; --card-bg: #FDF3EB">
        <template #header>
          <span class="card-title-text"><el-icon class="card-title-icon"><DataBoard /></el-icon>认证材料</span>
        </template>
        <div v-if="certifyImages.length" class="certify-section">
          <div class="image-grid">
            <el-image
              v-for="(url, i) in certifyImages"
              :key="i"
              :src="url"
              :preview-src-list="certifyImages"
              :initial-index="i"
              fit="cover"
              class="task-image anim-img"
              :style="{ animationDelay: (0.2 + i * 0.07) + 's' }"
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
        <el-alert v-if="user.certifyRemark" :title="'备注：' + user.certifyRemark" type="info" show-icon :closable="false" class="certify-remark" />
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, computed, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { Picture, InfoFilled, Checked, DataBoard } from '@element-plus/icons-vue'
import { getUserDetail } from '@/api/users'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const authStore = useAuthStore()
const isSuperAdmin = computed(() => authStore.adminInfo?.role === 1)
const userId = ref(Number(route.params.id))
const user = ref<any>(null)
const loading = ref(false)
const entered = ref(false)

function maskPhone(phone: string | null | undefined): string {
  if (!phone || phone.length < 7) return phone || '-'
  return phone.slice(0, 3) + '****' + phone.slice(-4)
}

function maskName(name: string | null | undefined): string {
  if (!name || name.length < 2) return name || '-'
  return name.slice(0, 1) + '*'
}

const certifyImages = computed(() => {
  if (!user.value?.certifyImg) return []
  return user.value.certifyImg.split(',').map((u: string) => u.trim()).filter(Boolean)
})

const certStyleMap: Record<number, { color: string; bgColor: string; label: string }> = {
  0: { color: '#8492A6', bgColor: '#EFF2F7', label: '未认证' },
  1: { color: '#C8925D', bgColor: '#FDF3EB', label: '审核中' },
  2: { color: '#2EB89E', bgColor: '#EDFAF7', label: '已认证' },
  3: { color: '#E87474', bgColor: '#FEF0F0', label: '认证驳回' },
}

function certStyle(s: number) {
  return certStyleMap[s] ?? { color: '#8492A6', bgColor: '#EFF2F7', label: '-' }
}

function accountStyle(s: number) {
  return s === 1
    ? { color: '#2EB89E', bgColor: '#EDFAF7', label: '正常' }
    : { color: '#E87474', bgColor: '#FEF0F0', label: '禁用' }
}

onMounted(async () => {
  loading.value = true
  try {
    user.value = await getUserDetail(userId.value)
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

.card-title-text {
  font-weight: 600;
  color: var(--text-primary);
}

/* ===== 独立配色卡片 ===== */
.color-card {
  border-left: 3px solid var(--card-color);
}

.color-card :deep(.el-card__header) {
  background: var(--card-bg);
  border-bottom-color: var(--card-color);
  border-bottom-width: 1px;
  transition: background 0.3s ease;
}

.card-title-icon {
  margin-right: 6px;
  font-size: 15px;
  vertical-align: -2px;
  color: var(--text-secondary);
}

.balance {
  font-size: 15px;
  font-weight: 600;
  color: var(--brand-accent);
}

.status-tag {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 10px;
  font-size: 13px;
  font-weight: 500;
}

.certify-section {
  margin-bottom: 16px;
}

.certify-remark {
  margin-top: 16px;
}

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
