<template>
  <section class="page-card">
    <header class="table-header">
      <el-segmented v-model="tab" :options="tabs" @change="load" />
      <el-button type="primary" @click="dialogVisible = true">新建审批</el-button>
    </header>
    <el-table v-loading="loading" :data="approvals" border>
      <el-table-column prop="title" label="标题" min-width="180" />
      <el-table-column prop="approvalType" label="类型" width="120" />
      <el-table-column prop="applicantName" label="申请人" width="120" />
      <el-table-column prop="currentApproverName" label="当前审批人" width="140" />
      <el-table-column prop="status" label="状态" width="120" />
      <el-table-column prop="submittedAt" label="提交时间" min-width="180" />
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
        <el-form-item label="标题"><el-input v-model="form.title" /></el-form-item>
        <el-form-item label="内容"><el-input v-model="form.content" type="textarea" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitDraft">保存草稿</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { createApproval, getApprovalTodos, getMyApprovals } from '@/api/approval'
import type { ApprovalItem } from '@/types/api'

const tabs = [
  { label: '我的申请', value: 'my' },
  { label: '我的待办', value: 'todos' },
]
const tab = ref('my')
const approvals = ref<ApprovalItem[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const form = reactive({ approvalType: 'LEAVE', title: '', content: '' })

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

async function submitDraft() {
  await createApproval({ ...form })
  ElMessage.success('草稿已创建')
  dialogVisible.value = false
  form.title = ''
  form.content = ''
  load()
}

onMounted(load)
</script>
