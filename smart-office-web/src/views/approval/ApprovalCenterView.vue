<template>
  <section class="page-card">
    <header class="table-header">
      <el-segmented v-model="tab" :options="tabs" @change="load" />
      <div class="header-actions">
        <el-button v-if="canManageRules" @click="openRuleDialog">审批规则</el-button>
        <el-button type="primary" @click="openCreateDialog">新建审批</el-button>
      </div>
    </header>

    <el-table v-loading="loading" :data="approvals" border>
      <el-table-column prop="title" label="标题" min-width="180" />
      <el-table-column label="类型" width="110">
        <template #default="{ row }">{{ typeText(row.approvalType) }}</template>
      </el-table-column>
      <el-table-column prop="applicantName" label="申请人" width="120" />
      <el-table-column prop="currentApproverName" label="当前审批人" width="140" />
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="statusTag(row.status)">{{ statusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="提交时间" min-width="170">
        <template #default="{ row }">{{ formatDateTime(row.submittedAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="250" fixed="right">
        <template #default="{ row }">
          <el-space>
            <el-button size="small" @click="openDetail(row)">详情</el-button>
            <el-button v-if="isDraft(row)" size="small" @click="openEditDialog(row)">编辑</el-button>
            <el-button v-if="isDraft(row)" size="small" type="danger" plain @click="handleDelete(row)">删除</el-button>
            <el-button v-if="canSubmit(row)" size="small" type="primary" @click="handleSubmit(row)">提交</el-button>
            <template v-if="tab === 'todos' && isActiveApproval(row.status)">
              <el-button size="small" type="success" @click="handleApprove(row)">通过</el-button>
              <el-button size="small" type="danger" plain @click="handleReject(row)">驳回</el-button>
            </template>
          </el-space>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑草稿' : '新建审批'" width="560px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="类型">
          <el-select v-model="form.approvalType">
            <el-option label="请假" value="LEAVE" />
            <el-option label="加班" value="OVERTIME" />
            <el-option label="报销" value="EXPENSE" />
            <el-option label="通用" value="GENERAL" />
          </el-select>
        </el-form-item>
        <el-form-item label="标题">
          <el-input v-model="form.title" />
        </el-form-item>
        <el-form-item v-if="form.approvalType === 'EXPENSE'" label="金额">
          <el-input-number v-model="form.amount" :min="0" :precision="2" class="amount-input" />
        </el-form-item>
        <el-form-item v-if="form.approvalType === 'LEAVE'" label="请假日期">
          <el-date-picker
            v-model="leaveRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
          />
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="form.content" type="textarea" :rows="4" placeholder="请填写原因、说明或补充信息" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button @click="saveDraft(false)">保存草稿</el-button>
        <el-button type="primary" @click="saveDraft(true)">保存并提交</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="ruleDialogVisible" title="审批规则" width="820px">
      <el-alert
        type="info"
        :closable="false"
        show-icon
        title="逗号分隔顺序步骤，竖线 | 分隔同一步骤的或签人（任一审批即可）。支持：DEPARTMENT_LEADER / ROLE:角色编码 / USER:用户ID"
      />
      <el-table :data="rules" border class="rule-table">
        <el-table-column label="类型" width="90">
          <template #default="{ row }">{{ typeText(row.approvalType) }}</template>
        </el-table-column>
        <el-table-column prop="name" label="规则名称" min-width="140" show-overflow-tooltip />
        <el-table-column label="优先级" width="80" align="center">
          <template #default="{ row }">{{ row.priority ?? '-' }}</template>
        </el-table-column>
        <el-table-column prop="amountLimit" label="金额门槛" width="100">
          <template #default="{ row }">{{ row.amountLimit ?? '-' }}</template>
        </el-table-column>
        <el-table-column prop="requiredRoles" label="审批节点" min-width="200" show-overflow-tooltip />
        <el-table-column label="超时" width="110">
          <template #default="{ row }">
            <template v-if="row.timeoutHours">{{ row.timeoutHours }}h → {{ timeoutActionText(row.timeoutAction) }}</template>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="120" show-overflow-tooltip />
        <el-table-column label="操作" width="130" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="editRule(row)">编辑</el-button>
            <el-button link type="danger" @click="removeRule(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-divider content-position="left">{{ editingRuleId ? '编辑规则' : '新增规则' }}</el-divider>
      <el-form :model="ruleForm" label-width="110px" class="rule-form">
        <el-form-item label="类型">
          <el-select v-model="ruleForm.approvalType">
            <el-option label="请假" value="LEAVE" />
            <el-option label="加班" value="OVERTIME" />
            <el-option label="报销" value="EXPENSE" />
            <el-option label="通用" value="GENERAL" />
          </el-select>
        </el-form-item>
        <el-form-item label="规则名称">
          <el-input v-model="ruleForm.name" placeholder="例如：高管请假规则" />
        </el-form-item>
        <el-form-item label="优先级">
          <el-input-number v-model="ruleForm.priority" :min="0" :max="1000" :step="10" class="amount-input" />
          <span class="field-hint">越高越优先，默认 10</span>
        </el-form-item>
        <el-form-item label="适用部门">
          <el-input v-model="ruleForm.deptId" placeholder="部门 ID，留空表示全局" />
        </el-form-item>
        <el-form-item label="申请人角色">
          <el-input v-model="ruleForm.applicantRoleCode" placeholder="例如 MANAGER，留空表示不限" />
        </el-form-item>
        <el-form-item label="金额门槛">
          <el-input-number v-model="ruleForm.amountLimit" :min="0" :precision="2" class="amount-input" />
        </el-form-item>
        <el-form-item label="审批节点" required>
          <el-input v-model="ruleForm.requiredRoles" placeholder="例如 DEPARTMENT_LEADER|ROLE:HR,ROLE:FINANCE" />
          <template v-if="ruleForm.requiredRoles">
            <div class="token-preview">
              <template v-for="(step, sidx) in parsedSteps" :key="sidx">
                <span v-if="sidx > 0" class="step-arrow">→</span>
                <el-tag
                  v-for="(token, tidx) in step"
                  :key="tidx"
                  :type="tokenType(token)"
                  size="small"
                  class="token-tag"
                >
                  {{ token }}
                  <template v-if="tidx < step.length - 1">
                    <span class="or-badge">或</span>
                  </template>
                </el-tag>
              </template>
            </div>
          </template>
        </el-form-item>
        <el-form-item label="超时时间(h)">
          <el-input-number v-model="ruleForm.timeoutHours" :min="0" :max="8760" class="amount-input" />
          <span class="field-hint">留空不启用超时</span>
        </el-form-item>
        <el-form-item v-if="ruleForm.timeoutHours" label="超时操作">
          <el-select v-model="ruleForm.timeoutAction" placeholder="选择超时处理方式" clearable>
            <el-option label="自动通过" value="AUTO_APPROVE" />
            <el-option label="自动驳回" value="AUTO_REJECT" />
            <el-option label="跳过升级" value="ESCALATE" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="ruleForm.status">
            <el-radio-button :label="1">启用</el-radio-button>
            <el-radio-button :label="0">禁用</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="ruleForm.remark" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetRuleForm">清空</el-button>
        <el-button type="primary" @click="saveRule">保存规则</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="detailVisible" title="审批详情" size="560px">
      <template v-if="detail">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="标题">{{ detail.title }}</el-descriptions-item>
          <el-descriptions-item label="类型">{{ typeText(detail.approvalType) }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ statusText(detail.status) }}</el-descriptions-item>
          <el-descriptions-item label="申请人">{{ detail.applicantName }}</el-descriptions-item>
          <el-descriptions-item label="当前审批人">{{ detail.currentApproverName || '-' }}</el-descriptions-item>
          <el-descriptions-item v-if="detail.amount !== undefined" label="金额">{{ detail.amount }}</el-descriptions-item>
          <el-descriptions-item v-if="detail.leaveStartDate" label="请假日期">
            {{ detail.leaveStartDate }} 至 {{ detail.leaveEndDate }}
          </el-descriptions-item>
          <el-descriptions-item label="提交时间">{{ formatDateTime(detail.submittedAt) }}</el-descriptions-item>
          <el-descriptions-item label="完成时间">{{ formatDateTime(detail.completedAt) }}</el-descriptions-item>
        </el-descriptions>

        <h3>审批内容</h3>
        <p class="detail-text">{{ detail.content || '-' }}</p>

        <h3>流转记录</h3>
        <el-timeline>
          <el-timeline-item
            v-for="record in detail.records || []"
            :key="record.id"
            :timestamp="formatDateTime(record.createTime)"
            placement="top"
          >
            <strong>{{ actionText(record.action) }}</strong>
            <span class="timeline-operator">{{ record.operatorName || record.operatorUserId }}</span>
            <p>{{ statusText(record.fromStatus || '') }} -> {{ statusText(record.toStatus || '') }}</p>
            <p v-if="record.comment">{{ record.comment }}</p>
          </el-timeline-item>
        </el-timeline>
      </template>
    </el-drawer>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  approveApproval,
  createApproval,
  createApprovalRule,
  deleteApproval,
  deleteApprovalRule,
  getApprovalDetail,
  getApprovalRules,
  getApprovalTodos,
  getMyApprovals,
  rejectApproval,
  submitApproval,
  updateApproval,
  updateApprovalRule,
} from '@/api/approval'
import { useAuthStore } from '@/stores/auth'
import type { ApiId, ApprovalItem, ApprovalRule, ApprovalRulePayload } from '@/types/api'

