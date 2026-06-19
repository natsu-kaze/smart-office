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
        <el-menu-item index="/dashboard">
          <el-icon><DataBoard /></el-icon>
          <span>工作台</span>
        </el-menu-item>
        <el-menu-item index="/system/users">
          <el-icon><User /></el-icon>
          <span>用户管理</span>
        </el-menu-item>
        <el-menu-item index="/org">
          <el-icon><OfficeBuilding /></el-icon>
          <span>组织架构</span>
        </el-menu-item>
        <el-menu-item index="/approvals">
          <el-icon><Tickets /></el-icon>
          <span>审批中心</span>
        </el-menu-item>
        <el-menu-item index="/messages">
          <el-icon><Bell /></el-icon>
          <span>消息中心</span>
        </el-menu-item>
        <el-menu-item index="/files">
          <el-icon><FolderOpened /></el-icon>
          <span>文件中心</span>
        </el-menu-item>
        <el-menu-item index="/policies">
          <el-icon><Document /></el-icon>
          <span>制度文档</span>
        </el-menu-item>
        <el-menu-item index="/attendance">
          <el-icon><Clock /></el-icon>
          <span>考勤打卡</span>
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
            <el-avatar :size="32">{{ authStore.displayName.slice(0, 1) }}</el-avatar>
            <span>{{ authStore.displayName }}</span>
          </button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="handleLogout">退出登录</el-dropdown-item>
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
import { Bell, Clock, DataBoard, Document, FolderOpened, OfficeBuilding, Tickets, User } from '@element-plus/icons-vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

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
