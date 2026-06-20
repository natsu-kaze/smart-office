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

export function uploadPolicy(file: File, data: Record<string, unknown>) {
  const formData = new FormData()
  formData.append('file', file)
  Object.entries(data).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      formData.append(key, String(value))
    }
  })
  return request.post<PolicyDocument>('/api/policies/upload', formData)
}

export function updatePolicy(id: ApiId, data: Record<string, unknown>) {
  return request.put<PolicyDocument>(`/api/policies/${id}`, data)
}

export function deletePolicy(id: ApiId) {
  return request.delete(`/api/policies/${id}`)
}
