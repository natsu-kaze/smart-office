import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import BasicLayout from '@/layouts/BasicLayout.vue'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/login/LoginView.vue'),
    meta: { public: true },
  },
  {
    path: '/',
    component: BasicLayout,
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'dashboard',
        component: () => import('@/views/dashboard/DashboardView.vue'),
        meta: { title: '工作台' },
      },
      {
        path: 'system/users',
        name: 'users',
        component: () => import('@/views/system/UserManagementView.vue'),
        meta: { title: '用户管理' },
      },
      {
        path: 'org',
        name: 'org',
        component: () => import('@/views/org/OrganizationView.vue'),
        meta: { title: '组织架构' },
      },
      {
        path: 'approvals',
        name: 'approvals',
        component: () => import('@/views/approval/ApprovalCenterView.vue'),
        meta: { title: '审批中心' },
      },
      {
        path: 'attendance',
        name: 'attendance',
        component: () => import('@/views/attendance/AttendanceView.vue'),
        meta: { title: '考勤打卡' },
      },
    ],
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/dashboard',
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to) => {
  const authStore = useAuthStore()
  if (to.meta.public) {
    return true
  }
  if (!authStore.isLoggedIn) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  return true
})

export default router
