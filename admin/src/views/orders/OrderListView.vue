<template>
  <div class="page">
    <el-card>
      <el-form :inline="true" :model="query" class="search-form">
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 140px">
            <el-option v-for="(label, key) in ORDER_STATUS" :key="key" :label="label" :value="Number(key)" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">搜索</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="orderId" label="订单ID" width="80" />
        <el-table-column prop="taskNo" label="任务编号" width="160" />
        <el-table-column prop="publicDesc" label="任务描述" min-width="200" show-overflow-tooltip />
        <el-table-column prop="publisherNickname" label="发布者" width="120" />
        <el-table-column prop="runnerNickname" label="跑腿员" width="120" />
        <el-table-column label="报酬" width="100">
          <template #default="{ row }">¥{{ Number(row.reward ?? 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)" size="small">{{ ORDER_STATUS[row.status as keyof typeof ORDER_STATUS] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="acceptTime" label="接单时间" width="170" />
        <el-table-column prop="expectFinishTime" label="预计送达" width="170" />
        <el-table-column label="操作" min-width="100">
          <template #default="{ row }">
            <el-button type="primary" link @click="$router.push(`/orders/${row.orderId}`)">详情</el-button>
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
import { listOrders } from '@/api/orders'
import { ORDER_STATUS } from '@/utils/constants'

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const query = reactive({ status: undefined as number | undefined, page: 1, size: 10 })

function statusTag(s: number) {
  const map: Record<number, string> = { 1: 'info', 2: 'warning', 3: '', 4: 'success', 5: 'danger' }
  return map[s] ?? 'info'
}

async function fetchData() {
  loading.value = true
  try {
    const res = await listOrders({ status: query.status, page: query.page, size: query.size }) as any
    tableData.value = res.records
    total.value = res.total
  } finally { loading.value = false }
}

function search() { query.page = 1; fetchData() }
function reset() { query.status = undefined; search() }

onMounted(fetchData)
</script>

<style scoped>
.search-form { margin-bottom: 16px; }
.search-form :deep(.el-form-item__label) { color: var(--text-secondary); }
.pagination { margin-top: 20px; display: flex; justify-content: center; }
:deep(.el-table) { border-radius: var(--radius-sm); }
:deep(.el-table th) { background: var(--neutral-surface); color: var(--text-secondary); font-weight: 600; }
</style>
