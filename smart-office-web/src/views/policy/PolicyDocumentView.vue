<template>
  <section class="policy-page">
    <div class="page-card">
      <header class="table-header">
        <div>
          <h2>制度文档</h2>
          <p>浏览企业制度、流程说明和公告文档，支持全文检索。</p>
        </div>
        <el-button v-if="canManage" type="primary" @click="$router.push('/policies/manage')">制度管理</el-button>
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
        <el-table-column prop="title" label="标题" min-width="220" show-overflow-tooltip />
        <el-table-column prop="summary" label="摘要" min-width="280" show-overflow-tooltip />
        <el-table-column prop="documentVersion" label="版本" width="110" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="publishedAt" label="发布时间" min-width="170" />
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openDetail(row)">详情</el-button>
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
import { computed, onMounted, reactive, ref } from 'vue'
import { getPolicies, getPolicyDetail } from '@/api/policy'
import { useAuthStore } from '@/stores/auth'
import type { PolicyDocument } from '@/types/api'

const authStore = useAuthStore()
const canManage = computed(() => authStore.hasPermission('policy:manage'))
const policies = ref<PolicyDocument[]>([])
const loading = ref(false)
const total = ref(0)
const detailVisible = ref(false)
const selectedPolicy = ref<PolicyDocument | null>(null)

const query = reactive({
  current: 1,
  size: 10,
  keyword: '',
  status: '',
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

async function openDetail(row: PolicyDocument) {
  selectedPolicy.value = await getPolicyDetail(row.id)
  detailVisible.value = true
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
