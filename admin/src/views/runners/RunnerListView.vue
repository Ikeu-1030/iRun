<template>
  <div class="page">
    <el-card>
      <el-form :inline="true" :model="query" class="search-form">
        <el-form-item label="关键词">
          <el-input v-model="query.keyword" placeholder="昵称/手机号" clearable @keyup.enter="search" />
        </el-form-item>
        <el-form-item label="认证状态">
          <el-select v-model="query.verifyStatus" placeholder="全部" clearable style="width: 120px">
            <el-option label="未认证" :value="0" />
            <el-option label="审核中" :value="1" />
            <el-option label="已认证" :value="2" />
            <el-option label="认证驳回" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">搜索</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="profileId" label="档案ID" width="80" />
        <el-table-column prop="nickname" label="昵称" width="120" />
        <el-table-column prop="phone" label="手机号" width="140" />
        <el-table-column prop="realName" label="真实姓名" width="100" />
        <el-table-column label="认证状态" width="100">
          <template #default="{ row }"><el-tag :type="certTag(row.verifyStatus)" size="small">{{ certLabel(row.verifyStatus) }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="creditScore" label="信用分" width="80" />
        <el-table-column label="评分" width="70">
          <template #default="{ row }">{{ row.avgRating != null ? Number(row.avgRating).toFixed(1) : '-' }}</template>
        </el-table-column>
        <el-table-column prop="totalOrders" label="历史接单" width="80" />
        <el-table-column prop="successOrders" label="成功完成" width="80" />
        <el-table-column label="累计收入" width="100">
          <template #default="{ row }">¥{{ (row.totalEarnings ?? 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column prop="currentOrders" label="当前接单" width="80" />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag v-if="row.isBanned" type="danger" size="small">已禁止</el-tag>
            <el-tag v-else-if="row.isOnline" type="success" size="small">在线</el-tag>
            <el-tag v-else type="info" size="small">离线</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="220">
          <template #default="{ row }">
            <el-button type="primary" link @click="showDetail(row.profileId)">详情</el-button>
            <el-button v-if="row.verifyStatus === 1" type="success" link @click="review(row, 2)">通过</el-button>
            <el-button v-if="row.verifyStatus === 1" type="danger" link @click="review(row, 3)">驳回</el-button>
            <el-button v-if="row.verifyStatus === 2" :type="row.isBanned ? 'success' : 'danger'" link @click="toggleBan(row)">
              {{ row.isBanned ? '恢复接单' : '禁止接单' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="query.page" v-model:page-size="query.size"
        :total="total" :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next"
        @change="fetchData" class="pagination"
      />
    </el-card>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="跑腿员详情" width="560px">
      <el-descriptions v-if="detail" :column="2" border>
        <el-descriptions-item label="档案ID">{{ detail.profileId }}</el-descriptions-item>
        <el-descriptions-item label="用户ID">{{ detail.userId }}</el-descriptions-item>
        <el-descriptions-item label="昵称">{{ detail.nickname }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ detail.phone }}</el-descriptions-item>
        <el-descriptions-item label="真实姓名">{{ detail.realName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="认证状态">
          <el-tag :type="certTag(detail.verifyStatus)" size="small">{{ certLabel(detail.verifyStatus) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="信用分">{{ detail.creditScore }}</el-descriptions-item>
        <el-descriptions-item label="平均评分">{{ detail.avgRating ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="历史接单">{{ detail.totalOrders }}</el-descriptions-item>
        <el-descriptions-item label="成功完成">{{ detail.successOrders }}</el-descriptions-item>
        <el-descriptions-item label="当前接单数">{{ detail.currentOrders }}</el-descriptions-item>
        <el-descriptions-item label="最大接单数">{{ detail.maxConcurrentOrders }}</el-descriptions-item>
        <el-descriptions-item label="在线状态">
          <el-tag :type="detail.isOnline ? 'success' : 'info'" size="small">{{ detail.isOnline ? '在线' : '离线' }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="接单权限">
          <el-tag :type="detail.isBanned ? 'danger' : 'success'" size="small">{{ detail.isBanned ? '已禁止' : '正常' }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="累计收入" :span="2">
          <span class="income">¥{{ (detail.totalEarnings ?? 0).toFixed(2) }}</span>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <!-- 审核弹窗 -->
    <el-dialog v-model="dialog.visible" :title="dialog.verifyStatus === 2 ? '通过认证' : '驳回认证'" width="400px">
      <el-input v-model="dialog.remark" placeholder="审核备注（选填）" type="textarea" />
      <template #footer>
        <el-button @click="dialog.visible = false">取消</el-button>
        <el-button type="primary" @click="submitReview">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listRunners, getRunnerDetail, reviewRunnerCert, toggleRunnerBan } from '@/api/runners'

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const query = reactive({ keyword: '', verifyStatus: undefined as number | undefined, page: 1, size: 10 })
const dialog = reactive({ visible: false, profileId: 0, verifyStatus: 2, remark: '' })

const detailVisible = ref(false)
const detail = ref<any>(null)

function certLabel(s: number) {
  const map: Record<number, string> = { 0: '未认证', 1: '审核中', 2: '已认证', 3: '认证驳回' }
  return map[s] ?? '-'
}
function certTag(s: number) {
  const map: Record<number, string> = { 0: 'info', 1: 'warning', 2: 'success', 3: 'danger' }
  return map[s] ?? 'info'
}

async function fetchData() {
  loading.value = true
  try {
    const res = await listRunners({ verifyStatus: query.verifyStatus, keyword: query.keyword || undefined, page: query.page, size: query.size }) as any
    tableData.value = res.records
    total.value = res.total
  } finally { loading.value = false }
}

function search() { query.page = 1; fetchData() }
function reset() { query.keyword = ''; query.verifyStatus = undefined; search() }

async function showDetail(profileId: number) {
  detailVisible.value = true
  detail.value = await getRunnerDetail(profileId)
}

function review(row: any, verifyStatus: number) {
  dialog.profileId = row.profileId
  dialog.verifyStatus = verifyStatus
  dialog.remark = ''
  dialog.visible = true
}

async function submitReview() {
  const action = dialog.verifyStatus === 2 ? '通过' : '驳回'
  await ElMessageBox.confirm(`确认${action}该跑腿员的认证？`, '提示', { type: 'warning' })
  await reviewRunnerCert(dialog.profileId, dialog.verifyStatus, dialog.remark || undefined)
  ElMessage.success('审核完成')
  dialog.visible = false
  fetchData()
}

async function toggleBan(row: any) {
  const action = row.isBanned ? '恢复接单' : '禁止接单'
  await ElMessageBox.confirm(`确认${action}跑腿员 "${row.nickname}"？`, '提示', { type: 'warning' })
  await toggleRunnerBan(row.profileId, !row.isBanned)
  ElMessage.success('操作成功')
  fetchData()
}

onMounted(fetchData)
</script>

<style scoped>
.search-form { margin-bottom: 16px; }
.search-form :deep(.el-form-item__label) { color: var(--text-secondary); }
.pagination { margin-top: 20px; display: flex; justify-content: center; }
.income { font-size: 18px; font-weight: bold; color: var(--color-success); }
:deep(.el-table) { border-radius: var(--radius-sm); }
:deep(.el-table th) { background: var(--neutral-surface); color: var(--text-secondary); font-weight: 600; }
</style>