const tabs = [
  { label: '我的申请', value: 'my' },
  { label: '我的审批待办', value: 'todos' },
]

const tab = ref('my')
const approvals = ref<ApprovalItem[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const detailVisible = ref(false)
const ruleDialogVisible = ref(false)
const detail = ref<ApprovalItem | null>(null)
const rules = ref<ApprovalRule[]>([])
const editingId = ref<ApiId | null>(null)
const editingRuleId = ref<ApiId | null>(null)
const leaveRange = ref<string[]>([])
const authStore = useAuthStore()
const canManageRules = computed(() => authStore.hasPermission('approval:rule:manage'))

const form = reactive({
  approvalType: 'LEAVE',
  title: '',
  content: '',
  amount: undefined as number | undefined,
})

const ruleForm = reactive<ApprovalRulePayload>({
  approvalType: 'LEAVE',
  name: '',
  priority: 10,
  amountLimit: undefined,
  applicantRoleCode: '',
  deptId: undefined,
  requiredRoles: 'DEPARTMENT_LEADER',
  timeoutHours: undefined,
  timeoutAction: undefined,
  status: 1,
  remark: '',
})

async function load() {
  loading.value = true
  try {
    const page = tab.value === 'my'
      ? await getMyApprovals({ current: 1, size: 20 })
      : await getApprovalTodos({ current: 1, size: 20 })
    approvals.value = page.records
  } finally {
    loading.value = false
  }
}

function openCreateDialog() {
  editingId.value = null
  form.approvalType = 'LEAVE'
  form.title = ''
  form.content = ''
  form.amount = undefined
  leaveRange.value = []
  dialogVisible.value = true
}

function openEditDialog(row: ApprovalItem) {
  editingId.value = row.id
  form.approvalType = row.approvalType
  form.title = row.title
  form.content = row.content || ''
  form.amount = row.amount
  leaveRange.value = row.leaveStartDate ? [row.leaveStartDate, row.leaveEndDate || row.leaveStartDate] : []
  dialogVisible.value = true
}

async function saveDraft(submitAfterSave: boolean) {
  if (!form.title.trim()) {
    ElMessage.warning('请填写审批标题')
    return
  }
  const payload = {
    ...form,
    leaveStartDate: form.approvalType === 'LEAVE' ? leaveRange.value[0] : undefined,
    leaveEndDate: form.approvalType === 'LEAVE' ? leaveRange.value[1] : undefined,
  }
  const approval = editingId.value
    ? await updateApproval(editingId.value, payload)
    : await createApproval(payload)
  if (submitAfterSave) {
    await submitApproval(approval.id)
    ElMessage.success('审批已提交，待办已生成')
  } else {
    ElMessage.success('草稿已保存')
  }
  dialogVisible.value = false
  await load()
}

async function handleSubmit(row: ApprovalItem) {
  await submitApproval(row.id)
  ElMessage.success('审批已提交')
  await load()
}

async function openDetail(row: ApprovalItem) {
  detail.value = await getApprovalDetail(row.id)
  detailVisible.value = true
}

async function handleApprove(row: ApprovalItem) {
  const { value } = await ElMessageBox.prompt('审批意见', '审批通过', {
    confirmButtonText: '通过',
    cancelButtonText: '取消',
    inputValue: '同意',
  })
  await approveApproval(row.id, value)
  ElMessage.success('已审批通过，申请人通知已生成')
  await load()
}

async function handleReject(row: ApprovalItem) {
  const { value } = await ElMessageBox.prompt('驳回原因', '驳回审批', {
    confirmButtonText: '驳回',
    cancelButtonText: '取消',
    inputValue: '不通过',
    confirmButtonClass: 'el-button--danger',
  })
  await rejectApproval(row.id, value)
  ElMessage.success('已驳回，申请人通知已生成')
  await load()
}

async function handleDelete(row: ApprovalItem) {
  await ElMessageBox.confirm(`确认删除草稿「${row.title}」吗？删除后无法恢复。`, '删除草稿', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消',
  })
  await deleteApproval(row.id)
  ElMessage.success('草稿已删除')
  await load()
}

