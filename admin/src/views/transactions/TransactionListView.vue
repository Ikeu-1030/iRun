<template>
  <div class="page">
    <el-card>
      <el-form :inline="true" :model="query" class="search-form">
        <el-form-item label="类型">
          <el-select v-model="query.type" placeholder="全部" clearable style="width: 140px">
            <el-option v-for="(label, key) in TRANSACTION_TYPES" :key="key" :label="label" :value="Number(key)" />
          </el-select>
        </el-form-item>
        <el-form-item label="用户ID">
          <el-input v-model="query.userId" placeholder="用户ID" clearable style="width: 140px" />
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker v-model="dateRange" type="datetimerange" range-separator="至" start-placeholder="开始" end-placeholder="结束" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 360px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">搜索</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="userNickname" label="用户" min-width="100" />
        <el-table-column prop="amount" label="金额" min-width="100" />
        <el-table-column label="类型" min-width="120">
          <template #default="{ row }">{{ TRANSACTION_TYPES[row.type as keyof typeof TRANSACTION_TYPES] }}</template>
        </el-table-column>
        <el-table-column prop="balanceBefore" label="变动前余额" min-width="110" />
        <el-table-column prop="balanceAfter" label="变动后余额" min-width="110" />
        <el-table-column prop="createdAt" label="时间" min-width="160" />
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
import { listTransactions } from '@/api/transactions'
import { TRANSACTION_TYPES } from '@/utils/constants'

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const dateRange = ref<[string, string] | null>(null)
const query = reactive({ type: undefined as number | undefined, userId: undefined as number | undefined, start: undefined as string | undefined, end: undefined as string | undefined, page: 1, size: 10 })

async function fetchData() {
  loading.value = true
  try {
    const res = await listTransactions({
      type: query.type,
      userId: query.userId ? Number(query.userId) : undefined,
      start: query.start,
      end: query.end,
      page: query.page,
      size: query.size
    }) as any
    tableData.value = res.records
    total.value = res.total
  } finally { loading.value = false }
}

function search() {
  query.page = 1
  if (dateRange.value) { query.start = dateRange.value[0]; query.end = dateRange.value[1] }
  else { query.start = undefined; query.end = undefined }
  fetchData()
}
function reset() { query.type = undefined; query.userId = undefined; dateRange.value = null; query.start = undefined; query.end = undefined; search() }

onMounted(fetchData)
</script>

<style scoped>
.search-form { margin-bottom: 16px; }
.search-form :deep(.el-form-item__label) { color: var(--text-secondary); }
.pagination { margin-top: 20px; display: flex; justify-content: center; }
:deep(.el-table) { border-radius: var(--radius-sm); }
:deep(.el-table th) { background: var(--neutral-surface); color: var(--text-secondary); font-weight: 600; }
</style>
