import request from '@/utils/request'
import type { ApprovalItem, PageResult } from '@/types/api'

export function getMyApprovals(params: Record<string, unknown>) {
  return request.get<PageResult<ApprovalItem>>('/api/approvals/my', { params })
}

export function getApprovalTodos(params: Record<string, unknown>) {
  return request.get<PageResult<ApprovalItem>>('/api/approvals/todos', { params })
}

export function createApproval(data: Record<string, unknown>) {
  return request.post('/api/approvals', data)
}
