<template>
  <div class="dashboard">
    <el-row :gutter="16" class="stat-row">
      <el-col v-for="item in stats" :key="item.label" :md="6" :sm="12" :xs="24">
        <div class="stat-card" :class="item.status">
          <span class="stat-label">{{ item.label }}</span>
          <strong class="stat-value">{{ item.value }}</strong>
          <small v-if="item.hint" class="stat-hint">{{ item.hint }}</small>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :md="12" :xs="24">
        <section class="quick-panel">
          <header>
            <h2>今日工作台</h2>
            <el-button type="primary" size="small" @click="refresh" :loading="loading">刷新</el-button>
          </header>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="当前用户">{{ authStore.displayName }}</el-descriptions-item>
            <el-descriptions-item label="考勤状态">
              <el-tag :type="attendanceTagType">{{ attendanceText }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="上班时间">{{ formatPunchTime(today.checkInTime) }}</el-descriptions-item>
            <el-descriptions-item label="下班时间">{{ formatPunchTime(today.checkOutTime) }}</el-descriptions-item>
          </el-descriptions>
        </section>
      </el-col>

      <el-col :md="12" :xs="24">
        <section class="quick-panel">
          <header>
            <h2>月度考勤概览</h2>
            <span class="month-label">{{ summaryMonth }}</span>
          </header>
          <div class="metric-grid">
            <div class="metric-item success">
              <strong>{{ monthlySummary?.normalDays ?? '-' }}</strong>
              <span>正常天数</span>
            </div>
            <div class="metric-item warning">
              <strong>{{ monthlySummary?.lateCount ?? '-' }}</strong>
              <span>迟到</span>
            </div>
            <div class="metric-item warning">
              <strong>{{ monthlySummary?.earlyLeaveCount ?? '-' }}</strong>
              <span>早退</span>
            </div>
            <div class="metric-item danger">
              <strong>{{ monthlySummary?.missingCount ?? '-' }}</strong>
              <span>缺卡</span>
            </div>
            <div class="metric-item info">
              <strong>{{ monthlySummary?.leaveDays ?? '-' }}</strong>
              <span>请假天数</span>
            </div>
          </div>
        </section>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { getTodayAttendance, getMonthlyAttendanceSummary } from '@/api/attendance'
import { getTodoCount, getUnreadCount } from '@/api/message'
import { useAuthStore } from '@/stores/auth'
import type { AttendanceSummary, AttendanceToday } from '@/types/api'

const authStore = useAuthStore()
const unreadCount = ref(0)
const todoCount = ref(0)
const loading = ref(false)
const today = ref<AttendanceToday>({})
const monthlySummary = ref<AttendanceSummary | null>(null)
const summaryMonth = currentMonth()

const statusText: Record<string, string> = {
  NORMAL: '正常',
  LATE: '迟到',
  EARLY_LEAVE: '早退',
  MISSING: '缺卡',
  LEAVE: '请假',
  OVERTIME: '加班',
  ABNORMAL: '异常',
}

const attendanceText = computed(() => {
  const inStatus = today.value.checkInStatus
  const outStatus = today.value.checkOutStatus
  if (inStatus === 'LEAVE' || outStatus === 'LEAVE') return '请假中'
  if (!today.value.checkInTime) return '未打卡'
  if (!today.value.checkOutTime) {
    return `已上班打卡（${formatStatus(today.value.checkInStatus)}）`
  }
  return `${formatStatus(inStatus)} / ${formatStatus(outStatus)}`
})

const attendanceTagType = computed(() => {
  const inStatus = today.value.checkInStatus
  const outStatus = today.value.checkOutStatus
  if (inStatus === 'LEAVE' || outStatus === 'LEAVE') return 'info'
  if (!today.value.checkInTime) return 'danger'
  if (!today.value.checkOutTime) return 'warning'
  if (inStatus === 'NORMAL' && outStatus === 'NORMAL') return 'success'
  return 'warning'
})

const stats = computed(() => {
  const checkInDone = !!today.value.checkInTime || today.value.checkInStatus === 'LEAVE'
  const checkOutDone = !!today.value.checkOutTime || today.value.checkOutStatus === 'LEAVE'
  const isLeave = today.value.checkInStatus === 'LEAVE' || today.value.checkOutStatus === 'LEAVE'
  return [
    { label: '待办', value: todoCount.value, status: todoCount.value > 0 ? 'warning' : '', hint: todoCount.value > 0 ? '有待处理审批' : '' },
    { label: '未读消息', value: unreadCount.value, status: unreadCount.value > 0 ? 'danger' : '', hint: unreadCount.value > 0 ? '有新通知' : '' },
    { label: '上班打卡', value: isLeave ? '请假' : checkInDone ? '已完成' : '未完成', status: isLeave ? 'info' : checkInDone ? '' : 'danger' },
    { label: '下班打卡', value: isLeave ? '请假' : checkOutDone ? '已完成' : '未完成', status: isLeave ? 'info' : checkOutDone ? '' : 'danger' },
  ]
})

async function refresh() {
  loading.value = true
  try {
    const [unread, todos, attendance, summary] = await Promise.all([
      getUnreadCount(),
      getTodoCount(),
      getTodayAttendance(),
      getMonthlyAttendanceSummary(summaryMonth).catch(() => null),
    ])
    unreadCount.value = unread
    todoCount.value = todos
    today.value = attendance
    monthlySummary.value = summary
  } finally {
    loading.value = false
  }
}

function formatStatus(status?: string) {
  if (!status) return '-'
  return statusText[status] || status
}

function formatPunchTime(value?: string) {
  if (!value) return '-'
  const match = value.match(/[ T](\d{2}:\d{2}:\d{2})/)
  return match ? match[1] : value
}

function currentMonth() {
  return new Date().toISOString().slice(0, 7)
}

onMounted(refresh)
</script>

<style scoped>
.dashboard {
  display: grid;
  gap: 18px;
}

.stat-row {
  margin: 0 !important;
}

.stat-card {
  display: grid;
  gap: 6px;
  padding: 18px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  margin-bottom: 0;
}

.stat-label {
  color: #6b7280;
  font-size: 14px;
}

.stat-value {
  font-size: 26px;
  font-weight: 700;
  color: #111827;
}

.stat-hint {
  color: #9ca3af;
  font-size: 12px;
}

.stat-card.warning .stat-value { color: #d97706; }
.stat-card.danger .stat-value { color: #dc2626; }
.stat-card.info .stat-value { color: #2563eb; }

.quick-panel {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  padding: 18px;
  height: 100%;
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

.month-label {
  color: #667085;
  font-size: 13px;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(100px, 1fr));
  gap: 12px;
}

.metric-item {
  display: grid;
  gap: 4px;
  padding: 12px;
  border: 1px solid #edf0f5;
  border-radius: 8px;
  background: #fafbfc;
  text-align: center;
}

.metric-item strong {
  font-size: 22px;
}

.metric-item span {
  color: #667085;
  font-size: 12px;
}

.metric-item.success strong { color: #16a34a; }
.metric-item.warning strong { color: #d97706; }
.metric-item.danger strong { color: #dc2626; }
.metric-item.info strong { color: #2563eb; }
</style>
