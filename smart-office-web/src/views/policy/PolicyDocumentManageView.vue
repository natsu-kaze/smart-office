<template>
  <section class="policy-manage-page">
    <div class="page-card">
      <header class="table-header">
        <div>
          <h2>制度管理</h2>
          <p>创建、编辑、发布和归档企业制度文档。</p>
        </div>
        <el-space wrap>
          <el-button @click="$router.push('/policies')">返回浏览</el-button>
          <el-button @click="openUploadDialog">上传制度</el-button>
          <el-button type="primary" @click="openCreateDialog">新建制度</el-button>
        </el-space>
      </header>

      <el-form class="filters" :model="query" inline>
        <el-form-item label="关键词">
          <el-input v-model="query.keyword" clearable placeholder="标题、摘要、正文" @keyup.enter="search" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" clearable placeholder="全部">
            <el-option label="草稿" value="DRAFT" />
            <el-option label="已发布" value="PUBLISHED" />
            <el-option label="已归档" value="ARCHIVED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">搜索</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="policies" border>
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
        <el-table-column prop="summary" label="摘要" min-width="240" show-overflow-tooltip />
        <el-table-column prop="documentVersion" label="版本" width="100" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="publishedAt" label="发布时间" min-width="160" />
        <el-table-column label="操作" width="380" fixed="right">
          <template #default="{ row }">
            <el-space wrap>
              <el-button size="small" @click="openDetail(row)">详情</el-button>
              <el-button size="small" type="primary" plain @click="openEditDialog(row)">编辑</el-button>
              <el-button
                v-if="row.status !== 'PUBLISHED'"
                size="small"
                type="success"
                plain
                @click="handleStatus(row, 'PUBLISHED')"
              >
                发布
              </el-button>
              <el-button v-if="row.status !== 'DRAFT'" size="small" plain @click="handleStatus(row, 'DRAFT')">
                转草稿
              </el-button>
              <el-button
                v-if="row.status !== 'ARCHIVED'"
                size="small"
                type="warning"
                plain
                @click="handleStatus(row, 'ARCHIVED')"
              >
                归档
              </el-button>
              <el-button size="small" type="danger" plain @click="handleDelete(row)">删除</el-button>
            </el-space>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination
          v-model:current-page="query.current"
          v-model:page-size="query.size"
          layout="total, sizes, prev, pager, next"
          :page-sizes="[10, 20, 50]"
          :total="total"
          @current-change="load"
          @size-change="load"
        />
      </div>
    </div>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑制度' : '新建制度'" width="680px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="标题" required>
          <el-input v-model="form.title" />
        </el-form-item>
        <el-form-item label="版本">
          <el-input v-model="form.documentVersion" placeholder="例如 v1.0" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio-button label="DRAFT">草稿</el-radio-button>
            <el-radio-button label="PUBLISHED">发布</el-radio-button>
            <el-radio-button label="ARCHIVED">归档</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="摘要">
          <el-input v-model="form.summary" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="正文">
          <el-input v-model="form.content" type="textarea" :rows="10" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="uploadVisible" title="上传制度文档" width="560px">
      <el-form :model="uploadForm" label-width="90px">
        <el-form-item label="文件" required>
          <el-upload
            drag
            :auto-upload="false"
            :limit="1"
            accept=".pdf,.txt,.md"
            :on-change="handleUploadChange"
            :on-remove="handleUploadRemove"
          >
            <div class="upload-title">拖拽或点击选择文件</div>
            <template #tip>
              <div class="upload-tip">支持 PDF、TXT、Markdown，上传后自动解析正文并同步检索索引。</div>
            </template>
          </el-upload>
        </el-form-item>
        <el-form-item label="标题">
          <el-input v-model="uploadForm.title" placeholder="不填则使用文件名" />
        </el-form-item>
        <el-form-item label="版本">
          <el-input v-model="uploadForm.documentVersion" placeholder="例如 v1.0" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="uploadForm.status">
            <el-radio-button label="DRAFT">草稿</el-radio-button>
            <el-radio-button label="PUBLISHED">发布</el-radio-button>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="uploadVisible = false">取消</el-button>
        <el-button type="primary" :loading="uploading" @click="handleUploadPolicy">上传并解析</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="detailVisible" title="制度详情" size="560px">
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
import type { UploadFile } from 'element-plus'
import { createPolicy, deletePolicy, getPolicies, getPolicyDetail, updatePolicy, uploadPolicy } from '@/api/policy'
import type { ApiId, PolicyDocument } from '@/types/api'

type PolicyStatus = 'DRAFT' | 'PUBLISHED' | 'ARCHIVED'

const policies = ref<PolicyDocument[]>([])
const loading = ref(false)
const total = ref(0)
const dialogVisible = ref(false)
const uploadVisible = ref(false)
const uploading = ref(false)
const detailVisible = ref(false)
const selectedPolicy = ref<PolicyDocument | null>(null)
const editingId = ref<ApiId | null>(null)
const selectedUploadFile = ref<File | null>(null)

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
  status: 'DRAFT' as PolicyStatus,
})

const uploadForm = reactive({
  title: '',
  documentVersion: 'v1.0',
  status: 'DRAFT' as PolicyStatus,
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

function search() {
  query.current = 1
  load()
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
  form.status = 'DRAFT'
  editingId.value = null
}

function openCreateDialog() {
  resetForm()
  dialogVisible.value = true
}

function openUploadDialog() {
  uploadForm.title = ''
  uploadForm.documentVersion = 'v1.0'
  uploadForm.status = 'DRAFT'
  selectedUploadFile.value = null
  uploadVisible.value = true
}

function openEditDialog(row: PolicyDocument) {
  editingId.value = row.id
  form.title = row.title
  form.content = row.content || ''
  form.summary = row.summary || ''
  form.documentVersion = row.documentVersion || 'v1.0'
  form.status = (row.status || 'DRAFT') as PolicyStatus
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

async function handleUploadPolicy() {
  if (!selectedUploadFile.value) {
    ElMessage.warning('请选择制度文件')
    return
  }
  uploading.value = true
  try {
    await uploadPolicy(selectedUploadFile.value, { ...uploadForm })
    ElMessage.success('制度文件已上传并解析')
    uploadVisible.value = false
    await load()
  } finally {
    uploading.value = false
  }
}

function handleUploadChange(file: UploadFile) {
  selectedUploadFile.value = file.raw || null
}

function handleUploadRemove() {
  selectedUploadFile.value = null
}

async function handleStatus(row: PolicyDocument, status: PolicyStatus) {
  await updatePolicy(row.id, {
    title: row.title,
    content: row.content || '',
    summary: row.summary || '',
    documentVersion: row.documentVersion || 'v1.0',
    status,
  })
  ElMessage.success(`状态已切换为${statusText(status)}`)
  await load()
}

async function handleDelete(row: PolicyDocument) {
  await ElMessageBox.confirm(`确认删除「${row.title}」吗？`, '删除制度', {
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
.policy-manage-page {
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

.filters {
  margin-bottom: 12px;
}

.filters :deep(.el-input),
.filters :deep(.el-select) {
  width: 220px;
}

.upload-title {
  color: #344054;
  font-size: 15px;
}

.upload-tip {
  color: #667085;
  font-size: 12px;
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
