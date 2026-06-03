<template>
  <div class="page" ref="pageRef">
    <el-button @click="$router.back()" style="margin-bottom:16px">← 返回</el-button>
    <el-card v-loading="loading">
      <template #header>
        <span>用户详情 #{{ userId }}</span>
      </template>
      <el-descriptions v-if="user" :column="2" border>
        <el-descriptions-item label="ID">{{ user.id }}</el-descriptions-item>
        <el-descriptions-item label="用户名">{{ user.username }}</el-descriptions-item>
        <el-descriptions-item label="头像">
          <el-avatar v-if="user.avatarUrl" :src="user.avatarUrl" :size="60" />
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item label="昵称">{{ user.nickname }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ user.phone }}</el-descriptions-item>
        <el-descriptions-item label="性别">{{ user.sex || '-' }}</el-descriptions-item>
        <el-descriptions-item label="真实姓名">{{ user.realName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="学号">{{ user.studentId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="学院">{{ user.campus || '-' }}</el-descriptions-item>
        <el-descriptions-item label="个性签名">{{ user.signature || '-' }}</el-descriptions-item>
        <el-descriptions-item label="余额">{{ user.balance }}</el-descriptions-item>
        <el-descriptions-item label="身份">
          <el-tag size="small">{{ user.verifyStatus === 2 ? '跑腿员' : '普通用户' }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="认证状态">
          <el-tag :type="certTag(user.isCertify)" size="small">{{ certLabel(user.isCertify) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="跑腿员认证">
          <el-tag :type="certTag(user.verifyStatus)" size="small">{{ certLabel(user.verifyStatus) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="账户状态">
          <el-tag :type="user.status === 1 ? 'success' : 'danger'" size="small">{{ user.status === 1 ? '正常' : '禁用' }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="最后登录">{{ user.lastLoginTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="注册时间">{{ user.createdAt }}</el-descriptions-item>
        <el-descriptions-item v-if="user.certifyImg" label="认证照片" :span="2">
          <el-image :src="user.certifyImg" style="max-width:300px" :preview-src-list="[user.certifyImg]" />
        </el-descriptions-item>
        <el-descriptions-item v-if="user.certifyRemark" label="认证备注" :span="2">{{ user.certifyRemark }}</el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import gsap from 'gsap'
import { getUserDetail } from '@/api/users'

const route = useRoute()
const userId = ref(Number(route.params.id))
const user = ref<any>(null)
const loading = ref(false)
const pageRef = ref<HTMLElement>()

function certLabel(s: number) {
  const map: Record<number, string> = { 0: '未认证', 1: '审核中', 2: '已认证', 3: '认证驳回' }
  return s != null ? map[s] ?? '-' : '-'
}
function certTag(s: number) {
  const map: Record<number, string> = { 0: 'info', 1: 'warning', 2: 'success', 3: 'danger' }
  return s != null ? map[s] ?? 'info' : 'info'
}

onMounted(async () => {
  loading.value = true
  try {
    user.value = await getUserDetail(userId.value)
    await nextTick()
    runEntrance()
  } finally { loading.value = false }
})

function runEntrance() {
  const el = pageRef.value
  if (!el) return

  const tl = gsap.timeline({ defaults: { ease: 'power3.out' } })

  // Back button
  const btn = el.querySelector('.el-button') as HTMLElement
  if (btn) tl.from(btn, { x: -16, opacity: 0, duration: 0.4 }, 0)

  // Card
  const card = el.querySelector('.el-card') as HTMLElement
  if (card) tl.from(card, { y: 24, opacity: 0, duration: 0.55 }, '-=0.25')

  // Descriptions rows stagger
  const rows = el.querySelectorAll('.el-descriptions__body tbody tr')
  if (rows.length) {
    tl.from(rows, {
      x: -8, opacity: 0, duration: 0.3, stagger: 0.04, ease: 'power3.out',
    }, '-=0.2')
  }
}
</script>
