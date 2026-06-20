<template>
  <section class="page-grid org-page">
    <div class="page-card">
      <header class="section-header org-header">
        <div>
          <h2>部门树</h2>
          <p>编辑上级部门即可调整组织上下级；部门主管会作为审批第一处理人。</p>
        </div>
        <el-space wrap>
          <el-button size="small" type="primary" @click="openCreateDepartment()">新增</el-button>
          <el-button size="small" :disabled="!selectedDepartment" @click="openEditDepartment">编辑</el-button>
          <el-button size="small" :disabled="!selectedDepartment" type="danger" plain @click="handleDeleteDepartment">
            删除
          </el-button>
          <el-button size="small" @click="load">刷新</el-button>
        </el-space>
      </header>

      <el-alert
        v-if="selectedDepartment"
        class="selected-alert"
        type="info"
        :closable="false"
        show-icon
      >
        <template #title>
          当前选中：{{ selectedDepartment.departmentName }}
          <span v-if="selectedDepartment.leaderName">，主管：{{ selectedDepartment.leaderName }}</span>
        </template>
      </el-alert>

      <el-tree
        :data="departments"
        node-key="id"
        default-expand-all
        highlight-current
        :props="{ label: 'departmentName', children: 'children' }"
        @node-click="handleDepartmentSelect"
      >
        <template #default="{ data }">
          <span class="tree-node">
            <span>{{ data.departmentName }}</span>
            <el-tag v-if="data.leaderName" size="small" type="info">主管 {{ data.leaderName }}</el-tag>
          </span>
        </template>
      </el-tree>
    </div>

    <div class="page-card">
      <header class="section-header">
        <h2>岗位列表</h2>
        <el-space wrap>
          <el-button size="small" type="primary" @click="openCreatePosition">新增岗位</el-button>
          <el-button size="small" @click="load">刷新</el-button>
        </el-space>
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
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-space>
              <el-button size="small" type="primary" plain @click="openEditPosition(row)">编辑</el-button>
              <el-button size="small" type="danger" plain @click="handleDeletePosition(row)">删除</el-button>
            </el-space>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="departmentDialogVisible" :title="departmentDialogTitle" width="560px">
      <el-form label-width="140px">
        <el-form-item label="上级部门">
          <el-select v-model="departmentForm.parentId" class="full-width" filterable>
            <el-option label="根部门" :value="0" />
            <el-option
              v-for="item in departmentOptions"
              :key="item.id"
              :label="item.label"
              :value="item.id"
              :disabled="isUnavailableParent(item.id)"
            />
          </el-select>
          <div class="form-help">编辑时修改这里，就会把部门移动到新的上级下面。</div>
        </el-form-item>
        <el-form-item label="部门编码" required>
          <el-input v-model="departmentForm.departmentCode" placeholder="例如 RD" />
        </el-form-item>
        <el-form-item label="部门名称" required>
          <el-input v-model="departmentForm.departmentName" placeholder="例如 研发部" />
        </el-form-item>
        <el-form-item label="部门主管ID">
          <el-input-number v-model="departmentForm.leaderUserId" class="full-width" :min="1" :controls="false" />
          <div class="form-help">审批提交后，第一审批人取申请人所在部门的主管。</div>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="departmentForm.sort" class="full-width" :min="0" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="departmentForm.status">
            <el-radio-button :label="1">启用</el-radio-button>
            <el-radio-button :label="0">禁用</el-radio-button>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="departmentDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitDepartment">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="positionDialogVisible" :title="positionDialogTitle" width="520px">
      <el-form label-width="96px">
        <el-form-item label="所属部门" required>
          <el-select v-model="positionForm.departmentId" class="full-width" filterable>
            <el-option v-for="item in departmentOptions" :key="item.id" :label="item.label" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="岗位编码" required>
          <el-input v-model="positionForm.positionCode" placeholder="例如 DEV" />
        </el-form-item>
        <el-form-item label="岗位名称" required>
          <el-input v-model="positionForm.positionName" placeholder="例如 后端工程师" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="positionForm.sort" class="full-width" :min="0" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="positionForm.status">
            <el-radio-button :label="1">启用</el-radio-button>
            <el-radio-button :label="0">禁用</el-radio-button>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="positionDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitPosition">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createDepartment,
  createPosition,
  deleteDepartment,
  deletePosition,
  getDepartmentTree,
  getPositions,
  updateDepartment,
  updatePosition,
} from '@/api/org'
import type {
  ApiId,
  DepartmentNode,
  DepartmentSavePayload,
  PositionItem,
  PositionSavePayload,
} from '@/types/api'

