<template>
  <div class="dashboard">
    <el-row :gutter="16" class="stats-row">
      <el-col :span="4" v-for="(card, idx) in statCards" :key="card.label">
        <el-card shadow="hover" class="stat-card" :ref="(el: any) => statRefs[idx] = el">
          <div class="stat-icon" :style="{ background: card.bg }">
            <el-icon :size="20"><component :is="card.icon" /></el-icon>
          </div>
          <div class="stat-body">
            <div class="stat-label">{{ card.label }}</div>
            <div class="stat-value" :data-target="card.raw" :ref="(el: any) => countRefs[idx] = el">0</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="chart-row">
      <el-col :span="12">
        <el-card class="chart-card" ref="chartCard1Ref">
          <template #header><span class="chart-title">近7天新增用户趋势</span></template>
          <v-chart :option="userTrendOption" style="height: 300px" autoresize />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card class="chart-card" ref="chartCard2Ref">
          <template #header><span class="chart-title">近7天收入趋势</span></template>
          <v-chart :option="revenueTrendOption" style="height: 300px" autoresize />
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="chart-row">
      <el-col :span="12">
        <el-card class="chart-card" ref="chartCard3Ref">
          <template #header><span class="chart-title">任务分类占比</span></template>
          <v-chart :option="categoryPieOption" style="height: 300px" autoresize />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card class="chart-card" ref="chartCard4Ref">
          <template #header><span class="chart-title">订单状态分布</span></template>
          <v-chart :option="orderStatusOption" style="height: 300px" autoresize />
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
import gsap from 'gsap'
import { getDashboard } from '@/api/dashboard'

use([CanvasRenderer, BarChart, LineChart, PieChart, GridComponent, LegendComponent, TitleComponent, TooltipComponent])

interface DashboardData {
  userCount: number; taskCount: number; orderCount: number
  runnerCount: number; onlineRunnerCount: number
  todayRevenue: number; todayNewUsers: number; todayNewTasks: number; todayCompletedOrders: number
  userTrend: { date: string; value: number }[]
  revenueTrend: { date: string; value: number }[]
  taskCategories: { name: string; value: number }[]
  orderStatusDistribution: { name: string; value: number }[]
}

const data = ref<DashboardData | null>(null)

const statRefs = ref<(HTMLElement | null)[]>([])
const countRefs = ref<(HTMLElement | null)[]>([])
const chartCard1Ref = ref<HTMLElement>()
const chartCard2Ref = ref<HTMLElement>()
const chartCard3Ref = ref<HTMLElement>()
const chartCard4Ref = ref<HTMLElement>()

const CHART_COLORS = ['#FF6B4A', '#2EC4B6', '#FFB347', '#3B82F6', '#EF4444']

const statCards = computed(() => [
  {
    label: '用户总数', raw: data.value?.userCount ?? 0,
    value: formatNum(data.value?.userCount ?? 0),
    icon: 'User', bg: '#FFF0ED', color: '#FF6B4A',
  },
  {
    label: '今日新增用户', raw: data.value?.todayNewUsers ?? 0,
    value: formatNum(data.value?.todayNewUsers ?? 0),
    icon: 'UserFilled', bg: '#D4F5F0', color: '#2EC4B6',
  },
  {
    label: '任务总数', raw: data.value?.taskCount ?? 0,
    value: formatNum(data.value?.taskCount ?? 0),
    icon: 'List', bg: '#FFF7ED', color: '#FFB347',
  },
  {
    label: '在线跑腿员', raw: data.value?.onlineRunnerCount ?? 0,
    value: formatNum(data.value?.onlineRunnerCount ?? 0),
    icon: 'Van', bg: '#E2EEFE', color: '#3B82F6',
  },
  {
    label: '今日交易额(元)', raw: data.value?.todayRevenue ?? 0,
    value: (data.value?.todayRevenue ?? 0).toFixed(2),
    icon: 'Money', bg: '#FDE2E2', color: '#EF4444',
  },
  {
    label: '今日完成订单', raw: data.value?.todayCompletedOrders ?? 0,
    value: formatNum(data.value?.todayCompletedOrders ?? 0),
    icon: 'Document', bg: '#F5F5F0', color: '#5E5D58',
  },
])

function formatNum(n: number) {
  if (n >= 10000) return (n / 10000).toFixed(1) + 'w'
  if (n >= 1000) return (n / 1000).toFixed(1) + 'k'
  return String(n)
}

const userTrendOption = computed(() => ({
  color: [CHART_COLORS[0]],
  tooltip: { trigger: 'axis', backgroundColor: '#fff', borderColor: '#e8e6e0', textStyle: { color: '#1C1B1A' } },
  grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
  xAxis: { type: 'category', data: data.value?.userTrend?.map(t => t.date) ?? [], axisLine: { lineStyle: { color: '#e8e6e0' } } },
  yAxis: { type: 'value', splitLine: { lineStyle: { color: '#f0efec' } } },
  series: [{
    data: data.value?.userTrend?.map(t => t.value) ?? [],
    type: 'line', smooth: true,
    areaStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: 'rgba(255,107,74,0.25)' }, { offset: 1, color: 'rgba(255,107,74,0.02)' }] } },
  }],
}))

