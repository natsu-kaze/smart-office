<template>
  <section class="page-card">
    <header class="table-header">
      <el-segmented v-model="tab" :options="tabs" @change="load" />
      <el-button type="primary" @click="openCreateDialog">新建审批</el-button>
    </header>

    <el-table v-loading="loading" :data="approvals" border>
      <el-table-column prop="title" label="标题" min-width="180" />
      <el-table-column prop="approvalType" label="类型" width="110" />
      <el-table-column prop="applicantName" label="申请人" width="120" />
      <el-table-column prop="currentApproverName" label="当前审批人" width="140" />
      <el-table-column prop="status" label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="statusTag(row.status)">{{ statusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="submittedAt" label="提交时间" min-width="170" />
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-space>
            <el-button
              v-if="row.status === 'DRAFT' || row.status === 'WITHDRAWN'"
              size="small"
              type="primary"
              @click="handleSubmit(row)"
            >
              提交
            </el-button>
            <template v-if="tab === 'todos' && row.status === 'PENDING'">
              <el-button size="small" type="success" @click="handleApprove(row)">通过</el-button>
              <el-button size="small" type="danger" plain @click="handleReject(row)">驳回</el-button>
            </template>
          </el-space>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" title="新建审批" width="520px">
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
        <el-form-item label="内容">
          <el-input v-model="form.content" type="textarea" :rows="4" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button @click="saveDraft(false)">保存草稿</el-button>
        <el-button type="primary" @click="saveDraft(true)">保存并提交</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  approveApproval,
  createApproval,
  getApprovalTodos,
  getMyApprovals,
  rejectApproval,
  submitApproval,
} from '@/api/approval'
import type { ApprovalItem } from '@/types/api'

const tabs = [
  { label: '我的申请', value: 'my' },
  { label: '我的审批待办', value: 'todos' },
]

const tab = ref('my')
const approvals = ref<ApprovalItem[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const form = reactive({
  approvalType: 'LEAVE',
  title: '',
  content: '',
  amount: undefined as number | undefined,
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
  form.approvalType = 'LEAVE'
  form.title = ''
  form.content = ''
  form.amount = undefined
  dialogVisible.value = true
}

async function saveDraft(submitAfterCreate: boolean) {
  if (!form.title.trim()) {
    ElMessage.warning('请填写审批标题')
    return
  }
  const approval = await createApproval({ ...form })
  if (submitAfterCreate) {
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

function statusText(status: string) {
  const map: Record<string, string> = {
    DRAFT: '草稿',
    PENDING: '待审批',
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
    APPROVED: 'success',
    REJECTED: 'danger',
    WITHDRAWN: 'info',
    CLOSED: 'info',
  }
  return map[status] || 'primary'
}

onMounted(load)
</script>

<style scoped>
.amount-input {
  width: 100%;
}
</style>