interface DepartmentOption {
  id: ApiId
  label: string
}

const departments = ref<DepartmentNode[]>([])
const positions = ref<PositionItem[]>([])
const selectedDepartment = ref<DepartmentNode>()
const loading = ref(false)

const departmentDialogVisible = ref(false)
const departmentDialogMode = ref<'create' | 'edit'>('create')
const departmentForm = reactive<DepartmentSavePayload>({
  parentId: 0,
  departmentCode: '',
  departmentName: '',
  leaderUserId: undefined,
  sort: 0,
  status: 1,
})

const positionDialogVisible = ref(false)
const positionDialogMode = ref<'create' | 'edit'>('create')
const editingPositionId = ref<ApiId>()
const positionForm = reactive<PositionSavePayload>({
  departmentId: '',
  positionCode: '',
  positionName: '',
  sort: 0,
  status: 1,
})

const departmentDialogTitle = computed(() =>
  departmentDialogMode.value === 'create' ? '新增部门' : '编辑部门',
)
const positionDialogTitle = computed(() =>
  positionDialogMode.value === 'create' ? '新增岗位' : '编辑岗位',
)
const departmentOptions = computed(() => flattenDepartments(departments.value))
const selectedDescendantIds = computed(() => {
  if (!selectedDepartment.value) return new Set<ApiId>()
  const ids = new Set<ApiId>()
  collectDepartmentIds(selectedDepartment.value.children || [], ids)
  return ids
})

async function load() {
  loading.value = true
  try {
    const [tree, positionPage] = await Promise.all([
      getDepartmentTree(),
      getPositions({ current: 1, size: 100 }),
    ])
    departments.value = tree
    positions.value = positionPage.records
    if (selectedDepartment.value) {
      selectedDepartment.value = findDepartment(tree, selectedDepartment.value.id)
    }
  } finally {
    loading.value = false
  }
}

function handleDepartmentSelect(node: DepartmentNode) {
  selectedDepartment.value = node
}

function openCreateDepartment(parent?: DepartmentNode) {
  departmentDialogMode.value = 'create'
  departmentForm.parentId = parent?.id ?? selectedDepartment.value?.id ?? 0
  departmentForm.departmentCode = ''
  departmentForm.departmentName = ''
  departmentForm.leaderUserId = undefined
  departmentForm.sort = 0
  departmentForm.status = 1
  departmentDialogVisible.value = true
}

function openEditDepartment() {
  if (!selectedDepartment.value) return
  departmentDialogMode.value = 'edit'
  departmentForm.parentId = selectedDepartment.value.parentId
  departmentForm.departmentCode = selectedDepartment.value.departmentCode
  departmentForm.departmentName = selectedDepartment.value.departmentName
  departmentForm.leaderUserId = selectedDepartment.value.leaderUserId
  departmentForm.sort = selectedDepartment.value.sort ?? 0
  departmentForm.status = selectedDepartment.value.status
  departmentDialogVisible.value = true
}

async function submitDepartment() {
  if (!departmentForm.departmentCode.trim() || !departmentForm.departmentName.trim()) {
    ElMessage.warning('请填写部门编码和部门名称')
    return
  }
  if (departmentDialogMode.value === 'create') {
    await createDepartment(normalizeDepartmentPayload())
    ElMessage.success('部门已新增')
  } else if (selectedDepartment.value) {
    await updateDepartment(selectedDepartment.value.id, normalizeDepartmentPayload())
    ElMessage.success('部门已更新')
  }
  departmentDialogVisible.value = false
  await load()
}

