import request from '@/utils/request'
import type {
  ApiId,
  MenuNode,
  PageResult,
  PasswordChangePayload,
  ProfileUpdatePayload,
  RoleItem,
  RoleSavePayload,
  UserItem,
} from '@/types/api'

export function createUser(data: { username: string; password: string; realName: string; phone?: string; email?: string; avatar?: string }) {
  return request.post<UserItem>('/api/system/users', data)
}

export function getUsers(params: Record<string, unknown>) {
  return request.get<PageResult<UserItem>>('/api/system/users', { params })
}

export function getUserOptions() {
  return request.get<UserItem[]>('/api/system/users/options')
}

export function getProfile() {
  return request.get<UserItem>('/api/system/users/profile')
}

export function updateProfile(data: ProfileUpdatePayload) {
  return request.put<UserItem>('/api/system/users/profile', data)
}

export function changePassword(data: PasswordChangePayload) {
  return request.put<void>('/api/system/users/profile/password', data)
}

export function assignUserRoles(userId: ApiId, roleIds: ApiId[]) {
  return request.put<void>(`/api/system/users/${userId}/roles`, { roleIds })
}

export function getRoles(params: Record<string, unknown>) {
  return request.get<PageResult<RoleItem>>('/api/system/roles', { params })
}

export function getRoleOptions() {
  return request.get<RoleItem[]>('/api/system/roles/options')
}

export function createRole(data: RoleSavePayload) {
  return request.post<RoleItem>('/api/system/roles', data)
}

export function updateRole(roleId: ApiId, data: RoleSavePayload) {
  return request.put<RoleItem>(`/api/system/roles/${roleId}`, data)
}

export function deleteRole(roleId: ApiId) {
  return request.delete<void>(`/api/system/roles/${roleId}`)
}

export function getRoleMenuIds(roleId: ApiId) {
  return request.get<ApiId[]>(`/api/system/roles/${roleId}/menus`)
}

export function assignRoleMenus(roleId: ApiId, menuIds: ApiId[]) {
  return request.put<void>(`/api/system/roles/${roleId}/menus`, { menuIds })
}

export function getMenusTree() {
  return request.get<MenuNode[]>('/api/system/menus/tree')
}
