<template>
  <div class="dashboard">
    <el-row :gutter="16">
      <el-col :md="6" :sm="12" :xs="24" v-for="item in stats" :key="item.label">
        <div class="stat-card">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
        </div>
      </el-col>
    </el-row>
    <section class="quick-panel">
      <header>
        <h2>今日工作台</h2>
        <el-button type="primary" @click="refresh">刷新</el-button>
      </header>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="当前用户">{{ authStore.displayName }}</el-descriptions-item>
        <el-descriptions-item label="待办数量">{{ todoCount }}</el-descriptions-item>
        <el-descriptions-item label="未读消息">{{ unreadCount }}</el-descriptions-item>
        <el-descriptions-item label="考勤状态">{{ attendanceText }}</el-descriptions-item>
      </el-descriptions>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { getTodayAttendance } from '@/api/attendance'
import { getTodoCount, getUnreadCount } from '@/api/message'
import { useAuthStore } from '@/stores/auth'
import type { AttendanceToday } from '@/types/api'

const authStore = useAuthStore()
const unreadCount = ref(0)
const todoCount = ref(0)
const today = ref<AttendanceToday>({})

const attendanceText = computed(() => {
  if (!today.value.checkInTime) return '未打卡'
  if (!today.value.checkOutTime) return `已上班打卡（${today.value.checkInStatus || 'NORMAL'}）`
  return `${today.value.checkInStatus || '-'} / ${today.value.checkOutStatus || '-'}`
})

const stats = computed(() => [
  { label: '待办', value: todoCount.value },
  { label: '未读消息', value: unreadCount.value },
  { label: '上班打卡', value: today.value.checkInTime ? '已完成' : '未完成' },
  { label: '下班打卡', value: today.value.checkOutTime ? '已完成' : '未完成' },
])

async function refresh() {
  const [unread, todos, attendance] = await Promise.all([
    getUnreadCount(),
    getTodoCount(),
    getTodayAttendance(),
  ])
  unreadCount.value = unread
  todoCount.value = todos
  today.value = attendance
}

onMounted(refresh)
</script>

<style scoped>
.dashboard {
  display: grid;
  gap: 18px;
}

.stat-card,
.quick-panel {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
}

.stat-card {
  display: grid;
  gap: 8px;
  margin-bottom: 16px;
  padding: 18px;
}

.stat-card span {
  color: #6b7280;
}

.stat-card strong {
  font-size: 26px;
}

.quick-panel {
  padding: 18px;
}

.quick-panel header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.quick-panel h2 {
  margin: 0;
  font-size: 18px;
}
</style>
