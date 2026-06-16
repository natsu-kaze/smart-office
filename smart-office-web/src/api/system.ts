import request from '@/utils/request'
import type { PageResult, UserItem } from '@/types/api'

export function getUsers(params: Record<string, unknown>) {
  return request.get<PageResult<UserItem>>('/api/system/users', { params })
}

export function getRoles(params: Record<string, unknown>) {
  return request.get('/api/system/roles', { params })
}

export function getMenusTree() {
  return request.get('/api/system/menus/tree')
}