async function openRuleDialog() {
  ruleDialogVisible.value = true
  await loadRules()
}

async function loadRules() {
  rules.value = await getApprovalRules()
}

function editRule(row: ApprovalRule) {
  editingRuleId.value = row.id
  ruleForm.approvalType = row.approvalType
  ruleForm.name = row.name || ''
  ruleForm.priority = row.priority ?? 10
  ruleForm.amountLimit = row.amountLimit
  ruleForm.applicantRoleCode = row.applicantRoleCode || ''
  ruleForm.deptId = row.deptId
  ruleForm.requiredRoles = row.requiredRoles
  ruleForm.timeoutHours = row.timeoutHours
  ruleForm.timeoutAction = row.timeoutAction
  ruleForm.status = row.status
  ruleForm.remark = row.remark || ''
}

function resetRuleForm() {
  editingRuleId.value = null
  ruleForm.approvalType = 'LEAVE'
  ruleForm.name = ''
  ruleForm.priority = 10
  ruleForm.amountLimit = undefined
  ruleForm.applicantRoleCode = ''
  ruleForm.deptId = undefined
  ruleForm.requiredRoles = 'DEPARTMENT_LEADER'
  ruleForm.timeoutHours = undefined
  ruleForm.timeoutAction = undefined
  ruleForm.status = 1
  ruleForm.remark = ''
}

