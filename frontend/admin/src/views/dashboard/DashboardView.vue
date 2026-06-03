<template>
  <div class="dashboard">
    <el-row :gutter="16" class="stats-row">
      <el-col :span="4" v-for="card in statCards" :key="card.label">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-label">{{ card.label }}</div>
          <div class="stat-value">{{ card.value }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="12">
        <el-card>
          <template #header>近7天新增用户趋势</template>
          <v-chart :option="userTrendOption" style="height: 300px" autoresize />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>近7天收入趋势</template>
          <v-chart :option="revenueTrendOption" style="height: 300px" autoresize />
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="12">
        <el-card>
          <template #header>任务分类占比</template>
          <v-chart :option="categoryPieOption" style="height: 300px" autoresize />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { BarChart, LineChart, PieChart } from 'echarts/charts'
import { GridComponent, LegendComponent, TitleComponent, TooltipComponent } from 'echarts/components'
import { getDashboard } from '@/api/dashboard'

use([CanvasRenderer, BarChart, LineChart, PieChart, GridComponent, LegendComponent, TitleComponent, TooltipComponent])

interface DashboardData {
  userCount: number; taskCount: number; orderCount: number
  runnerCount: number; onlineRunnerCount: number
  todayRevenue: number; todayNewUsers: number; todayNewTasks: number; todayCompletedOrders: number
  userTrend: { date: string; value: number }[]
  revenueTrend: { date: string; value: number }[]
  taskCategories: { name: string; value: number }[]
}

const data = ref<DashboardData | null>(null)

const statCards = computed(() => [
  { label: '用户总数', value: data.value?.userCount ?? 0 },
  { label: '今日新增用户', value: data.value?.todayNewUsers ?? 0 },
  { label: '任务总数', value: data.value?.taskCount ?? 0 },
  { label: '在线跑腿员', value: data.value?.onlineRunnerCount ?? 0 },
  { label: '今日交易额(元)', value: (data.value?.todayRevenue ?? 0).toFixed(2) },
  { label: '今日完成订单', value: data.value?.todayCompletedOrders ?? 0 }
])

const userTrendOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  xAxis: { type: 'category', data: data.value?.userTrend?.map(t => t.date) ?? [] },
  yAxis: { type: 'value' },
  series: [{ data: data.value?.userTrend?.map(t => t.value) ?? [], type: 'line', smooth: true, areaStyle: { opacity: 0.3 } }]
}))

const revenueTrendOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  xAxis: { type: 'category', data: data.value?.revenueTrend?.map(t => t.date) ?? [] },
  yAxis: { type: 'value', name: '元' },
  series: [{ data: data.value?.revenueTrend?.map(t => t.value) ?? [], type: 'bar', itemStyle: { color: '#67C23A' } }]
}))

const categoryPieOption = computed(() => ({
  tooltip: { trigger: 'item' },
  legend: { orient: 'vertical', left: 'left' },
  series: [{
    type: 'pie', radius: ['40%', '70%'],
    data: data.value?.taskCategories?.map(c => ({ name: c.name, value: c.value })) ?? [],
    emphasis: { itemStyle: { shadowBlur: 10, shadowOffsetX: 0, shadowColor: 'rgba(0, 0, 0, 0.5)' } }
  }]
}))

onMounted(async () => {
  data.value = await getDashboard() as DashboardData
})
</script>

<style scoped>
.stats-row { margin-bottom: 0; }
.stat-card { text-align: center; }
.stat-label { font-size: 14px; color: #909399; margin-bottom: 8px; }
.stat-value { font-size: 28px; font-weight: bold; color: #303133; }
</style>
