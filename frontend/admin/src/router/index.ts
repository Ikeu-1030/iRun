import { createRouter, createWebHistory } from 'vue-router'
import { getAdminToken } from '@/utils/request'

const router = createRouter({
  history: createWebHistory(),
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
    next()
  }
})

export default router