async function saveRule() {
  if (!ruleForm.requiredRoles.trim()) {
    ElMessage.warning('请填写审批节点')
    return
  }
  if (editingRuleId.value) {
    await updateApprovalRule(editingRuleId.value, ruleForm)
    ElMessage.success('审批规则已更新')
  } else {
    await createApprovalRule(ruleForm)
    ElMessage.success('审批规则已创建')
  }
  resetRuleForm()
  await loadRules()
}

async function removeRule(row: ApprovalRule) {
  await ElMessageBox.confirm(`确认删除「${typeText(row.approvalType)}」审批规则吗？`, '删除审批规则', {
    type: 'warning',
  })
  await deleteApprovalRule(row.id)
  ElMessage.success('审批规则已删除')
  if (editingRuleId.value === row.id) {
    resetRuleForm()
  }
  await loadRules()
}

// Token preview: parse requiredRoles into step groups for visual display
const parsedSteps = computed(() => {
  if (!ruleForm.requiredRoles) return []
  return ruleForm.requiredRoles
    .split(',')
    .filter(s => s.trim())
    .map(step =>
      step
        .split('|')
        .filter(t => t.trim())
        .map(t => t.trim()),
    )
})

function tokenType(token: string): 'primary' | 'success' | 'warning' | 'danger' | 'info' {
  if (/^DEPARTMENT_LEADER$/i.test(token) || /^LEADER$/i.test(token)) return 'primary'
  if (/^ROLE:/i.test(token)) return 'success'
  if (/^USER:/i.test(token)) return 'warning'
  return 'info'
}

