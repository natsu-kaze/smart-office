import request from '@/utils/request'
import type { DepartmentNode } from '@/types/api'

export function getCompany() {
  return request.get('/api/org/company')
}

export function getDepartmentTree() {
  return request.get<DepartmentNode[]>('/api/org/departments/tree')
}

export function getPositions(params: Record<string, unknown>) {
  return request.get('/api/org/positions', { params })
}

export function getEmployees(params: Record<string, unknown>) {
  return request.get('/api/org/employees', { params })
}
