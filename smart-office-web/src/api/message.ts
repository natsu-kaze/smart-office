import request from '@/utils/request'
import type { ApiId, MessageNotice, MessageTodo, PageResult } from '@/types/api'

export function getUnreadCount() {
  return request.get<number>('/api/messages/unread-count')
}

export function getTodoCount() {
  return request.get<number>('/api/messages/todos/count')
}

export function getMessages(params: Record<string, unknown>) {
  return request.get<PageResult<MessageNotice>>('/api/messages', { params })
}

export function getMessageTodos(params: Record<string, unknown>) {
  return request.get<PageResult<MessageTodo>>('/api/messages/todos', { params })
}

export function markMessageRead(id: ApiId) {
  return request.patch(`/api/messages/${id}/read`)
}

export function completeMessageTodo(id: ApiId) {
  return request.patch(`/api/messages/todos/${id}/complete`)
}
