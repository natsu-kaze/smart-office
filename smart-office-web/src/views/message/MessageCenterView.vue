<template>
  <section class="message-page">
    <div class="page-card">
      <header class="table-header">
        <h2>待办中心</h2>
        <el-button @click="load">刷新</el-button>
      </header>
      <el-table v-loading="loading" :data="todos" border>
        <el-table-column prop="title" label="待办标题" min-width="180" />
        <el-table-column prop="businessType" label="业务类型" width="120" />
        <el-table-column prop="businessId" label="业务 ID" width="120" />
        <el-table-column prop="status" label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="row.status === 'DONE' ? 'success' : 'warning'">
              {{ row.status === 'DONE' ? '已完成' : '待处理' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" min-width="170" />
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'PENDING'"
              size="small"
              type="primary"
              plain
              @click="handleCompleteTodo(row.id)"
            >
              标记完成
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div class="page-card">
      <header class="table-header">
        <h2>通知消息</h2>
        <el-tag>未读 {{ unreadCount }}</el-tag>
      </header>
      <el-table v-loading="loading" :data="messages" border>
        <el-table-column prop="title" label="标题" min-width="160" />
        <el-table-column prop="content" label="内容" min-width="220" />
        <el-table-column prop="businessType" label="业务类型" width="120" />
        <el-table-column prop="createTime" label="创建时间" min-width="170" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.readStatus === 1 ? 'info' : 'warning'">
              {{ row.readStatus === 1 ? '已读' : '未读' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="110">
          <template #default="{ row }">
            <el-button
              v-if="row.readStatus === 0"
              size="small"
              type="primary"
              plain
              @click="handleRead(row.id)"
            >
              标记已读
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  completeMessageTodo,
  getMessageTodos,
  getMessages,
  getUnreadCount,
  markMessageRead,
} from '@/api/message'
import type { ApiId, MessageNotice, MessageTodo } from '@/types/api'

const todos = ref<MessageTodo[]>([])
const messages = ref<MessageNotice[]>([])
const unreadCount = ref(0)
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    const [todoPage, messagePage, unread] = await Promise.all([
      getMessageTodos({ current: 1, size: 20 }),
      getMessages({ current: 1, size: 20 }),
      getUnreadCount(),
    ])
    todos.value = todoPage.records
    messages.value = messagePage.records
    unreadCount.value = unread
  } finally {
    loading.value = false
  }
}

async function handleCompleteTodo(id: ApiId) {
  await completeMessageTodo(id)
  ElMessage.success('待办已完成')
  await load()
}

async function handleRead(id: ApiId) {
  await markMessageRead(id)
  ElMessage.success('消息已读')
  await load()
}

onMounted(load)
</script>

<style scoped>
.message-page {
  display: grid;
  gap: 16px;
}
</style>
