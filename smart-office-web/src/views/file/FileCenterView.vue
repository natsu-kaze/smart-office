<template>
  <section class="page-card">
    <header class="table-header">
      <h2>文件中心</h2>
      <el-space>
        <input ref="fileInputRef" class="native-file" type="file" @change="handleNativeFileChange" />
        <el-button :loading="uploading" type="primary" @click="fileInputRef?.click()">上传文件</el-button>
        <el-button @click="load">刷新</el-button>
      </el-space>
    </header>

    <el-table v-loading="loading" :data="files" border>
      <el-table-column prop="originalName" label="文件名" min-width="220" />
      <el-table-column prop="contentType" label="类型" min-width="160" />
      <el-table-column label="大小" width="120">
        <template #default="{ row }">{{ formatSize(row.size) }}</template>
      </el-table-column>
      <el-table-column prop="bucket" label="Bucket" width="150" />
      <el-table-column prop="objectKey" label="对象 Key" min-width="260" />
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-space>
            <el-button size="small" type="primary" plain @click="handlePreview(row)">预览</el-button>
            <el-button size="small" @click="handleDownload(row)">下载</el-button>
          </el-space>
        </template>
      </el-table-column>
    </el-table>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { downloadFile, getFilePreviewUrl, getMyFiles, uploadFile } from '@/api/file'
import type { FileRecord } from '@/types/api'

const files = ref<FileRecord[]>([])
const loading = ref(false)
const uploading = ref(false)
const fileInputRef = ref<HTMLInputElement>()

async function load() {
  loading.value = true
  try {
    const page = await getMyFiles({ current: 1, size: 20 })
    files.value = page.records
  } finally {
    loading.value = false
  }
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
  const url = await getFilePreviewUrl(row.id)
  window.open(url, '_blank', 'noopener,noreferrer')
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
</style>
