<template>
  <section class="page-card">
    <header class="table-header">
      <el-input v-model="keyword" clearable placeholder="搜索用户名/姓名/手机号" style="max-width: 280px" @keyup.enter="loadUsers" />
      <el-button type="primary" @click="loadUsers">查询</el-button>
    </header>
    <el-table v-loading="loading" :data="users" border>
      <el-table-column prop="username" label="用户名" min-width="130" />
      <el-table-column prop="realName" label="姓名" min-width="120" />
      <el-table-column prop="phone" label="手机号" min-width="140" />
      <el-table-column prop="email" label="邮箱" min-width="180" />
      <el-table-column label="角色" min-width="160">
        <template #default="{ row }">{{ row.roleNames?.join('、') || '-' }}</template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination
      v-model:current-page="current"
      v-model:page-size="size"
      :total="total"
      background
      layout="total, prev, pager, next"
      class="pager"
      @current-change="loadUsers"
    />
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { getUsers } from '@/api/system'
import type { UserItem } from '@/types/api'

const users = ref<UserItem[]>([])
const keyword = ref('')
const current = ref(1)
const size = ref(10)
const total = ref(0)
const loading = ref(false)

async function loadUsers() {
  loading.value = true
  try {
    const page = await getUsers({ current: current.value, size: size.value, keyword: keyword.value })
    users.value = page.records
    total.value = page.total
  } finally {
    loading.value = false
  }
}

onMounted(loadUsers)
</script>
