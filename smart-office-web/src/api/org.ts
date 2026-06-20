import request from '@/utils/request'
import type {
  ApiId,
  CompanyInfo,
  DepartmentNode,
  DepartmentSavePayload,
  EmployeeItem,
  PageResult,
  PositionItem,
  PositionSavePayload,
} from '@/types/api'

export function getCompany() {
  return request.get<CompanyInfo>('/api/org/company')
}

export function getDepartmentTree() {
  return request.get<DepartmentNode[]>('/api/org/departments/tree')
}

export function createDepartment(data: DepartmentSavePayload) {
  return request.post<DepartmentNode>('/api/org/departments', data)
}

export function updateDepartment(id: ApiId, data: DepartmentSavePayload) {
  return request.put<DepartmentNode>(`/api/org/departments/${id}`, data)
}

export function deleteDepartment(id: ApiId) {
  return request.delete(`/api/org/departments/${id}`)
}

export function getPositions(params: Record<string, unknown>) {
  return request.get<PageResult<PositionItem>>('/api/org/positions', { params })
}

export function createPosition(data: PositionSavePayload) {
  return request.post<PositionItem>('/api/org/positions', data)
}

export function updatePosition(id: ApiId, data: PositionSavePayload) {
  return request.put<PositionItem>(`/api/org/positions/${id}`, data)
}

export function deletePosition(id: ApiId) {
  return request.delete(`/api/org/positions/${id}`)
}

export function getEmployees(params: Record<string, unknown>) {
  return request.get<PageResult<EmployeeItem>>('/api/org/employees', { params })
}
