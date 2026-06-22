import { createRouter, createWebHistory } from 'vue-router'
import { getAdminToken } from '@/utils/tokenStore'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory('/api/'),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/login/LoginView.vue'),
      meta: { title: '登录' }
    },
    {
      path: '/',
      component: () => import('@/layouts/AdminLayout.vue'),
      redirect: '/dashboard',
      children: [
        {
          path: 'dashboard',
          name: 'Dashboard',
          component: () => import('@/views/dashboard/DashboardView.vue'),
          meta: { title: '仪表盘', icon: 'Odometer' }
        },
        {
          path: 'users',
          name: 'Users',
          component: () => import('@/views/users/UserListView.vue'),
          meta: { title: '用户管理', icon: 'User' }
        },
        {
          path: 'users/:id',
          name: 'UserDetail',
          component: () => import('@/views/users/UserDetailView.vue'),
          meta: { title: '用户详情', hidden: true }
        },
        {
          path: 'audit',
          name: 'Audit',
          component: () => import('@/views/audit/AuditView.vue'),
          meta: { title: '审核管理', icon: 'Checked' }
        },
        {
          path: 'runners',
          name: 'Runners',
          component: () => import('@/views/runners/RunnerListView.vue'),
          meta: { title: '跑腿员管理', icon: 'Van' }
        },
        {
          path: 'runners/:id',
          name: 'RunnerDetail',
          component: () => import('@/views/runners/RunnerDetailView.vue'),
          meta: { title: '跑腿员详情', hidden: true }
        },
        {
          path: 'tasks',
          name: 'Tasks',
          component: () => import('@/views/tasks/TaskListView.vue'),
          meta: { title: '任务管理', icon: 'List' }
        },
        {
          path: 'tasks/:id',
          name: 'TaskDetail',
          component: () => import('@/views/tasks/TaskDetailView.vue'),
          meta: { title: '任务详情', hidden: true }
        },
        {
          path: 'orders',
          name: 'Orders',
          component: () => import('@/views/orders/OrderListView.vue'),
          meta: { title: '订单管理', icon: 'Document' }
        },
        {
          path: 'orders/:id',
          name: 'OrderDetail',
          component: () => import('@/views/orders/OrderDetailView.vue'),
          meta: { title: '订单详情', hidden: true }
        },
        {
          path: 'transactions',
          name: 'Transactions',
          component: () => import('@/views/transactions/TransactionListView.vue'),
          meta: { title: '资金流水', icon: 'Money' }
        },
        {
          path: 'notifications',
          name: 'Notifications',
          component: () => import('@/views/notifications/NotificationView.vue'),
          meta: { title: '消息管理', icon: 'Message' }
        },
        {
          path: 'employees',
          name: 'Employees',
          component: () => import('@/views/employees/EmployeeListView.vue'),
          meta: { title: '员工管理', icon: 'UserFilled', role: [1] }
        },
        {
          path: 'settings',
          name: 'Settings',
          component: () => import('@/views/settings/SettingsView.vue'),
          meta: { title: '系统设置', icon: 'Setting', role: [1] }
        },
        {
          path: 'logs',
          name: 'OperationLogs',
          component: () => import('@/views/logs/OperationLogView.vue'),
          meta: { title: '操作日志', icon: 'Tickets', role: [1] }
        },
        {
          path: '/:pathMatch(.*)*',
          name: 'NotFound',
          component: () => import('@/views/error/NotFoundView.vue'),
          meta: { title: '404', hidden: true }
        }
      ]
    }
  ]
})

router.beforeEach((to, _from, next) => {
  document.title = (to.meta.title as string) ? `${to.meta.title} - 小i跑腿管理端` : '小i跑腿管理端'
  if (to.path !== '/login' && !getAdminToken()) {
    next('/login')
  } else if (to.path === '/login' && getAdminToken()) {
    next('/dashboard')
  } else {
    // 路由级角色校验：meta.role 声明的路由仅限对应角色访问
    const requiredRoles = to.meta.role as number[] | undefined
    if (requiredRoles && requiredRoles.length > 0) {
      const authStore = useAuthStore()
      const userRole = authStore.adminInfo?.role
      if (userRole === undefined || !requiredRoles.includes(userRole)) {
        next('/dashboard')
        return
      }
    }
    next()
  }
})

export default router
