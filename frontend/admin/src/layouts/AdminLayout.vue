<template>
  <el-container class="admin-layout">
    <el-aside :width="appStore.sidebarCollapsed ? '64px' : '220px'" class="aside">
      <div class="logo">
        <span v-if="!appStore.sidebarCollapsed">小i跑腿管理端</span>
        <span v-else>小i</span>
      </div>
      <el-menu
        :default-active="currentRoute"
        :collapse="appStore.sidebarCollapsed"
        router
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409EFF"
      >
        <el-menu-item v-for="item in visibleMenu" :key="item.path" :index="item.path">
          <el-icon><component :is="item.icon" /></el-icon>
          <template #title>{{ item.title }}</template>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="header-left">
          <el-icon class="collapse-btn" @click="appStore.toggleSidebar">
            <Fold v-if="!appStore.sidebarCollapsed" />
            <Expand v-else />
          </el-icon>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item v-if="currentTitle">{{ currentTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <el-dropdown trigger="click">
            <span class="user-info">
              {{ authStore.adminInfo?.name || authStore.adminInfo?.username }}
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="handleLogout">
                  <el-icon><SwitchButton /></el-icon> 退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'

const route = useRoute()
const authStore = useAuthStore()
const appStore = useAppStore()

const currentRoute = computed(() => route.path)
const currentTitle = computed(() => route.meta?.title as string)

interface MenuItem {
  path: string
  title: string
  icon: string
  roles?: number[]
}

const menuItems: MenuItem[] = [
  { path: '/dashboard', title: '仪表盘', icon: 'Odometer' },
  { path: '/users', title: '用户管理', icon: 'User' },
  { path: '/runners', title: '跑腿员管理', icon: 'Van' },
  { path: '/tasks', title: '任务管理', icon: 'List' },
  { path: '/orders', title: '订单管理', icon: 'Document' },
  { path: '/transactions', title: '资金流水', icon: 'Money' },
  { path: '/notifications', title: '消息管理', icon: 'Message' },
  { path: '/audit', title: '审核管理', icon: 'CircleCheck' },
  { path: '/employees', title: '员工管理', icon: 'UserFilled', roles: [1] },
  { path: '/logs', title: '操作日志', icon: 'Tickets', roles: [1] }
]

async function handleLogout() {
  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', { type: 'warning' })
    authStore.logout()
  } catch { /* 用户取消 */ }
}

const visibleMenu = computed(() => menuItems.filter(item => {
  if (!item.roles) return true
  return authStore.adminInfo && item.roles.includes(authStore.adminInfo.role)
}))

</script>

<style scoped>
.admin-layout {
  height: 100vh;
}
.aside {
  background-color: #304156;
  transition: width 0.3s;
  overflow: hidden;
}
.logo {
  height: 60px;
  line-height: 60px;
  text-align: center;
  color: #fff;
  font-size: 18px;
  font-weight: bold;
  background-color: #2b3a4a;
  white-space: nowrap;
}
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #fff;
  border-bottom: 1px solid #e6e6e6;
  padding: 0 20px;
}
.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}
.collapse-btn {
  font-size: 20px;
  cursor: pointer;
}
.user-info {
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 4px;
}
</style>
