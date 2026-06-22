<template>
  <div class="page">
    <el-card>
      <el-form :inline="true" :model="query" class="search-form">
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 140px">
            <el-option v-for="(label, key) in TASK_STATUS" :key="key" :label="label" :value="Number(key)" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">搜索</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="taskNo" label="任务编号" width="160" />
        <el-table-column label="任务类型" width="130">
          <template #default="{ row }">
            <span
              class="task-type-label"
              :style="{ color: taskTypeStyle(row.type).color, background: taskTypeStyle(row.type).bgColor }"
            >
              <el-icon class="task-type-label-icon"><component :is="taskTypeStyle(row.type).icon" /></el-icon>
              {{ taskTypeLabel(row.type) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="publicDesc" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column prop="publisherNickname" label="发布者" width="100" />
        <el-table-column label="报酬" width="100">
          <template #default="{ row }"><span class="reward-inline">¥{{ Number(row.reward).toFixed(2) }}</span></template>
        </el-table-column>
        <el-table-column prop="pickupAddress" label="取件地址" width="140" show-overflow-tooltip />
        <el-table-column prop="deliveryAddress" label="送达地址" width="140" show-overflow-tooltip />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)" size="small">{{ TASK_STATUS[row.status as keyof typeof TASK_STATUS] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="publishTime" label="发布时间" width="170" />
        <el-table-column label="操作" min-width="220">
          <template #default="{ row }">
            <el-button type="primary" link @click="$router.push(`/tasks/${row.taskId}`)">详情</el-button>
            <el-popconfirm
              v-if="row.status !== 5 && row.status !== 6"
              title="确定修改任务状态？"
              @confirm="updateStatus(row)"
            >
              <template #reference>
                <el-button type="warning" link>修改状态</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="query.page" v-model:page-size="query.size"
        :total="total" :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next"
        @change="fetchData" class="pagination"
      />
    </el-card>

    <el-dialog v-model="statusDialog.visible" title="修改任务状态" width="420px">
      <div class="status-change-info">
        <span class="status-from" :style="{ color: statusStyle(statusDialog.currentStatus).color, background: statusStyle(statusDialog.currentStatus).bgColor }">
          {{ TASK_STATUS[statusDialog.currentStatus as keyof typeof TASK_STATUS] }}
        </span>
        <el-icon class="status-arrow"><Right /></el-icon>
        <el-select v-model="statusDialog.newStatus" placeholder="选择目标状态" style="width:180px">
          <el-option
            v-for="s in validNextStatuses"
            :key="s"
            :label="TASK_STATUS[s as keyof typeof TASK_STATUS]"
            :value="s"
          >
            <span class="status-option" :style="{ color: statusStyle(s).color }">
              {{ TASK_STATUS[s as keyof typeof TASK_STATUS] }}
            </span>
          </el-option>
        </el-select>
      </div>
      <div v-if="validNextStatuses.length === 0" class="status-no-next">
        <el-icon><WarningFilled /></el-icon>
        <span>当前状态为终态，无法变更</span>
      </div>
      <template #footer>
        <el-button @click="statusDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="confirmStatus" :disabled="saving || validNextStatuses.length === 0">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Box, KnifeFork, Document, ShoppingCart, MoreFilled, Right, WarningFilled } from '@element-plus/icons-vue'
import { listTasks, updateTaskStatus } from '@/api/tasks'
import { TASK_STATUS, TASK_STATE_MACHINE } from '@/utils/constants'

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const query = reactive({ status: undefined as number | undefined, page: 1, size: 10 })
const statusDialog = reactive({ visible: false, taskId: 0, currentStatus: 1, newStatus: 1 })
const saving = ref(false)

/** 当前任务状态可转移到的合法目标状态列表 */
const validNextStatuses = computed(() => {
  return TASK_STATE_MACHINE[statusDialog.currentStatus] ?? []
})

const taskTypeLabelMap: Record<string, string> = {
  '代取快递': '代取快递', 'daiqukuaidi': '代取快递',
  '代拿餐食': '代拿餐食', 'dana canshi': '代拿餐食',
  '校内代办': '校内代办', 'xiaonei daiban': '校内代办',
  '代购物品': '代购物品', 'daigou wupin': '代购物品',
  '通用代办': '通用代办', 'tongyong daiban': '通用代办',
}

const taskTypeStyleMap: Record<string, { color: string; bgColor: string; icon: any }> = {
  '代取快递': { color: '#E8734A', bgColor: '#FFF2ED', icon: Box },
  'daiqukuaidi': { color: '#E8734A', bgColor: '#FFF2ED', icon: Box },
  '代拿餐食': { color: '#2EB89E', bgColor: '#EDFAF7', icon: KnifeFork },
  'dana canshi': { color: '#2EB89E', bgColor: '#EDFAF7', icon: KnifeFork },
  '校内代办': { color: '#5B9BD5', bgColor: '#EFF5FB', icon: Document },
  'xiaonei daiban': { color: '#5B9BD5', bgColor: '#EFF5FB', icon: Document },
  '代购物品': { color: '#8B6BAE', bgColor: '#F6F1FA', icon: ShoppingCart },
  'daigou wupin': { color: '#8B6BAE', bgColor: '#F6F1FA', icon: ShoppingCart },
  '通用代办': { color: '#C8925D', bgColor: '#FDF3EB', icon: MoreFilled },
  'tongyong daiban': { color: '#C8925D', bgColor: '#FDF3EB', icon: MoreFilled },
}

function taskTypeLabel(type: string) { return taskTypeLabelMap[type] || type }
function taskTypeStyle(type: string) {
  return taskTypeStyleMap[type] ?? { color: '#909399', bgColor: '#F5F5F5', icon: Document }
}

function statusTag(s: number) {
  const map: Record<number, string> = { 1: 'info', 2: '', 3: 'warning', 4: '', 5: 'success', 6: 'danger' }
  return map[s] ?? 'info'
}

async function fetchData() {
  loading.value = true
  try {
    const res = await listTasks({ status: query.status, page: query.page, size: query.size }) as any
    tableData.value = res.records
    total.value = res.total
  } finally { loading.value = false }
}

function search() { query.page = 1; fetchData() }
function reset() { query.status = undefined; search() }

const statusStyleMap: Record<number, { color: string; bgColor: string }> = {
  1: { color: '#8492A6', bgColor: '#EFF2F7' },
  2: { color: '#C8925D', bgColor: '#FDF3EB' },
  3: { color: '#5B9BD5', bgColor: '#EFF5FB' },
  4: { color: '#8B6BAE', bgColor: '#F6F1FA' },
  5: { color: '#2EB89E', bgColor: '#EDFAF7' },
  6: { color: '#E87474', bgColor: '#FEF0F0' },
}

function statusStyle(s: number) {
  return statusStyleMap[s] ?? { color: '#8492A6', bgColor: '#EFF2F7' }
}

function updateStatus(row: any) {
  statusDialog.taskId = row.taskId
  statusDialog.currentStatus = row.status
  statusDialog.newStatus = TASK_STATE_MACHINE[row.status]?.[0] ?? row.status
  statusDialog.visible = true
}

async function confirmStatus() {
  if (saving.value) return
  saving.value = true
  try {
    await updateTaskStatus(statusDialog.taskId, statusDialog.newStatus)
    ElMessage.success('状态已更新')
    statusDialog.visible = false
    fetchData()
  } finally {
    saving.value = false
  }
}

onMounted(fetchData)
</script>

<style scoped>
.search-form { margin-bottom: 16px; }
.search-form :deep(.el-form-item__label) { color: var(--text-secondary); }
.pagination { margin-top: 20px; display: flex; justify-content: center; }
:deep(.el-table) { border-radius: var(--radius-sm); }
:deep(.el-table th) { background: var(--neutral-surface); color: var(--text-secondary); font-weight: 600; }

.task-type-label {
  display: inline-flex;
  align-items: center;
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 12px;
  font-weight: 500;
}

.task-type-label-icon {
  margin-right: 2px;
  font-size: 12px;
}

.reward-inline {
  color: var(--brand-accent);
  font-weight: 500;
}

/* ===== 状态修改对话框 ===== */
.status-change-info {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 8px 0 4px;
}

.status-from {
  display: inline-block;
  padding: 4px 14px;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 500;
}

.status-arrow {
  color: var(--text-placeholder);
  font-size: 18px;
}

.status-option {
  font-weight: 500;
}

.status-no-next {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 12px;
  margin-top: 8px;
  color: var(--text-secondary);
  font-size: 13px;
  background: var(--neutral-surface);
  border-radius: var(--radius-sm);
}
</style>
