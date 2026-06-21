<template>
  <section class="attendance-page">
    <div class="page-card today-card">
      <header class="section-header">
        <div>
          <h2>今日考勤</h2>
          <p>先完成打卡，再看个人和部门出勤情况。</p>
        </div>
        <el-button @click="load">刷新</el-button>
      </header>

      <el-descriptions :column="2" border>
        <el-descriptions-item label="上班时间">{{ formatTime(today.checkInTime) }}</el-descriptions-item>
        <el-descriptions-item label="上班状态">
          <el-tag :type="statusTag(today.checkInStatus)">{{ statusText(today.checkInStatus) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="下班时间">{{ formatTime(today.checkOutTime) }}</el-descriptions-item>
        <el-descriptions-item label="下班状态">
          <el-tag :type="statusTag(today.checkOutStatus)">{{ statusText(today.checkOutStatus) }}</el-tag>
        </el-descriptions-item>
      </el-descriptions>

      <div class="actions">
        <el-tooltip content="请先完成上班打卡" :disabled="!!today.checkInTime || todayIsLeave">
          <el-button type="primary" :disabled="!today.checkInTime || todayIsLeave || !!today.checkOutTime" @click="handleCheckIn">
            上班打卡
          </el-button>
        </el-tooltip>
        <el-tooltip :content="!today.checkInTime ? '请先完成上班打卡' : '已下班打卡'" :disabled="!!today.checkInTime && !today.checkOutTime && !todayIsLeave">
          <el-button type="success" :disabled="!today.checkInTime || todayIsLeave || !!today.checkOutTime" @click="handleCheckOut">
            下班打卡
          </el-button>
        </el-tooltip>
        <el-text v-if="todayIsLeave" type="info">今天已审批为请假，无需打卡。</el-text>
        <el-text v-else-if="today.checkInTime && today.checkOutTime" type="success">今日打卡已完成</el-text>
        <el-text v-else-if="today.checkInTime && !today.checkOutTime" type="warning">上班已打卡，下班别忘了</el-text>
      </div>
    </div>

    <div class="summary-grid">
      <div class="page-card summary-card">
        <header class="section-header compact">
          <h2>我的月度概览</h2>
          <el-date-picker v-model="summaryMonth" type="month" value-format="YYYY-MM" @change="loadSummary" />
        </header>
        <div class="metric-grid">
          <div class="metric-item success">
            <strong>{{ summary?.normalDays ?? 0 }}</strong>
            <span>正常天数</span>
          </div>
          <div class="metric-item warning">
            <strong>{{ summary?.lateCount ?? 0 }}</strong>
            <span>迟到</span>
          </div>
          <div class="metric-item warning">
            <strong>{{ summary?.earlyLeaveCount ?? 0 }}</strong>
            <span>早退</span>
          </div>
          <div class="metric-item danger">
            <strong>{{ summary?.missingCount ?? 0 }}</strong>
            <span>缺卡</span>
          </div>
          <div class="metric-item info">
            <strong>{{ summary?.leaveDays ?? 0 }}</strong>
            <span>请假天数</span>
          </div>
        </div>
      </div>

      <div class="page-card chart-card">
        <header class="section-header compact">
          <h2>状态分布</h2>
          <span>{{ recordMode === 'department' ? '部门筛选范围' : '我的记录' }}</span>
        </header>
        <div class="status-bars">
          <div v-for="item in statusDistribution" :key="item.key" class="status-row">
            <span>{{ item.label }}</span>
            <div class="bar-track">
              <i :class="item.key" :style="{ width: item.percent + '%' }" />
            </div>
            <strong>{{ item.count }}</strong>
          </div>
        </div>
      </div>
    </div>

    <div v-if="recordMode === 'department'" class="page-card department-overview">
      <header class="section-header">
        <div>
          <h2>部门出勤看板</h2>
          <p>{{ snapshotDate }} 当天，应到、实到、未到人员一眼看清。</p>
        </div>
      </header>
      <div class="attendance-dashboard">
        <div class="ring" :style="{ '--rate': attendanceRate + '%' }">
          <strong>{{ attendanceRate }}%</strong>
          <span>出勤率</span>
        </div>
        <div class="dashboard-metrics">
          <div>
            <strong>{{ departmentSnapshot.expected }}</strong>
            <span>应到</span>
          </div>
          <div>
            <strong>{{ departmentSnapshot.present }}</strong>
            <span>实到</span>
          </div>
          <div>
            <strong>{{ departmentSnapshot.leave }}</strong>
            <span>请假</span>
          </div>
          <div>
            <strong>{{ departmentSnapshot.absent }}</strong>
            <span>未到</span>
          </div>
          <div>
            <strong>{{ departmentSnapshot.abnormal }}</strong>
            <span>异常</span>
          </div>
        </div>
        <div class="absent-list">
          <h3>未到人员</h3>
          <el-empty v-if="!absentEmployees.length" description="暂无未到人员" :image-size="70" />
          <el-tag v-for="name in absentEmployees" v-else :key="name" type="danger" effect="plain">
            {{ name }}
          </el-tag>
        </div>
      </div>
    </div>

    <div class="page-card">
      <header class="section-header">
        <div>
          <h2>考勤记录</h2>
          <p>个人只能查看自己的记录；管理员和主管可查看部门记录。</p>
        </div>
        <el-segmented v-model="recordMode" :options="recordModes" @change="handleModeChange" />
      </header>

      <el-alert
        v-if="!canViewDepartment"
        class="permission-tip"
        type="warning"
        show-icon
        :closable="false"
        title="当前账号只能查看个人考勤记录，部门记录需要管理员或主管权限。"
      />

      <el-form class="filters" :model="recordQuery" inline>
        <el-form-item v-if="recordMode === 'department'" label="部门">
          <el-select v-model="recordQuery.departmentId" filterable placeholder="请选择部门" @change="handleDepartmentChange">
            <el-option v-for="item in departmentOptions" :key="item.id" :label="item.label" :value="item.id" />
          </el-select>
        </el-form-item>
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
        <el-table-column v-if="recordMode === 'department'" prop="departmentName" label="部门" width="160" />
        <el-table-column prop="checkInTime" label="上班时间" min-width="170">
          <template #default="{ row }">{{ formatDateTime(row.checkInTime) }}</template>
        </el-table-column>
        <el-table-column prop="checkInStatus" label="上班状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.checkInStatus)">{{ statusText(row.checkInStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="checkOutTime" label="下班时间" min-width="170">
          <template #default="{ row }">{{ formatDateTime(row.checkOutTime) }}</template>
        </el-table-column>
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
          layout="total, sizes, prev, pager, next"
          :page-sizes="[10, 20, 50]"
          :total="recordTotal"
          @current-change="loadRecords"
          @size-change="loadRecords"
        />
      </div>
    </div>
    <el-dialog v-model="punchDialog.visible" :title="punchDialog.type === 'in' ? '上班打卡' : '下班打卡'" width="420px" :close-on-click-modal="false" center>
      <div class="punch-dialog-body">
        <div class="punch-icon" :class="punchDialog.type">
          <span>{{ punchDialog.type === 'in' ? '☀️' : '🌙' }}</span>
        </div>
        <div class="punch-time">{{ punchDialog.currentTime }}</div>
        <div class="punch-label">{{ punchDialog.type === 'in' ? '上班打卡' : '下班打卡' }}</div>
        <el-input
          v-model="punchDialog.remark"
          type="textarea"
          :rows="2"
          placeholder="备注（选填），如：外出见客户、远程办公"
          maxlength="200"
          show-word-limit
        />
      </div>
      <template #footer>
        <el-button @click="punchDialog.visible = false">取消</el-button>
        <el-button :type="punchDialog.type === 'in' ? 'primary' : 'success'" :loading="punchDialog.loading" @click="confirmPunch">
          确认打卡
        </el-button>
      </template>
    </el-dialog>
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
import { getDepartmentTree, getEmployees } from '@/api/org'
import { useAuthStore } from '@/stores/auth'
import type {
  ApiId,
  AttendanceRecord,
  AttendanceSummary,
  AttendanceToday,
  DepartmentNode,
  EmployeeItem,
} from '@/types/api'

interface DepartmentOption {
  id: ApiId
  label: string
}

interface StatusItem {
  key: string
  label: string
  count: number
  percent: number
}

const authStore = useAuthStore()
const today = ref<AttendanceToday>({})
const summary = ref<AttendanceSummary | null>(null)
const records = ref<AttendanceRecord[]>([])
const chartRecords = ref<AttendanceRecord[]>([])
const snapshotRecords = ref<AttendanceRecord[]>([])
const departmentEmployees = ref<EmployeeItem[]>([])
const departments = ref<DepartmentNode[]>([])
const recordLoading = ref(false)
const recordTotal = ref(0)
const recordMode = ref<'mine' | 'department'>('mine')
const summaryMonth = ref(currentMonth())

const punchDialog = reactive({
  visible: false,
  type: 'in' as 'in' | 'out',
  currentTime: '',
  remark: '',
  loading: false,
  timer: null as ReturnType<typeof setInterval> | null,
})

const recordQuery = reactive({
  current: 1,
  size: 10,
  startDate: firstDayOfMonth(),
  endDate: todayString(),
  departmentId: undefined as ApiId | undefined,
})

const canViewDepartment = computed(() => authStore.hasPermission('attendance:department:list'))
const recordModes = computed(() => [
  { label: '我的记录', value: 'mine' },
  ...(canViewDepartment.value ? [{ label: '部门记录', value: 'department' }] : []),
])
const departmentOptions = computed(() => flattenDepartments(departments.value))
const snapshotDate = computed(() => recordQuery.endDate || todayString())
const todayIsLeave = computed(() => [today.value.checkInStatus, today.value.checkOutStatus].includes('LEAVE'))

const statusDistribution = computed<StatusItem[]>(() => {
  const base = recordMode.value === 'department' ? chartRecords.value : records.value
  const counts = {
    normal: 0,
    late: 0,
    early: 0,
    missing: 0,
    leave: 0,
    abnormal: 0,
  }
  base.forEach((record) => {
    const statuses = [record.checkInStatus, record.checkOutStatus]
    if (statuses.includes('LEAVE')) {
      counts.leave += 1
      return
    }
    if (statuses.includes('MISSING')) counts.missing += 1
    if (statuses.includes('LATE')) counts.late += 1
    if (statuses.includes('EARLY_LEAVE')) counts.early += 1
    if (statuses.includes('ABNORMAL')) counts.abnormal += 1
    if (record.checkInStatus === 'NORMAL' && record.checkOutStatus === 'NORMAL') counts.normal += 1
  })
  const total = Math.max(1, Object.values(counts).reduce((sum, count) => sum + count, 0))
  return [
    { key: 'normal', label: '正常', count: counts.normal, percent: Math.round((counts.normal / total) * 100) },
    { key: 'late', label: '迟到', count: counts.late, percent: Math.round((counts.late / total) * 100) },
    { key: 'early', label: '早退', count: counts.early, percent: Math.round((counts.early / total) * 100) },
    { key: 'missing', label: '缺卡', count: counts.missing, percent: Math.round((counts.missing / total) * 100) },
    { key: 'leave', label: '请假', count: counts.leave, percent: Math.round((counts.leave / total) * 100) },
    { key: 'abnormal', label: '异常', count: counts.abnormal, percent: Math.round((counts.abnormal / total) * 100) },
  ]
})

const departmentSnapshot = computed(() => {
  const expected = departmentEmployees.value.length
  const presentUserIds = new Set(
    snapshotRecords.value
      .filter((record) => record.checkInStatus && record.checkInStatus !== 'MISSING' && record.checkInStatus !== 'LEAVE')
      .map((record) => String(record.userId)),
  )
  const leaveUserIds = new Set(
    snapshotRecords.value
      .filter((record) => record.checkInStatus === 'LEAVE' || record.checkOutStatus === 'LEAVE')
      .map((record) => String(record.userId)),
  )
  const abnormal = snapshotRecords.value.filter((record) => isAbnormal(record)).length
  return {
    expected,
    present: presentUserIds.size,
    leave: leaveUserIds.size,
    absent: Math.max(expected - presentUserIds.size - leaveUserIds.size, 0),
    abnormal,
  }
})

const attendanceRate = computed(() => {
  if (!departmentSnapshot.value.expected) return 0
  return Math.round((departmentSnapshot.value.present / departmentSnapshot.value.expected) * 100)
})

const absentEmployees = computed(() => {
  const presentUserIds = new Set(
    snapshotRecords.value
      .filter((record) => record.checkInStatus && record.checkInStatus !== 'MISSING' && record.checkInStatus !== 'LEAVE')
      .map((record) => String(record.userId)),
  )
  return departmentEmployees.value
    .filter((employee) => !presentUserIds.has(String(employee.userId)))
    .map((employee) => employee.realName || employee.employeeNo)
})

async function load() {
  const [todayResult] = await Promise.all([getTodayAttendance(), loadSummary(), loadRecords()])
  today.value = todayResult
}

async function loadSummary() {
  summary.value = await getMonthlyAttendanceSummary(summaryMonth.value)
}

async function loadRecords() {
  if (recordMode.value === 'department' && !canViewDepartment.value) {
    recordMode.value = 'mine'
    ElMessage.warning('当前账号没有部门考勤权限')
  }
  if (recordMode.value === 'department' && !recordQuery.departmentId) {
    await ensureDepartments()
    recordQuery.departmentId = departmentOptions.value[0]?.id
  }
  recordLoading.value = true
  try {
    const params = buildRecordParams()
    const page = recordMode.value === 'department'
      ? await getDepartmentAttendanceRecords(params)
      : await getMyAttendanceRecords(params)
    records.value = page.records
    recordTotal.value = page.total
    if (recordMode.value === 'department') {
      await loadDepartmentAnalytics()
    } else {
      chartRecords.value = page.records
      snapshotRecords.value = []
      departmentEmployees.value = []
    }
  } finally {
    recordLoading.value = false
  }
}

async function loadDepartmentAnalytics() {
  if (!recordQuery.departmentId) return
  const [chartPage, dayPage, employeePage] = await Promise.all([
    getDepartmentAttendanceRecords({ ...buildRecordParams(), current: 1, size: 500 }),
    getDepartmentAttendanceRecords({
      departmentId: recordQuery.departmentId,
      startDate: snapshotDate.value,
      endDate: snapshotDate.value,
      current: 1,
      size: 500,
    }),
    getEmployees({ departmentId: recordQuery.departmentId, employmentStatus: 'ACTIVE', current: 1, size: 500 }),
  ])
  chartRecords.value = chartPage.records
  snapshotRecords.value = dayPage.records
  departmentEmployees.value = employeePage.records
}

function openPunchDialog(type: 'in' | 'out') {
  punchDialog.type = type
  punchDialog.remark = ''
  punchDialog.loading = false
  punchDialog.currentTime = nowTimeString()
  punchDialog.visible = true
  if (punchDialog.timer) clearInterval(punchDialog.timer)
  punchDialog.timer = setInterval(() => {
    punchDialog.currentTime = nowTimeString()
  }, 1000)
}

function closePunchDialog() {
  punchDialog.visible = false
  if (punchDialog.timer) {
    clearInterval(punchDialog.timer)
    punchDialog.timer = null
  }
}

async function confirmPunch() {
  punchDialog.loading = true
  try {
    const remark = punchDialog.remark.trim() || undefined
    if (punchDialog.type === 'in') {
      today.value = await checkIn(remark)
      ElMessage.success('上班打卡成功')
    } else {
      today.value = await checkOut(remark)
      ElMessage.success('下班打卡成功')
    }
    closePunchDialog()
    await loadRecords()
  } catch {
    // error handled by interceptor
  } finally {
    punchDialog.loading = false
  }
}

function handleCheckIn() {
  openPunchDialog('in')
}

function handleCheckOut() {
  openPunchDialog('out')
}

async function handleModeChange() {
  recordQuery.current = 1
  if (recordMode.value === 'department') {
    await ensureDepartments()
  }
  await loadRecords()
}

async function handleDepartmentChange() {
  recordQuery.current = 1
  await loadRecords()
}

async function ensureDepartments() {
  if (!departments.value.length) {
    departments.value = await getDepartmentTree()
  }
}

function buildRecordParams() {
  return {
    ...recordQuery,
    departmentId: recordMode.value === 'department' ? recordQuery.departmentId : undefined,
  }
}

function isAbnormal(record: AttendanceRecord) {
  return [record.checkInStatus, record.checkOutStatus].some((status) =>
    ['LATE', 'EARLY_LEAVE', 'MISSING', 'ABNORMAL'].includes(status || ''),
  )
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

function formatDateTime(value?: string) {
  if (!value) return '-'
  const normalized = value.replace('T', ' ')
  const match = normalized.match(/^(\d{4}-\d{2}-\d{2})[ T](\d{2}:\d{2}:\d{2})/)
  return match ? `${match[1]} ${match[2]}` : normalized
}

function formatTime(value?: string) {
  if (!value) return '-'
  const match = value.match(/[ T](\d{2}:\d{2}:\d{2})/)
  return match ? match[1] : formatDateTime(value)
}

function flattenDepartments(nodes: DepartmentNode[], level = 0): DepartmentOption[] {
  return nodes.flatMap((node) => [
    {
      id: node.id,
      label: `${'　'.repeat(level)}${node.departmentName}`,
    },
    ...flattenDepartments(node.children || [], level + 1),
  ])
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

function nowTimeString() {
  const now = new Date()
  return now.toLocaleTimeString('zh-CN', { hour12: false })
}

onMounted(load)
</script>

<style scoped>
.attendance-page {
  display: grid;
  gap: 16px;
  min-width: 0;
}

.section-header,
.actions {
  display: flex;
  align-items: center;
  gap: 12px;
  justify-content: space-between;
}

.section-header h2 {
  margin: 0;
  font-size: 18px;
}

.section-header p,
.section-header span {
  margin: 4px 0 0;
  color: #667085;
  font-size: 13px;
}

.section-header.compact {
  align-items: flex-start;
}

.actions {
  justify-content: flex-start;
  margin-top: 18px;
}

.summary-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 16px;
  min-width: 0;
}

.summary-card,
.chart-card {
  min-width: 0;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 12px;
  margin-top: 16px;
}

.metric-item,
.dashboard-metrics > div {
  display: grid;
  gap: 4px;
  padding: 12px;
  border: 1px solid #edf0f5;
  border-radius: 8px;
  background: #fafbfc;
}

.metric-item strong,
.dashboard-metrics strong {
  font-size: 24px;
}

.metric-item span,
.dashboard-metrics span {
  color: #667085;
  font-size: 13px;
}

.metric-item.success strong {
  color: #16a34a;
}

.metric-item.warning strong {
  color: #d97706;
}

.metric-item.danger strong {
  color: #dc2626;
}

.metric-item.info strong {
  color: #2563eb;
}

.status-bars {
  display: grid;
  gap: 10px;
  margin-top: 16px;
}

.status-row {
  display: grid;
  grid-template-columns: 48px minmax(0, 1fr) 32px;
  align-items: center;
  gap: 10px;
  font-size: 13px;
}

.bar-track {
  height: 10px;
  overflow: hidden;
  border-radius: 999px;
  background: #eef2f7;
}

.bar-track i {
  display: block;
  height: 100%;
  min-width: 4px;
  border-radius: inherit;
}

.bar-track .normal {
  background: #22c55e;
}

.bar-track .late,
.bar-track .early {
  background: #f59e0b;
}

.bar-track .missing,
.bar-track .abnormal {
  background: #ef4444;
}

.bar-track .leave {
  background: #3b82f6;
}

.attendance-dashboard {
  display: grid;
  grid-template-columns: 160px minmax(0, 1fr) minmax(220px, 0.9fr);
  gap: 18px;
  align-items: center;
}

.ring {
  width: 132px;
  height: 132px;
  display: grid;
  place-content: center;
  border-radius: 50%;
  background:
    radial-gradient(circle at center, #fff 58%, transparent 60%),
    conic-gradient(#3b82f6 var(--rate), #e5e7eb 0);
  text-align: center;
}

.ring strong {
  font-size: 24px;
}

.ring span {
  color: #667085;
  font-size: 13px;
}

.dashboard-metrics {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(110px, 1fr));
  gap: 12px;
}

.absent-list {
  min-height: 120px;
  padding: 12px;
  border: 1px solid #fee2e2;
  border-radius: 8px;
  background: #fff7f7;
}

.absent-list h3 {
  margin: 0 0 10px;
  font-size: 15px;
}

.absent-list .el-tag {
  margin: 0 8px 8px 0;
}

.permission-tip,
.filters {
  margin-bottom: 12px;
}

.filters :deep(.el-select) {
  width: 220px;
}

@media (max-width: 1100px) {
  .summary-grid,
  .attendance-dashboard {
    grid-template-columns: 1fr;
  }

  .dashboard-metrics,
  .metric-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

.punch-dialog-body {
  display: grid;
  gap: 16px;
  justify-items: center;
  padding: 12px 0;
}

.punch-icon {
  width: 72px;
  height: 72px;
  display: grid;
  place-content: center;
  border-radius: 50%;
  font-size: 32px;
}

.punch-icon.in {
  background: #eff6ff;
}

.punch-icon.out {
  background: #f0fdf4;
}

.punch-time {
  font-size: 36px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  color: #1a1a2e;
}

.punch-label {
  font-size: 15px;
  color: #667085;
}

.punch-dialog-body .el-textarea {
  width: 100%;
}
</style>
