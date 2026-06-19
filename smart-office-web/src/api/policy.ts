import request from '@/utils/request'
import type { ApiId, PageResult, PolicyDocument } from '@/types/api'

export function getPolicies(params: Record<string, unknown>) {
  return request.get<PageResult<PolicyDocument>>('/api/policies', { params })
}

export function getPolicyDetail(id: ApiId) {
  return request.get<PolicyDocument>(`/api/policies/${id}`)
}

export function createPolicy(data: Record<string, unknown>) {
  return request.post<PolicyDocument>('/api/policies', data)
}

export function updatePolicy(id: ApiId, data: Record<string, unknown>) {
  return request.put<PolicyDocument>(`/api/policies/${id}`, data)
}

export function deletePolicy(id: ApiId) {
  return request.delete(`/api/policies/${id}`)
}