const revenueTrendOption = computed(() => ({
  color: [CHART_COLORS[1]],
  tooltip: { trigger: 'axis', backgroundColor: '#fff', borderColor: '#e8e6e0', textStyle: { color: '#1C1B1A' } },
  grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
  xAxis: { type: 'category', data: data.value?.revenueTrend?.map(t => t.date) ?? [], axisLine: { lineStyle: { color: '#e8e6e0' } } },
  yAxis: { type: 'value', name: '元', splitLine: { lineStyle: { color: '#f0efec' } } },
  series: [{
    data: data.value?.revenueTrend?.map(t => t.value) ?? [],
    type: 'bar', barWidth: '50%',
    itemStyle: { borderRadius: [4, 4, 0, 0], color: '#2EC4B6' },
  }],
}))

const categoryPieOption = computed(() => ({
  color: CHART_COLORS,
  tooltip: { trigger: 'item', backgroundColor: '#fff', borderColor: '#e8e6e0', textStyle: { color: '#1C1B1A' } },
  legend: { orient: 'vertical', left: 'left', textStyle: { color: '#5E5D58' } },
  series: [{
    type: 'pie', radius: ['45%', '72%'],
    data: data.value?.taskCategories?.map(c => ({ name: c.name, value: c.value })) ?? [],
    emphasis: { itemStyle: { shadowBlur: 10, shadowOffsetX: 0, shadowColor: 'rgba(0, 0, 0, 0.15)' } },
    label: { show: false },
  }],
}))

const orderStatusOption = computed(() => ({
  color: CHART_COLORS,
  tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' }, backgroundColor: '#fff', borderColor: '#e8e6e0', textStyle: { color: '#1C1B1A' } },
  grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
  xAxis: { type: 'category', data: data.value?.orderStatusDistribution?.map(d => d.name) ?? [], axisLine: { lineStyle: { color: '#e8e6e0' } } },
  yAxis: { type: 'value', minInterval: 1, splitLine: { lineStyle: { color: '#f0efec' } } },
  series: [{
    data: data.value?.orderStatusDistribution?.map((d, i) => ({ value: d.value, itemStyle: { color: CHART_COLORS[i % CHART_COLORS.length], borderRadius: [4, 4, 0, 0] } })) ?? [],
    type: 'bar', barWidth: '50%',
    label: { show: true, position: 'top', color: '#5E5D58' },
  }],
}))

function statusColor(name: string) {
  const map: Record<string, string> = { '待取货': '#F59E0B', '配送中': '#3B82F6', '待确认': '#2EC4B6', '已完成': '#8F8D88', '已取消': '#EF4444' }
  return map[name] || '#3B82F6'
}

async function loadData() {
  data.value = await getDashboard() as DashboardData
  await Promise.resolve() // nextTick

  // GSAP 卡片入场
  const validStats = statRefs.value.filter(Boolean)
  if (validStats.length) {
    gsap.from(validStats, {
      y: 24, opacity: 0, duration: 0.6, stagger: 0.08, ease: 'power3.out',
    })
  }

  // 数字递增动画
  statCards.value.forEach((card, i) => {
    const el = countRefs.value[i]
    if (el && card.raw > 0) {
      const obj = { val: 0 }
      gsap.to(obj, {
        val: card.raw, duration: 1.2, ease: 'power2.out',
        onUpdate() {
          el.innerText = formatNum(Math.round(obj.val))
        },
      })
    }
  })

  // 图表卡片入场
  const chartCards = [chartCard1Ref.value, chartCard2Ref.value, chartCard3Ref.value, chartCard4Ref.value].filter(Boolean)
  if (chartCards.length) {
    gsap.from(chartCards, {
      y: 20, opacity: 0, duration: 0.5, stagger: 0.1, ease: 'power3.out', delay: 0.3,
    })
  }
}

onMounted(loadData)
</script>

<style scoped>
.chart-row {
  margin-top: 16px;
}

/* ===== 统计卡片 ===== */
.stat-card {
  cursor: pointer;
  transition: transform var(--duration-normal) var(--ease-out), box-shadow var(--duration-normal) var(--ease-out);
}

.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-lg) !important;
}

.stat-card :deep(.el-card__body) {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
}

.stat-icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-body {
  flex: 1;
  min-width: 0;
}

.stat-label {
  font-size: 13px;
  color: var(--text-tertiary);
  margin-bottom: 4px;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: var(--text-primary);
  font-variant-numeric: tabular-nums;
}

/* ===== 图表卡片 ===== */
.chart-card {
  transition: transform var(--duration-normal) var(--ease-out), box-shadow var(--duration-normal) var(--ease-out);
}

.chart-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md) !important;
}

.chart-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
}
</style>
