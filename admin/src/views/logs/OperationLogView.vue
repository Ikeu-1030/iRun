<template>
  <div class="page">
    <el-card>
      <el-form :inline="true" :model="query" class="search-form">
        <el-form-item label="操作模块">
          <el-select v-model="query.module" placeholder="全部" clearable style="width: 140px">
            <el-option label="用户管理" value="用户管理" />
            <el-option label="任务管理" value="任务管理" />
            <el-option label="订单管理" value="订单管理" />
            <el-option label="跑腿员管理" value="跑腿员管理" />
            <el-option label="员工管理" value="员工管理" />
            <el-option label="消息管理" value="消息管理" />
            <el-option label="财务" value="财务" />
          </el-select>
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
        <el-table-column type="expand">
          <template #default="{ row }">
            <div class="expand-detail" v-if="row.requestParams">
              <h4>请求参数</h4>
              <pre class="params-json">{{ formatJson(row.requestParams) }}</pre>
            </div>
            <div v-else class="expand-empty">暂无请求参数记录</div>
          </template>
        </el-table-column>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="adminName" label="操作人" width="100" />
        <el-table-column prop="module" label="模块" width="100" />
        <el-table-column prop="action" label="操作" width="100" />
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column prop="requestMethod" label="请求方式" width="80" />
        <el-table-column prop="requestUrl" label="请求URL" width="160" show-overflow-tooltip />
        <el-table-column prop="ip" label="IP" width="130" />
        <el-table-column prop="createdAt" label="时间" width="180" />
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
import { listLogs } from '@/api/logs'

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const dateRange = ref<[string, string] | null>(null)
const query = reactive({ module: undefined as string | undefined, start: undefined as string | undefined, end: undefined as string | undefined, page: 1, size: 10 })

async function fetchData() {
  loading.value = true
  try {
    const res = await listLogs({ module: query.module, start: query.start, end: query.end, page: query.page, size: query.size }) as any
    tableData.value = res.records
    total.value = res.total
  } finally { loading.value = false }
}

const SENSITIVE_KEYS = ['password', 'payPassword', 'newPassword', 'newPayPassword', 'token', 'refreshToken', 'code', 'idNumber']

function redact(obj: any): any {
  if (obj == null) return obj
  if (Array.isArray(obj)) return obj.map(redact)
  if (typeof obj === 'object') {
    const out: any = {}
    for (const key of Object.keys(obj)) {
      out[key] = SENSITIVE_KEYS.includes(key) ? '***' : redact(obj[key])
    }
    return out
  }
  return obj
}

function formatJson(str: string) {
  try { return JSON.stringify(redact(JSON.parse(str)), null, 2) } catch { return str }
}

function search() {
  query.page = 1
  if (dateRange.value) { query.start = dateRange.value[0]; query.end = dateRange.value[1] }
  else { query.start = undefined; query.end = undefined }
  fetchData()
}
function reset() { query.module = undefined; dateRange.value = null; query.start = undefined; query.end = undefined; search() }

onMounted(fetchData)
</script>

<style scoped>
.search-form { margin-bottom: 16px; }
.search-form :deep(.el-form-item__label) { color: var(--text-secondary); }
.pagination { margin-top: 20px; display: flex; justify-content: center; }
:deep(.el-table) { border-radius: var(--radius-sm); }
:deep(.el-table th) { background: var(--neutral-surface); color: var(--text-secondary); font-weight: 600; }
.expand-detail { padding: 8px 16px; }
.expand-detail h4 { margin: 0 0 8px; font-size: 14px; color: var(--text-primary); }
.params-json { background: var(--neutral-surface); padding: 12px; border-radius: var(--radius-sm); font-size: 13px; line-height: 1.6; white-space: pre-wrap; word-break: break-all; margin: 0; max-height: 300px; overflow-y: auto; }
.expand-empty { padding: 16px; color: var(--text-tertiary); font-size: 13px; text-align: center; }
</style>
