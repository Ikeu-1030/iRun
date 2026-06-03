<template>
  <div class="page">
    <el-card>
      <el-form :inline="true" :model="query" class="search-form">
        <el-form-item label="关键词">
          <el-input v-model="query.keyword" placeholder="用户名/手机号/昵称" clearable @keyup.enter="search" />
        </el-form-item>
        <el-form-item label="账户状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
            <el-option label="正常" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="认证状态">
          <el-select v-model="query.isCertify" placeholder="全部" clearable style="width: 120px">
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
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="nickname" label="昵称" width="150" />
        <el-table-column prop="phone" label="手机号" width="140" />
        <el-table-column label="认证状态" width="100">
          <template #default="{ row }">
            <el-tag :type="certifyTagType(row.isCertify)" size="small">{{ certifyLabel(row.isCertify) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="账户状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">{{ row.status === 1 ? '正常' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="balance" label="余额" width="100" />
        <el-table-column prop="lastLoginTime" label="最后登录" width="180" />
        <el-table-column prop="createdAt" label="注册时间" width="180" />
        <el-table-column label="操作" min-width="180">
          <template #default="{ row }">
            <el-button type="primary" link @click="$router.push(`/users/${row.id}`)">详情</el-button>
            <el-button v-if="row.status === 1" type="warning" link @click="toggleStatus(row, false)">封禁</el-button>
            <el-button v-else type="success" link @click="toggleStatus(row, true)">解封</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="query.page" v-model:page-size="query.size"
        :total="total" :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next"
        @change="fetchData" class="pagination"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listUsers, toggleUserStatus } from '@/api/users'

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)

const query = reactive({ keyword: '', status: undefined as number | undefined, isCertify: undefined as number | undefined, page: 1, size: 10 })

function certifyLabel(s: number) {
  const map: Record<number, string> = { 0: '未认证', 1: '审核中', 2: '已认证', 3: '认证驳回' }
  return map[s] ?? '-'
}
function certifyTagType(s: number) {
  const map: Record<number, string> = { 0: 'info', 1: 'warning', 2: 'success', 3: 'danger' }
  return map[s] ?? 'info'
}

async function fetchData() {
  loading.value = true
  try {
    const res = await listUsers({
      keyword: query.keyword || undefined,
      status: query.status,
      isCertify: query.isCertify,
      page: query.page,
      size: query.size
    }) as any
    tableData.value = res.records
    total.value = res.total
  } finally { loading.value = false }
}

function search() { query.page = 1; fetchData() }
function reset() { query.keyword = ''; query.status = undefined; query.isCertify = undefined; search() }

async function toggleStatus(row: any, enabled: boolean) {
  await ElMessageBox.confirm(`确认${enabled ? '解封' : '封禁'}用户 "${row.nickname || row.username}"？`, '提示', { type: 'warning' })
  await toggleUserStatus(row.id, enabled)
  ElMessage.success('操作成功')
  fetchData()
}

onMounted(fetchData)
</script>

<style scoped>
.toolbar { display: flex; justify-content: flex-end; margin-bottom: 12px; }
.search-form { margin-bottom: 16px; }
.search-form :deep(.el-form-item__label) { color: var(--text-secondary); }
.pagination { margin-top: 20px; display: flex; justify-content: center; }
:deep(.el-table) { border-radius: var(--radius-sm); }
:deep(.el-table th) { background: var(--neutral-surface); color: var(--text-secondary); font-weight: 600; }
</style>
