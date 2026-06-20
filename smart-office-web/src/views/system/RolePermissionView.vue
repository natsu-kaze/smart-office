<template>
  <section class="role-page">
    <div class="page-card role-list">
      <header class="table-header">
        <div class="filters">
          <el-input v-model="query.keyword" clearable placeholder="搜索角色编码、角色名称" @keyup.enter="loadRoles" />
          <el-button type="primary" @click="loadRoles">查询</el-button>
        </div>
        <el-button type="primary" @click="openRoleDialog()">新增角色</el-button>
      </header>

      <el-table v-loading="loading" :data="roles" border highlight-current-row @current-change="handleCurrentRole">
        <el-table-column prop="roleCode" label="角色编码" min-width="140" />
        <el-table-column prop="roleName" label="角色名称" min-width="140" />
        <el-table-column prop="sort" label="排序" width="80" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="180" show-overflow-tooltip />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click.stop="openRoleDialog(row)">编辑</el-button>
            <el-button link type="danger" @click.stop="handleDeleteRole(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="query.current"
        v-model:page-size="query.size"
        :total="total"
        background
        layout="total, prev, pager, next"
        class="pager"
        @current-change="loadRoles"
      />
    </div>

    <div class="page-card menu-panel">
      <header class="section-header">
        <div>
          <h2>菜单权限</h2>
          <p>{{ currentRole ? `当前角色：${currentRole.roleName}` : '请选择左侧角色后分配菜单和按钮权限' }}</p>
        </div>
        <el-button type="primary" :disabled="!currentRole" :loading="savingMenus" @click="saveRoleMenus">保存授权</el-button>
      </header>

      <el-tree
        ref="menuTreeRef"
        :data="menus"
        :props="treeProps"
        node-key="id"
        show-checkbox
        default-expand-all
        :expand-on-click-node="false"
      >
        <template #default="{ data }">
          <span class="menu-node">
            <el-tag size="small" :type="menuTagType(data.menuType)">{{ menuTypeText(data.menuType) }}</el-tag>
            <span>{{ data.menuName }}</span>
            <small v-if="data.permission">{{ data.permission }}</small>
          </span>
        </template>
      </el-tree>
    </div>

    <el-dialog v-model="roleDialogVisible" :title="editingRoleId ? '编辑角色' : '新增角色'" width="520px">
      <el-form :model="roleForm" label-width="88px">
        <el-form-item label="角色编码" required>
          <el-input v-model="roleForm.roleCode" placeholder="例如 ADMIN" />
        </el-form-item>
        <el-form-item label="角色名称" required>
          <el-input v-model="roleForm.roleName" placeholder="例如 管理员" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="roleForm.sort" :min="0" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="roleForm.status">
            <el-radio-button :label="1">启用</el-radio-button>
            <el-radio-button :label="0">禁用</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="roleForm.remark" type="textarea" :rows="3" placeholder="角色说明" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="roleDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingRole" @click="saveRole">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  assignRoleMenus,
  createRole,
  deleteRole,
  getMenusTree,
  getRoleMenuIds,
  getRoles,
  updateRole,
} from '@/api/system'
import type { ApiId, MenuNode, RoleItem, RoleSavePayload } from '@/types/api'

const roles = ref<RoleItem[]>([])
const menus = ref<MenuNode[]>([])
const total = ref(0)
const loading = ref(false)
const savingRole = ref(false)
const savingMenus = ref(false)
const roleDialogVisible = ref(false)
const editingRoleId = ref<ApiId | null>(null)
const currentRole = ref<RoleItem | null>(null)
const menuTreeRef = ref()

const query = reactive({
  current: 1,
  size: 10,
  keyword: '',
})

const roleForm = reactive<RoleSavePayload>({
  roleCode: '',
  roleName: '',
  sort: 0,
  status: 1,
  remark: '',
})

const treeProps = {
  label: 'menuName',
  children: 'children',
}

async function loadRoles() {
  loading.value = true
  try {
    const page = await getRoles(query)
    roles.value = page.records
    total.value = page.total
    if (!currentRole.value && roles.value.length > 0) {
      await handleCurrentRole(roles.value[0])
    }
  } finally {
    loading.value = false
  }
}

async function loadMenus() {
  menus.value = await getMenusTree()
}

async function handleCurrentRole(row?: RoleItem) {
  if (!row) return
  currentRole.value = row
  const menuIds = await getRoleMenuIds(row.id)
  await nextTick()
  menuTreeRef.value?.setCheckedKeys(menuIds)
}

function openRoleDialog(row?: RoleItem) {
  editingRoleId.value = row?.id || null
  roleForm.roleCode = row?.roleCode || ''
  roleForm.roleName = row?.roleName || ''
  roleForm.sort = row?.sort || 0
  roleForm.status = row?.status ?? 1
  roleForm.remark = row?.remark || ''
  roleDialogVisible.value = true
}

async function saveRole() {
  if (!roleForm.roleCode.trim() || !roleForm.roleName.trim()) {
    ElMessage.warning('请填写角色编码和角色名称')
    return
  }
  savingRole.value = true
  try {
    if (editingRoleId.value) {
      await updateRole(editingRoleId.value, roleForm)
      ElMessage.success('角色已更新')
    } else {
      await createRole(roleForm)
      ElMessage.success('角色已创建')
    }
    roleDialogVisible.value = false
    await loadRoles()
  } finally {
    savingRole.value = false
  }
}

async function handleDeleteRole(row: RoleItem) {
  await ElMessageBox.confirm(`确认删除角色「${row.roleName}」吗？已分配给用户的角色不能删除。`, '删除角色', {
    type: 'warning',
  })
  await deleteRole(row.id)
  ElMessage.success('角色已删除')
  if (currentRole.value?.id === row.id) {
    currentRole.value = null
    menuTreeRef.value?.setCheckedKeys([])
  }
  await loadRoles()
}

async function saveRoleMenus() {
  if (!currentRole.value) return
  const checked = menuTreeRef.value?.getCheckedKeys(false) || []
  const halfChecked = menuTreeRef.value?.getHalfCheckedKeys() || []
  const menuIds = Array.from(new Set([...checked, ...halfChecked])) as ApiId[]
  savingMenus.value = true
  try {
    await assignRoleMenus(currentRole.value.id, menuIds)
    ElMessage.success('角色权限已保存')
  } finally {
    savingMenus.value = false
  }
}

function menuTypeText(type: string) {
  return type === 'CATALOG' ? '目录' : type === 'BUTTON' ? '按钮' : '菜单'
}

function menuTagType(type: string) {
  return type === 'BUTTON' ? 'warning' : type === 'CATALOG' ? 'info' : 'success'
}

onMounted(async () => {
  await Promise.all([loadMenus(), loadRoles()])
})
</script>

<style scoped>
.role-page {
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(360px, 0.9fr);
  gap: 16px;
}

.table-header,
.section-header {
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

.section-header h2 {
  margin: 0;
  font-size: 18px;
}

.section-header p {
  margin: 4px 0 0;
  color: #667085;
  font-size: 13px;
}

.menu-panel {
  min-height: 560px;
}

.menu-node {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.menu-node small {
  color: #98a2b3;
}

@media (max-width: 1100px) {
  .role-page {
    grid-template-columns: 1fr;
  }
}
</style>
