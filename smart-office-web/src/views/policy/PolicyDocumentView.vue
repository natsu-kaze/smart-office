<template>
  <section class="policy-page">
    <div class="page-card">
      <header class="table-header">
        <div>
          <h2>制度文档</h2>
          <p>检索和维护企业制度、流程说明与公告文件</p>
        </div>
        <el-button type="primary" @click="openCreateDialog">新建制度</el-button>
      </header>

      <el-form class="filters" :model="query" inline>
        <el-form-item label="关键词">
          <el-input
            v-model="query.keyword"
            clearable
            placeholder="标题、摘要、正文"
            @keyup.enter="load"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" clearable placeholder="全部">
            <el-option label="草稿" value="DRAFT" />
            <el-option label="已发布" value="PUBLISHED" />
            <el-option label="已归档" value="ARCHIVED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="load">搜索</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="policies" border>
        <el-table-column prop="title" label="标题" min-width="220" show-overflow-tooltip />
        <el-table-column prop="summary" label="摘要" min-width="260" show-overflow-tooltip />
        <el-table-column prop="documentVersion" label="版本" width="110" />
        <el-table-column prop="status" label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="publishedAt" label="发布时间" min-width="170" />
        <el-table-column label="操作" width="210" fixed="right">
          <template #default="{ row }">
            <el-space>
              <el-button size="small" @click="openDetail(row)">详情</el-button>
              <el-button size="small" type="primary" plain @click="openEditDialog(row)">编辑</el-button>
              <el-button size="small" type="danger" plain @click="handleDelete(row)">删除</el-button>
            </el-space>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination
          v-model:current-page="query.current"
          v-model:page-size="query.size"
          layout="total, prev, pager, next"
          :total="total"
          @current-change="load"
          @size-change="load"
        />
      </div>
    </div>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑制度' : '新建制度'" width="640px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="标题">
          <el-input v-model="form.title" />
        </el-form-item>
        <el-form-item label="版本">
          <el-input v-model="form.documentVersion" placeholder="例如 v1.0" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status">
            <el-option label="草稿" value="DRAFT" />
            <el-option label="已发布" value="PUBLISHED" />
            <el-option label="已归档" value="ARCHIVED" />
          </el-select>
        </el-form-item>
        <el-form-item label="摘要">
          <el-input v-model="form.summary" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="正文">
          <el-input v-model="form.content" type="textarea" :rows="8" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="detailVisible" title="制度详情" size="520px">
      <template v-if="selectedPolicy">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="标题">{{ selectedPolicy.title }}</el-descriptions-item>
          <el-descriptions-item label="版本">{{ selectedPolicy.documentVersion || '-' }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ statusText(selectedPolicy.status) }}</el-descriptions-item>
          <el-descriptions-item label="发布时间">{{ selectedPolicy.publishedAt || '-' }}</el-descriptions-item>
        </el-descriptions>
        <h3>摘要</h3>
        <p class="detail-text">{{ selectedPolicy.summary || '-' }}</p>
        <h3>正文</h3>
        <p class="detail-text">{{ selectedPolicy.content || '-' }}</p>
      </template>
    </el-drawer>
  </section>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createPolicy, deletePolicy, getPolicies, getPolicyDetail, updatePolicy } from '@/api/policy'
import type { ApiId, PolicyDocument } from '@/types/api'

const policies = ref<PolicyDocument[]>([])
const loading = ref(false)
const total = ref(0)
const dialogVisible = ref(false)
const detailVisible = ref(false)
const selectedPolicy = ref<PolicyDocument | null>(null)
const editingId = ref<ApiId | null>(null)

const query = reactive({
  current: 1,
  size: 10,
  keyword: '',
  status: '',
})

const form = reactive({
  title: '',
  content: '',
  summary: '',
  documentVersion: 'v1.0',
  status: 'PUBLISHED',
})

async function load() {
  loading.value = true
  try {
    const page = await getPolicies({ ...query })
    policies.value = page.records
    total.value = page.total
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  query.current = 1
  query.keyword = ''
  query.status = ''
  load()
}

function resetForm() {
  form.title = ''
  form.content = ''
  form.summary = ''
  form.documentVersion = 'v1.0'
  form.status = 'PUBLISHED'
  editingId.value = null
}

function openCreateDialog() {
  resetForm()
  dialogVisible.value = true
}

function openEditDialog(row: PolicyDocument) {
  editingId.value = row.id
  form.title = row.title
  form.content = row.content || ''
  form.summary = row.summary || ''
  form.documentVersion = row.documentVersion || 'v1.0'
  form.status = row.status || 'DRAFT'
  dialogVisible.value = true
}

async function openDetail(row: PolicyDocument) {
  selectedPolicy.value = await getPolicyDetail(row.id)
  detailVisible.value = true
}

async function handleSave() {
  if (!form.title.trim()) {
    ElMessage.warning('请填写制度标题')
    return
  }
  if (editingId.value) {
    await updatePolicy(editingId.value, { ...form })
    ElMessage.success('制度已更新')
  } else {
    await createPolicy({ ...form })
    ElMessage.success('制度已创建')
  }
  dialogVisible.value = false
  await load()
}

async function handleDelete(row: PolicyDocument) {
  await ElMessageBox.confirm(`确认删除「${row.title}」？`, '删除制度', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消',
  })
  await deletePolicy(row.id)
  ElMessage.success('制度已删除')
  await load()
}

function statusText(status: string) {
  const map: Record<string, string> = {
    DRAFT: '草稿',
    PUBLISHED: '已发布',
    ARCHIVED: '已归档',
  }
  return map[status] || status
}

function statusTag(status: string) {
  const map: Record<string, 'success' | 'warning' | 'info' | 'primary'> = {
    DRAFT: 'info',
    PUBLISHED: 'success',
    ARCHIVED: 'warning',
  }
  return map[status] || 'primary'
}

onMounted(load)
</script>

<style scoped>
.policy-page {
  display: grid;
  gap: 16px;
}

.table-header p {
  margin: 4px 0 0;
  color: #6b7280;
  font-size: 13px;
}

.filters {
  margin-bottom: 12px;
}

.filters :deep(.el-input),
.filters :deep(.el-select) {
  width: 220px;
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
</style>
