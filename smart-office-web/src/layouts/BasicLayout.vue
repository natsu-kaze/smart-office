<template>
  <el-container class="app-shell">
    <el-aside class="sidebar" width="232px">
      <div class="brand">
        <div class="brand-mark">SO</div>
        <div>
          <strong>Smart Office</strong>
          <span>智能办公系统</span>
        </div>
      </div>
      <el-menu :default-active="route.path" router class="nav-menu">
        <el-menu-item v-for="item in visibleNavItems" :key="item.path" :index="item.path">
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="topbar">
        <div>
          <h1>{{ route.meta.title || '工作台' }}</h1>
          <p>微服务联调版</p>
        </div>
        <el-dropdown>
          <button class="user-button">
            <el-avatar :size="32" :src="authStore.user?.avatar">{{ authStore.displayName.slice(0, 1) }}</el-avatar>
            <span>{{ authStore.displayName }}</span>
          </button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="router.push('/profile')">个人中心</el-dropdown-item>
              <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>
      <el-main class="page-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Bell, Clock, DataBoard, Document, FolderOpened, Lock, OfficeBuilding, Tickets, User } from '@element-plus/icons-vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const navItems = [
  { path: '/dashboard', label: '工作台', icon: DataBoard, permission: 'dashboard:view' },
  { path: '/system/users', label: '用户管理', icon: User, permission: 'sys:user:list', roles: ['ADMIN'] },
  { path: '/system/roles', label: '角色权限', icon: Lock, permission: 'sys:role:list', roles: ['ADMIN'] },
  { path: '/org', label: '组织架构', icon: OfficeBuilding, permission: 'org:manage', roles: ['ADMIN', 'MANAGER'] },
  { path: '/approvals', label: '审批中心', icon: Tickets, permission: 'approval:list' },
  { path: '/messages', label: '消息中心', icon: Bell, permission: 'message:list' },
  { path: '/files', label: '文件中心', icon: FolderOpened, permission: 'file:list' },
  { path: '/policies', label: '制度文档', icon: Document, permission: 'policy:list' },
  { path: '/attendance', label: '考勤打卡', icon: Clock, permission: 'attendance:list' },
]

const visibleNavItems = computed(() => navItems.filter((item) => canAccess(item.permission, item.roles)))

function canAccess(permission: string, roles?: string[]) {
  if (authStore.hasRole('ADMIN')) return true
  if (roles?.some((role) => authStore.hasRole(role))) return true
  return authStore.hasPermission(permission)
}

async function handleLogout() {
  await authStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.app-shell {
  min-height: 100vh;
  background: #f5f7fb;
}

.sidebar {
  background: #111827;
  color: #fff;
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  height: 72px;
  padding: 0 20px;
}

.brand-mark {
  display: grid;
  width: 36px;
  height: 36px;
  place-items: center;
  border-radius: 8px;
  background: #2563eb;
  font-weight: 700;
}

.brand strong,
.brand span {
  display: block;
}

.brand span {
  margin-top: 3px;
  color: #9ca3af;
  font-size: 12px;
}

.nav-menu {
  border-right: 0;
  background: transparent;
}

.nav-menu :deep(.el-menu-item) {
  color: #cbd5e1;
}

.nav-menu :deep(.el-menu-item.is-active) {
  color: #fff;
  background: #1d4ed8;
}

.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 72px;
  border-bottom: 1px solid #e5e7eb;
  background: #fff;
}

.topbar h1 {
  margin: 0;
  font-size: 20px;
}

.topbar p {
  margin: 4px 0 0;
  color: #6b7280;
  font-size: 13px;
}

.user-button {
  display: flex;
  align-items: center;
  gap: 10px;
  border: 0;
  background: transparent;
  cursor: pointer;
}

.page-main {
  padding: 20px;
}
</style>
