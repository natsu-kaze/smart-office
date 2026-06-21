<template>
  <section class="page-card">
    <header class="table-header">
      <div class="filters">
        <el-input v-model="keyword" clearable placeholder="搜索用户名、姓名、手机号" @keyup.enter="loadUsers" />
        <el-button type="primary" @click="loadUsers">查询</el-button>
      </div>
      <el-space>
        <el-button type="primary" @click="openCreateDialog">新建用户</el-button>
        <el-button @click="loadUsers">刷新</el-button>
      </el-space>
    </header>

    <el-table v-loading="loading" :data="users" border>
      <el-table-column prop="id" label="用户ID" width="180" />
      <el-table-column prop="username" label="用户名" min-width="130" />
      <el-table-column prop="realName" label="姓名" min-width="120" />
      <el-table-column prop="phone" label="手机号" min-width="140" />
      <el-table-column prop="email" label="邮箱" min-width="180" />
      <el-table-column label="角色" min-width="180">
        <template #default="{ row }">{{ row.roleNames?.join('、') || '-' }}</template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="130" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openRoleDialog(row)">分配角色</el-button>
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

    <el-dialog v-model="createDialogVisible" title="新建用户" width="480px">
      <el-form :model="createForm" label-width="80px">
        <el-form-item label="用户名" required>
          <el-input v-model="createForm.username" placeholder="登录用户名" />
        </el-form-item>
        <el-form-item label="密码" required>
          <el-input v-model="createForm.password" type="password" show-password placeholder="登录密码" />
        </el-form-item>
        <el-form-item label="姓名" required>
          <el-input v-model="createForm.realName" placeholder="真实姓名" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="createForm.phone" placeholder="手机号" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="createForm.email" placeholder="邮箱地址" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="handleCreate">创建</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="roleDialogVisible" title="分配用户角色" width="460px">
      <el-form label-width="88px">
        <el-form-item label="用户">
          <el-input :model-value="selectedUser?.realName || selectedUser?.username" disabled />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="selectedRoleIds" multiple clearable placeholder="请选择角色" style="width: 100%">
            <el-option v-for="role in roleOptions" :key="role.id" :label="role.roleName" :value="role.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="roleDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingRoles" @click="saveUserRoles">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { assignUserRoles, createUser, getRoleOptions, getUsers } from '@/api/system'
import type { ApiId, RoleItem, UserItem } from '@/types/api'

const users = ref<UserItem[]>([])
const roleOptions = ref<RoleItem[]>([])
const keyword = ref('')
const current = ref(1)
const size = ref(10)
const total = ref(0)
const loading = ref(false)
const roleDialogVisible = ref(false)
const savingRoles = ref(false)
const selectedUser = ref<UserItem | null>(null)
const selectedRoleIds = ref<ApiId[]>([])
const createDialogVisible = ref(false)
const creating = ref(false)
const createForm = reactive({
  username: '',
  password: '',
  realName: '',
  phone: '',
  email: '',
})

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

async function loadRoles() {
  roleOptions.value = await getRoleOptions()
}

function openRoleDialog(row: UserItem) {
  selectedUser.value = row
  selectedRoleIds.value = [...(row.roleIds || [])]
  roleDialogVisible.value = true
}

function openCreateDialog() {
  createForm.username = ''
  createForm.password = ''
  createForm.realName = ''
  createForm.phone = ''
  createForm.email = ''
  createDialogVisible.value = true
}

async function handleCreate() {
  if (!createForm.username.trim() || !createForm.password.trim() || !createForm.realName.trim()) {
    ElMessage.warning('用户名、密码、姓名不能为空')
    return
  }
  creating.value = true
  try {
    await createUser({ ...createForm })
    ElMessage.success('用户创建成功')
    createDialogVisible.value = false
    await loadUsers()
  } finally {
    creating.value = false
  }
}

async function saveUserRoles() {
  if (!selectedUser.value) return
  savingRoles.value = true
  try {
    await assignUserRoles(selectedUser.value.id, selectedRoleIds.value)
    ElMessage.success('用户角色已更新')
    roleDialogVisible.value = false
    await loadUsers()
  } finally {
    savingRoles.value = false
  }
}

onMounted(async () => {
  await Promise.all([loadUsers(), loadRoles()])
})
</script>

<style scoped>
.table-header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.filters {
  display: flex;
  gap: 10px;
}

.filters .el-input {
  width: 280px;
}

.pager {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
