<template>
  <el-container class="admin-layout">
    <el-aside :width="appStore.sidebarCollapsed ? '64px' : '220px'" class="aside">
      <div class="logo">
        <img class="logo-icon" src="/logo.svg" alt="小i跑腿" />
        <span v-if="!appStore.sidebarCollapsed" class="logo-text">小i跑腿管理端</span>
        <span v-else class="logo-text">小i</span>
      </div>
      <el-menu
        :default-active="currentRoute"
        :collapse="appStore.sidebarCollapsed"
        router
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
              <el-avatar :size="32" :src="avatarUrl" class="user-avatar" />
              <span class="user-name">{{ authStore.adminInfo?.name || authStore.adminInfo?.username }}</span>
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
        <router-view v-slot="{ Component }">
          <Transition name="page-fade" mode="out-in">
            <component :is="Component" />
          </Transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'

const route = useRoute()
const authStore = useAuthStore()
const appStore = useAppStore()

const currentRoute = computed(() => route.path)
const currentTitle = computed(() => route.meta?.title as string)

const avatarUrl = '/api/imgs/default_avatar.jpg'

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
  { path: '/logs', title: '操作日志', icon: 'Tickets', roles: [1] },
]

async function handleLogout() {
  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', { type: 'warning' })
    authStore.logout()
  } catch { /* 用户取消 */ }
}

const visibleMenu = computed(() =>
  menuItems.filter((item) => {
    if (!item.roles) return true
    return authStore.adminInfo && item.roles.includes(authStore.adminInfo.role)
  })
)

onMounted(() => {
  authStore.fetchAdminInfo()
})
</script>

<style scoped>
.admin-layout {
  height: 100vh;
}

/* ===== 侧边栏 ===== */
.aside {
  background: linear-gradient(180deg, #1a1a2e 0%, #16213e 50%, #0f2b3d 100%);
  transition: width 0.3s var(--ease-out);
  overflow: hidden;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  background: var(--sidebar-logo-bg, #151d2b);
  white-space: nowrap;
  overflow: hidden;
}

.logo-icon {
  width: 28px;
  height: 28px;
  flex-shrink: 0;
}

.logo-text {
  color: #fff;
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 1px;
}

/* Override Element Plus menu styles in sidebar */
.aside :deep(.el-menu) {
  border-right: none;
  background: transparent;
}

.aside :deep(.el-menu-item) {
  color: #bfcbd9;
  margin: 4px 8px;
  border-radius: 8px;
  transition: all var(--duration-normal) var(--ease-out);
}

.aside :deep(.el-menu-item:hover) {
  background: rgba(255, 107, 74, 0.08) !important;
  color: #fff;
}

.aside :deep(.el-menu-item.is-active) {
  color: #fff !important;
  background: rgba(255, 107, 74, 0.15) !important;
  border-left: 3px solid #FF6B4A;
  padding-left: 17px !important;
}

/* ===== 顶栏 ===== */
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-bottom: 1px solid var(--neutral-outline-light);
  padding: 0 24px;
  height: 56px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.collapse-btn {
  font-size: 20px;
  cursor: pointer;
  color: var(--text-secondary);
  transition: color var(--duration-fast);
}

.collapse-btn:hover {
  color: var(--brand-primary);
}

.user-info {
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 8px;
  border-radius: 8px;
  transition: background var(--duration-fast);
}

.user-info:hover {
  background: var(--neutral-surface);
}

.user-avatar {
  flex-shrink: 0;
}

.user-name {
  font-size: 14px;
  color: var(--text-primary);
}

/* ===== 页面过渡 ===== */
.page-fade-enter-active,
.page-fade-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.page-fade-enter-from {
  opacity: 0;
  transform: translateY(8px);
}

.page-fade-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}
</style>
