<template>
  <section class="message-page">
    <div class="page-card">
      <header class="table-header">
        <h2>待办中心</h2>
        <el-button @click="load">刷新</el-button>
      </header>
      <el-table v-loading="loading" :data="todos" border>
        <el-table-column label="待办标题" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">{{ localizeText(row.title) }}</template>
        </el-table-column>
        <el-table-column label="来源" width="120">
          <template #default="{ row }">{{ businessTypeText(row.businessType) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="row.status === 'DONE' ? 'success' : 'warning'">
              {{ todoStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" min-width="170">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="130">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'PENDING'"
              size="small"
              type="primary"
              plain
              @click="handleCompleteTodo(row.id)"
            >
              标记完成
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div class="page-card announcement-card">
      <header class="table-header">
        <div>
          <h2>公告栏</h2>
        </div>
        <el-space wrap>
          <el-button v-if="canPublishAnnouncement" type="primary" @click="openAnnouncementDialog">
            发布公告
          </el-button>
          <el-button @click="load">刷新</el-button>
        </el-space>
      </header>
      <el-empty v-if="!announcements.length && !loading" description="暂无公告" :image-size="80" />
      <div v-else v-loading="loading" class="announcement-list">
        <article v-for="item in announcements" :key="item.id" class="announcement-item">
          <div>
            <h3>{{ localizeText(item.title) }}</h3>
            <p>{{ localizeText(item.content) }}</p>
            <div class="announcement-meta">
              <span v-if="item.senderName">发布者：{{ item.senderName }}</span>
              <span>{{ formatTime(item.createTime) }}</span>
            </div>
          </div>
          <div class="announcement-actions">
            <el-tag :type="item.readStatus === 1 ? 'info' : 'warning'">
              {{ item.readStatus === 1 ? '已读' : '未读' }}
            </el-tag>
            <el-button v-if="item.readStatus === 0" size="small" type="primary" plain @click="handleRead(item.id)">
              已读
            </el-button>
            <el-button size="small" type="danger" plain @click="handleDeleteMessage(item.id)">删除</el-button>
          </div>
        </article>
      </div>
    </div>

    <div class="page-card">
      <header class="table-header">
        <div>
          <h2>通知消息</h2>
          <p>审批、考勤、文件等普通通知集中在这里处理。</p>
        </div>
        <el-space wrap>
          <el-tag>未读 {{ unreadCount }}</el-tag>
          <el-button :disabled="!selectedMessages.length" @click="handleBatchRead">批量已读</el-button>
          <el-button :disabled="!selectedMessages.length" type="danger" plain @click="handleBatchDelete">
            批量删除
          </el-button>
        </el-space>
      </header>

      <el-tabs v-model="messageTab" @tab-change="handleMessageTabChange">
        <el-tab-pane label="全部" name="all" />
        <el-tab-pane label="未读" name="unread" />
        <el-tab-pane label="已读" name="read" />
      </el-tabs>

      <el-table
        v-loading="loading"
        :data="messages"
        border
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="48" />
        <el-table-column label="标题" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">{{ localizeText(row.title) }}</template>
        </el-table-column>
        <el-table-column label="内容" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">{{ localizeText(row.content) }}</template>
        </el-table-column>
        <el-table-column label="来源" width="110">
          <template #default="{ row }">{{ businessTypeText(row.businessType) }}</template>
        </el-table-column>
        <el-table-column label="创建时间" min-width="155">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.readStatus === 1 ? 'info' : 'warning'">
              {{ row.readStatus === 1 ? '已读' : '未读' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160">
          <template #default="{ row }">
            <el-space>
              <el-button v-if="row.readStatus === 0" size="small" type="primary" plain @click="handleRead(row.id)">
                已读
              </el-button>
              <el-button size="small" type="danger" plain @click="handleDeleteMessage(row.id)">删除</el-button>
            </el-space>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination
          v-model:current-page="messagePage.current"
          v-model:page-size="messagePage.size"
          layout="total, prev, pager, next"
          :total="messagePage.total"
          @current-change="loadMessages"
        />
      </div>
    </div>

    <el-dialog v-model="announcementVisible" title="发布公告" width="560px">
      <el-form label-width="96px">
        <el-form-item label="接收范围">
          <el-select v-model="announcementForm.targetType" class="full-width">
            <el-option label="全部员工" value="ALL" />
            <el-option label="指定部门" value="DEPARTMENT" />
            <el-option label="部门及下级" value="SUB_DEPARTMENTS" />
            <el-option label="主管" value="MANAGERS" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="needsDepartment" label="部门">
          <el-select v-model="announcementForm.departmentId" class="full-width" filterable>
            <el-option v-for="item in departmentOptions" :key="item.id" :label="item.label" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="标题" required>
          <el-input v-model="announcementForm.title" placeholder="请输入公告标题" />
        </el-form-item>
        <el-form-item label="内容" required>
          <el-input v-model="announcementForm.content" type="textarea" :rows="5" placeholder="请输入公告内容" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="announcementVisible = false">取消</el-button>
        <el-button type="primary" @click="handlePublishAnnouncement">发布</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  batchDeleteMessages,
  batchMarkMessagesRead,
  completeMessageTodo,
  deleteMessage,
  getMessageTodos,
  getMessages,
  getUnreadCount,
  markMessageRead,
  publishAnnouncement,
} from '@/api/message'
import { getDepartmentTree } from '@/api/org'
import { useAuthStore } from '@/stores/auth'
import type { ApiId, DepartmentNode, MessageNotice, MessageTodo } from '@/types/api'

interface DepartmentOption {
  id: ApiId
  label: string
}

const authStore = useAuthStore()
const todos = ref<MessageTodo[]>([])
const messages = ref<MessageNotice[]>([])
const announcements = ref<MessageNotice[]>([])
const selectedMessages = ref<MessageNotice[]>([])
const departments = ref<DepartmentNode[]>([])
const unreadCount = ref(0)
const loading = ref(false)
const announcementVisible = ref(false)
const messageTab = ref('all')

const messagePage = reactive({
  current: 1,
  size: 20,
  total: 0,
})

const canPublishAnnouncement = computed(() => authStore.hasPermission('message:announcement:send'))
const needsDepartment = computed(() =>
  ['DEPARTMENT', 'SUB_DEPARTMENTS'].includes(announcementForm.targetType),
)
const departmentOptions = computed(() => flattenDepartments(departments.value))

const announcementForm = reactive({
  targetType: 'ALL',
  departmentId: undefined as ApiId | undefined,
  title: '',
  content: '',
})

const businessTypeMap: Record<string, string> = {
  APPROVAL: '审批',
  ATTENDANCE: '考勤',
  ANNOUNCEMENT: '公告',
  FILE: '文件',
  POLICY: '制度',
  AI: 'AI',
  GENERAL: '系统',
}

const statusTextMap: Record<string, string> = {
  PENDING: '待处理',
  DONE: '已完成',
}

const attendanceStatusMap: Record<string, string> = {
  NORMAL: '正常',
  LATE: '迟到',
  EARLY_LEAVE: '早退',
  MISSING: '缺卡',
  LEAVE: '请假',
  ABNORMAL: '异常',
  UNKNOWN: '未知',
}

async function load() {
  loading.value = true
  try {
    const [todoPage, announcementPage, unread] = await Promise.all([
      getMessageTodos({ current: 1, size: 20 }),
      getMessages({ current: 1, size: 6, businessType: 'ANNOUNCEMENT' }),
      getUnreadCount(),
    ])
    todos.value = todoPage.records
    announcements.value = announcementPage.records
    unreadCount.value = unread
    selectedMessages.value = []
    await loadMessages()
  } finally {
    loading.value = false
  }
}

async function loadMessages() {
  const params: Record<string, unknown> = {
    current: messagePage.current,
    size: messagePage.size,
    excludeBusinessType: 'ANNOUNCEMENT',
  }
  if (messageTab.value === 'unread') {
    params.readStatus = 0
  } else if (messageTab.value === 'read') {
    params.readStatus = 1
  }
  const page = await getMessages(params)
  messages.value = page.records
  messagePage.total = page.total
}

function handleMessageTabChange() {
  messagePage.current = 1
  loadMessages()
}

async function openAnnouncementDialog() {
  if (!departments.value.length) {
    departments.value = await getDepartmentTree()
  }
  announcementVisible.value = true
}

async function handlePublishAnnouncement() {
  if (!announcementForm.title.trim() || !announcementForm.content.trim()) {
    ElMessage.warning('请填写公告标题和内容')
    return
  }
  if (needsDepartment.value && !announcementForm.departmentId) {
    ElMessage.warning('请选择接收部门')
    return
  }
  const result = await publishAnnouncement({
    title: announcementForm.title,
    content: announcementForm.content,
    targetType: announcementForm.targetType,
    departmentId: needsDepartment.value ? announcementForm.departmentId : null,
  })
  ElMessage.success(`公告已发送给 ${result.recipientCount} 人`)
  announcementVisible.value = false
  announcementForm.title = ''
  announcementForm.content = ''
  await load()
}

async function handleCompleteTodo(id: ApiId) {
  await completeMessageTodo(id)
  ElMessage.success('待办已完成')
  await load()
}

async function handleRead(id: ApiId) {
  await markMessageRead(id)
  ElMessage.success('消息已读')
  await load()
}

async function handleDeleteMessage(id: ApiId) {
  await ElMessageBox.confirm('确定删除这条通知吗？', '删除通知', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消',
  })
  await deleteMessage(id)
  ElMessage.success('通知已删除')
  await load()
}

async function handleBatchRead() {
  const ids = selectedMessages.value
    .filter((message) => message.readStatus === 0)
    .map((message) => message.id)
  if (!ids.length) {
    ElMessage.info('选中的消息都已读了')
    return
  }
  await batchMarkMessagesRead(ids)
  ElMessage.success(`已标记 ${ids.length} 条消息为已读`)
  await load()
}

async function handleBatchDelete() {
  const ids = selectedMessages.value.map((message) => message.id)
  if (!ids.length) return
  await ElMessageBox.confirm(`确定删除选中的 ${ids.length} 条通知吗？`, '批量删除', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消',
  })
  await batchDeleteMessages(ids)
  ElMessage.success(`已删除 ${ids.length} 条通知`)
  await load()
}

function handleSelectionChange(selection: MessageNotice[]) {
  selectedMessages.value = selection
}

function businessTypeText(type?: string) {
  if (!type) return '-'
  return businessTypeMap[type] || type
}

function todoStatusText(status: string) {
  return statusTextMap[status] || status
}

function localizeText(text?: string) {
  if (!text) return '-'
  if (text.startsWith('Approval todo: ')) {
    return text.replace('Approval todo: ', '审批待办：')
  }
  if (text === 'New approval todo') return '新的审批待办'
  if (text === 'Approval passed') return '审批已通过'
  if (text === 'Approval rejected') return '审批已拒绝'
  if (text === 'Attendance abnormal') return '考勤异常'

  const approvedMatch = text.match(/^(.+) has been approved$/)
  if (approvedMatch) return `${approvedMatch[1]} 已通过`

  const rejectedMatch = text.match(/^(.+) has been rejected$/)
  if (rejectedMatch) return `${rejectedMatch[1]} 已拒绝`

  const attendanceMatch = text.match(
    /^Attendance abnormal on (.+): check-in=(.+), check-out=(.+)$/,
  )
  if (attendanceMatch) {
    return `${attendanceMatch[1]} 考勤异常：签到=${attendanceStatusText(
      attendanceMatch[2],
    )}，签退=${attendanceStatusText(attendanceMatch[3])}`
  }

  return text
}

function attendanceStatusText(status: string) {
  return attendanceStatusMap[status] || status
}

function formatTime(value?: string) {
  if (!value) return '-'
  return value.replace('T', ' ')
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

onMounted(load)
</script>

<style scoped>
.message-page {
  display: grid;
  gap: 16px;
}

.table-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.table-header h2 {
  margin: 0;
  font-size: 18px;
}

.table-header p {
  margin: 4px 0 0;
  color: #667085;
  font-size: 13px;
}

.announcement-list {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 12px;
}

.announcement-item {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  min-height: 112px;
  padding: 14px;
  border: 1px solid #dbeafe;
  border-radius: 8px;
  background: #f8fbff;
}

.announcement-actions {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;
  flex: 0 0 auto;
}

.announcement-item h3 {
  margin: 0 0 8px;
  font-size: 16px;
}

.announcement-item p {
  margin: 0 0 12px;
  color: #344054;
  line-height: 1.6;
}

.announcement-item span {
  color: #667085;
  font-size: 12px;
}

.announcement-meta {
  display: flex;
  gap: 16px;
  color: #667085;
  font-size: 12px;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}

.full-width {
  width: 100%;
}
</style>
