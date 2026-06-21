import request from '@/utils/request'
import type { ApiId, ApprovalItem, ApprovalRecord, ApprovalRule, ApprovalRulePayload, PageResult } from '@/types/api'

export function getMyApprovals(params: Record<string, unknown>) {
  return request.get<PageResult<ApprovalItem>>('/api/approvals/my', { params })
}

export function getApprovalTodos(params: Record<string, unknown>) {
  return request.get<PageResult<ApprovalItem>>('/api/approvals/todos', { params })
}

export function getApprovalDetail(id: ApiId) {
  return request.get<ApprovalItem>(`/api/approvals/${id}`)
}

export function getApprovalTimeline(id: ApiId) {
  return request.get<ApprovalRecord[]>(`/api/approvals/${id}/timeline`)
}

export function createApproval(data: Record<string, unknown>) {
  return request.post<ApprovalItem>('/api/approvals', data)
}

export function updateApproval(id: ApiId, data: Record<string, unknown>) {
  return request.put<ApprovalItem>(`/api/approvals/${id}`, data)
}

export function submitApproval(id: ApiId) {
  return request.post<ApprovalItem>(`/api/approvals/${id}/submit`)
}

export function approveApproval(id: ApiId, comment?: string) {
  return request.post<ApprovalItem>(`/api/approvals/${id}/approve`, { comment })
}

export function rejectApproval(id: ApiId, comment?: string) {
  return request.post<ApprovalItem>(`/api/approvals/${id}/reject`, { comment })
}

export function deleteApproval(id: ApiId) {
  return request.delete<void>(`/api/approvals/${id}`)
}

export function getApprovalRules() {
  return request.get<ApprovalRule[]>('/api/approvals/rules')
}

export function createApprovalRule(data: ApprovalRulePayload) {
  return request.post<ApprovalRule>('/api/approvals/rules', data)
}

export function updateApprovalRule(id: ApiId, data: ApprovalRulePayload) {
  return request.put<ApprovalRule>(`/api/approvals/rules/${id}`, data)
}

export function deleteApprovalRule(id: ApiId) {
  return request.delete<void>(`/api/approvals/rules/${id}`)
}
