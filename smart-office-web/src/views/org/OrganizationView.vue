<template>
  <section class="page-grid">
    <div class="page-card">
      <header class="section-header">
        <h2>部门树</h2>
        <el-button @click="load">刷新</el-button>
      </header>
      <el-tree :data="departments" node-key="id" default-expand-all :props="{ label: 'departmentName', children: 'children' }" />
    </div>
    <div class="page-card">
      <header class="section-header">
        <h2>岗位列表</h2>
      </header>
      <el-table v-loading="loading" :data="positions" border>
        <el-table-column prop="positionCode" label="岗位编码" min-width="130" />
        <el-table-column prop="positionName" label="岗位名称" min-width="150" />
        <el-table-column prop="departmentName" label="部门" min-width="150" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { getDepartmentTree, getPositions } from '@/api/org'
import type { DepartmentNode } from '@/types/api'

const departments = ref<DepartmentNode[]>([])
const positions = ref<unknown[]>([])
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    const [tree, positionPage] = await Promise.all([
      getDepartmentTree(),
      getPositions({ current: 1, size: 20 }),
    ])
    departments.value = tree
    positions.value = (positionPage as { records: unknown[] }).records
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>
