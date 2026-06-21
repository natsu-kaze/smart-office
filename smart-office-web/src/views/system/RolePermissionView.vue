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
      <el-alert
        class="permission-help"
        type="info"
        :closable="false"
        show-icon
        title="菜单用于控制左侧入口是否可见，按钮权限用于控制发布公告、删除文件、维护制度、分配角色等操作是否允许。保存后相关用户需要重新登录获取新的权限。"
      />
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
        check-strictly
        default-expand-all
        :expand-on-click-node="false"
      >
        <template #default="{ data }">
          <span class="menu-node">
            <el-tag size="small" :type="menuTagType(data.menuType)">{{ menuTypeText(data.menuType) }}</el-tag>
            <span>{{ displayMenuName(data.menuName) }}</span>
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
  const menuIds = menuTreeRef.value?.getCheckedKeys() as ApiId[] || []
  savingMenus.value = true
  try {
    await assignRoleMenus(currentRole.value.id, menuIds)
    ElMessage.success('角色权限已保存，相关用户重新登录后生效')
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

function displayMenuName(name: string) {
  const map: Record<string, string> = {
    // 目录/菜单
    Dashboard: '工作台',
    Workbench: '工作台',
    'System Management': '系统管理',
    System: '系统管理',
    'User Management': '用户管理',
    Users: '用户管理',
    'Role & Permission': '角色权限',
    'Role Permission': '角色权限',
    Roles: '角色权限',
    Organization: '组织架构',
    'Organization Structure': '组织架构',
    Org: '组织架构',
    'Approval Center': '审批中心',
    Approvals: '审批中心',
    'My Applications': '我的申请',
    'My Todos': '我的审批待办',
    'Message Center': '消息中心',
    Messages: '消息中心',
    'File Center': '文件中心',
    Files: '文件中心',
    'Policy Documents': '制度文档',
    'Policy Document': '制度文档',
    Policies: '制度文档',
    Attendance: '考勤打卡',
    'Attendance Clock': '考勤打卡',
    // 按钮权限
    'Assign User Role': '分配用户角色',
    'Assign User Roles': '分配用户角色',
    'Maintain Role': '维护角色',
    'Role Maintenance': '维护角色',
    'Assign Role Menu': '分配角色菜单',
    'Assign Role Menus': '分配角色菜单',
    'Maintain Approval Rules': '维护审批规则',
    'Approval Rule Maintenance': '维护审批规则',
    'Post Announcement': '发布公告',
    'Post Announcements': '发布公告',
    Announcements: '发布公告',
    'Delete File': '删除文件',
    'Delete Files': '删除文件',
    'Maintain Policy': '维护制度',
    'Policy Maintenance': '维护制度',
    'Department Attendance': '部门考勤',
    'Dept Attendance': '部门考勤',
  }
  return map[name] || name
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

.permission-help {
  margin-bottom: 14px;
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
