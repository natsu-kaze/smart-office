import request from '@/utils/request'
import type { ApprovalItem, PageResult } from '@/types/api'

export function getMyApprovals(params: Record<string, unknown>) {
  return request.get<PageResult<ApprovalItem>>('/api/approvals/my', { params })
}

export function getApprovalTodos(params: Record<string, unknown>) {
  return request.get<PageResult<ApprovalItem>>('/api/approvals/todos', { params })
}

export function createApproval(data: Record<string, unknown>) {
  return request.post<ApprovalItem>('/api/approvals', data)
}

export function submitApproval(id: number) {
  return request.post<ApprovalItem>(`/api/approvals/${id}/submit`)
}

export function approveApproval(id: number, comment?: string) {
  return request.post<ApprovalItem>(`/api/approvals/${id}/approve`, { comment })
}

export function rejectApproval(id: number, comment?: string) {
  return request.post<ApprovalItem>(`/api/approvals/${id}/reject`, { comment })
}