async function handleDeleteDepartment() {
  if (!selectedDepartment.value) return
  await ElMessageBox.confirm(`确定删除部门「${selectedDepartment.value.departmentName}」吗？`, '删除部门', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消',
  })
  await deleteDepartment(selectedDepartment.value.id)
  ElMessage.success('部门已删除')
  selectedDepartment.value = undefined
  await load()
}

function openCreatePosition() {
  const defaultDepartmentId = selectedDepartment.value?.id ?? departmentOptions.value[0]?.id
  if (defaultDepartmentId === undefined) {
    ElMessage.warning('请先新增部门')
    return
  }
  positionDialogMode.value = 'create'
  editingPositionId.value = undefined
  positionForm.departmentId = defaultDepartmentId
  positionForm.positionCode = ''
  positionForm.positionName = ''
  positionForm.sort = 0
  positionForm.status = 1
  positionDialogVisible.value = true
}

function openEditPosition(row: PositionItem) {
  positionDialogMode.value = 'edit'
  editingPositionId.value = row.id
  positionForm.departmentId = row.departmentId
  positionForm.positionCode = row.positionCode
  positionForm.positionName = row.positionName
  positionForm.sort = row.sort ?? 0
  positionForm.status = row.status
  positionDialogVisible.value = true
}

async function submitPosition() {
  if (!positionForm.departmentId || !positionForm.positionCode.trim() || !positionForm.positionName.trim()) {
    ElMessage.warning('请填写所属部门、岗位编码和岗位名称')
    return
  }
  if (positionDialogMode.value === 'create') {
    await createPosition({ ...positionForm })
    ElMessage.success('岗位已新增')
  } else if (editingPositionId.value) {
    await updatePosition(editingPositionId.value, { ...positionForm })
    ElMessage.success('岗位已更新')
  }
  positionDialogVisible.value = false
  await load()
}

async function handleDeletePosition(row: PositionItem) {
  await ElMessageBox.confirm(`确定删除岗位「${row.positionName}」吗？`, '删除岗位', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消',
  })
  await deletePosition(row.id)
  ElMessage.success('岗位已删除')
  await load()
}

function normalizeDepartmentPayload(): DepartmentSavePayload {
  return {
    ...departmentForm,
    leaderUserId: departmentForm.leaderUserId || null,
  }
}

function isUnavailableParent(id: ApiId) {
  if (departmentDialogMode.value !== 'edit' || !selectedDepartment.value) {
    return false
  }
  return id === selectedDepartment.value.id || selectedDescendantIds.value.has(id)
}

function flattenDepartments(nodes: DepartmentNode[], level = 0): DepartmentOption[] {
  return nodes.flatMap((node) => [
    {
      id: node.id,
      label: `${'　'.repeat(level)}${node.departmentName}`,
    },
    ...flattenDepartments(node.children || [], level + 1),
  ])
}

function collectDepartmentIds(nodes: DepartmentNode[], ids: Set<ApiId>) {
  nodes.forEach((node) => {
    ids.add(node.id)
    collectDepartmentIds(node.children || [], ids)
  })
}

function findDepartment(nodes: DepartmentNode[], id: ApiId): DepartmentNode | undefined {
  for (const node of nodes) {
    if (node.id === id) return node
    const found = findDepartment(node.children || [], id)
    if (found) return found
  }
  return undefined
}

onMounted(load)
</script>

<style scoped>
.org-page {
  grid-template-columns: minmax(420px, 0.8fr) minmax(0, 1.2fr);
}

.org-header {
  align-items: flex-start;
}

.org-header p {
  max-width: 360px;
  margin: 4px 0 0;
  color: #6b7280;
  font-size: 13px;
  line-height: 1.5;
}

.selected-alert {
  margin-bottom: 12px;
}

.tree-node {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.full-width {
  width: 100%;
}

.form-help {
  margin-top: 6px;
  color: #6b7280;
  font-size: 12px;
  line-height: 1.5;
}

@media (max-width: 1100px) {
  .org-page {
    grid-template-columns: 1fr;
  }
}
</style>
