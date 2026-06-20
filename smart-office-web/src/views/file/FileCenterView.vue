<template>
  <section class="page-card">
    <header class="table-header">
      <h2>文件中心</h2>
      <el-space wrap>
        <input ref="fileInputRef" class="native-file" type="file" @change="handleNativeFileChange" />
        <el-button :loading="uploading" type="primary" @click="fileInputRef?.click()">上传文件</el-button>
        <el-button @click="load">刷新</el-button>
      </el-space>
    </header>

    <el-form class="filters" :model="query" inline>
      <el-form-item label="关键词">
        <el-input v-model="query.keyword" clearable placeholder="文件名、类型" @keyup.enter="search" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="search">搜索</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="files" border>
      <el-table-column prop="originalName" label="文件名" min-width="260" show-overflow-tooltip />
      <el-table-column label="文件类型" min-width="180" show-overflow-tooltip>
        <template #default="{ row }">{{ fileTypeText(row.contentType) }}</template>
      </el-table-column>
      <el-table-column label="大小" width="120">
        <template #default="{ row }">{{ formatSize(row.size) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="230" fixed="right">
        <template #default="{ row }">
          <el-space>
            <el-button size="small" type="primary" plain @click="handlePreview(row)">预览</el-button>
            <el-button size="small" @click="handleDownload(row)">下载</el-button>
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

    <el-dialog v-model="previewVisible" :title="previewFile?.originalName || '文件预览'" width="72%">
      <div v-if="previewUrl" class="preview-box">
        <img v-if="isImagePreview" :src="previewUrl" :alt="previewFile?.originalName" class="preview-image" />
        <iframe v-else :src="previewUrl" class="preview-frame" title="文件预览" />
      </div>
      <el-empty v-else description="暂无可预览地址" />
      <template #footer>
        <el-button @click="previewVisible = false">关闭</el-button>
        <el-button :disabled="!previewUrl" @click="openPreviewInNewWindow">新窗口打开</el-button>
        <el-button v-if="previewFile" type="primary" @click="handleDownload(previewFile)">下载</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deleteFile, downloadFile, getFilePreviewUrl, getMyFiles, uploadFile } from '@/api/file'
import type { FileRecord } from '@/types/api'

const files = ref<FileRecord[]>([])
const total = ref(0)
const loading = ref(false)
const uploading = ref(false)
const fileInputRef = ref<HTMLInputElement>()
const previewVisible = ref(false)
const previewUrl = ref('')
const previewFile = ref<FileRecord>()

const query = reactive({
  current: 1,
  size: 20,
  keyword: '',
})

const isImagePreview = computed(() => {
  const type = previewFile.value?.contentType || ''
  return type.startsWith('image/')
})

async function load() {
  loading.value = true
  try {
    const page = await getMyFiles({ ...query })
    files.value = page.records
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
  load()
}

async function handleNativeFileChange(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  uploading.value = true
  try {
    await uploadFile(file, 'FILE')
    ElMessage.success('文件已上传')
    await load()
  } finally {
    uploading.value = false
  }
}

async function handlePreview(row: FileRecord) {
  try {
    previewFile.value = row
    previewUrl.value = await getFilePreviewUrl(row.id)
    previewVisible.value = true
  } catch {
    previewFile.value = undefined
    previewUrl.value = ''
  }
}

async function handleDownload(row: FileRecord) {
  const blob = await downloadFile(row.id)
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = row.originalName
  document.body.appendChild(link)
  link.click()
  link.remove()
  URL.revokeObjectURL(url)
}

async function handleDelete(row: FileRecord) {
  await ElMessageBox.confirm(`确定删除文件「${row.originalName}」吗？`, '删除文件', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消',
  })
  await deleteFile(row.id)
  ElMessage.success('文件已删除')
  await load()
}

function openPreviewInNewWindow() {
  if (previewUrl.value) {
    window.open(previewUrl.value, '_blank', 'noopener,noreferrer')
  }
}

function fileTypeText(contentType?: string) {
  if (!contentType) return '-'
  if (contentType === 'application/pdf') return 'PDF 文档'
  if (contentType.startsWith('image/')) return '图片'
  if (contentType.startsWith('text/')) return '文本'
  return contentType
}

function formatSize(size: number) {
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  return `${(size / 1024 / 1024).toFixed(1)} MB`
}

onMounted(load)
</script>

<style scoped>
.native-file {
  display: none;
}

.filters {
  margin-bottom: 12px;
}

.filters :deep(.el-input) {
  width: 260px;
}

.preview-box {
  min-height: 420px;
}

.preview-image {
  display: block;
  max-width: 100%;
  max-height: 70vh;
  margin: 0 auto;
  object-fit: contain;
}

.preview-frame {
  width: 100%;
  height: 70vh;
  border: 1px solid var(--el-border-color);
  border-radius: 6px;
}
</style>
