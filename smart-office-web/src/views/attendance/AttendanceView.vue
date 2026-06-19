<template>
  <section class="attendance-page">
    <div class="page-card attendance">
      <header>
        <h2>今日考勤</h2>
        <el-button @click="load">刷新</el-button>
      </header>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="上班时间">{{ today.checkInTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="上班状态">{{ statusText(today.checkInStatus) }}</el-descriptions-item>
        <el-descriptions-item label="下班时间">{{ today.checkOutTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="下班状态">{{ statusText(today.checkOutStatus) }}</el-descriptions-item>
      </el-descriptions>
      <div class="actions">
        <el-button type="primary" @click="handleCheckIn">上班打卡</el-button>
        <el-button type="success" @click="handleCheckOut">下班打卡</el-button>
      </div>
    </div>

    <div class="summary-grid">
      <div class="page-card summary-card">
        <header>
          <h2>月度统计</h2>
          <el-date-picker v-model="summaryMonth" type="month" value-format="YYYY-MM" @change="loadSummary" />
        </header>
        <el-row :gutter="12">
          <el-col :span="8"><strong>{{ summary?.normalDays ?? 0 }}</strong><span>正常天数</span></el-col>
          <el-col :span="8"><strong>{{ summary?.lateCount ?? 0 }}</strong><span>迟到</span></el-col>
          <el-col :span="8"><strong>{{ summary?.earlyLeaveCount ?? 0 }}</strong><span>早退</span></el-col>
          <el-col :span="8"><strong>{{ summary?.missingCount ?? 0 }}</strong><span>缺卡</span></el-col>
          <el-col :span="8"><strong>{{ summary?.leaveDays ?? 0 }}</strong><span>请假天数</span></el-col>
          <el-col :span="8"><strong>{{ summary?.overtimeHours ?? 0 }}</strong><span>加班小时</span></el-col>
        </el-row>
      </div>
    </div>

    <div class="page-card">
      <header class="table-header">
        <div>
          <h2>考勤记录</h2>
          <p>查看个人和部门考勤明细</p>
        </div>
        <el-segmented v-model="recordMode" :options="recordModes" @change="loadRecords" />
      </header>

      <el-form class="filters" :model="recordQuery" inline>
        <el-form-item label="开始日期">
          <el-date-picker v-model="recordQuery.startDate" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="结束日期">
          <el-date-picker v-model="recordQuery.endDate" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadRecords">查询</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="recordLoading" :data="records" border>
        <el-table-column prop="attendanceDate" label="日期" width="130" />
        <el-table-column v-if="recordMode === 'department'" prop="realName" label="员工" width="120" />
        <el-table-column v-if="recordMode === 'department'" prop="departmentName" label="部门" width="150" />
        <el-table-column prop="checkInTime" label="上班时间" min-width="170" />
        <el-table-column prop="checkInStatus" label="上班状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.checkInStatus)">{{ statusText(row.checkInStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="checkOutTime" label="下班时间" min-width="170" />
        <el-table-column prop="checkOutStatus" label="下班状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.checkOutStatus)">{{ statusText(row.checkOutStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="220" show-overflow-tooltip />
      </el-table>

      <div class="pager">
        <el-pagination
          v-model:current-page="recordQuery.current"
          v-model:page-size="recordQuery.size"
          layout="total, prev, pager, next"
          :total="recordTotal"
          @current-change="loadRecords"
          @size-change="loadRecords"
        />
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  checkIn,
  checkOut,
  getDepartmentAttendanceRecords,
  getMonthlyAttendanceSummary,
  getMyAttendanceRecords,
  getTodayAttendance,
} from '@/api/attendance'
import type { AttendanceRecord, AttendanceSummary, AttendanceToday } from '@/types/api'

const today = ref<AttendanceToday>({})
const summary = ref<AttendanceSummary | null>(null)
const records = ref<AttendanceRecord[]>([])
const recordLoading = ref(false)
const recordTotal = ref(0)
const recordMode = ref('mine')
const recordModes = [
  { label: '我的记录', value: 'mine' },
  { label: '部门记录', value: 'department' },
]

const summaryMonth = ref(currentMonth())
const recordQuery = reactive({
  current: 1,
  size: 10,
  startDate: firstDayOfMonth(),
  endDate: todayString(),
})

const recordApi = computed(() => recordMode.value === 'department'
  ? getDepartmentAttendanceRecords
  : getMyAttendanceRecords)

async function load() {
  const [todayResult] = await Promise.all([
    getTodayAttendance(),
    loadSummary(),
    loadRecords(),
  ])
  today.value = todayResult
}

async function loadSummary() {
  summary.value = await getMonthlyAttendanceSummary(summaryMonth.value)
}

async function loadRecords() {
  recordLoading.value = true
  try {
    const page = await recordApi.value({ ...recordQuery })
    records.value = page.records
    recordTotal.value = page.total
  } finally {
    recordLoading.value = false
  }
}

async function handleCheckIn() {
  today.value = await checkIn()
  ElMessage.success('上班打卡成功')
}

async function handleCheckOut() {
  today.value = await checkOut()
  ElMessage.success('下班打卡成功')
}

function statusText(status?: string) {
  const map: Record<string, string> = {
    NORMAL: '正常',
    LATE: '迟到',
    EARLY_LEAVE: '早退',
    MISSING: '缺卡',
    LEAVE: '请假',
    OVERTIME: '加班',
    ABNORMAL: '异常',
  }
  return status ? map[status] || status : '-'
}

function statusTag(status?: string) {
  const map: Record<string, 'success' | 'warning' | 'danger' | 'info' | 'primary'> = {
    NORMAL: 'success',
    LATE: 'warning',
    EARLY_LEAVE: 'warning',
    MISSING: 'danger',
    LEAVE: 'info',
    OVERTIME: 'primary',
    ABNORMAL: 'danger',
  }
  return status ? map[status] || 'info' : 'info'
}

function todayString() {
  return new Date().toISOString().slice(0, 10)
}

function currentMonth() {
  return todayString().slice(0, 7)
}

function firstDayOfMonth() {
  return `${currentMonth()}-01`
}

onMounted(load)
</script>

<style scoped>
.attendance-page {
  display: grid;
  gap: 16px;
}

.attendance header,
.actions {
  display: flex;
  align-items: center;
  gap: 12px;
  justify-content: space-between;
}

.attendance h2 {
  margin: 0 0 16px;
  font-size: 18px;
}

.actions {
  justify-content: flex-start;
  margin-top: 18px;
}

.summary-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
}

.summary-card :deep(.el-col) {
  display: grid;
  gap: 4px;
  margin-top: 12px;
}

.summary-card strong {
  font-size: 22px;
}

.summary-card span {
  color: #6b7280;
  font-size: 13px;
}

.table-header p {
  margin: 4px 0 0;
  color: #6b7280;
  font-size: 13px;
}

.filters {
  margin-bottom: 12px;
}
</style>