function isDraft(row: ApprovalItem) {
  return row.status === 'DRAFT'
}

function canSubmit(row: ApprovalItem) {
  return row.status === 'DRAFT' || row.status === 'WITHDRAWN'
}

function statusText(status: string) {
  const map: Record<string, string> = {
    DRAFT: '草稿',
    PENDING: '待审批',
    PROCESSING: '审批中',
    APPROVED: '已通过',
    REJECTED: '已驳回',
    WITHDRAWN: '已撤回',
    CLOSED: '已关闭',
  }
  return map[status] || status
}

function statusTag(status: string) {
  const map: Record<string, 'success' | 'warning' | 'danger' | 'info' | 'primary'> = {
    DRAFT: 'info',
    PENDING: 'warning',
    PROCESSING: 'primary',
    APPROVED: 'success',
    REJECTED: 'danger',
    WITHDRAWN: 'info',
    CLOSED: 'info',
  }
  return map[status] || 'primary'
}

function isActiveApproval(status: string) {
  return status === 'PENDING' || status === 'PROCESSING'
}

function typeText(type: string) {
  const map: Record<string, string> = {
    LEAVE: '请假',
    OVERTIME: '加班',
    EXPENSE: '报销',
    GENERAL: '通用',
  }
  return map[type] || type
}

function actionText(action: string) {
  const map: Record<string, string> = {
    CREATE: '创建',
    SUBMIT: '提交',
    APPROVE: '通过',
    REJECT: '驳回',
    WITHDRAW: '撤回',
    CLOSE: '关闭',
    DELETE: '删除',
  }
  return map[action] || action
}

function timeoutActionText(action?: string) {
  const map: Record<string, string> = {
    AUTO_APPROVE: '自动通过',
    AUTO_REJECT: '自动驳回',
    ESCALATE: '跳过升级',
  }
  return action ? (map[action] || action) : '-'
}

function formatDateTime(value?: string) {
  if (!value) return '-'
  const normalized = value.replace('T', ' ')
  const match = normalized.match(/^(\d{4}-\d{2}-\d{2})[ T](\d{2}:\d{2}:\d{2})/)
  return match ? `${match[1]} ${match[2]}` : normalized
}

onMounted(load)
</script>

<style scoped>
.amount-input {
  width: 100%;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.rule-table {
  margin-top: 12px;
}

.rule-form :deep(.el-select),
.rule-form :deep(.el-input-number) {
  width: 100%;
}

h3 {
  margin: 20px 0 8px;
  font-size: 15px;
}

.detail-text {
  margin: 0;
  color: #374151;
  line-height: 1.7;
  white-space: pre-wrap;
}

.timeline-operator {
  margin-left: 8px;
  color: #6b7280;
}

.token-preview {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 4px;
  margin-top: 6px;
}

.step-arrow {
  color: #909399;
  margin: 0 2px;
  font-weight: bold;
}

.token-tag {
  display: inline-flex;
  align-items: center;
  gap: 2px;
}

.or-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: #f56c6c;
  color: #fff;
  font-size: 10px;
  border-radius: 50%;
  width: 16px;
  height: 16px;
  margin-left: 2px;
}

.field-hint {
  margin-left: 8px;
  color: #909399;
  font-size: 12px;
}
</style>
