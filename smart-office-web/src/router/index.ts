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
        meta: { title: '工作台', permission: 'dashboard:view' },
      },
      {
        path: 'system/users',
        name: 'users',
        component: () => import('@/views/system/UserManagementView.vue'),
        meta: { title: '用户管理', permission: 'sys:user:list', roles: ['ADMIN'] },
      },
      {
        path: 'system/roles',
        name: 'roles',
        component: () => import('@/views/system/RolePermissionView.vue'),
        meta: { title: '角色权限', permission: 'sys:role:list', roles: ['ADMIN'] },
      },
      {
        path: 'org',
        name: 'org',
        component: () => import('@/views/org/OrganizationView.vue'),
        meta: { title: '组织架构', permission: 'org:manage', roles: ['ADMIN', 'MANAGER'] },
      },
      {
        path: 'approvals',
        name: 'approvals',
        component: () => import('@/views/approval/ApprovalCenterView.vue'),
        meta: { title: '审批中心', permission: 'approval:list' },
      },
      {
        path: 'messages',
        name: 'messages',
        component: () => import('@/views/message/MessageCenterView.vue'),
        meta: { title: '消息中心', permission: 'message:list' },
      },
      {
        path: 'files',
        name: 'files',
        component: () => import('@/views/file/FileCenterView.vue'),
        meta: { title: '文件中心', permission: 'file:list' },
      },
      {
        path: 'policies',
        name: 'policies',
        component: () => import('@/views/policy/PolicyDocumentView.vue'),
        meta: { title: '制度文档', permission: 'policy:list' },
      },
      {
        path: 'attendance',
        name: 'attendance',
        component: () => import('@/views/attendance/AttendanceView.vue'),
        meta: { title: '考勤打卡', permission: 'attendance:list' },
      },
      {
        path: 'profile',
        name: 'profile',
        component: () => import('@/views/profile/ProfileCenterView.vue'),
        meta: { title: '个人中心' },
      },
      {
        path: '403',
        name: 'forbidden',
        component: () => import('@/views/error/ForbiddenView.vue'),
        meta: { title: '无权限访问' },
      },
      {
        path: '404',
        name: 'notFound',
        component: () => import('@/views/error/NotFoundView.vue'),
        meta: { title: '页面不存在' },
      },
    ],
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/404',
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
  if (authStore.hasRole('ADMIN')) {
    return true
  }
  const roles = to.meta.roles as string[] | undefined
  if (roles?.some((role) => authStore.hasRole(role))) {
    return true
  }
  const permission = to.meta.permission as string | undefined
  if (permission && !authStore.hasPermission(permission)) {
    return { path: '/403' }
  }
  return true
})

export default router
